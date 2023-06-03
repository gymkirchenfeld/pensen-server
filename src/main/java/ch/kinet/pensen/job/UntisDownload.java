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
import ch.kinet.csv.CsvWriter;
import ch.kinet.http.Data;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Subject;
import ch.kinet.pensen.data.Teacher;
import java.util.List;
import java.util.stream.Collectors;

public final class UntisDownload extends JobImplementation {

    private List<Course> courses;
    private PensenData pensenData;
    private List<SchoolClass> schoolClasses;
    private SchoolYear schoolYear;
    private SemesterEnum semester;

    public UntisDownload() {
        super("Untis-Export");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null;
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        semester = pensenData.getSemesterById(data.getInt("semester", -1));
        if (schoolYear == null || semester == null) {
            return false;
        }

        courses = pensenData.loadAllCourses(schoolYear).collect(Collectors.toList());
        schoolClasses = pensenData.streamSchoolClassesFor(schoolYear, null).collect(Collectors.toList());
        return true;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        CsvGenerator generator = new CsvGenerator(semester);
        callback.step();
        setProduct(generator.getData());
        callback.step();
    }

    private class CsvGenerator {

        private final CsvWriter csv = CsvWriter.create(9);
        private final SemesterEnum semester;
        private int id;

        public CsvGenerator(SemesterEnum semester) {
            this.semester = semester;
        }

        private Data getData() {
            id = 1;
            for (Course course : courses) {
                if (!course.isCancelled() && course.lessons(semester) > 0) {
                    exportCourse(course);
                }

                id = id + 1;
            }

            return Data.csv(csv.toString(), getFileName());
        }

        private String getFileName() {
            StringBuilder result = new StringBuilder();
            result.append("Untis Export ");
            result.append(schoolYear.getCode());
            result.append("-");
            result.append(semester.getId());
            result.append(".txt");
            return result.toString();
        }

        private void exportCourse(Course course) {
            if (course.isCrossClass()) {
                // export cross-class courses for all school classes with matching grade
                for (SchoolClass schoolClass : schoolClasses) {
                    if (Util.equal(schoolClass.gradeFor(schoolYear), course.getGrade())) {
                        exportCourseSchoolClass(course, schoolClass, id);
                    }
                }
            }
            else {
                // export "normal" course
                course.schoolClasses().forEachOrdered(schoolClass -> exportCourseSchoolClass(course, schoolClass, id));
            }
        }

        private void exportCourseSchoolClass(Course course, SchoolClass schoolClass, int id) {
            if (course.teachers(semester).count() > 0) {
                course.teachers(semester).forEachOrdered(teacher
                    -> writeLine(id, course.lessons(semester), schoolClass, teacher, course.getSubject(), course.getGrade())
                );
            }
            else {
                writeLine(id, course.lessons(semester), schoolClass, null, course.getSubject(), course.getGrade());
            }
        }

        private void writeLine(int id, double lessons, SchoolClass schoolClass, Teacher teacher, Subject subject, Grade grade) {
            // 1. Spalte: Fortlaufende Nummer
            csv.append(id);
            // 2. Spalte: Wochenstunden
            csv.append(lessons);
            // 3. Spalte: Wochenstunden Klasse
            csv.append();
            // 4. Spalte: Wochenstunden Lehrperson
            csv.append();
            // 5. Klasse
            csv.append(schoolClass.getCode());
            // 6. Lehrperson
            csv.append(teacher == null ? "???" : teacher.getCode());
            // 7. Fach
            csv.append(subject.getCode());
            // 8. Fachtyp
            csv.append(subject.getType().getCode());
            // 9. Stufe
            csv.append(grade.getCode());
        }
    }
}
