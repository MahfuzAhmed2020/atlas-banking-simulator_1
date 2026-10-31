package com.atlas.banking;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
@Component
class MonthlyStatementJob {
 final Accounts accounts;final MonthlyStatementGenerator generator;
 MonthlyStatementJob(Accounts a,MonthlyStatementGenerator g){accounts=a;generator=g;}
 @Scheduled(cron="0 0 5 1 * *",zone="America/New_York") void run(){
  YearMonth month=YearMonth.now(ZoneId.of("America/New_York")).minusMonths(1);
  for(BankAccount a:accounts.findAll())try{generator.generate(a.id,month);}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Statement generation failed for account {}",a.id,ex);}
 }
}
@org.springframework.stereotype.Service
class MonthlyStatementGenerator {
 final Accounts accounts;final DocumentService documents;
 MonthlyStatementGenerator(Accounts a,DocumentService d){accounts=a;documents=d;}
 @org.springframework.transaction.annotation.Transactional void generate(Long id,YearMonth month){BankAccount a=accounts.lock(id).orElseThrow();if(a.createdAt!=null&&a.createdAt.toLocalDate().isAfter(month.atEndOfMonth()))return;documents.snapshot(a,"STATEMENT",month.atDay(1),month.atEndOfMonth(),null);}
}
