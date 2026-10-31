package com.atlas.banking;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.*;
import java.util.*;

@Entity @Table(name="authenticator_credentials")
class AuthenticatorCredential extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="encrypted_secret") String encryptedSecret;
 @Column(name="last_step") long lastStep=-1;
 int attempts;
 @Column(name="window_start") long windowStart;
}
interface Authenticators extends org.springframework.data.jpa.repository.JpaRepository<AuthenticatorCredential,Long> {
 boolean existsByUserId(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<AuthenticatorCredential> findByUserId(Long id);
}
@Service
class AuthenticatorService {
 Policy policy(){return policy;}
 final Authenticators credentials;final Policy policy;final AuditService audit;final Notices notices;
 @Value("${app.totp-key:}") String key;
 AuthenticatorService(Authenticators c,Policy p,AuditService a,Notices n){credentials=c;policy=p;audit=a;notices=n;}
 boolean enabled(Long id){return credentials.existsByUserId(id);}
 byte[] key(){try{byte[] bytes=Base64.getDecoder().decode(key);if(bytes.length!=32)throw new IllegalArgumentException();return bytes;}catch(IllegalArgumentException ex){throw new IllegalStateException("Configure ATLAS_TOTP_KEY with a random 32-byte Base64 encryption key before authenticator enrollment");}}
 String encrypt(String secret){try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key(),"AES"),new GCMParameterSpec(128,nonce));byte[] encrypted=cipher.doFinal(secret.getBytes(StandardCharsets.US_ASCII));return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12+encrypted.length).put(nonce).put(encrypted).array());}catch(GeneralSecurityException ex){throw new IllegalStateException("Authenticator encryption failed",ex);}}
 String decrypt(String value){try{byte[] bytes=Base64.getDecoder().decode(value);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key(),"AES"),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.US_ASCII);}catch(GeneralSecurityException ex){throw new IllegalStateException("Authenticator key unavailable or changed",ex);}}
 static String secret(){byte[] b=new byte[20];new SecureRandom().nextBytes(b);String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";StringBuilder s=new StringBuilder();int buffer=0,bits=0;for(byte v:b){buffer=(buffer<<8)|(v&255);bits+=8;while(bits>=5){bits-=5;s.append(alphabet.charAt((buffer>>bits)&31));}}return s.toString();}
 static byte[] decode(String value){ByteArrayOutputStreamHolder holder=new ByteArrayOutputStreamHolder();int buffer=0,bits=0;for(char c:value.toCharArray()){int index="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);if(index<0)throw new IllegalArgumentException("Invalid authenticator secret");buffer=(buffer<<5)|index;bits+=5;if(bits>=8){bits-=8;holder.out.write((buffer>>bits)&255);}}return holder.out.toByteArray();}
 private static class ByteArrayOutputStreamHolder{java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();}
 static String code(String secret,long step){try{Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(decode(secret),"HmacSHA1"));byte[] hash=mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());int offset=hash[19]&15;int number=((hash[offset]&127)<<24)|((hash[offset+1]&255)<<16)|((hash[offset+2]&255)<<8)|(hash[offset+3]&255);return String.format(Locale.ROOT,"%06d",number%1000000);}catch(GeneralSecurityException ex){throw new IllegalStateException(ex);}}
 static long match(String secret,String otp,long now){if(otp==null||!otp.matches("[0-9]{6}"))return -1;for(long step=now/30-1;step<=now/30+1;step++)if(MessageDigest.isEqual(code(secret,step).getBytes(StandardCharsets.US_ASCII),otp.getBytes(StandardCharsets.US_ASCII)))return step;return -1;}
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) boolean verify(Long id,String otp){
  AuthenticatorCredential c=credentials.findByUserId(id).orElseThrow(()->new IllegalArgumentException("Authenticator not enrolled"));long now=Instant.now().getEpochSecond();
  if(now-c.windowStart>=300){c.windowStart=now;c.attempts=0;}if(c.attempts>=5)return false;c.attempts++;
  long step=match(decrypt(c.encryptedSecret),otp,now);if(step<=c.lastStep)return false;c.lastStep=step;c.attempts=0;return true;
 }
 @Transactional void enroll(Long id,String secret,String otp){if(enabled(id))throw new IllegalArgumentException("Authenticator already enabled");long step=match(secret,otp,Instant.now().getEpochSecond());if(step<0)throw new IllegalArgumentException("Invalid authenticator code");AuthenticatorCredential c=new AuthenticatorCredential();c.userId=id;c.encryptedSecret=encrypt(secret);c.lastStep=step;credentials.save(c);audit.record(id,"TOTP_ENABLED","USER",id);notices.post(id,"Authenticator enabled","Your account now requires authenticator codes for login. Keep your authenticator available.");}
}
@RestController @RequestMapping("/api/auth/authenticator")
class AuthenticatorController {
 final AuthenticatorService service;
 AuthenticatorController(AuthenticatorService s){service=s;}
 Long user(Authentication a,HttpServletRequest r){if(!Boolean.TRUE.equals(r.getSession().getAttribute("MFA_OK")))throw new org.springframework.security.access.AccessDeniedException("Complete MFA first");return service.policy().user(a.getName()).id;}
 @GetMapping Map<String,Boolean> status(Authentication a,HttpServletRequest r){return Map.of("enabled",service.enabled(user(a,r)));}
 @PostMapping("/enroll") Map<String,String> enroll(Authentication a,HttpServletRequest r){Long id=user(a,r);service.key();if(service.enabled(id))throw new IllegalArgumentException("Authenticator already enabled");String secret=AuthenticatorService.secret();r.getSession().setAttribute("TOTP_PENDING",secret);r.getSession().setAttribute("TOTP_PENDING_AT",Instant.now().getEpochSecond());r.getSession().setAttribute("TOTP_ATTEMPTS",0);return Map.of("secret",secret,"uri","otpauth://totp/Uchit%20Shikkha%20Bank:"+java.net.URLEncoder.encode(a.getName(),StandardCharsets.UTF_8)+"?secret="+secret+"&issuer=Uchit%20Shikkha%20Bank&algorithm=SHA1&digits=6&period=30");}
 record Code(String code){}
 @PostMapping("/confirm") void confirm(@RequestBody Code code,Authentication a,HttpServletRequest r){Long id=user(a,r);Object created=r.getSession().getAttribute("TOTP_PENDING_AT");Integer attempts=(Integer)r.getSession().getAttribute("TOTP_ATTEMPTS");if(!(created instanceof Long t)||Instant.now().getEpochSecond()-t>300||attempts==null||attempts>=5)throw new IllegalArgumentException("Start a new enrollment");r.getSession().setAttribute("TOTP_ATTEMPTS",attempts+1);service.enroll(id,(String)r.getSession().getAttribute("TOTP_PENDING"),code.code());r.getSession().removeAttribute("TOTP_PENDING");r.getSession().removeAttribute("TOTP_PENDING_AT");}
}
