package com.cc.booktalk.application.user.service.square;

import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.square.SquareFeedQueryDTO;
import com.cc.booktalk.interfaces.vo.user.square.SquareFeedVO;

public interface SquareFeedService {

    PageResult<SquareFeedVO> getFeed(SquareFeedQueryDTO queryDTO);
}
