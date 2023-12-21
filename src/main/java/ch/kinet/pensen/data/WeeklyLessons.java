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

import ch.kinet.reflect.PropertyInitializer;

public class WeeklyLessons {

    public static final String DB_LESSONS = "Lessons";
    public static final String DB_PAYROLL_TYPE = "PayrollType";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";

    private final SchoolYear schoolYear;
    private final PayrollType payrollType;
    private double lessons;

    @PropertyInitializer({DB_PAYROLL_TYPE, DB_SCHOOL_YEAR})
    public WeeklyLessons(PayrollType payrollType, SchoolYear schoolYear) {
        this.schoolYear = schoolYear;
        this.payrollType = payrollType;
    }

    public double getLessons() {
        return lessons;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public void setLessons(double lessons) {
        this.lessons = lessons;
    }
}
