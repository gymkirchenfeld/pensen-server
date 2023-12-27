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
package ch.kinet.pensen.data;

import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;

public class LessonTableEntry {

    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_DIVISION = "Division";
    public static final String DB_GRADE = "Grade";
    public static final String DB_LESSONS_1 = "Lessons1";
    public static final String DB_LESSONS_2 = "Lessons2";
    public static final String DB_SUBJECT = "Subject";
    public static final String DB_TYPE = "Type";

    private final Curriculum curriculum;
    private final Division division;
    private final Grade grade;
    private final Subject subject;
    private double lessons1;
    private double lessons2;
    private LessonType type;

    @PropertyInitializer({DB_CURRICULUM, DB_DIVISION, DB_GRADE, DB_SUBJECT})
    public LessonTableEntry(Curriculum curriculum, Division division, Grade grade, Subject subject) {
        this.curriculum = curriculum;
        this.division = division;
        this.grade = grade;
        this.subject = subject;
    }

    @Persistence(key = true)
    public Curriculum getCurriculum() {
        return curriculum;
    }

    @Persistence(key = true)
    public Division getDivision() {
        return division;
    }

    @Persistence(key = true)
    public Grade getGrade() {
        return grade;
    }

    public double getLessons1() {
        return lessons1;
    }

    public double getLessons2() {
        return lessons2;
    }

    @Persistence(key = true)
    public Subject getSubject() {
        return subject;
    }

    public LessonType getType() {
        return type;
    }

    public void setLessons1(double lessons1) {
        this.lessons1 = lessons1;
    }

    public void setLessons2(double lessons2) {
        this.lessons2 = lessons2;
    }

    public void setType(LessonType type) {
        this.type = type;
    }

    public LessonType.Enum typeEnum() {
        return LessonType.toEnum(type);
    }
}
