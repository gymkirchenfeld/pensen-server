/*
 * Copyright (C) 2022 - 2025 by Stefan Rothe
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
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.LessonTableEntry;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Subject;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.server.Authorisation;
import java.util.ArrayList;
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
        return authorisation.isEditAllowed();
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
            stepCount += 1; // employments
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
    public void run(Account creator, JobCallback callback) {
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
        callback.step();
    }

    private void initSchoolClasses(JobCallback callback) {
        callback.info("Eröffne Schuljahr {0} mit {1} vorhandenen Kursen.", schoolYear.getCode(), courses.get(schoolYear).size());
        for (SchoolClass schoolClass : schoolClasses) {
            Grade grade = schoolClass.gradeFor(schoolYear);
            callback.info("Erstelle Kurse für Klasse {0} auf Stufe {1}.", schoolClass.getCode(), grade.getCode());
            pensenData.loadLessonTableEntriesRaw(schoolClass.getCurriculum(), schoolClass.getDivision()).filter(
                entry -> Util.equal(entry.getGrade(), grade)
            ).forEachOrdered(
                entry -> checkCourse(schoolClass, grade, entry, callback)
            );
            callback.step();
        }
    }

    private void initSpecialCourses(JobCallback callback) {
        callback.info("Erstelle gesamtschulische Kurse.");
        callback.step();
        pensenData.streamCurriculums().filter(curriculum -> !curriculum.isArchived()).forEachOrdered(
            curriculum -> initSpecialCourses(curriculum, callback)
        );
    }

    private void initSpecialCourses(Curriculum curriculum, JobCallback callback) {
        pensenData.loadLessonTableEntriesRaw(curriculum, null).forEachOrdered(
            entry -> checkSpecialCourse(entry, callback)
        );
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

    private void checkCourse(SchoolClass schoolClass, Grade grade, LessonTableEntry entry, JobCallback callback) {
        Subject subject = entry.getSubject();
        if (subject.isCrossClass()) {
            return;
        }

        Course course = findCourse(schoolYear, schoolClass, subject);
        if (course != null) {
            return;
        }

        Curriculum curriculum = schoolClass.getCurriculum();
        double lessons1 = entry.getLessons1();
        double lessons2 = entry.getLessons2();
        switch (entry.typeEnum()) {
            case start:
                callback.info("Erstelle Kurs {0} {1}.", subject.getCode(), schoolClass.getCode());
                course = pensenData.createCourse("", curriculum, grade, lessons1, lessons2, schoolYear, subject);
                course.setSchoolClasses(Stream.of(schoolClass));
                pensenData.updateCourse(course, Util.createSet(Course.DB_SCHOOL_CLASS_IDS));
                courses.get(schoolYear).add(course);
                break;
            case continuation:
                Course previousCourse = findCourse(previousSchoolYear, schoolClass, subject);
                if (previousCourse != null && !previousCourse.isCancelled()) {
                    callback.info("Kopiere Kurs {0}.", previousCourse);
                    course = pensenData.copyCourse(previousCourse, lessons1, lessons2, schoolYear, grade);
                    courses.get(schoolYear).add(course);
                }
                break;
        }
    }

    private List<Course> findSpecialCourses(SchoolYear schoolYear, Curriculum curriculum, Grade grade, Subject subject) {
        List<Course> result = new ArrayList<>();
        for (Course course : specialCourses.get(schoolYear)) {
            if (Util.equal(course.getCurriculum(), curriculum) &&
                Util.equal(course.getSubject(), subject) &&
                Util.equal(course.getGrade(), grade)) {
                result.add(course);
            }
        }

        return result;
    }

    private void checkSpecialCourse(LessonTableEntry entry, JobCallback callback) {
        Curriculum curriculum = entry.getCurriculum();
        Subject subject = entry.getSubject();
        Grade grade = entry.getGrade();
        if (!subject.isCrossClass()) {
            return;
        }

        List<Course> courses = findSpecialCourses(schoolYear, curriculum, grade, subject);
        if (!courses.isEmpty()) {
            return;
        }

        double lessons1 = entry.getLessons1();
        double lessons2 = entry.getLessons2();
        switch (entry.typeEnum()) {
            case start:
                // create same amount of courses as last year
                int count = findSpecialCourses(previousSchoolYear, curriculum, grade, subject).size();
                for (int i = 0; i < count; ++i) {
                    callback.info("Erstelle Kurs {0} {1}.", subject.getCode(), grade.getCode());
                    pensenData.createCourse("", curriculum, grade, lessons1, lessons2, schoolYear, subject);
                }
                break;
            case continuation:
                findSpecialCourses(previousSchoolYear, curriculum, curriculum.previousGrade(grade), subject).
                    forEach(previousCourse -> {
                        callback.info("Kopiere Kurs {0}.", previousCourse);
                        Course course = pensenData.copyCourse(previousCourse, lessons1, lessons2, schoolYear, grade);
                        specialCourses.get(schoolYear).add(course);
                    });
                break;

        }
    }

    private void loadCourses(Map<SchoolYear, List<Course>> map, SchoolYear sy, boolean crossClass) {
        map.put(sy, pensenData.loadCourses(sy, crossClass).collect(Collectors.toList()));
    }
}
