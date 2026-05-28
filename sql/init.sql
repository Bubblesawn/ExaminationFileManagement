SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS exam_record DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_record;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码哈希',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(32) COMMENT '联系电话',
    email VARCHAR(128) COMMENT '电子邮箱',
    avatar VARCHAR(512) COMMENT '头像地址',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态：ENABLED 启用，DISABLED 禁用',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_sort INT NOT NULL DEFAULT 0 COMMENT '角色排序',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '数据权限范围：ALL 全部，SELF 本人',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '角色状态：ENABLED 启用，DISABLED 禁用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_code (role_code),
    KEY idx_sys_role_status (status)
) COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID，0表示根节点',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(32) NOT NULL COMMENT '菜单类型：CATALOG 目录，MENU 菜单，BUTTON 按钮',
    route_path VARCHAR(255) COMMENT '前端路由路径',
    component_path VARCHAR(255) COMMENT '前端组件路径',
    permission_code VARCHAR(128) COMMENT '权限标识',
    icon VARCHAR(64) COMMENT '菜单图标',
    menu_sort INT NOT NULL DEFAULT 0 COMMENT '菜单排序',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示，0 隐藏',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '菜单状态：ENABLED 启用，DISABLED 禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_menu_permission_code (permission_code),
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_status (status)
) COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id)
) COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_menu_id (menu_id)
) COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    user_id BIGINT COMMENT '用户ID',
    login_status VARCHAR(32) NOT NULL COMMENT '登录状态：SUCCESS 成功，FAIL 失败',
    failure_reason VARCHAR(255) COMMENT '失败原因',
    login_ip VARCHAR(64) COMMENT '登录IP',
    user_agent VARCHAR(512) COMMENT '浏览器用户代理',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_sys_login_log_username (username),
    KEY idx_sys_login_log_user_id (user_id),
    KEY idx_sys_login_log_login_time (login_time),
    KEY idx_sys_login_log_status (login_status)
) COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(255) COMMENT '操作说明',
    request_method VARCHAR(16) COMMENT '请求方式',
    request_uri VARCHAR(255) COMMENT '请求地址',
    request_param TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    operation_status VARCHAR(32) NOT NULL COMMENT '操作状态：SUCCESS 成功，FAIL 失败',
    error_message TEXT COMMENT '错误信息',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operation_ip VARCHAR(64) COMMENT '操作IP',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    cost_time BIGINT COMMENT '耗时，单位毫秒',
    KEY idx_sys_operation_log_module (module_name),
    KEY idx_sys_operation_log_operator_id (operator_id),
    KEY idx_sys_operation_log_operation_time (operation_time),
    KEY idx_sys_operation_log_status (operation_status)
) COMMENT='操作日志表';

ALTER TABLE sys_user
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY username VARCHAR(64) NOT NULL COMMENT '登录账号',
    MODIFY password VARCHAR(255) NOT NULL COMMENT '登录密码哈希',
    MODIFY real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    MODIFY phone VARCHAR(32) NULL COMMENT '联系电话',
    MODIFY email VARCHAR(128) NULL COMMENT '电子邮箱',
    MODIFY avatar VARCHAR(512) NULL COMMENT '头像地址',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态：ENABLED 启用，DISABLED 禁用',
    MODIFY last_login_time DATETIME NULL COMMENT '最后登录时间',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '系统用户表';

ALTER TABLE sys_role
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    MODIFY role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    MODIFY role_sort INT NOT NULL DEFAULT 0 COMMENT '角色排序',
    MODIFY data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '数据权限范围：ALL 全部，SELF 本人',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '角色状态：ENABLED 启用，DISABLED 禁用',
    MODIFY remark VARCHAR(512) NULL COMMENT '备注',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '系统角色表';

ALTER TABLE sys_menu
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID，0表示根节点',
    MODIFY menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    MODIFY menu_type VARCHAR(32) NOT NULL COMMENT '菜单类型：CATALOG 目录，MENU 菜单，BUTTON 按钮',
    MODIFY route_path VARCHAR(255) NULL COMMENT '前端路由路径',
    MODIFY component_path VARCHAR(255) NULL COMMENT '前端组件路径',
    MODIFY permission_code VARCHAR(128) NULL COMMENT '权限标识',
    MODIFY icon VARCHAR(64) NULL COMMENT '菜单图标',
    MODIFY menu_sort INT NOT NULL DEFAULT 0 COMMENT '菜单排序',
    MODIFY visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示，0 隐藏',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '菜单状态：ENABLED 启用，DISABLED 禁用',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '系统菜单表';

