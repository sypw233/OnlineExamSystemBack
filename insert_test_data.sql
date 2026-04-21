-- 额外测试数据插入脚本，使用子查询避免硬编码ID

-- 追加的用户
INSERT INTO users (username, password, real_name, role, email) VALUES
('teacher_liu', '123456', '刘老师', 'teacher', 'liu@example.com'),
('teacher_chen', '123456', '陈老师', 'teacher', 'chen@example.com'),
('student_zh', '123456', '张三', 'student', 'zhangsan@example.com'),
('student_li', '123456', '李四', 'student', 'lisi@example.com'),
('student_wa', '123456', '王五', 'student', 'wangwu@example.com')
ON CONFLICT (username) DO NOTHING;

-- 追加的课程
INSERT INTO courses (course_name, teacher_id, description) VALUES
('计算机科学导论', (SELECT id FROM users WHERE username='teacher_liu'), '计算机科学入门课程'),
('数据结构与算法', (SELECT id FROM users WHERE username='teacher_chen'), '核心计算机专业科'),
('人工智能基础', (SELECT id FROM users WHERE username='teacher_chen'), '带你走进AI的世界');

-- 追加的选课记录
INSERT INTO course_selections (student_id, course_id) VALUES
((SELECT id FROM users WHERE username='student_zh'), (SELECT id FROM courses WHERE course_name='计算机科学导论')),
((SELECT id FROM users WHERE username='student_zh'), (SELECT id FROM courses WHERE course_name='数据结构与算法')),
((SELECT id FROM users WHERE username='student_li'), (SELECT id FROM courses WHERE course_name='计算机科学导论')),
((SELECT id FROM users WHERE username='student_li'), (SELECT id FROM courses WHERE course_name='人工智能基础')),
((SELECT id FROM users WHERE username='student_wa'), (SELECT id FROM courses WHERE course_name='数据结构与算法')),
((SELECT id FROM users WHERE username='student_wa'), (SELECT id FROM courses WHERE course_name='人工智能基础'))
ON CONFLICT DO NOTHING;

-- 追加的试题
INSERT INTO questions (content, type, options, answer, analysis, difficulty, category, creator_id) VALUES
('计算机的“大脑”通常是指硬件中的哪一个部件？', 'single', '["内存", "硬盘", "CPU", "显卡"]', 'C', 'CPU即中央处理器，是计算机的运算核心和控制核心。', 'easy', '计算机基础', (SELECT id FROM users WHERE username='teacher_liu')),
('以下哪些属于面向对象编程语言？', 'multiple', '["Java", "C++", "C", "Python"]', 'A,B,D', 'C语言是面向过程的语言', 'medium', '编程语言', (SELECT id FROM users WHERE username='teacher_chen')),
('HTML是Hyper Text Markup Language的缩写。', 'true_false', NULL, 'True', 'HTML全称为超文本标记语言', 'easy', '前端开发', (SELECT id FROM users WHERE username='teacher_liu')),
('常见的数据结构中，先进先出的数据结构被称为____。', 'fill_blank', NULL, '队列', '队列(Queue)是一种先进先出(FIFO)的数据结构。', 'easy', '数据结构', (SELECT id FROM users WHERE username='teacher_chen')),
('请简述TCP和UDP的区别。', 'short_answer', NULL, 'TCP面向连接，可靠；UDP面向无连接。', '...', 'medium', '计算机网络', (SELECT id FROM users WHERE username='teacher_chen'));

-- 追加的题库
INSERT INTO question_banks (name, description, creator_id) VALUES
('计算机基础题库-1', '适合大一新生的基础题目汇总', (SELECT id FROM users WHERE username='teacher_liu')),
('数据结构期末突击题库-1', '精选100道数据结构考题', (SELECT id FROM users WHERE username='teacher_chen'));

-- 关联试题 (使用刚才插入题目的部分内容匹配)
INSERT INTO question_bank_questions (bank_id, question_id) VALUES
((SELECT id FROM question_banks WHERE name='计算机基础题库-1'), (SELECT id FROM questions WHERE content LIKE '计算机的“大脑”通常是指硬件中的哪一个部件？' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='计算机基础题库-1'), (SELECT id FROM questions WHERE content LIKE 'HTML是Hyper Text Markup Language的缩写。' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='数据结构期末突击题库-1'), (SELECT id FROM questions WHERE content LIKE '以下哪些属于面向对象编程语言？' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='数据结构期末突击题库-1'), (SELECT id FROM questions WHERE content LIKE '常见的数据结构中，先进先出的数据结构被称为____。' LIMIT 1)),
((SELECT id FROM question_banks WHERE name='数据结构期末突击题库-1'), (SELECT id FROM questions WHERE content LIKE '请简述TCP和UDP的区别。' LIMIT 1))
ON CONFLICT DO NOTHING;

-- 追加的考试安排
INSERT INTO exams (title, description, course_id, creator_id, start_time, end_time, total_score, status, needs_grading) VALUES
('计算机导论测试', '闭卷', (SELECT id FROM courses WHERE course_name='计算机科学导论' LIMIT 1), (SELECT id FROM users WHERE username='teacher_liu'), NOW(), NOW() + INTERVAL '2 hours', 100, 1, FALSE),
('数据结构期中', '闭卷', (SELECT id FROM courses WHERE course_name='数据结构与算法' LIMIT 1), (SELECT id FROM users WHERE username='teacher_chen'), NOW(), NOW() + INTERVAL '1 hours', 100, 1, TRUE);

-- 考试包含的题目
INSERT INTO exam_questions (exam_id, question_id, score, sequence) VALUES
((SELECT id FROM exams WHERE title='计算机导论测试'), (SELECT id FROM questions WHERE content LIKE '计算机的“大脑”%' LIMIT 1), 50, 1),
((SELECT id FROM exams WHERE title='计算机导论测试'), (SELECT id FROM questions WHERE content LIKE 'HTML是%' LIMIT 1), 50, 2),
((SELECT id FROM exams WHERE title='数据结构期中'), (SELECT id FROM questions WHERE content LIKE '以下哪些属于%' LIMIT 1), 30, 1),
((SELECT id FROM exams WHERE title='数据结构期中'), (SELECT id FROM questions WHERE content LIKE '常见的数据%' LIMIT 1), 30, 2),
((SELECT id FROM exams WHERE title='数据结构期中'), (SELECT id FROM questions WHERE content LIKE '请简述TCP和UDP%' LIMIT 1), 40, 3)
ON CONFLICT DO NOTHING;
