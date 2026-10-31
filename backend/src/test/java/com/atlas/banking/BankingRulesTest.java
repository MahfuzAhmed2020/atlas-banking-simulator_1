package com.atlas.banking;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class BankingRulesTest {
 @Test void acceptsValidMoney(){assertEquals(new BigDecimal("12.50"),Ledgers.money(new BigDecimal("12.5")));}
 @Test void rejectsNegativeAmount(){assertThrows(IllegalArgumentException.class,()->Ledgers.money(new BigDecimal("-1.00")));}
 @Test void rejectsFractionalCent(){assertThrows(IllegalArgumentException.class,()->Ledgers.money(new BigDecimal("1.001")));}
 @Test void weekendMovesToMonday(){assertEquals(LocalDate.of(2026,11,2),DepositService.nextBusiness(LocalDate.of(2026,10,30)));}
 @Test void afterMondayIsTuesday(){assertEquals(LocalDate.of(2026,11,3),DepositService.nextBusiness(LocalDate.of(2026,11,2)));}
}
