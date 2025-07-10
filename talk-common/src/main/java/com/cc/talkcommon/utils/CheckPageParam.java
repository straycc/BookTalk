package com.cc.talkcommon.utils;


import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.dto.PageDTO;

public class CheckPageParam {
    public static <T extends PageDTO> boolean checkPageDTO (T pageDTO){
        if(pageDTO == null){
            throw new BaseException("参数为null！");
        }
        if(pageDTO.getPageNum() == null || pageDTO.getPageSize() == null ||
                pageDTO.getPageNum() < 1 || pageDTO.getPageSize() < 1){
            throw new BaseException("分页参数有误!");
        }
        return true;
    }
}
