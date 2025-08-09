package com.cc.talkcommon.constant;

public class BusinessConstant {





    // 权限相关
    public static final String WITH_NO_AUTHORITION = "没有权限" ;


    //参数相关
    public static final String PARAM_ERROR = "参数错误" ;
    public static final String BATCH_DELETE_ERROR = "批量删除失败" ;

    //用户角色相关
    public static final String USER_ROLE_USER = "普通用户";
    public static final String USER_ROLE_ADMIN = "管理员用户";
    public static final Long USER_ROLE_ADMIN_ID = 0L;

    //tag相关
    public static final String TAG_WITH_BOOKS = "部分标签已被书籍引用，无法删除";
    public static final String CATEGROY_WITH_TAGS = "该分类下暂无标签";
    public static final String TAG_NAME_REPEAT = "标签名重复";
    public static final String TAG_NAME_NOTEXIST = "标签名不存在";
    public static final String TAG_UPDATE_ERROR = "标签更新失败";
    public static final String TAG_UPDATE_SUCESS = "标签更新成功";
    public static final String TAG_DELETE_ERROR = "标签删除失败";
    public static final String TAG_DELETE_SUCESS = "标签删除成功";
    public static final String TAG_INSERT_SUCESS = "标签新增成功";
    public static final String TAG_INSERT_ERROR = "标签新增失败";

    public static final String TAG_NOT_EXIST = "标签不存在";
    public static final String TAG_CREAT_SUCESS = "标签建立成功";
    public static final String TAG_CREAT_ERROR = "标签建立失败";

    //book相关
    public static final String BOOK_NOTEXIST = "书籍不存在";
    public static final String BOOK_UPLOAD_ERROR = "上传书籍失败";
    public static final String BOOK_UPDATE_MYSQL_ERROR = "更新MySQL书籍失败";
    public static final String BOOK_UPDATE_ES_ERROR = "更新Elasticsearch书籍失败";
    public static final String BOOK_DELETE_MYSQL_ERROR = "删除MySQL书籍失败";
    public static final String BOOK_DELETE_ES_ERROR = "删除Elasticsearch书籍失败";
    public static final String BOOK_SEARCH_ERROR = "图书搜索失败";


    //缓存相关
    public static final String TRYLOCK_ERROR = "获取分布式锁失败";

    public static final Integer BOOK_CACAHE_EXPIRETIME = 7;

    //分类相关
    public static final String CATEGORY_NOT_EXIST = "分类不存在";

}
