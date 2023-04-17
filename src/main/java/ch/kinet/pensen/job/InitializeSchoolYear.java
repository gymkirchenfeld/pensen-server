/*
 * Copyright (C) 2022 - 2023 by Stefan Rothe
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
package ch.kinet.pensen.job;

import ch.kinet.DataManager;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.DefaultLessons;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Subject;
import ch.kinet.pensen.data.Teacher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InitializeSchoolYear extends JobImplementation {

    private Map<SchoolYear, List<Course>> courses;
    private Map<SchoolYear, List<Course>> specialCourses;
    private PensenData pensenData;
    private SchoolYear previousSchoolYear;
    private List<SchoolClass> schoolClasses;
    private SchoolYear schoolYear;
    private long stepCount;

    public InitializeSchoolYear() {
        super("Schuljahr eröffnen");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        if (schoolYear != null) {
            schoolClasses = pensenData.streamSchoolClassesFor(schoolYear, null).collect(Collectors.toList());
            previousSchoolYear = schoolYear.previous();
            courses = new HashMap<>();
            specialCourses = new HashMap<>();
            loadCourses(courses, schoolYear, false);
            loadCourses(courses, previousSchoolYear, false);
            loadCourses(specialCourses, schoolYear, true);
            loadCourses(specialCourses, previousSchoolYear, true);
            stepCount = pensenData.loadEmployments(previousSchoolYear, null).count();
            stepCount += schoolClasses.size();
            stepCount += 3;
        }

        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return stepCount;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        copyEmployments(callback);
        initSchoolClasses(callback);
        initSpecialCourses(callback);
        copyPool(callback);
        callback.info("Berechne IPB-Endsaldi.");
        pensenData.recalculateBalance(schoolYear);
        callback.step();
        callback.info("Eröffnung ist beendet worden.");
    }

    private void copyEmployments(JobCallback callback) {
        callback.info("Kopiere Anstellungen.");
        Map<Teacher, Employment> existing = new HashMap<>();
        pensenData.loadEmployments(schoolYear, null).forEachOrdered(employment -> {
            existing.put(employment.getTeacher(), employment);
        });

        pensenData.loadEmployments(previousSchoolYear, null).forEachOrdered(employment -> {
            callback.step();
            if (!existing.containsKey(employment.getTeacher())) {
                pensenData.createEmployment(
                    schoolYear,
                    employment.getTeacher(),
                    employment.getDivision(),
                    employment.getEmploymentMax(),
                    employment.getEmploymentMin(),
                    employment.getPayment1(),
                    employment.getPayment2(),
                    employment.isTemporary(),
                    null);
            }
        });
    }

    private void initSchoolClasses(JobCallback callback) {
        callback.info("Eröffne Schuljahr {0} mit {1} vorhandenen Kursen.", schoolYear.getCode(), courses.get(schoolYear).size());
        for (SchoolClass schoolClass : schoolClasses) {
            Grade grade = schoolClass.gradeFor(schoolYear);
            callback.info("Erstelle Kurse für Klasse {0} auf Stufe {1}.", schoolClass.getCode(), grade.getCode());
            pensenData.loadDefaultLessons(schoolClass.getCurriculum(), schoolClass.getDivision()).forEachOrdered(
                entry -> checkCourse(schoolClass, grade, entry, callback)
            );
            callback.step();
        }
    }

    private void initSpecialCourses(JobCallback callback) {
        callback.info("Erstelle gesamtschulische Kurse.");
        callback.step();
        specialCourses.get(previousSchoolYear).forEach(course -> checkSpecialCourse(course, callback));
    }

    private void copyPool(JobCallback callback) {
        callback.step();
        if (pensenData.loadPoolEntries(schoolYear).count() > 0) {
            return;
        }

        callback.info("Kopiere Pooleinträge.");
        pensenData.loadPoolEntries(previousSchoolYear).filter(
            entry -> entry.getType().isAutoCopy()
        ).forEachOrdered(entry -> {
            pensenData.createPoolEntry(
                entry.getDescription(),
                entry.getPercent1(),
                entry.getPercent2(),
                schoolYear,
                entry.getTeacher(),
                entry.getType());
        });
    }

    private Course findCourse(SchoolYear sy, SchoolClass schoolClass, Subject subject) {
        for (Course course : courses.get(sy)) {
            if (Util.equal(course.getSubject(), subject) && course.contains(schoolClass)) {
                return course;
            }
        }

        return null;
    }

    private void checkCourse(SchoolClass schoolClass, Grade grade, DefaultLessons entry, JobCallback callback) {
        double lessons1 = entry.lessonsFor(SemesterEnum.First, grade);
        double lessons2 = entry.lessonsFor(SemesterEnum.Second, grade);
        Subject subject = entry.getSubject();
        Curriculum curriculum = schoolClass.getCurriculum();
        if (subject.isCrossClass() || (lessons1 < 0 && lessons2 < 0)) {
            return;
        }

        Course course = findCourse(schoolYear, schoolClass, subject);
        if (course == null) {
            Course previousCourse = findCourse(previousSchoolYear, schoolClass, subject);
            if (previousCourse == null) {
                callback.info("Erstelle Kurs {0} {1}.", subject.getCode(), schoolClass.getCode());
                course = pensenData.createCourse("", curriculum, grade, lessons1, lessons2, schoolYear, subject);
                course.setSchoolClasses(Stream.of(schoolClass));
                pensenData.updateCourse(course, Util.createSet(Course.DB_SCHOOL_CLASS_IDS));
            }
            else {
                callback.info("Kopiere Kurs {0}.", previousCourse);
                course = pensenData.copyCourse(previousCourse, lessons1, lessons2, schoolYear, grade);
            }

            courses.get(schoolYear).add(course);
        }
    }

    private Course findSpecialCourse(Grade grade, Subject subject, Stream<Teacher> teachers) {
        List<Teacher> teacherList = teachers.collect(Collectors.toList());
        for (Course course : specialCourses.get(schoolYear)) {
            if (Util.equal(course.getSubject(), subject) && Util.equal(course.getGrade(), grade) &&
                course.containsAny(teacherList)) {
                return course;
            }
        }

        return null;
    }

    private void checkSpecialCourse(Course previousCourse, JobCallback callback) {
        Curriculum curriculum = previousCourse.getCurriculum();
        Grade grade = curriculum.nextGrade(previousCourse.getGrade());
        if (grade == null) {
            return;
        }

        Subject subject = previousCourse.getSubject();
        Course course = findSpecialCourse(grade, subject, previousCourse.teachers());
        if (course != null) {
            return;
        }

        DefaultLessons defaultLessons = pensenData.loadDefaultLessons(curriculum, null, subject);
        double lessons1 = defaultLessons.lessonsFor(SemesterEnum.First, grade);
        double lessons2 = defaultLessons.lessonsFor(SemesterEnum.Second, grade);
        if (lessons1 < 0 && lessons2 < 0) {
            return;
        }

        callback.info("Erstelle Kurs {0} {1}.", subject.getCode(), grade.getCode());
        course = pensenData.createCourse("", curriculum, grade, lessons1, lessons2, schoolYear, subject);
        course.setTeachers1(previousCourse.teachers(SemesterEnum.First));
        course.setTeachers2(previousCourse.teachers(SemesterEnum.Second));
        pensenData.updateCourse(course, Util.createSet(Course.DB_TEACHER_IDS_1, Course.DB_TEACHER_IDS_2));
    }

    private void loadCourses(Map<SchoolYear, List<Course>> map, SchoolYear sy, boolean crossClass) {
        map.put(sy, pensenData.loadCourses(sy, crossClass).collect(Collectors.toList()));
    }
}
