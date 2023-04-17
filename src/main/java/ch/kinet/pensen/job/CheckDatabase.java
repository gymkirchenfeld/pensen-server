/*
 * Copyright (C) 2023 by Stefan Rothe
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
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Subject;
import java.util.stream.Collectors;

public class CheckDatabase extends JobImplementation {

    private PensenData pensenData;

    public CheckDatabase() {
        super("Datenbank bereinigen");
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
        return true;
    }

    @Override
    public long getStepCount() {
        return pensenData.streamCurriculums().count() + pensenData.streamSchoolYears().count();
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        pensenData.streamCurriculums().forEachOrdered(curriculum -> checkCurriculum(curriculum, callback));
        pensenData.streamSchoolYears().forEachOrdered(schoolYear -> checkSchoolYear(schoolYear, callback));
    }

    private void checkCurriculum(Curriculum curriculum, JobCallback callback) {
        callback.info("Überprüfe Lehrgang {0}", curriculum);
        callback.step();
        pensenData.loadDefaultLessons(curriculum, null).forEachOrdered(
            defaultLessons -> checkDefaultLessons(defaultLessons, true, callback)
        );
        pensenData.streamDivisions().forEachOrdered(division -> {
            pensenData.loadDefaultLessons(curriculum, division).forEachOrdered(
                defaultLessons -> checkDefaultLessons(defaultLessons, false, callback)
            );
        });
    }

    private void checkDefaultLessons(DefaultLessons defaultLessons, boolean crossClass, JobCallback callback) {
        Subject subject = defaultLessons.getSubject();
        if (subject.isCrossClass() && !crossClass) {
            callback.info("Der Datenbankeintrag zum Fach {0} muss einer Organisationseinheit zugeorndet sein.",
                          subject);
        }

        if (!subject.isCrossClass() && crossClass) {
            callback.info("Der Datenbankeintrag zum Fach {0} darf nicht Organisationseinheit zugeorndet sein.",
                          subject);
        }
    }

    private void checkSchoolYear(SchoolYear schoolYear, JobCallback callback) {
        callback.info("Überprüfe Schuljahr {0}", schoolYear);
        callback.step();
        pensenData.loadAllCourses(schoolYear).forEachOrdered(course -> checkCourse(course, callback));
    }

    private void checkCourse(Course course, JobCallback callback) {
        // check curriculum and grade
        Curriculum courseCurriculum = course.getCurriculum();
        Grade courseGrade = course.getGrade();
        boolean updateCurriculum = courseCurriculum == null;

        if (course.isCrossClass()) {
            if (!course.getSubject().isCrossClass()) {
                callback.info("Der Kurs {0} ist als gesamtschulisch markiert, das Fach {1} ist jedoch nicht gesamtschulisch.",
                              course, course.getSubject());
            }

            if (courseCurriculum == null) {
                courseCurriculum = curriculumFor(courseGrade);
            }
        }
        else {
            if (course.getSubject().isCrossClass()) {
                callback.info("Der Kurs {0} ist nicht als gesamtschulisch markiert, das Fach {1} ist jedoch gesamtschulisch.",
                              course, course.getSubject());
            }

            for (SchoolClass schoolClass : course.schoolClasses().collect(Collectors.toList())) {
                if (schoolClass == null) {
                    callback.info("Der Kurs {0} enthält eine Klassen-ID, welche keiner Klasse zugeordnet ist.", course);
                    continue;
                }

                Curriculum schoolClassCurriculum = schoolClass.getCurriculum();
                if (courseCurriculum == null) {
                    courseCurriculum = schoolClassCurriculum;
                }
                else if (!Util.equal(courseCurriculum, schoolClassCurriculum)) {
                    callback.info("Der Kurs {0} enthält Klassen mit unterschiedlichen Lehrgängen.", course);
                    updateCurriculum = false;
                }

                Grade schoolClassGrade = schoolClass.gradeFor(course.getSchoolYear());
                if (courseGrade == null) {
                    courseGrade = schoolClassGrade;
                }
                else if (!Util.equal(courseGrade, schoolClassGrade)) {
                    callback.info("Der Kurs {0} enthält Klassen mit unterschiedlichen Schulstufen.", course);
                }
            }
        }

        if (courseCurriculum != null) {
            if (!courseCurriculum.containsGrade(courseGrade)) {
                callback.info("Beim Kurs {0} stimmen Lehrgang {1} und Schulstufe {2} nicht überein.",
                              course, courseCurriculum, courseGrade);
            }

            if (updateCurriculum) {
                callback.info("Aktualisiere den Lehrgang von Kurs {0}", course);
                course.setCurriculum(courseCurriculum);
                pensenData.updateCourse(course, Util.createSet(Course.DB_CURRICULUM));
            }
        }
    }

    private Curriculum curriculumFor(Grade grade) {
        for (Curriculum curriculum : pensenData.streamCurriculums().collect(Collectors.toList())) {
            if (curriculum.containsGrade(grade)) {
                return curriculum;
            }
        }

        return null;
    }
}
