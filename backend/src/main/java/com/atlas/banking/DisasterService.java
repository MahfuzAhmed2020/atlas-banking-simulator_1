package com.atlas.banking;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import java.time.*;
import java.util.*;

@Service
class DisasterService {
 private final Disasters disasters;private final Deferrals deferrals;private final Loans loans;private final Users users;
 private final Notices notices;private final AuditService audits;
 private final RestClient client=RestClient.builder().baseUrl("https://www.fema.gov").build();
 DisasterService(Disasters d,Deferrals def,Loans l,Users u,Notices n,AuditService a){disasters=d;deferrals=def;loans=l;users=u;notices=n;audits=a;}
 @Transactional int synchronize(){
  // Public FEMA dataset: disaster-level county FIPS fields. ZIP-only matches are insufficient.
  String path="/api/open/v2/DisasterDeclarationsSummaries?$top=1000&$orderby=declarationDate%20desc";
  JsonNode root=client.get().uri(path).retrieve().body(JsonNode.class);
  if(root==null||!root.has("DisasterDeclarationsSummaries"))throw new IllegalStateException("Unexpected FEMA response");
  int newRows=0;
  for(JsonNode row:root.get("DisasterDeclarationsSummaries")){
   try{
    int number=row.path("disasterNumber").asInt(0);
    String state=row.path("fipsStateCode").asText("").replaceAll("[^0-9]","");
    String county=row.path("fipsCountyCode").asText("").replaceAll("[^0-9]","");
    if(number<1||state.isBlank()||county.isBlank())continue;
    String fips=String.format("%02d%03d",Integer.parseInt(state),Integer.parseInt(county));
    if(!fips.matches("[0-9]{5}")||disasters.existsByDisasterNumberAndCountyFips(number,fips))continue;
    String dateText=row.path("declarationDate").asText("");
    LocalDate date=LocalDate.parse(dateText.substring(0,10));
    FemaDisaster f=new FemaDisaster();f.disasterNumber=number;f.countyFips=fips;f.declarationDate=date;
    disasters.save(f);newRows++;
   }catch(RuntimeException ignored){/* Some FEMA records may omit county-level identifiers. */}
  }
  return newRows;
 }
 @Transactional int applyRule(){
  int applied=0;
  for(BankLoan loan:loans.findAll()){
   if(!"ACTIVE".equals(loan.status)||loan.dueOn.isBefore(LocalDate.now()))continue;
   BankUser customer=users.findById(loan.userId).orElseThrow();
   // Production would use verified geocoding and additional policy/compliance checks.
   if(!customer.addressVerified||customer.countyFips==null)continue;
   if(!deferrals.findByLoanId(loan.id).isEmpty())continue; // one deferral per loan in this demo
   for(FemaDisaster f:disasters.findByCountyFips(customer.countyFips)){
    if(f.declarationDate.isBefore(LocalDate.now().minusDays(90))||f.declarationDate.isAfter(LocalDate.now()))continue;
    if(deferrals.existsByLoanIdAndDisasterNumber(loan.id,f.disasterNumber))continue;
    Deferral decision=new Deferral();decision.loanId=loan.id;decision.disasterNumber=f.disasterNumber;
    decision.originalDue=loan.dueOn;decision.revisedDue=loan.dueOn.plusDays(30);decision.status="APPROVED";
    deferrals.save(decision);loan.dueOn=decision.revisedDue;loans.save(loan);
    notices.post(customer.id,"30-day disaster payment deferral","Your simulated loan "+loan.name+" was deferred to "+loan.dueOn+" under demo bank policy; FEMA declaration #"+f.disasterNumber+".");
    audits.record(null,"FEMA_30_DAY_DEFERRAL","LOAN",loan.id);applied++;break;
   }
  }
  return applied;
 }
 @Transactional void verifyAddress(Long userId,String countyFips){
  if(countyFips==null||!countyFips.matches("[0-9]{5}"))throw new IllegalArgumentException("Five-digit county FIPS required");
  BankUser u=users.findById(userId).orElseThrow();u.countyFips=countyFips;u.addressVerified=true;users.save(u);
 }
}
@RestController @RequestMapping("/api/admin/fema") @PreAuthorize("hasRole('ADMIN')")
class DisasterController {
 private final DisasterService service;private final Disasters disasters;private final Deferrals deferrals;
 DisasterController(DisasterService s,Disasters d,Deferrals def){service=s;disasters=d;deferrals=def;}
 @PostMapping("/sync") Map<String,Object> sync(){return Map.of("newRecords",service.synchronize());}
 @PostMapping("/evaluate") Map<String,Object> evaluate(){return Map.of("approvedDeferrals",service.applyRule());}
 @PostMapping("/verify-county/{userId}") Map<String,String> verify(@PathVariable Long userId,@RequestBody Map<String,String> body){
  service.verifyAddress(userId,body.get("countyFips"));return Map.of("status","ADDRESS_COUNTY_ADMIN_VERIFIED");
 }
 @GetMapping("/disasters") List<FemaDisaster> disasters(){return disasters.findAll();}
 @GetMapping("/deferrals") List<Deferral> decisions(){return deferrals.findAll();}
}
