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

public final class Employment extends Entity {

    public static final String DB_CLOSING_BALANCE = "ClosingBalance";
    public static final String DB_COMMENTS = "Comments";
    public static final String DB_DIVISION = "Division";
    public static final String DB_EMPLOYMENT_MAX = "EmploymentMax";
    public static final String DB_EMPLOYMENT_MIN = "EmploymentMin";
    public static final String DB_OPENING_BALANCE = "OpeningBalance";
    public static final String DB_PAYMENT1 = "Payment1";
    public static final String DB_PAYMENT2 = "Payment2";
    public static final String DB_SCHOOL_YEAR = "SchoolYear";
    public static final String DB_TEACHER = "Teacher";
    public static final String DB_TEMPORARY = "Temporary";
    public static final String JSON_CHANGE = "change";
    public static final String JSON_CLOSING_BALANCE = "closingBalance";
    public static final String JSON_COMMENTS = "comments";
    public static final String JSON_DIVISION = "division";
    public static final String JSON_EMPLOYMENT_MAX = "employmentMax";
    public static final String JSON_EMPLOYMENT_MIN = "employmentMin";
    public static final String JSON_OPENING_BALANCE = "openingBalance";
    public static final String JSON_PAYMENT1 = "payment1";
    public static final String JSON_PAYMENT2 = "payment2";
    public static final String JSON_SCHOOL_YEAR = "schoolYear";
    public static final String JSON_TEACHER = "teacher";
    public static final String JSON_TEMPORARY = "temporary";
    private final SchoolYear schoolYear;
    private final Teacher teacher;
    private double closingBalance;
    private String comments;
    private Division division;
    private double employmentMax;
    private double employmentMin;
    private double openingBalance;
    private double payment1;
    private double payment2;
    private boolean temporary;

    @PropertyInitializer({DB_ID, DB_SCHOOL_YEAR, DB_TEACHER})
    public Employment(int id, SchoolYear schoolYear, Teacher teacher) {
        super(id);
        this.schoolYear = schoolYear;
        this.teacher = teacher;
    }

    public SemesterValue withAgeRelief(SemesterValue percent) {
        return SemesterValue.create(
            withAgeRelief(SemesterEnum.First, percent.semester1()),
            withAgeRelief(SemesterEnum.Second, percent.semester2())
        );
    }

    public double withAgeRelief(SemesterEnum semester, double percent) {
        return percent * (1.0 + schoolYear.ageReliefFactor(teacher, semester) / 100);
    }

    public double withoutAgeRelief(SemesterEnum semester, double percent) {
        return percent / (1.0 + schoolYear.ageReliefFactor(teacher, semester) / 100);
    }

    public double ageReliefFactor(SemesterEnum semester) {
        return schoolYear.ageReliefFactor(teacher, semester);
    }

    public double getClosingBalance() {
        return closingBalance;
    }

    public String getComments() {
        return comments;
    }

    public Division getDivision() {
        return division;
    }

    public double getEmploymentMax() {
        return employmentMax;
    }

    public double getEmploymentMin() {
        return employmentMin;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }

    public double getPayment1() {
        return payment1;
    }

    public double getPayment2() {
        return payment2;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public SemesterValue paymentTarget() {
        return SemesterValue.create(payment1, payment2);
    }

    public void setClosingBalance(double closingBalance) {
        this.closingBalance = closingBalance;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public void setEmploymentMax(double employmentMax) {
        this.employmentMax = employmentMax;
    }

    public void setEmploymentMin(double employmentMin) {
        this.employmentMin = employmentMin;
    }

    public void setOpeningBalance(double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public void setPayment1(double payment1) {
        this.payment1 = payment1;
    }

    public void setPayment2(double payment2) {
        this.payment2 = payment2;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CHANGE, closingBalance - openingBalance);
        result.put(JSON_CLOSING_BALANCE, closingBalance);
        result.put(JSON_COMMENTS, comments);
        result.putTerse(JSON_DIVISION, division);
        result.put(JSON_EMPLOYMENT_MAX, employmentMax);
        result.put(JSON_EMPLOYMENT_MIN, employmentMin);
        result.put(JSON_OPENING_BALANCE, openingBalance);
        result.put(JSON_PAYMENT1, payment1);
        result.put(JSON_PAYMENT2, payment2);
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.putTerse(JSON_TEACHER, teacher);
        result.put(JSON_TEMPORARY, temporary);
        return result;
    }

    @Override
    protected int doCompare(Entity entity) {
        int result = 0;
        if (entity instanceof Employment) {
            Employment other = (Employment) entity;
            result = -Util.compare(schoolYear, other.schoolYear);
            if (result == 0) {
                result = Util.compare(teacher, other.teacher);
            }
        }

        if (result == 0) {
            result = super.doCompare(entity);
        }

        return result;
    }
}
