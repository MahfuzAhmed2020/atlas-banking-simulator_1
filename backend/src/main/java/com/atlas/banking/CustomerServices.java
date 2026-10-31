package com.atlas.banking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
class CustomerServices {
 Policy policy(){return policy;} Cards cards(){return cards;} ServiceRequests requests(){return requests;}
 SupportMessages messages(){return messages;} Settings settings(){return settings;} TransferSchedules schedules(){return schedules;} AuditService audit(){return audit;}
 final Policy policy; final Accounts accounts; final Cards cards; final ServiceRequests requests;
 final SupportMessages messages; final Settings settings; final TransferSchedules schedules;
 final BankingWorkflows banking; final Notices notices; final AuditService audit; final PasswordEncoder encoder;final Checks checks;
 CustomerServices(Policy p,Accounts a,Cards c,ServiceRequests r,SupportMessages m,Settings s,
 TransferSchedules ts,BankingWorkflows b,Notices n,AuditService au,PasswordEncoder e,Checks checks){
  policy=p;accounts=a;cards=c;requests=r;messages=m;settings=s;schedules=ts;banking=b;notices=n;audit=au;encoder=e;this.checks=checks;
 }
 static String text(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw new IllegalArgumentException("Text required (maximum "+max+" characters)");return value.trim();}
 static void owner(Long expected,Long actual){if(!Objects.equals(expected,actual))throw new AccessDeniedException("Not your record");}
 @Transactional ServiceRequest request(String email,Long accountId,String kind,String details){
  kind=text(kind,40);
  BankUser u=policy.user(email);
  if(accountId!=null)policy.own(email,accountId);
  if(!Set.of("CLOSURE","CARD_ISSUE","CARD_REPLACEMENT","CARD_LIMIT_INCREASE","DISPUTE","STOP_PAYMENT","BENEFICIARY","JOINT_HOLDER","ACCOUNT_DOCUMENT","SUPPORT","FEEDBACK","DIRECT_DEPOSIT").contains(kind))throw new IllegalArgumentException("Unsupported service request");
  if(!Set.of("SUPPORT","FEEDBACK").contains(kind)&&accountId==null)throw new IllegalArgumentException("Select an account");
  ServiceRequest r=new ServiceRequest();r.userId=u.id;r.accountId=accountId;r.kind=kind;r.details=text(details,2000);requests.save(r);
  audit.record(u.id,"SERVICE_REQUESTED","SERVICE_REQUEST",r.id);
  notices.post(u.id,"Service request received","Your "+kind.replace('_',' ')+" request #"+r.id+" is pending review.");return r;
 }
 @Transactional ServiceRequest review(String email,Long id,boolean approve,String notes){
  BankUser staff=policy.user(email);ServiceRequest r=requests.lock(id).orElseThrow(()->new IllegalArgumentException("Request not found"));
  policy.assigned(email,r.userId);
  if(!"PENDING".equals(r.status))throw new IllegalArgumentException("Request already reviewed");
  if(approve){
   // Financial/card/ownership changes require an administrator, not general support staff.
   if(!Set.of("SUPPORT","FEEDBACK","ACCOUNT_DOCUMENT","DIRECT_DEPOSIT").contains(r.kind)&&!"ADMIN".equals(staff.role))throw new AccessDeniedException("Administrator approval required");
   if("CLOSURE".equals(r.kind)){
    BankAccount a=accounts.lock(r.accountId).orElseThrow();
    if(a.balance.signum()!=0)throw new IllegalArgumentException("Account must have a zero balance before closure");
    if(banking.bills().findByAccountId(a.id).stream().anyMatch(bill->"SCHEDULED".equals(bill.status)))throw new IllegalArgumentException("Cancel pending bill payments before closure");
    if(banking.advances().existsByAccountIdAndStatus(a.id,"OUTSTANDING"))throw new IllegalArgumentException("Repay outstanding advances before closure");
    if(checks.findByUserIdOrderByIdDesc(a.userId).stream().anyMatch(d->d.accountId.equals(a.id)&&Set.of("PENDING_REVIEW","APPROVED_HOLD").contains(d.status)))throw new IllegalArgumentException("Resolve pending check deposits before closure");
    if(schedules.findAll().stream().anyMatch(s->"SCHEDULED".equals(s.status)&&(s.fromAccountId.equals(a.id)||s.toAccountId.equals(a.id))))throw new IllegalArgumentException("Cancel scheduled transfers before closure");
    a.status="CLOSED";accounts.save(a);
    cards.findByUserId(r.userId).stream().filter(c->c.accountId.equals(a.id)).forEach(c->{c.status="CLOSED";cards.save(c);});
   } else if("CARD_ISSUE".equals(r.kind)||"CARD_REPLACEMENT".equals(r.kind)){
    BankAccount a=accounts.lock(r.accountId).orElseThrow();if(!"ACTIVE".equals(a.status))throw new IllegalArgumentException("Active account required");
    if(!Set.of("CHECKING","BUSINESS_CHECKING").contains(a.accountType))throw new IllegalArgumentException("Debit cards require a checking account");
    if("CARD_REPLACEMENT".equals(r.kind))cards.findByUserId(r.userId).stream().filter(c->c.accountId.equals(a.id)).forEach(c->{c.status="REPLACED";cards.save(c);});
    BankCard c=new BankCard();c.userId=r.userId;c.accountId=a.id;c.lastFour=String.format("%04d",new java.security.SecureRandom().nextInt(10000));cards.save(c);
   } else if(!Set.of("SUPPORT","FEEDBACK","ACCOUNT_DOCUMENT","DIRECT_DEPOSIT").contains(r.kind)){
    throw new IllegalArgumentException("This request requires investigation; record a resolution note without claiming an automatic account change");
   }
  }
  r.status=approve?"APPROVED":"REJECTED";r.reviewNotes=text(notes,2000);r.reviewedBy=staff.id;requests.save(r);
  notices.post(r.userId,"Service request updated","Request #"+id+" is "+r.status+". "+r.reviewNotes);audit.record(staff.id,"SERVICE_REVIEWED","SERVICE_REQUEST",id);return r;
 }
 @Transactional BankCard card(String email,Long id,String action,String pin,BigDecimal spending,BigDecimal atm,Boolean online,Boolean international){
  action=text(action,20);
  BankUser u=policy.user(email);BankCard c=cards.lock(id).orElseThrow(()->new IllegalArgumentException("Card not found"));owner(u.id,c.userId);
  if(Set.of("LOST","STOLEN","REPLACED","CLOSED").contains(c.status))throw new IllegalArgumentException("Card is permanently unavailable; request a replacement");
  switch(action){
   case "ACTIVATE" -> {if(!"INACTIVE".equals(c.status))throw new IllegalArgumentException("Only a new card can be activated");c.status="ACTIVE";}
   case "LOCK" -> {if(!"ACTIVE".equals(c.status))throw new IllegalArgumentException("Only active cards can be locked");c.status="LOCKED";}
   case "UNLOCK" -> {if(!"LOCKED".equals(c.status))throw new IllegalArgumentException("Only locked cards can be unlocked");c.status="ACTIVE";}
   case "LOST","STOLEN" -> c.status=action;
   case "PIN" -> {if(pin==null||!pin.matches("[0-9]{4,6}"))throw new IllegalArgumentException("Use a fictional 4-6 digit PIN");c.pinHash=encoder.encode(pin);}
   case "CONTROLS" -> {
    if(spending==null||atm==null||spending.signum()<0||atm.signum()<0||spending.compareTo(new BigDecimal("1000"))>0||atm.compareTo(new BigDecimal("300"))>0)throw new IllegalArgumentException("Self-service limits: spending $0-$1000 and ATM $0-$300; increases require review");
    if(spending.scale()>2||atm.scale()>2)throw new IllegalArgumentException("Limits allow two decimal places");
    c.spendingLimit=spending;c.atmLimit=atm;if(online!=null)c.onlineEnabled=online;if(international!=null)c.internationalEnabled=international;
   }
   default -> throw new IllegalArgumentException("Unknown card operation");
  }
  cards.save(c);notices.post(u.id,"Card security update","Card ending "+c.lastFour+": "+action+" completed in the simulator.");audit.record(u.id,"CARD_"+action,"CARD",id);return c;
 }
 @Transactional SupportMessage message(String email,Long customerId,String subject,String body){
  BankUser u=policy.user(email);
  if("CUSTOMER".equals(u.role)){owner(u.id,customerId);}else policy.assigned(email,customerId);
  if(!"CUSTOMER".equals(policy.user(email).role)&&!"CUSTOMER".equals(policyUser(customerId).role))throw new IllegalArgumentException("Select a customer");
  SupportMessage m=new SupportMessage();m.customerId=customerId;m.senderId=u.id;m.subject=text(subject,160);m.body=text(body,4000);messages.save(m);
  audit.record(u.id,"SECURE_MESSAGE_SENT","MESSAGE",m.id);
  if(!u.id.equals(customerId))notices.post(customerId,"New secure message","Sign in to read a response from customer support.");return m;
 }
 BankUser policyUser(Long id){return policy.usersForServices(id);}
 @Transactional CustomerSettings settings(String email,String language,boolean accessibility,boolean marketing){
  BankUser u=policy.user(email);if(!Set.of("en","es").contains(language))throw new IllegalArgumentException("Supported preference values are en and es");
  CustomerSettings s=settings.findByUserId(u.id).orElseGet(CustomerSettings::new);s.userId=u.id;s.language=language;s.accessibility=accessibility;s.marketing=marketing;
  audit.record(u.id,"SETTINGS_UPDATED","USER",u.id);return settings.save(s);
 }
 static LocalDate next(LocalDate date,String frequency){return switch(frequency){case "WEEKLY"->date.plusWeeks(1);case "MONTHLY"->date.plusMonths(1);default->date;};}
 @Transactional TransferSchedule schedule(String email,Long from,Long to,BigDecimal amount,LocalDate date,String frequency){
  if(from==null||to==null||frequency==null)throw new IllegalArgumentException("Funding account, recipient and frequency are required");
  BankUser u=policy.user(email);policy.own(email,from);
  if(from.equals(to)||!accounts.existsById(to))throw new IllegalArgumentException("Select a different recipient account");
  if(date==null||date.isBefore(LocalDate.now())||date.isAfter(LocalDate.now().plusYears(1)))throw new IllegalArgumentException("Choose a date within the next year");
  if(!Set.of("ONCE","WEEKLY","MONTHLY").contains(frequency))throw new IllegalArgumentException("Invalid frequency");
  amount=Ledgers.money(amount);if(amount.compareTo(new BigDecimal("10000"))>0)throw new IllegalArgumentException("Maximum $10,000 per transfer");
  TransferSchedule s=new TransferSchedule();s.userId=u.id;s.fromAccountId=from;s.toAccountId=to;s.amount=amount;s.nextDate=date;s.frequency=frequency;schedules.save(s);
  notices.post(u.id,"Transfer scheduled","Transfer #"+s.id+" scheduled for "+date);audit.record(u.id,"TRANSFER_SCHEDULED","SCHEDULE",s.id);return s;
 }
 @Transactional void runSchedule(Long id){
  TransferSchedule s=schedules.lock(id).orElseThrow();if(!"SCHEDULED".equals(s.status)||s.nextDate.isAfter(LocalDate.now()))return;
  banking.transfer(s.fromAccountId,s.toAccountId,s.amount,"SCHEDULE:"+s.id+":"+s.nextDate,s.userId);
  if("ONCE".equals(s.frequency))s.status="COMPLETED";else s.nextDate=next(s.nextDate,s.frequency);s.lastError=null;schedules.save(s);
 }
 @Transactional void failSchedule(Long id){TransferSchedule s=schedules.lock(id).orElseThrow();if(!"SCHEDULED".equals(s.status))return;s.status="FAILED";s.lastError="Execution failed; check account status, balance and limits. Create a new schedule after resolving the issue.";schedules.save(s);notices.post(s.userId,"Scheduled transfer failed",s.lastError);}
}
