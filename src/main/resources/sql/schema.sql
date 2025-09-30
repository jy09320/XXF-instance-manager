
-- Napcat实例管理器 - 数据库表结构

-- 删除现有表（如果存在）
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

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_napcat_instance_name ON napcat_instance(name);
CREATE INDEX IF NOT EXISTS idx_napcat_instance_status ON napcat_instance(status);
CREATE INDEX IF NOT EXISTS idx_napcat_instance_qq_account ON napcat_instance(qq_account);
CREATE INDEX IF NOT EXISTS idx_napcat_instance_deleted ON napcat_instance(deleted);

-- 创建JSONB索引（用于配置的查询）
CREATE INDEX IF NOT EXISTS idx_napcat_instance_config_gin ON napcat_instance USING gin(config);

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