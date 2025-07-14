package com.cc.talkserver.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.BuildQueryWrapper;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.dto.BookShowDTO;
import com.cc.talkpojo.dto.PageBookDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.vo.BookVO;
import com.cc.talkserver.user.mapper.BookUserMapper;
import com.cc.talkserver.user.mapper.CategoryUserMapper;
import com.cc.talkserver.user.service.BookUserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书主表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Service
public class BookUserServiceImpl extends ServiceImpl<BookUserMapper, Book> implements BookUserService {

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private CategoryUserMapper categoryUserMapper;

    @Override
    public PageResult<BookShowDTO> getBookPage(PageBookDTO pageBookDTO) {

        // 1. 参数检查
        CheckPageParam.checkPageDTO(pageBookDTO);

        // 2. 构建查询条件(工具类)
        LambdaQueryWrapper<Book> wrapper = BuildQueryWrapper.buildBookQueryWrapper(pageBookDTO);

        // 3. 执行分页查询，使用 PageHelper 来实现分页
        PageHelper.startPage(pageBookDTO.getPageNum(), pageBookDTO.getPageSize());
        List<Book> booksList = bookUserMapper.selectList(wrapper);

        // PageHelper 会自动将结果封装成 Page 对象，转换为 Page<Book>
        Page<Book> pageInfo = (Page<Book>) booksList;

        // 4. 转换为 VO 列表
        List<BookShowDTO> voList = booksList.stream()
                .map(book -> ConvertUtils.convert(book, BookShowDTO.class))
                .collect(Collectors.toList());

        // 5. 封装 PageResult
        return new PageResult<>(pageInfo.getTotal(), voList);

    }


    /**
     * 获取书籍详情
     * @param id
     * @return
     */
    @Override
    public BookVO getBookDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Book book = bookUserMapper.selectById(id);
        if (book == null) {
            throw new BaseException(BusinessConstant.BOOK_NOTEXIST);
        }
        BookVO bookVO = ConvertUtils.convert(book, BookVO.class);
        //查询所属分类
        BookCategory bookCategory = categoryUserMapper.selectById(book.getCategoryId());
        if(bookCategory != null){
            bookVO.setCategoryName(bookCategory.getName());
        }
        return bookVO;
    }
}
