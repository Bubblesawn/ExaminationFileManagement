SET NAMES utf8mb4;

USE exam_record;

-- 系统管理测试用户：当前登录接口兼容 {noop} 明文占位，后续用户管理接口可改为 {sha256} 哈希。
INSERT INTO sys_user (username, password, real_name, phone, email, status)
SELECT 'admin', '{noop}admin123', '系统管理员', '13800000001', 'admin@example.com', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, password, real_name, phone, email, status)
SELECT 'record_manager', '{noop}record123', '考籍管理员', '13800000002', 'record@example.com', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'record_manager');

INSERT INTO sys_user (username, password, real_name, phone, email, status)
SELECT 'auditor', '{noop}audit123', '业务审核员', '13800000003', 'audit@example.com', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'auditor');

SET @user_admin := (SELECT id FROM sys_user WHERE username = 'admin');
SET @user_record_manager := (SELECT id FROM sys_user WHERE username = 'record_manager');
SET @user_auditor := (SELECT id FROM sys_user WHERE username = 'auditor');

-- 系统角色测试数据。
INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark)
SELECT 'SUPER_ADMIN', '超级管理员', 1, 'ALL', 'ENABLED', '拥有系统全部管理权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark)
SELECT 'RECORD_MANAGER', '考籍管理员', 2, 'ALL', 'ENABLED', '负责考生信息和考籍档案维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'RECORD_MANAGER');

INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark)
SELECT 'BUSINESS_AUDITOR', '业务审核员', 3, 'SELF', 'ENABLED', '负责免考、转考和毕业等业务审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'BUSINESS_AUDITOR');

SET @role_super_admin := (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN');
SET @role_record_manager := (SELECT id FROM sys_role WHERE role_code = 'RECORD_MANAGER');
SET @role_business_auditor := (SELECT id FROM sys_role WHERE role_code = 'BUSINESS_AUDITOR');

INSERT INTO sys_user_role (user_id, role_id)
SELECT @user_admin, @role_super_admin
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = @user_admin AND role_id = @role_super_admin);

INSERT INTO sys_user_role (user_id, role_id)
SELECT @user_record_manager, @role_record_manager
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = @user_record_manager AND role_id = @role_record_manager);

INSERT INTO sys_user_role (user_id, role_id)
SELECT @user_auditor, @role_business_auditor
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = @user_auditor AND role_id = @role_business_auditor);

-- 系统菜单测试数据。
INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '工作台', 'MENU', '/dashboard', 'dashboard/DashboardView', 'dashboard:view', 'Monitor', 1, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'dashboard:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '系统管理', 'CATALOG', '/system', 'system/SystemView', 'system:view', 'Setting', 10, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:view');

SET @menu_system := (SELECT id FROM sys_menu WHERE permission_code = 'system:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT @menu_system, '用户管理', 'MENU', '/system/users', 'system/UserManageView', 'system:user:view', 'User', 1, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:user:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT @menu_system, '角色管理', 'MENU', '/system/roles', 'system/RoleManageView', 'system:role:view', 'UserCog', 2, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:role:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT @menu_system, '菜单管理', 'MENU', '/system/menus', 'system/MenuManageView', 'system:menu:view', 'Menu', 3, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:menu:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT @menu_system, '日志管理', 'MENU', '/system/logs', 'system/LogManageView', 'system:log:view', 'FileClock', 4, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:log:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '考生管理', 'MENU', '/candidates', 'candidate/CandidateListView', 'candidate:view', 'Users', 20, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'candidate:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '考籍档案', 'MENU', '/records', 'record/RecordListView', 'record:view', 'FolderArchive', 30, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'record:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '材料审核', 'MENU', '/materials', 'material/MaterialAuditView', 'material:audit:view', 'Files', 40, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'material:audit:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '免考办理', 'MENU', '/exemption', 'exemption/ExemptionView', 'exemption:view', 'BadgeCheck', 50, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'exemption:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '课程顶替', 'MENU', '/course-replace', 'course/CourseReplaceView', 'course-replace:view', 'Repeat', 60, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'course-replace:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '转入转出', 'MENU', '/transfer', 'transfer/TransferView', 'transfer:view', 'ArrowLeftRight', 70, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'transfer:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '毕业申请', 'MENU', '/graduation', 'graduation/GraduationView', 'graduation:view', 'GraduationCap', 80, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'graduation:view');

INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, permission_code, icon, menu_sort, visible, status)
SELECT 0, '智能辅助', 'MENU', '/ai-assistant', 'ai/AiAssistantView', 'ai:view', 'Bot', 90, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'ai:view');

