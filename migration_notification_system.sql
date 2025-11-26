-- 通知系统模块 - 数据库迁移脚本
-- 创建日期: 2025-11-26

-- 创建notifications表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    related_id BIGINT,
    is_read BOOLEAN DEFAULT false,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_create_time ON notifications(user_id, create_time DESC);

-- 添加表注释
COMMENT ON TABLE notifications IS '系统通知表';
COMMENT ON COLUMN notifications.id IS '通知ID';
COMMENT ON COLUMN notifications.user_id IS '接收用户ID';
COMMENT ON COLUMN notifications.type IS '通知类型（EXAM_PUBLISHED/EXAM_REMINDER/GRADE_RELEASED/COURSE_UPDATE/SYSTEM_ANNOUNCEMENT）';
COMMENT ON COLUMN notifications.title IS '通知标题';
COMMENT ON COLUMN notifications.content IS '通知内容';
COMMENT ON COLUMN notifications.related_id IS '关联的考试/课程ID';
COMMENT ON COLUMN notifications.is_read IS '是否已读';
COMMENT ON COLUMN notifications.create_time IS '创建时间';
