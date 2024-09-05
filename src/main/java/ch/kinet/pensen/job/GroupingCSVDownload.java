/*
 * Copyright (C) 2024 by Stefan Rothe
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
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class GroupingCSVDownload extends JobImplementation {

    private static final Set<String> SUBJECTS = Util.createSet(
        "B",
        "B PR",
        "BG",
        "C",
        "C PR",
        "D",
        "E",
        "EWR",
        "F",
        "G",
        "Ge",
        "GG",
        "M",
        "Me",
        "MU",
        "P",
        "SF PH",
        "SF PP",
        "SF WR"
    );
    private final Map<SchoolClass, Set<Course>> schoolClassMap = new HashMap<>();
    private final Map<Teacher, Set<Course>> teacherMap = new HashMap<>();
    private final List<List<Course>> results = new ArrayList<>();
    private PensenData pensenData;
    private SchoolYear schoolYear;
    private int courseCount = 4;

    public GroupingCSVDownload() {
        super("Gruppierung");
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
        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        createMaps();
        buildGroups();
        exportGroups();
    }

    private void createMaps() {
        pensenData.loadAllCourses(schoolYear)
            .filter(this::filterCourse)
            .forEachOrdered(course -> {
                course.schoolClasses().forEachOrdered(schoolClass -> {
                    if (!schoolClassMap.containsKey(schoolClass)) {
                        schoolClassMap.put(schoolClass, new HashSet<>());
                    }

                    schoolClassMap.get(schoolClass).add(course);
                });
                course.teachers().forEachOrdered(teacher -> {
                    if (!teacherMap.containsKey(teacher)) {
                        teacherMap.put(teacher, new HashSet<>());
                    }

                    teacherMap.get(teacher).add(course);
                });
            });

    }

    private void buildGroups() {
        schoolClassMap.keySet().forEach(sc1 -> {
            schoolClassMap.get(sc1).forEach(course1 -> {
                Teacher t1 = course1.teachers().findFirst().get();
                teacherMap.get(t1).forEach(course2 -> {
                    SchoolClass sc2 = course2.schoolClasses().findFirst().get();
                    if (sc1.getId() != sc2.getId()) {
                        schoolClassMap.get(sc2).forEach(course3 -> {
                            Teacher t2 = course3.teachers().findFirst().get();
                            if (t2.getId() != t1.getId()) {
                                teacherMap.get(t2).forEach(course4 -> {
                                    SchoolClass sc4 = course4.schoolClasses().findFirst().get();
                                    if (sc1.getId() == sc4.getId()) {
                                        tryGrouping(course1, course2, course3, course4);
                                    }
                                });
                            }
                        });
                    }
                });
            });
        });
    }

    private void tryGrouping(Course... courses) {
        List<Course> entry = Arrays.asList(courses);
        Set<Course> s = new HashSet<>(entry);
        if (s.size() == courseCount) {
            results.add(entry);
        }
    }

    private void exportGroups() {
        CsvWriter csv = CsvWriter.create(createHeaders());
        results.forEach(result -> {
            result.forEach(course -> {
                csv.append(course.getSubject().getCode());
                csv.append(course.schoolClasses().findFirst().get().getCode());
                csv.append(course.teachers().findFirst().get().getCode());
            });
        });

        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        Stream.Builder<String> builder = Stream.builder();
        for (int i = 1; i <= courseCount; ++i) {
            builder.add("Fach " + String.valueOf(i));
            builder.add("Klasse " + String.valueOf(i));
            builder.add("Lehrperson " + String.valueOf(i));
        }
        return builder.build();

    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Planung Liebefeld ");
        result.append(schoolYear.getCode());
        result.append(".csv");
        return result.toString();
    }

    private boolean filterCourse(Course course) {
        return course.schoolClasses().count() == 1 &&
            course.teachers().count() == 1 &&
            SUBJECTS.contains(course.getSubject().getCode());
    }
}
