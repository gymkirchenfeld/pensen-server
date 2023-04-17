/*
 * Copyright (C) 2023 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.reflect.PropertyInitializer;

public class DefaultLessonEntry {

    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_DIVISION = "Division";
    public static final String DB_GRADE = "Grade";
    public static final String DB_LESSONS = "Lessons";
    public static final String DB_SUBJECT = "Subject";
    private final Curriculum curriculum;
    private final Division division;
    private final Grade grade;
    private final Subject subject;
    private double lessons;

    @PropertyInitializer({DB_CURRICULUM, DB_DIVISION, DB_GRADE, DB_SUBJECT})
    public DefaultLessonEntry(Curriculum curriculum, Division division, Grade grade, Subject subject) {
        this.curriculum = curriculum;
        this.division = division;
        this.grade = grade;
        this.subject = subject;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public Division getDivision() {
        return division;
    }

    public Grade getGrade() {
        return grade;
    }

    public double getLessons() {
        return lessons;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setLessons(double lessons) {
        this.lessons = lessons;
    }
}
