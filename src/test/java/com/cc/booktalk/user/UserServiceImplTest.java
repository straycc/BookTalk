package com.cc.booktalk.user;

import com.cc.booktalk.application.user.service.user.impl.UserServiceImpl;
import com.cc.booktalk.common.jwt.JwtUtil;
import com.cc.booktalk.common.context.UserContext;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.domain.entity.user.User;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.user.UserMapper;
import com.cc.booktalk.interfaces.dto.user.UserLoginDTO;
import com.cc.booktalk.interfaces.dto.user.UserDTO;
import com.cc.booktalk.interfaces.dto.user.UserPasswordChangeDTO;
import com.cc.booktalk.interfaces.vo.user.user.UserLoginVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks private UserServiceImpl userService;
    @Mock private UserMapper userMapper;
    @Mock private UserInfoUserMapper userInfoUserMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> customObjectRedisTemplate;

    @AfterEach
    void clearContext() {
        UserContext.removeUser();
    }

    @Test
    void loginSucceedsWhenUserCacheIsUnavailable() {
        User user = User.builder()
                .id(2L)
                .username("alice")
                .password("$2a$10$hash")
                .status(1)
                .role("user")
                .build();
        UserInfo userInfo = UserInfo.builder().userId(2L).nickname("Alice").build();
        UserLoginDTO request = new UserLoginDTO();
        request.setUsername("alice");
        request.setPassword("BookTalk@123");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(userInfoUserMapper.selectOne(any())).thenReturn(userInfo);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(customObjectRedisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        UserLoginVO result = userService.login(request);

        assertEquals(2L, result.getUserId());
        assertEquals("token", result.getToken());
    }

    @Test
    void changePasswordVerifiesCurrentPasswordAndStoresEncodedPassword() {
        UserContext.saveUser(UserDTO.builder().id(2L).username("alice").build());
        User stored = User.builder().id(2L).password("$2a$10$encoded-old").build();
        UserPasswordChangeDTO request = new UserPasswordChangeDTO();
        request.setCurrentPassword("BookTalk@123");
        request.setNewPassword("BookTalk@456");
        request.setConfirmPassword("BookTalk@456");

        when(userMapper.selectById(2L)).thenReturn(stored);
        when(passwordEncoder.matches("BookTalk@123", "$2a$10$encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("BookTalk@456", "$2a$10$encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("BookTalk@456")).thenReturn("encoded-new");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.changePassword(request);

        verify(userMapper).updateById((User) org.mockito.ArgumentMatchers.argThat(
                user -> ((User) user).getId().equals(2L) && "encoded-new".equals(((User) user).getPassword())));
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        UserContext.saveUser(UserDTO.builder().id(2L).username("alice").build());
        UserPasswordChangeDTO request = new UserPasswordChangeDTO();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("BookTalk@456");
        request.setConfirmPassword("BookTalk@456");

        when(userMapper.selectById(2L)).thenReturn(User.builder().id(2L).password("$2a$10$encoded-old").build());
        when(passwordEncoder.matches("wrong-password", "$2a$10$encoded-old")).thenReturn(false);

        assertThrows(BaseException.class, () -> userService.changePassword(request));
    }
}