ALTER TABLE sys_user_role
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY user_id BIGINT NOT NULL COMMENT '用户ID',
    MODIFY role_id BIGINT NOT NULL COMMENT '角色ID',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    COMMENT = '用户角色关联表';

ALTER TABLE sys_role_menu
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY role_id BIGINT NOT NULL COMMENT '角色ID',
    MODIFY menu_id BIGINT NOT NULL COMMENT '菜单ID',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    COMMENT = '角色菜单关联表';

ALTER TABLE sys_login_log
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY username VARCHAR(64) NOT NULL COMMENT '登录账号',
    MODIFY user_id BIGINT NULL COMMENT '用户ID',
    MODIFY login_status VARCHAR(32) NOT NULL COMMENT '登录状态：SUCCESS 成功，FAIL 失败',
    MODIFY failure_reason VARCHAR(255) NULL COMMENT '失败原因',
    MODIFY login_ip VARCHAR(64) NULL COMMENT '登录IP',
    MODIFY user_agent VARCHAR(512) NULL COMMENT '浏览器用户代理',
    MODIFY login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    COMMENT = '登录日志表';

ALTER TABLE sys_operation_log
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    MODIFY operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    MODIFY operation_desc VARCHAR(255) NULL COMMENT '操作说明',
    MODIFY request_method VARCHAR(16) NULL COMMENT '请求方式',
    MODIFY request_uri VARCHAR(255) NULL COMMENT '请求地址',
    MODIFY request_param TEXT NULL COMMENT '请求参数',
    MODIFY response_result TEXT NULL COMMENT '响应结果',
    MODIFY operation_status VARCHAR(32) NOT NULL COMMENT '操作状态：SUCCESS 成功，FAIL 失败',
    MODIFY error_message TEXT NULL COMMENT '错误信息',
    MODIFY operator_id BIGINT NULL COMMENT '操作人ID',
    MODIFY operator_name VARCHAR(64) NULL COMMENT '操作人姓名',
    MODIFY operation_ip VARCHAR(64) NULL COMMENT '操作IP',
    MODIFY operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    MODIFY cost_time BIGINT NULL COMMENT '耗时，单位毫秒',
    COMMENT = '操作日志表';

CREATE TABLE IF NOT EXISTS candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '考生姓名',
    gender VARCHAR(16) COMMENT '性别',
    id_card VARCHAR(32) NOT NULL COMMENT '身份证号',
    admission_no VARCHAR(64) COMMENT '准考证号',
    birth_date DATE COMMENT '出生日期',
    nation VARCHAR(32) COMMENT '民族',
    political_status VARCHAR(32) COMMENT '政治面貌',
    education_level VARCHAR(32) COMMENT '学历层次',
    major_name VARCHAR(128) COMMENT '报考专业',
    phone VARCHAR(32) COMMENT '联系电话',
    email VARCHAR(128) COMMENT '电子邮箱',
    address VARCHAR(255) COMMENT '联系地址',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考生状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_candidate_id_card (id_card),
    UNIQUE KEY uk_candidate_admission_no (admission_no),
    KEY idx_candidate_name (name),
    KEY idx_candidate_status (status)
) COMMENT='考生基础信息表';

CREATE TABLE IF NOT EXISTS student_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    candidate_id BIGINT NOT NULL COMMENT '考生ID',
    record_no VARCHAR(64) NOT NULL COMMENT '考籍号',
    enroll_batch VARCHAR(64) COMMENT '注册批次',
    education_level VARCHAR(32) COMMENT '考籍层次',
    major_code VARCHAR(64) COMMENT '专业代码',
    major_name VARCHAR(128) COMMENT '专业名称',
    record_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考籍状态',
    archive_status VARCHAR(32) NOT NULL DEFAULT 'UNARCHIVED' COMMENT '归档状态：UNARCHIVED 未归档，ARCHIVED 已归档',
    archive_time DATETIME COMMENT '归档时间',
    archive_operator_id BIGINT COMMENT '归档操作人ID',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_record_no (record_no),
    KEY idx_student_record_candidate_id (candidate_id),
    KEY idx_student_record_status (record_status),
    KEY idx_student_record_archive_status (archive_status)
) COMMENT='考籍档案表';

