/*
 * Copyright (C) 2022 - 2023 by Sebastian Forster, Stefan Rothe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY); without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.kinet.pensen.data;

import ch.kinet.BaseData;
import ch.kinet.Binary;
import ch.kinet.DataManager;
import ch.kinet.Date;
import ch.kinet.Entities;
import ch.kinet.JsonArray;
import ch.kinet.PropertyMap;
import ch.kinet.SetComparison;
import ch.kinet.Timestamp;
import ch.kinet.Util;
import ch.kinet.pensen.calculation.Calculation;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.server.Configuration;
import ch.kinet.sql.Condition;
import ch.kinet.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PensenData extends BaseData implements Context {

    private final Entities<CalculationMode> calculationModes = Entities.create();
    private final Entities<Curriculum> curriculums = Entities.create();
    private final Entities<Division> divisions = Entities.create();
    private final Entities<Gender> genders = Entities.create();
    private final Entities<Grade> grades = Entities.create();
    private final Lookup<LessonType> lessonTypes = Lookup.create();
    private final Object lock = new Object();
    private final Entities<PayrollType> payrollTypes = Entities.create();
    private final Entities<PoolType> poolTypes = Entities.create();
    private final Entities<PostingType> postingTypes = Entities.create();
    private final Entities<SchoolClass> schoolClasses = Entities.create();
    private final Entities<SchoolYear> schoolYears = Entities.create();
    private final Entities<SubjectCategory> subjectCategories = Entities.create();
    private final Entities<Subject> subjects = Entities.create();
    private final Entities<SubjectType> subjectTypes = Entities.create();
    private final Entities<Teacher> teachers = Entities.create();
    private final Entities<ThesisType> thesisTypes = Entities.create();
    private final String schema;
    private Map<String, Authorisation> authorisations = new HashMap<>();

    public PensenData() {
        schema = Configuration.getInstance().getDbSchema();
    }

    @Override
    protected void doInitDependencies(DataManager dataManager) {
    }

    @Override
    protected void doInitLookups(Connection connection) {
        connection.addLookup(Authorisation.class);
        connection.addLookup(Course.class);
        connection.addLookup(Curriculum.class);
        connection.addLookup(Division.class);
        connection.addLookup(Gender.class);
        connection.addLookup(Grade.class);
        connection.addLookup(CalculationMode.class);
        connection.addLookup(LessonType.class);
        connection.addLookup(PayrollType.class);
        connection.addLookup(PoolType.class);
        connection.addLookup(Posting.class);
        connection.addLookup(PostingType.class);
        connection.addLookup(SchoolClass.class);
        connection.addLookup(SchoolYear.class);
        connection.addLookup(Subject.class);
        connection.addLookup(SubjectCategory.class);
        connection.addLookup(SubjectType.class);
        connection.addLookup(Teacher.class);
        connection.addLookup(ThesisType.class);
    }

    @Override
    protected void doInitData() {
        subjectCategories.addAll(getConnection().selectAll(schema, SubjectCategory.class));
        calculationModes.addAll(getConnection().selectAll(schema, CalculationMode.class));
        divisions.addAll(getConnection().selectAll(schema, Division.class));
        genders.addAll(getConnection().selectAll(schema, Gender.class));
        lessonTypes.addAll(getConnection().selectAll(schema, LessonType.class));
        payrollTypes.addAll(getConnection().selectAll(schema, PayrollType.class));
        poolTypes.addAll(getConnection().selectAll(schema, PoolType.class));
        postingTypes.addAll(getConnection().selectAll(schema, PostingType.class));
        schoolYears.addAll(getConnection().selectAll(schema, SchoolYear.class));
        thesisTypes.addAll(getConnection().selectAll(schema, ThesisType.class));
        getConnection().selectAll(schema, WeeklyLessons.class).forEachOrdered(item -> {
            item.getSchoolYear().putWeeklyLessons(item.getPayrollType(), item.getLessons());
        });

        // use getSchoolYears to get sorted list
        SchoolYear previous = null;
        for (SchoolYear current : schoolYears) {
            current.setPrevious(previous);
            previous = current;
        }

        authorisations = getConnection().selectAll(schema, Authorisation.class).collect(Collectors.toMap(
            item -> item.getAccountName(), item -> item
        ));
        grades.addAll(getConnection().selectAll(schema, Grade.class));
        curriculums.addAll(getConnection().selectAll(schema, Curriculum.class));
        getConnection().selectAll(schema, CurriculumGrade.class).forEachOrdered(
            item -> item.getCurriculum().addGrade(item.getGrade())
        );

        curriculums.forEach(curriculum -> curriculum.sortGrades());
        schoolClasses.addAll(getConnection().selectAll(schema, SchoolClass.class));
        teachers.addAll(getConnection().selectAll(schema, Teacher.class));

        getConnection().selectAll(schema, TeacherDepartment.class).forEachOrdered(item -> {
            item.getTeacher().getDepartments().add(item.getSubjectCategory());
        });

        subjectTypes.addAll(getConnection().selectAll(schema, SubjectType.class));
        subjects.addAll(getConnection().selectAll(schema, Subject.class));
    }

    public Course copyCourse(Course original, double lessons1, double lessons2, SchoolYear schoolYear, Grade grade) {
        Course result = createCourse(
            original.getComments(), original.getCurriculum(), grade, lessons1, lessons2, schoolYear, original.getSubject()
        );

        Set<Teacher> origTeachers1 = original.teachers(SemesterEnum.First).collect(Collectors.toSet());
        Set<Teacher> origTeachers2 = original.teachers(SemesterEnum.Second).collect(Collectors.toSet());
        if (lessons1 > 0) {
            result.setTeachers1(origTeachers1.isEmpty() ? origTeachers2.stream() : origTeachers1.stream());
        }

        if (lessons2 > 0) {
            result.setTeachers2(origTeachers2.isEmpty() ? origTeachers1.stream() : origTeachers2.stream());
        }

        result.setSchoolClasses(original.schoolClasses());
        updateCourse(result, Util.createSet(
                     Course.DB_SCHOOL_CLASS_IDS, Course.DB_TEACHER_IDS_1, Course.DB_TEACHER_IDS_2)
        );
        return result;
    }

    public Course createCourse(String comments, Curriculum curriculum, Grade grade, double lessons1, double lessons2,
                               SchoolYear schoolYear, Subject subject) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Course.DB_CANCELLED, false);
        properties.put(Course.DB_COMMENTS, comments);
        properties.put(Course.DB_CURRICULUM, curriculum);
        properties.put(Course.DB_GRADE, grade);
        properties.put(Course.DB_LESSONS_1, lessons1);
        properties.put(Course.DB_LESSONS_2, lessons2);
        properties.put(Course.DB_SCHOOL_YEAR, schoolYear);
        properties.put(Course.DB_SUBJECT, subject);
        properties.put(Course.DB_CROSS_CLASS, subject.isCrossClass());
        return getConnection().insert(schema, Course.class, properties);
    }

    public Curriculum createCurriculum(String code, String description) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Curriculum.DB_ARCHIVED, false);
        properties.put(Curriculum.DB_CODE, code);
        properties.put(Curriculum.DB_DESCRIPTION, description);
        Curriculum result = getConnection().insert(schema, Curriculum.class, properties);
        curriculums.add(result);
        return result;
    }

    public Division createDivision(String code, String description, String grouping, String headName,
                                   Binary headSignature, String headTitle, Binary logo) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Division.DB_CODE, code);
        properties.put(Division.DB_DESCRIPTION, description);
        properties.put(Division.DB_GROUPING, grouping);
        properties.put(Division.DB_HEAD_NAME, headName);
        properties.put(Division.DB_HEAD_SIGNATURE, headSignature);
        properties.put(Division.DB_HEAD_TITLE, headTitle);
        properties.put(Division.DB_LOGO, logo);
        Division result = getConnection().insert(schema, Division.class, properties);
        divisions.add(result);
        return result;
    }

    public Employment createEmployment(SchoolYear schoolYear, Teacher teacher, Division division,
                                       double employmentMax, double employmentMin, double payment1,
                                       double payment2, boolean temporary, String comments) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Employment.DB_COMMENTS, comments);
        properties.put(Employment.DB_DIVISION, division);
        properties.put(Employment.DB_EMPLOYMENT_MAX, employmentMax);
        properties.put(Employment.DB_EMPLOYMENT_MIN, employmentMin);
        properties.put(Employment.DB_PAYMENT1, payment1);
        properties.put(Employment.DB_PAYMENT2, payment2);
        properties.put(Employment.DB_SCHOOL_YEAR, schoolYear);
        properties.put(Employment.DB_TEACHER, teacher);
        properties.put(Employment.DB_TEMPORARY, temporary);
        return getConnection().insert(schema, Employment.class, properties);
    }

    public Note createNote(Teacher teacher, String text, String createdBy) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Note.DB_CREATED_BY, createdBy);
        properties.put(Note.DB_CREATED_ON, Timestamp.now());
        properties.put(Note.DB_TEACHER, teacher);
        properties.put(Note.DB_TEXT, text);
        return getConnection().insert(schema, Note.class, properties);
    }

    public PoolEntry createPoolEntry(String description, double percent1, double percent2,
                                     SchoolYear schoolYear, Teacher teacher, PoolType type) {
        PropertyMap properties = PropertyMap.create();
        properties.put(PoolEntry.DB_DESCRIPTION, description);
        properties.put(PoolEntry.DB_PERCENT_1, percent1);
        properties.put(PoolEntry.DB_PERCENT_2, percent2);
        properties.put(PoolEntry.DB_SCHOOL_YEAR, schoolYear);
        properties.put(PoolEntry.DB_TEACHER, teacher);
        properties.put(PoolEntry.DB_TYPE, type);
        return getConnection().insert(schema, PoolEntry.class, properties);
    }

    public Posting createPosting(String description, Date endDate, SchoolYear schoolYear, Date startDate, Teacher teacher) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Posting.DB_DESCRIPTION, description);
        properties.put(Posting.DB_END_DATE, endDate);
        properties.put(Posting.DB_SCHOOL_YEAR, schoolYear);
        properties.put(Posting.DB_START_DATE, startDate);
        properties.put(Posting.DB_TEACHER, teacher);
        return getConnection().insert(schema, Posting.class, properties);
    }

    public SchoolClass createSchoolClass(String code, Curriculum curriculum, Division division, int graduationYear) {
        PropertyMap properties = PropertyMap.create();
        properties.put(SchoolClass.DB_ARCHIVED, false);
        properties.put(SchoolClass.DB_CODE, code);
        properties.put(SchoolClass.DB_CURRICULUM, curriculum);
        properties.put(SchoolClass.DB_DIVISION, division);
        properties.put(SchoolClass.DB_GRADUATION_YEAR, graduationYear);
        SchoolClass result = getConnection().insert(schema, SchoolClass.class, properties);
        schoolClasses.add(result);
        return result;
    }

    public SchoolYear createSchoolYear(CalculationMode calculationMode, String code, String description,
                                       int graduationYear, int weeks) {
        PropertyMap properties = PropertyMap.create();
        properties.put(SchoolYear.DB_ARCHIVED, false);
        properties.put(SchoolYear.DB_CALCULATION_MODE, calculationMode);
        properties.put(SchoolYear.DB_CODE, code);
        properties.put(SchoolYear.DB_DESCRIPTION, description);
        properties.put(SchoolYear.DB_FINALISED, false);
        properties.put(SchoolYear.DB_GRADUATION_YEAR, graduationYear);
        properties.put(SchoolYear.DB_WEEKS, weeks);
        SchoolYear result = getConnection().insert(schema, SchoolYear.class, properties);
        result.setPrevious(schoolYears.last());
        schoolYears.add(result);
        return result;
    }

    public Subject createSubject(SubjectCategory category, String code, boolean crossClass, String description,
                                 String eventoCode, SubjectType type) {
        int sortOrder = 0;
        for (Subject subject : subjects) {
            sortOrder = Math.max(sortOrder, subject.getSortOrder());
        }

        PropertyMap properties = PropertyMap.create();
        properties.put(Subject.DB_ARCHIVED, false);
        properties.put(Subject.DB_CATEGORY, category);
        properties.put(Subject.DB_CODE, code);
        properties.put(Subject.DB_CROSS_CLASS, crossClass);
        properties.put(Subject.DB_DESCRIPTION, description);
        properties.put(Subject.DB_EVENTO_CODE, eventoCode);
        properties.put(Subject.DB_SORT_ORDER, sortOrder + 1);
        properties.put(Subject.DB_TYPE, type);
        Subject result = getConnection().insert(schema, Subject.class, properties);
        subjects.add(result);
        return result;
    }

    public Teacher createTeacher(Date birthday, String code, String email, String employeeNumber, String firstName,
                                 String lastName, String title) {
        PropertyMap properties = PropertyMap.create();
        properties.put(Teacher.DB_ARCHIVED, false);
        properties.put(Teacher.DB_BIRTHDAY, birthday);
        properties.put(Teacher.DB_CODE, code);
        properties.put(Teacher.DB_EMAIL, email);
        properties.put(Teacher.DB_EMPLOYEE_NUMBER, employeeNumber);
        properties.put(Teacher.DB_FIRST_NAME, firstName);
        properties.put(Teacher.DB_LAST_NAME, lastName);
        properties.put(Teacher.DB_TITLE, title);
        Teacher result = getConnection().insert(schema, Teacher.class, properties);
        teachers.add(result);
        return result;
    }

    public void deleteCourse(Course course) {
        getConnection().delete(schema, course);
    }

    public void deleteEmployment(Employment employment) {
        SchoolYear schoolYear = employment.getSchoolYear();
        Teacher teacher = employment.getTeacher();
        getConnection().delete(
            schema, PoolEntry.class,
            Condition.and(
                Condition.equals(PoolEntry.DB_SCHOOL_YEAR, schoolYear),
                Condition.equals(PoolEntry.DB_TEACHER, teacher)
            )
        );
        getConnection().delete(
            schema, Posting.class,
            Condition.and(
                Condition.equals(Posting.DB_SCHOOL_YEAR, schoolYear),
                Condition.equals(Posting.DB_TEACHER, teacher)
            )
        );
        getConnection().delete(
            schema, ThesisEntry.class,
            Condition.and(
                Condition.equals(ThesisEntry.DB_SCHOOL_YEAR, schoolYear),
                Condition.equals(ThesisEntry.DB_TEACHER, teacher)
            )
        );
        loadAllCourses(schoolYear).filter(course -> course.contains(teacher)).forEachOrdered(course -> {
            removeTeacher(course, teacher);
        });

        getConnection().delete(schema, employment);
    }

    public void deleteNote(Note note) {
        getConnection().delete(schema, note);
    }

    public void deletePoolEntry(PoolEntry poolEntry) {
        getConnection().delete(schema, poolEntry);
    }

    public void deletePosting(Posting posting) {
        getConnection().delete(schema, posting);
    }

    public boolean deleteSchoolClass(SchoolClass schoolClass) {
        // remove school class from all courses
        for (SchoolYear schoolYear : schoolYears) {
            List<Course> courses = loadCourses(schoolYear, false).collect(Collectors.toList());
            for (Course course : courses) {
                if (course.contains(schoolClass)) {
                    if (schoolYear.isFinalised() || schoolYear.isArchived()) {
                        // Cannot delete a school class in a finalised school year
                        System.out.println("Trying to delete school class in finalised school year");
                        return false;
                    }

                    Set<SchoolClass> scs = course.schoolClasses().collect(Collectors.toSet());
                    if (scs.size() == 1) {
                        // delete course if it was only school class
                        deleteCourse(course);
                    }
                    else {
                        // otherwise, remove school class from course
                        scs.remove(schoolClass);
                        course.setSchoolClasses(scs.stream());
                        updateCourse(course, Util.createSet(Course.DB_SCHOOL_CLASS_IDS));
                    }
                }
            }
        }

        getConnection().delete(schema, schoolClass);
        schoolClasses.remove(schoolClass);
        return true;
    }

    public LessonType emptyLessonType() {
        return getLessonTypeByEnum(LessonType.Enum.NoLessons);
    }

    public Authorisation getAuthorisationByName(String name) {
        if (!authorisations.containsKey(name)) {
            return null;
        }

        return authorisations.get(name);
    }

    public CalculationMode getCalculationModeById(int id) {
        return calculationModes.byId(id);
    }

    public Curriculum getCurriculumById(int id) {
        return curriculums.byId(id);
    }

    public Division getDivisionById(int id) {
        return divisions.byId(id);
    }

    public Gender getGenderById(int id) {
        return genders.byId(id);
    }

    @Override
    public Grade getGradeById(int id) {
        return grades.byId(id);
    }

    public LessonType getLessonTypeByEnum(LessonType.Enum lessonTypeEnum) {
        return lessonTypes.byCode(lessonTypeEnum.getCode());
    }

    public LessonType getLessonTypeById(int id) {
        return lessonTypes.byId(id);
    }

    public PayrollType getPayrollTypeById(int id) {
        return payrollTypes.byId(id);
    }

    public PoolType getPoolTypeById(int id) {
        return poolTypes.byId(id);
    }

    @Override
    public SchoolClass getSchoolClassById(int id) {
        return schoolClasses.byId(id);
    }

    public SchoolYear getSchoolYearById(int id) {
        return schoolYears.byId(id);
    }

    public Subject getSubjectById(int id) {
        return subjects.byId(id);
    }

    public SemesterEnum getSemesterById(int id) {
        switch (id) {
            case 1:
                return SemesterEnum.First;
            case 2:
                return SemesterEnum.Second;
            default:
                return null;
        }
    }

    public SubjectCategory getSubjectCategoryById(int id) {
        return subjectCategories.byId(id);
    }

    public SubjectType getSubjectTypeById(int id) {
        return subjectTypes.byId(id);
    }

    @Override
    public Teacher getTeacherById(int id) {
        return teachers.byId(id);
    }

    public Stream<Course> loadAllCourses(SchoolYear schoolYear) {
        Condition where = Condition.equals(Course.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, Course.class, where).map(course -> course.resolve(this));
    }

    public Course loadCourse(int id) {
        Condition where = Condition.equals(Course.DB_ID, id);
        return getConnection().selectOne(schema, Course.class, where).resolve(this);
    }

    public Stream<Course> loadCourses(SchoolYear schoolYear, boolean crossClass) {
        Condition where = Condition.and(
            Condition.equals(Course.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(Course.DB_CROSS_CLASS, crossClass)
        );
        return getConnection().select(schema, Course.class, where).map(course -> course.resolve(this));
    }

    public CourseTable loadCourseTable(SchoolYear schoolYear, Division division, Grade grade,
                                       SubjectCategory subjectCategory) {
        return CourseTable.create(
            streamSchoolClassesFor(schoolYear, null).filter(schoolClass -> schoolClass.filter(division, grade, schoolYear)),
            streamSubjects().filter(subject -> !subject.isCrossClass() && !subject.isArchived() && subject.filter(subjectCategory)),
            loadCourses(schoolYear, false)
        );
    }

    public Employment loadEmployment(int id) {
        Condition where = Condition.equals(Employment.DB_ID, id);
        return getConnection().selectOne(schema, Employment.class, where);
    }

    public Employment loadEmployment(SchoolYear schoolYear, Teacher teacher) {
        Condition where = Condition.and(
            Condition.equals(Employment.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(Employment.DB_TEACHER, teacher)
        );
        return getConnection().selectOne(schema, Employment.class, where);
    }

    public Stream<Employment> loadEmployments(SchoolYear schoolYear, Division division) {
        Condition where = Condition.equals(Employment.DB_SCHOOL_YEAR, schoolYear);
        if (division != null) {
            where = Condition.and(where, Condition.equals(Employment.DB_DIVISION, division));
        }

        return getConnection().select(schema, Employment.class, where).sorted();
    }

    public LessonTable loadLessonTable(Curriculum curriculum, Division division) {
        LessonType emptyType = emptyLessonType();
        return LessonTable.create(
            curriculum, division, emptyType, streamSubjects(), loadLessonTableEntriesRaw(curriculum, division)
        );
    }

    public Stream<LessonTable.Entry> loadLessonTableEntries(Curriculum curriculum, Division division, Subject subject) {
        LessonType emptyType = emptyLessonType();
        Map<Grade, LessonTable.Entry> map = curriculum.grades().collect(
            Collectors.toMap(grade -> grade, grade -> LessonTable.createEntry(grade, emptyType))
        );
        Condition where = Condition.and(
            Condition.equals(LessonTableEntry.DB_CURRICULUM, curriculum),
            Condition.equals(LessonTableEntry.DB_SUBJECT, subject),
            division == null ? Condition.isNull(LessonTableEntry.DB_DIVISION) :
                Condition.equals(LessonTableEntry.DB_DIVISION, division));
        getConnection().select(schema, LessonTableEntry.class, where).forEachOrdered(item -> {
            if (map.containsKey(item.getGrade())) {
                map.get(item.getGrade()).setData(item);
            }
        });
        return map.values().stream().sorted();
    }

    public Stream<LessonTableEntry> loadLessonTableEntriesRaw(Curriculum curriculum, Division division) {
        Condition where = Condition.and(
            Condition.equals(LessonTableEntry.DB_CURRICULUM, curriculum),
            division == null ? Condition.isNull(LessonTableEntry.DB_DIVISION) :
                Condition.equals(LessonTableEntry.DB_DIVISION, division));
        return getConnection().select(schema, LessonTableEntry.class, where);
    }

    public LessonTableEntry loadLessonTableEntry(Curriculum curriculum, Division division, Subject subject, Grade grade) {
        Condition where = Condition.and(
            Condition.equals(LessonTableEntry.DB_CURRICULUM, curriculum),
            Condition.equals(LessonTableEntry.DB_SUBJECT, subject),
            Condition.equals(LessonTableEntry.DB_GRADE, grade),
            division == null ? Condition.isNull(LessonTableEntry.DB_DIVISION) :
                Condition.equals(LessonTableEntry.DB_DIVISION, division));
        return getConnection().selectOne(schema, LessonTableEntry.class, where);
    }

    public Note loadNote(int id) {
        Condition where = Condition.equals(Note.DB_ID, id);
        return getConnection().selectOne(schema, Note.class, where);
    }

    public Stream<Note> loadNotes(Teacher teacher) {
        Condition where = Condition.equals(Note.DB_TEACHER, teacher);
        return getConnection().select(schema, Note.class, where).sorted();
    }

    public Stream<PoolEntry> loadPoolEntries(SchoolYear schoolYear) {
        Condition where = Condition.equals(PoolEntry.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, PoolEntry.class, where).sorted();
    }

    public Stream<PoolEntry> loadPoolEntries(SchoolYear schoolYear, Teacher teacher) {
        Condition where = Condition.and(
            Condition.equals(PoolEntry.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(PoolEntry.DB_TEACHER, teacher)
        );
        return getConnection().select(schema, PoolEntry.class, where).sorted();
    }

    public PoolEntry loadPoolEntry(int id) {
        Condition where = Condition.equals(PoolEntry.DB_ID, id);
        return getConnection().selectOne(schema, PoolEntry.class, where);
    }

    public Stream<PostingDetail> loadPostingDetails(SchoolYear schoolYear) {
        Condition where = Condition.equals(PostingDetail.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, PostingDetail.class, where);
    }

    public Stream<PostingDetail> loadPostingDetails(SchoolYear schoolYear, Teacher teacher) {
        Condition where = Condition.and(
            Condition.equals(PostingDetail.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(PostingDetail.DB_TEACHER, teacher)
        );
        return getConnection().select(schema, PostingDetail.class, where);
    }

    public ValueMap<PostingType> loadPostingDetails(Posting posting) {
        ValueMap<PostingType> result = ValueMap.create(streamPostingTypes());
        Condition where = Condition.equals(PostingDetail.DB_POSTING, posting);
        getConnection().select(schema, PostingDetail.class, where).forEachOrdered(item -> {
            result.put(item.getType(), item.getValue());
        });
        return result;
    }

    public Stream<Posting> loadPostings(SchoolYear schoolYear) {
        Condition where = Condition.equals(Posting.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, Posting.class, where).sorted();
    }

    public Stream<Posting> loadPostings(SchoolYear schoolYear, Teacher teacher) {
        Condition where = Condition.and(
            Condition.equals(Posting.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(Posting.DB_TEACHER, teacher)
        );

        return getConnection().select(schema, Posting.class, where).sorted();
    }

    public Posting loadPosting(int id) {
        Condition where = Condition.equals(Posting.DB_ID, id);
        return getConnection().selectOne(schema, Posting.class, where);
    }

    public Settings loadSettings(Authorisation authorisation) {
        Condition where = Condition.equals(Settings.DB_ACCOUNT, authorisation);
        synchronized (lock) {
            Settings result = getConnection().selectOne(schema, Settings.class, where);
            if (result == null) {
                PropertyMap properties = PropertyMap.create();
                properties.put(Settings.DB_ACCOUNT, authorisation);
                result = getConnection().insert(schema, Settings.class, properties);
            }

            return result;
        }
    }

    public Stream<Teacher> loadTeachersForSchoolYear(SchoolYear schoolYear) {
        Condition where = Condition.equals(Employment.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, Employment.class, where).map(item -> item.getTeacher()).sorted();
    }

    public Stream<Employment> loadTeacherHistory(Teacher teacher) {
        Condition where = Condition.equals(Employment.DB_TEACHER, teacher);
        return getConnection().select(schema, Employment.class, where).sorted();
    }

    public Stream<ThesisEntry> loadThesisEntries(SchoolYear schoolYear) {
        Condition where = Condition.equals(ThesisEntry.DB_SCHOOL_YEAR, schoolYear);
        return getConnection().select(schema, ThesisEntry.class, where);
    }

    public Stream<ThesisEntry> loadThesisEntries(SchoolYear schoolYear, Teacher teacher) {
        Condition where = Condition.and(
            Condition.equals(ThesisEntry.DB_SCHOOL_YEAR, schoolYear),
            Condition.equals(ThesisEntry.DB_TEACHER, teacher)
        );
        return getConnection().select(schema, ThesisEntry.class, where);
    }

    public Workload loadWorkload(Employment employment) {
        SchoolYear schoolYear = employment.getSchoolYear();
        Teacher teacher = employment.getTeacher();
        return createWorkload(
            employment,
            loadAllCourses(schoolYear).filter(course -> course.contains(teacher) && !course.isCancelled()),
            loadPoolEntries(schoolYear, teacher),
            loadPostings(schoolYear, teacher),
            loadPostingDetails(schoolYear, teacher),
            loadThesisEntries(schoolYear, teacher)
        );
    }

    public Workloads loadWorkloads(SchoolYear schoolYear, Division division) {
        List<Course> courses = loadAllCourses(schoolYear).collect(Collectors.toList());
        List<PoolEntry> poolEntries = loadPoolEntries(schoolYear).collect(Collectors.toList());
        List<Posting> postings = loadPostings(schoolYear).collect(Collectors.toList());
        List<PostingDetail> postingDetails = loadPostingDetails(schoolYear).collect(Collectors.toList());
        List<ThesisEntry> thesisEntries = loadThesisEntries(schoolYear).collect(Collectors.toList());
        Map<Teacher, Workload> map = loadEmployments(schoolYear, division).collect(Collectors.toMap(
            employment -> employment.getTeacher(),
            employment -> {
                Teacher teacher = employment.getTeacher();
                return createWorkload(
                    employment,
                    courses.stream().filter(course -> course.contains(teacher) && !course.isCancelled()),
                    poolEntries.stream().filter(item -> item.filter(teacher)),
                    postings.stream().filter(item -> item.filter(teacher)),
                    postingDetails.stream().filter(item -> item.filter(teacher)),
                    thesisEntries.stream().filter(item -> item.filter(teacher))
                );
            }));

        return Workloads.create(schoolYear, map);
    }

    public Set<SchoolClass> parseSchoolClasses(JsonArray json) {
        Set<SchoolClass> result = new HashSet<>();
        if (json == null) {
            return result;
        }

        for (int i = 0; i < json.length(); ++i) {
            SchoolClass schoolClass = getSchoolClassById(json.getObjectId(i, -1));
            if (schoolClass != null) {
                result.add(schoolClass);
            }
        }

        return result;
    }

    public Set<Teacher> parseTeachers(JsonArray json) {
        Set<Teacher> result = new HashSet<>();
        if (json == null) {
            return result;
        }

        for (int i = 0; i < json.length(); ++i) {
            Teacher teacher = getTeacherById(json.getObjectId(i, -1));
            if (teacher != null) {
                result.add(teacher);
            }
        }

        return result;
    }

    public void recalculateBalance(SchoolYear schoolYear) {
        loadEmployments(schoolYear, null).forEachOrdered(employment -> recalculateBalance(employment));
    }

    public void recalculateBalance(SchoolYear schoolYear, Teacher teacher) {
        recalculateBalance(loadEmployment(schoolYear, teacher));
    }

    public void recalculateBalance(Employment employment) {
        if (employment == null) {
            return;
        }

        Workload workload = loadWorkload(employment);
        employment.setClosingBalance(workload.getClosingBalance());
        getConnection().update(schema, employment, Employment.DB_CLOSING_BALANCE);
        // update opening balance of next school year
        Employment next = loadNextEmployment(employment);
        if (next != null) {
            next.setOpeningBalance(workload.getClosingBalance());
            getConnection().update(schema, next, Employment.DB_OPENING_BALANCE);
        }
    }

    public void saveLessonTableEntries(Curriculum curriculum, Division division, Subject subject,
                                       Stream<LessonTable.Entry> entries) {
        synchronized (lock) {
            Condition where = Condition.and(
                Condition.equals(LessonTableEntry.DB_CURRICULUM, curriculum),
                Condition.equals(LessonTableEntry.DB_SUBJECT, subject),
                division == null ? Condition.isNull(LessonTableEntry.DB_DIVISION) :
                    Condition.equals(LessonTableEntry.DB_DIVISION, division));
            getConnection().delete(schema, LessonTableEntry.class, where);
            entries.filter(entry -> entry.typeEnum() != LessonType.Enum.NoLessons).forEachOrdered(entry -> {
                PropertyMap properties = PropertyMap.create();
                properties.put(LessonTableEntry.DB_CURRICULUM, curriculum);
                properties.put(LessonTableEntry.DB_DIVISION, division);
                properties.put(LessonTableEntry.DB_SUBJECT, subject);
                properties.put(LessonTableEntry.DB_GRADE, entry.getGrade());
                properties.put(LessonTableEntry.DB_LESSONS_1, entry.getLessons1());
                properties.put(LessonTableEntry.DB_LESSONS_2, entry.getLessons2());
                properties.put(LessonTableEntry.DB_TYPE, entry.getType());
                getConnection().insert(schema, LessonTableEntry.class, properties);
            });
        }
    }

    public void savePostingDetails(Posting posting, ValueMap<PostingType> map) {
        synchronized (lock) {
            Condition where = Condition.equals(PostingDetail.DB_POSTING, posting);
            getConnection().delete(schema, PostingDetail.class, where);
            map.stream().filter(entry -> entry.getValue() != 0).forEachOrdered(entry -> {
                PropertyMap properties = PropertyMap.create();
                properties.put(PostingDetail.DB_POSTING, posting);
                properties.put(PostingDetail.DB_SCHOOL_YEAR, posting.getSchoolYear());
                properties.put(PostingDetail.DB_TEACHER, posting.getTeacher());
                properties.put(PostingDetail.DB_TYPE, entry.getKey());
                properties.put(PostingDetail.DB_VALUE, entry.getValue());
                getConnection().insert(schema, PostingDetail.class, properties);
            });
        }
    }

    public void saveThesisEntries(SchoolYear schoolYear, Teacher teacher, ValueMap<ThesisType> map) {
        synchronized (lock) {
            Condition where = Condition.and(
                Condition.equals(ThesisEntry.DB_SCHOOL_YEAR, schoolYear),
                Condition.equals(ThesisEntry.DB_TEACHER, teacher)
            );
            getConnection().delete(schema, ThesisEntry.class, where);
            map.stream().filter(entry -> entry.getValue() != 0).forEachOrdered(entry -> {
                PropertyMap properties = PropertyMap.create();
                properties.put(ThesisEntry.DB_SCHOOL_YEAR, schoolYear);
                properties.put(ThesisEntry.DB_TEACHER, teacher);
                properties.put(ThesisEntry.DB_TYPE, entry.getKey());
                properties.put(ThesisEntry.DB_COUNT, entry.getValue());
                getConnection().insert(schema, ThesisEntry.class, properties);
            });
        }
    }

    public void saveWeeklyLessons(SchoolYear schoolYear, ValueMap<PayrollType> map) {
        synchronized (lock) {
            Condition where = Condition.equals(WeeklyLessons.DB_SCHOOL_YEAR, schoolYear);
            getConnection().delete(schema, WeeklyLessons.class, where);
            schoolYear.clearWeeklyLessons();
            map.stream().filter(entry -> entry.getValue() != 0).forEachOrdered(entry -> {
                schoolYear.putWeeklyLessons(entry.getKey(), entry.getValue());
                PropertyMap properties = PropertyMap.create();
                properties.put(WeeklyLessons.DB_SCHOOL_YEAR, schoolYear);
                properties.put(WeeklyLessons.DB_PAYROLL_TYPE, entry.getKey());
                properties.put(WeeklyLessons.DB_LESSONS, entry.getValue());
                getConnection().insert(schema, WeeklyLessons.class, properties);
            });
        }
    }

    public Stream<CalculationMode> streamCalculationModes() {
        return calculationModes.stream();
    }

    public Stream<Curriculum> streamCurriculums() {
        return curriculums.stream();
    }

    public Stream<Division> streamDivisions() {
        return divisions.stream();
    }

    public Stream<Gender> streamGenders() {
        return genders.stream();
    }

    public Stream<Grade> streamGrades() {
        return grades.stream();
    }

    public Stream<LessonType> streamLessonTypes() {
        return lessonTypes.stream();
    }

    public Stream<PayrollType> streamPayrollTypes() {
        return payrollTypes.stream();
    }

    public Stream<PoolType> streamPoolTypes() {
        return poolTypes.stream();
    }

    public Stream<PostingType> streamPostingTypes() {
        return postingTypes.stream();
    }

    public Stream<SchoolClass> streamSchoolClasses() {
        return schoolClasses.stream();
    }

    public Stream<SchoolClass> streamSchoolClassesFor(SchoolYear schoolYear, Grade grade) {
        return schoolClasses.stream().filter(item -> {
            Grade g = item.gradeFor(schoolYear);
            return g != null && (grade == null || grade.equals(g));
        });
    }

    public Stream<SchoolYear> streamSchoolYears() {
        return schoolYears.stream();
    }

    public Stream<Subject> streamSubjects() {
        return subjects.stream();
    }

    public Stream<SubjectCategory> streamSubjectCategories() {
        return subjectCategories.stream();
    }

    public Stream<SubjectType> streamSubjectTypes() {
        return subjectTypes.stream();
    }

    public Stream<Teacher> streamTeachers() {
        return teachers.stream();
    }

    public Stream<ThesisType> streamThesisTypes() {
        return thesisTypes.stream();
    }

    public void updateCourse(Course course, Set<String> properties) {
        getConnection().update(schema, course, properties);
    }

    public void updateCurriculum(Curriculum curriculum, Set<String> properties) {
        getConnection().update(schema, curriculum, properties);
    }

    public void updateDivision(Division division, Set<String> properties) {
        getConnection().update(schema, division, properties);
    }

    public void updateEmployment(Employment employment, Set<String> properties) {
        getConnection().update(schema, employment);
    }

    public void updateGrade(Grade grade, Set<String> properties) {
        getConnection().update(schema, grade, properties);
    }

    public void updatePoolEntry(PoolEntry poolEntry, Set<String> properties) {
        getConnection().update(schema, poolEntry, properties);
    }

    public void updatePosting(Posting posting, Set<String> properties) {
        getConnection().update(schema, posting, properties);
    }

    public void updateSchoolClass(SchoolClass schoolClass, Set<String> properties) {
        getConnection().update(schema, schoolClass, properties);
    }

    public void updateSchoolYear(SchoolYear schoolYear, Set<String> properties) {
        getConnection().update(schema, schoolYear, properties);
    }

    public void updateSettings(Settings settings, Set<String> properties) {
        getConnection().update(schema, settings, properties);
    }

    public void updateSubject(Subject subject, Set<String> properties) {
        getConnection().update(schema, subject, properties);
    }

    public void updateTeacher(Teacher teacher, Set<String> properties) {
        getConnection().update(schema, teacher, properties);
    }

    public void updateTeacherDepartments(Teacher teacher, Set<SubjectCategory> departments) {
        SetComparison<SubjectCategory> comp = SetComparison.create(teacher.getDepartments(), departments);
        for (SubjectCategory removed : comp.getRemoved()) {
            teacher.getDepartments().remove(removed);
            Condition where = Condition.and(
                Condition.equals(TeacherDepartment.DB_SUBJECT_CATEGORY, removed),
                Condition.equals(TeacherDepartment.DB_TEACHER, teacher)
            );
            getConnection().delete(schema, TeacherDepartment.class, where);
        }
        for (SubjectCategory added : comp.getAdded()) {
            teacher.getDepartments().add(added);
            PropertyMap properties = PropertyMap.create();
            properties.put(TeacherDepartment.DB_SUBJECT_CATEGORY, added);
            properties.put(TeacherDepartment.DB_TEACHER, teacher);
            getConnection().insert(schema, TeacherDepartment.class, properties);
        }
    }

    private Workload createWorkload(Employment employment, Stream<Course> courses, Stream<PoolEntry> poolEntries,
                                    Stream<Posting> postings, Stream<PostingDetail> postingDetails, Stream<ThesisEntry> thesisEntries) {
        Calculation calculation = Calculation.create(employment, streamPayrollTypes());
        courses.forEachOrdered(entry -> calculation.addCourse(entry));
        poolEntries.forEachOrdered(entry -> calculation.addPoolEntry(entry));
        postings.forEachOrdered(entry -> calculation.addPosting(entry));
        postingDetails.forEachOrdered(entry -> calculation.addPostingDetail(entry));
        thesisEntries.forEachOrdered(entry -> calculation.addThesisEntry(entry));
        return calculation.createWorkload();
    }

    private Employment loadNextEmployment(Employment employment) {
        SchoolYear next = employment.getSchoolYear().next();
        if (next == null) {
            return null;
        }

        return loadEmployment(next, employment.getTeacher());
    }

    private void removeTeacher(Course course, Teacher teacher) {
        course.removeTeacher(teacher);
        updateCourse(course, Util.createSet(Course.DB_TEACHER_IDS_1, Course.DB_TEACHER_IDS_2));
        course.teachers().forEachOrdered(otherTeacher -> {
            recalculateBalance(course.getSchoolYear(), otherTeacher);
        });
    }
}
