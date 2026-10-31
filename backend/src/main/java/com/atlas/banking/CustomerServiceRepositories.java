package com.atlas.banking;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.LocalDate;

interface Settings extends JpaRepository<CustomerSettings,Long> {Optional<CustomerSettings> findByUserId(Long userId);}
interface Cards extends JpaRepository<BankCard,Long> {
 List<BankCard> findByUserId(Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from BankCard c where c.id=:id") Optional<BankCard> lock(Long id);
}
interface ServiceRequests extends JpaRepository<ServiceRequest,Long> {
 List<ServiceRequest> findByUserIdOrderByIdDesc(Long userId);
 List<ServiceRequest> findByStatusOrderByIdDesc(String status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ServiceRequest r where r.id=:id") Optional<ServiceRequest> lock(Long id);
}
interface SupportMessages extends JpaRepository<SupportMessage,Long> {List<SupportMessage> findByCustomerIdOrderByIdDesc(Long customerId);}
interface Billers extends JpaRepository<SavedBiller,Long> {List<SavedBiller> findByUserId(Long userId);}
interface TransferSchedules extends JpaRepository<TransferSchedule,Long> {
 List<TransferSchedule> findByUserIdOrderByIdDesc(Long userId);
 List<TransferSchedule> findByStatusAndNextDateLessThanEqual(String status,LocalDate date);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from TransferSchedule s where s.id=:id") Optional<TransferSchedule> lock(Long id);
}
interface Products extends JpaRepository<BankingProduct,Long> {Optional<BankingProduct> findByCode(String code);}
interface Documents extends JpaRepository<BankDocument,Long> {
 List<BankDocument> findByUserIdOrderByIdDesc(Long userId);
 Optional<BankDocument> findByAccountIdAndKindAndPeriodStartAndPeriodEnd(Long accountId,String kind,LocalDate start,LocalDate end);
}
interface Deliveries extends JpaRepository<DocumentDelivery,Long> {List<DocumentDelivery> findByDocumentIdOrderByIdDesc(Long documentId);}
