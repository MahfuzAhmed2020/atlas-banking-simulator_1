package com.atlas.banking;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:disabled;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=none","app.demo-seed=false","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
class DocumentationDisabledTest {
 @Autowired MockMvc mvc;
 @Test @WithMockUser(roles="ADMIN") void disabledEvenForAdmin() throws Exception {
  mvc.perform(get("/v3/api-docs").sessionAttr("MFA_OK",true)).andExpect(status().isNotFound());
  mvc.perform(get("/swagger-ui/index.html").sessionAttr("MFA_OK",true)).andExpect(status().isNotFound());
 }
}
