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

import ch.kinet.Date;
import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;

public final class SchoolYear extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_CALCULATION_MODE = "CalculationMode";
    public static final String DB_CODE = "Code";
    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_FINALISED = "Finalised";
    public static final String DB_GRADUATION_YEAR = "GraduationYear";
    public static final String DB_WEEKS = "Weeks";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_CALCULATION_MODE = "calculationMode";
    public static final String JSON_CODE = "code";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_FINALISED = "finalised";
    public static final String JSON_GRADUATION_YEAR = "graduationYear";
    public static final String JSON_WEEKS = "weeks";
    private final int graduationYear;
    private boolean archived;
    private CalculationMode calculationMode;
    private String code;
    private String description;
    private boolean finalised;
    private SchoolYear next;
    private SchoolYear previous;
    private int weeks;

    @PropertyInitializer({DB_GRADUATION_YEAR, DB_ID})
    public SchoolYear(int graduationYear, int id) {
        super(id);
        this.graduationYear = graduationYear;
    }

    public double ageReliefFactor(Teacher teacher, SemesterEnum semester) {
        return _ageReliefFactor(teacher.ageOn(startOfSemester(semester)));
    }

    public CalculationMode getCalculationMode() {
        return calculationMode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isFinalised() {
        return finalised;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public int getWeeks() {
        return weeks;
    }

    public SchoolYear next() {
        return next;
    }

    public SchoolYear previous() {
        return previous;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setCalculationMode(CalculationMode calculationMode) {
        this.calculationMode = calculationMode;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFinalised(boolean finalised) {
        this.finalised = finalised;
    }

    public void setWeeks(int weeks) {
        this.weeks = weeks;
    }

    public Date startOfSemester(SemesterEnum semester) {
        switch (semester) {
            case First:
                return Date.create(1, 8, graduationYear - 1);
            case Second:
                return Date.create(1, 2, graduationYear);
            default:
                throw new IllegalArgumentException();
        }
    }

    void setPrevious(SchoolYear previous) {
        this.previous = previous;
        if (previous != null) {
            previous.next = this;
        }
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.putTerse(JSON_CALCULATION_MODE, calculationMode);
        result.put(JSON_FINALISED, finalised);
        result.put(JSON_GRADUATION_YEAR, graduationYear);
        result.put(JSON_WEEKS, weeks);
        return result;
    }

    @Override
    public String toString() {
        return getCode();
    }

    private static double _ageReliefFactor(int age) {
        if (age < 50) {
            return 0.0d;
        }

        if (age < 54) {
            return 4.0d;
        }

        if (age < 58) {
            return 8.0d;
        }

        return 12.0d;
    }
}
