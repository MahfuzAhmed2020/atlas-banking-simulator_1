package com.atlas.banking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;

@Tag(name="Authentication and MFA")
@RestController @RequestMapping("/api/auth")
class AuthController {
 private final Users users;private final PasswordEncoder encoder;private final CodesService codes;private final Referrals referrals;private final AuditService audit;
 AuthController(Users users,PasswordEncoder encoder,CodesService codes,Referrals referrals,AuditService audit){this.users=users;this.encoder=encoder;this.codes=codes;this.referrals=referrals;this.audit=audit;}
 record Registration(@NotBlank String fullName,@Email @NotBlank String email,@NotBlank String password,String phone,
  String street,String city,String stateCode,String zipCode,String referralCode) {}
 record OtpRequest(@Email String email,@NotBlank String code) {}
 record PasswordRequest(@Email String email) {}
 record PasswordReset(@Email String email,@NotBlank String code,@NotBlank String newPassword) {}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken csrf){return Map.of("token",csrf.getToken(),"header",csrf.getHeaderName());}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/register") @Transactional Map<String,Object> register(@Valid @RequestBody Registration r){
  if(users.findByEmailIgnoreCase(r.email()).isPresent())throw new IllegalArgumentException("Email already registered");
  if(r.password().length()<12 || !r.password().matches(".*[A-Z].*") || !r.password().matches(".*[0-9].*") || !r.password().matches(".*[^a-zA-Z0-9].*"))
    throw new IllegalArgumentException("Password requires 12+ characters, uppercase, number and symbol");
  BankUser u=new BankUser();u.email=r.email().trim().toLowerCase(Locale.ROOT);u.passwordHash=encoder.encode(r.password());
  u.fullName=r.fullName().trim();u.role="CUSTOMER";u.phone=r.phone();u.street=r.street();u.city=r.city();u.stateCode=r.stateCode();u.zipCode=r.zipCode();
  u.referralCode="REF"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
  if(r.referralCode()!=null && !r.referralCode().isBlank()) {
    BankUser sponsor=users.findByReferralCode(r.referralCode().trim().toUpperCase(Locale.ROOT))
      .orElseThrow(()->new IllegalArgumentException("Unknown referral code"));u.referredBy=sponsor.id;
  }
  users.save(u);
  if(u.referredBy!=null){Referral referral=new Referral();referral.referrerId=u.referredBy;referral.referredId=u.id;referral.status="REGISTERED";referrals.save(referral);}
  codes.deliver(u.email,"EMAIL");audit.record(u.id,"CUSTOMER_REGISTERED","USER",u.id);
  return Map.of("message","Registration created. Retrieve verification code from MailHog (http://localhost:8025)","customerId",u.id);
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/verify-email") @Transactional Map<String,String> verifyEmail(@Valid @RequestBody OtpRequest req){
  if(!codes.verify(req.email(),"EMAIL",req.code()))throw new IllegalArgumentException("Invalid or expired code");
  BankUser u=users.findByEmailIgnoreCase(req.email()).orElseThrow();u.emailVerified=true;users.save(u);
  return Map.of("message","Email verified. You can now log in.");
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/me") Map<String,Object> me(Authentication authentication,HttpServletRequest req){
  BankUser u=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
  return Map.of("id",u.id,"email",u.email,"fullName",u.fullName,"role",u.role,"mfaVerified",Boolean.TRUE.equals(req.getSession().getAttribute("MFA_OK")),"emailVerified",u.emailVerified,"phoneVerified",u.phoneVerified,"referralCode",u.referralCode);
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/mfa/resend") Map<String,String> resend(Authentication authentication){codes.deliver(authentication.getName(),"LOGIN");return Map.of("message","New verification code sent");}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/mfa/verify") @Transactional Map<String,String> mfa(@RequestBody Map<String,String> body,Authentication authentication,HttpServletRequest request){
  BankUser u=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
  if(!u.emailVerified)throw new IllegalArgumentException("Verify your registered email first");
  if(!codes.verify(u.email,"LOGIN",body.getOrDefault("code","")))throw new IllegalArgumentException("Incorrect or expired code");
  request.getSession().setAttribute("MFA_OK",true);audit.record(u.id,"MFA_LOGIN","USER",u.id);
  return Map.of("message","MFA verified");
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/phone/request") Map<String,String> phoneRequest(Authentication authentication){
  BankUser u=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
  if(u.phone==null||!u.phone.matches("[+0-9() -]{10,20}"))throw new IllegalArgumentException("Enter a properly formatted test phone at registration");
  // Free local simulation: prove possession of email, NOT the mobile phone. Do not mark as real SMS verified.
  codes.deliver(u.email,"PHONE_DEMO");return Map.of("message","Simulated phone-test code sent to MailHog, not an SMS");
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/phone/verify") @Transactional Map<String,String> phoneVerify(Authentication authentication,@RequestBody Map<String,String> body){
  BankUser u=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
  if(!codes.verify(u.email,"PHONE_DEMO",body.getOrDefault("code","")))throw new IllegalArgumentException("Incorrect code");
  u.phoneVerified=true;users.save(u);return Map.of("message","Simulated phone test passed; this does not prove actual phone ownership");
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/password/request") Map<String,String> passwordRequest(@RequestBody PasswordRequest req){
  users.findByEmailIgnoreCase(req.email()).ifPresent(u->codes.deliver(u.email,"PASSWORD"));
  return Map.of("message","If the email is registered, a reset code has been sent.");
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/password/reset") @Transactional Map<String,String> resetPassword(@RequestBody PasswordReset req){
  if(req.newPassword().length()<12 || !req.newPassword().matches(".*[A-Z].*") || !req.newPassword().matches(".*[0-9].*") || !req.newPassword().matches(".*[^a-zA-Z0-9].*"))
    throw new IllegalArgumentException("Password must contain 12+ characters, uppercase, number and symbol");
  if(!codes.verify(req.email(),"PASSWORD",req.code()))throw new IllegalArgumentException("Invalid or expired code");
  BankUser u=users.findByEmailIgnoreCase(req.email()).orElseThrow();u.passwordHash=encoder.encode(req.newPassword());users.save(u);
  audit.record(u.id,"PASSWORD_RESET","USER",u.id);return Map.of("message","Password reset. Please sign in.");
 }
}
@RestControllerAdvice
class BankErrors {
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) ResponseEntity<Map<String,String>> invalid(RuntimeException e){
  return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid request":e.getMessage()));
 }
 @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) ResponseEntity<Map<String,String>> forbidden(Exception e){
  return ResponseEntity.status(403).body(Map.of("error","Not authorized for this operation"));
 }
 @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> validation(Exception e){
  return ResponseEntity.badRequest().body(Map.of("error","Invalid input values"));
 }
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) ResponseEntity<Map<String,String>> duplicate(Exception e){
  return ResponseEntity.status(409).body(Map.of("error","Duplicate or conflicting record"));
 }
}
