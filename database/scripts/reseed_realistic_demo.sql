BEGIN;

TRUNCATE TABLE
    exam_submissions,
    exam_questions,
    exams,
    question_bank_questions,
    question_banks,
    course_selections,
    courses,
    questions,
    notifications,
    ai_config,
    users
RESTART IDENTITY CASCADE;

DO $$
DECLARE
    password_hash CONSTANT TEXT := '$2a$10$ZqVUOyZXy4N2Qm9wvackauUWUNUGOvETJdIiMSFaPOnpyr3T1z7Cy';
    teacher_count CONSTANT INT := 20;
    student_count CONSTANT INT := 500;
    course_names TEXT[] := ARRAY[
        '高等数学', '大学英语', 'Java 程序设计', '数据结构', '数据库系统', '操作系统',
        '计算机网络', '软件工程', '人工智能导论', '移动应用开发', '离散数学', '计算机组成原理',
        '编译原理', '信息安全基础', 'Python 数据分析', 'Web 前端开发', '机器学习基础', '云计算概论',
        '数字逻辑', '算法设计与分析', 'Linux 系统管理', '嵌入式系统', '计算机图形学', '软件测试'
    ];
    bank_suffixes TEXT[] := ARRAY['基础题库', '强化题库'];
    surnames TEXT[] := ARRAY['赵','钱','孙','李','周','吴','郑','王','冯','陈','褚','卫','蒋','沈','韩','杨','朱','秦','尤','许'];
    given_names TEXT[] := ARRAY['明','华','婷','磊','娜','杰','静','鹏','颖','超','雪','晨','琪','阳','辉','琳','宇','敏','凯','瑶'];
    current_teacher_id BIGINT;
    current_course_id BIGINT;
    bank_id BIGINT;
    current_exam_id BIGINT;
    bank_name TEXT;
    course_name TEXT;
    teacher_name TEXT;
    student_name TEXT;
    question_type TEXT;
    difficulty TEXT;
    options_json JSONB;
    answer_text TEXT;
    analysis_text TEXT;
    question_content TEXT;
    question_id BIGINT;
    current_student_id BIGINT;
    selected_course_id BIGINT;
    course_index INT;
    bank_no INT;
    question_no INT;
    exam_status INT;
    start_at TIMESTAMP;
    end_at TIMESTAMP;
    enrolled_students BIGINT[];
    score_value INT;
