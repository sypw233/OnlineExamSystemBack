-- 为 exams 表添加监考控制字段
-- 执行日期: 2025-11-26

-- 添加允许的平台字段
ALTER TABLE exams ADD COLUMN IF NOT EXISTS allowed_platforms VARCHAR(50);
COMMENT ON COLUMN exams.allowed_platforms IS '允许的考试平台: desktop, mobile, both';

-- 添加严格监考模式字段
ALTER TABLE exams ADD COLUMN IF NOT EXISTS strict_mode BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN exams.strict_mode IS '是否开启严格监考模式';

-- 添加最大切出次数字段
ALTER TABLE exams ADD COLUMN IF NOT EXISTS max_switch_count INTEGER;
COMMENT ON COLUMN exams.max_switch_count IS '最大允许切出次数（null表示无限制）';

-- 添加强制全屏字段
ALTER TABLE exams ADD COLUMN IF NOT EXISTS fullscreen_required BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN exams.fullscreen_required IS '是否要求全屏模式（仅桌面端）';

-- 更新已有数据的默认值
UPDATE exams SET 
    allowed_platforms = 'both',
    strict_mode = FALSE,
    fullscreen_required = FALSE
WHERE allowed_platforms IS NULL;
