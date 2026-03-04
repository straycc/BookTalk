package com.cc.booktalk.entity.dto.user;

import lombok.*;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phone;

    private Integer status;

    private String role;
}

