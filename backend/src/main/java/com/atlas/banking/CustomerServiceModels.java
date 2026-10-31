package com.atlas.banking;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="customer_settings") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class CustomerSettings extends BaseRecord {
 @Column(name="user_id") Long userId;
 String language="en"; boolean accessibility; boolean marketing;
}
@Entity @Table(name="bank_cards") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class BankCard extends BaseRecord {
 @Column(name="user_id") Long userId; @Column(name="account_id") Long accountId;
 @Column(name="last_four") String lastFour;
 String kind="DEBIT",status="INACTIVE";
 @Column(name="spending_limit") BigDecimal spendingLimit=new BigDecimal("1000.00");
 @Column(name="atm_limit") BigDecimal atmLimit=new BigDecimal("300.00");
 @Column(name="online_enabled") boolean onlineEnabled=true;
 @Column(name="international_enabled") boolean internationalEnabled;
 @Column(name="pin_hash") @JsonIgnore String pinHash;
}
@Entity @Table(name="service_requests") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class ServiceRequest extends BaseRecord {
 @Column(name="user_id") Long userId; @Column(name="account_id") Long accountId;
 String kind,details,status="PENDING";
 @Column(name="review_notes") String reviewNotes;
 @Column(name="reviewed_by") Long reviewedBy;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@Entity @Table(name="support_messages") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class SupportMessage extends BaseRecord {
 @Column(name="customer_id") Long customerId; @Column(name="sender_id") Long senderId;
 String subject,body;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@Entity @Table(name="saved_billers") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class SavedBiller extends BaseRecord {
 @Column(name="user_id") Long userId; String name,reference;
}
@Entity @Table(name="transfer_schedules") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class TransferSchedule extends BaseRecord {
 @Column(name="user_id") Long userId;
 @Column(name="from_account_id") Long fromAccountId;
 @Column(name="to_account_id") Long toAccountId;
 BigDecimal amount;
 @Column(name="next_date") LocalDate nextDate;
 String frequency,status="SCHEDULED";
 @Column(name="last_error") String lastError;
}
@Entity @Table(name="banking_products") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class BankingProduct extends BaseRecord { String code,name; boolean enabled=true; }
@Entity @Table(name="bank_documents") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class BankDocument extends BaseRecord {
 @Column(name="user_id") Long userId; @Column(name="account_id") Long accountId;
 String kind;
 @Column(name="period_start") LocalDate periodStart;
 @Column(name="period_end") LocalDate periodEnd;
 @Column(columnDefinition="MEDIUMTEXT") @JsonIgnore String snapshot;
 @Lob @Column(name="pdf_data",columnDefinition="MEDIUMBLOB") @JsonIgnore byte[] pdfData;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
@Entity @Table(name="document_deliveries") @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
class DocumentDelivery extends BaseRecord {
 @Column(name="document_id") Long documentId; String status;
 @Column(name="created_at",insertable=false,updatable=false) LocalDateTime createdAt;
}
