alter sequence pensen.authorisation_id rename to account_id;
drop sequence pensen.default_lessons_id;

alter table pensen.course alter column grade_id set not null;