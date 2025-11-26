-- AI辅助判题模块 - 数据库迁移脚本
-- 创建日期: 2025-11-26

-- 创建ai_config表
CREATE TABLE IF NOT EXISTS ai_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(255),
    updated_by BIGINT REFERENCES users(id),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_ai_config_key ON ai_config(config_key);

-- 插入默认配置
INSERT INTO ai_config (config_key, config_value, description) VALUES
('system_prompt', '你是一个专业的教师助手，负责评估学生的答案。

评分标准：
1. 准确性：概念是否正确
2. 完整性：是否涵盖关键点
3. 清晰度：表述是否清晰

请根据题目、参考答案和学生答案，给出：
- suggestedScore: 建议分数（0到题目满分之间的整数）
- explanation: 评分说明（简要解释为什么给这个分数）
- strengths: 优点列表（数组）
- improvements: 改进建议列表（数组）

请以JSON格式返回结果，严格按照以下格式：
{
  "suggestedScore": 数字,
  "explanation": "文本",
  "strengths": ["优点1", "优点2"],
  "improvements": ["建议1", "建议2"]
}', 'AI判题系统提示词'),

('model_name', 'gpt-3.5-turbo', '使用的OpenAI模型'),
('temperature', '0.3', '模型温度参数（0-2之间，越低越确定）'),
('max_tokens', '500', '最大响应Token数'),
('api_base_url', 'https://api.openai.com/v1', 'OpenAI API基础URL')
ON CONFLICT (config_key) DO NOTHING;

COMMENT ON TABLE ai_config IS 'AI辅助判题配置表';
COMMENT ON COLUMN ai_config.config_key IS '配置键（唯一）';
COMMENT ON COLUMN ai_config.config_value IS '配置值';
COMMENT ON COLUMN ai_config.description IS '配置描述';
COMMENT ON COLUMN ai_config.updated_by IS '最后更新人ID';
COMMENT ON COLUMN ai_config.update_time IS '更新时间';
COMMENT ON COLUMN ai_config.create_time IS '创建时间';
