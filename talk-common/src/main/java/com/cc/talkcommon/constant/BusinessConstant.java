package com.cc.talkcommon.constant;

public class BusinessConstant {
    public static final String PARAM_ERROR = "参数错误" ;
    public static final String BATCH_DELETE_ERROR = "批量删除失败" ;

    //tag想相关
    public static final String TAG_WITH_BOOKS = "部分标签已被书籍引用，无法删除";
    public static final String CATEGROY_WITH_TAGS = "该分类下暂无标签";
    public static final String TAG_NAME_REPEAT = "标签名重复";
    public static final String TAG_NAME_NOTEXIST = "标签名不存在";
    public static final String TAG_UPDATE_ERROR = "标签更新失败";
    public static final String TAG_DELETE_ERROR = "标签删除失败";

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


}
