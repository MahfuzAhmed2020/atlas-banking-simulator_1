package com.atlas.banking;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.*;
import java.nio.file.*;
import java.security.*;
import java.io.*;
import java.util.*;

@Service
class DepositService {
 private final Checks checks;private final Accounts accounts;private final Ledgers ledger;private final Notices notices;private final AuditService audit;
 @Value("${app.uploads:./private-uploads}") String uploadDir;
 DepositService(Checks checks,Accounts a,Ledgers l,Notices n,AuditService audit){this.checks=checks;this.accounts=a;this.ledger=l;this.notices=n;this.audit=audit;}
 static LocalDate observed(LocalDate date){
  return switch(date.getDayOfWeek()){
   case SATURDAY->date.minusDays(1);case SUNDAY->date.plusDays(1);default->date;
  };
 }
 static LocalDate weekdayHoliday(int year,int month,DayOfWeek weekday,int ordinal){
  LocalDate d=LocalDate.of(year,month,1);
  if(ordinal>0)return d.with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(ordinal,weekday));
  return d.with(java.time.temporal.TemporalAdjusters.lastInMonth(weekday));
 }
 static boolean businessDay(LocalDate d){
  if(d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY)return false;
  int y=d.getYear();
  for(int yr=y;yr<=y+1;yr++){
   if(d.equals(observed(LocalDate.of(yr,1,1)))||d.equals(observed(LocalDate.of(yr,6,19)))||
      d.equals(observed(LocalDate.of(yr,7,4)))||d.equals(observed(LocalDate.of(yr,11,11)))||
      d.equals(observed(LocalDate.of(yr,12,25)))||
      d.equals(weekdayHoliday(yr,1,DayOfWeek.MONDAY,3))||
      d.equals(weekdayHoliday(yr,2,DayOfWeek.MONDAY,3))||
      d.equals(weekdayHoliday(yr,5,DayOfWeek.MONDAY,-1))||
      d.equals(weekdayHoliday(yr,9,DayOfWeek.MONDAY,1))||
      d.equals(weekdayHoliday(yr,10,DayOfWeek.MONDAY,2))||
      d.equals(weekdayHoliday(yr,11,DayOfWeek.THURSDAY,4)))return false;
  }
  return true;
 }
 static LocalDate nextBusiness(LocalDate date){LocalDate day=date.plusDays(1);while(!businessDay(day))day=day.plusDays(1);return day;}
 byte[] image(MultipartFile part)throws IOException {
  if(part==null||part.isEmpty()||part.getSize()>6*1024*1024)throw new IllegalArgumentException("Front and back images required (max 6MB each)");
  byte[] b=part.getBytes();
  boolean png=b.length>8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71;
  boolean jpeg=b.length>4&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;
  if(!png&&!jpeg)throw new IllegalArgumentException("Only PNG and JPEG check images allowed");
  return b;
 }
 String hash(byte[] a,byte[] b){
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");md.update(a);md.update(b);return HexFormat.of().formatHex(md.digest());}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 @Transactional CheckDeposit submit(Long userId,Long accountId,String number,BigDecimal amount,boolean instant,MultipartFile front,MultipartFile back)throws IOException{
  BankAccount a=accounts.findById(accountId).orElseThrow();
  if(!a.userId.equals(userId)||!"ACTIVE".equals(a.status))throw new org.springframework.security.access.AccessDeniedException("Not your account");
  if(number==null || !number.matches("[A-Za-z0-9-]{3,40}"))throw new IllegalArgumentException("Invalid simulated check number");
  amount=Ledgers.money(amount);
  if(instant&&amount.compareTo(new BigDecimal("5.00"))<=0)throw new IllegalArgumentException("Instant deposit requires more than $5");
  byte[] f=image(front),b=image(back);String checksum=hash(f,b);
  if(checks.existsByAccountIdAndCheckNumber(accountId,number)||checks.existsByImageHash(checksum))throw new IllegalArgumentException("Possible duplicate check deposit");
  Files.createDirectories(Path.of(uploadDir));
  String fpath=UUID.randomUUID()+".image";String bpath=UUID.randomUUID()+".image";
  try{
   Files.write(Path.of(uploadDir,fpath),f,StandardOpenOption.CREATE_NEW);
   Files.write(Path.of(uploadDir,bpath),b,StandardOpenOption.CREATE_NEW);
  }catch(IOException e){Files.deleteIfExists(Path.of(uploadDir,fpath));Files.deleteIfExists(Path.of(uploadDir,bpath));throw e;}
  CheckDeposit d=new CheckDeposit();d.userId=userId;d.accountId=accountId;d.checkNumber=number;d.amount=amount;
  d.instant=instant;d.fee=instant?new BigDecimal("5.00"):BigDecimal.ZERO;
  d.frontPath=fpath;d.backPath=bpath;d.imageHash=checksum;d.status="PENDING_REVIEW";
  checks.save(d);notices.post(userId,"Check deposit submitted","Simulated check #"+number+" for $"+amount+" is pending approval.");
  audit.record(userId,"CHECK_SUBMITTED","DEPOSIT",d.id);return d;
 }
 @Transactional CheckDeposit review(Long id,Long reviewer,boolean approve){
  CheckDeposit d=checks.lock(id).orElseThrow();
  if(!"PENDING_REVIEW".equals(d.status))throw new IllegalArgumentException("Deposit already reviewed");
  d.reviewedBy=reviewer;
  if(!approve){d.status="REJECTED";checks.save(d);notices.post(d.userId,"Check deposit rejected","Simulated check #"+d.checkNumber+" was not approved.");}
  else if(d.instant){settle(d);}
  else {d.status="APPROVED_HOLD";d.availableOn=nextBusiness(LocalDate.now());
   checks.save(d);notices.post(d.userId,"Standard deposit approved","Funds expected to be available on "+d.availableOn+" (simulation).");}
  audit.record(reviewer,approve?"CHECK_APPROVED":"CHECK_REJECTED","DEPOSIT",id);return d;
 }
 @Transactional void settle(CheckDeposit d){
  if(!"PENDING_REVIEW".equals(d.status)&&!"APPROVED_HOLD".equals(d.status))return;
  BankAccount a=ledger.account(d.accountId);
  if(!"ACTIVE".equals(a.status))throw new IllegalArgumentException("Cannot credit inactive account");
  BigDecimal net=d.amount.subtract(d.fee);
  a.balance=a.balance.add(net);accounts.save(a);
  Map<String,BigDecimal> postings=new LinkedHashMap<>();
  postings.put("HOUSE:CHECK_CLEARING",d.amount.negate());
  postings.put(Ledgers.key(a.id),net);
  if(d.fee.signum()>0)postings.put("HOUSE:DEPOSIT_FEE_REVENUE",d.fee);
  ledger.post("CHECK:"+d.id,"Simulated check deposit and disclosed fee",postings);
  d.status="FUNDS_AVAILABLE";d.availableOn=LocalDate.now();checks.save(d);
  notices.post(d.userId,"Deposit funds available","$"+net+" is available in your simulated account. Processing fee $"+d.fee+".");
 }
 byte[] readOwned(Long userId,Long depositId,String side){
  CheckDeposit d=checks.findById(depositId).orElseThrow();
  if(!d.userId.equals(userId))throw new org.springframework.security.access.AccessDeniedException("Not your check");
  return load(d,side);
 }
 byte[] load(CheckDeposit d,String side){
  try{return Files.readAllBytes(Path.of(uploadDir,"back".equals(side)?d.backPath:d.frontPath));}
  catch(IOException e){throw new IllegalStateException("Image unavailable");}
 }
}
