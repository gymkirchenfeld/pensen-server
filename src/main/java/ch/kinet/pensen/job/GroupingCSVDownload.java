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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
    private final SortedMap<SchoolClass, SortedSet<Teacher>> teacherMap = new TreeMap<>();
    private final Map<SchoolClass, Map<Teacher, Course>> courseMap = new HashMap<>();
    private final List<Group> groups = new ArrayList<>();
    private PensenData pensenData;
    private SchoolYear schoolYear;
    private int mode = 0;
    private int minTeacherCount = 4;

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
        mode = data.getInt("mode", -1);
        switch (mode) {
            case 1:
                minTeacherCount = 2;
                break;
            case 2:
                minTeacherCount = 4;
                break;
        }
        return schoolYear != null && mode != -1;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        init();
        createGroups();
        Collections.sort(groups);
        exportCourses();
        //   exportGroups();
    }

    private void init() {
        pensenData.loadAllCourses(schoolYear)
            .filter(this::filterCourse)
            .forEachOrdered(this::processCourse);
    }

    private void processCourse(Course course) {
        SchoolClass schoolClass = course.schoolClasses().findFirst().get();
        Teacher teacher = course.teachers().findFirst().get();
        if (!teacherMap.containsKey(schoolClass)) {
            teacherMap.put(schoolClass, new TreeSet<>());
        }

        teacherMap.get(schoolClass).add(teacher);
        if (!courseMap.containsKey(schoolClass)) {
            courseMap.put(schoolClass, new HashMap<>());
        }

        courseMap.get(schoolClass).put(teacher, course);
    }

    private void createGroups() {
        List<SchoolClass> schoolClasses = pensenData.streamSchoolClassesFor(schoolYear, null)
            .sorted()
            .collect(Collectors.toList());
        for (int i = 0; i < schoolClasses.size(); ++i) {
            SchoolClass schoolClassA = schoolClasses.get(i);
            for (int j = i + 1; j < schoolClasses.size(); ++j) {
                SchoolClass schoolClassB = schoolClasses.get(j);
                checkGroup(schoolClassA, schoolClassB);
            }
        }
    }

    private void checkGroup(SchoolClass schoolClassA, SchoolClass schoolClassB) {
        if (!(teacherMap.containsKey(schoolClassA) && teacherMap.containsKey(schoolClassB))) {
            return;
        }

        SortedSet<Teacher> intersection = new TreeSet<>(teacherMap.get(schoolClassA));
        intersection.retainAll(teacherMap.get(schoolClassB));
        if (intersection.size() > 1) {
            groups.add(new Group(intersection, schoolClassA, schoolClassB));
        }
    }

    private void exportCourses() {
        CsvWriter csv = CsvWriter.create(createCourseHeaders());
        groups.stream().filter(group -> group.getTeacherCount() >= minTeacherCount).forEachOrdered(
            group -> exportCoursesForGroup(csv, group)
        );
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createCourseHeaders() {
        Stream.Builder<String> builder = Stream.builder();
        for (int i = 1; i <= 2 * minTeacherCount; ++i) {
            builder.add("Fach " + String.valueOf(i));
            builder.add("Klasse " + String.valueOf(i));
            builder.add("Lehrperson " + String.valueOf(i));
        }
        return builder.build();
    }

    private void exportCoursesForGroup(CsvWriter csv, Group group) {
        group.streamTeacherSets(minTeacherCount).forEachOrdered(set -> exportLine(csv, group, set));
    }

    private void exportLine(CsvWriter csv, Group group, Set<Teacher> teachers) {
        teachers.forEach(teacher -> {
            exportCourse(csv, group.schoolClassA, teacher);
            exportCourse(csv, group.schoolClassB, teacher);
        });
    }

    private void exportCourse(CsvWriter csv, SchoolClass schoolClass, Teacher teacher) {
        Course course = courseMap.get(schoolClass).get(teacher);
        csv.append(course.getSubject().getCode());
        csv.append(schoolClass.getCode());
        csv.append(teacher.getCode());
    }

    private void exportGroups() {
        CsvWriter csv = CsvWriter.create(createGroupHeaders());
        groups.forEach(result -> result.write(csv));
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createGroupHeaders() {
        Stream.Builder<String> builder = Stream.builder();
        builder.add("Klasse 1");
        builder.add("Klasse 2");
        builder.add("Lehrperson 1");
        builder.add("Lehrperson 2");
        builder.add("Lehrperson 3");
        builder.add("Lehrperson 4");
        builder.add("Lehrperson 5");
        builder.add("Lehrperson 6");
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

    private static final class Group implements Comparable<Group> {

        private final SchoolClass schoolClassA;
        private final SchoolClass schoolClassB;
        private final SortedSet<Teacher> teachers;

        private Group(SortedSet<Teacher> teachers, SchoolClass schoolClassA, SchoolClass schoolClassB) {
            this.teachers = teachers;
            this.schoolClassA = schoolClassA;
            this.schoolClassB = schoolClassB;
        }

        private int getTeacherCount() {
            return teachers.size();
        }

        private Stream<SortedSet<Teacher>> streamTeacherSets(int size) {
            return getSubsets(teachers, size);
        }

        private void write(CsvWriter csv) {
            csv.append(schoolClassA.getCode());
            csv.append(schoolClassB.getCode());

            for (Teacher teacher : teachers) {
                csv.append(teacher.getCode());
            }

            int fill = 6 - teachers.size();
            for (int i = 0; i < fill; ++i) {
                csv.append();
            }
        }

        @Override
        public int compareTo(Group other) {
            int result = other.teachers.size() - teachers.size();
            if (result == 0) {
                result = schoolClassA.compareTo(other.schoolClassA);
            }

            if (result == 0) {
                result = schoolClassB.compareTo(other.schoolClassB);
            }

            return result;
        }
    }

    private static Stream<SortedSet<Teacher>> getSubsets(SortedSet<Teacher> set, int size) {
        int currentSize = set.size();
        Stream<SortedSet<Teacher>> result = Stream.of(set);
        while (currentSize > size) {
            result = result.flatMap(s -> getSubsets(s));
            currentSize -= 1;
        }

        return result;
    }

    private static Stream<SortedSet<Teacher>> getSubsets(SortedSet<Teacher> set) {
        return set.stream().map(item -> {
            SortedSet<Teacher> clone = new TreeSet<>(set);
            clone.remove(item);
            return clone;
        });
    }
}
