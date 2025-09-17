package com.cc.talkcommon.utils;


import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkpojo.dto.PageDTO;

public class CheckPageParam {
    public static <T extends PageDTO> boolean checkPageDTO (T pageDTO){
        if(pageDTO == null){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if(pageDTO.getPageNum() == null || pageDTO.getPageSize() == null ||
                pageDTO.getPageNum() < 1 || pageDTO.getPageSize() < 1){
            throw new BaseException(BusinessConstant.PAGE_PARAM_ERROR);
        }
        return true;
    }
}
