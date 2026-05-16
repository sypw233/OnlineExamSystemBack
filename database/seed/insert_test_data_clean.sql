-- 全新测试数据，使用唯一的用户名和邮箱

-- 追加的用户
INSERT INTO users (username, password, real_name, role, email) VALUES
('teacher_wang_v2', '123456', '王老师', 'teacher', 'wang_v2@example.com'),
('teacher_zhao_v2', '123456', '赵老师', 'teacher', 'zhao_v2@example.com'),
('student_sun_v2', '123456', '孙同学', 'student', 'sun_v2@example.com'),
('student_zhou_v2', '123456', '周同学', 'student', 'zhou_v2@example.com'),
('student_wu_v2', '123456', '吴同学', 'student', 'wu_v2@example.com')
ON CONFLICT DO NOTHING;

-- 追加的课程
INSERT INTO courses (course_name, teacher_id, description) VALUES
('离散数学', (SELECT id FROM users WHERE username='teacher_wang_v2'), '计算机科学基础课'),
('操作系统', (SELECT id FROM users WHERE username='teacher_zhao_v2'), '理解计算机系统'),
('计算机组成原理', (SELECT id FROM users WHERE username='teacher_zhao_v2'), '硬件基础');

-- 追加的选课记录
INSERT INTO course_selections (student_id, course_id) VALUES
((SELECT id FROM users WHERE username='student_sun_v2'), (SELECT id FROM courses WHERE course_name='离散数学')),
((SELECT id FROM users WHERE username='student_sun_v2'), (SELECT id FROM courses WHERE course_name='操作系统')),
((SELECT id FROM users WHERE username='student_zhou_v2'), (SELECT id FROM courses WHERE course_name='离散数学')),
((SELECT id FROM users WHERE username='student_zhou_v2'), (SELECT id FROM courses WHERE course_name='计算机组成原理')),
((SELECT id FROM users WHERE username='student_wu_v2'), (SELECT id FROM courses WHERE course_name='操作系统')),
((SELECT id FROM users WHERE username='student_wu_v2'), (SELECT id FROM courses WHERE course_name='计算机组成原理'))
ON CONFLICT DO NOTHING;

-- 追加的试题
INSERT INTO questions (content, type, options, answer, analysis, difficulty, category, creator_id) VALUES
('以下哪个公式是错排公式？', 'single', '["D(n)=n!(1-1/1!+...)", "C(n, m)", "P(n, m)", "O(n)"]', 'A', '错排公式特指...', 'easy', '离散数学', (SELECT id FROM users WHERE username='teacher_wang_v2')),
('以下哪些是操作系统的特征？', 'multiple', '["并发", "共享", "虚拟", "异步"]', 'A,B,C,D', '操作系统四大基本特征', 'medium', '操作系统', (SELECT id FROM users WHERE username='teacher_zhao_v2')),
('RAM是只读存储器。', 'true_false', NULL, 'False', 'RAM是随机存取存储器', 'easy', '计算机组成原理', (SELECT id FROM users WHERE username='teacher_zhao_v2')),
('进程的状态主要有就绪态、运行态和____。', 'fill_blank', NULL, '阻塞态', '基本三态模型', 'easy', '操作系统', (SELECT id FROM users WHERE username='teacher_zhao_v2')),
('简述冯诺依曼体系结构。', 'short_answer', NULL, '包含五大部件...', '体系结构核心', 'medium', '计算机组成原理', (SELECT id FROM users WHERE username='teacher_zhao_v2'));

-- 追加的题库
INSERT INTO question_banks (name, description, creator_id) VALUES
('离散数学题库', '基础考核内容', (SELECT id FROM users WHERE username='teacher_wang_v2')),
('操作系统题库', '精选题目', (SELECT id FROM users WHERE username='teacher_zhao_v2')),
('计组题库', '精选题目', (SELECT id FROM users WHERE username='teacher_zhao_v2'));

-- 关联试题 (使用刚才插入题目的部分内容匹配)
INSERT INTO question_bank_questions (bank_id, question_id) VALUES
((SELECT id FROM question_banks WHERE name='离散数学题库'), (SELECT id FROM questions WHERE content LIKE '以下哪个公式是错排公式？' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='操作系统题库'), (SELECT id FROM questions WHERE content LIKE '以下哪些是操作系统的特征？' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='计组题库'), (SELECT id FROM questions WHERE content LIKE 'RAM是只读存储器。' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='操作系统题库'), (SELECT id FROM questions WHERE content LIKE '进程的状态主要有就绪态、运行态和____。' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='计组题库'), (SELECT id FROM questions WHERE content LIKE '简述冯诺依曼体系结构。' LIMIT 1))
ON CONFLICT DO NOTHING;

-- 追加的考试安排
INSERT INTO exams (title, description, course_id, creator_id, start_time, end_time, total_score, status, needs_grading) VALUES
('离散期中测试', '闭卷', (SELECT id FROM courses WHERE course_name='离散数学' LIMIT 1), (SELECT id FROM users WHERE username='teacher_wang_v2'), NOW(), NOW() + INTERVAL '2 hours', 100, 1, FALSE),
('操作系统期中测试', '闭卷', (SELECT id FROM courses WHERE course_name='操作系统' LIMIT 1), (SELECT id FROM users WHERE username='teacher_zhao_v2'), NOW(), NOW() + INTERVAL '1 hours', 100, 1, TRUE);

-- 考试包含的题目
INSERT INTO exam_questions (exam_id, question_id, score, sequence) VALUES
((SELECT id FROM exams WHERE title='离散期中测试'), (SELECT id FROM questions WHERE content LIKE '以下哪个公式是错排%' LIMIT 1), 100, 1),
((SELECT id FROM exams WHERE title='操作系统期中测试'), (SELECT id FROM questions WHERE content LIKE '以下哪些是操作系%' LIMIT 1), 50, 1),
((SELECT id FROM exams WHERE title='操作系统期中测试'), (SELECT id FROM questions WHERE content LIKE '进程的状态%' LIMIT 1), 50, 2)
ON CONFLICT DO NOTHING;