CREATE TABLE IF NOT EXISTS record_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    material_type VARCHAR(64) NOT NULL COMMENT '材料类型',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    original_file_name VARCHAR(255) COMMENT '原始文件名称',
    file_url VARCHAR(512) NOT NULL COMMENT '文件地址',
    file_size BIGINT COMMENT '文件大小，单位字节',
    file_suffix VARCHAR(32) COMMENT '文件后缀',
    mime_type VARCHAR(128) COMMENT '文件 MIME 类型',
    preview_url VARCHAR(512) COMMENT '预览地址',
    upload_user_id BIGINT COMMENT '上传人ID',
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    audit_opinion VARCHAR(512) COMMENT '审核意见',
    audit_user_id BIGINT COMMENT '审核人ID',
    audit_time DATETIME COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_record_material_record_id (record_id),
    KEY idx_record_material_type (material_type),
    KEY idx_record_material_audit_status (audit_status)
) COMMENT='考籍材料表';

CREATE TABLE IF NOT EXISTS material_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    type_code VARCHAR(64) NOT NULL COMMENT '材料类型编码',
    type_name VARCHAR(128) NOT NULL COMMENT '材料类型名称',
    description VARCHAR(512) COMMENT '类型说明',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED 启用，DISABLED 禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_material_type_code (type_code),
    KEY idx_material_type_status (status),
    KEY idx_material_type_sort (sort_order)
) COMMENT='材料类型表';

CREATE TABLE IF NOT EXISTS course_replacement_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    source_course_code VARCHAR(64) NOT NULL COMMENT '原课程代码',
    source_course_name VARCHAR(128) NOT NULL COMMENT '原课程名称',
    target_course_code VARCHAR(64) NOT NULL COMMENT '顶替课程代码',
    target_course_name VARCHAR(128) NOT NULL COMMENT '顶替课程名称',
    major_code VARCHAR(64) COMMENT '适用专业代码，空值表示通用规则',
    education_level VARCHAR(32) COMMENT '适用学历层次，空值表示通用规则',
    credit DECIMAL(5,2) COMMENT '顶替课程学分',
    rule_status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '规则状态：ENABLED 启用，DISABLED 禁用',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_course_replacement_rule_scope (source_course_code, target_course_code, major_code),
    KEY idx_course_replacement_rule_source (source_course_code),
    KEY idx_course_replacement_rule_target (target_course_code),
    KEY idx_course_replacement_rule_status (rule_status)
) COMMENT='课程顶替规则表';

CREATE TABLE IF NOT EXISTS record_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    before_status VARCHAR(32) COMMENT '变更前状态',
    after_status VARCHAR(32) NOT NULL COMMENT '变更后状态',
    change_reason VARCHAR(512) COMMENT '状态变更原因',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_record_status_log_record_id (record_id),
    KEY idx_record_status_log_operation_time (operation_time),
    KEY idx_record_status_log_after_status (after_status)
) COMMENT='档案状态记录表';

CREATE TABLE IF NOT EXISTS record_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    change_type VARCHAR(64) NOT NULL COMMENT '变更类型',
    change_field VARCHAR(128) COMMENT '变更字段',
    before_value TEXT COMMENT '变更前内容',
    after_value TEXT COMMENT '变更后内容',
    change_reason VARCHAR(512) COMMENT '变更原因',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_record_change_log_record_id (record_id),
    KEY idx_record_change_log_change_type (change_type),
    KEY idx_record_change_log_operation_time (operation_time)
) COMMENT='档案变更记录表';

CREATE TABLE IF NOT EXISTS audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    application_id BIGINT COMMENT '通用申请ID，关联 business_application.id',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    record_id BIGINT COMMENT '考籍档案ID',
    audit_action VARCHAR(32) NOT NULL DEFAULT 'SUBMIT' COMMENT '流程动作：SUBMIT 提交，APPROVE 通过，REJECT 驳回，WITHDRAW 撤回',
    before_status VARCHAR(32) COMMENT '操作前申请状态',
    after_status VARCHAR(32) NOT NULL COMMENT '操作后申请状态',
    audit_status VARCHAR(32) NOT NULL COMMENT '审核状态',
    audit_opinion VARCHAR(512) COMMENT '审核意见或流程说明',
    auditor_id BIGINT COMMENT '审核人或操作人ID',
    auditor_name VARCHAR(64) COMMENT '审核人或操作人姓名',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_audit_record_application_id (application_id),
    KEY idx_audit_record_business (business_type, business_id),
    KEY idx_audit_record_status (audit_status),
    KEY idx_audit_record_operation_time (operation_time)
) COMMENT='审核记录表';

