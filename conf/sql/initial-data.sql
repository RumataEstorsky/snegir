-- INSERT INTO bases(base, commentary, create_time) VALUE ('Super difficult Exam', 'I wish you good luck, no one passed this!');

INSERT INTO opros_templates (template, base_id, group_id, begin_date, end_date, count_attempts, template_author_id)
VALUES('Super STEEL Template', 7, 2, '2010-01-01', '2099-01-01', 999, 2)
;

INSERT INTO templates_and_sections VALUES (1, 26), (1,27)
;


INSERT INTO user_passes (session_id, template_id, user_id, begin_time, end_time, time_on_test, all_questions, right_answers, universal_ball)
VALUES ((SELECT MAX(session_id) FROM sessions WHERE user_id = 2),
        1,2, now(), now(), '0:0:1', 0, 0, 1)
;


INSERT INTO user_answers (user_pass_id, question_id, is_right, answer_time)
SELECT (SELECT MAX(user_pass_id) FROM user_passes WHERE user_id = 2),
       question_id,
       TRUE,
       now() FROM questions WHERE section_id IN (26,27)
;

ALTER TABLE users ADD role VARCHAR(9) DEFAULT 'student' NOT NULL;
UPDATE users SET role = 'admin' WHERE login IN ('v01', 'valrkl');