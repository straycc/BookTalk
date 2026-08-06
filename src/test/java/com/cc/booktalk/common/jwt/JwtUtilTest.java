package com.cc.booktalk.common.jwt;

import cn.hutool.jwt.JWT;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        JwtUtil jwtUtil = new JwtUtil(properties);
        UserDTO user = UserDTO.builder()
                .id(42L)
                .username("reader")
                .nickname("读者")
                .status(1)
                .role("user")
                .build();

        JWT jwt = jwtUtil.verifyToken(jwtUtil.generateToken(user));
        UserDTO parsed = jwtUtil.parseUserDTO(jwt);

        assertEquals(42L, parsed.getId());
        assertEquals("reader", parsed.getUsername());
        assertEquals("user", parsed.getRole());
    }

    @Test
    void shouldRejectWeakConfiguredSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        assertThrows(IllegalStateException.class, () -> new JwtUtil(properties));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtProperties first = new JwtProperties();
        first.setSecret("11111111111111111111111111111111");
        JwtProperties second = new JwtProperties();
        second.setSecret("22222222222222222222222222222222");
        JwtUtil issuer = new JwtUtil(first);
        JwtUtil verifier = new JwtUtil(second);
        UserDTO user = UserDTO.builder().id(1L).username("u").status(1).role("user").build();

        String token = issuer.generateToken(user);

        assertThrows(BaseException.class, () -> verifier.verifyToken(token));
    }
}