CREATE TABLE IF NOT EXISTS process_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型：EXEMPTION 免考，COURSE_REPLACE 课程顶替，TRANSFER_IN 转入，TRANSFER_OUT 转出，GRADUATION 毕业',
    status_code VARCHAR(32) NOT NULL COMMENT '流程状态编码',
    status_name VARCHAR(64) NOT NULL COMMENT '流程状态名称',
    status_sort INT NOT NULL DEFAULT 0 COMMENT '状态排序',
    initial_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否初始状态：1 是，0 否',
    final_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否终态：1 是，0 否',
    allow_edit TINYINT NOT NULL DEFAULT 0 COMMENT '当前状态是否允许编辑申请：1 是，0 否',
    allow_withdraw TINYINT NOT NULL DEFAULT 0 COMMENT '当前状态是否允许撤回申请：1 是，0 否',
    next_status_codes VARCHAR(255) COMMENT '允许流转的下一状态编码，多个用英文逗号分隔',
    description VARCHAR(512) COMMENT '状态说明',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态：ENABLED 启用，DISABLED 禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_process_status_type_code (business_type, status_code),
    KEY idx_process_status_type_sort (business_type, status_sort),
    KEY idx_process_status_enabled (status)
) COMMENT='流程状态表';

CREATE TABLE IF NOT EXISTS business_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    application_no VARCHAR(64) NOT NULL COMMENT '申请编号',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型：EXEMPTION 免考，COURSE_REPLACE 课程顶替，TRANSFER_IN 转入，TRANSFER_OUT 转出，GRADUATION 毕业',
    record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    candidate_id BIGINT NOT NULL COMMENT '考生ID',
    application_title VARCHAR(128) COMMENT '申请标题',
    application_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：DRAFT 草稿，SUBMITTED 已提交，AUDITING 审核中，APPROVED 已通过，REJECTED 已驳回，WITHDRAWN 已撤回',
    current_node_code VARCHAR(64) COMMENT '当前流程节点编码',
    current_node_name VARCHAR(64) COMMENT '当前流程节点名称',
    course_code VARCHAR(64) COMMENT '目标课程代码，免考或课程顶替业务使用',
    course_name VARCHAR(128) COMMENT '目标课程名称，免考或课程顶替业务使用',
    source_course_code VARCHAR(64) COMMENT '来源课程代码，免考或课程顶替业务使用',
    source_course_name VARCHAR(128) COMMENT '来源课程名称，免考或课程顶替业务使用',
    exemption_reason VARCHAR(512) COMMENT '免考原因',
    material_ids_json TEXT COMMENT '申请材料ID列表JSON',
    extension_data_json TEXT COMMENT '业务扩展字段快照JSON',
    apply_user_id BIGINT COMMENT '提交人ID',
    apply_user_name VARCHAR(64) COMMENT '提交人姓名',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    withdraw_time DATETIME COMMENT '撤回时间',
    withdraw_reason VARCHAR(512) COMMENT '撤回原因',
    audit_user_id BIGINT COMMENT '审核人ID',
    audit_user_name VARCHAR(64) COMMENT '审核人姓名',
    audit_time DATETIME COMMENT '审核时间',
    audit_opinion VARCHAR(512) COMMENT '审核意见',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_business_application_no (application_no),
    KEY idx_business_application_type_status (business_type, application_status),
    KEY idx_business_application_record_id (record_id),
    KEY idx_business_application_candidate_id (candidate_id),
    KEY idx_business_application_submit_time (submit_time)
) COMMENT='通用业务申请表';

