-- 为 exam_submissions 表添加监考数据字段
-- 执行日期: 2025-11-26

-- 添加切出次数字段
ALTER TABLE exam_submissions ADD COLUMN IF NOT EXISTS switch_count INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN exam_submissions.switch_count IS '切出考试页面次数';

-- 添加监考数据字段
ALTER TABLE exam_submissions ADD COLUMN IF NOT EXISTS proctoring_data JSONB;
COMMENT ON COLUMN exam_submissions.proctoring_data IS '监考详细数据（JSON格式）';

-- 更新已有数据的默认值
UPDATE exam_submissions SET 
    switch_count = 0
WHERE switch_count IS NULL;
