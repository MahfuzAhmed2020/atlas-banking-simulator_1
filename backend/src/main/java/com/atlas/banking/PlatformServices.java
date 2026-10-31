package com.atlas.banking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
class AuditService {
 private final Audits audits;
 AuditService(Audits audits) {this.audits=audits;}
 @Transactional void record(Long actor,String action,String targetType,Object id) {
  AuditEvent e=new AuditEvent(); e.actorId=actor;e.action=action;e.targetType=targetType;e.targetId=String.valueOf(id);audits.save(e);
 }
}
@Service
class Notices {
 private final Notifications notifications;private final Users users; private final JavaMailSender mail;
 @Value("${app.mail-from}") String sender;
 Notices(Notifications n,Users u,JavaMailSender mail){this.notifications=n;this.users=u;this.mail=mail;}
 @Transactional void post(Long userId,String title,String body) {post(userId,title,body,true);}
 @Transactional void post(Long userId,String title,String body,boolean sendEmail){
  BankNotification n=new BankNotification();n.userId=userId;n.title=title;n.message=body;notifications.save(n);
  if(sendEmail)sendEmail(users.findById(userId).orElseThrow().email,title,body);
 }
 void sendEmail(String recipient,String subject,String content) {
  try {SimpleMailMessage message=new SimpleMailMessage();message.setFrom(sender);message.setTo(recipient);
   message.setSubject(subject);message.setText(content);mail.send(message);
  } catch(Exception ex) { org.slf4j.LoggerFactory.getLogger(Notices.class).warn("MailHog delivery unavailable; saved in-app notification: {}",subject); }
 }
}
@Service
class CodesService {
 private final Codes codes;private final Users users;private final PasswordEncoder encoder;private final Notices notices;
 private final SecureRandom secureRandom=new SecureRandom();
 CodesService(Codes c,Users u,PasswordEncoder p,Notices n){codes=c;users=u;encoder=p;notices=n;}
 @Transactional void deliver(String email,String purpose){
  BankUser u=users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalArgumentException("Unknown user"));
  String otp=String.format("%06d",secureRandom.nextInt(1_000_000));
  OneTimeCode code=new OneTimeCode();code.userId=u.id;code.codeHash=encoder.encode(otp);
  code.purpose=purpose;code.expiresAt=LocalDateTime.now().plusMinutes(5);codes.save(code);
  notices.sendEmail(u.email,"Atlas Bank demo verification code", "Your simulated bank "+purpose+" code is "+otp+". It expires in 5 minutes. Never share it.");
 }
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) boolean verify(String email,String purpose,String otp){
  BankUser u=users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalArgumentException("Invalid code"));
  List<OneTimeCode> items=codes.findTop10ByUserIdAndPurposeOrderByIdDesc(u.id,purpose);
  if(items.isEmpty())return false;
  OneTimeCode code=items.get(0);
  if(code.used || code.expiresAt.isBefore(LocalDateTime.now()) || code.attempts>=5)return false;
  code.attempts++;
  if(!encoder.matches(otp,code.codeHash))return false;
  code.used=true;
  return true;
 }
}
@Service
class Ledgers {
 private final Accounts accounts;private final Journals journals;private final Postings postings;private final Preferences prefs;private final Notices notices;
 Ledgers(Accounts a,Journals j,Postings p,Preferences prefs,Notices notices){accounts=a;journals=j;postings=p;this.prefs=prefs;this.notices=notices;}
 static BigDecimal money(BigDecimal value){
  if(value==null || value.signum()<=0 || value.scale()>2 || value.compareTo(new BigDecimal("1000000"))>0)
   throw new IllegalArgumentException("Amount must be positive, up to $1,000,000, with at most two decimals");
  return value.setScale(2);
 }
 @Transactional BankAccount account(Long id){return accounts.lock(id).orElseThrow(()->new IllegalArgumentException("Unknown account"));}
 @Transactional void post(String key,String description,Map<String,BigDecimal> amounts){
  if(journals.existsByReferenceKey(key))throw new IllegalStateException("Duplicate transaction reference");
  BigDecimal total=amounts.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
  if(total.compareTo(BigDecimal.ZERO)!=0)throw new IllegalStateException("Unbalanced journal entry");
  JournalEntry j=new JournalEntry();j.referenceKey=key;j.description=description;journals.save(j);
  amounts.forEach((ledger,amount)->{
    if(amount.signum()==0)return;
    LedgerPosting p=new LedgerPosting();p.journalId=j.id;p.ledgerAccount=ledger;p.amount=amount;postings.save(p);
  });
 }
 static String key(Long accountId){return "CUSTOMER:"+accountId;}
 @Transactional void credit(BankAccount a,BigDecimal amount,String journalKey,String description,String source){
  amount=money(amount);
  a.balance=a.balance.add(amount);accounts.save(a);
  post(journalKey,description,Map.of(key(a.id),amount,source,amount.negate()));
  balanceAlert(a);largeAlert(a,amount);
 }
 @Transactional void debit(BankAccount a,BigDecimal amount,String journalKey,String description,String destination){
  amount=money(amount);
  if(!"ACTIVE".equals(a.status) || a.balance.compareTo(amount)<0)throw new IllegalArgumentException("Account inactive or insufficient available balance");
  a.balance=a.balance.subtract(amount);accounts.save(a);
  post(journalKey,description,Map.of(key(a.id),amount.negate(),destination,amount));
  balanceAlert(a);largeAlert(a,amount);
 }
 void largeAlert(BankAccount a,BigDecimal amount){prefs.findByAccountId(a.id).ifPresent(p->{
   if(amount.compareTo(p.largeTransaction)>=0)notices.post(a.userId,"Large account transaction","A simulated transaction of $"+amount+" occurred on account ending "+a.accountNumber.substring(a.accountNumber.length()-4),p.emailEnabled);
  });}
 void balanceAlert(BankAccount a){
  prefs.findByAccountId(a.id).ifPresent(p->{if(a.balance.compareTo(p.lowBalance)<0)
    notices.post(a.userId,"Low account balance","Account ending "+a.accountNumber.substring(a.accountNumber.length()-4)+" is below $"+p.lowBalance,p.emailEnabled);
  });
 }
 @Transactional void transfer(BankAccount sender,BankAccount recipient,BigDecimal amount,String key){
  amount=money(amount);
  if(sender.id.equals(recipient.id))throw new IllegalArgumentException("Cannot transfer to the same account");
  if(!"ACTIVE".equals(sender.status) || !"ACTIVE".equals(recipient.status))throw new IllegalArgumentException("Account inactive");
  if(sender.balance.compareTo(amount)<0)throw new IllegalArgumentException("Insufficient funds");
  sender.balance=sender.balance.subtract(amount);recipient.balance=recipient.balance.add(amount);
  accounts.save(sender);accounts.save(recipient);
  post(key,"Internal customer transfer",Map.of(key(sender.id),amount.negate(),key(recipient.id),amount));
  balanceAlert(sender);largeAlert(sender,amount);largeAlert(recipient,amount);
 }
}
@Service
class Policy {
 private final Users users;private final Accounts accounts;private final Assignments assigns;
 Policy(Users u,Accounts a,Assignments as){users=u;accounts=a;assigns=as;}
 BankUser user(String email){return users.findByEmailIgnoreCase(email).orElseThrow();}
 BankAccount own(String email,Long accountId){
  BankUser u=user(email);BankAccount a=accounts.findById(accountId).orElseThrow(()->new IllegalArgumentException("Unknown account"));
  if(!a.userId.equals(u.id))throw new org.springframework.security.access.AccessDeniedException("Not your account");
  return a;
 }
 void assigned(String employee,Long customer){
  BankUser e=user(employee);
  if("ADMIN".equals(e.role))return;
  if(!"EMPLOYEE".equals(e.role)||!assigns.existsByEmployeeIdAndCustomerId(e.id,customer))
   throw new org.springframework.security.access.AccessDeniedException("Customer not assigned");
 }
}
