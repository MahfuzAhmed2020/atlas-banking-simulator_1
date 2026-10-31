package com.atlas.banking;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@RestController @RequestMapping("/api/admin/demo") @PreAuthorize("hasRole('ADMIN')")
class GenerateDemoCustomers {
 private final Users users;private final Accounts accounts;private final Preferences preferences;private final PasswordEncoder encoder;
 private final Ledgers ledger;private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Value("${app.demo-seed:false}") boolean enabled;
 @Value("${app.default-user-password}") String demoPassword;
 GenerateDemoCustomers(Users u,Accounts a,Preferences p,PasswordEncoder pass,Ledgers ledger,org.springframework.jdbc.core.JdbcTemplate jdbc){users=u;accounts=a;preferences=p;encoder=pass;this.ledger=ledger;this.jdbc=jdbc;}
 @PostMapping("/generate") @Transactional Map<String,Object> create(@RequestParam(defaultValue="10") int count){
  if(!enabled)throw new IllegalArgumentException("Demo generation disabled");
  if(count<1||count>1000)throw new IllegalArgumentException("Count must be between 1 and 1000");
  String hash=encoder.encode(demoPassword);int created=0;
  for(int i=0;i<count;i++){
   String email="demo."+UUID.randomUUID().toString().substring(0,12)+"@atlas.test";
   BankUser u=new BankUser();u.email=email;u.fullName="Fictional Bank Customer "+(i+1);
   u.role="CUSTOMER";u.passwordHash=hash;u.emailVerified=true;u.referralCode="GEN"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
   u.city="Example City";u.stateCode="NY";u.zipCode="10001";users.save(u);
   for(String type:List.of("CHECKING","SAVINGS")){
    BankAccount a=new BankAccount();a.userId=u.id;a.accountType=type;a.status="ACTIVE";
    a.accountNumber="SIM"+UUID.randomUUID().toString().replace("-","").substring(0,15).toUpperCase(Locale.ROOT);accounts.save(a);
    AlertPreference p=new AlertPreference();p.accountId=a.id;preferences.save(p);
   }created++;
  }
  return Map.of("created",created,"simulationOnly",true);
 }
 @PostMapping("/payroll-history") @Transactional Map<String,Object> seedPayroll(@RequestParam Long accountId,@RequestParam(defaultValue="800.00") java.math.BigDecimal amount){
  if(!enabled)throw new IllegalArgumentException("Demo generation disabled");
  BankAccount a=ledger.account(accountId);
  if(!"CHECKING".equals(a.accountType))throw new IllegalArgumentException("Checking account required");
  for(int w=3;w>=0;w--){
   String key="DEMO_WEEKLY_PAYROLL:"+accountId+":"+w;
   jdbc.update("INSERT INTO direct_deposits (account_id,amount,employer,created_at) VALUES (?,?,?,?)",
      accountId,Ledgers.money(amount),"Fictional Example Employer",java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusWeeks(w)));
   ledger.credit(a,amount,key,"Historical simulated weekly payroll","HOUSE:PAYROLL_CLEARING");
  }
  return Map.of("weeklyDepositsCreated",4,"eligibleForDemoAdvance",true);
 }

}