CREATE TABLE IF NOT EXISTS application_extension_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    application_id BIGINT NOT NULL COMMENT '通用申请ID，关联 business_application.id',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    field_code VARCHAR(64) NOT NULL COMMENT '扩展字段编码',
    field_name VARCHAR(128) NOT NULL COMMENT '扩展字段名称',
    field_value TEXT COMMENT '扩展字段值',
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '字段值类型：STRING 字符串，NUMBER 数值，DATE 日期，JSON JSON对象',
    required_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：1 是，0 否',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_application_extension_field (application_id, field_code),
    KEY idx_application_extension_application_id (application_id),
    KEY idx_application_extension_type_code (business_type, field_code)
) COMMENT='申请业务扩展字段表';

ALTER TABLE candidate
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY name VARCHAR(64) NOT NULL COMMENT '考生姓名',
    MODIFY gender VARCHAR(16) NULL COMMENT '性别',
    MODIFY id_card VARCHAR(32) NOT NULL COMMENT '身份证号',
    MODIFY admission_no VARCHAR(64) NULL COMMENT '准考证号',
    MODIFY birth_date DATE NULL COMMENT '出生日期',
    MODIFY nation VARCHAR(32) NULL COMMENT '民族',
    MODIFY political_status VARCHAR(32) NULL COMMENT '政治面貌',
    MODIFY education_level VARCHAR(32) NULL COMMENT '学历层次',
    MODIFY major_name VARCHAR(128) NULL COMMENT '报考专业',
    MODIFY phone VARCHAR(32) NULL COMMENT '联系电话',
    MODIFY email VARCHAR(128) NULL COMMENT '电子邮箱',
    MODIFY address VARCHAR(255) NULL COMMENT '联系地址',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考生状态',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '考生基础信息表';

ALTER TABLE student_record
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY candidate_id BIGINT NOT NULL COMMENT '考生ID',
    MODIFY record_no VARCHAR(64) NOT NULL COMMENT '考籍号',
    MODIFY enroll_batch VARCHAR(64) NULL COMMENT '注册批次',
    MODIFY education_level VARCHAR(32) NULL COMMENT '考籍层次',
    MODIFY major_code VARCHAR(64) NULL COMMENT '专业代码',
    MODIFY major_name VARCHAR(128) NULL COMMENT '专业名称',
    MODIFY record_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考籍状态',
    MODIFY archive_status VARCHAR(32) NOT NULL DEFAULT 'UNARCHIVED' COMMENT '归档状态：UNARCHIVED 未归档，ARCHIVED 已归档',
    MODIFY archive_time DATETIME NULL COMMENT '归档时间',
    MODIFY archive_operator_id BIGINT NULL COMMENT '归档操作人ID',
    MODIFY remark VARCHAR(512) NULL COMMENT '备注',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '考籍档案表';

ALTER TABLE record_material
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    MODIFY material_type VARCHAR(64) NOT NULL COMMENT '材料类型',
    MODIFY file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    MODIFY original_file_name VARCHAR(255) NULL COMMENT '原始文件名称',
    MODIFY file_url VARCHAR(512) NOT NULL COMMENT '文件地址',
    MODIFY file_size BIGINT NULL COMMENT '文件大小，单位字节',
    MODIFY file_suffix VARCHAR(32) NULL COMMENT '文件后缀',
    MODIFY mime_type VARCHAR(128) NULL COMMENT '文件 MIME 类型',
    MODIFY preview_url VARCHAR(512) NULL COMMENT '预览地址',
    MODIFY upload_user_id BIGINT NULL COMMENT '上传人ID',
    MODIFY audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    MODIFY audit_opinion VARCHAR(512) NULL COMMENT '审核意见',
    MODIFY audit_user_id BIGINT NULL COMMENT '审核人ID',
    MODIFY audit_time DATETIME NULL COMMENT '审核时间',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '考籍材料表';

ALTER TABLE material_type
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY type_code VARCHAR(64) NOT NULL COMMENT '材料类型编码',
    MODIFY type_name VARCHAR(128) NOT NULL COMMENT '材料类型名称',
    MODIFY description VARCHAR(512) NULL COMMENT '类型说明',
    MODIFY sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED 启用，DISABLED 禁用',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '材料类型表';

