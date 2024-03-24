/*
 * Copyright (C) 2022 - 2024 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Entity;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Course extends Entity {

    public static final String DB_CANCELLED = "Cancelled";
    public static final String DB_COMMENTS = "Comments";
    public static final String DB_CROSS_CLASS = "CrossClass";
    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_GRADE = "Grade";
    public static final String DB_LESSONS_1 = "Lessons1";
    public static final String DB_LESSONS_2 = "Lessons2";
    public static final String DB_SCHOOL_CLASS_IDS = "SchoolClassIds";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_SUBJECT = "Subject";
    public static final String DB_TEACHER_IDS_1 = "TeacherIds1";
    public static final String DB_TEACHER_IDS_2 = "TeacherIds2";
    public static final String JSON_CANCELLED = "cancelled";
    public static final String JSON_COMMENTS = "comments";
    public static final String JSON_CROSS_CLASS = "crossClass";
    public static final String JSON_CURRICULUM = "curriculum";
    public static final String JSON_GRADE = "grade";
    public static final String JSON_LESSONS_1 = "lessons1";
    public static final String JSON_LESSONS_2 = "lessons2";
    public static final String JSON_SCHOOL_CLASSES = "schoolClasses";
    public static final String JSON_SCHOOL_YEAR = "schoolYear";
    public static final String JSON_SUBJECT = "subject";
    public static final String JSON_TEACHERS_1 = "teachers1";
    public static final String JSON_TEACHERS_2 = "teachers2";

    private final boolean crossClass;
    private Curriculum curriculum;
    private final SchoolYear schoolYear;
    private final Subject subject;
    private boolean cancelled;
    private String comments;
    private Grade grade;
    private double lessons1;
    private double lessons2;
    private List<SchoolClass> schoolClasses = new ArrayList<>();
    private List<Integer> schoolClassIds = new ArrayList<>();
    private List<Teacher> teachers1 = new ArrayList<>();
    private List<Integer> teacherIds1 = new ArrayList<>();
    private List<Teacher> teachers2 = new ArrayList<>();
    private List<Integer> teacherIds2 = new ArrayList<>();

    @PropertyInitializer({DB_CROSS_CLASS, DB_GRADE, DB_ID, DB_SCHOOL_YEAR, DB_SUBJECT})
    public Course(boolean crossClass, Grade grade, int id, SchoolYear schoolYear,
                  Subject subject) {
        super(id);
        this.crossClass = crossClass;
        this.grade = grade;
        this.schoolYear = schoolYear;
        this.subject = subject;
        if (crossClass && grade == null) {
            throw new IllegalArgumentException("Cross-class course requires a grade.");
        }
    }

    public boolean contains(Teacher teacher) {
        return teachers1.contains(teacher) || teachers2.contains(teacher);
    }

    public boolean containsAny(Iterable<Teacher> teachers) {
        for (Teacher teacher : teachers) {
            if (teachers1.contains(teacher) || teachers2.contains(teacher)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(SchoolClass schoolClass) {
        return schoolClasses.contains(schoolClass);
    }

    public String displayText(SchoolClass schoolClass, SemesterEnum semester) {
        if (schoolClasses.size() > 1 && !Util.equal(schoolClass, schoolClasses.get(0))) {
            return schoolClasses.get(0).getCode();
        }

        double lessons = lessons(semester);
        List<Teacher> teachers = teachersFor(semester);
        if (teachers.isEmpty()) {
            return String.valueOf(lessons);
        }

        return teachers.stream().map(teacher -> teacher.getCode()).collect(Collectors.joining(", "));
    }

    public Stream<Division> divisions() {
        return schoolClasses().map(sc -> sc.getDivision()).distinct();
    }

    public PayrollType payrollType() {
        return subject.isClassLesson() ? grade.getClassLessonPayrollType() : grade.getPayrollType();
    }

    public String getComments() {
        return comments;
    }

    public Grade getGrade() {
        return grade;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public double getLessons1() {
        return lessons1;
    }

    public double getLessons2() {
        return lessons2;
    }

    public Stream<Integer> getSchoolClassIds() {
        return schoolClasses.stream().map(teacher -> teacher.getId());
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Subject getSubject() {
        return subject;
    }

    public Stream<Integer> getTeacherIds1() {
        return teachers1.stream().map(teacher -> teacher.getId());
    }

    public Stream<Integer> getTeacherIds2() {
        return teachers2.stream().map(teacher -> teacher.getId());
    }

    @Persistence(ignore = true)
    public boolean isMergableWith(Course other) {
        return Util.equal(subject, other.subject) &&
            Util.equal(schoolYear, other.schoolYear) &&
            Util.equal(grade, other.grade);
    }

    public boolean isCrossClass() {
        return crossClass;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public double lessons(SemesterEnum semester) {
        if (cancelled) {
            return 0d;
        }

        switch (semester) {
            case First:
                return lessons1;
            case Second:
                return lessons2;
            default:
                throw new IllegalArgumentException();
        }
    }

    public double lessonsFor(Teacher teacher, SemesterEnum semester) {
        List<Teacher> teachersForSemester = teachersFor(semester);
        if (!teachersForSemester.contains(teacher)) {
            return 0d;
        }

        int teacherCount = teachersForSemester.size();
        if (teacherCount == 0) {
            return 0d;
        }

        return lessons(semester) / teacherCount;
    }

    public boolean open() {
        double open1 = !cancelled && teachers1.isEmpty() ? lessons1 : 0;
        double open2 = !cancelled && teachers2.isEmpty() ? lessons2 : 0;
        return open1 > 0 || open2 > 0;
    }

    public double percent(SemesterEnum semester) {
        return schoolYear.lessonsToPercent(payrollType(), lessons(semester));
    }

    public double percentFor(Teacher teacher, SemesterEnum semester) {
        return schoolYear.lessonsToPercent(payrollType(), lessonsFor(teacher, semester));
    }

    public Stream<SchoolClass> schoolClasses() {
        return schoolClasses.stream();
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Deprecated
    public void setCurriculum(Curriculum curriculum) {
        this.curriculum = curriculum;
    }

    @Deprecated
    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public void setLessons1(double lessons1) {
        this.lessons1 = lessons1;
    }

    public void setLessons2(double lessons2) {
        this.lessons2 = lessons2;
    }

    @Persistence(ignore = true)
    public void setSchoolClasses(Stream<SchoolClass> schoolClasses) {
        this.schoolClasses = schoolClasses.collect(Collectors.toList());
    }

    public void setSchoolClassIds(Stream<Integer> schoolClassIds) {
        this.schoolClassIds = schoolClassIds.collect(Collectors.toList());
    }

    public void setTeacherIds1(Stream<Integer> teacherIds1) {
        this.teacherIds1 = teacherIds1.collect(Collectors.toList());
    }

    public void setTeacherIds2(Stream<Integer> teacherIds2) {
        this.teacherIds2 = teacherIds2.collect(Collectors.toList());
    }

    @Persistence(ignore = true)
    public void setTeachers1(Stream<Teacher> teachers1) {
        this.teachers1 = teachers1.collect(Collectors.toList());
    }

    @Persistence(ignore = true)
    public void setTeachers2(Stream<Teacher> teachers2) {
        this.teachers2 = teachers2.collect(Collectors.toList());
    }

    public Stream<Teacher> teachers() {
        Set<Teacher> result = new HashSet<>(teachers1);
        result.addAll(teachers2);
        return result.stream();
    }

    void removeTeacher(Teacher teacher) {
        teachers1.remove(teacher);
        teachers2.remove(teacher);
    }

    public Stream<Teacher> teachers(SemesterEnum semester) {
        return teachersFor(semester).stream();
    }

    @Override
    public JsonObject toJsonTerse() {
        double open1 = !cancelled && teachers1.isEmpty() ? lessons1 : 0;
        double open2 = !cancelled && teachers2.isEmpty() ? lessons2 : 0;
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CANCELLED, cancelled);
        result.put(JSON_COMMENTS, comments);
        result.putTerse(JSON_CURRICULUM, curriculum);
        result.putTerse(JSON_GRADE, getGrade());
        result.put(JSON_LESSONS_1, lessons1);
        result.put(JSON_LESSONS_2, lessons2);
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.put("open1", open1);
        result.put("open2", open2);
        result.put(JSON_TEACHERS_1, JsonArray.createTerse(teachers1.stream()));
        result.put(JSON_TEACHERS_2, JsonArray.createTerse(teachers2.stream()));
        result.put(JSON_SCHOOL_CLASSES, JsonArray.createTerse(schoolClasses.stream()));
        result.putTerse(JSON_SUBJECT, subject);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(subject.getCode());
        result.append(' ');
        result.append(schoolClasses.stream().map(sc -> sc == null ? "???" : sc.getCode()).collect(Collectors.joining(" ")));
        result.append(" (ID ");
        result.append(getId());
        result.append(")");
        return result.toString();
    }

    Course resolve(Context context) {
        schoolClasses = schoolClassIds.stream().map(id -> context.getSchoolClassById(id)).sorted().collect(Collectors.toList());
        teachers1 = teacherIds1.stream().map(id -> context.getTeacherById(id)).sorted().collect(Collectors.toList());
        teachers2 = teacherIds2.stream().map(id -> context.getTeacherById(id)).sorted().collect(Collectors.toList());
        return this;
    }

    private List<Teacher> teachersFor(SemesterEnum semester) {
        switch (semester) {
            case First:
                return teachers1;
            case Second:
                return teachers2;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected int doCompare(Entity entity) {
        int result = 0;
        if (entity instanceof Course) {
            Course other = (Course) entity;
            result = -Util.compare(getGrade(), other.getGrade());
            if (result == 0) {
                result = Util.compare(firstSchoolClass(), other.firstSchoolClass());
            }

            if (result == 0) {
                result = Util.compare(getSubject(), other.getSubject());
            }
        }

        if (result == 0) {
            result = super.doCompare(entity);
        }

        return result;
    }

    private SchoolClass firstSchoolClass() {
        return schoolClasses.isEmpty() ? null : schoolClasses.get(0);
    }
}
