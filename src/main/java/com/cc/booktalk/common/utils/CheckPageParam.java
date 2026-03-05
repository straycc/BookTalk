package com.cc.booktalk.common.utils;


import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.interfaces.dto.user.base.PageDTO;

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
