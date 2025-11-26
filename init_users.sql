-- 更新用户密码为BCrypt加密后的值

-- admin/admin123
UPDATE users SET password = '$2a$10$YQ7nL.SqVLPPmEODunVOHOqHxZGZvZ7C6B2eC7kBx7YtFOJPLNLYG' WHERE username = 'admin';

-- 或者直接插入新的测试用户
INSERT INTO users (username, password, real_name, role, email, status) VALUES
('admin', '$2a$10$YQ7nL.SqVLPPmEODunVOHOqHxZGZvZ7C6B2eC7kBx7YtFOJPLNLYG', '系统管理员', 'admin', 'admin@example.com', 1),
('teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张老师', 'teacher', 'teacher1@example.com', 1),
('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李学生', 'student', 'student1@example.com', 1)
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password;

-- 密码说明：
-- admin123 -> $2a$10$YQ7nL.SqVLPPmEODunVOHOqHxZGZvZ7C6B2eC7kBx7YtFOJPLNLYG
-- 123456   -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
