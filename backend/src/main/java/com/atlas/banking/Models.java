package com.atlas.banking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@MappedSuperclass
abstract class BaseRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="users")
class BankUser extends BaseRecord {
 @Column(nullable=false, unique=true) String email;
 @Column(name="password_hash",nullable=false) @com.fasterxml.jackson.annotation.JsonIgnore String passwordHash;
 @Column(name="full_name",nullable=false) String fullName;
 String phone,street,city;
 @Column(name="state_code") String stateCode;
 @Column(name="zip_code") String zipCode;
 @Column(name="county_fips") String countyFips;
 @Column(name="address_verified") boolean addressVerified;
 String role;
 boolean active=true;
 @Column(name="email_verified") boolean emailVerified;
 @Column(name="phone_verified") boolean phoneVerified;
 @Column(name="referral_code",unique=true) String referralCode;
 @Column(name="referred_by") Long referredBy;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="bank_accounts")
class BankAccount extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="account_number",unique=true) String accountNumber;
 @Column(name="account_type") String accountType;
 String status="ACTIVE";
 @Column(precision=18,scale=2) BigDecimal balance=BigDecimal.ZERO;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="account_applications")
class AccountApplication extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="account_type") String accountType;
 String status="PENDING";
 @Column(name="reviewed_by") Long reviewedBy;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="journal_entries")
class JournalEntry extends BaseRecord {
 @Column(name="reference_key",unique=true) String referenceKey;
 String description;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="ledger_postings")
class LedgerPosting extends BaseRecord {
 @Column(name="journal_id") Long journalId;
 @Column(name="ledger_account") String ledgerAccount;
 @Column(precision=18,scale=2) BigDecimal amount;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="check_deposits")
class CheckDeposit extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="account_id") Long accountId;
 @Column(name="check_number") String checkNumber;
 @Column(precision=18,scale=2) BigDecimal amount;
 boolean instant;
 BigDecimal fee=BigDecimal.ZERO;
 String status="SUBMITTED";
 @Column(name="front_path") @com.fasterxml.jackson.annotation.JsonIgnore String frontPath;
 @Column(name="back_path") @com.fasterxml.jackson.annotation.JsonIgnore String backPath;
 @Column(name="image_hash") String imageHash;
 @Column(name="available_on") LocalDate availableOn;
 @Column(name="reviewed_by") Long reviewedBy;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="transfers")
class BankTransfer extends BaseRecord {
 @Column(name="from_account_id") Long fromAccountId;
 @Column(name="to_account_id") Long toAccountId;
 BigDecimal amount;
 String status;
 @Column(name="request_key",unique=true) String requestKey;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="direct_deposits")
class DirectDeposit extends BaseRecord {
 @Column(name="account_id") Long accountId;
 BigDecimal amount;
 String employer;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="cash_advances")
class CashAdvance extends BaseRecord {
 @Column(name="account_id") Long accountId;
 BigDecimal amount;
 BigDecimal repaid=BigDecimal.ZERO;
 String status;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="referrals")
class Referral extends BaseRecord {
 @Column(name="referrer_id") Long referrerId;
 @Column(name="referred_id") Long referredId;
 String status;
 @Column(name="bonus_amount") BigDecimal bonusAmount=new BigDecimal("500.00");
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="bill_payments")
class BillPayment extends BaseRecord {
 String frequency="ONCE";
 @Column(name="parent_id") Long parentId;
 @Column(name="account_id") Long accountId;
 String biller,reference;
 BigDecimal amount;
 @Column(name="scheduled_on") LocalDate scheduledOn;
 String status;
 @Column(name="request_key",unique=true) String requestKey;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="loans")
class BankLoan extends BaseRecord {
 @Column(name="user_id") Long userId;
 String name;
 @Column(name="amount_due") BigDecimal amountDue;
 @Column(name="due_on") LocalDate dueOn;
 String status="ACTIVE";
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="fema_disasters")
class FemaDisaster extends BaseRecord {
 @Column(name="disaster_number") Integer disasterNumber;
 @Column(name="county_fips") String countyFips;
 @Column(name="declaration_date") LocalDate declarationDate;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="deferrals")
class Deferral extends BaseRecord {
 @Column(name="loan_id") Long loanId;
 @Column(name="disaster_number") Integer disasterNumber;
 @Column(name="original_due") LocalDate originalDue;
 @Column(name="revised_due") LocalDate revisedDue;
 String status;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="alert_preferences")
class AlertPreference extends BaseRecord {
 @Column(name="account_id",unique=true) Long accountId;
 @Column(name="low_balance") BigDecimal lowBalance=new BigDecimal("100.00");
 @Column(name="large_transaction") BigDecimal largeTransaction=new BigDecimal("500.00");
 @Column(name="email_enabled") boolean emailEnabled=true;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="notifications")
class BankNotification extends BaseRecord {
 @Column(name="user_id") Long userId;
 String title,message;
 @Column(name="read_flag") boolean readFlag;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="one_time_codes")
class OneTimeCode extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="code_hash") String codeHash;
 String purpose;
 @Column(name="expires_at") LocalDateTime expiresAt;
 int attempts;
 boolean used;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="customer_assignments")
class CustomerAssignment extends BaseRecord {
 @Column(name="employee_id") Long employeeId;
 @Column(name="customer_id") Long customerId;
}
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility=com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@Entity @Table(name="audit_events")
class AuditEvent extends BaseRecord {
 @Column(name="actor_id") Long actorId;
 String action;
 @Column(name="target_type") String targetType;
 @Column(name="target_id") String targetId;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