-- 超级管理员拥有全部菜单权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_super_admin, m.id
FROM sys_menu m
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = @role_super_admin AND rm.menu_id = m.id
);

-- 考籍管理员拥有考生、考籍、材料和智能辅助相关菜单权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_record_manager, m.id
FROM sys_menu m
WHERE m.permission_code IN ('dashboard:view', 'candidate:view', 'record:view', 'material:audit:view', 'ai:view')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = @role_record_manager AND rm.menu_id = m.id
  );

-- 业务审核员拥有业务流程和材料审核相关菜单权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_business_auditor, m.id
FROM sys_menu m
WHERE m.permission_code IN (
    'dashboard:view',
    'material:audit:view',
    'exemption:view',
    'course-replace:view',
    'transfer:view',
    'graduation:view',
    'ai:view'
)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = @role_business_auditor AND rm.menu_id = m.id
  );

-- 登录日志和操作日志样例，便于第二阶段日志页面联调。
INSERT INTO sys_login_log (username, user_id, login_status, failure_reason, login_ip, user_agent)
SELECT 'admin', @user_admin, 'SUCCESS', NULL, '127.0.0.1', 'Mock Browser'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_login_log
    WHERE username = 'admin' AND login_status = 'SUCCESS' AND login_ip = '127.0.0.1'
);

INSERT INTO sys_login_log (username, user_id, login_status, failure_reason, login_ip, user_agent)
SELECT 'unknown', NULL, 'FAIL', '账号不存在', '127.0.0.1', 'Mock Browser'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_login_log
    WHERE username = 'unknown' AND login_status = 'FAIL' AND failure_reason = '账号不存在'
);

INSERT INTO sys_operation_log (
    module_name,
    operation_type,
    operation_desc,
    request_method,
    request_uri,
    operation_status,
    operator_id,
    operator_name,
    operation_ip,
    cost_time
)
SELECT '系统管理', 'CREATE', '初始化系统用户、角色和菜单测试数据', 'SQL', 'sql/test-data.sql', 'SUCCESS', @user_admin, '系统管理员', '127.0.0.1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_operation_log
    WHERE module_name = '系统管理' AND operation_type = 'CREATE' AND request_uri = 'sql/test-data.sql'
);

-- 测试考生基础信息：身份证号保持唯一，便于重复执行脚本。
INSERT INTO candidate (name, gender, id_card, admission_no, phone, status)
SELECT '张明', '男', '420101200101011234', 'ZK20260001', '13800010001', 'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM candidate WHERE id_card = '420101200101011234');

INSERT INTO candidate (name, gender, id_card, admission_no, phone, status)
SELECT '李晓雨', '女', '420101200202022345', 'ZK20260002', '13800010002', 'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM candidate WHERE id_card = '420101200202022345');

INSERT INTO candidate (name, gender, id_card, admission_no, phone, status)
SELECT '王博', '男', '420101200003033456', 'ZK20260003', '13800010003', 'LOCKED'
WHERE NOT EXISTS (SELECT 1 FROM candidate WHERE id_card = '420101200003033456');

INSERT INTO candidate (name, gender, id_card, admission_no, phone, status)
SELECT '陈思琪', '女', '420101199912124567', 'ZK20260004', '13800010004', 'NORMAL'
WHERE NOT EXISTS (SELECT 1 FROM candidate WHERE id_card = '420101199912124567');

SET @candidate_zhang := (SELECT id FROM candidate WHERE id_card = '420101200101011234');
SET @candidate_li := (SELECT id FROM candidate WHERE id_card = '420101200202022345');
SET @candidate_wang := (SELECT id FROM candidate WHERE id_card = '420101200003033456');
SET @candidate_chen := (SELECT id FROM candidate WHERE id_card = '420101199912124567');

-- 测试考籍档案：考籍号保持唯一。
INSERT INTO student_record (candidate_id, record_no, record_status, remark)
SELECT @candidate_zhang, 'KJ20260001', 'NORMAL', '2026 年春季批次新建考籍'
WHERE NOT EXISTS (SELECT 1 FROM student_record WHERE record_no = 'KJ20260001');

INSERT INTO student_record (candidate_id, record_no, record_status, remark)
SELECT @candidate_li, 'KJ20260002', 'NORMAL', '课程免考材料待审核'
WHERE NOT EXISTS (SELECT 1 FROM student_record WHERE record_no = 'KJ20260002');

