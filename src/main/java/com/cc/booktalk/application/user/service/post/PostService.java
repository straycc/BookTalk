package com.cc.booktalk.application.user.service.post;

import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.dto.user.post.PostCreateDTO;
import com.cc.booktalk.interfaces.dto.user.post.PostPageDTO;
import com.cc.booktalk.interfaces.vo.user.post.PostVO;

public interface PostService {

    Long createPost(PostCreateDTO postCreateDTO);

    void deletePost(Long postId);

    PostVO getPostDetail(Long postId);

    PageResult<PostVO> getPostPage(PostPageDTO postPageDTO);
}
