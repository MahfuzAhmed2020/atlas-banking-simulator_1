package com.atlas.banking;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDate;
@Component
class ScheduledTransferJobs {
 final TransferSchedules schedules; final CustomerServices services;
 ScheduledTransferJobs(TransferSchedules s,CustomerServices c){schedules=s;services=c;}
 @Scheduled(fixedDelay=60000) void execute(){
  if(!DepositService.businessDay(LocalDate.now()))return;
  for(TransferSchedule s:schedules.findByStatusAndNextDateLessThanEqual("SCHEDULED",LocalDate.now())){
   try{services.runSchedule(s.id);}catch(Exception ex){services.failSchedule(s.id);}
  }
 }
}
