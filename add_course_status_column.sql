-- 为 courses 表添加 status 字段
ALTER TABLE courses ADD COLUMN IF NOT EXISTS status INTEGER NOT NULL DEFAULT 1;

-- 为现有课程设置状态为活跃
UPDATE courses SET status = 1 WHERE status IS NULL;

-- 添加注释
COMMENT ON COLUMN courses.status IS '课程状态：1=活跃，0=停用';
