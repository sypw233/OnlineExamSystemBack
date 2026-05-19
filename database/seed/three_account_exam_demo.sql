-- Three-account focused demo data.
-- Accounts: admin / teacher1 / student, password: 123456.
-- Safe to run repeatedly. It replaces only the demo courses, banks, exams,
-- questions, submissions and notifications created by this script.

BEGIN;

DO $$
DECLARE
    password_hash CONSTANT TEXT := '$2a$10$Tv6Jxlg8QAdgP5MV8.4sTOoiv2Dm36WEiaanikWcXkuv49wDPhXoW';
    admin_user_id BIGINT;
    teacher_user_id BIGINT;
    student_user_id BIGINT;
    current_course_id BIGINT;
    current_bank_id BIGINT;
    current_exam_id BIGINT;
    current_question_id BIGINT;
    course_index INT;
    question_no INT;
    exam_index INT;
    type_slot INT;
    sequence_no INT;
    question_score INT;
    gained_score INT;
    total_score INT;
    question_type TEXT;
    difficulty TEXT;
    topic TEXT;
    course_name TEXT;
    bank_name TEXT;
    options_json JSONB;
    answer_text TEXT;
    analysis_text TEXT;
    answers_json JSONB;
    scores_json JSONB;
    exam_start TIMESTAMP;
    exam_end TIMESTAMP;
    future_exam_id BIGINT;
    active_exam_id BIGINT;
    ended_exam_id BIGINT;
    course_names TEXT[] := ARRAY[
        '三账号测试-数据结构与算法实训',
        '三账号测试-数据库系统项目实践',
        '三账号测试-软件工程与测试实践'
    ];
    course_descriptions TEXT[] := ARRAY[
        '围绕真实业务中的数据组织、检索、排序与复杂度分析建立练习场景。',
        '围绕订单、库存、权限、报表等项目场景设计数据库建模与 SQL 练习。',
        '围绕需求澄清、迭代管理、缺陷分析和自动化测试建立综合训练。'
    ];
    bank_names TEXT[] := ARRAY[
        '三账号测试-数据结构综合题库',
        '三账号测试-数据库系统综合题库',
        '三账号测试-软件工程综合题库'
    ];
    ds_topics TEXT[] := ARRAY[
        '数组与链表选型', '栈和队列的业务建模', '哈希表冲突处理', '二叉树遍历',
        '图搜索与最短路径', '排序算法稳定性', '递归边界设计', '动态规划状态转移',
        '缓存淘汰策略', '算法复杂度评估'
    ];
    db_topics TEXT[] := ARRAY[
        '订单表范式设计', '索引选择性分析', '事务隔离级别', '慢查询定位',
        '多表连接查询', '权限表建模', '库存扣减一致性', '读写分离风险',
        '数据备份恢复', '报表聚合优化'
    ];
    se_topics TEXT[] := ARRAY[
        '用户故事拆分', '需求变更评审', '接口契约测试', '缺陷复现路径',
        '持续集成流水线', '测试用例分层', '代码评审标准', '发布回滚方案',
        '风险矩阵评估', '性能瓶颈定位'
    ];
