BEGIN;

CREATE OR REPLACE FUNCTION demo_replace_course_names(input_text text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        REPLACE(COALESCE(input_text, ''),
                                            'Advanced Mathematics', '高等数学'),
                                        'College English', '大学英语'),
                                    'Java Programming', 'Java 程序设计'),
                                'Data Structures', '数据结构'),
                            'Database Systems', '数据库系统'),
                        'Operating Systems', '操作系统'),
                    'Computer Networks', '计算机网络'),
                'Software Engineering', '软件工程'),
            'AI Introduction', '人工智能导论'),
        'Mobile App Development', '移动应用开发');
$$;

UPDATE users
SET real_name = CASE
    WHEN username = 'admin' THEN '系统管理员'
    WHEN username = 'student' THEN '演示学生'
    WHEN role = 'teacher' THEN '教师' || LPAD(REGEXP_REPLACE(username, '^teacher', ''), 2, '0')
    WHEN role = 'student' THEN '学生' || LPAD(REGEXP_REPLACE(username, '^student', ''), 3, '0')
    ELSE real_name
END;

UPDATE courses
SET course_name = CASE id
    WHEN 1 THEN '高等数学'
    WHEN 2 THEN '大学英语'
    WHEN 3 THEN 'Java 程序设计'
    WHEN 4 THEN '数据结构'
    WHEN 5 THEN '数据库系统'
    WHEN 6 THEN '操作系统'
    WHEN 7 THEN '计算机网络'
    WHEN 8 THEN '软件工程'
    WHEN 9 THEN '人工智能导论'
    WHEN 10 THEN '移动应用开发'
    ELSE course_name
END,
description = CASE id
    WHEN 1 THEN '高等数学课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 2 THEN '大学英语课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 3 THEN 'Java 程序设计课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 4 THEN '数据结构课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 5 THEN '数据库系统课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 6 THEN '操作系统课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 7 THEN '计算机网络课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 8 THEN '软件工程课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 9 THEN '人工智能导论课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    WHEN 10 THEN '移动应用开发课程演示数据，用于课程、题库、考试与阅卷流程测试。'
    ELSE description
END;

UPDATE question_banks
SET name = REPLACE(demo_replace_course_names(name), 'Bank ', '题库'),
    description = REPLACE(demo_replace_course_names(description), 'with 100 questions.', '演示题库，包含 100 道题目。');

UPDATE exams
SET title = CASE course_id
    WHEN 1 THEN '高等数学综合测试'
    WHEN 2 THEN '大学英语综合测试'
    WHEN 3 THEN 'Java 程序设计综合测试'
    WHEN 4 THEN '数据结构综合测试'
    WHEN 5 THEN '数据库系统综合测试'
    WHEN 6 THEN '操作系统综合测试'
    WHEN 7 THEN '计算机网络综合测试'
    WHEN 8 THEN '软件工程综合测试'
    WHEN 9 THEN '人工智能导论综合测试'
    WHEN 10 THEN '移动应用开发综合测试'
    ELSE title
END,
description = CASE course_id
    WHEN 1 THEN '高等数学课程已发布的综合测试。'
    WHEN 2 THEN '大学英语课程已发布的综合测试。'
    WHEN 3 THEN 'Java 程序设计课程已发布的综合测试。'
    WHEN 4 THEN '数据结构课程已发布的综合测试。'
    WHEN 5 THEN '数据库系统课程已发布的综合测试。'
    WHEN 6 THEN '操作系统课程已发布的综合测试。'
    WHEN 7 THEN '计算机网络课程已发布的综合测试。'
    WHEN 8 THEN '软件工程课程已发布的综合测试。'
    WHEN 9 THEN '人工智能导论课程已发布的综合测试。'
    WHEN 10 THEN '移动应用开发课程已发布的综合测试。'
    ELSE description
END;

UPDATE questions
SET category = CASE category
    WHEN 'Math' THEN '数学'
    WHEN 'English' THEN '英语'
    WHEN 'Java' THEN '编程'
    WHEN 'DataStructure' THEN '数据结构'
    WHEN 'Database' THEN '数据库'
    WHEN 'OS' THEN '操作系统'
    WHEN 'Network' THEN '计算机网络'
    WHEN 'SE' THEN '软件工程'
    WHEN 'AI' THEN '人工智能'
    WHEN 'Mobile' THEN '移动开发'
    ELSE category