ALTER TABLE course_replacement_rule
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY source_course_code VARCHAR(64) NOT NULL COMMENT '原课程代码',
    MODIFY source_course_name VARCHAR(128) NOT NULL COMMENT '原课程名称',
    MODIFY target_course_code VARCHAR(64) NOT NULL COMMENT '顶替课程代码',
    MODIFY target_course_name VARCHAR(128) NOT NULL COMMENT '顶替课程名称',
    MODIFY major_code VARCHAR(64) NULL COMMENT '适用专业代码，空值表示通用规则',
    MODIFY education_level VARCHAR(32) NULL COMMENT '适用学历层次，空值表示通用规则',
    MODIFY credit DECIMAL(5,2) NULL COMMENT '顶替课程学分',
    MODIFY rule_status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '规则状态：ENABLED 启用，DISABLED 禁用',
    MODIFY effective_date DATE NULL COMMENT '生效日期',
    MODIFY expire_date DATE NULL COMMENT '失效日期',
    MODIFY remark VARCHAR(512) NULL COMMENT '备注',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '课程顶替规则表';

ALTER TABLE record_status_log
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    MODIFY before_status VARCHAR(32) NULL COMMENT '变更前状态',
    MODIFY after_status VARCHAR(32) NOT NULL COMMENT '变更后状态',
    MODIFY change_reason VARCHAR(512) NULL COMMENT '状态变更原因',
    MODIFY operator_id BIGINT NULL COMMENT '操作人ID',
    MODIFY operator_name VARCHAR(64) NULL COMMENT '操作人姓名',
    MODIFY operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    COMMENT = '档案状态记录表';

ALTER TABLE record_change_log
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    MODIFY change_type VARCHAR(64) NOT NULL COMMENT '变更类型',
    MODIFY change_field VARCHAR(128) NULL COMMENT '变更字段',
    MODIFY before_value TEXT NULL COMMENT '变更前内容',
    MODIFY after_value TEXT NULL COMMENT '变更后内容',
    MODIFY change_reason VARCHAR(512) NULL COMMENT '变更原因',
    MODIFY operator_id BIGINT NULL COMMENT '操作人ID',
    MODIFY operator_name VARCHAR(64) NULL COMMENT '操作人姓名',
    MODIFY operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    COMMENT = '档案变更记录表';

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'application_id'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN application_id BIGINT NULL COMMENT ''通用申请ID，关联 business_application.id'' AFTER id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'record_id'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN record_id BIGINT NULL COMMENT ''考籍档案ID'' AFTER business_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'audit_action'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN audit_action VARCHAR(32) NOT NULL DEFAULT ''SUBMIT'' COMMENT ''流程动作：SUBMIT 提交，APPROVE 通过，REJECT 驳回，WITHDRAW 撤回'' AFTER record_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'before_status'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN before_status VARCHAR(32) NULL COMMENT ''操作前申请状态'' AFTER audit_action',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'after_status'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN after_status VARCHAR(32) NOT NULL DEFAULT ''SUBMITTED'' COMMENT ''操作后申请状态'' AFTER before_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'auditor_name'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN auditor_name VARCHAR(64) NULL COMMENT ''审核人或操作人姓名'' AFTER auditor_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_record'
      AND COLUMN_NAME = 'operation_time'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE audit_record ADD COLUMN operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''操作时间'' AFTER auditor_name',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE audit_record
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY application_id BIGINT NULL COMMENT '通用申请ID，关联 business_application.id',
    MODIFY business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    MODIFY business_id BIGINT NOT NULL COMMENT '业务ID',
    MODIFY record_id BIGINT NULL COMMENT '考籍档案ID',
    MODIFY audit_action VARCHAR(32) NOT NULL DEFAULT 'SUBMIT' COMMENT '流程动作：SUBMIT 提交，APPROVE 通过，REJECT 驳回，WITHDRAW 撤回',
    MODIFY before_status VARCHAR(32) NULL COMMENT '操作前申请状态',
    MODIFY after_status VARCHAR(32) NOT NULL COMMENT '操作后申请状态',
    MODIFY audit_status VARCHAR(32) NOT NULL COMMENT '审核状态',
    MODIFY audit_opinion VARCHAR(512) NULL COMMENT '审核意见或流程说明',
    MODIFY auditor_id BIGINT NULL COMMENT '审核人或操作人ID',
    MODIFY auditor_name VARCHAR(64) NULL COMMENT '审核人或操作人姓名',
    MODIFY operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    COMMENT = '审核记录表';

