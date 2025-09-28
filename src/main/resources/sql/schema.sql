
-- Napcat实例管理器 - 数据库表结构

-- 删除现有表（如果存在）
-- DROP TABLE IF EXISTS message_log CASCADE;
-- DROP TABLE IF EXISTS napcat_instance CASCADE;

-- 创建napcat实例表
CREATE TABLE IF NOT EXISTS napcat_instance (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    container_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'STOPPED',
    config JSONB,
    port INTEGER,
    qq_account VARCHAR(20),
    deleted SMALLINT NOT NULL DEFAULT 0,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建消息日志表
CREATE TABLE IF NOT EXISTS message_log (
    id VARCHAR(36) PRIMARY KEY,
    instance_id VARCHAR(36) NOT NULL,
    direction VARCHAR(10) NOT NULL, -- 'INBOUND' or 'OUTBOUND'
    message_type VARCHAR(50),
    content JSONB NOT NULL,
    forwarded BOOLEAN DEFAULT FALSE,
    deleted SMALLINT NOT NULL DEFAULT 0,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instance_id) REFERENCES napcat_instance(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_napcat_instance_name ON napcat_instance(name);
CREATE INDEX IF NOT EXISTS idx_napcat_instance_status ON napcat_instance(status);
CREATE INDEX IF NOT EXISTS idx_napcat_instance_deleted ON napcat_instance(deleted);

CREATE INDEX IF NOT EXISTS idx_message_log_instance_id ON message_log(instance_id);
CREATE INDEX IF NOT EXISTS idx_message_log_direction ON message_log(direction);
CREATE INDEX IF NOT EXISTS idx_message_log_created_time ON message_log(created_time);
CREATE INDEX IF NOT EXISTS idx_message_log_deleted ON message_log(deleted);

-- 创建JSONB索引（用于配置和消息内容的查询）
CREATE INDEX IF NOT EXISTS idx_napcat_instance_config_gin ON napcat_instance USING gin(config);
CREATE INDEX IF NOT EXISTS idx_message_log_content_gin ON message_log USING gin(content);

-- 添加注释
COMMENT ON TABLE napcat_instance IS 'Napcat实例表';
COMMENT ON COLUMN napcat_instance.id IS '主键ID';
COMMENT ON COLUMN napcat_instance.name IS '实例名称';
COMMENT ON COLUMN napcat_instance.container_id IS 'Docker容器ID';
COMMENT ON COLUMN napcat_instance.status IS '运行状态: STOPPED, STARTING, RUNNING, STOPPING, ERROR';
COMMENT ON COLUMN napcat_instance.config IS '实例配置信息(JSON格式)';
COMMENT ON COLUMN napcat_instance.port IS '实例端口号';
COMMENT ON COLUMN napcat_instance.qq_account IS 'QQ账号';
COMMENT ON COLUMN napcat_instance.deleted IS '逻辑删除标记: 0-未删除, 1-已删除';

COMMENT ON TABLE message_log IS '消息日志表';
COMMENT ON COLUMN message_log.id IS '主键ID';
COMMENT ON COLUMN message_log.instance_id IS '实例ID';
COMMENT ON COLUMN message_log.direction IS '消息方向: INBOUND-接收, OUTBOUND-发送';
COMMENT ON COLUMN message_log.message_type IS '消息类型';
COMMENT ON COLUMN message_log.content IS '消息内容(JSON格式)';
COMMENT ON COLUMN message_log.forwarded IS '是否已转发';
COMMENT ON COLUMN message_log.deleted IS '逻辑删除标记: 0-未删除, 1-已删除';