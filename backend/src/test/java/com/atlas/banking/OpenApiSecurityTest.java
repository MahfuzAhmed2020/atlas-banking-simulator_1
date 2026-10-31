package com.atlas.banking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:docs;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=none", "spring.flyway.enabled=true", "app.demo-seed=false", "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@AutoConfigureMockMvc
class OpenApiSecurityTest {
 @Autowired MockMvc mvc;
 @Test void anonymousCannotReadYaml() throws Exception {mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isUnauthorized());}
 @Test void anonymousCannotReadDocs() throws Exception {mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="CUSTOMER") void customerCannotReadDocs() throws Exception {mvc.perform(get("/v3/api-docs").sessionAttr("MFA_OK",true)).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="ADMIN") void mfaRequired() throws Exception {mvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="ADMIN") void generatedDocsIncludeControllersAndSchemas() throws Exception {
  mvc.perform(get("/v3/api-docs").sessionAttr("MFA_OK",true)).andExpect(status().isOk())
   .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
   .andExpect(jsonPath("$.paths['/api/customer/transfers'].post").exists())
   .andExpect(jsonPath("$.paths['/api/customer/preferences/{accountId}'].put").exists())
   .andExpect(jsonPath("$.components.securitySchemes.session.name").value("JSESSIONID"));
 }
 @Test @WithMockUser(roles="ADMIN") void allGroupsRegistered() throws Exception {
  mvc.perform(get("/v3/api-docs/swagger-config").sessionAttr("MFA_OK",true)).andExpect(status().isOk()).andExpect(jsonPath("$.urls.length()").value(12));
  mvc.perform(get("/v3/api-docs/Money-Transfers").sessionAttr("MFA_OK",true)).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/customer/transfers']").exists()).andExpect(jsonPath("$.paths['/api/auth/me']").doesNotExist());
 }
 @Test @WithMockUser(roles="ADMIN") void writesStillRequireCsrf() throws Exception {mvc.perform(post("/api/admin/assign").sessionAttr("MFA_OK",true).contentType("application/json").content("{}")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="ADMIN") void adminCannotExecuteCustomerOperations() throws Exception {mvc.perform(post("/api/customer/transfers").with(csrf()).sessionAttr("MFA_OK",true).contentType("application/json").content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":10,\"requestKey\":\"test-request-0001\"}")).andExpect(status().isForbidden());}

 @Autowired Users users;
 @Autowired Accounts accounts;
 @Autowired Ledgers ledger;
 @Autowired Postings postings;
 @Autowired BankingWorkflows workflows;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.mail.javamail.JavaMailSender mail;
 BankAccount testAccount(String email){BankUser u=new BankUser();u.email=email;u.fullName="Fictional Test";u.passwordHash="not-a-login-hash";u.role="CUSTOMER";u.referralCode=java.util.UUID.randomUUID().toString().substring(0,12);users.save(u);BankAccount a=new BankAccount();a.userId=u.id;a.accountNumber="SIM"+java.util.UUID.randomUUID().toString().substring(0,15);a.accountType="CHECKING";return accounts.save(a);}
 @Test @org.springframework.transaction.annotation.Transactional void transferPostsBalancedJournalAndRetriesOnce(){
  BankAccount a=testAccount("sender@integration.test"),b=testAccount("recipient@integration.test");
  ledger.credit(a,new java.math.BigDecimal("800.00"),"test-funding","Test funds","HOUSE:TEST");
  BankTransfer first=workflows.transfer(a.id,b.id,new java.math.BigDecimal("200.00"),"integration-transfer-01",a.userId);
  BankTransfer retry=workflows.transfer(a.id,b.id,new java.math.BigDecimal("200.00"),"integration-transfer-01",a.userId);
  org.junit.jupiter.api.Assertions.assertEquals(first.id,retry.id);
  org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("600.00"),accounts.findById(a.id).orElseThrow().balance);
  org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("200.00"),accounts.findById(b.id).orElseThrow().balance);
  org.junit.jupiter.api.Assertions.assertEquals(0,postings.findAll().stream().map(p->p.amount).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).compareTo(java.math.BigDecimal.ZERO));
 }
 @Test @org.springframework.transaction.annotation.Transactional void insufficientFundsDoNotMoveMoney(){
  BankAccount a=testAccount("empty@integration.test"),b=testAccount("target@integration.test");
  org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->workflows.transfer(a.id,b.id,new java.math.BigDecimal("1.00"),"insufficient-funds-test",a.userId));
  org.junit.jupiter.api.Assertions.assertEquals(0,b.balance.signum());
 }
 @Test void serializesAccountsButOmitsPrivateFields() throws Exception {
  BankAccount a=new BankAccount();a.id=42L;a.accountType="CHECKING";
  org.junit.jupiter.api.Assertions.assertEquals(42,mapper.readTree(mapper.writeValueAsString(a)).get("id").asInt());
  BankUser u=new BankUser();u.passwordHash="private-secret";
  org.junit.jupiter.api.Assertions.assertFalse(mapper.writeValueAsString(u).contains("private-secret"));
  CheckDeposit d=new CheckDeposit();d.frontPath="private-file";
  org.junit.jupiter.api.Assertions.assertFalse(mapper.writeValueAsString(d).contains("private-file"));
 }

 @Autowired @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping") org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;
 @Test @WithMockUser(roles="ADMIN") void everyImplementedApiMappingIsDocumented() throws Exception {
  String json=mvc.perform(get("/v3/api-docs").sessionAttr("MFA_OK",true)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  com.fasterxml.jackson.databind.JsonNode paths=mapper.readTree(json).get("paths");
  mappings.getHandlerMethods().forEach((mapping,handler)->{
   if(!handler.getBeanType().getPackageName().equals("com.atlas.banking"))return;
   for(String path:mapping.getPatternValues())if(path.startsWith("/api/"))for(var method:mapping.getMethodsCondition().getMethods())
    org.junit.jupiter.api.Assertions.assertTrue(paths.path(path).has(method.name().toLowerCase()),"Undocumented: "+method+" "+path);
  });
  org.junit.jupiter.api.Assertions.assertTrue(paths.path("/api/auth/login").has("post"));
 }

 @Test @WithMockUser(roles="ADMIN") void documentationMatchesProjectVersionAndAuthenticationRules() throws Exception {
  mvc.perform(get("/v3/api-docs").sessionAttr("MFA_OK",true)).andExpect(status().isOk())
   .andExpect(jsonPath("$.info.version").value("0.1.0"))
   .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").isEmpty())
   .andExpect(jsonPath("$.paths['/api/auth/csrf'].get.security").isEmpty())
   .andExpect(jsonPath("$.paths['/api/auth/me'].get['x-permissions']").value(org.hamcrest.Matchers.containsString("Authenticated session")));
  mvc.perform(get("/v3/api-docs/Employee-Management").sessionAttr("MFA_OK",true)).andExpect(status().isOk())
   .andExpect(jsonPath("$.paths['/api/admin/assignments/{id}'].delete").exists());
 }
}
