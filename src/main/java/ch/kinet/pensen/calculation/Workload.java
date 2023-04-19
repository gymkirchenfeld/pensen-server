/*
 * Copyright (C) 2022 by Sebastian Forster, Stefan Rothe
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
package ch.kinet.pensen.calculation;

import ch.kinet.Json;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Teacher;

public final class Workload implements Json {

    private static final String JSON_COURSES = "courses";
    private static final String JSON_PAYROLL = "payroll";
    private static final String JSON_POOL = "pool";
    private static final String JSON_POSTINGS = "postings";
    private static final String JSON_SCHOOL_YEAR = "schoolYear";
    private static final String JSON_SUMMARY = "summary";
    private static final String JSON_TEACHER = "teacher";
    private static final String JSON_THESES = "theses";

    private final double ageReliefFactor1;
    private final double ageReliefFactor2;
    private final Courses courses;
    private final Employment employment;
    private final double payment;
    private final Payroll payroll;
    private final Pool pool;
    private final Postings postings;
    private final SchoolYear schoolYear;
    private final Summary summary;
    private final Teacher teacher;
    private final Theses theses;
    private double closingBalance;

    Workload(Employment employment, Courses courses, Pool pool, Theses theses, Postings postings, Summary summary,
             Payroll payroll) {
        ageReliefFactor1 = employment.ageReliefFactor(SemesterEnum.First);
        ageReliefFactor2 = employment.ageReliefFactor(SemesterEnum.Second);
        teacher = employment.getTeacher();
        this.employment = employment;
        this.schoolYear = employment.getSchoolYear();
        this.courses = courses;
        this.pool = pool;
        this.postings = postings;
        this.theses = theses;
        this.summary = summary;
        this.payroll = payroll;
        payment = payroll.percent().mean();

        closingBalance = employment.getOpeningBalance();
        closingBalance += summary.total().percentWithAgeRelief();
        closingBalance += postings.totalPercent();
        closingBalance -= payment;
//        payroll.addDifference(employment.getPayment1(), employment.getPayment2());
    }

    public double ageReliefFactor(SemesterEnum semester) {
        switch (semester) {
            case First:
                return ageReliefFactor1;
            case Second:
                return ageReliefFactor2;
            default:
                return 0.0;
        }
    }

    public double ageReliefFactor1() {
        return ageReliefFactor1;
    }

    public double ageReliefFactor2() {
        return ageReliefFactor2;
    }

    public Courses courses() {
        return courses;
    }

    public double getClosingBalance() {
        return closingBalance;
    }

    public Employment getEmployment() {
        return employment;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public Payroll payroll() {
        return payroll;
    }

    public Pool pool() {
        return pool;
    }

    public Postings postings() {
        return postings;
    }

    public Summary summary() {
        return summary;
    }

    public Theses theses() {
        return theses;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.putTerse(JSON_TEACHER, teacher);
        result.putTerse(JSON_COURSES, courses);
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.putTerse(JSON_PAYROLL, payroll);
        result.putTerse(JSON_POOL, pool);
        result.putTerse(JSON_POSTINGS, postings);
        result.putTerse(JSON_THESES, theses);
        result.putTerse(JSON_SUMMARY, summary);
        result.put("balance", balance());
        result.putTerse("payroll", payroll);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        return toJsonTerse();
    }

    private JsonArray balance() {
        JsonArray result = JsonArray.create();
        result.add(balanceLine("Anfangssaldo", employment.getOpeningBalance()));
        result.add(balanceLine("Pensum", summary.total().percentWithAgeRelief()));
        result.add(balanceLine("Ein- und Ausbuchungen", postings.totalPercent()));
        result.add(balanceLine("Auszahlung", -payment));
        result.add(balanceLine("Schlusssaldo", closingBalance));
        return result;
    }

    private JsonObject balanceLine(String description, double percent) {
        JsonObject result = JsonObject.create();
        result.put("description", description);
        result.put("percent", percent);
        return result;
    }
}
