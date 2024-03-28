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
package ch.kinet.pensen.data;

import ch.kinet.Json;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LessonTable implements Json {

    public static final String JSON_GRADE = "grade";
    public static final String JSON_LESSONS_1 = "lessons1";
    public static final String JSON_LESSONS_2 = "lessons2";
    public static final String JSON_SUBJECT = "subject";
    public static final String JSON_TYPE = "type";
    private static final String JSON_GRADES = "grades";
    private static final String JSON_ID = "id";
    private static final String JSON_ITEMS = "items";

    private final Curriculum curriculum;
    private final Division division;
    private final SortedMap<Subject, Map<Grade, LessonTable.Entry>> map;
    private final List<Subject> subjectList;

    public static LessonTable create(Curriculum curriculum, Division division, LessonType emptyType,
                                     Stream<Subject> subjects, Stream<LessonTableEntry> slots) {
        LessonTable result = new LessonTable(curriculum, division, emptyType, subjects);
        slots.forEachOrdered(result::addLessonTableEntry);
        return result;
    }

    public static Entry createEntry(Grade grade, double lessons1, double lessons2, LessonType type) {
        return new Entry(grade, lessons1, lessons2, type);
    }

    public static Entry createEntry(Grade grade, LessonType type) {
        return createEntry(grade, 0.0, 0.0, type);
    }

    public static String resourceId(Curriculum curriculum, Subject subject, Division division) {
        StringBuilder result = new StringBuilder();
        result.append(curriculum.getId());
        result.append("-");
        result.append(subject.getId());
        if (division != null) {
            result.append("-");
            result.append(division.getId());
        }

        return result.toString();
    }

    private LessonTable(Curriculum curriculum, Division division, LessonType emptyType, Stream<Subject> subjects) {
        this.curriculum = curriculum;
        this.division = division;
        map = new TreeMap<>();
        boolean crossClass = division == null;
        subjectList = subjects.filter(subject -> subject.isCrossClass() == crossClass).collect(Collectors.toList());
        subjectList.forEach(subject -> {
            SortedMap<Grade, Entry> gradeMap = new TreeMap<>();
            curriculum.grades().forEach(grade -> gradeMap.put(grade, createEntry(grade, emptyType)));
            map.put(subject, gradeMap);
        });
    }

    public Entry getEntry(Subject subject, Grade grade) {
        Map<Grade, Entry> gradeMap = map.get(subject);
        if (gradeMap == null) {
            return null;
        }

        return gradeMap.get(grade);
    }

    public Stream<Grade> grades() {
        return curriculum.grades();
    }

    public Stream<Subject> subjects() {
        return subjectList.stream();
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_GRADES, JsonArray.createTerse(curriculum.grades()));

        JsonArray items = JsonArray.create();
        subjectList.forEach(subject -> {
            JsonObject item = JsonObject.create();
            item.put(JSON_ID, resourceId(curriculum, subject, division));
            item.putTerse(JSON_SUBJECT, subject);
            curriculum.grades().forEach(grade -> {
                item.putTerse(String.valueOf(grade.getId()), getEntry(subject, grade));
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

    private void addLessonTableEntry(LessonTableEntry entry) {
        Map<Grade, Entry> gradeMap = map.get(entry.getSubject());
        if (gradeMap == null) {
            return;
        }

        Entry internalEntry = gradeMap.get(entry.getGrade());
        if (internalEntry != null) {
            internalEntry.setData(entry);
        }
    }

    public static final class Entry implements Comparable<Entry>, Json {

        private final Grade grade;
        private double lessons1;
        private double lessons2;
        private LessonType type;

        Entry(Grade grade, double lessons1, double lessons2, LessonType type) {
            this.grade = grade;
            this.lessons1 = lessons1;
            this.lessons2 = lessons2;
            this.type = type;
        }

        @Override
        public int compareTo(Entry other) {
            return Util.compare(grade, other.grade);
        }

        public Grade getGrade() {
            return grade;
        }

        public double getLessons1() {
            return lessons1;
        }

        public double getLessons2() {
            return lessons2;
        }

        public LessonType getType() {
            return type;
        }

        void setData(LessonTableEntry entry) {
            this.lessons1 = entry.getLessons1();
            this.lessons2 = entry.getLessons2();
            this.type = entry.getType();
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.putTerse(JSON_GRADE, grade);
            result.put(JSON_LESSONS_1, lessons1);
            result.put(JSON_LESSONS_2, lessons2);
            result.putTerse(JSON_TYPE, type);
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }

        public LessonType.Enum typeEnum() {
            return LessonType.toEnum(type);
        }
    }
}
