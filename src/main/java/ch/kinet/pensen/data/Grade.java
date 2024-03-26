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

public final class Grade extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_CLASS_LESSON_PAYROLL_TYPE = "ClassLessonPayrollType";
    public static final String DB_CODE = "Code";
    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_PAYROLL_TYPE = "PayrollType";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_CLASS_LESSON_PAYROLL_TYPE = "classLessonPayrollType";
    public static final String JSON_CODE = "code";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_PAYROLL_TYPE = "payrollType";
    private boolean archived;
    private PayrollType classLessonPayrollType;
    private String code;
    private String description;
    private PayrollType payrollType;

    @PropertyInitializer({DB_ID})
    public Grade(int id) {
        super(id);
    }

    public PayrollType getClassLessonPayrollType() {
        return classLessonPayrollType;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setClassLessonPayrollType(PayrollType classLessonPayrollType) {
        this.classLessonPayrollType = classLessonPayrollType;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPayrollType(PayrollType payrollType) {
        this.payrollType = payrollType;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        result.put(JSON_ARCHIVED, archived);
        result.putTerse(JSON_PAYROLL_TYPE, payrollType);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.putTerse(JSON_CLASS_LESSON_PAYROLL_TYPE, classLessonPayrollType);
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        return result;
    }

    @Override
    public String toString() {
        return getCode();
    }
}
