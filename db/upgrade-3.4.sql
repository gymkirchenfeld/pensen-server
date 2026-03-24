alter table pensen.payroll_type
add column lesson_based boolean not null default true;

create table pensen.weekly_lessons (
  school_year_id integer not null,
  payroll_type_id integer not null,
  lessons double precision not null,
  primary key (school_year_id, payroll_type_id),
  foreign key (school_year_id) references pensen.school_year (id) on update cascade,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant delete, insert, select on table pensen.weekly_lessons to "pensenmanager";

insert into pensen.weekly_lessons (school_year_id, payroll_type_id, lessons)
select sy.id, pt.id, pt.weekly_lessons
from pensen.school_year sy, pensen.payroll_type pt
where pt.weekly_lessons > 0;

-- table lesson_type
create table pensen.lesson_type (
  id integer not null primary key,
  code character varying(10) not null,
  description character varying(100) not null
);
grant select on table pensen.lesson_type to "pensenmanager";

insert into pensen.lesson_type (id, code, description)
values
(1, 'X', 'nicht vorhanden'),
(2, 'W', 'Weiterführung'),
(3, 'A', 'Anfang'),
(4, 'AO', 'Anfang optional');


-- table lesson_table_entry
create table pensen.lesson_table_entry (
  curriculum_id integer not null,
  division_id integer,
  subject_id integer not null,
  grade_id integer not null,
  type_id integer not null,
  lessons1 double precision,
  lessons2 double precision,
  unique (curriculum_id, division_id, subject_id, grade_id),
  foreign key (curriculum_id) references pensen.curriculum (id) on update cascade on delete cascade,
  foreign key (division_id) references pensen.division (id) on update cascade on delete cascade,
  foreign key (subject_id) references pensen.subject (id) on update cascade on delete cascade,
  foreign key (grade_id) references pensen.grade (id) on update cascade on delete cascade,
  foreign key (type_id) references pensen.lesson_type (id) on update cascade on delete cascade
);
grant insert, select, update, delete on table pensen.lesson_table_entry to "pensenmanager";

insert into pensen.lesson_table_entry (curriculum_id, division_id, subject_id, grade_id, type_id, lessons1, lessons2)
select curriculum_id, division_id, subject_id, 1, 2, lessons1[1], lessons2[1]
from pensen.default_lessons where curriculum_id = 3;

insert into pensen.lesson_table_entry (curriculum_id, division_id, subject_id, grade_id, type_id, lessons1, lessons2)
select curriculum_id, division_id, subject_id, 2, 2, lessons1[2], lessons2[2]
from pensen.default_lessons where curriculum_id = 3;

insert into pensen.lesson_table_entry (curriculum_id, division_id, subject_id, grade_id, type_id, lessons1, lessons2)
select curriculum_id, division_id, subject_id, 3, 2, lessons1[3], lessons2[3]
from pensen.default_lessons where curriculum_id = 3;

insert into pensen.lesson_table_entry (curriculum_id, division_id, subject_id, grade_id, type_id, lessons1, lessons2)
select curriculum_id, division_id, subject_id, 4, 2, lessons1[4], lessons2[4]
from pensen.default_lessons where curriculum_id = 3;

insert into pensen.lesson_table_entry (curriculum_id, division_id, subject_id, grade_id, type_id, lessons1, lessons2)
select curriculum_id, division_id, subject_id, 5, 2, lessons1[5], lessons2[5]
from pensen.default_lessons where curriculum_id = 3;


delete from pensen.lesson_table_entry where lessons1 < 0 and lessons2 < 0;
