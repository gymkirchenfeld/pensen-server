create schema pensen;
grant usage on schema pensen to "pensenmanager";

-- table account
create sequence pensen.account_id;
grant usage on sequence pensen.account_id to "pensenmanager";


create table pensen.account (
  id integer not null primary key,
  name text not null unique,
  edit_allowed boolean not null default false,
  grant_allowed boolean not null default false
);
grant delete, insert, select, update on table pensen.account to "pensenmanager";

-- table settings
create sequence pensen.settings_id;
grant usage on sequence pensen.settings_id to "pensenmanager";

create table pensen.settings (
  id integer not null primary key,
  account_id integer not null,
  mail_body text,
  mail_from text,
  mail_subject text,
  foreign key (account_id) references pensen.account (id) on update cascade on delete cascade
);
grant insert, select, update on table pensen.settings to "pensenmanager";

-- table subject_category
create table pensen.subject_category (
  id integer not null primary key,
  code text not null unique,
  description text not null
);
grant select on table pensen.subject_category to "pensenmanager";

-- table subject_type
create table pensen.subject_type (
  id integer not null primary key,
  code text not null,
  description text not null,
  sort_order integer not null
);
grant select on table pensen.subject_type to "pensenmanager";

-- table subject
create sequence pensen.subject_id;
grant usage on sequence pensen.subject_id to "pensenmanager";

create table pensen.subject (
  id integer not null primary key,
  archived boolean not null default false,
  code text,
  cross_class boolean not null default false,
  description text,
  category_id integer,
  type_id integer not null,
  evento_code text,
  sort_order integer not null default 0,
  foreign key (category_id) references pensen.subject_category (id) on update cascade,
  foreign key (type_id) references pensen.subject_type (id) on update cascade
);
grant delete, insert, select, update on table pensen.subject to "pensenmanager";

-- table payroll_type
create table pensen.payroll_type (
  id integer not null primary key,
  code text not null,
  description text not null,
  lesson_based boolean not null default true,
  saldo_resolving_order integer not null,
  ipb_correction_allowed boolean not null default false,
  constraint ipb_correction_allowed_only_if_lesson_based check (lesson_based = true or ipb_correction_allowed = false)
);
grant select on table pensen.payroll_type to "pensenmanager";

-- table curriculum
create sequence pensen.curriculum_id;
grant usage on sequence pensen.curriculum_id to "pensenmanager";

create table pensen.curriculum (
  id integer not null primary key,
  code text not null,
  description text not null,
  archived boolean not null default false
);
grant delete, insert, select, update on table pensen.curriculum to "pensenmanager";

-- table grade
create sequence pensen.grade_id;
grant usage on sequence pensen.grade_id to "pensenmanager";

