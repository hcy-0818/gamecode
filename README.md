# Game Vault — 游戏账号交易平台

基于 **Spring Cloud Alibaba** 微服务架构的游戏账号交易平台，支持账号挂牌、价格协商（还价）、订单交易、管理端审批等完整业务流程。

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-brightgreen) ![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2024.0.0-blue) ![JDK](https://img.shields.io/badge/JDK-17-orange) ![MyBatis Plus](https://img.shields.io/badge/MyBatis_Plus-3.5.9-red)


## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.4.4 |
| 微服务 | Spring Cloud (Moorgate) + Spring Cloud Alibaba | 2024.0.0 / 2023.0.1.0 |
| 注册/配置中心 | Nacos | 2.4.3 |
| API 网关 | Spring Cloud Gateway (WebFlux) | — |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer | — |
| ORM | MyBatis Plus | 3.5.9 |
| 缓存 | Redis + Spring Data Redis + Spring Cache | — |
| 认证 | JWT (jjwt 0.12.6) | — |
| 数据库 | MySQL | 8.0+ |
| 构建工具 | Maven Wrapper | 3.9.16 |


## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                     Nacos Server (:8848)                      │
│                  注册中心 + 配置中心                          │
└──────┬──────────┬──────────┬──────────┬──────────┬───────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌──────────┐
│ Gateway  ││  User    ││ Account  ││  Order   ││ Bargain  │
│  :8080   ││  :8081   ││  :8082   ││  :8083   ││  :8084   │
│ 网关+鉴权││ 用户服务 ││ 账号服务 ││ 订单服务 ││ 还价服务 │
└────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘
     │           │           │           │           │
     └───────────┴───────────┴───────────┴───────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
        ┌─────┴─────┐       ┌──────┴──────┐
        │   MySQL   │       │    Redis    │
        │ game_trade│       │   :6379     │
        └───────────┘       └─────────────┘
```

## 模块划分

```
demo6-cloud/
├── demo6-common/              # 公共模块：实体、DTO、工具类、Result、JwtUtil
├── demo6-gateway/             # API 网关 (:8080) — 路由、JWT 鉴权、静态资源
├── demo6-user-service/        # 用户服务 (:8081) — 注册/登录/个人中心
├── demo6-account-service/     # 账号服务 (:8082) — 账号管理/挂牌审核
├── demo6-order-service/       # 订单服务 (:8083) — 订单创建/支付
└── demo6-bargain-service/     # 还价服务 (:8084) — 价格协商
```

## 业务流程图

```
卖家                          平台                          买家
 │                             │                             │
 ├─ 登记账号 ─────────────────►│                             │
 │                             ├─ 管理员审批 ────────────────►│
 │                             │                             │
 │                             │◄── 浏览账号 ◄──────────────┤
 │                             │                             │
 │◄── 收到还价 ◄──────────────┤ ├── 发起还价 ◄──────────────┤
 │                             │                             │
 ├─ 接受/还价 ─────────────────►├── 通知买家 ◄───────────────┤
 │                             │                             │
 │                             │◄── 买家接受 ◄──────────────┤
 │                             │                             │
 │◄── 订单通知 ◄──────────────┤ ├── 创建订单 ◄──────────────┤
 │                             │                             │
 │                             │◄── 支付订单 ◄──────────────┤
```

## 前置条件

| 中间件 | 版本要求 | 端口 | 说明 |
|--------|----------|------|------|
| JDK | 17+ | — | 必须 |
| MySQL | 8.0+ | 3306 | 数据库 `game_trade` 自动创建 |
| Redis | 5.0+ | 6379 | 缓存 + Token 黑名单 |
| Nacos | 2.4.x | 8848 | 注册中心 + 配置中心 |

**默认账号密码：**
- 管理员：`admin` / `admin123`
- 普通用户：`user1` / `123456`

**数据库配置：**
```yaml
spring.datasource:
  url: jdbc:mysql://localhost:3306/game_trade?serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
  username: root
  password: 123456
```

## 快速启动

### 1. 启动中间件

确保 MySQL 和 Redis 已启动，然后启动 Nacos（单机模式）：

```bash
cd nacos/bin
startup.cmd -m standalone       # Windows
# 或
sh startup.sh -m standalone     # Linux/Mac
```

### 2. 启动微服务

**方式一：一键启动（推荐）**

双击 `start.bat`，自动按顺序启动 Nacos + 全部 5 个服务。

**方式二：手动逐个启动**

```bash
# 先安装公共模块
mvnw install -DskipTests

# 按顺序启动各服务
cd demo6-gateway      && ../mvnw spring-boot:run
cd demo6-user-service && ../mvnw spring-boot:run
cd demo6-account-service && ../mvnw spring-boot:run
cd demo6-order-service  && ../mvnw spring-boot:run
cd demo6-bargain-service && ../mvnw spring-boot:run
```

### 3. 访问

| 入口 | 地址 |
|------|------|
| 用户端首页 | http://localhost:8080 |
| 管理后台 | http://localhost:8080/game-account-manager.html |
| Nacos 控制台 | http://localhost:8848/nacos (`nacos`/`nacos`) |

### 4. 停止服务

双击 `stop-all.bat`，或手动：

```bash
# 逐个停止
netstat -ano | findstr ":8080"   # 找到 PID
taskkill /F /PID <PID>
```

## API 一览

### 用户服务 (user-service)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | ❌ |
| POST | `/api/user/login` | 用户登录 | ❌ |
| POST | `/api/admin/login` | 管理员登录 | ❌ |
| POST | `/api/user/logout` | 退出登录 | ✅ |
| GET | `/api/user/profile` | 获取个人资料 | ✅ |
| POST | `/api/user/profile` | 更新个人资料 | ✅ |
| POST | `/api/user/change-password` | 修改密码 | ✅ |
| POST | `/api/user/avatar` | 上传头像 | ✅ |
| GET | `/api/user/avatar/{filename}` | 获取头像文件 | ❌ |
| GET | `/api/user/info` | 当前用户信息（兼容） | ✅ |
| GET | `/api/admin/users` | 用户列表 | ✅ ADMIN |

### 账号服务 (account-service)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/account/list` | 在售账号列表 | ❌ |
| GET | `/api/account/detail/{id}` | 账号详情 | ❌ |
| POST | `/api/account/create` | 管理员创建账号 | ✅ ADMIN |
| GET | `/api/account/mySales` | 我的出售列表 | ✅ |
| POST | `/api/account/retrieve/{id}` | 取回账号 | ✅ |
| DELETE | `/api/account/delete/{id}` | 删除账号 | ✅ |
| POST | `/api/account/uploadImages` | 上传账号截图 | ✅ |
| GET | `/api/account/image/{filename}` | 获取账号截图 | ❌ |
| POST | `/api/registration/submit` | 提交挂牌登记 | ✅ |
| GET | `/api/admin/registrations` | 审核列表 | ✅ ADMIN |
| POST | `/api/admin/approve/{id}` | 审批通过 | ✅ ADMIN |
| POST | `/api/admin/reject/{id}` | 驳回登记 | ✅ ADMIN |
| PUT | `/api/admin/account/update` | 更新账号信息 | ✅ ADMIN |
| POST | `/api/admin/account/offline/{id}` | 下架账号 | ✅ ADMIN |
| POST | `/api/admin/account/online/{id}` | 上架账号 | ✅ ADMIN |

### 订单服务 (order-service)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/order/create` | 创建订单 | ✅ |
| POST | `/api/order/pay/{id}` | 支付订单 | ✅ |
| POST | `/api/order/cancel/**` | 取消订单 | ✅ |
| GET | `/api/order/myOrders` | 我的订单 | ✅ |
| GET | `/api/order/detail/{id}` | 订单详情 | ✅ |
| GET | `/api/admin/orders` | 全部订单 | ✅ ADMIN |

### 还价服务 (bargain-service)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/bargain/create` | 发起还价 | ✅ |
| POST | `/api/bargain/accept/{id}` | 卖家接受买家出价 | ✅ |
| POST | `/api/bargain/counter/{id}` | 卖家还价 | ✅ |
| POST | `/api/bargain/buyerAccept/{id}` | 买家接受卖家还价 | ✅ |
| POST | `/api/bargain/delete/{id}` | 删除还价记录 | ✅ |
| GET | `/api/bargain/myBargains` | 我的还价列表 | ✅ |
| GET | `/api/bargain/receivedBargains` | 收到的还价 | ✅ |

## 配置说明

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `NACOS_SERVER` | `localhost:8848` | Nacos 服务地址 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥 |

### Standalone 模式（无 Nacos）

使用 `application-standalone.yml` profile 启动，跳过 Nacos 注册与配置中心，通过直连 URL 调用 Feign：

```bash
mvnw spring-boot:run -Dspring-boot.run.profiles=standalone
```

## 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 数据库 | 共享 MySQL，分表 | 无需分布式事务中间件 |
| 认证方案 | JWT + Gateway 全局过滤器 | 无状态，支持水平扩展 |
| 分布式事务 | 补偿模式（非 Seata） | 规模适中，单库场景足够 |
| 缓存策略 | Cache-Aside + Spring Cache | `@Cacheable`/手动清除 |
| 静态资源 | Gateway 托管 | 无需额外 Web 服务器 |

## Redis 缓存设计

| 缓存键 | 内容 | TTL | 失效时机 |
|--------|------|-----|----------|
| `account:listed:all` | 首页在售列表 | 10 min | 创建/更新/删除/审批后清除 |
| `account:detail:{id}` | 账号详情 | 10 min | 同上 |
| `order:user:{userId}` | 用户订单 | 3 min | 订单创建/支付/取消后清除 |
| `bargain:user:{userId}` | 用户还价 | 5 min | 还价变更后清除 |
| `token:blacklist` | JWT 黑名单 (Set) | = Token 剩余有效期 | 退出登录时写入 |

## 数据库表

| 表名 | 说明 | 所属服务 |
|------|------|----------|
| `sys_user` | 用户表 | user-service |
| `game_account` | 游戏账号表 | account-service |
| `registration` | 挂牌登记表 | account-service |
| `order_info` | 订单表 | order-service |
| `bargain` | 还价表 | bargain-service |

所有表使用 MyBatis Plus 逻辑删除（`deleted` 字段，1=已删除，0=未删除），主键自增。

## 项目目录

```
demo6-cloud/
├── pom.xml                          # 父 POM（版本管理）
├── start.bat                        # 一键启动脚本
├── stop-all.bat                     # 停止所有服务
├── start-standalone.bat             # 无 Nacos 模式启动
├── mvnw / mvnw.cmd                  # Maven Wrapper
├── nacos/                           # Nacos Server（建议移出项目目录）
├── demo6-common/
│   └── src/main/java/com/example/
│       ├── common/
│       │   ├── Result.java              # 统一响应体 {code, message, data}
│       │   ├── GlobalExceptionHandler.java
│       │   ├── context/UserContext.java # ThreadLocal 用户上下文
│       │   ├── config/CommonConfig.java
│       │   └── jwt/JwtUtil.java         # JWT 生成/验证
│       ├── entity/                      # 5 个实体类
│       └── dto/                         # DTO
├── demo6-gateway/
│   └── src/main/
│       ├── java/com/example/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   └── WebFluxConfig.java
│       │   └── filter/
│       │       └── AuthGlobalFilter.java     # 全局 JWT 鉴权
│       └── resources/
│           ├── application.yml
│           └── static/                       # 前端页面（4 个 HTML）
├── demo6-user-service/
├── demo6-account-service/
├── demo6-order-service/
└── demo6-bargain-service/
```

## 常见问题

### Nacos 启动失败

确认 Java 环境变量正确，Nacos 需要 JDK 8+。查看 `nacos/logs/start.out` 日志。

### 服务启动后端口被占用

```bash
# Windows
netstat -ano | findstr ":8080"
taskkill /F /PID <PID>
```

### 前端登录后提示"请先登录"

1. 检查浏览器 `localStorage` 是否有 `jwt_token`
2. 确认 Gateway 的 `AuthGlobalFilter` 白名单配置正确
3. Token 有效期 30 分钟，过期需重新登录

### Redis 缓存数据异常

```bash
redis-cli FLUSHDB     # 清空当前数据库缓存
```

### 修改公共模块后服务报错

```bash
mvnw install -DskipTests   # 父项目下重新安装
```

## License

MIT
