package com.atlas.banking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
class BankingWorkflows {
 Bills bills(){return bills;} Advances advances(){return advances;}
 final Users users;final Accounts accounts;final Applications applications;final Ledgers ledgers;
 final Transfers transfers;final DirectDeposits payroll;final Advances advances;final Referrals referrals;
 final Bills bills;final Preferences prefs;final Notices notices;final AuditService audit;final Products products;
 BankingWorkflows(Users u,Accounts a,Applications ap,Ledgers l,Transfers t,DirectDeposits d,Advances ad,
  Referrals r,Bills b,Preferences p,Notices n,AuditService audit,Products products){
  this.users=u;this.accounts=a;this.applications=ap;this.ledgers=l;this.transfers=t;this.payroll=d;
  this.advances=ad;this.referrals=r;this.bills=b;this.prefs=p;this.notices=n;this.audit=audit;this.products=products;
 }
 @Transactional AccountApplication apply(Long user,String type) {
  BankingProduct product=products.findByCode(type).orElseThrow(()->new IllegalArgumentException("Unsupported account product"));
  if(!product.enabled)throw new IllegalArgumentException("This product is not currently available");
  if(!List.of("CHECKING","SAVINGS","BUSINESS_CHECKING","BUSINESS_SAVINGS","MONEY_MARKET","CD").contains(type))throw new IllegalArgumentException("This product requires a specialist credit or joint-owner application; it cannot be opened as a deposit account");
  AccountApplication ap=new AccountApplication();ap.userId=user;ap.accountType=type;ap.status="PENDING";applications.save(ap);
  audit.record(user,"ACCOUNT_APPLIED","APPLICATION",ap.id);return ap;
 }
 @Transactional BankAccount approveApplication(Long appId,Long reviewer){
  AccountApplication ap=applications.lock(appId).orElseThrow();
  if(!products.findByCode(ap.accountType).map(p->p.enabled).orElse(false))throw new IllegalArgumentException("Product is disabled");
  if(!"PENDING".equals(ap.status))throw new IllegalArgumentException("Application already processed");
  BankAccount a=new BankAccount();a.userId=ap.userId;a.accountType=ap.accountType;a.status="ACTIVE";
  a.accountNumber="SIM"+UUID.randomUUID().toString().replace("-","").substring(0,14).toUpperCase(Locale.ROOT);
  accounts.save(a);ap.status="APPROVED";ap.reviewedBy=reviewer;
  AlertPreference pref=new AlertPreference();pref.accountId=a.id;prefs.save(pref);
  notices.post(a.userId,"Account approved","Your simulated "+a.accountType+" account ending "+a.accountNumber.substring(a.accountNumber.length()-4)+" is ready.");
  audit.record(reviewer,"ACCOUNT_APPROVED","APPLICATION",ap.id);
  // Demo policy: referral reward after first approved account. Only once per referred person.
  referrals.findByReferredId(a.userId).ifPresent(r->{
    if("REGISTERED".equals(r.status)){
     List<BankAccount> sponsor=accounts.findByUserIdAndStatus(r.referrerId,"ACTIVE");
     if(!sponsor.isEmpty()){
      BankAccount payee=ledgers.account(sponsor.get(0).id);
      ledgers.credit(payee,new BigDecimal("500.00"),"REFERRAL:"+r.id,"Qualified simulated referral bonus","HOUSE:REFERRAL_EXPENSE");
      r.status="PAID";referrals.save(r);
      notices.post(r.referrerId,"Referral reward credited","Your $500 simulated referral bonus has been credited.");
     } else {r.status="QUALIFIED_PENDING_ACCOUNT";referrals.save(r);}
    }
  });
  return a;
 }
 @Transactional BankTransfer transfer(Long senderId,Long recipientId,BigDecimal amount,String requestKey,Long senderUser){
  if(requestKey==null || !requestKey.matches("[A-Za-z0-9:_-]{8,90}"))throw new IllegalArgumentException("Provide a unique request key (8-90 characters)");
  Optional<BankTransfer> previous=transfers.findByRequestKey(requestKey);
  if(previous.isPresent()){
   BankAccount owned=accounts.findById(senderId).orElseThrow(()->new IllegalArgumentException("Account not found"));
   if(!owned.userId.equals(senderUser))throw new org.springframework.security.access.AccessDeniedException("Not your account");
   BankTransfer old=previous.get();
   if(!old.fromAccountId.equals(senderId)||!old.toAccountId.equals(recipientId)||old.amount.compareTo(amount)!=0)
    throw new IllegalArgumentException("Idempotency key already used for different transfer");
   return old;
  }
  if(senderId.equals(recipientId))throw new IllegalArgumentException("Cannot transfer to same account");
  BankAccount first=ledgers.account(Math.min(senderId,recipientId));
  BankAccount second=ledgers.account(Math.max(senderId,recipientId));
  BankAccount source=senderId.equals(first.id)?first:second, target=recipientId.equals(first.id)?first:second;
  if(!source.userId.equals(senderUser))throw new org.springframework.security.access.AccessDeniedException("Not your account");
  if(amount.compareTo(new BigDecimal("10000.00"))>0)throw new IllegalArgumentException("Maximum $10,000 per transfer");
  ledgers.transfer(source,target,amount,"TRANSFER:"+requestKey);
  BankTransfer t=new BankTransfer();t.fromAccountId=senderId;t.toAccountId=recipientId;t.amount=Ledgers.money(amount);t.status="COMPLETED";t.requestKey=requestKey;transfers.save(t);
  notices.post(source.userId,"Transfer sent","Sent $"+amount+" to account ending "+target.accountNumber.substring(target.accountNumber.length()-4));
  notices.post(target.userId,"Transfer received","Received $"+amount+" from another simulated bank account.");
  audit.record(senderUser,"TRANSFER_COMPLETED","TRANSFER",t.id);
  return t;
 }
 @Transactional DirectDeposit payPayroll(Long accountId,BigDecimal amount,String employer,Long admin){
  BankAccount a=ledgers.account(accountId);
  if(!"ACTIVE".equals(a.status))throw new IllegalArgumentException("Inactive account");
  DirectDeposit d=new DirectDeposit();d.accountId=a.id;d.amount=Ledgers.money(amount);d.employer=employer;payroll.save(d);
  ledgers.credit(a,d.amount,"PAYROLL:"+d.id,"Simulated direct deposit - "+employer,"HOUSE:PAYROLL_CLEARING");
  audit.record(admin,"PAYROLL_SIMULATED","ACCOUNT",a.id);
  notices.post(a.userId,"Direct deposit received","A simulated payroll deposit of $"+amount+" was credited.");
  // Repayment uses payroll funds, never unrelated account balances.
  for(CashAdvance cash:advances.findByAccountId(a.id)){
   if(!"OUTSTANDING".equals(cash.status))continue;
   BigDecimal due=cash.amount.subtract(cash.repaid);
   BigDecimal repay=due.min(d.amount).min(a.balance);
   if(repay.signum()>0){ledgers.debit(a,repay,"ADVANCE_REPAY:"+d.id+":"+cash.id,"Advance repayment from qualifying payroll","HOUSE:ADVANCE_RECEIVABLE");
    cash.repaid=cash.repaid.add(repay);cash.status=cash.repaid.compareTo(cash.amount)>=0?"REPAID":"OUTSTANDING";advances.save(cash);
    notices.post(a.userId,"Advance repayment","$"+repay+" repaid from your simulated direct deposit.");
   }
  }
  return d;
 }
 @Transactional CashAdvance advance(Long userId,Long accountId){
  BankAccount a=ledgers.account(accountId);
  if(!a.userId.equals(userId) || !"ACTIVE".equals(a.status) || !"CHECKING".equals(a.accountType))throw new IllegalArgumentException("Only your active checking account is eligible");
  if(advances.existsByAccountIdAndStatus(a.id,"OUTSTANDING"))throw new IllegalArgumentException("Existing advance must be repaid");
  // Require four payroll records in separate recent ISO calendar weeks, within six weeks.
  List<DirectDeposit> deposits=payroll.findByAccountIdOrderByIdDesc(a.id);
  java.time.temporal.WeekFields wf=java.time.temporal.WeekFields.ISO;
  Set<String> uniqueWeeks=new HashSet<>();
  for(DirectDeposit d:deposits){if(d.createdAt!=null && d.createdAt.isAfter(LocalDateTime.now().minusWeeks(6)))
   uniqueWeeks.add(d.createdAt.getYear()+"-"+d.createdAt.get(wf.weekOfWeekBasedYear()));}
  if(uniqueWeeks.size()<4)throw new IllegalArgumentException("Requires payroll deposits in at least 4 recent weeks");
  CashAdvance c=new CashAdvance();c.accountId=a.id;c.amount=new BigDecimal("100.00");c.status="OUTSTANDING";advances.save(c);
  ledgers.credit(a,c.amount,"ADVANCE:"+c.id,"Bank-funded simulated early cash advance","HOUSE:ADVANCE_RECEIVABLE");
  notices.post(userId,"$100 advance approved","Your simulated $100 advance was credited. Repayment is due from your next qualifying payroll deposit.");
  audit.record(userId,"ADVANCE_GRANTED","ADVANCE",c.id);return c;
 }
 @Transactional BillPayment bill(Long userId,Long accountId,String biller,String reference,BigDecimal amount,LocalDate date,String requestKey){
  amount=Ledgers.money(amount);
  if(accountId==null||date==null)throw new IllegalArgumentException("Account and payment date required");
  BankAccount a=accounts.findById(accountId).orElseThrow();
  if(!a.userId.equals(userId)||!"ACTIVE".equals(a.status))throw new org.springframework.security.access.AccessDeniedException("Not your account");
  if(biller==null||biller.isBlank()||biller.length()>160||reference==null||reference.isBlank()||reference.length()>80)throw new IllegalArgumentException("Biller and reference required (reference maximum 80 characters)");
  if(date.isBefore(LocalDate.now())||date.isAfter(LocalDate.now().plusYears(1)))throw new IllegalArgumentException("Invalid payment date");
  if(requestKey==null||!requestKey.matches("[A-Za-z0-9:_-]{8,90}"))throw new IllegalArgumentException("Unique request key required");
  Optional<BillPayment> prior=bills.findByRequestKey(requestKey);
  if(prior.isPresent()){
   BillPayment old=prior.get();
   if(!old.accountId.equals(accountId)||!old.biller.equals(biller)||!old.reference.equals(reference)||old.amount.compareTo(amount)!=0||!old.scheduledOn.equals(date))throw new IllegalArgumentException("Idempotency key already used for a different payment");
   return old;
  }
  BillPayment b=new BillPayment();b.accountId=a.id;b.biller=biller;b.reference=reference;b.amount=Ledgers.money(amount);b.scheduledOn=date;b.status="SCHEDULED";b.requestKey=requestKey;bills.save(b);
  notices.post(userId,"Bill payment scheduled","$"+amount+" payment to "+biller+" scheduled for "+date);
  audit.record(userId,"BILL_SCHEDULED","BILL",b.id);return b;
 }
 @Transactional void executeBill(Long billId){
  BillPayment b=bills.lock(billId).orElseThrow();if(!"SCHEDULED".equals(b.status))return;
  BankAccount a=ledgers.account(b.accountId);
  if(a.balance.compareTo(b.amount)<0||!"ACTIVE".equals(a.status)){
   b.status="FAILED";bills.save(b);notices.post(a.userId,"Bill payment failed","Insufficient funds or inactive account for "+b.biller);return;
  }
  ledgers.debit(a,b.amount,"BILL:"+b.id,"Simulated bill payment to "+b.biller,"HOUSE:BILLER_SETTLEMENT");
  b.status="COMPLETED";bills.save(b);notices.post(a.userId,"Bill payment completed","Paid $"+b.amount+" to fictional biller "+b.biller);
  if(!"ONCE".equals(b.frequency)){
   BillPayment next=new BillPayment();next.accountId=b.accountId;next.biller=b.biller;next.reference=b.reference;next.amount=b.amount;
   next.frequency=b.frequency;next.parentId=b.parentId==null?b.id:b.parentId;next.scheduledOn=CustomerServices.next(b.scheduledOn,b.frequency);next.status="SCHEDULED";
   next.requestKey="AUTOPAY:"+next.parentId+":"+next.scheduledOn;bills.save(next);
  }
 }
}
