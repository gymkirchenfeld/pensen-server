/*
 * Copyright (C) 2022 - 2024 by Sebastian Forster, Stefan Rothe
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
import ch.kinet.reflect.PropertyInitializer;
import java.time.LocalDate;
import java.util.Optional;

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
    public static final String JSON_LESSONS = "lessons";
    public static final String JSON_PAYROLL_TYPE = "payrollType";
    public static final String JSON_WEEKLY_LESSONS = "weeklyLessons";
    public static final String JSON_WEEKS = "weeks";
    private final int graduationYear;
    private final ValueMap<PayrollType> weeklyLessons = ValueMap.create();
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
        // Stichtag ist der Tag vor dem Beginn des Semesters
        return _ageReliefFactor(teacher.ageOn(startOfSemester(semester).minusDays(1)));
    }

    public CalculationMode.Enum calculationModeEnum() {
        return CalculationMode.toEnum(calculationMode);
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

    public double lessonsToPercent(PayrollType payrollType, double lessons) {
        Optional<Double> wl = weeklyLessons.get(payrollType);
        if (wl.isEmpty()) {
            return 0;
        }

        return lessons * 100 / wl.get();
    }

    public SchoolYear next() {
        return next;
    }

    public double percentToLessons(PayrollType payrollType, double percent) {
        Optional<Double> wl = weeklyLessons.get(payrollType);
        if (wl.isEmpty()) {
            return 0;
        }

        return percent * wl.get() / 100;
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

    public LocalDate startOfSemester(SemesterEnum semester) {
        switch (semester) {
            case First:
                return LocalDate.of(graduationYear - 1, 8, 1);
            case Second:
                return LocalDate.of(graduationYear, 2, 1);
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
        JsonObject result = super.toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.putTerse(JSON_CALCULATION_MODE, calculationMode);
        result.put(JSON_FINALISED, finalised);
        result.put(JSON_GRADUATION_YEAR, graduationYear);
        result.put(JSON_WEEKS, weeks);
        result.putTerse(JSON_WEEKLY_LESSONS, weeklyLessons);
        return result;
    }

    @Override
    public String toString() {
        return getCode();
    }

    public double weeklyLessons(PayrollType payrollType) {
        return weeklyLessons.get(payrollType).orElse(0.0);
    }

    void clearWeeklyLessons() {
        weeklyLessons.clear();
    }

    void putWeeklyLessons(PayrollType payrollType, double lessons) {
        weeklyLessons.put(payrollType, lessons);
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
