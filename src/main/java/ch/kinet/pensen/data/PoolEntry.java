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

import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.PropertyInitializer;

public final class PoolEntry extends Entity {

    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_PERCENT_1 = "Percent1";
    public static final String DB_PERCENT_2 = "Percent2";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_TEACHER = "Teacher";
    public static final String DB_TYPE = "Type";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_PERCENT_1 = "percent1";
    public static final String JSON_PERCENT_2 = "percent2";
    public static final String JSON_SCHOOL_YEAR = "schoolYear";
    public static final String JSON_TEACHER = "teacher";
    public static final String JSON_TYPE = "type";

    private final SchoolYear schoolYear;
    private String description;
    private double percent1;
    private double percent2;
    private PoolType type;
    private Teacher teacher;

    @PropertyInitializer({DB_ID, DB_SCHOOL_YEAR})
    public PoolEntry(int id, SchoolYear schoolYear) {
        super(id);
        this.schoolYear = schoolYear;
    }

    public boolean filter(Teacher teacher) {
        return Util.equal(this.teacher, teacher);
    }

    public String getDescription() {
        return description;
    }

    public double getPercent1() {
        return percent1;
    }

    public double getPercent2() {
        return percent2;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public PoolType getType() {
        return type;
    }

    public SemesterValue percent() {
        return SemesterValue.create(percent1, percent2);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPercent1(double percent1) {
        this.percent1 = percent1;
    }

    public void setPercent2(double percent2) {
        this.percent2 = percent2;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public void setType(PoolType type) {
        this.type = type;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_DESCRIPTION, description);
        result.put(JSON_PERCENT_1, percent1);
        result.put(JSON_PERCENT_2, percent2);
        result.putTerse(JSON_TYPE, type);
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.putTerse(JSON_TEACHER, teacher);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        return toJsonTerse();
    }

    @Override
    protected int doCompare(Entity entity) {
        if (entity instanceof PoolEntry) {
            PoolEntry other = (PoolEntry) entity;
            int result = Util.compare(teacher, other.teacher);
            if (result == 0) {
                result = Util.compare(description, other.description);
            }

            if (result == 0) {
                result = super.doCompare(entity);
            }

            return result;
        }
        else {
            return super.doCompare(entity);
        }
    }
}
