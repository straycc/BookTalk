# BookTalk

BookTalk 是一个面向读者的图书发现、书评讨论和个人书架应用。项目包含 Spring Boot 后端与 Vue 3 前端，核心关注阅读决策和社区互动，而非在线电子书阅读。

## 已实现能力

- 图书发现：分类、标签、全文搜索和图书详情。
- 书评与社区：发布书评、帖子、评论、点赞、标签关联和通知。
- 个人书架：想读、在读、已读状态管理与年度阅读统计。
- 推荐：用户行为采集、兴趣画像、标签/分类/作者召回、已交互图书过滤、热门兜底和 Redis 缓存。
- 数据一致性：评论、点赞和书评评分聚合在事务内维护；图书评分统一来自 `book_review.score`。
- AI 基础能力：保留会话和推荐解释基础设施，后续将扩展为阅读决策助手。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 11, Spring Boot 2.6, MyBatis-Plus |
| 前端 | Vue 3, Vite, Vue Router, Lucide |
| 数据 | MySQL 8, Redis, RabbitMQ |
| 搜索 | Elasticsearch 8（可选） |
| 其他 | Redisson, WebSocket, JWT, Aliyun OSS |

## 目录

```text
BookTalk/
├── booktalk-web/              Vue 3 前端
├── sql/                       建表、演示数据和迁移脚本
├── src/main/java/             后端代码
│   └── com/cc/booktalk/
│       ├── application/       用例和事务编排
│       ├── domain/            业务模型与规则
│       ├── infrastructure/    持久化、缓存、消息和外部服务
│       └── interfaces/        HTTP、MQ、WebSocket 和定时任务入口
├── docker-compose.yml         本地依赖服务
└── .env.example               环境变量样例
```

## 本地启动

### 1. 启动依赖服务

复制 `.env.example` 为本地 `.env`，按需要调整端口和密码，然后启动 MySQL、Redis、RabbitMQ：

```bash
docker compose up -d mysql redis rabbitmq
```

Elasticsearch 仅在需要搜索功能时启动：

```bash
docker compose --profile search up -d elasticsearch
```

默认 MySQL 数据库为 `book_talk`，根密码为 `booktalk`。首次创建容器时会执行 `sql/booktalk.sql`。

### 2. 初始化演示数据

在数据库创建完成后执行：

```bash
docker exec -i booktalk-mysql mysql -uroot -pbooktalk book_talk < sql/booktalk-demo-data.sql
```

Windows PowerShell：

```powershell
Get-Content -Raw sql/booktalk-demo-data.sql | docker exec -i booktalk-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" book_talk'
```

演示账号：`alice`、`bob`、`carol`，密码均为 `BookTalk@123`。

### 3. 启动后端

项目使用本机 Maven；当前不提供 Maven Wrapper。

```bash
mvn spring-boot:run
```

后端默认地址：`http://localhost:8081`。

常用环境变量见 `.env.example`，包括 `MYSQL_*`、`REDIS_*`、`RABBITMQ_*`、`ELASTICSEARCH_URIS`、`JWT_SECRET` 和 `AI_*`。Spring Boot 不会自动读取项目根目录的 `.env`，请在 IDE 运行配置、系统环境变量或容器环境中设置它们。

### 4. 启动前端

```bash
cd booktalk-web
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`，默认直连后端 `http://127.0.0.1:8081`。如需修改后端地址，在 `booktalk-web/.env.local` 中设置 `VITE_API_BASE_URL`。

## 数据库脚本

- `sql/booktalk.sql`：全新环境的完整表结构。
- `sql/booktalk-demo-data.sql`：可重复执行的本地演示数据。
- `sql/migrations/`：已有数据库的顺序迁移脚本。生产或保留数据的环境请先备份，再按日期顺序执行。

评分来源已收敛为 `book_review.score`；`book.average_score` 与 `book.score_count` 是聚合字段，由书评发布、修改、删除时同步重算。

## 验证

```bash
mvn test
```

前端构建验证：

```bash
cd booktalk-web
npm run build
```

## 下一步

推荐链路已经可以作为工具层使用。下一阶段计划实现阅读决策助手：根据自然语言需求调用搜索、推荐和书架能力，返回可操作的图书卡片；不依赖在线阅读或电子书全文。
