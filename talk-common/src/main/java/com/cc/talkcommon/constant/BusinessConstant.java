package com.cc.talkcommon.constant;

public class BusinessConstant {



    // 枚举类型相关
    public static final String TARGETTYPE_ERROR = "目标类型非法" ;


    // 权限相关
    public static final String WITH_NO_AUTHORITION = "没有权限" ;


    //参数相关
    public static final String PARAM_ERROR = "参数错误" ;
    public static final String PAGE_PARAM_ERROR = "分页参数错误" ;
    public static final String BATCH_DELETE_ERROR = "批量删除失败" ;

    //用户角色相关
    public static final String USER_ROLE_USER = "普通用户";
    public static final String USER_ROLE_ADMIN = "管理员用户";
    public static final Long USER_ROLE_ADMIN_ID = 0L;
    public static final String USER_DEFAULT_PASSWORD = "123456";


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


    //书评相关
    public static final  String REVIEW_TYPE_ERROR = "书评类型有误";
    public static final  String REVIEW_AUTH_ERROR = "无权限修改他人的书评";
    public static final  Integer  REVIEW_TYPE_DEFAULT = 0;
    public static final  String  REVIEW_Title_DEFAULT = "标题未填写";
    public static final  String REVIEW_NOTEXIST = "书评不存在";

    public static final  String REVIEW_BOOK_NOTEXIST = "书评书籍不存在";
    public static final  Integer REVIEW_WAIT_AUDIT = 1; // 书评待审核
    public static final  Integer REVIEW_TYPE_SHORT = 0; // 短书评
    public static final  Integer REVIEW_TYPE_LONG = 1; // 长书评

    //Result
    public static final  String PUBLISH_BOOK_SUCCESS = "发布书评成功";
    public static final  String DELETE_BOOK_SUCCESS = "删除书评成功";
    public static final  String UPDATE_BOOK_SUCCESS = "修改书评成功";

    //评论相关内容
    public static final  String COMMENT_ISEMPTY = "书评为空";
    public static final  String PARENTCOMMENT_NOTEXIST = "父评论不存在";
    public static final  String PUBLISH_COMMENT_SUCCESS = "发布评论成功";
    public static final  String DELETE_COMMENT_SUCCESS = "删除评论成功";
    public static final  String COMMENT_NOTEXIST = "评论不存在";
    public static final  String DELETE_COMMENT_ERROR = "仅支持删除自己的评论";

    //点赞相关内容
    public static final  String Like_RECORED_NOTEXIST = "点赞记录不存在，无法取消";
    public static final  String LIKE_TYPE_BOOKLIST = "bookList";
    public static final  String LIKE_TYPE_REVIEW = "bookReview";
    public static final  String LIKE_TYPE_COMMENT = "comment";



}
