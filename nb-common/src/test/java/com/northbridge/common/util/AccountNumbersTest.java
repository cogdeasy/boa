package com.northbridge.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AccountNumbersTest {

    @Test
    public void masksAllButLastFour() {
        assertEquals("********3455", AccountNumbers.mask("104000123455"));
    }
}
