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

import ch.kinet.Json;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CourseTable implements Json {

    private static final String JSON_COURSES = "courses";
    private static final String JSON_ID = "id";
    private static final String JSON_ITEMS = "items";
    private static final String JSON_SCHOOL_CLASSES = "schoolClasses";
    private static final String JSON_SUBJECT = "subject";
    private final List<Course> courseList = new ArrayList<>();
    private SortedMap<Subject, Map<SchoolClass, Entry>> map;
    private final List<SchoolClass> schoolClassList;
    private final List<Subject> subjectList;

    public static CourseTable create(Stream<SchoolClass> schoolClasses, Stream<Subject> subjects,
                                     Stream<Course> courses) {
        CourseTable result = new CourseTable(subjects, schoolClasses);
        courses.forEachOrdered(result::addCourse);
        return result;
    }

    private CourseTable(Stream<Subject> subjects, Stream<SchoolClass> schoolClasses) {
        map = new TreeMap<>();
        schoolClassList = schoolClasses.collect(Collectors.toList());
        subjectList = subjects.collect(Collectors.toList());
        subjectList.forEach(subject -> {
            SortedMap<SchoolClass, Entry> schoolClassMap = new TreeMap<>();
            schoolClassList.forEach(schoolClass -> schoolClassMap.put(schoolClass, new Entry(schoolClass)));
            map.put(subject, schoolClassMap);
        });
    }

    public Entry getEntry(Subject subject, SchoolClass schoolClass) {
        Map<SchoolClass, Entry> schoolClassMap = map.get(subject);
        if (schoolClassMap == null) {
            return null;
        }

        return schoolClassMap.get(schoolClass);
    }

    public Stream<SchoolClass> schoolClasses() {
        return schoolClassList.stream();
    }

    public Stream<Subject> subjects() {
        return subjectList.stream();
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_SCHOOL_CLASSES, JsonArray.createTerse(schoolClassList.stream()));

        JsonObject courses = JsonObject.create();
        courseList.forEach(course -> courses.putTerse(String.valueOf(course.getId()), course));
        result.put(JSON_COURSES, courses);

        JsonArray items = JsonArray.create();
        subjectList.forEach(subject -> {
            JsonObject item = JsonObject.create();
            item.putTerse(JSON_SUBJECT, subject);
            schoolClassList.forEach(schoolClass -> {
                Entry entry = getEntry(subject, schoolClass);
                if (entry != null && !entry.isEmpty()) {
                    item.putTerse(schoolClass.getCode(), entry);
                }
            });

            items.add(item);
        });

        result.put(JSON_ITEMS, items);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        return toJsonTerse();
    }

    private void addCourse(Course course) {
        Map<SchoolClass, Entry> schoolClassMap = map.get(course.getSubject());
        if (schoolClassMap == null) {
            return;
        }

        courseList.add(course);
        course.schoolClasses().forEachOrdered(schoolClass -> {
            if (schoolClass != null) {
                Entry entry = schoolClassMap.get(schoolClass);
                if (entry != null) {
                    entry.add(course);
                }
            }
        });
    }

    public static class Entry implements Json {

        private final List<Course> courses = new ArrayList<>();
        private final SchoolClass schoolClass;

        Entry(SchoolClass schoolClass) {
            this.schoolClass = schoolClass;
        }

        public boolean isEmpty() {
            return courses.isEmpty();
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_ID, courses.get(0).getId());
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }

        @Override
        public String toString() {
            if (courses.isEmpty()) {
                return "-";
            }

            if (courses.size() > 1) {
                return "(Fehler)";
            }

            Course course = courses.get(0);
            if (course.isCancelled()) {
                return "-";
            }

            String display1 = course.displayText(schoolClass, SemesterEnum.First);
            String display2 = course.displayText(schoolClass, SemesterEnum.Second);
            if (Util.equal(display1, display2)) {
                return display1;
            }
            else {
                return display1 + " / " + display2;
            }
        }

        private void add(Course course) {
            courses.add(course);
        }
    }
}