BEGIN
    INSERT INTO users (username, password, nickname, real_name, role, email, status, create_time)
    VALUES ('admin', password_hash, '系统管理员', '系统管理员', 'admin', 'admin@example.com', 1, NOW())
    ON CONFLICT (username) DO UPDATE SET
        password = EXCLUDED.password,
        nickname = EXCLUDED.nickname,
        real_name = EXCLUDED.real_name,
        role = EXCLUDED.role,
        email = EXCLUDED.email,
        status = 1;

    INSERT INTO users (username, password, nickname, real_name, role, email, status, create_time)
    VALUES ('teacher1', password_hash, '张老师', '张明远', 'teacher', 'teacher1@example.com', 1, NOW())
    ON CONFLICT (username) DO UPDATE SET
        password = EXCLUDED.password,
        nickname = EXCLUDED.nickname,
        real_name = EXCLUDED.real_name,
        role = EXCLUDED.role,
        email = EXCLUDED.email,
        status = 1;

    INSERT INTO users (username, password, nickname, real_name, role, email, status, create_time)
    VALUES ('student', password_hash, '陈同学', '陈思远', 'student', 'student@example.com', 1, NOW())
    ON CONFLICT (username) DO UPDATE SET
        password = EXCLUDED.password,
        nickname = EXCLUDED.nickname,
        real_name = EXCLUDED.real_name,
        role = EXCLUDED.role,
        email = EXCLUDED.email,
        status = 1;

    SELECT id INTO admin_user_id FROM users WHERE username = 'admin';
    SELECT id INTO teacher_user_id FROM users WHERE username = 'teacher1';
    SELECT id INTO student_user_id FROM users WHERE username = 'student';

    DELETE FROM notifications
    WHERE notifications.user_id IN (admin_user_id, teacher_user_id, student_user_id);

    DELETE FROM exam_submissions
    WHERE exam_submissions.user_id = student_user_id
       OR exam_submissions.exam_id IN (
            SELECT e.id
            FROM exams e
            WHERE e.creator_id = teacher_user_id
               OR e.course_id IN (SELECT c0.id FROM courses c0 WHERE c0.teacher_id = teacher_user_id)
       );

    DELETE FROM exam_questions
    WHERE exam_questions.exam_id IN (
            SELECT e.id
            FROM exams e
            WHERE e.creator_id = teacher_user_id
               OR e.course_id IN (SELECT c0.id FROM courses c0 WHERE c0.teacher_id = teacher_user_id)
       )
       OR exam_questions.question_id IN (
            SELECT q0.id FROM questions q0 WHERE q0.creator_id = teacher_user_id
       );

    DELETE FROM exams
    WHERE exams.creator_id = teacher_user_id
       OR exams.course_id IN (SELECT c0.id FROM courses c0 WHERE c0.teacher_id = teacher_user_id);

    DELETE FROM course_selections
    WHERE course_selections.student_id = student_user_id
       OR course_selections.course_id IN (SELECT c0.id FROM courses c0 WHERE c0.teacher_id = teacher_user_id);

    DELETE FROM question_bank_questions
    WHERE question_bank_questions.bank_id IN (
            SELECT qb0.id FROM question_banks qb0 WHERE qb0.creator_id = teacher_user_id
       )
       OR question_bank_questions.question_id IN (
            SELECT q0.id FROM questions q0 WHERE q0.creator_id = teacher_user_id
       );

    DELETE FROM question_banks WHERE question_banks.creator_id = teacher_user_id;
    DELETE FROM questions WHERE questions.creator_id = teacher_user_id;
    DELETE FROM courses WHERE courses.teacher_id = teacher_user_id;

    FOR course_index IN 1..3 LOOP
        course_name := course_names[course_index];
        bank_name := bank_names[course_index];

        INSERT INTO courses (course_name, teacher_id, description, status, create_time)
        VALUES (course_name, teacher_user_id, course_descriptions[course_index], 1, NOW() - (course_index || ' days')::interval)
        RETURNING id INTO current_course_id;

        INSERT INTO course_selections (student_id, course_id, selection_time)
        VALUES (student_user_id, current_course_id, NOW() - (course_index || ' hours')::interval)
        ON CONFLICT (student_id, course_id) DO NOTHING;

        INSERT INTO question_banks (name, description, creator_id, create_time)
        VALUES (
            bank_name,
            course_name || '专用题库，覆盖单选、多选、判断、填空、简答五类题型。',
            teacher_user_id,
            NOW() - (course_index || ' days')::interval
        )
        RETURNING id INTO current_bank_id;

        FOR question_no IN 1..30 LOOP
            topic := CASE course_index
                WHEN 1 THEN ds_topics[((question_no - 1) % array_length(ds_topics, 1)) + 1]
                WHEN 2 THEN db_topics[((question_no - 1) % array_length(db_topics, 1)) + 1]
                ELSE se_topics[((question_no - 1) % array_length(se_topics, 1)) + 1]
            END;

            type_slot := question_no % 10;
            question_type := CASE
                WHEN type_slot BETWEEN 1 AND 4 THEN 'single'
                WHEN type_slot IN (5, 6) THEN 'multiple'
                WHEN type_slot IN (7, 8) THEN 'true_false'
                WHEN type_slot = 9 THEN 'fill_blank'
                ELSE 'short_answer'
            END;
            difficulty := CASE
                WHEN question_no % 3 = 1 THEN 'easy'
                WHEN question_no % 3 = 2 THEN 'medium'
                ELSE 'hard'
            END;

            IF question_type = 'single' THEN
                options_json := jsonb_build_array(
                    'A. 直接套用模板并忽略上下文',
                    'B. 先识别约束、输入规模和风险点',
                    'C. 只关注界面展示效果',
                    'D. 推迟所有验证到上线后'
                );
                answer_text := 'B';
                analysis_text := '真实项目中应先明确约束和风险，再选择实现方案。';
            ELSIF question_type = 'multiple' THEN
                options_json := jsonb_build_array(
                    'A. 明确输入输出边界',
                    'B. 补充异常场景验证',
                    'C. 删除关键日志',
                    'D. 记录性能或一致性风险'
                );
                answer_text := 'A,B,D';
                analysis_text := '工程化处理需要覆盖边界、异常和可观测性，不能删除关键诊断信息。';
            ELSIF question_type = 'true_false' THEN
                options_json := NULL;
                answer_text := CASE WHEN question_no % 4 = 0 THEN 'false' ELSE 'true' END;
                analysis_text := '判断题重点考查对场景限制和前提条件的理解。';
            ELSIF question_type = 'fill_blank' THEN
                options_json := NULL;
                answer_text := CASE course_index
                    WHEN 1 THEN '时间复杂度'
                    WHEN 2 THEN '事务'
                    ELSE '回归测试'
                END;
                analysis_text := '填空题要求准确写出核心术语。';
            ELSE
                options_json := NULL;
                answer_text := '应从业务目标、约束条件、关键步骤、验证方式和风险控制五个方面展开说明。';
                analysis_text := '主观题重点考查是否能结合项目场景完整说明分析过程。';
            END IF;

            INSERT INTO questions (content, type, options, answer, analysis, difficulty, category, creator_id, create_time)
            VALUES (
                CASE question_type
                    WHEN 'single' THEN format('在“%s”场景中，处理“%s”问题时，最合理的第一步是什么？', course_name, topic)
                    WHEN 'multiple' THEN format('在“%s”场景中，为保证“%s”的实现质量，哪些做法是合理的？', course_name, topic)
                    WHEN 'true_false' THEN format('判断：在“%s”的“%s”场景中，只要功能能运行，就可以忽略边界条件验证。', course_name, topic)
                    WHEN 'fill_blank' THEN format('在“%s”的“%s”场景中，常用的核心概念是____。', course_name, topic)
                    ELSE format('结合“%s”的“%s”场景，说明你会如何分析问题、设计方案并验证结果。', course_name, topic)
                END,
                question_type,
                options_json,
                answer_text,
                analysis_text,
                difficulty,
                course_name,
                teacher_user_id,
                NOW() - ((question_no % 12) || ' hours')::interval
            )
            RETURNING id INTO current_question_id;

            INSERT INTO question_bank_questions (bank_id, question_id)
            VALUES (current_bank_id, current_question_id);
        END LOOP;

        FOR exam_index IN 1..3 LOOP
            IF exam_index = 1 THEN
                exam_start := NOW() + (course_index || ' days')::interval;
                exam_end := exam_start + INTERVAL '90 minutes';
            ELSIF exam_index = 2 THEN
                exam_start := NOW() - INTERVAL '30 minutes';
                exam_end := NOW() + INTERVAL '90 minutes';
            ELSE
                exam_start := NOW() - ((course_index + 6) || ' days')::interval;
                exam_end := exam_start + INTERVAL '90 minutes';
            END IF;

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
                course_name || CASE exam_index
                    WHEN 1 THEN '-预约测验（未到时间）'
                    WHEN 2 THEN '-课堂限时测验'
                    ELSE '-阶段综合考试'
                END,
                '三账号测试考试：题型比例为单选40%、多选20%、判断20%、填空10%、简答10%，总分100分。',
                current_course_id,
                teacher_user_id,
                exam_start,
                exam_end,
                90,
                100,
                CASE WHEN exam_index = 3 THEN 2 ELSE 1 END,
                TRUE,
                'both',
                TRUE,
                3,
                FALSE,
                NOW() - (exam_index || ' hours')::interval
            )
            RETURNING id INTO current_exam_id;

            sequence_no := 0;
            FOR current_question_id, question_type IN
                SELECT q.id, q.type
                FROM questions q
                JOIN question_bank_questions qbq ON qbq.question_id = q.id
                WHERE qbq.bank_id = current_bank_id
                ORDER BY q.id
                OFFSET ((exam_index - 1) * 5)
                LIMIT 20
            LOOP
                sequence_no := sequence_no + 1;
                question_score := CASE question_type
                    WHEN 'single' THEN 3
                    WHEN 'multiple' THEN 5
                    WHEN 'true_false' THEN 4
                    ELSE 10
                END;

                INSERT INTO exam_questions (exam_id, question_id, score, sequence)
                VALUES (current_exam_id, current_question_id, question_score, sequence_no);
            END LOOP;

            IF exam_index = 3 THEN
                answers_json := '{}'::jsonb;
                scores_json := '{}'::jsonb;
                total_score := 0;

                FOR current_question_id, question_type, question_score IN
                    SELECT q.id, q.type, eq.score
                    FROM exam_questions eq
                    JOIN questions q ON q.id = eq.question_id
                    WHERE eq.exam_id = current_exam_id
                    ORDER BY eq.sequence
                LOOP
                    answers_json := answers_json || jsonb_build_object(
                        current_question_id::text,
                        CASE question_type
                            WHEN 'single' THEN 'B'
                            WHEN 'multiple' THEN 'A,B,D'
                            WHEN 'true_false' THEN 'true'
                            WHEN 'fill_blank' THEN CASE course_index WHEN 1 THEN '时间复杂度' WHEN 2 THEN '事务' ELSE '回归测试' END
                            ELSE '我会先确认业务目标和约束条件，再拆分关键步骤，补充边界用例和异常路径，最后通过数据或测试结果验证方案是否达标。'
                        END
                    );
                    gained_score := CASE
                        WHEN question_type IN ('single', 'multiple', 'fill_blank') THEN question_score
                        WHEN question_type = 'true_false' THEN question_score - CASE WHEN current_question_id % 3 = 0 THEN 2 ELSE 0 END
                        ELSE question_score - 2
                    END;
                    total_score := total_score + gained_score;
                    scores_json := scores_json || jsonb_build_object(current_question_id::text, gained_score);
                END LOOP;

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
                    student_user_id,
                    answers_json,
                    jsonb_build_object(
                        'questionScores', scores_json,
                        'comment', '三账号测试自动生成批改记录：客观题已自动计分，主观题按参考答案给出人工分。'
                    ),
                    total_score,
                    2,
                    exam_start,
                    exam_end - INTERVAL '8 minutes',
                    course_index - 1,
                    jsonb_build_object(
                        'events',
                        CASE WHEN course_index = 1 THEN jsonb_build_array()
                             ELSE jsonb_build_array(jsonb_build_object(
                                 'timestamp', (exam_start + INTERVAL '20 minutes')::text,
                                 'type', 'blur',
                                 'detail', '三账号测试：学生短暂切出考试页面后返回'
                             ))
                        END,
                        'switchCount',
                        course_index - 1
                    )
                );
            END IF;
        END LOOP;
    END LOOP;

    SELECT id INTO future_exam_id FROM exams WHERE title = '三账号测试-数据结构与算法实训-预约测验（未到时间）' LIMIT 1;
    SELECT id INTO active_exam_id FROM exams WHERE title = '三账号测试-数据结构与算法实训-课堂限时测验' LIMIT 1;
    SELECT id INTO ended_exam_id FROM exams WHERE title = '三账号测试-数据结构与算法实训-阶段综合考试' LIMIT 1;

    INSERT INTO notifications (user_id, type, title, content, related_id, is_read, create_time)
    VALUES
        (student_user_id, 'EXAM_PUBLISHED', '三账号测试-考试已发布', '数据结构与算法实训预约测验已发布，但考试时间未到，请在开考后进入。', future_exam_id, FALSE, NOW() - INTERVAL '9 minutes'),
        (student_user_id, 'EXAM_REMINDER', '三账号测试-限时测验进行中', '课堂限时测验已经开始，请在考试结束前完成提交。', active_exam_id, FALSE, NOW() - INTERVAL '8 minutes'),
        (student_user_id, 'GRADE_RELEASED', '三账号测试-阶段成绩已发布', '阶段综合考试已经完成批改，可以在考试历史中查看成绩和详情。', ended_exam_id, FALSE, NOW() - INTERVAL '7 minutes'),
        (student_user_id, 'COURSE_UPDATE', '三账号测试-课程资料更新', '三门测试课程均已加入当前学生账号，可用于完整流程验证。', NULL, FALSE, NOW() - INTERVAL '6 minutes'),
        (teacher_user_id, 'EXAM_PUBLISHED', '三账号测试-教师考试发布提醒', '当前已生成9场考试，其中3场未到考试时间、3场正在进行、3场已结束。', active_exam_id, FALSE, NOW() - INTERVAL '5 minutes'),
        (teacher_user_id, 'SYSTEM_ANNOUNCEMENT', '三账号测试-题库已生成', '已为教师账号生成3个题库，每个题库包含30道拟真题目。', NULL, FALSE, NOW() - INTERVAL '4 minutes'),
        (teacher_user_id, 'GRADE_RELEASED', '三账号测试-批改样例可查看', '已结束考试包含一份学生提交和批改记录，可用于教师阅卷页面验证。', ended_exam_id, FALSE, NOW() - INTERVAL '3 minutes'),
        (admin_user_id, 'SYSTEM_ANNOUNCEMENT', '三账号测试-数据集重建完成', '三账号测试课程、题库、考试、通知和提交记录已经重建。', NULL, FALSE, NOW() - INTERVAL '2 minutes'),
        (admin_user_id, 'COURSE_UPDATE', '三账号测试-课程状态正常', '3门课程均处于启用状态，学生账号已经完成选课。', NULL, FALSE, NOW() - INTERVAL '90 seconds'),
        (admin_user_id, 'EXAM_CHANGED', '三账号测试-时间状态覆盖完成', '测试数据覆盖未到时间、进行中、已结束三类考试状态。', future_exam_id, FALSE, NOW() - INTERVAL '30 seconds');
END $$;

COMMIT;
