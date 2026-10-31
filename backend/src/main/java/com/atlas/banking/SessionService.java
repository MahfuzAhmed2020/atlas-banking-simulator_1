package com.atlas.banking;
import jakarta.persistence.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

@Entity @Table(name="customer_sessions") @com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
class CustomerSession extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="session_hash") @com.fasterxml.jackson.annotation.JsonIgnore String sessionHash;
 @Column(name="device_hash") @com.fasterxml.jackson.annotation.JsonIgnore String deviceHash;
 @Column(name="user_agent") String userAgent;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
 boolean revoked;
}
interface CustomerSessions extends JpaRepository<CustomerSession,Long> {
 Optional<CustomerSession> findBySessionHash(String hash);List<CustomerSession> findByUserIdOrderByIdDesc(Long id);
 boolean existsByUserIdAndDeviceHash(Long userId,String hash);
}
@Service
class SessionService {
 @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie;
 CustomerSessions sessions(){return sessions;} Users users(){return users;} AuditService audit(){return audit;}
 final CustomerSessions sessions;final Users users;final Notices notices;final AuditService audit;
 SessionService(CustomerSessions s,Users u,Notices n,AuditService a){sessions=s;users=u;notices=n;audit=a;}
 static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional void register(BankUser u,HttpServletRequest request,HttpServletResponse response){
  String device=Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0])).filter(c->"ATLAS_DEVICE".equals(c.getName())).map(Cookie::getValue).filter(v->v.matches("[a-zA-Z0-9-]{36}")).findFirst().orElseGet(()->UUID.randomUUID().toString());
  String deviceHash=hash(device);boolean recognized=sessions.existsByUserIdAndDeviceHash(u.id,deviceHash);
  CustomerSession s=new CustomerSession();s.userId=u.id;s.sessionHash=hash(request.getSession().getId());s.deviceHash=deviceHash;
  s.userAgent=Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown browser");if(s.userAgent.length()>240)s.userAgent=s.userAgent.substring(0,240);
  sessions.findBySessionHash(s.sessionHash).ifPresent(old->sessions.delete(old));sessions.save(s);
  response.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from("ATLAS_DEVICE",device).httpOnly(true).secure(secureCookie||request.isSecure()).sameSite("Lax").path("/").maxAge(Duration.ofDays(90)).build().toString());
  if(!recognized)notices.post(u.id,"New device sign-in","A new browser completed MFA. Review your security sessions if this was unexpected.");
  audit.record(u.id,"SESSION_RECOGNIZED","SESSION",s.id);
 }
 boolean allowed(String email,HttpSession session){BankUser u=users.findByEmailIgnoreCase(email).orElse(null);if(u!=null&&!u.active)return false;return session==null||sessions.findBySessionHash(hash(session.getId())).map(s->!s.revoked).orElse(true);}
 @Transactional void revokeAll(Long userId){for(CustomerSession s:sessions.findByUserIdOrderByIdDesc(userId)){s.revoked=true;sessions.save(s);}}
}
@RestController @RequestMapping("/api/auth/sessions")
class SessionController {
 final SessionService service;
 SessionController(SessionService s){service=s;}
 void mfa(HttpServletRequest r){if(!Boolean.TRUE.equals(r.getSession().getAttribute("MFA_OK")))throw new org.springframework.security.access.AccessDeniedException("Complete MFA first");}
 @GetMapping List<CustomerSession> list(Authentication a,HttpServletRequest r){mfa(r);return service.sessions().findByUserIdOrderByIdDesc(service.users().findByEmailIgnoreCase(a.getName()).orElseThrow().id);}
 @DeleteMapping("/{id}") @Transactional void revoke(@PathVariable Long id,Authentication a,HttpServletRequest r){mfa(r);BankUser u=service.users().findByEmailIgnoreCase(a.getName()).orElseThrow();CustomerSession s=service.sessions().findById(id).orElseThrow(()->new IllegalArgumentException("Session not found"));CustomerServices.owner(u.id,s.userId);s.revoked=true;service.sessions().save(s);service.audit().record(u.id,"SESSION_REVOKED","SESSION",id);}
}
