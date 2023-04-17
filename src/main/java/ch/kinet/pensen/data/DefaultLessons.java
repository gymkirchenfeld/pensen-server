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

import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;
import java.util.ArrayList;
import java.util.List;

public class DefaultLessons extends Entity {

    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_DIVISION = "Division";
    public static final String DB_LESSONS_1 = "Lessons1";
    public static final String DB_LESSONS_2 = "Lessons2";
    public static final String DB_SUBJECT = "Subject";
    public static final String JSON_CURRICULUM = "curriculum";
    public static final String JSON_DIVISION = "division";
    public static final String JSON_LESSONS_1 = "lessons1";
    public static final String JSON_LESSONS_2 = "lessons2";
    public static final String JSON_SUBJECT = "subject";
    private final Curriculum curriculum;
    private final Division division;
    private final Subject subject;
    private EntityMap<Grade> lessonMap1 = EntityMap.create();
    private EntityMap<Grade> lessonMap2 = EntityMap.create();
    private List<Double> lessons1 = new ArrayList<>();
    private List<Double> lessons2 = new ArrayList<>();

    @PropertyInitializer({DB_CURRICULUM, DB_DIVISION, DB_ID, DB_SUBJECT})
    public DefaultLessons(Curriculum curriculum, Division division, int id, Subject subject) {
        super(id);
        this.curriculum = curriculum;
        this.division = division;
        this.subject = subject;
    }

    @Override
    public int compareTo(Entity entity) {
        if (entity instanceof DefaultLessons) {
            DefaultLessons other = (DefaultLessons) entity;
            int result = Util.compare(subject, other.subject);
            return result == 0 ? super.compareTo(entity) : result;
        }
        else {
            return super.compareTo(entity);
        }
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public Division getDivision() {
        return division;
    }

    public List<Double> getLessons1() {
        return lessonMap1.toList();
    }

    public List<Double> getLessons2() {
        return lessonMap2.toList();
    }

    public Subject getSubject() {
        return subject;
    }

    public EntityMap<Grade> lessonMap(SemesterEnum semester) {
        switch (semester) {
            case First:
                return lessonMap1;
            case Second:
                return lessonMap2;
            default:
                return null;
        }
    }

    public Double lessonsFor(SemesterEnum semester, Grade grade) {
        return lessonMap(semester).get(grade);
    }

    @Persistence(ignore = true)
    public void setLessonMap1(EntityMap<Grade> lessonMap1) {
        this.lessonMap1 = lessonMap1;
    }

    @Persistence(ignore = true)
    public void setLessonMap2(EntityMap<Grade> lessonMap2) {
        this.lessonMap2 = lessonMap2;
    }

    public void setLessons1(List<Double> lessons1) {
        this.lessons1 = lessons1;
        if (this.lessons1 == null) {
            this.lessons1 = new ArrayList<>();
        }
    }

    public void setLessons2(List<Double> lessons2) {
        this.lessons2 = lessons2;
        if (this.lessons2 == null) {
            this.lessons2 = new ArrayList<>();
        }
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.putTerse(JSON_CURRICULUM, curriculum);
        result.putTerse(JSON_DIVISION, division);
        result.putTerse(JSON_SUBJECT, subject);
        result.putTerse(JSON_LESSONS_1, lessonMap1);
        result.putTerse(JSON_LESSONS_2, lessonMap2);
        return result;
    }

    DefaultLessons resolve() {
        lessonMap1 = EntityMap.parseList(lessons1, curriculum.grades());
        lessonMap2 = EntityMap.parseList(lessons2, curriculum.grades());
        return this;
    }
}
