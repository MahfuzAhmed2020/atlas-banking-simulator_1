package com.atlas.banking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.io.*;

@Service
class DocumentService {
 Policy policy(){return policy;} Documents documents(){return documents;} Deliveries deliveries(){return deliveries;}
 final Documents documents; final Deliveries deliveries; final Accounts accounts; final Policy policy;
 final JdbcTemplate jdbc;final AuditService audit;final Notices notices;final JavaMailSender mail;final ObjectMapper mapper;
 @Value("${app.mail-from}") String sender;
 DocumentService(Documents d,Deliveries de,Accounts a,Policy p,JdbcTemplate j,AuditService au,Notices n,JavaMailSender m,ObjectMapper om){documents=d;deliveries=de;accounts=a;policy=p;jdbc=j;audit=au;notices=n;mail=m;mapper=om;}
 void access(String email,Long userId){BankUser u=policy.user(email);if("CUSTOMER".equals(u.role))CustomerServices.owner(u.id,userId);else policy.assigned(email,userId);}
 @Transactional BankDocument generate(String email,Long accountId,String kind,LocalDate start,LocalDate end){
  BankAccount a=accounts.lock(accountId).orElseThrow(()->new IllegalArgumentException("Account not found"));access(email,a.userId);
  return snapshot(a,kind,start,end,policy.user(email).id);
 }
 @Transactional BankDocument snapshot(BankAccount a,String kind,LocalDate start,LocalDate end,Long actor){
  if(!Set.of("STATEMENT","BALANCE","TRANSACTIONS","DIRECT_DEPOSIT").contains(kind))throw new IllegalArgumentException("Unsupported document type");
  if(start==null||end==null||end.isBefore(start)||end.isAfter(LocalDate.now())||start.isBefore(LocalDate.of(2000,1,1))||end.isAfter(start.plusYears(2)))throw new IllegalArgumentException("Choose a valid historical period of at most two years");
  if("BALANCE".equals(kind)&&( !start.equals(LocalDate.now())||!end.equals(LocalDate.now())))throw new IllegalArgumentException("Balance letters use today's balance");
  Optional<BankDocument> old=documents.findByAccountIdAndKindAndPeriodStartAndPeriodEnd(a.id,kind,start,end);if(old.isPresent())return old.get();
  String key=Ledgers.key(a.id);
  BigDecimal total=jdbc.queryForObject("select coalesce(sum(amount),0) from ledger_postings where ledger_account=?",BigDecimal.class,key);
  if(total.compareTo(a.balance)!=0)throw new IllegalStateException("Account balance does not reconcile with ledger; document generation stopped");
  BigDecimal opening=jdbc.queryForObject("select coalesce(sum(p.amount),0) from ledger_postings p join journal_entries j on j.id=p.journal_id where p.ledger_account=? and j.created_at<?",BigDecimal.class,key,start.atStartOfDay());
  List<Map<String,Object>> rows=jdbc.queryForList("select j.created_at as posted_at,j.description,p.amount from ledger_postings p join journal_entries j on j.id=p.journal_id where p.ledger_account=? and j.created_at>=? and j.created_at<? order by j.created_at,j.id,p.id",key,start.atStartOfDay(),end.plusDays(1).atStartOfDay());
  BigDecimal closing=opening;List<String> lines=new ArrayList<>();
  BankUser customer=policy.usersForServices(a.userId);
  lines.add("Customer: "+customer.fullName);lines.add("Account: **** "+a.accountNumber.substring(Math.max(0,a.accountNumber.length()-4))+" / "+a.accountType);
  lines.add("Period: "+start+" to "+end);lines.add("Opening balance: $"+opening.toPlainString());
  lines.add("Date | Description | Amount (USD)");
  for(Map<String,Object> row:rows){BigDecimal amount=(BigDecimal)row.get("AMOUNT");if(amount==null)amount=(BigDecimal)row.get("amount");closing=closing.add(amount);lines.add(row.values().stream().map(String::valueOf).reduce((x,y)->x+" | "+y).orElse(""));}
  lines.add("Closing balance: $"+closing.toPlainString());
  if("BALANCE".equals(kind))lines.add("Balance confirmed at generation: $"+a.balance.toPlainString());
  if("DIRECT_DEPOSIT".equals(kind))lines.add("Direct deposit setup: use only fictional SIM identifiers. No live payroll network is connected.");
  lines.add("All balances and transactions are simulated. This document has no negotiable value.");
  return save(a,kind,start,end,lines,actor);
 }
 BankDocument save(BankAccount a,String kind,LocalDate start,LocalDate end,List<String> lines,Long actor){
  try{
   BankDocument d=new BankDocument();d.userId=a.userId;d.accountId=a.id;d.kind=kind;d.periodStart=start;d.periodEnd=end;d.snapshot=mapper.writeValueAsString(lines);d.pdfData=render(kind.replace('_',' '),lines);documents.save(d);
   audit.record(actor,"DOCUMENT_GENERATED","DOCUMENT",d.id);notices.post(a.userId,"Document ready","Your "+kind.replace('_',' ').toLowerCase(Locale.ROOT)+" is available in Statements and Documents.",false);return d;
  }catch(IOException ex){throw new IllegalStateException("Could not generate PDF",ex);}
 }
 @Transactional byte[] download(String email,Long id){BankDocument d=documents.findById(id).orElseThrow(()->new IllegalArgumentException("Document not found"));access(email,d.userId);audit.record(policy.user(email).id,"DOCUMENT_VIEWED","DOCUMENT",id);return d.pdfData;}
 @Transactional DocumentDelivery email(String email,Long id){
  BankDocument d=documents.findById(id).orElseThrow(()->new IllegalArgumentException("Document not found"));access(email,d.userId);
  DocumentDelivery delivery=new DocumentDelivery();delivery.documentId=id;delivery.status="FAILED";
  try{var message=mail.createMimeMessage();var helper=new MimeMessageHelper(message,true,"UTF-8");helper.setFrom(sender);helper.setTo(policy.usersForServices(d.userId).email);helper.setSubject("Uchit Shikkha Bank simulation document #"+id);helper.setText("Fictional banking document for local testing only. Your document is also available after signing in.");helper.addAttachment("atlas-document-"+id+".pdf",new ByteArrayResource(d.pdfData),"application/pdf");mail.send(message);delivery.status="SENT";}
  catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Test document delivery failed for document {}",id);}
  deliveries.save(delivery);audit.record(policy.user(email).id,"DOCUMENT_EMAIL_"+delivery.status,"DOCUMENT",id);return delivery;
 }
 static byte[] render(String title,List<String> lines)throws IOException{
  try(PDDocument pdf=new PDDocument();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
   PDFont font=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
   PDFont bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
   List<String> wrapped=new ArrayList<>();
   for(String value:lines){String clean=value.replaceAll("[^\\x20-\\x7E]","?");while(clean.length()>88){int split=clean.lastIndexOf(' ',88);if(split<1)split=88;wrapped.add(clean.substring(0,split));clean=clean.substring(split).stripLeading();}wrapped.add(clean);wrapped.add("");}
   int count=Math.max(1,(wrapped.size()+39)/40);
   for(int page=0;page<count;page++){
    PDPage p=new PDPage(PDRectangle.LETTER);pdf.addPage(p);
    try(PDPageContentStream out=new PDPageContentStream(pdf,p)){
     out.setNonStrokingColor(new java.awt.Color(12,86,62));out.addRect(0,680,612,112);out.fill();
     out.setNonStrokingColor(java.awt.Color.WHITE);write(out,bold,23,44,742,"UCHIT SHIKKHA BANK");write(out,font,11,44,717,"FICTIONAL BANKING SIMULATOR / TEST ONLY");
     out.setNonStrokingColor(java.awt.Color.BLACK);write(out,bold,16,44,650,title);
     int y=622;for(int i=page*40;i<Math.min(wrapped.size(),(page+1)*40);i++){write(out,font,10,44,y,wrapped.get(i));y-=13;}
     out.setNonStrokingColor(java.awt.Color.GRAY);write(out,font,9,44,45,"Simulated funds only. Not valid for real financial use.       Page "+(page+1)+" of "+count);
    }
   }
   pdf.save(bytes);return bytes.toByteArray();
  }
 }
 @Transactional BankDocument receipt(String email,String type,Long sourceId){
  if(sourceId==null||sourceId<1)throw new IllegalArgumentException("Source record ID required");
  Map<String,Object> row;Long accountId;String kind=type+"_"+sourceId;
  BankUser viewer=policy.user(email);
  switch(type){
   case "TRANSFER_RECEIPT" -> {
    row=one("select from_account_id,to_account_id,amount,status,created_at from transfers where id=?",sourceId);
    Long from=((Number)row.get("from_account_id")).longValue(),to=((Number)row.get("to_account_id")).longValue();
    accountId="CUSTOMER".equals(viewer.role)&&accounts.findById(to).orElseThrow().userId.equals(viewer.id)?to:from;
   }
   case "BILL_RECEIPT" -> {row=one("select account_id,biller,reference,amount,scheduled_on,status from bill_payments where id=?",sourceId);accountId=((Number)row.get("account_id")).longValue();if(!"COMPLETED".equals(row.get("status")))throw new IllegalArgumentException("Receipt is available after payment completes");}
   case "CHECK_RECEIPT" -> {row=one("select account_id,check_number,amount,fee,status,available_on from check_deposits where id=?",sourceId);accountId=((Number)row.get("account_id")).longValue();if(!"FUNDS_AVAILABLE".equals(row.get("status")))throw new IllegalArgumentException("Receipt is available after funds are released");}
   case "ADVANCE_LETTER" -> {row=one("select account_id,amount,repaid,status,created_at from cash_advances where id=?",sourceId);accountId=((Number)row.get("account_id")).longValue();}
   case "LOAN_STATEMENT" -> {row=one("select user_id,name,amount_due,due_on,status from loans where id=?",sourceId);Long uid=((Number)row.get("user_id")).longValue();access(email,uid);accountId=firstAccount(uid);}
   case "FEMA_LETTER" -> {row=one("select l.user_id,d.disaster_number,d.original_due,d.revised_due,d.status from deferrals d join loans l on l.id=d.loan_id where d.id=?",sourceId);Long uid=((Number)row.get("user_id")).longValue();access(email,uid);accountId=firstAccount(uid);}
   case "REFERRAL_LETTER" -> {row=one("select referrer_id,bonus_amount,status from referrals where id=?",sourceId);Long uid=((Number)row.get("referrer_id")).longValue();access(email,uid);if(!"PAID".equals(row.get("status")))throw new IllegalArgumentException("Reward confirmation requires a paid reward");accountId=firstAccount(uid);}
   case "ACCOUNT_LETTER" -> {row=one("select user_id,account_type,status,created_at from bank_accounts where id=?",sourceId);accountId=sourceId;}
   default -> throw new IllegalArgumentException("Unsupported receipt type");
  }
  BankAccount a=accounts.lock(accountId).orElseThrow();access(email,a.userId);LocalDate today=LocalDate.now();
  Optional<BankDocument> old=documents.findByAccountIdAndKindAndPeriodStartAndPeriodEnd(a.id,kind,today,today);if(old.isPresent())return old.get();
  List<String> lines=new ArrayList<>();lines.add("Customer: "+policy.usersForServices(a.userId).fullName);lines.add("Account: **** "+a.accountNumber.substring(a.accountNumber.length()-4));lines.add("Source reference: "+sourceId);lines.add("Generated: "+LocalDateTime.now());
  row.forEach((k,v)->{if(!k.equalsIgnoreCase("user_id")&&!k.equalsIgnoreCase("referrer_id")&&!k.endsWith("account_id"))lines.add(k.replace('_',' ')+": "+String.valueOf(v));});
  lines.add("Fictional banking simulation. No real funds or payment network involved.");return save(a,kind,today,today,lines,viewer.id);
 }
 Map<String,Object> one(String sql,Long id){List<Map<String,Object>> rows=jdbc.queryForList(sql,id);if(rows.isEmpty())throw new IllegalArgumentException("Source record not found");return rows.getFirst();}
 Long firstAccount(Long userId){return accounts.findByUserId(userId).stream().map(a->a.id).findFirst().orElseThrow(()->new IllegalArgumentException("Customer has no account for this document"));}
 static void write(PDPageContentStream out,PDFont font,int size,int x,int y,String text)throws IOException{out.beginText();out.setFont(font,size);out.newLineAtOffset(x,y);out.showText(text);out.endText();}
}
