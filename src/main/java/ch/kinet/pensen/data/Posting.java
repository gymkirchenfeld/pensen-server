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

import ch.kinet.Date;
import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.PropertyInitializer;

public final class Posting extends Entity {

    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_END_DATE = "EndDate";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_START_DATE = "StartDate";
    public static final String DB_TEACHER = "Teacher";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_DETAILS = "details";
    public static final String JSON_END_DATE = "endDate";
    public static final String JSON_SCHOOL_YEAR = "schoolYear";
    public static final String JSON_START_DATE = "startDate";
    public static final String JSON_TEACHER = "teacher";

    private final SchoolYear schoolYear;
    private String description;
    private Date endDate;
    private Date startDate;
    private Teacher teacher;

    @PropertyInitializer({DB_ID, DB_SCHOOL_YEAR})
    public Posting(int id, SchoolYear schoolYear) {
        super(id);
        this.schoolYear = schoolYear;
    }

    public boolean filter(Teacher teacher) {
        return Util.equal(this.teacher, teacher);
    }

    public String getDescription() {
        return description;
    }

    public Date getEndDate() {
        return endDate;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public SemesterEnum semester() {
        if (startDate == null || startDate.before(schoolYear.startOfSemester(SemesterEnum.Second))) {
            return SemesterEnum.First;
        }
        else {
            return SemesterEnum.Second;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_DESCRIPTION, description);
        result.put(JSON_END_DATE, endDate);
        result.put(JSON_START_DATE, startDate);
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.putTerse(JSON_TEACHER, teacher);
        return result;
    }

    @Override
    protected int doCompare(Entity entity) {
        if (entity instanceof Posting) {
            Posting other = (Posting) entity;
            int result = Util.compare(teacher, other.teacher);
            if (result == 0) {
                result = Util.compare(startDate, other.startDate);
            }

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
