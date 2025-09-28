# Napcat实例管理器技术设计文档

## 项目概述

本项目是一个基于Spring Boot 3.5.6的Java微服务，用于管理多个Docker napcat实例的生命周期，并通过WebSocket转发napcat的收发消息到指定接口。

## 技术栈

### 核心框架
- **Spring Boot 3.5.6** - 主框架
- **Java 17** - 运行时
- **Maven** - 依赖管理

### 数据层
- **PostgreSQL** - 主数据库
- **MyBatis-Plus** - ORM框架，提供强大的CRUD操作
  - 自动生成基础CRUD操作
  - 支持条件构造器，灵活查询
  - 内置分页插件
  - 逻辑删除支持
  - SQL性能分析
- **HikariCP** - 连接池

### WebSocket & 消息处理
- **Spring WebSocket** - WebSocket支持
- **Jackson** - JSON序列化

### Docker集成
- **Docker Java** - Docker API客户端

### 监控与文档
- **Spring Boot Actuator** - 健康检查和监控
- **SpringDoc OpenAPI** - API文档生成

## 架构设计

### 1. 分层架构
```
┌─────────────────────────────────────┐
│            Controller Layer         │  REST API & WebSocket端点
├─────────────────────────────────────┤
│             Service Layer           │  业务逻辑处理
│  ┌─────────────┬─────────────────────┤
│  │ Docker Svc  │ Napcat Svc │ WS Svc │
├──┴─────────────┴─────────────────────┤
│           Mapper Layer              │  数据访问层(MyBatis-Plus)
├─────────────────────────────────────┤
│          PostgreSQL Database        │  持久化存储
└─────────────────────────────────────┘
```

### 2. 核心模块

#### 实例管理模块 (Instance Management)
- **NapcatInstanceService** - 实例CRUD和生命周期管理
- **DockerService** - Docker容器操作封装
- **InstanceMonitorService** - 实例状态监控

#### WebSocket消息模块 (Message Handling)
- **NapcatWebSocketHandler** - 处理napcat的WebSocket连接
- **ClientWebSocketHandler** - 处理客户端WebSocket连接
- **MessageForwarder** - 消息转发处理器
- **MessageProcessor** - 消息预处理和后处理

#### 配置管理模块 (Configuration)
- **DockerConfig** - Docker客户端配置
- **WebSocketConfig** - WebSocket配置
- **DatabaseConfig** - 数据库配置

## 数据模型

### 核心实体

#### NapcatInstance
```sql
CREATE TABLE napcat_instance (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    container_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'STOPPED',
    config JSONB,
    port INTEGER,
    qq_account VARCHAR(20),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### MessageLog
```sql
CREATE TABLE message_log (
    id BIGSERIAL PRIMARY KEY,
    instance_id BIGINT NOT NULL REFERENCES napcat_instance(id),
    direction VARCHAR(10) NOT NULL, -- 'INBOUND' or 'OUTBOUND'
    message_type VARCHAR(50),
    content JSONB NOT NULL,
    forwarded BOOLEAN DEFAULT FALSE,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## API设计

### REST API端点

#### 实例管理
- `POST /api/instances` - 创建napcat实例
- `GET /api/instances` - 获取实例列表
- `GET /api/instances/{id}` - 获取实例详情
- `PUT /api/instances/{id}/start` - 启动实例
- `PUT /api/instances/{id}/stop` - 停止实例
- `PUT /api/instances/{id}/restart` - 重启实例
- `DELETE /api/instances/{id}` - 删除实例

#### 消息管理
- `GET /api/messages` - 获取消息日志
- `GET /api/messages/{instanceId}` - 获取指定实例的消息

### WebSocket端点
- `/ws/napcat/{instanceId}` - Napcat实例连接端点
- `/ws/client` - 客户端连接端点

## 消息转发流程

```
Napcat实例 -> WebSocket连接 -> MessageProcessor -> 转发队列 -> 目标接口
     |                                                              ^
     v                                                              |
  消息日志 <-- 持久化存储 <-- MessageLogger <-- MessageForwarder <--+
```

### 消息处理步骤
1. **接收消息** - WebSocket Handler接收napcat消息
2. **预处理** - MessageProcessor进行消息验证和格式化
3. **持久化** - 保存到message_log表
4. **转发** - MessageForwarder发送到目标接口
5. **状态更新** - 更新转发状态

## 配置管理

### application.yml 结构
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/napcat_manager
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted

napcat:
  docker:
    image: "mlikiowa/napcat-docker:latest"
    network: "napcat-network"

  websocket:
    allowed-origins: ["http://localhost:3000"]

  forwarding:
    target-url: ${FORWARD_URL:http://localhost:8081/api/messages}
    retry-attempts: 3
    timeout: 30s
```

## 部署配置

### Docker环境要求
- Docker Engine 20.0+
- 网络访问权限
- 卷挂载权限

### 数据库要求
- PostgreSQL 12+
- 连接池配置
- 适当的索引策略

## 监控和日志

### 健康检查端点
- `/actuator/health` - 应用健康状态
- `/actuator/metrics` - 应用指标
- `/actuator/info` - 应用信息

### 日志策略
- 应用日志：Logback + SLF4J
- 访问日志：Spring Boot内置
- 消息日志：数据库持久化

## 安全考虑

1. **WebSocket认证** - Token或Session验证
2. **API访问控制** - 基于角色的访问控制
3. **Docker安全** - 限制容器权限
4. **数据库安全** - 连接加密和访问控制

## 扩展性设计

1. **水平扩展** - 多实例部署支持
2. **消息队列** - 支持外部消息队列集成
3. **插件系统** - 消息处理器插件化
4. **配置中心** - 支持外部配置管理