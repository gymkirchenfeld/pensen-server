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

import ch.kinet.Util;
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;

public class ThesisEntry {

    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_TEACHER = "Teacher";
    public static final String DB_TYPE = "Type";
    public static final String DB_COUNT = "Count";
    private final SchoolYear schoolYear;
    private final Teacher teacher;
    private final ThesisType type;
    private final double count;

    @PropertyInitializer({DB_COUNT, DB_SCHOOL_YEAR, DB_TEACHER, DB_TYPE})
    public ThesisEntry(double count, SchoolYear schoolYear, Teacher teacher, ThesisType type) {
        this.schoolYear = schoolYear;
        this.teacher = teacher;
        this.type = type;
        this.count = count;
    }

    public boolean filter(Teacher teacher) {
        return Util.equal(this.teacher, teacher);
    }

    public double getCount() {
        return count;
    }

    @Persistence(key = true)
    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    @Persistence(key = true)
    public Teacher getTeacher() {
        return teacher;
    }

    @Persistence(key = true)
    public ThesisType getType() {
        return type;
    }
}
