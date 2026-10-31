package com.atlas.banking;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/api/customer/services") @PreAuthorize("hasRole('CUSTOMER')")
class CustomerServicesController {
 final CustomerServices s;final Billers billers;final Products products;final Bills bills;
 CustomerServicesController(CustomerServices s,Billers b,Products p,Bills bills){this.s=s;billers=b;products=p;this.bills=bills;}
 Long uid(Authentication a){return s.policy().user(a.getName()).id;}
 @GetMapping("/products") List<BankingProduct> products(){return products.findAll().stream().filter(p->p.enabled).toList();}
 @GetMapping("/cards") List<BankCard> cards(Authentication a){return s.cards().findByUserId(uid(a));}
 record CardUpdate(String action,String pin,BigDecimal spendingLimit,BigDecimal atmLimit,Boolean onlineEnabled,Boolean internationalEnabled){}
 @PostMapping("/cards/{id}") BankCard card(@PathVariable Long id,@RequestBody CardUpdate r,Authentication a){return s.card(a.getName(),id,r.action(),r.pin(),r.spendingLimit(),r.atmLimit(),r.onlineEnabled(),r.internationalEnabled());}
 @GetMapping("/requests") List<ServiceRequest> requests(Authentication a){return s.requests().findByUserIdOrderByIdDesc(uid(a));}
 record Request(Long accountId,String kind,String details){}
 @PostMapping("/requests") ServiceRequest request(@RequestBody Request r,Authentication a){return s.request(a.getName(),r.accountId(),r.kind(),r.details());}
 @GetMapping("/messages") List<SupportMessage> messages(Authentication a){return s.messages().findByCustomerIdOrderByIdDesc(uid(a));}
 record Message(String subject,String body){}
 @PostMapping("/messages") SupportMessage message(@RequestBody Message r,Authentication a){return s.message(a.getName(),uid(a),r.subject(),r.body());}
 @GetMapping("/settings") CustomerSettings settings(Authentication a){CustomerSettings x=s.settings().findByUserId(uid(a)).orElseGet(CustomerSettings::new);x.userId=uid(a);return x;}
 record SettingsUpdate(String language,boolean accessibility,boolean marketing){}
 @PutMapping("/settings") CustomerSettings settings(@RequestBody SettingsUpdate r,Authentication a){return s.settings(a.getName(),r.language(),r.accessibility(),r.marketing());}
 @GetMapping("/billers") List<SavedBiller> billers(Authentication a){return billers.findByUserId(uid(a));}
 record Biller(String name,String reference){}
 @PostMapping("/billers") @Transactional SavedBiller biller(@RequestBody Biller r,Authentication a){SavedBiller b=new SavedBiller();b.userId=uid(a);b.name=CustomerServices.text(r.name(),160);b.reference=CustomerServices.text(r.reference(),160);return billers.save(b);}
 @DeleteMapping("/billers/{id}") @Transactional void deleteBiller(@PathVariable Long id,Authentication a){SavedBiller b=billers.findById(id).orElseThrow(()->new IllegalArgumentException("Biller not found"));CustomerServices.owner(uid(a),b.userId);billers.delete(b);}
 @GetMapping("/schedules") List<TransferSchedule> schedules(Authentication a){return s.schedules().findByUserIdOrderByIdDesc(uid(a));}
 record Schedule(Long fromAccountId,Long toAccountId,BigDecimal amount,LocalDate nextDate,String frequency){}
 @PostMapping("/schedules") TransferSchedule schedule(@RequestBody Schedule r,Authentication a){return s.schedule(a.getName(),r.fromAccountId(),r.toAccountId(),r.amount(),r.nextDate(),r.frequency());}
 @DeleteMapping("/schedules/{id}") @Transactional void cancelSchedule(@PathVariable Long id,Authentication a){TransferSchedule x=s.schedules().lock(id).orElseThrow(()->new IllegalArgumentException("Schedule not found"));CustomerServices.owner(uid(a),x.userId);if(!"SCHEDULED".equals(x.status))throw new IllegalArgumentException("Only pending schedules can be cancelled");x.status="CANCELLED";s.schedules().save(x);s.audit().record(uid(a),"SCHEDULE_CANCELLED","SCHEDULE",id);}
 @DeleteMapping("/bills/{id}") @Transactional void cancelBill(@PathVariable Long id,Authentication a){BillPayment b=bills.lock(id).orElseThrow(()->new IllegalArgumentException("Payment not found"));s.policy().own(a.getName(),b.accountId);if(!"SCHEDULED".equals(b.status))throw new IllegalArgumentException("Only pending payments can be cancelled");b.status="CANCELLED";bills.save(b);s.audit().record(uid(a),"BILL_CANCELLED","BILL",id);}
 @GetMapping("/branches") List<Map<String,String>> branches(){return List.of(Map.of("name","Uchit Shikkha Bank fictional branch","address","100 Simulation Way, Test City","hours","Mon-Fri 9am-5pm","note","Simulation location; no physical banking services"));}
}

@RestController @RequestMapping("/api/employee/services") @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
class StaffServicesController {
 final CustomerServices s;
 StaffServicesController(CustomerServices s){this.s=s;}
 @GetMapping("/requests") List<ServiceRequest> requests(Authentication a){BankUser u=s.policy().user(a.getName());return s.requests().findByStatusOrderByIdDesc("PENDING").stream().filter(r->"ADMIN".equals(u.role)||assigned(a.getName(),r.userId)).toList();}
 boolean assigned(String email,Long id){try{s.policy().assigned(email,id);return true;}catch(org.springframework.security.access.AccessDeniedException ex){return false;}}
 record Review(boolean approve,String notes){}
 @PostMapping("/requests/{id}/review") ServiceRequest review(@PathVariable Long id,@RequestBody Review r,Authentication a){return s.review(a.getName(),id,r.approve(),r.notes());}
 @GetMapping("/messages/{customerId}") List<SupportMessage> messages(@PathVariable Long customerId,Authentication a){s.policy().assigned(a.getName(),customerId);return s.messages().findByCustomerIdOrderByIdDesc(customerId);}
 @PostMapping("/messages/{customerId}") SupportMessage message(@PathVariable Long customerId,@RequestBody CustomerServicesController.Message r,Authentication a){return s.message(a.getName(),customerId,r.subject(),r.body());}
}

@RestController @RequestMapping("/api/admin/products") @PreAuthorize("hasRole('ADMIN')")
class ProductController {
 final Products products;final AuditService audit;final Policy policy;
 ProductController(Products p,AuditService a,Policy policy){products=p;audit=a;this.policy=policy;}
 @GetMapping List<BankingProduct> all(){return products.findAll();}
 record Update(boolean enabled){}
 @PatchMapping("/{id}") @Transactional BankingProduct update(@PathVariable Long id,@RequestBody Update r,Authentication a){BankingProduct p=products.findById(id).orElseThrow(()->new IllegalArgumentException("Product not found"));if(r.enabled()&&!Set.of("CHECKING","SAVINGS","BUSINESS_CHECKING","BUSINESS_SAVINGS","MONEY_MARKET","CD").contains(p.code))throw new IllegalArgumentException("Implement the specialist credit or joint-owner workflow before enabling this product");p.enabled=r.enabled();products.save(p);audit.record(policy.user(a.getName()).id,"PRODUCT_UPDATED","PRODUCT",id);return p;}
}