END,
content = CASE type
    WHEN 'single' THEN
        demo_replace_course_names(
            CASE category
                WHEN 'Math' THEN 'Advanced Mathematics'
                WHEN 'English' THEN 'College English'
                WHEN 'Java' THEN 'Java Programming'
                WHEN 'DataStructure' THEN 'Data Structures'
                WHEN 'Database' THEN 'Database Systems'
                WHEN 'OS' THEN 'Operating Systems'
                WHEN 'Network' THEN 'Computer Networks'
                WHEN 'SE' THEN 'Software Engineering'
                WHEN 'AI' THEN 'AI Introduction'
                WHEN 'Mobile' THEN 'Mobile App Development'
                ELSE 'Course'
            END
        ) || '单选题 ' || id || '：下列哪一项最符合核心概念？'
    WHEN 'multiple' THEN
        demo_replace_course_names(
            CASE category
                WHEN 'Math' THEN 'Advanced Mathematics'
                WHEN 'English' THEN 'College English'
                WHEN 'Java' THEN 'Java Programming'
                WHEN 'DataStructure' THEN 'Data Structures'
                WHEN 'Database' THEN 'Database Systems'
                WHEN 'OS' THEN 'Operating Systems'
                WHEN 'Network' THEN 'Computer Networks'
                WHEN 'SE' THEN 'Software Engineering'
                WHEN 'AI' THEN 'AI Introduction'
                WHEN 'Mobile' THEN 'Mobile App Development'
                ELSE 'Course'
            END
        ) || '多选题 ' || id || '：下列哪些表述正确？'
    WHEN 'true_false' THEN
        demo_replace_course_names(
            CASE category
                WHEN 'Math' THEN 'Advanced Mathematics'
                WHEN 'English' THEN 'College English'
                WHEN 'Java' THEN 'Java Programming'
                WHEN 'DataStructure' THEN 'Data Structures'
                WHEN 'Database' THEN 'Database Systems'
                WHEN 'OS' THEN 'Operating Systems'
                WHEN 'Network' THEN 'Computer Networks'
                WHEN 'SE' THEN 'Software Engineering'
                WHEN 'AI' THEN 'AI Introduction'
                WHEN 'Mobile' THEN 'Mobile App Development'
                ELSE 'Course'
            END
        ) || '判断题 ' || id || '：该知识点可以用于实际分析。'
    WHEN 'fill_blank' THEN
        demo_replace_course_names(
            CASE category
                WHEN 'Math' THEN 'Advanced Mathematics'
                WHEN 'English' THEN 'College English'
                WHEN 'Java' THEN 'Java Programming'
                WHEN 'DataStructure' THEN 'Data Structures'
                WHEN 'Database' THEN 'Database Systems'
                WHEN 'OS' THEN 'Operating Systems'
                WHEN 'Network' THEN 'Computer Networks'
                WHEN 'SE' THEN 'Software Engineering'
                WHEN 'AI' THEN 'AI Introduction'
                WHEN 'Mobile' THEN 'Mobile App Development'
                ELSE 'Course'
            END
        ) || '填空题 ' || id || '：关键术语是 ____。'
    WHEN 'short_answer' THEN
        demo_replace_course_names(
            CASE category
                WHEN 'Math' THEN 'Advanced Mathematics'
                WHEN 'English' THEN 'College English'
                WHEN 'Java' THEN 'Java Programming'
                WHEN 'DataStructure' THEN 'Data Structures'
                WHEN 'Database' THEN 'Database Systems'
                WHEN 'OS' THEN 'Operating Systems'
                WHEN 'Network' THEN 'Computer Networks'
                WHEN 'SE' THEN 'Software Engineering'
                WHEN 'AI' THEN 'AI Introduction'
                WHEN 'Mobile' THEN 'Mobile App Development'
                ELSE 'Course'
            END
        ) || '简答题 ' || id || '：请说明该知识点的定义、应用场景与注意事项。'
    ELSE content
END,
options = CASE type
    WHEN 'single' THEN '["A. 基础概念", "B. 核心概念", "C. 延伸概念", "D. 错误概念"]'
    WHEN 'multiple' THEN '["A. 基本原理", "B. 实际应用场景", "C. 无关表述", "D. 常见误区"]'
    ELSE options
END,
answer = CASE type
    WHEN 'single' THEN 'B'
    WHEN 'multiple' THEN 'A,B'
    WHEN 'true_false' THEN 'true'
    WHEN 'fill_blank' THEN demo_replace_course_names(
        CASE category
            WHEN 'Math' THEN 'Advanced Mathematics'
            WHEN 'English' THEN 'College English'
            WHEN 'Java' THEN 'Java Programming'
            WHEN 'DataStructure' THEN 'Data Structures'
            WHEN 'Database' THEN 'Database Systems'
            WHEN 'OS' THEN 'Operating Systems'
            WHEN 'Network' THEN 'Computer Networks'
            WHEN 'SE' THEN 'Software Engineering'
            WHEN 'AI' THEN 'AI Introduction'
            WHEN 'Mobile' THEN 'Mobile App Development'
            ELSE ''
        END
    ) || '核心概念'
    WHEN 'short_answer' THEN '请结合课堂内容与实际场景进行说明。'
    ELSE answer
END,
analysis = demo_replace_course_names(
    CASE category
        WHEN 'Math' THEN '题目解析：本题考查 Advanced Mathematics 相关知识点。'
        WHEN 'English' THEN '题目解析：本题考查 College English 相关知识点。'
        WHEN 'Java' THEN '题目解析：本题考查 Java Programming 相关知识点。'
        WHEN 'DataStructure' THEN '题目解析：本题考查 Data Structures 相关知识点。'
        WHEN 'Database' THEN '题目解析：本题考查 Database Systems 相关知识点。'
        WHEN 'OS' THEN '题目解析：本题考查 Operating Systems 相关知识点。'
        WHEN 'Network' THEN '题目解析：本题考查 Computer Networks 相关知识点。'
        WHEN 'SE' THEN '题目解析：本题考查 Software Engineering 相关知识点。'
        WHEN 'AI' THEN '题目解析：本题考查 AI Introduction 相关知识点。'
        WHEN 'Mobile' THEN '题目解析：本题考查 Mobile App Development 相关知识点。'
        ELSE analysis
    END
);

UPDATE notifications
SET title = CASE
    WHEN title = 'Test environment reset' THEN '测试环境已重置'
    ELSE REPLACE(demo_replace_course_names(title), ' Comprehensive Test published', '综合测试已发布')
END,
content = CASE
    WHEN title = 'Test environment reset' THEN '测试环境、演示账号和课程数据已经重新初始化。'
    ELSE REPLACE(
        REPLACE(demo_replace_course_names(content), 'Your course has published an exam: ', '您的课程已发布考试：'),
        ' Comprehensive Test',
        '综合测试'
    )
END;

DROP FUNCTION demo_replace_course_names(text);

COMMIT;
