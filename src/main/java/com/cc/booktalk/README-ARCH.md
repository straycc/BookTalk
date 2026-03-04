依赖方向约束：
interfaces -> application -> domain
infrastructure -> application/domain(接口)
禁止反向依赖；common 只放纯公共能力

入口命名约束：
- `@RabbitListener` 使用 `*Consumer`，放在 `interfaces/mq/consumer`
- `@EventListener` 使用 `*Listener`，放在 `interfaces/event/listener`
- `@Scheduled` / `@XxlJob` 使用 `*Job`，放在 `interfaces/schedule/job`

分层职责：
- interfaces 只做参数接收与转发（薄壳）
- application 只做用例编排（事务、调用顺序、权限协同）
- domain 承载业务规则与计算逻辑

目录落位：
- `interfaces`：controller / mq consumer / event listener / schedule job
- `application`：用例编排与事务
- `domain`：业务规则与策略
- `infrastructure`：mapper、缓存、消息、第三方实现
