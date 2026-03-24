create sequence pensen.account_id;
grant usage on sequence pensen.account_id to "dbpensen";
select setval('pensen.account_id', (select max(id) from pensen.account));

drop sequence pensen.authorisation_id;
drop sequence pensen.default_lessons_id;

alter table pensen.course alter column grade_id set not null;