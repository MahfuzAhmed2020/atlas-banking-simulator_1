package com.atlas.banking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Configuration
class DemoSeed {
 @Bean CommandLineRunner seed(Users users,PasswordEncoder encoder,Accounts accounts,Applications applications,Preferences prefs,
   @Value("${app.demo-seed:false}") boolean enabled,@Value("${app.default-admin-password}") String adminPassword,
   @Value("${app.default-user-password}") String userPassword){
  return args->{
   if(!enabled||users.count()>0)return;
   BankUser owner=make("owner@atlas.test","Bank Owner","ADMIN",adminPassword,encoder);users.save(owner);
   BankUser employee=make("employee@atlas.test","Emily Employee","EMPLOYEE",userPassword,encoder);users.save(employee);
   for(int i=1;i<=3;i++){
    BankUser u=make("customer"+i+"@atlas.test","Demo Customer "+i,"CUSTOMER",userPassword,encoder);
    u.phone="20255501"+String.format("%02d",i);u.city="Testville";u.stateCode="NY";u.zipCode="10001";
    users.save(u);
    BankAccount checking=new BankAccount();checking.userId=u.id;checking.accountType="CHECKING";
    checking.accountNumber="SIMCHECK"+String.format("%07d",i);checking.status="ACTIVE";accounts.save(checking);
    BankAccount savings=new BankAccount();savings.userId=u.id;savings.accountType="SAVINGS";
    savings.accountNumber="SIMSAVE"+String.format("%08d",i);savings.status="ACTIVE";accounts.save(savings);
    for(BankAccount a:List.of(checking,savings)){AlertPreference p=new AlertPreference();p.accountId=a.id;prefs.save(p);}
   }
   org.slf4j.LoggerFactory.getLogger(DemoSeed.class).info("SIMULATION seeded 1 admin, 1 employee and 3 customers; no real money; MFA codes delivered to MailHog.");
  };
 }
 private static BankUser make(String email,String name,String role,String password,PasswordEncoder encoder){
  BankUser u=new BankUser();u.email=email;u.fullName=name;u.role=role;u.passwordHash=encoder.encode(password);u.emailVerified=true;u.phoneVerified=true;
  u.referralCode="SEED"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);return u;
 }
}
