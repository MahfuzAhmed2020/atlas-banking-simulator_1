package com.atlas.banking;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Tag(name = "Administrator Operations")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController {
  private final Users users;
  private final Accounts accounts;
  private final Applications apps;
  private final BankingWorkflows bank;
  private final Checks checks;
  private final DepositService deposits;
  private final Assignments assigns;
  private final Audits audit;
  private final PasswordEncoder encoder;
  private final Loans loans;
  private final Notices notices;

  AdminController(Users u, Accounts a, Applications ap, BankingWorkflows b, Checks c, DepositService ds, Assignments as,
      Audits audit, PasswordEncoder pass, Loans loans, Notices n) {
    users = u;
    accounts = a;
    apps = ap;
    bank = b;
    checks = c;
    deposits = ds;
    assigns = as;
    this.audit = audit;
    encoder = pass;
    this.loans = loans;
    notices = n;
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/overview")
  Map<String, Object> overview() {
    return Map.of("customers", users.findByRole("CUSTOMER").size(), "employees", users.findByRole("EMPLOYEE").size(),
        "pendingApplications", apps.findByStatus("PENDING").size(), "pendingChecks",
        checks.findByStatus("PENDING_REVIEW").size());
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/customers")
  List<Map<String, Object>> customers() {
    return users.findByRole("CUSTOMER").stream().map(u -> Map.<String, Object>of("id", u.id, "fullName", u.fullName,
        "email", u.email, "verified", u.emailVerified, "active", u.active)).toList();
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/employees")
  List<Map<String, Object>> employees() {
    return users.findByRole("EMPLOYEE").stream()
        .map(u -> Map.<String, Object>of("id", u.id, "name", u.fullName, "email", u.email, "active", u.active))
        .toList();
  }

  @Operation(summary = "Enable or disable an employee", description = "ADMIN only. Cannot change your own privileges or modify customer/admin records.")
  @PatchMapping("/employees/{id}")
  @Transactional
  Map<String, Object> updateEmployee(@PathVariable Long id, @RequestBody Map<String, Boolean> body,
      Authentication auth) {
    BankUser employee = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    if (!"EMPLOYEE".equals(employee.role) || employee.email.equalsIgnoreCase(auth.getName()))
      throw new org.springframework.security.access.AccessDeniedException("Employee management only");
    if (!body.containsKey("active") || body.get("active") == null)
      throw new IllegalArgumentException("active boolean is required");
    employee.active = body.get("active");
    users.save(employee);
    return Map.of("id", id, "active", employee.active);
  }

  @Operation(summary = "Remove an employee/customer assignment", description = "ADMIN only. Retains the employee and customer records.")
  @DeleteMapping("/assignments/{id}")
  @Transactional
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  void removeAssignment(@PathVariable Long id) {
    if (!assigns.existsById(id))
      throw new IllegalArgumentException("Assignment not found");
    assigns.deleteById(id);
  }

  record EmployeeRequest(String fullName, String email, String password) {
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/employees")
  @Transactional
  Map<String, Object> addEmployee(@RequestBody EmployeeRequest req) {
    if (req.password() == null || req.password().length() < 12)
      throw new IllegalArgumentException("Password must have at least 12 characters");
    if (users.findByEmailIgnoreCase(req.email()).isPresent())
      throw new IllegalArgumentException("Email registered");
    BankUser u = new BankUser();
    u.email = req.email().toLowerCase(Locale.ROOT);
    u.fullName = req.fullName();
    u.passwordHash = encoder.encode(req.password());
    u.role = "EMPLOYEE";
    u.emailVerified = true;
    u.referralCode = "EMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    users.save(u);
    return Map.of("employeeId", u.id, "status", "created");
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/assign")
  @Transactional
  Map<String, String> assign(@RequestBody Map<String, Long> payload) {
    Long employee = payload.get("employeeId"), customer = payload.get("customerId");
    if (!"EMPLOYEE".equals(users.findById(employee).orElseThrow().role)
        || !"CUSTOMER".equals(users.findById(customer).orElseThrow().role))
      throw new IllegalArgumentException("Invalid employee/customer pair");
    if (!assigns.existsByEmployeeIdAndCustomerId(employee, customer)) {
      CustomerAssignment a = new CustomerAssignment();
      a.employeeId = employee;
      a.customerId = customer;
      assigns.save(a);
    }
    return Map.of("status", "assigned");
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/applications")
  List<AccountApplication> applications() {
    return apps.findByStatus("PENDING");
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/applications/{id}/approve")
  BankAccount approve(@PathVariable Long id, Authentication auth) {
    return bank.approveApplication(id, users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id);
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/applications/{id}/reject")
  @Transactional
  AccountApplication reject(@PathVariable Long id) {
    AccountApplication ap = apps.findById(id).orElseThrow();
    if (!"PENDING".equals(ap.status))
      throw new IllegalArgumentException("Already reviewed");
    ap.status = "REJECTED";
    apps.save(ap);
    notices.post(ap.userId, "Account application rejected", "Your simulated banking application was not approved.");
    return ap;
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/checks")
  List<CheckDeposit> checks() {
    return checks.findByStatus("PENDING_REVIEW");
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/checks/{id}/review")
  CheckDeposit review(@PathVariable Long id, @RequestBody Map<String, Boolean> data, Authentication auth) {
    return deposits.review(id, users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id,
        Boolean.TRUE.equals(data.get("approve")));
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/checks/{id}/{side}")
  org.springframework.http.ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable String side) {
    if (!List.of("front", "back").contains(side))
      throw new IllegalArgumentException("Invalid side");
    CheckDeposit d = checks.findById(id).orElseThrow();
    byte[] bytes = deposits.load(d, side);
    return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
        .contentType((bytes[0] & 255) == 137 ? org.springframework.http.MediaType.IMAGE_PNG
            : org.springframework.http.MediaType.IMAGE_JPEG)
        .body(bytes);
  }

  record PayrollRequest(Long accountId, BigDecimal amount, String employer) {
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/payroll")
  DirectDeposit payroll(@RequestBody PayrollRequest r, Authentication auth) {
    return bank.payPayroll(r.accountId(), r.amount(), r.employer(),
        users.findByEmailIgnoreCase(auth.getName()).orElseThrow().id);
  }

  record LoanRequest(Long customerId, String name, BigDecimal amountDue, java.time.LocalDate dueOn) {
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/loans")
  @Transactional
  BankLoan createLoan(@RequestBody LoanRequest r) {
    if (!"CUSTOMER".equals(users.findById(r.customerId()).orElseThrow().role))
      throw new IllegalArgumentException("Invalid customer");
    BankLoan loan = new BankLoan();
    loan.userId = r.customerId();
    loan.name = r.name();
    loan.amountDue = Ledgers.money(r.amountDue());
    loan.dueOn = r.dueOn();
    loans.save(loan);
    return loan;
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/audits")
  List<AuditEvent> audit() {
    return audit.findTop100ByOrderByIdDesc();
  }
}

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
class EmployeeController {
  private final Users users;
  private final Assignments assigns;
  private final Policy policy;
  private final Checks checks;
  private final DepositService deposits;

  EmployeeController(Users users, Assignments assigns, Policy p, Checks checks, DepositService ds) {
    this.users = users;
    this.assigns = assigns;
    policy = p;
    this.checks = checks;
    deposits = ds;
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/customers")
  List<Map<String, Object>> myCustomers(Authentication auth) {
    BankUser emp = policy.user(auth.getName());
    return assigns.findByEmployeeId(emp.id).stream().map(as -> users.findById(as.customerId).orElseThrow())
        .map(u -> Map.<String, Object>of("id", u.id, "name", u.fullName, "email", u.email)).toList();
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/checks")
  List<CheckDeposit> queue(Authentication auth) {
    BankUser emp = policy.user(auth.getName());
    if ("ADMIN".equals(emp.role))
      return checks.findByStatus("PENDING_REVIEW");
    Set<Long> customers = new HashSet<>();
    assigns.findByEmployeeId(emp.id).forEach(a -> customers.add(a.customerId));
    return checks.findByStatus("PENDING_REVIEW").stream().filter(d -> customers.contains(d.userId)).toList();
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/checks/{id}/{side}")
  org.springframework.http.ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable String side,
      Authentication auth) {
    if (!List.of("front", "back").contains(side))
      throw new IllegalArgumentException("Invalid side");
    CheckDeposit check = checks.findById(id).orElseThrow();
    policy.assigned(auth.getName(), check.userId);
    byte[] bytes = deposits.load(check, side);
    return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
        .contentType((bytes[0] & 255) == 137 ? org.springframework.http.MediaType.IMAGE_PNG
            : org.springframework.http.MediaType.IMAGE_JPEG)
        .body(bytes);
  }

  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @PostMapping("/checks/{id}/review")
  CheckDeposit review(@PathVariable Long id, @RequestBody Map<String, Boolean> data, Authentication auth) {
    CheckDeposit d = checks.findById(id).orElseThrow();
    policy.assigned(auth.getName(), d.userId);
    return deposits.review(id, policy.user(auth.getName()).id, Boolean.TRUE.equals(data.get("approve")));
  }
}

@RestController
class StatusController {
  @Operation(description = "Uses the application session and enforces route and resource permissions.")
  @ApiResponse(responseCode = "200", description = "Operation completed")
  @GetMapping("/api/health")
  Map<String, String> health() {
    return Map.of("status", "UP", "environment", "SIMULATION_ONLY");
  }

}
