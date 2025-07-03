# 用户相关数据库表设计

#用户基本信息表
CREATE TABLE user (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',
                      username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录用，唯一）',
                      password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                      email VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
                      phone VARCHAR(20) DEFAULT NULL COMMENT '手机号（用于展示或绑定）',
                      status TINYINT DEFAULT 1 COMMENT '账号状态：1正常，0禁用',
                      role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
                      create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
                      update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账号信息最后修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录账号信息表';



#用户详细信息表
CREATE TABLE user_info (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                           user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
                           nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称（可重复）',
                           avatar VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
                           background VARCHAR(255) DEFAULT NULL COMMENT '背景图地址',
                           gender CHAR(1) DEFAULT 'O' COMMENT '性别（M男/F女/O其他）',
                           birthday DATE DEFAULT NULL COMMENT '生日',
                           region VARCHAR(100) DEFAULT NULL COMMENT '所在地区',
                           signature VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
                           level INT DEFAULT 1 COMMENT '用户等级',
                           experience INT DEFAULT 0 COMMENT '经验值',

                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '资料创建时间',
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料最后修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展资料表（展示/编辑）';
