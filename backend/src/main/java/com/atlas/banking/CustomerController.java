package com.atlas.banking;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Tag(name="Customer Management")
@RestController @RequestMapping("/api/customer") @PreAuthorize("hasRole('CUSTOMER')")
class CustomerController {
 private final Users users;private final Accounts accounts;private final Policy policy;private final Applications apps;
 private final BankingWorkflows bank;private final Transfers transfers;private final DirectDeposits payroll;
 private final Advances advances;private final Referrals referrals;private final Bills bills;private final Loans loans;
 private final Deferrals deferrals;private final Preferences prefs;private final Notifications notifications;private final Postings postings;
 private final AuditService audit;
 CustomerController(Users u,Accounts a,Policy p,Applications ap,BankingWorkflows b,Transfers t,DirectDeposits d,Advances adv,Referrals ref,
 Bills bills,Loans loans,Deferrals def,Preferences pref,Notifications n,Postings postings,AuditService audit){
  users=u;accounts=a;policy=p;apps=ap;bank=b;transfers=t;payroll=d;advances=adv;referrals=ref;this.bills=bills;this.loans=loans;deferrals=def;this.prefs=pref;notifications=n;this.postings=postings;this.audit=audit;
 }
 BankUser current(Authentication a){return policy.user(a.getName());}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/dashboard") Map<String,Object> dashboard(Authentication auth){
  BankUser u=current(auth);List<BankAccount> acts=accounts.findByUserId(u.id);
  return Map.of("customer",Map.of("id",u.id,"name",u.fullName,"email",u.email,"referralCode",u.referralCode),
   "accounts",acts,"notifications",notifications.findTop60ByUserIdOrderByIdDesc(u.id),
   "applications",apps.findByUserId(u.id));
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/accounts") List<BankAccount> accounts(Authentication auth){return accounts.findByUserId(current(auth).id);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/applications") @Transactional AccountApplication apply(@RequestBody Map<String,String> body,Authentication auth){return bank.apply(current(auth).id,body.getOrDefault("accountType",""));}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/applications") List<AccountApplication> applications(Authentication auth){return apps.findByUserId(current(auth).id);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/accounts/{id}/postings") List<LedgerPosting> history(@PathVariable Long id,Authentication auth){policy.own(auth.getName(),id);return postings.findTop80ByLedgerAccountOrderByIdDesc(Ledgers.key(id));}

 @Operation(summary="Paged account ledger", description="CUSTOMER and account owner only. Page starts at 0, size is 1?100. Entries sorted newest first. Each signed amount is a ledger posting; no unsupported filter is silently applied.")
 @GetMapping("/accounts/{id}/transactions")
 org.springframework.data.domain.Page<LedgerPosting> transactions(@PathVariable Long id,
  @io.swagger.v3.oas.annotations.Parameter(description="Zero-based page",example="0") @RequestParam(defaultValue="0") int page,
  @io.swagger.v3.oas.annotations.Parameter(description="Page size 1?100",example="20") @RequestParam(defaultValue="20") int size,
  @io.swagger.v3.oas.annotations.Parameter(description="Posting direction",schema=@Schema(allowableValues={"ALL","CREDIT","DEBIT"})) @RequestParam(defaultValue="ALL") String direction,Authentication auth){
  policy.own(auth.getName(),id);if(page<0||size<1||size>100)throw new IllegalArgumentException("page must be nonnegative and size 1?100");
  if(!List.of("ALL","CREDIT","DEBIT").contains(direction))throw new IllegalArgumentException("direction must be ALL, CREDIT or DEBIT");
  return postings.filtered(Ledgers.key(id),direction,org.springframework.data.domain.PageRequest.of(page,size,org.springframework.data.domain.Sort.by("id").descending()));
 }
 @Schema(description="Atomic simulated transfer. Supply a unique requestKey; retry the same request with the same key.")
 record TransferRequest(@NotNull Long fromAccountId,Long toAccountId,String recipientEmailOrPhone,@NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) @Schema(example="50.00", minimum="0.01") BigDecimal amount,@Schema(example="transfer-demo-0001") String requestKey) {}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(examples=@io.swagger.v3.oas.annotations.media.ExampleObject(value="{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":50.00,\"requestKey\":\"swagger-transfer-0001\"}")))
 @PostMapping("/transfers") BankTransfer transfer(@jakarta.validation.Valid @RequestBody TransferRequest req,Authentication auth){
  BankUser user=current(auth);policy.own(auth.getName(),req.fromAccountId());
  Long target=req.toAccountId();
  if(target==null){
   String destination=req.recipientEmailOrPhone();
   if(destination==null||destination.isBlank())throw new IllegalArgumentException("Recipient account or verified email/phone required");
   BankUser recipient=users.findByEmailIgnoreCase(destination).or(()->users.findByPhone(destination)).orElseThrow(()->new IllegalArgumentException("Recipient not found"));
   if(!recipient.emailVerified || (!destination.contains("@") && !recipient.phoneVerified))throw new IllegalArgumentException("Recipient identifier is not verified for this simulation");
   target=accounts.findByUserIdAndStatus(recipient.id,"ACTIVE").stream().filter(a->"CHECKING".equals(a.accountType)).map(a->a.id).findFirst()
    .orElseThrow(()->new IllegalArgumentException("Recipient has no eligible checking account"));
  }
  return bank.transfer(req.fromAccountId(),target,req.amount(),req.requestKey(),user.id);
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/transfers") List<BankTransfer> transfers(@RequestParam Long accountId,Authentication auth){policy.own(auth.getName(),accountId);return transfers.findByFromAccountIdOrToAccountIdOrderByIdDesc(accountId,accountId);}
 record BillRequest(Long accountId,String biller,String reference,BigDecimal amount,LocalDate scheduledOn,String requestKey){}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/bills") BillPayment bill(@RequestBody BillRequest req,Authentication auth){return bank.bill(current(auth).id,req.accountId(),req.biller(),req.reference(),req.amount(),req.scheduledOn(),req.requestKey());}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/bills") List<BillPayment> bills(@RequestParam Long accountId,Authentication auth){policy.own(auth.getName(),accountId);return bills.findByAccountId(accountId);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/payroll") List<DirectDeposit> payroll(@RequestParam Long accountId,Authentication auth){policy.own(auth.getName(),accountId);return payroll.findByAccountIdOrderByIdDesc(accountId);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/advances") CashAdvance cashAdvance(@RequestBody Map<String,Long> payload,Authentication auth){return bank.advance(current(auth).id,payload.get("accountId"));}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/advances") List<CashAdvance> advances(@RequestParam Long accountId,Authentication auth){policy.own(auth.getName(),accountId);return advances.findByAccountId(accountId);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/referrals") Map<String,Object> referrals(Authentication auth){BankUser u=current(auth);return Map.of("code",u.referralCode,"history",referrals.findByReferrerId(u.id));}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/loans") List<BankLoan> loans(Authentication auth){return loans.findByUserId(current(auth).id);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/deferrals") List<Deferral> deferrals(Authentication auth){
  return loans.findByUserId(current(auth).id).stream().flatMap(loan->deferrals.findByLoanId(loan.id).stream()).toList();
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/preferences") List<AlertPreference> preferences(Authentication auth){
  return accounts.findByUserId(current(auth).id).stream().map(a->prefs.findByAccountId(a.id).orElse(null)).filter(Objects::nonNull).toList();
 }
 record AlertUpdate(BigDecimal lowBalance,BigDecimal largeTransaction,boolean emailEnabled) {}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PutMapping("/preferences/{accountId}") @Transactional AlertPreference alerts(@PathVariable Long accountId,@RequestBody AlertUpdate req,Authentication auth){
  BankAccount a=policy.own(auth.getName(),accountId);AlertPreference p=prefs.findByAccountId(a.id).orElseThrow();
  if(req.lowBalance()==null||req.lowBalance().signum()<0||req.largeTransaction()==null||req.largeTransaction().signum()<0)
   throw new IllegalArgumentException("Alert thresholds cannot be negative");
  p.lowBalance=req.lowBalance();p.largeTransaction=req.largeTransaction();p.emailEnabled=req.emailEnabled();prefs.save(p);
  audit.record(current(auth).id,"ALERTS_UPDATED","ACCOUNT",a.id);return p;
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/notifications") List<BankNotification> notifications(Authentication auth){return notifications.findTop60ByUserIdOrderByIdDesc(current(auth).id);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping("/notifications/{id}/read") @Transactional void read(@PathVariable Long id,Authentication auth){
  BankNotification n=notifications.findById(id).orElseThrow();if(!n.userId.equals(current(auth).id))throw new org.springframework.security.access.AccessDeniedException("Not your notification");n.readFlag=true;notifications.save(n);
 }
}