BEGIN
    INSERT INTO users (username, password, real_name, role, email, status, create_time)
    VALUES ('admin', password_hash, '系统管理员', 'admin', 'admin@example.com', 1, NOW());

    FOR course_index IN 1..teacher_count LOOP
        teacher_name := surnames[((course_index - 1) % array_length(surnames, 1)) + 1]
            || given_names[((course_index * 2 - 1) % array_length(given_names, 1)) + 1]
            || '老师';
        INSERT INTO users (username, password, real_name, role, email, status, create_time)
        VALUES (
            'teacher' || course_index,
            password_hash,
            teacher_name,
            'teacher',
            format('teacher%1$s@example.com', course_index),
            1,
            NOW() - (course_index || ' days')::interval
        );
    END LOOP;

    INSERT INTO users (username, password, real_name, role, email, status, create_time)
    VALUES ('student', password_hash, '演示学生', 'student', 'student@example.com', 1, NOW());

    FOR course_index IN 1..student_count LOOP
        student_name := surnames[((course_index + 3) % array_length(surnames, 1)) + 1]
            || given_names[((course_index * 3 + 1) % array_length(given_names, 1)) + 1]
            || '同学';
        INSERT INTO users (username, password, real_name, role, email, status, create_time)
        VALUES (
            format('student%1$03s', course_index),
            password_hash,
            student_name,
            'student',
            format('student%1$03s@example.com', course_index),
            1,
            NOW() - ((course_index % 90) || ' days')::interval
        );
    END LOOP;

    FOR course_index IN 1..array_length(course_names, 1) LOOP
        course_name := course_names[course_index];
        current_teacher_id := ((course_index - 1) % teacher_count) + 2;
        INSERT INTO courses (course_name, teacher_id, description, status, create_time)
        VALUES (
            course_name,
            current_teacher_id,
            course_name || '课程示例数据，包含课程、题库、考试、通知与成绩记录。',
            1,
            NOW() - ((course_index + 5) || ' days')::interval
        )
        RETURNING id INTO current_course_id;

        FOR bank_no IN 1..array_length(bank_suffixes, 1) LOOP
            bank_name := course_name || bank_suffixes[bank_no];
            INSERT INTO question_banks (name, description, creator_id, create_time)
            VALUES (
                bank_name,
                course_name || '的' || bank_suffixes[bank_no] || '，共 100 道题。',
                current_teacher_id,
                NOW() - ((course_index + bank_no) || ' days')::interval
            )
            RETURNING id INTO bank_id;

            FOR question_no IN 1..100 LOOP
                question_type := CASE question_no % 5
                    WHEN 1 THEN 'single'
                    WHEN 2 THEN 'multiple'
                    WHEN 3 THEN 'true_false'
                    WHEN 4 THEN 'fill_blank'
                    ELSE 'short_answer'
                END;
                difficulty := CASE question_no % 3
                    WHEN 1 THEN 'easy'
                    WHEN 2 THEN 'medium'
                    ELSE 'hard'
                END;

                IF question_type = 'single' THEN
                    options_json := jsonb_build_array('A. 基础概念', 'B. 核心概念', 'C. 实践技巧', 'D. 常见误区');
                    answer_text := 'B';
                    question_content := format('%s 单选题 %s：以下哪一项最符合该知识点的核心概念？', course_name, question_no);
                ELSIF question_type = 'multiple' THEN
                    options_json := jsonb_build_array('A. 关键特征', 'B. 典型应用', 'C. 错误说法', 'D. 实施步骤');
                    answer_text := 'A,B,D';
                    question_content := format('%s 多选题 %s：下列哪些表述是正确的？', course_name, question_no);
                ELSIF question_type = 'true_false' THEN
                    options_json := NULL;
                    answer_text := 'false';
                    question_content := format('%s 判断题 %s：该知识点可以脱离具体场景直接套用。', course_name, question_no);
                ELSIF question_type = 'fill_blank' THEN
                    options_json := NULL;
                    answer_text := course_name || '关键术语';
                    question_content := format('%s 填空题 %s：该主题中的关键术语是 ____。', course_name, question_no);
                ELSE
                    options_json := NULL;
                    answer_text := '可从定义、原理、应用场景与注意事项四个方面作答。';
                    question_content := format('%s 简答题 %s：请结合教学内容说明该知识点的定义、应用场景与注意事项。', course_name, question_no);
                END IF;

                analysis_text := format('解析：本题围绕《%s》中的重点知识设计，建议从概念、场景与实践三个维度理解。', course_name);

                INSERT INTO questions (content, type, options, answer, analysis, difficulty, category, creator_id, create_time)
                VALUES (
                    question_content,
                    question_type,
                    options_json,
                    answer_text,
                    analysis_text,
                    difficulty,
                    course_name,
                    current_teacher_id,
                    NOW() - ((question_no % 45) || ' days')::interval
                )
                RETURNING id INTO question_id;

                INSERT INTO question_bank_questions (bank_id, question_id)
                VALUES (bank_id, question_id);
            END LOOP;
        END LOOP;
    END LOOP;

    FOR current_student_id IN 22..(student_count + 21) LOOP
        FOR bank_no IN 0..2 LOOP
            selected_course_id := ((current_student_id + bank_no * 7) % array_length(course_names, 1)) + 1;
            INSERT INTO course_selections (student_id, course_id, selection_time)
            VALUES (current_student_id, selected_course_id, NOW() - ((current_student_id % 30) || ' days')::interval)
            ON CONFLICT (student_id, course_id) DO NOTHING;
        END LOOP;
    END LOOP;

    INSERT INTO course_selections (student_id, course_id, selection_time)
    VALUES (21, 1, NOW()), (21, 4, NOW()), (21, 9, NOW()), (21, 15, NOW())
    ON CONFLICT (student_id, course_id) DO NOTHING;

    FOR course_index IN 1..array_length(course_names, 1) LOOP
        current_course_id := course_index;
        current_teacher_id := ((course_index - 1) % teacher_count) + 2;
        course_name := course_names[course_index];
        exam_status := CASE
            WHEN course_index <= 12 THEN 2
            WHEN course_index <= 18 THEN 1
            ELSE 0
        END;
        start_at := CASE
            WHEN exam_status = 2 THEN NOW() - (course_index || ' days')::interval
            WHEN exam_status = 1 THEN NOW() + ((course_index % 3) || ' hours')::interval
            ELSE NOW() + ((course_index + 2) || ' days')::interval
        END;
        end_at := CASE
            WHEN exam_status = 2 THEN start_at + INTERVAL '2 hours'
            WHEN exam_status = 1 THEN start_at + INTERVAL '2 hours'
            ELSE start_at + INTERVAL '2 days'
        END;

        INSERT INTO exams (
            title,
            description,
            course_id,
            creator_id,
            start_time,
            end_time,
            duration,
            total_score,
            status,
            needs_grading,
            allowed_platforms,
            strict_mode,
            max_switch_count,
            fullscreen_required,
            create_time
        )
        VALUES (
            course_name || CASE
                WHEN exam_status = 2 THEN '期中考试'
                WHEN exam_status = 1 THEN '单元测验'
                ELSE '期末模拟'
            END,
            course_name || '自动生成的考试数据，用于演示完整考试流程。',
            current_course_id,
            current_teacher_id,
            start_at,
            end_at,
            120,
            100,
            exam_status,
            TRUE,
            'both',
            FALSE,
            6,
            FALSE,
            NOW() - ((course_index + 2) || ' days')::interval
        )
        RETURNING id INTO current_exam_id;

        FOR question_id IN
            SELECT q.id
            FROM questions q
            JOIN question_bank_questions qbq ON qbq.question_id = q.id
            JOIN question_banks qb ON qb.id = qbq.bank_id
            WHERE qb.name = course_name || '基础题库'
            ORDER BY q.id
            LIMIT 10
        LOOP
            INSERT INTO exam_questions (exam_id, question_id, score, sequence)
            VALUES (
                current_exam_id,
                question_id,
                10,
                (SELECT COALESCE(MAX(eq.sequence), 0) + 1 FROM exam_questions eq WHERE eq.exam_id = current_exam_id)
            );
        END LOOP;

        IF exam_status = 2 THEN
            SELECT ARRAY(
                SELECT student_id
                FROM course_selections
                WHERE course_id = current_course_id
                ORDER BY student_id
                LIMIT 12
            ) INTO enrolled_students;

            FOREACH current_student_id IN ARRAY enrolled_students LOOP
                score_value := 55 + ((current_student_id + current_course_id) % 41);
                INSERT INTO exam_submissions (
                    exam_id,
                    user_id,
                    answers,
                    submit_detail,
                    submit_score,
                    status,
                    start_time,
                    submit_time,
                    switch_count,
                    proctoring_data
                )
                VALUES (
                    current_exam_id,
                    current_student_id,
                    jsonb_build_object('demo', 'seeded'),
                    jsonb_build_object(
                        'questionScores', jsonb_build_object('summary', score_value),
                        'comment', '示例成绩数据'
                    ),
                    score_value,
                    2,
                    start_at,
                    end_at - INTERVAL '5 minutes',
                    current_student_id % 3,
                    CASE current_student_id % 3
                        WHEN 0 THEN jsonb_build_object(
                            'events', jsonb_build_array(),
                            'switchCount', 0
                        )
                        WHEN 1 THEN jsonb_build_object(
                            'events', jsonb_build_array(
                                jsonb_build_object(
                                    'timestamp', (end_at - INTERVAL '35 minutes')::text,
                                    'type', 'blur',
                                    'detail', '考试过程中短暂离开考试窗口'
                                )
                            ),
                            'switchCount', 1
                        )
                        ELSE jsonb_build_object(
                            'events', jsonb_build_array(
                                jsonb_build_object(
                                    'timestamp', (end_at - INTERVAL '42 minutes')::text,
                                    'type', 'blur',
                                    'detail', '切换到其他应用后返回'
                                ),
                                jsonb_build_object(
                                    'timestamp', (end_at - INTERVAL '18 minutes')::text,
                                    'type', 'tab_switch',
                                    'detail', '再次切出考试页面'
                                )
                            ),
                            'switchCount', 2
                        )
                    END
                );
            END LOOP;
        END IF;
    END LOOP;

    INSERT INTO notifications (user_id, type, title, content, related_id, is_read, create_time)
    SELECT
        u.id,
        CASE
            WHEN u.role = 'teacher' THEN 'SYSTEM_ANNOUNCEMENT'
            WHEN u.role = 'student' THEN 'EXAM_REMINDER'
            ELSE 'SYSTEM_ANNOUNCEMENT'
        END,
        CASE
            WHEN u.role = 'teacher' THEN '教学任务提醒'
            WHEN u.role = 'student' THEN '考试安排更新'
            ELSE '系统运行日报'
        END,
        CASE
            WHEN u.role = 'teacher' THEN '请及时检查课程考试发布情况，并关注待批改的主观题。'
            WHEN u.role = 'student' THEN '您有新的课程考试安排，请尽快查看并确认考试时间。'
            ELSE '演示环境数据已重建，当前数据库包含大量中文示例数据。'
        END,
        NULL,
        FALSE,
        NOW() - ((u.id % 15) || ' hours')::interval
    FROM users u
    WHERE (u.role = 'teacher' AND u.id <= 21)
       OR (u.role = 'student' AND u.id <= 120)
       OR u.role = 'admin';

    INSERT INTO ai_config (config_key, config_value, description, create_time, update_time) VALUES
    ('system_prompt', '你是在线考试系统的智能阅卷助手。请严格依据题目、参考答案、学生答案和满分进行评分。
评分原则：
1. 只评价答案内容，不因措辞风格、字数多少或与参考答案表述不同而扣分；语义等价应给分。
2. 按知识点覆盖度、逻辑完整性、关键步骤或概念准确性分配分数；不得给负分，不得超过满分。
3. 学生答案为空、明显无关或仅重复题干时给 0 分。
4. 若参考答案不完整，以题干要求和学科常识补充判断，但必须保持保守。
5. explanation 使用中文，简洁说明给分依据；strengths 和 improvements 分别给出 1-3 条。
6. 只能返回 JSON 对象，不要 Markdown，不要代码块，不要额外文本。
返回格式：
{"suggestedScore":0,"explanation":"...","strengths":["..."],"improvements":["..."]}', 'AI 判题系统提示词', NOW(), NOW()),
    ('api_key', '', 'OpenAI 兼容接口密钥，可选', NOW(), NOW()),
    ('model_name', 'kimi-k2.6', '当前使用的模型名称', NOW(), NOW()),
    ('temperature', '0.3', '模型温度参数', NOW(), NOW()),
    ('max_tokens', '500', '最大响应 Token 数', NOW(), NOW()),
    ('api_base_url', 'https://api.moonshot.ai/v1', 'OpenAI 兼容接口基础地址', NOW(), NOW()),
    ('ai_batch_concurrency', '5', 'AI 批量评分并发数', NOW(), NOW()),
    ('provider_mode', 'preset', '模型来源模式：preset/custom', NOW(), NOW()),
    ('provider_preset', 'kimi_default', '默认模型预设', NOW(), NOW());
END $$;

COMMIT;
