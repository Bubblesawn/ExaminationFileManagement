CREATE DATABASE IF NOT EXISTS exam_record DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_record;

CREATE TABLE IF NOT EXISTS candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '考生姓名',
    gender VARCHAR(16) COMMENT '性别',
    id_card VARCHAR(32) NOT NULL COMMENT '身份证号',
    admission_no VARCHAR(64) COMMENT '准考证号',
    phone VARCHAR(32) COMMENT '联系电话',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考生状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_candidate_id_card (id_card)
) COMMENT='考生基础信息表';

CREATE TABLE IF NOT EXISTS student_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    candidate_id BIGINT NOT NULL COMMENT '考生ID',
    record_no VARCHAR(64) NOT NULL COMMENT '考籍号',
    record_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '考籍状态',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_record_no (record_no),
    KEY idx_student_record_candidate_id (candidate_id)
) COMMENT='考籍档案表';

CREATE TABLE IF NOT EXISTS record_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT NOT NULL COMMENT '考籍档案ID',
    material_type VARCHAR(64) NOT NULL COMMENT '材料类型',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_url VARCHAR(512) NOT NULL COMMENT '文件地址',
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_record_material_record_id (record_id)
) COMMENT='考籍材料表';

CREATE TABLE IF NOT EXISTS audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    audit_status VARCHAR(32) NOT NULL COMMENT '审核状态',
    audit_opinion VARCHAR(512) COMMENT '审核意见',
    auditor_id BIGINT COMMENT '审核人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_audit_record_business (business_type, business_id)
) COMMENT='审核记录表';

