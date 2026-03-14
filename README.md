# BookTalk

## 项目描述
BookTalk 是一个面向读者的图书分享与讨论平台后端，采用领域驱动分层思想组织代码。平台支持用户发现图书、发布书评、参与评论点赞、管理个人书架，并根据阅读行为获取个性化推荐。系统同时提供实时通知、基于 Elasticsearch 的搜索能力与事件驱动处理机制。

## 系统架构
项目采用分层架构，依赖方向明确：

`interfaces -> application -> domain`

- `interfaces`：接口层（Controller、MQ Consumer、Event Listener、Schedule Job）
- `application`：应用层（用例编排、事务协调、流程控制）
- `domain`：领域层（核心业务规则、热度与推荐计算）
- `infrastructure`：基础设施层（持久化、缓存、消息、搜索、外部服务集成）
- `common`：公共能力层（工具、异常、结果封装等）

架构规则：接口层保持薄壳，领域层承载核心业务逻辑。

## 技术栈

| 类别 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 框架 | Spring Boot | 2.6.13 | 核心应用框架 |
| 语言 | Java | 11 | 开发语言 |
| 数据库 | MySQL | 8.0.30 | 主数据存储 |
| ORM | MyBatis-Plus | 3.5.7 | 数据库操作与映射 |
| 缓存 | Redis + Redisson | 3.23.3 | 分布式缓存与锁 |
| 搜索 | Elasticsearch | 8.13.0 | 全文图书检索 |
| 消息队列 | RabbitMQ | - | 事件驱动通信 |
| 任务调度 | XXL-Job | 2.4.0 | 分布式定时任务 |
| 文件存储 | Aliyun OSS | 3.17.2 | 图片与文件存储 |
| 实时通信 | WebSocket | - | 实时通知 |
| 认证 | JWT | - | Token 认证 |
| 工具库 | Hutool | 5.8.21 | Java 工具类库 |

## 核心特点

### 1. 搜索与内容发现
- 基于 Elasticsearch 的图书全文检索
- 支持分类与标签筛选

### 2. 社交互动
- 书评与评分
- 评论与点赞
- 书单创建与分享
- 个人书架与阅读状态管理

### 3. 个性化推荐
- 基于用户行为与兴趣标签进行推荐
- 热门内容兜底，提升冷启动体验
- AOP 行为采集支持兴趣更新与推荐计算

### 4. 排行与趋势发现
- 热门图书排行
- 热门书评排行
- 支持日/周/月周期排行

### 5. 实时通知
- 基于 RabbitMQ + WebSocket 的实时触达
- 支持书评回复、点赞动态、系统通知等场景

### 6. 可扩展架构能力
- 事件驱动解耦核心链路（通知、行为、推荐）
- Redis + Redisson 支撑高频读写与并发控制
- XXL-Job 支撑推荐与排行的周期刷新任务

## 项目结构

```text
src/main/java/com/cc/booktalk/
├── application/
├── domain/
├── infrastructure/
├── interfaces/
└── common/
```

详细结构可参考 `src/main/java/com/cc/booktalk/README-ARCH.md`。

## 快速启动

1. 准备依赖服务：MySQL、Redis、RabbitMQ、Elasticsearch
2. 配置 `src/main/resources/application.yaml` 与环境变量
3. 启动项目：

```bash
mvn clean spring-boot:run
```

或：

```bash
mvn clean package
java -jar target/*.jar
```
