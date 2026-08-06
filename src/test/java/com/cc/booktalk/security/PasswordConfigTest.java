package com.cc.booktalk.security;

import com.cc.booktalk.infrastructure.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTest {

    @Test
    void bcryptShouldUseSaltAndVerifyRawPassword() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String first = encoder.encode("BookTalk@123");
        String second = encoder.encode("BookTalk@123");

        assertTrue(encoder.matches("BookTalk@123", first));
        assertFalse(first.equals(second));
    }
}
