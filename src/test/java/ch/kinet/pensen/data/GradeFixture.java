/*
 * Copyright (C) 2026 by Johannes Lieberherr (ttools gmbh)
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

import java.util.stream.Stream;

public final class GradeFixture {

    public static final Grade GYM1 = grade(1, "GYM1", "GYM1", PayrollTypeFixture.G1, false, PayrollTypeFixture.GK1);
    public static final Grade GYM2 = grade(2, "GYM2", "GYM2", PayrollTypeFixture.G2, false, PayrollTypeFixture.GK2);
    public static final Grade GYM3 = grade(3, "GYM3", "GYM3", PayrollTypeFixture.G2, false, PayrollTypeFixture.GK2);
    public static final Grade GYM4 = grade(4, "GYM4", "GYM4", PayrollTypeFixture.G2, false, PayrollTypeFixture.GK2);
    public static final Grade GYM5 = grade(5, "GYM5", "GYM5", PayrollTypeFixture.G2, false, PayrollTypeFixture.GK2);
    public static final Grade FMS1 = grade(6, "FMS1", "FMS1", PayrollTypeFixture.F, false, PayrollTypeFixture.FK);
    public static final Grade FMS2 = grade(7, "FMS2", "FMS2", PayrollTypeFixture.F, false, PayrollTypeFixture.FK);
    public static final Grade FMS3 = grade(8, "FMS3", "FMS3", PayrollTypeFixture.F, false, PayrollTypeFixture.FK);
    public static final Grade BME1 = grade(9, "BME1", "BME1", PayrollTypeFixture.BM, false, PayrollTypeFixture.BM);
    public static final Grade BME2 = grade(10, "BME2", "BME2", PayrollTypeFixture.BM, false, PayrollTypeFixture.BM);
    public static final Grade BME3 = grade(11, "BME3", "BME3", PayrollTypeFixture.BM, false, PayrollTypeFixture.BM);
    public static final Grade BME4 = grade(12, "BME4", "BME4", PayrollTypeFixture.BM, false, PayrollTypeFixture.BM);
    public static final Grade BMEP = grade(13, "BMEP", "BMEP", PayrollTypeFixture.BP, false, PayrollTypeFixture.BP);
    public static final Grade FMSP = grade(14, "FMSP", "FMSP", PayrollTypeFixture.F, false, PayrollTypeFixture.FK);


    public static Stream<Grade> stream() {
        return Stream.of(GYM1, GYM2, GYM3, GYM4, GYM5, FMS1, FMS2, FMS3, BME1, BME2, BME3, BME4, BMEP, FMSP);
    }


    private static Grade grade(int id, String code, String description, PayrollType payrollType,
                               boolean archived, PayrollType classLessonPayrollType) {
        Grade result = new Grade(id);
        result.setCode(code);
        result.setDescription(description);
        result.setPayrollType(payrollType);
        result.setArchived(archived);
        result.setClassLessonPayrollType(classLessonPayrollType);
        return result;
    }

    private GradeFixture() {
    }
}
