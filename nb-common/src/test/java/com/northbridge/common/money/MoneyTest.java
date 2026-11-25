package com.northbridge.common.money;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MoneyTest {

    @Test
    public void roundsHalfEvenToCents() {
        assertEquals("10.12", Money.of("10.125").getAmount().toPlainString());
        assertEquals("10.14", Money.of("10.135").getAmount().toPlainString());
    }

    @Test
    public void subtractsIntoNegative() {
        assertTrue(Money.of("5.00").minus(Money.of("7.50")).isNegative());
    }
}
