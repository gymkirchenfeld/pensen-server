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
package ch.kinet.pensen.server;

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.SetComparison;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.LessonTableEntry;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Subject;
import ch.kinet.pensen.data.Teacher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CourseResource extends EntityResource<Course> {

    private static final String QUERY_CROSS_CLASS = "crossClass";
    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        if (!query.hasKey(QUERY_SCHOOL_YEAR)) {
            return Response.badRequest();
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.notFound();
        }

        if (query.hasKey(QUERY_CROSS_CLASS)) {
            boolean crossClass = query.getBoolean(QUERY_CROSS_CLASS, true);
            return Response.jsonArrayTerse(pensenData.loadCourses(schoolYear, crossClass));
        }
        else {
            return Response.jsonArrayTerse(pensenData.loadAllCourses(schoolYear));
        }
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(object);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        if (data.hasKey("merge")) {
            return merge(data.getArray("merge"));
        }

        if (data.hasKey("split")) {
            return split(data.getInt("split", -1));
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(data.getObjectId(Course.JSON_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.badRequest();
        }

        if (schoolYear.isArchived()) {
            return Response.badRequest("Zu archivierten Schuljahren können keine Kurse hinzugefügt werden.");
        }

        Subject subject = pensenData.getSubjectById(data.getObjectId(Course.JSON_SUBJECT, -1));
        if (subject == null) {
            return Response.badRequest();
        }

        Curriculum curriculum = null;
        Grade grade = null;
        Division division = null;
        Set<SchoolClass> schoolClasses = new HashSet<>();
        if (subject.isCrossClass()) {
            curriculum = pensenData.getCurriculumById(data.getObjectId(Course.JSON_CURRICULUM, -1));
            grade = pensenData.getGradeById(data.getObjectId(Course.JSON_GRADE, -1));
        }
        else {
            schoolClasses.addAll(pensenData.parseSchoolClasses(data.getArray(Course.JSON_SCHOOL_CLASSES)));
            for (SchoolClass schoolClass : schoolClasses) {
                Curriculum schoolClassCurriculum = schoolClass.getCurriculum();
                if (division == null) {
                    division = schoolClass.getDivision();
                }

                if (curriculum == null) {
                    curriculum = schoolClassCurriculum;
                }
                else if (!Util.equal(curriculum, schoolClassCurriculum)) {
                    return Response.badRequest("Alle Klassen eines Kurses müssen dem gleichen Lehrgang zugeordnet sein.");
                }

                Grade schoolClassGrade = schoolClass.gradeFor(schoolYear);
                if (grade == null) {
                    grade = schoolClassGrade;
                }
                else if (!Util.equal(grade, schoolClassGrade)) {
                    return Response.badRequest("Alle Klassen eines Kurses müssen der gleichen Schulstufe zugeorndet sein.");
                }
            }
        }

        if (curriculum == null) {
            return Response.badRequest("Ein Kurs muss einen Lehrgang haben.");
        }

        if (grade == null) {
            return Response.badRequest("Ein Kurs muss eine Schulstufe haben.");
        }

        double lessons1 = data.getDouble(Course.JSON_LESSONS_1, 0.0);
        double lessons2 = data.getDouble(Course.JSON_LESSONS_2, 0.0);
        if (lessons1 < 0 || lessons2 < 0) {
            LessonTableEntry entry = pensenData.loadLessonTableEntry(curriculum, division, subject, grade);
            if (entry != null) {
                lessons1 = entry.getLessons1();
                lessons2 = entry.getLessons2();
            }
        }

        String comments = data.getString(Course.JSON_COMMENTS);
        Set<Teacher> teachers1 = pensenData.parseTeachers(data.getArray(Course.JSON_TEACHERS_1));
        Set<Teacher> teachers2 = pensenData.parseTeachers(data.getArray(Course.JSON_TEACHERS_2));

        Course result = pensenData.createCourse(comments, curriculum, grade, lessons1, lessons2, schoolYear, subject);
        result.setSchoolClasses(schoolClasses.stream());
        result.setTeachers1(teachers1.stream());
        result.setTeachers2(teachers2.stream());
        pensenData.updateCourse(result, Util.createSet(
                                Course.DB_SCHOOL_CLASS_IDS, Course.DB_TEACHER_IDS_1, Course.DB_TEACHER_IDS_2));
        teachers1.addAll(teachers2);
        teachers1.stream().forEachOrdered(teacher -> pensenData.recalculateBalance(schoolYear, teacher));
        return Response.createdJsonVerbose(result);
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data
    ) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data
    ) {
        if (object.getSchoolYear().isArchived()) {
            return Response.badRequest("Kurse in archivierten Schuljahren können nicht verändert werden.");
        }

        boolean cancelled = data.getBoolean(Course.JSON_CANCELLED, false);
        String comments = data.getString(Course.JSON_COMMENTS);
        double lessons1 = data.getDouble(Course.JSON_LESSONS_1);
        double lessons2 = data.getDouble(Course.JSON_LESSONS_2);
        if (lessons1 < 0 || lessons2 < 0) {
            return Response.badRequest("Negative Lektionenzahlen sind nicht erlaubt.");
        }

        Set<Teacher> teachers1 = pensenData.parseTeachers(data.getArray(Course.JSON_TEACHERS_1));
        Set<Teacher> teachers2 = pensenData.parseTeachers(data.getArray(Course.JSON_TEACHERS_2));

        Set<String> changed = new HashSet<>();
        Set<Teacher> affectedTeachers = new HashSet<>(object.teachers().collect(Collectors.toSet()));

        if (!Util.equal(object.isCancelled(), cancelled)) {
            object.setCancelled(cancelled);
            changed.add(Course.DB_CANCELLED);
        }

        if (!Util.equal(object.getComments(), comments)) {
            object.setComments(comments);
            changed.add(Course.DB_COMMENTS);
        }

        if (!Util.equal(object.getLessons1(), lessons1)) {
            object.setLessons1(lessons1);
            changed.add(Course.DB_LESSONS_1);
            object.teachers(SemesterEnum.First).forEachOrdered(teacher -> affectedTeachers.add(teacher));
        }

        if (!Util.equal(object.getLessons2(), lessons2)) {
            object.setLessons2(lessons2);
            changed.add(Course.DB_LESSONS_2);
            object.teachers(SemesterEnum.Second).forEachOrdered(teacher -> affectedTeachers.add(teacher));
        }

        SetComparison<Teacher> changes1 = SetComparison.create(Util.createSet(object.teachers(SemesterEnum.First)), teachers1);
        if (changes1.hasChanges()) {
            object.setTeachers1(teachers1.stream());
            affectedTeachers.addAll(teachers1);
            changed.add(Course.DB_TEACHER_IDS_1);
        }

        SetComparison<Teacher> changes2 = SetComparison.create(Util.createSet(object.teachers(SemesterEnum.Second)), teachers2);
        if (changes2.hasChanges()) {
            object.setTeachers2(teachers2.stream());
            affectedTeachers.addAll(teachers2);
            changed.add(Course.DB_TEACHER_IDS_2);
        }

        pensenData.updateCourse(object, changed);
        affectedTeachers.stream().forEachOrdered(teacher -> pensenData.recalculateBalance(object.getSchoolYear(), teacher));
        return Response.noContent();
    }

    @Override
    protected boolean isDeleteAllowed(Authorisation authorisation
    ) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response delete(Authorisation authorisation
    ) {
        if (object.getSchoolYear().isArchived()) {
            return Response.forbidden();
        }

        pensenData.deleteCourse(object);
        return Response.noContent();
    }

    @Override
    protected Course loadObject(int id
    ) {
        return pensenData.loadCourse(id);
    }

    private Response merge(JsonArray ids) {
        // TODO: Check grade and curriculum
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < ids.length(); ++i) {
            Course course = pensenData.loadCourse(ids.getInt(i, -1));
            if (course == null) {
                return Response.notFound();
            }

            courses.add(course);
        }

        if (courses.isEmpty()) {
            return Response.badRequest();
        }

        Set<SchoolClass> schoolClasses = new HashSet<>();
        Course firstCourse = courses.get(0);
        if (firstCourse.isCrossClass()) {
            return Response.badRequest();
        }

        for (Course course : courses) {
            if (!course.isMergableWith(firstCourse)) {
                return Response.badRequest();
            }
            else {
                course.schoolClasses().forEachOrdered(schoolClass -> schoolClasses.add(schoolClass));
            }
        }

        courses.remove(firstCourse);
        for (Course course : courses) {
            pensenData.deleteCourse(course);
        }

        firstCourse.setSchoolClasses(schoolClasses.stream());
        pensenData.updateCourse(firstCourse, Util.createSet(Course.DB_SCHOOL_CLASS_IDS));
        return Response.noContent();
    }

    private Response split(int id) {
        Course course = pensenData.loadCourse(id);
        if (course == null) {
            return Response.notFound();
        }

        if (course.isCrossClass()) {
            return Response.badRequest("Gesamtschulische Kurse können nicht aufgeteilt werden.");
        }

        List<SchoolClass> schoolClasses = course.schoolClasses().collect(Collectors.toList());
        if (schoolClasses.size() < 2) {
            return Response.badRequest("Ein Kurs mit einer Klasse kann nicht aufgeteilt werden.");
        }

        SchoolClass first = schoolClasses.get(0);
        schoolClasses.remove(first);
        course.setSchoolClasses(Stream.of(first));
        pensenData.updateCourse(course, Util.createSet(Course.DB_SCHOOL_CLASS_IDS));
        for (SchoolClass schoolClass : schoolClasses) {
            cloneCourse(course, schoolClass);
        }

        return Response.noContent();
    }

    private void cloneCourse(Course original, SchoolClass schoolClass) {
        Course course = pensenData.createCourse(
            original.getComments(),
            original.getCurriculum(),
            original.getGrade(),
            original.getLessons1(),
            original.getLessons2(),
            original.getSchoolYear(),
            original.getSubject());
        course.setTeachers1(original.teachers(SemesterEnum.First));
        course.setTeachers2(original.teachers(SemesterEnum.Second));
        course.setSchoolClasses(Stream.of(schoolClass));
        pensenData.updateCourse(course, Util.createSet(Course.DB_SCHOOL_CLASS_IDS, Course.DB_TEACHER_IDS_1, Course.DB_TEACHER_IDS_2));
    }
}