INSERT INTO student_record (candidate_id, record_no, record_status, remark)
SELECT @candidate_wang, 'KJ20260003', 'SUSPENDED', '联系方式异常，暂缓办理'
WHERE NOT EXISTS (SELECT 1 FROM student_record WHERE record_no = 'KJ20260003');

INSERT INTO student_record (candidate_id, record_no, record_status, remark)
SELECT @candidate_chen, 'KJ20260004', 'NORMAL', '毕业申请预审通过'
WHERE NOT EXISTS (SELECT 1 FROM student_record WHERE record_no = 'KJ20260004');

SET @record_zhang := (SELECT id FROM student_record WHERE record_no = 'KJ20260001');
SET @record_li := (SELECT id FROM student_record WHERE record_no = 'KJ20260002');
SET @record_wang := (SELECT id FROM student_record WHERE record_no = 'KJ20260003');
SET @record_chen := (SELECT id FROM student_record WHERE record_no = 'KJ20260004');

-- 测试材料信息。
INSERT INTO record_material (record_id, material_type, file_name, file_url, audit_status)
SELECT @record_zhang, 'ID_CARD', '张明身份证扫描件.pdf', '/uploads/materials/zhangming-id-card.pdf', 'APPROVED'
WHERE NOT EXISTS (
    SELECT 1 FROM record_material
    WHERE record_id = @record_zhang AND material_type = 'ID_CARD' AND file_name = '张明身份证扫描件.pdf'
);

INSERT INTO record_material (record_id, material_type, file_name, file_url, audit_status)
SELECT @record_li, 'COURSE_EXEMPTION', '李晓雨课程免考证明.pdf', '/uploads/materials/lixiaoyu-course-exemption.pdf', 'PENDING'
WHERE NOT EXISTS (
    SELECT 1 FROM record_material
    WHERE record_id = @record_li AND material_type = 'COURSE_EXEMPTION' AND file_name = '李晓雨课程免考证明.pdf'
);

INSERT INTO record_material (record_id, material_type, file_name, file_url, audit_status)
SELECT @record_wang, 'TRANSFER', '王博转考申请表.pdf', '/uploads/materials/wangbo-transfer.pdf', 'REJECTED'
WHERE NOT EXISTS (
    SELECT 1 FROM record_material
    WHERE record_id = @record_wang AND material_type = 'TRANSFER' AND file_name = '王博转考申请表.pdf'
);

INSERT INTO record_material (record_id, material_type, file_name, file_url, audit_status)
SELECT @record_chen, 'GRADUATION', '陈思琪毕业申请材料.pdf', '/uploads/materials/chensiqi-graduation.pdf', 'APPROVED'
WHERE NOT EXISTS (
    SELECT 1 FROM record_material
    WHERE record_id = @record_chen AND material_type = 'GRADUATION' AND file_name = '陈思琪毕业申请材料.pdf'
);

-- 测试审核记录。
INSERT INTO audit_record (business_type, business_id, audit_status, audit_opinion, auditor_id)
SELECT 'MATERIAL', @record_zhang, 'APPROVED', '身份信息清晰，材料有效。', 1001
WHERE NOT EXISTS (
    SELECT 1 FROM audit_record
    WHERE business_type = 'MATERIAL' AND business_id = @record_zhang AND audit_status = 'APPROVED'
);

INSERT INTO audit_record (business_type, business_id, audit_status, audit_opinion, auditor_id)
SELECT 'COURSE_EXEMPTION', @record_li, 'PENDING', '等待人工复核课程成绩证明。', 1002
WHERE NOT EXISTS (
    SELECT 1 FROM audit_record
    WHERE business_type = 'COURSE_EXEMPTION' AND business_id = @record_li AND audit_status = 'PENDING'
);

INSERT INTO audit_record (business_type, business_id, audit_status, audit_opinion, auditor_id)
SELECT 'TRANSFER', @record_wang, 'REJECTED', '申请表缺少原考籍地盖章。', 1003
WHERE NOT EXISTS (
    SELECT 1 FROM audit_record
    WHERE business_type = 'TRANSFER' AND business_id = @record_wang AND audit_status = 'REJECTED'
);

INSERT INTO audit_record (business_type, business_id, audit_status, audit_opinion, auditor_id)
SELECT 'GRADUATION', @record_chen, 'APPROVED', '毕业申请材料完整，准予进入下一流程。', 1001
WHERE NOT EXISTS (
    SELECT 1 FROM audit_record
    WHERE business_type = 'GRADUATION' AND business_id = @record_chen AND audit_status = 'APPROVED'
);
