package com.atlas.banking;

import org.springframework.context.annotation.*;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;

@Configuration
class OpenApiDocumentation {
 static final String[] GROUPS={"Authentication and MFA","Customer Management","Checking and Savings Accounts","Money Transfers","Mobile Check Deposits","Bill Payments","Cash Advances","Referral System","FEMA Disaster Relief","Notifications and Alerts","Employee Management","Administrator Operations"};
 static String category(String path){
  if(path.contains("/auth/"))return GROUPS[0];
  if(path.contains("/fema")||path.contains("/deferrals"))return GROUPS[8];
  if(path.contains("/checks"))return GROUPS[4];
  if(path.contains("/transfers"))return GROUPS[3];
  if(path.contains("/bills"))return GROUPS[5];
  if(path.contains("/advances"))return GROUPS[6];
  if(path.contains("/referrals"))return GROUPS[7];
  if(path.contains("/notifications")||path.contains("/preferences"))return GROUPS[9];
  if(path.contains("/employees")||path.contains("/employee/")||path.endsWith("/assign")||path.contains("/assignments"))return GROUPS[10];
  if(path.contains("/accounts")||path.contains("/applications")||path.contains("/payroll"))return GROUPS[2];
  if(path.contains("/customers")||path.contains("/dashboard"))return GROUPS[1];
  return GROUPS[11];
 }
 @Bean OpenAPI bankingOpenApi(){
  OpenAPI api=new OpenAPI().info(new Info().title("Atlas Banking SIMULATION API").version("0.1.0")
   .description("Fictional funds only. Sign in through the same-origin React app and complete email MFA before opening Swagger. Documentation requires ADMIN; customer operations still require CUSTOMER and employee operations require EMPLOYEE or ADMIN with assignment checks. Swagger uses the existing HttpOnly JSESSIONID cookie. GET /api/auth/csrf refreshes XSRF-TOKEN; Swagger sends X-XSRF-TOKEN for writes. Do not paste session cookies or passwords into Authorize. Lists currently use documented fixed limits; page/size parameters are not supported unless shown."))
   .components(new Components().addSecuritySchemes("session",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID").description("Browser-managed session after login and MFA; cannot be manually set by Swagger.")))
   .addSecurityItem(new SecurityRequirement().addList("session"));
  for(String group:GROUPS)api.addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name(group));
  addSessionEndpoints(api);
  return api;
 }
 @Bean GlobalOperationCustomizer bankingOperations(){return (op,handler)->{
  if(op.getSummary()==null)op.setSummary(handler.getMethod().getName().replaceAll("([a-z])([A-Z])","$1 $2"));
  PreAuthorize rule=handler.getMethodAnnotation(PreAuthorize.class);
  if(rule==null)rule=handler.getBeanType().getAnnotation(PreAuthorize.class);
  boolean publicEndpoint=handler.getBeanType()==StatusController.class || (handler.getBeanType()==AuthController.class && Set.of("csrf","register","verifyEmail","passwordRequest","resetPassword").contains(handler.getMethod().getName()));
  String permission=rule!=null?rule.value():publicEndpoint?"Public endpoint; CSRF still required for writes.":"Authenticated session; authentication endpoints may be used before MFA completion.";
  if(publicEndpoint)op.setSecurity(Collections.emptyList());
  op.addExtension("x-permissions",permission);
  op.setDescription((op.getDescription()==null?"":op.getDescription()+"\n\n")+"Permissions: "+permission+". Ownership/assignment checks apply. Protected banking calls require completed MFA. Validation and business-rule failures return 400; duplicate records return 409. Empty collections are returned as [].");
  for(String code:List.of("400","401","403","409")){
   String reason=switch(code){case "400"->"Invalid request or business rule";case "401"->"Sign in required";case "403"->"Role, ownership, MFA or CSRF check failed";default->"Duplicate or conflicting record";};
   op.getResponses().addApiResponse(code,new ApiResponse().description(reason).content(new Content().addMediaType("application/json",new MediaType().schema(new ObjectSchema().addProperty("error",new StringSchema())).example(Map.of("error",reason)))));
  }
  return op;
 };}

 private static void addSessionEndpoints(OpenAPI api){
  Schema<?> form=new ObjectSchema().addProperty("email",new StringSchema().example("owner@atlas.test")).addProperty("password",new StringSchema().format("password"));form.setRequired(List.of("email","password"));
  io.swagger.v3.oas.models.Operation login=new io.swagger.v3.oas.models.Operation().operationId("sessionLogin").summary("Password login; email MFA required next").tags(List.of(GROUPS[0])).security(Collections.emptyList())
   .description("Spring Security filter endpoint. Requires CSRF. Rotates the session and issues an email code. Complete POST /api/auth/mfa/verify before protected calls. Signing in replaces the existing browser session.")
   .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody().required(true).content(new Content().addMediaType("application/x-www-form-urlencoded",new MediaType().schema(form))))
   .responses(new io.swagger.v3.oas.models.responses.ApiResponses().addApiResponse("200",new ApiResponse().description("Password accepted; MFA pending").content(new Content().addMediaType("application/json",new MediaType().example(Map.of("mfaRequired",true,"message","Enter the emailed code"))))).addApiResponse("401",new ApiResponse().description("Invalid credentials")).addApiResponse("403",new ApiResponse().description("Missing or incorrect CSRF token")));
  api.path("/api/auth/login",new PathItem().post(login));
  api.path("/api/auth/logout",new PathItem().post(new io.swagger.v3.oas.models.Operation().operationId("sessionLogout").summary("Invalidate current session").tags(List.of(GROUPS[0])).description("Requires CSRF; ends Swagger access until next administrator login and MFA.").responses(new io.swagger.v3.oas.models.responses.ApiResponses().addApiResponse("204",new ApiResponse().description("Session invalidated")))));
 }
 private GroupedOpenApi group(int index){String group=GROUPS[index];return GroupedOpenApi.builder().group(group.replace(' ','-')).displayName(group).pathsToMatch("/api/**")
 .addOpenApiCustomizer(api->{if(group.equals(GROUPS[0]))addSessionEndpoints(api);if(api.getPaths()!=null){api.getPaths().entrySet().removeIf(e->!category(e.getKey()).equals(group));api.getPaths().forEach((path,item)->item.readOperations().forEach(op->op.setTags(List.of(group))));}}).build();}
 @Bean GroupedOpenApi apiGroup0(){return group(0);}
 @Bean GroupedOpenApi apiGroup1(){return group(1);}
 @Bean GroupedOpenApi apiGroup2(){return group(2);}
 @Bean GroupedOpenApi apiGroup3(){return group(3);}
 @Bean GroupedOpenApi apiGroup4(){return group(4);}
 @Bean GroupedOpenApi apiGroup5(){return group(5);}
 @Bean GroupedOpenApi apiGroup6(){return group(6);}
 @Bean GroupedOpenApi apiGroup7(){return group(7);}
 @Bean GroupedOpenApi apiGroup8(){return group(8);}
 @Bean GroupedOpenApi apiGroup9(){return group(9);}
 @Bean GroupedOpenApi apiGroup10(){return group(10);}
 @Bean GroupedOpenApi apiGroup11(){return group(11);}
}
