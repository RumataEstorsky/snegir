-- пр_доступ_к_базам: УН доступа, УН базы, УН пользователя, ФИО, Тип доступа, Права администрирования, С какого времени
DROP VIEW пр_доступ_к_базам;
DROP VIEW пр_письма;
DROP VIEW пр_шаблоны_опроса;

CREATE OR REPLACE VIEW vw_bases_access AS
SELECT ba.access_id,
       ba.base_id,
       u.user_id,
       u.fio,
       if(ba.read_only, 'только чтение', 'полный доступ')             AS access_type,
       if(ba.general_user, 'администратор базы', 'пользователь базы') AS gen_user,
       date_format(ba.create_time, '%d.%m.%Y %T')                     AS created_at
FROM (snegir.bases_access ba
         JOIN snegir.vw_users_ext u on ((ba.user_id = u.user_id)))
;

-- пр_письма: УН письма, Тема, УН отправителя, УН получателя, Отправитель, Получатель, Отправлено, Открыто, Отправлено (для сортировки), Открыто (для сортировки), Содержание, Скрыть у отправителя, Скрыть у получателя
CREATE OR REPLACE VIEW vw_letters
AS
SELECT l.letter_id,
       ifnull(l.theme, '<без темы>') AS theme,
       l.from_user_id,
       l.to_user_id,
       concat(uf.family_name, ' ', uf.name, ' ', ifnull(uf.patronymic, '')) AS from_full_name,
       concat(ut.family_name, ' ', ut.name, ' ', ifnull(ut.patronymic, '')) AS to_full_name,
       date_format(l.create_time, '%d.%m.%Y %T') AS created_at,
       ifnull(date_format(l.read_time, '%d.%m.%Y %T'), 'еще не прочитано') AS read_at,
       l.create_time,
       l.read_time,
       l.letter,
       (l.hide_from + 0) AS hide_from,
       (l.hide_to + 0) AS hide_to
FROM snegir.letters l
         JOIN snegir.users ut ON l.to_user_id = ut.user_id
         JOIN snegir.users uf ON l.from_user_id = uf.user_id
;

-- пр_шаблоны_опроса: ун шаблона, название шаблона, ун базы, название базы, автор шаблона, ун группы, для группы, с какого числа, по какое число, количество попыток, включено разделов, вопросов, прибл. время, всего сдач
CREATE OR REPLACE VIEW vw_templates
AS
SELECT t.template_id,
       t.template,
       b.base_id,
       b.base,
       u.fio AS template_author,
       t.group_id,
       g.full_name,
       t.begin_date,
       t.end_date,
       if((t.count_attempts = 0), 'не ограничено', t.count_attempts) AS count_attempts,
       ifnull(ts.sections_count, '0') AS sections_count,
       ifnull(stat.questions_count, '0') AS questions_count,
       ifnull(stat.time, sec_to_time(0)) AS estimation_time,
       ifnull(tp.passes_count, '0') AS passes_count
FROM snegir.opros_templates t
    JOIN snegir.vw_groups_ext g ON t.group_id = g.group_id
    JOIN snegir.vw_users_ext u ON t.template_author_id = u.user_id
    JOIN snegir.bases b ON b.base_id = t.base_id
    LEFT JOIN snegir.vw_template_sections_count ts ON t.template_id = ts.template_id
    LEFT JOIN snegir.vw_template_passes_count tp ON t.template_id = tp.template_id
    LEFT JOIN snegir.vw_template_sum stat ON t.template_id = stat.template_id
;

-- Сколько же попыток использовал пользователь
CREATE OR REPLACE VIEW vw_used_attempts AS
SELECT template_id,user_id, COUNT(*) AS c
FROM user_passes
GROUP BY template_id,user_id
;

-- Чтобы  выдавались только последняя сдача студента по этому шаблону (если их несколько)
CREATE OR REPLACE VIEW vw_last_pass_by_user AS
SELECT template_id,user_id, MAX(user_pass_id) AS user_pass_id
FROM user_passes
GROUP BY user_id, template_id
;

