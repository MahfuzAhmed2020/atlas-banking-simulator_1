package com.atlas.banking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

interface Users extends JpaRepository<BankUser,Long> {
 Optional<BankUser> findByEmailIgnoreCase(String email);
 Optional<BankUser> findByReferralCode(String referralCode);
 Optional<BankUser> findByPhone(String phone);
 List<BankUser> findByRole(String role);
}
interface Accounts extends JpaRepository<BankAccount,Long> {
 List<BankAccount> findByUserId(Long userId);
 List<BankAccount> findByUserIdAndStatus(Long userId,String status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from BankAccount a where a.id=:id") Optional<BankAccount> lock(@Param("id") Long id);
}
interface Applications extends JpaRepository<AccountApplication,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from AccountApplication a where a.id=:id") Optional<AccountApplication> lock(@Param("id") Long id); List<AccountApplication> findByUserId(Long id); List<AccountApplication> findByStatus(String status); }
interface Journals extends JpaRepository<JournalEntry,Long> { boolean existsByReferenceKey(String key); }
interface Postings extends JpaRepository<LedgerPosting,Long> { @Query("select p from LedgerPosting p where p.ledgerAccount=:account and (:direction='ALL' or (:direction='CREDIT' and p.amount>0) or (:direction='DEBIT' and p.amount<0))") org.springframework.data.domain.Page<LedgerPosting> filtered(@Param("account") String account,@Param("direction") String direction,org.springframework.data.domain.Pageable pageable); List<LedgerPosting> findTop80ByLedgerAccountOrderByIdDesc(String account); }
interface Checks extends JpaRepository<CheckDeposit,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from CheckDeposit d where d.id=:id") Optional<CheckDeposit> lock(@Param("id") Long id);
 List<CheckDeposit> findByUserIdOrderByIdDesc(Long userId);
 List<CheckDeposit> findByStatus(String status);
 List<CheckDeposit> findByStatusAndAvailableOnLessThanEqual(String status,java.time.LocalDate date);
 boolean existsByAccountIdAndCheckNumber(Long accountId,String checkNumber);
 boolean existsByImageHash(String imageHash);
}
interface Transfers extends JpaRepository<BankTransfer,Long> { Optional<BankTransfer> findByRequestKey(String key); List<BankTransfer> findByFromAccountIdOrToAccountIdOrderByIdDesc(Long from,Long to); }
interface DirectDeposits extends JpaRepository<DirectDeposit,Long> { List<DirectDeposit> findByAccountIdOrderByIdDesc(Long id); }
interface Advances extends JpaRepository<CashAdvance,Long> { boolean existsByAccountIdAndStatus(Long id,String status); List<CashAdvance> findByAccountId(Long id); }
interface Referrals extends JpaRepository<Referral,Long> { Optional<Referral> findByReferredId(Long id); List<Referral> findByReferrerId(Long id); }
interface Bills extends JpaRepository<BillPayment,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from BillPayment b where b.id=:id") Optional<BillPayment> lock(@Param("id") Long id);
 List<BillPayment> findByStatusAndScheduledOnLessThanEqual(String status,java.time.LocalDate date); List<BillPayment> findByAccountId(Long id); Optional<BillPayment> findByRequestKey(String key); }
interface Loans extends JpaRepository<BankLoan,Long> { List<BankLoan> findByUserId(Long id); }
interface Disasters extends JpaRepository<FemaDisaster,Long> { boolean existsByDisasterNumberAndCountyFips(Integer number,String fips); List<FemaDisaster> findByCountyFips(String fips); }
interface Deferrals extends JpaRepository<Deferral,Long> { boolean existsByLoanIdAndDisasterNumber(Long loanId,Integer disaster); List<Deferral> findByLoanId(Long id); }
interface Preferences extends JpaRepository<AlertPreference,Long> { Optional<AlertPreference> findByAccountId(Long id); }
interface Notifications extends JpaRepository<BankNotification,Long> { List<BankNotification> findTop60ByUserIdOrderByIdDesc(Long id); }
interface Codes extends JpaRepository<OneTimeCode,Long> { List<OneTimeCode> findTop10ByUserIdAndPurposeOrderByIdDesc(Long id,String purpose); }
interface Assignments extends JpaRepository<CustomerAssignment,Long> { boolean existsByEmployeeIdAndCustomerId(Long employee,Long customer); List<CustomerAssignment> findByEmployeeId(Long employee); }
interface Audits extends JpaRepository<AuditEvent,Long> { List<AuditEvent> findTop100ByOrderByIdDesc(); }
