package com.library.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordUtilTest {

    @Test
    public void hash_producesDifferentHashEachTime() {
        String h1 = PasswordUtil.hash("secret123");
        String h2 = PasswordUtil.hash("secret123");
        assertNotEquals(h1, h2); // salted -> different every time
    }

    @Test
    public void verify_succeedsForCorrectPassword() {
        String hash = PasswordUtil.hash("mySecret1");
        assertTrue(PasswordUtil.verify("mySecret1", hash));
    }

    @Test
    public void verify_failsForWrongPassword() {
        String hash = PasswordUtil.hash("mySecret1");
        assertFalse(PasswordUtil.verify("wrongPassword", hash));
    }
}
