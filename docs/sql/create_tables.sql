-- ============================================================
-- BuddyStudyGuard 云端数据库建表脚本（CloudBase PostgreSQL / PostgREST）
-- ============================================================
-- 说明：
--   1. 所有家庭标识统一使用 family_code 字段，应用层按 family_code 过滤实现家庭数据共享。
--   2. users 表以 uid(text) 为主键（对应登录返回的 sub），其余表用 id(bigserial) 自增主键。
--   3. 时间字段统一使用 bigint 存毫秒时间戳（System.currentTimeMillis()）。
--   4. 不启用 RLS，家庭数据共享由应用层 family_code 过滤保证。
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    uid          TEXT PRIMARY KEY,
    identity     TEXT,
    nickname     TEXT,
    phone        TEXT,
    family_code  TEXT,
    created_at   BIGINT
);

-- 家庭关系表
CREATE TABLE IF NOT EXISTS family_groups (
    id           BIGSERIAL PRIMARY KEY,
    family_code  TEXT NOT NULL,
    student_uid  TEXT,
    parent_uid   TEXT,
    created_at   BIGINT
);

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id           BIGSERIAL PRIMARY KEY,
    uid          TEXT,
    phone        TEXT,
    sender_type  TEXT,
    sender_name  TEXT,
    content      TEXT,
    timestamp    BIGINT,
    family_code  TEXT
);

-- 任务表
CREATE TABLE IF NOT EXISTS tasks (
    id           BIGSERIAL PRIMARY KEY,
    uid          TEXT,
    title        TEXT,
    content      TEXT,
    completed    BOOLEAN DEFAULT FALSE,
    deadline     BIGINT,
    family_code  TEXT,
    created_at   BIGINT
);

-- 应用使用记录表
CREATE TABLE IF NOT EXISTS app_usage (
    id           BIGSERIAL PRIMARY KEY,
    uid          TEXT,
    app_name     TEXT,
    duration     BIGINT,
    date         TEXT,
    timestamp    BIGINT,
    family_code  TEXT,
    package_name TEXT,
    category     TEXT
);

-- 应用锁定规则表（家长下发，快照覆盖式同步）
CREATE TABLE IF NOT EXISTS app_lock_rules (
    id           BIGSERIAL PRIMARY KEY,
    family_code  TEXT NOT NULL,
    package_name TEXT,
    app_name     TEXT,
    locked       BOOLEAN DEFAULT FALSE,
    updated_at   BIGINT
);

-- 应用时长限制规则表（家长下发，快照覆盖式同步）
CREATE TABLE IF NOT EXISTS app_limit_rules (
    id           BIGSERIAL PRIMARY KEY,
    family_code  TEXT NOT NULL,
    package_name TEXT,
    app_name     TEXT,
    daily_limit_ms BIGINT DEFAULT 0,
    enabled      BOOLEAN DEFAULT TRUE,
    updated_at   BIGINT
);

-- 禁用时段表（家长下发，快照覆盖式同步）
-- packages 存逗号分隔的包名列表（如 com.a,com.b），空字符串表示 applies_to_all 生效
CREATE TABLE IF NOT EXISTS blocked_time_windows (
    id           BIGSERIAL PRIMARY KEY,
    family_code  TEXT NOT NULL,
    label        TEXT,
    start_minute INT,
    end_minute   INT,
    days_of_week INT,
    applies_to_all BOOLEAN DEFAULT TRUE,
    enabled      BOOLEAN DEFAULT TRUE,
    packages     TEXT,
    updated_at   BIGINT
);

-- 学生端已安装应用清单表（学生上报 / 家长拉取合并）
CREATE TABLE IF NOT EXISTS child_apps (
    id           BIGSERIAL PRIMARY KEY,
    uid          TEXT,
    family_code  TEXT,
    package_name TEXT,
    app_name     TEXT,
    category     TEXT,
    updated_at   BIGINT
);

-- ============================================================
-- 已有库升级：为 app_usage 追加 package_name / category 两列
-- （家长端报告读取云端弟弟数据需要）
-- 请在 CloudBase 控制台手动执行以下语句：
-- ============================================================
ALTER TABLE app_usage ADD COLUMN IF NOT EXISTS package_name TEXT;
ALTER TABLE app_usage ADD COLUMN IF NOT EXISTS category TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS image TEXT;
