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
import ch.kinet.reflect.PropertyInitializer;

public final class PostingDetail {

    public static final String DB_POSTING = "Posting";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_TEACHER = "Teacher";
    public static final String DB_TYPE = "Type";
    public static final String DB_VALUE = "Value";
    private final Posting posting;
    private final SchoolYear schoolYear;
    private final Teacher teacher;
    private final PostingType type;
    private final double value;

    @PropertyInitializer({DB_POSTING, DB_SCHOOL_YEAR, DB_TEACHER, DB_TYPE, DB_VALUE})
    public PostingDetail(Posting posting, SchoolYear schoolYear, Teacher teacher, PostingType type, double value) {
        this.posting = posting;
        this.schoolYear = schoolYear;
        this.teacher = teacher;
        this.type = type;
        this.value = value;
    }

    public boolean filter(Teacher teacher) {
        return Util.equal(this.teacher, teacher);
    }

    public Posting getPosting() {
        return posting;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public PostingType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }
}
