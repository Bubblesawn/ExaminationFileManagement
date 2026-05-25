USE exam_record;

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