ALTER TABLE process_status
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY business_type VARCHAR(64) NOT NULL COMMENT '业务类型：EXEMPTION 免考，COURSE_REPLACE 课程顶替，TRANSFER_IN 转入，TRANSFER_OUT 转出，GRADUATION 毕业',
    MODIFY status_code VARCHAR(32) NOT NULL COMMENT '流程状态编码',
    MODIFY status_name VARCHAR(64) NOT NULL COMMENT '流程状态名称',
    MODIFY status_sort INT NOT NULL DEFAULT 0 COMMENT '状态排序',
    MODIFY initial_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否初始状态：1 是，0 否',
    MODIFY final_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否终态：1 是，0 否',
    MODIFY allow_edit TINYINT NOT NULL DEFAULT 0 COMMENT '当前状态是否允许编辑申请：1 是，0 否',
    MODIFY allow_withdraw TINYINT NOT NULL DEFAULT 0 COMMENT '当前状态是否允许撤回申请：1 是，0 否',
    MODIFY next_status_codes VARCHAR(255) NULL COMMENT '允许流转的下一状态编码，多个用英文逗号分隔',
    MODIFY description VARCHAR(512) NULL COMMENT '状态说明',
    MODIFY status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态：ENABLED 启用，DISABLED 禁用',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程状态表';

ALTER TABLE business_application
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY application_no VARCHAR(64) NOT NULL COMMENT '申请编号',
    MODIFY business_type VARCHAR(64) NOT NULL COMMENT '业务类型：EXEMPTION 免考，COURSE_REPLACE 课程顶替，TRANSFER_IN 转入，TRANSFER_OUT 转出，GRADUATION 毕业',
    MODIFY record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    MODIFY candidate_id BIGINT NOT NULL COMMENT '考生ID',
    MODIFY application_title VARCHAR(128) NULL COMMENT '申请标题',
    MODIFY application_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：DRAFT 草稿，SUBMITTED 已提交，AUDITING 审核中，APPROVED 已通过，REJECTED 已驳回，WITHDRAWN 已撤回',
    MODIFY current_node_code VARCHAR(64) NULL COMMENT '当前流程节点编码',
    MODIFY current_node_name VARCHAR(64) NULL COMMENT '当前流程节点名称',
    MODIFY course_code VARCHAR(64) NULL COMMENT '目标课程代码，免考或课程顶替业务使用',
    MODIFY course_name VARCHAR(128) NULL COMMENT '目标课程名称，免考或课程顶替业务使用',
    MODIFY source_course_code VARCHAR(64) NULL COMMENT '来源课程代码，免考或课程顶替业务使用',
    MODIFY source_course_name VARCHAR(128) NULL COMMENT '来源课程名称，免考或课程顶替业务使用',
    MODIFY exemption_reason VARCHAR(512) NULL COMMENT '免考原因',
    MODIFY material_ids_json TEXT NULL COMMENT '申请材料ID列表JSON',
    MODIFY extension_data_json TEXT NULL COMMENT '业务扩展字段快照JSON',
    MODIFY apply_user_id BIGINT NULL COMMENT '提交人ID',
    MODIFY apply_user_name VARCHAR(64) NULL COMMENT '提交人姓名',
    MODIFY submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    MODIFY withdraw_time DATETIME NULL COMMENT '撤回时间',
    MODIFY withdraw_reason VARCHAR(512) NULL COMMENT '撤回原因',
    MODIFY audit_user_id BIGINT NULL COMMENT '审核人ID',
    MODIFY audit_user_name VARCHAR(64) NULL COMMENT '审核人姓名',
    MODIFY audit_time DATETIME NULL COMMENT '审核时间',
    MODIFY audit_opinion VARCHAR(512) NULL COMMENT '审核意见',
    MODIFY remark VARCHAR(512) NULL COMMENT '备注',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '通用业务申请表';

ALTER TABLE application_extension_field
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY application_id BIGINT NOT NULL COMMENT '通用申请ID，关联 business_application.id',
    MODIFY business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    MODIFY field_code VARCHAR(64) NOT NULL COMMENT '扩展字段编码',
    MODIFY field_name VARCHAR(128) NOT NULL COMMENT '扩展字段名称',
    MODIFY field_value TEXT NULL COMMENT '扩展字段值',
    MODIFY value_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '字段值类型：STRING 字符串，NUMBER 数值，DATE 日期，JSON JSON对象',
    MODIFY required_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：1 是，0 否',
    MODIFY sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    MODIFY create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '申请业务扩展字段表';