create table pensen.grade (
  id integer not null primary key,
  code text not null,
  description text not null,
  payroll_type_id integer not null,
  archived boolean not null default false,
  class_lesson_payroll_type_id integer not null,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade,
  foreign key (class_lesson_payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant delete, insert, select, update on table pensen.grade to "pensenmanager";

-- table curriculum_grade
create table pensen.curriculum_grade (
  curriculum_id integer not null,
  grade_id integer not null,
  primary key (curriculum_id, grade_id),
  foreign key (curriculum_id) references pensen.curriculum (id) on update cascade,
  foreign key (grade_id) references pensen.grade (id) on update cascade
);
grant select on table pensen.curriculum_grade to "pensenmanager";

-- table calculation_mode
create table pensen.calculation_mode (
  id integer not null primary key,
  code text not null,
  description text not null
);
grant select on table pensen.calculation_mode to "pensenmanager";

-- table school_year
create sequence pensen.school_year_id;
grant usage on sequence pensen.school_year_id to "pensenmanager";

create table pensen.school_year (
  id integer not null primary key,
  archived boolean not null default false,
  code text,
  finalised boolean not null default false,
  graduation_year integer not null,
  description text,
  weeks integer not null,
  calculation_mode_id integer not null,
  foreign key (calculation_mode_id) references pensen.calculation_mode (id) on update cascade on delete cascade
);
grant delete, insert, select, update on table pensen.school_year to "pensenmanager";

-- table weekly_lessons
create table pensen.weekly_lessons (
  school_year_id integer not null,
  payroll_type_id integer not null,
  lessons double precision not null,
  primary key (school_year_id, payroll_type_id),
  foreign key (school_year_id) references pensen.school_year (id) on update cascade,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant delete, insert, select on table pensen.weekly_lessons to "pensenmanager";

-- table division
create sequence pensen.division_id;
grant usage on sequence pensen.division_id to "pensenmanager";

create table pensen.division (
  id integer not null primary key,
  code text not null unique,
  description text not null,
  head_name text,
  head_signature bytea,
  logo bytea,
  grouping text,
  head_title text
);
grant delete, insert, select, update on table pensen.division to "pensenmanager";

-- table lesson_type
create table pensen.lesson_type (
  id integer not null primary key,
  code text not null,
  description text not null
);
grant select on table pensen.lesson_type to "pensenmanager";

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

-- table school_class
create sequence pensen.school_class_id;
grant usage on sequence pensen.school_class_id to "pensenmanager";

create table pensen.school_class (
  id integer not null primary key,
  archived boolean not null default false,
  code text,
  graduation_year integer not null,
  curriculum_id integer not null,
  division_id integer not null,
  foreign key (curriculum_id) references pensen.curriculum (id) on update cascade,
  foreign key (division_id) references pensen.division (id) on update cascade
);
grant delete, insert, select, update on table pensen.school_class to "pensenmanager";

-- table gender
create table pensen.gender (
  id integer not null primary key,
  code text not null,
  description text not null,
  salutation text
);
grant select on table pensen.gender to "pensenmanager";

-- table teacher
create sequence pensen.teacher_id;
grant usage on sequence pensen.teacher_id to "pensenmanager";

create table pensen.teacher (
  id integer not null primary key,
  archived boolean not null default false,
  code text,
  title text,
  first_name text,
  last_name text,
  email text,
  birthday date,
  employee_number text,
  gender_id integer not null,
  foreign key (gender_id) references pensen.gender (id) on update cascade
);
grant insert, select, update on table pensen.teacher to "pensenmanager";

-- table note
create sequence pensen.note_id;
grant usage on sequence pensen.note_id to "pensenmanager";

create table pensen.note (
  id integer not null primary key,
  teacher_id integer not null,
  text text,
  created_by text,
  created_on timestamp,
  foreign key (teacher_id) references pensen.teacher (id) on update cascade
);
grant insert, select, update, delete on table pensen.note to "pensenmanager";

-- table teacher_department
create table pensen.teacher_department (
  teacher_id integer not null,
  subject_category_id integer not null,
  primary key (teacher_id, subject_category_id),
  foreign key (teacher_id) references pensen.teacher (id) on update cascade,
  foreign key (subject_category_id) references pensen.subject_category (id) on update cascade
);
grant delete, insert, select on table pensen.teacher_department to "pensenmanager";

-- table thesis_type
create table pensen.thesis_type (
  id integer not null primary key,
  code text not null,
  description text not null,
  percent double precision not null,
  payroll_type_id integer not null,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant select on table pensen.thesis_type to "pensenmanager";

-- table employment
create sequence pensen.employment_id;
grant usage on sequence pensen.employment_id to "pensenmanager";

create table pensen.employment (
  id integer not null primary key,
  teacher_id integer not null,
  school_year_id integer not null,
  division_id integer not null,
  employment_min double precision,
  employment_max double precision,
  opening_balance double precision,
  closing_balance double precision,
  payment1 double precision,
  payment2 double precision,
  comments text,
  temporary boolean not null,
  unique (school_year_id, teacher_id),
  foreign key (teacher_id) references pensen.teacher (id) on update cascade,
  foreign key (school_year_id) references pensen.school_year (id) on update cascade,
  foreign key (division_id) references pensen.division (id) on update cascade
);
grant delete, insert, select, update on table pensen.employment to "pensenmanager";

-- table thesis_entry
create table pensen.thesis_entry (
  school_year_id integer not null,
  teacher_id integer not null,
  type_id integer not null,
  count double precision not null,
  primary key (school_year_id, teacher_id, type_id),
  foreign key (school_year_id, teacher_id) references pensen.employment (school_year_id, teacher_id) on update cascade,
  foreign key (type_id) references pensen.thesis_type (id)
);
grant delete, insert, select on table pensen.thesis_entry to "pensenmanager";

-- table course
create sequence pensen.course_id;
grant usage on sequence pensen.course_id to "pensenmanager";

create table pensen.course (
  id integer not null primary key,
  cancelled boolean not null default false,
  comments text,
  grade_id integer not null,
  lessons1 double precision not null,
  lessons2 double precision not null,
  school_class_ids integer[],
  school_year_id integer not null,
  cross_class boolean not null,
  subject_id integer not null,
  teacher_ids1 integer[],
  teacher_ids2 integer[],
  curriculum_id integer not null,
  foreign key (curriculum_id) references pensen.curriculum (id) on update cascade,
  foreign key (school_year_id) references pensen.school_year (id) on update cascade,
  foreign key (grade_id) references pensen.grade (id) on update cascade,
  foreign key (subject_id) references pensen.subject (id) on update cascade
);
grant delete, insert, select, update on table pensen.course to "pensenmanager";

-- table pool_type
create table pensen.pool_type (
  id integer not null primary key,
  code text not null,
  description text not null,
  auto_copy boolean not null default true,
  payroll_type_id integer not null,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant select on table pensen.pool_type to "pensenmanager";

-- table pool_entry
create sequence pensen.pool_entry_id;
grant usage on sequence pensen.pool_entry_id to "pensenmanager";

create table pensen.pool_entry (
  id integer not null primary key,
  teacher_id integer not null,
  school_year_id integer not null,
  description text,
  percent1 double precision,
  percent2 double precision,
  type_id integer not null,
  foreign key (teacher_id) references pensen.teacher (id) on update cascade,
  foreign key (school_year_id) references pensen.school_year (id) on update cascade,
  foreign key (type_id) references pensen.pool_type (id) on update cascade
);
grant delete, insert, select, update on table pensen.pool_entry to "pensenmanager";

-- table posting_type
create table pensen.posting_type (
  id integer not null primary key,
  code text not null,
  description text not null,
  payroll_type_id integer not null,
  percent boolean not null default false,
  foreign key (payroll_type_id) references pensen.payroll_type (id) on update cascade
);
grant select on table pensen.posting_type to "pensenmanager";

-- table posting
create sequence pensen.posting_id;
grant usage on sequence pensen.posting_id to "pensenmanager";

create table pensen.posting (
  id integer not null primary key,
  teacher_id integer not null,
  school_year_id integer not null,
  description text,
  start_date date,
  end_date date,
  foreign key (teacher_id) references pensen.teacher (id) on update cascade,
  foreign key (school_year_id) references pensen.school_year (id) on update cascade
);
grant delete, insert, select, update on table pensen.posting to "pensenmanager";

-- table posting_detail
create table pensen.posting_detail (
  posting_id integer not null,
  type_id integer not null,
  school_year_id integer not null,
  teacher_id integer not null,
  value double precision not null,
  primary key (posting_id, type_id),
  foreign key (posting_id) references pensen.posting (id) on update cascade on delete cascade,
  foreign key (school_year_id) references pensen.school_year (id),
  foreign key (teacher_id) references pensen.teacher (id),
  foreign key (type_id) references pensen.posting_type (id)
);
grant delete, insert, select on table pensen.posting_detail to "pensenmanager";
