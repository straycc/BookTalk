package com.cc.booktalk.common.context;

import com.cc.booktalk.interfaces.dto.user.UserDTO;

public class UserContext {

    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
