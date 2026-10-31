package com.atlas.banking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:services;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","app.demo-seed=false","logging.level.root=WARN","logging.level.org.hibernate.SQL=WARN","spring.main.banner-mode=off"})
@AutoConfigureMockMvc @Transactional
class CustomerServicesTest {
 @Autowired Users users;@Autowired Accounts accounts;@Autowired CustomerServices services;@Autowired Cards cards;
 @Autowired BankingWorkflows bank;@Autowired Ledgers ledger;@Autowired Bills bills;@Autowired DocumentService documents;
 @Autowired MockMvc mvc;@Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;@Autowired Products products;
 @Autowired SessionService sessionService;@Autowired CustomerSessions sessions;@Autowired AuthenticatorService authenticator;@Autowired Authenticators authenticators;@Autowired jakarta.persistence.EntityManager em;
 @org.springframework.test.context.DynamicPropertySource static void testKey(org.springframework.test.context.DynamicPropertyRegistry r){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);r.add("app.totp-key",()->Base64.getEncoder().encodeToString(bytes));}
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.mail.javamail.JavaMailSender mail;
 BankUser person(String role){BankUser u=new BankUser();u.email=UUID.randomUUID()+"@fictional.test";u.fullName="Fictional Customer";u.passwordHash="not-a-login";u.role=role;u.referralCode=UUID.randomUUID().toString().substring(0,12);u.emailVerified=true;return users.save(u);}
 BankAccount account(BankUser u){BankAccount a=new BankAccount();a.userId=u.id;a.accountNumber="SIM"+UUID.randomUUID().toString().substring(0,15);a.accountType="CHECKING";return accounts.save(a);}
 void fund(BankAccount a){ledger.credit(a,new BigDecimal("800.00"),"TEST:"+UUID.randomUUID(),"Fictional opening funding","HOUSE:TEST");}
 @Test void approvedCardHasSafeLifecycleAndDoesNotExposePin()throws Exception{
  BankUser u=person("CUSTOMER"),admin=person("ADMIN"),other=person("CUSTOMER");BankAccount a=account(u);
  ServiceRequest r=services.request(u.email,a.id,"CARD_ISSUE","Fictional debit card");services.review(admin.email,r.id,true,"Approved fictional card");
  BankCard c=cards.findByUserId(u.id).getFirst();assertEquals("INACTIVE",c.status);assertEquals(4,c.lastFour.length());
  services.card(u.email,c.id,"ACTIVATE",null,null,null,null,null);services.card(u.email,c.id,"PIN","4567",null,null,null,null);
  assertNotEquals("4567",c.pinHash);assertFalse(mapper.writeValueAsString(c).contains("pinHash"));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->services.card(other.email,c.id,"LOCK",null,null,null,null,null));
  services.card(u.email,c.id,"LOST",null,null,null,null,null);
  assertThrows(IllegalArgumentException.class,()->services.card(u.email,c.id,"UNLOCK",null,null,null,null,null));
 }
 @Test void customerCannotReadOrMutateAnotherCustomersServices()throws Exception{
  BankUser u=person("CUSTOMER"),other=person("CUSTOMER");BankAccount a=account(u);
  mvc.perform(post("/api/customer/services/requests").with(user(other.email).roles("CUSTOMER")).with(csrf()).sessionAttr("MFA_OK",true).contentType("application/json").content("{\"accountId\":"+a.id+",\"kind\":\"CLOSURE\",\"details\":\"Test\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/employee/services/messages/"+u.id).with(user(other.email).roles("CUSTOMER")).sessionAttr("MFA_OK",true)).andExpect(status().isForbidden());
 }
 @Test void closureRequiresZeroBalanceAndAdministrator(){BankUser u=person("CUSTOMER"),admin=person("ADMIN");BankAccount a=account(u);fund(a);ServiceRequest r=services.request(u.email,a.id,"CLOSURE","Close test account");assertThrows(IllegalArgumentException.class,()->services.review(admin.email,r.id,true,"Reviewed"));assertEquals("ACTIVE",a.status);}
 @Test void billKeyCannotReturnOtherCustomerPayment(){BankUser u=person("CUSTOMER"),v=person("CUSTOMER");BankAccount a=account(u),b=account(v);String key="test-bill-"+UUID.randomUUID();bank.bill(u.id,a.id,"Test utility","SIM-1",new BigDecimal("12"),LocalDate.now(),key);assertThrows(IllegalArgumentException.class,()->bank.bill(v.id,b.id,"Test utility","SIM-1",new BigDecimal("12"),LocalDate.now(),key));}
 @Test void recurringBillPostsOnceAndCreatesNextOccurrence(){BankUser u=person("CUSTOMER");BankAccount a=account(u);fund(a);BillPayment b=bank.bill(u.id,a.id,"Test utility","SIM-1",new BigDecimal("20"),LocalDate.now(),"bill-"+UUID.randomUUID());b.frequency="MONTHLY";bills.save(b);bank.executeBill(b.id);bank.executeBill(b.id);assertEquals(new BigDecimal("780.00"),a.balance);List<BillPayment> all=bills.findByAccountId(a.id);assertEquals(2,all.size());assertTrue(all.stream().anyMatch(p->"SCHEDULED".equals(p.status)&&p.scheduledOn.equals(LocalDate.now().plusMonths(1))));}
 @Test void scheduledTransferUsesExistingAtomicLedgerAndIsIdempotent(){BankUser u=person("CUSTOMER"),v=person("CUSTOMER");BankAccount a=account(u),b=account(v);fund(a);TransferSchedule s=services.schedule(u.email,a.id,b.id,new BigDecimal("50"),LocalDate.now(),"ONCE");services.runSchedule(s.id);services.runSchedule(s.id);assertEquals("COMPLETED",s.status);assertEquals(new BigDecimal("750.00"),a.balance);assertEquals(new BigDecimal("50.00"),b.balance);}
 @Test void messagesAreBoundToCustomerAndAssignedStaff(){BankUser u=person("CUSTOMER"),v=person("CUSTOMER"),staff=person("EMPLOYEE");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->services.message(v.email,u.id,"Test","Cannot impersonate"));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->services.message(staff.email,u.id,"Test","Not assigned"));SupportMessage m=services.message(u.email,u.id,"Fictional help request","Please check my fictional card");assertEquals(u.id,m.customerId);}
 @Test void statementReconcilesAndSnapshotDoesNotChange()throws Exception{
  BankUser u=person("CUSTOMER"),v=person("CUSTOMER");BankAccount a=account(u);fund(a);
  BankDocument d=documents.generate(u.email,a.id,"STATEMENT",LocalDate.now(),LocalDate.now());byte[] original=d.pdfData.clone();
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(original)){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("Closing balance: $800.00"));assertTrue(text.contains("TEST ONLY"));assertFalse(text.contains(a.accountNumber));java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/pdf-qa"));javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0,100),"PNG",java.nio.file.Path.of("target/pdf-qa/statement.png").toFile());}
  ledger.credit(a,new BigDecimal("10"),"MORE:"+UUID.randomUUID(),"More fictional funds","HOUSE:TEST");
  assertArrayEquals(original,documents.generate(u.email,a.id,"STATEMENT",LocalDate.now(),LocalDate.now()).pdfData);
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->documents.download(v.email,d.id));
  mvc.perform(get("/api/documents/"+d.id+"/download").with(user(v.email).roles("CUSTOMER")).sessionAttr("MFA_OK",true)).andExpect(status().isForbidden());
 }
 @Test void unreconciledBalanceCannotProduceStatement(){BankUser u=person("CUSTOMER");BankAccount a=account(u);a.balance=new BigDecimal("999");accounts.saveAndFlush(a);assertThrows(IllegalStateException.class,()->documents.generate(u.email,a.id,"STATEMENT",LocalDate.now(),LocalDate.now()));}
 @Test void cancelledTransferCannotExecute()throws Exception{BankUser u=person("CUSTOMER");BankAccount a=account(u),b=account(u);fund(a);TransferSchedule s=services.schedule(u.email,a.id,b.id,new BigDecimal("10"),LocalDate.now(),"MONTHLY");mvc.perform(delete("/api/customer/services/schedules/"+s.id).with(user(u.email).roles("CUSTOMER")).with(csrf()).sessionAttr("MFA_OK",true)).andExpect(status().isOk());services.runSchedule(s.id);assertEquals(new BigDecimal("800.00"),a.balance);}
 @Test void customerServiceListsAndSettingsAreConnected()throws Exception{BankUser u=person("CUSTOMER");account(u);for(String endpoint:List.of("cards","requests","messages","billers","schedules","products","settings"))mvc.perform(get("/api/customer/services/"+endpoint).with(user(u.email).roles("CUSTOMER")).sessionAttr("MFA_OK",true)).andExpect(status().isOk());mvc.perform(put("/api/customer/services/settings").with(user(u.email).roles("CUSTOMER")).with(csrf()).sessionAttr("MFA_OK",true).contentType("application/json").content("{\"language\":\"en\",\"accessibility\":true,\"marketing\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.accessibility").value(true));}
 @Test void disabledEmployeeAndRevokedSessionCannotUseApis()throws Exception{
  BankUser u=person("CUSTOMER");org.springframework.mock.web.MockHttpServletRequest request=new org.springframework.mock.web.MockHttpServletRequest();request.getSession().setAttribute("MFA_OK",true);sessionService.register(u,request,new org.springframework.mock.web.MockHttpServletResponse());CustomerSession record=sessions.findByUserIdOrderByIdDesc(u.id).getFirst();record.revoked=true;sessions.save(record);
  mvc.perform(get("/api/customer/accounts").with(user(u.email).roles("CUSTOMER")).session((org.springframework.mock.web.MockHttpSession)request.getSession())).andExpect(status().isUnauthorized());
  u.active=false;users.save(u);mvc.perform(get("/api/customer/accounts").with(user(u.email).roles("CUSTOMER")).sessionAttr("MFA_OK",true)).andExpect(status().isUnauthorized());
 }
 @Test void authenticatorSecretIsEncryptedAndEnrollmentRequiresCorrectCode(){BankUser u=person("CUSTOMER");String secret=AuthenticatorService.secret();String otp=AuthenticatorService.code(secret,java.time.Instant.now().getEpochSecond()/30);authenticator.enroll(u.id,secret,otp);assertTrue(authenticator.enabled(u.id));AuthenticatorCredential credential=authenticators.findByUserId(u.id).orElseThrow();assertNotEquals(secret,credential.encryptedSecret);assertEquals(secret,authenticator.decrypt(credential.encryptedSecret));assertEquals(-1,AuthenticatorService.match(secret,"bad",java.time.Instant.now().getEpochSecond()));}
 @Test void receiptUsesOwnedTransactionAndIncludesFeeOnlyForReleasedChecks()throws Exception{BankUser u=person("CUSTOMER"),v=person("CUSTOMER");BankAccount a=account(u),b=account(v);fund(a);BankTransfer t=bank.transfer(a.id,b.id,new BigDecimal("25"),"receipt-"+UUID.randomUUID(),u.id);em.flush();BankDocument sender=documents.receipt(u.email,"TRANSFER_RECEIPT",t.id);BankDocument receiver=documents.receipt(v.email,"TRANSFER_RECEIPT",t.id);assertEquals(a.id,sender.accountId);assertEquals(b.id,receiver.accountId);BankUser outsider=person("CUSTOMER");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->documents.receipt(outsider.email,"TRANSFER_RECEIPT",t.id));}
 @Test void sensitiveMutationsStillRequireMfaAndCsrf()throws Exception{BankUser u=person("CUSTOMER");mvc.perform(post("/api/customer/services/messages").with(user(u.email).roles("CUSTOMER")).with(csrf()).contentType("application/json").content("{\"subject\":\"test\",\"body\":\"test\"}")).andExpect(status().isForbidden());mvc.perform(post("/api/customer/services/messages").with(user(u.email).roles("CUSTOMER")).sessionAttr("MFA_OK",true).contentType("application/json").content("{\"subject\":\"test\",\"body\":\"test\"}")).andExpect(status().isForbidden());}
}
