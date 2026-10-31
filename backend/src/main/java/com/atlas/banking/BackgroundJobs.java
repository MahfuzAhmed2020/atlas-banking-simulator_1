package com.atlas.banking;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Component
class BackgroundJobs {
 private final Checks checks;private final DepositService deposits;private final Bills bills;private final BankingWorkflows banking;private final DisasterService disaster;
 @Value("${app.fema.enabled:false}") boolean femaEnabled;
 BackgroundJobs(Checks c,DepositService d,Bills b,BankingWorkflows banking,DisasterService disaster){checks=c;deposits=d;bills=b;this.banking=banking;this.disaster=disaster;}
 @Scheduled(fixedDelay=60000)
 void processDueItems(){
  if(!DepositService.businessDay(LocalDate.now()))return;
  for(CheckDeposit check:checks.findByStatusAndAvailableOnLessThanEqual("APPROVED_HOLD",LocalDate.now())){
   try{deposits.settle(check);}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Check settlement exception ID {}",check.id,ex);}
  }
  for(BillPayment bill:bills.findByStatusAndScheduledOnLessThanEqual("SCHEDULED",LocalDate.now())){
   try{banking.executeBill(bill.id);}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Bill payment exception ID {}",bill.id,ex);}
  }
 }
 @Scheduled(cron="0 0 6 * * *",zone="America/New_York")
 void femaDaily(){
  if(!femaEnabled)return;
  try{disaster.synchronize();disaster.applyRule();}
  catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("FEMA sync skipped; retry on next run",ex);}
 }
}
