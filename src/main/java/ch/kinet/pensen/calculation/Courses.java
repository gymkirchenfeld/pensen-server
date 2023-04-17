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
package ch.kinet.pensen.calculation;

import ch.kinet.Json;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.Subject;
import java.util.stream.Stream;

public final class Courses extends ItemList<Courses.Item> {

    static Courses create() {
        return new Courses();
    }

    private static final String JSON_GRADE = "grade";
    private static final String JSON_LESSONS1 = "lessons1";
    private static final String JSON_LESSONS2 = "lessons2";
    private static final String JSON_PERCENT1 = "percent1";
    private static final String JSON_PERCENT2 = "percent2";
    private static final String JSON_SCHOOL_CLASSES = "schoolClasses";
    private static final String JSON_SUBJECT = "subject";
    private static final String JSON_TOTAL = "total";

    private double lessons1;
    private double percent1;
    private double lessons2;
    private double percent2;

    private Courses() {
    }

    public double lessons1() {
        return lessons1;
    }

    public double lessons2() {
        return lessons2;
    }

    public double percent1() {
        return percent1;
    }

    public double percent2() {
        return percent2;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject total = JsonObject.create();
        total.put(JSON_LESSONS1, lessons1);
        total.put(JSON_LESSONS2, lessons2);
        total.put(JSON_PERCENT1, percent1);
        total.put(JSON_PERCENT2, percent2);

        JsonObject result = super.toJsonTerse();
        result.put(JSON_TOTAL, total);
        return result;
    }

    void addItem(Course course, double lessons1, double percent1, double lessons2, double percent2) {
        add(new Item(course, lessons1, percent1, lessons2, percent2));
        this.lessons1 += lessons1;
        this.lessons2 += lessons2;
        this.percent1 += percent1;
        this.percent2 += percent2;
    }

    public static final class Item implements Json {

        private final Course course;
        private final double lessons1;
        private final double percent1;
        private final double lessons2;
        private final double percent2;

        private Item(Course course, double lessons1, double percent1, double lessons2, double percent2) {
            this.course = course;
            this.lessons1 = lessons1;
            this.percent1 = percent1;
            this.lessons2 = lessons2;
            this.percent2 = percent2;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_LESSONS1, lessons1);
            result.put(JSON_LESSONS2, lessons2);
            result.put(JSON_PERCENT1, percent1);
            result.put(JSON_PERCENT2, percent2);
            result.putTerse(JSON_SUBJECT, course.getSubject());
            result.put(JSON_SCHOOL_CLASSES, JsonArray.createTerse(course.schoolClasses()));
            result.putTerse(JSON_GRADE, course.getGrade());
            return result;
        }

        public Grade grade() {
            return course.getGrade();
        }

        public double lessons1() {
            return lessons1;
        }

        public double lessons2() {
            return lessons2;
        }

        public double percent1() {
            return percent1;
        }

        public double percent2() {
            return percent2;
        }

        public Stream<SchoolClass> schoolClasses() {
            return course.schoolClasses();
        }

        public Subject subject() {
            return course.getSubject();
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }
    }
}
