# NapCat WebSocket连接配置计划

## 问题描述
当前项目无法接收到NapCat的消息，原因是NapCat没有配置WebSocket客户端连接到项目服务。需要实现方式2：让NapCat主动连接到项目服务。

## 当前状态分析
- **NapCat状态**: 已登录QQ，能接收消息，但OneBot11配置中websocketClients为空
- **项目状态**: WebSocket服务正常运行，监听路径 `/ws/napcat/*`
- **问题**: NapCat没有连接到项目，消息无法传递

## 实施计划

### 步骤1: 分析当前项目的WebSocket端点配置
**目标**: 确认项目的WebSocket配置信息

**检查内容**:
- 项目监听端口（默认8080）
- WebSocket路径：`/ws/napcat/{instanceId}`
- 允许的origins配置

**验证命令**:
```bash
# 检查项目配置
cat src/main/resources/application.yml
# 或
cat src/main/resources/application-dev.yml
```

### 步骤2: 确定项目监听的WebSocket地址和端口
**目标**: 确定NapCat需要连接的完整地址

**预期结果**:
- 项目端口: `8080`
- WebSocket端点: `ws://host.docker.internal:8080/ws/napcat/1`
- 实例ID: `1` (根据实际实例ID调整)

### 步骤3: 修改Docker容器创建时的网络配置
**目标**: 确保容器能访问宿主机服务

**当前问题**: 容器可能无法访问宿主机的8080端口

**解决方案**:
选择以下方案之一：

**方案A: 添加host映射**
```bash
docker run --add-host host.docker.internal:host-gateway ...
```

**方案B: 使用host网络模式**
```bash
docker run --network host ...
```

**方案C: 使用宿主机IP**
```bash
# 获取宿主机IP
ip route | grep docker0
# 配置连接到实际IP地址
```

### 步骤4: 配置NapCat的OneBot11 WebSocket客户端连接
**目标**: 让NapCat主动连接到项目服务

**操作步骤**:

1. **编辑配置文件**:
```bash
docker exec napcat-instance-1 bash -c "cat > /app/napcat/config/onebot11_3645563149.json << 'EOF'
{
  \"network\": {
    \"httpServers\": [],
    \"httpSseServers\": [],
    \"httpClients\": [],
    \"websocketServers\": [],
    \"websocketClients\": [
      {
        \"name\": \"xxf-instance-manager\",
        \"url\": \"ws://host.docker.internal:8080/ws/napcat/1\",
        \"token\": \"\",
        \"enable\": true,
        \"reconnectInterval\": 3000,
        \"heartInterval\": 30000
      }
    ],
    \"plugins\": []
  },
  \"musicSignUrl\": \"\",
  \"enableLocalFile2Url\": false,
  \"parseMultMsg\": false
}
EOF"
```

2. **验证配置**:
```bash
docker exec napcat-instance-1 cat /app/napcat/config/onebot11_3645563149.json
```

### 步骤5: 重启NapCat容器使配置生效
**目标**: 应用新的WebSocket配置

**操作步骤**:
```bash
# 重启容器
docker restart napcat-instance-1

# 查看启动日志
docker logs napcat-instance-1 -f
```

**期待的日志信息**:
- WebSocket客户端连接建立
- 连接到xxf-instance-manager成功的日志

### 步骤6: 测试消息接收功能
**目标**: 验证消息能正常传递到项目

**测试步骤**:
1. **启动项目服务**:
```bash
mvn spring-boot:run
```

2. **查看项目日志**:
```bash
tail -f logs/application.log
```

3. **发送测试消息**:
- 向NapCat登录的QQ号发送消息
- 观察项目日志是否有WebSocket连接和消息接收记录

**期待的日志**:
```
INFO - Napcat instance 1 connected via WebSocket: xxx
INFO - Processed message from instance 1: message
```

### 步骤7: 验证消息日志存储
**目标**: 确认消息正确存储到数据库

**验证方法**:

1. **检查数据库记录**:
```sql
SELECT * FROM message_log ORDER BY created_time DESC LIMIT 10;
```

2. **通过API查看**:
```bash
curl http://localhost:8080/api/messages/1
```

3. **检查消息转发**:
- 确认`napcat.forwarding.enabled`配置
- 查看转发日志

## 故障排查

### 常见问题及解决方案

1. **容器无法连接宿主机**:
   - 检查防火墙设置
   - 尝试不同的网络配置方案
   - 使用`docker exec`进入容器测试连通性

2. **WebSocket连接失败**:
   - 检查项目是否正常启动
   - 验证WebSocket端点路径
   - 查看项目和容器日志

3. **消息接收不到**:
   - 确认QQ已正常登录
   - 检查OneBot11配置是否正确
   - 验证WebSocket连接状态

### 调试命令

```bash
# 查看容器网络信息
docker exec napcat-instance-1 ip route

# 测试连通性
docker exec napcat-instance-1 curl -v http://host.docker.internal:8080/health

# 查看NapCat日志
docker logs napcat-instance-1 --tail 50 -f

# 查看项目日志
tail -f logs/application.log | grep -i websocket
```

## 配置文件备份

### 原始OneBot11配置
```json
{
  "network": {
    "httpServers": [],
    "httpSseServers": [],
    "httpClients": [],
    "websocketServers": [],
    "websocketClients": [],
    "plugins": []
  },
  "musicSignUrl": "",
  "enableLocalFile2Url": false,
  "parseMultMsg": false
}
```

### 目标OneBot11配置
```json
{
  "network": {
    "httpServers": [],
    "httpSseServers": [],
    "httpClients": [],
    "websocketServers": [],
    "websocketClients": [
      {
        "name": "xxf-instance-manager",
        "url": "ws://host.docker.internal:8080/ws/napcat/1",
        "token": "",
        "enable": true,
        "reconnectInterval": 3000,
        "heartInterval": 30000
      }
    ],
    "plugins": []
  },
  "musicSignUrl": "",
  "enableLocalFile2Url": false,
  "parseMultMsg": false
}
```

## 预期结果

完成配置后，应该实现：
1. NapCat主动连接到项目WebSocket端点
2. 项目能接收到所有QQ消息
3. 消息正确存储到数据库
4. 消息转发功能正常工作
5. 可通过API查看消息历史

## 注意事项

1. **实例ID**: 确保WebSocket路径中的实例ID与数据库中的实例ID一致
2. **端口冲突**: 确认8080端口没有被其他服务占用
3. **配置备份**: 修改配置前建议备份原始文件
4. **日志监控**: 整个过程中持续监控日志输出
5. **网络安全**: 生产环境中考虑添加token验证

---

**文档创建时间**: 2025-09-28
**预计执行时间**: 30-60分钟
**难度等级**: 中等