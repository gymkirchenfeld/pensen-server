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
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;

public final class SchoolClass extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_CODE = "Code";
    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_DIVISION = "Division";
    public static final String DB_GRADUATION_YEAR = "GraduationYear";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_CODE = "code";
    public static final String JSON_CURRICULUM = "curriculum";
    public static final String JSON_DIVISION = "division";
    public static final String JSON_GRADUATION_YEAR = "graduationYear";

    private final Curriculum curriculum;
    private final int graduationYear;
    private boolean archived;
    private String code;
    private Division division;

    @PropertyInitializer({DB_CURRICULUM, DB_GRADUATION_YEAR, DB_ID})
    public SchoolClass(Curriculum curriculum, int graduationYear, int id) {
        super(id);
        this.curriculum = curriculum;
        this.graduationYear = graduationYear;
    }

    public boolean filter(Division division, Grade grade, SchoolYear schoolYear) {
        return (division == null || Util.equal(this.division, division)) &&
            (grade == null || Util.equal(gradeFor(schoolYear), grade));
    }

    public String getCode() {
        return code;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public Division getDivision() {
        return division;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public Grade gradeFor(SchoolYear schoolYear) {
        return curriculum.gradeFor(schoolYear, graduationYear);
    }

    public boolean isArchived() {
        return archived;
    }

    @Persistence(ignore = true)
    public boolean isActive(SchoolYear schoolYear) {
        return graduationYear >= schoolYear.getGraduationYear();
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CODE, code);
        result.putTerse(JSON_DIVISION, division);
        result.put(JSON_GRADUATION_YEAR, graduationYear);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.putTerse(JSON_CURRICULUM, curriculum);
        return result;
    }

    @Override
    protected int doCompare(Entity entity) {
        if (entity instanceof SchoolClass) {
            SchoolClass other = (SchoolClass) entity;
            int result = -Util.compare(graduationYear, other.graduationYear);
            if (result == 0) {
                result = Util.compare(code, other.code);
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
