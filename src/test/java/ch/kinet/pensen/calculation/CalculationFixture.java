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
package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.PoolEntry;
import ch.kinet.pensen.data.PoolType;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SchoolYearTestAccess;
import ch.kinet.pensen.data.Teacher;
import java.time.LocalDate;
import java.util.stream.Stream;

/**
 * Baut eine Anstellung ohne Datenbank auf, mit der alle Berechnungsmodi gefüttert werden können.
 */
final class CalculationFixture {

    /**
     * Wochenlektionen der Standard-Teilanstellung. Der Wert stammt aus dem Rechenbeispiel im
     * Kommentar von {@link CalculationLessons2#calculateLessonDifference(double, PayrollType)}.
     * 10.94 Lektionen entsprechen damit exakt 50 %.
     */
    static final double WEEKLY_LESSONS_GYM = 21.88;
    static final double WEEKLY_LESSONS_FMS = 22.5;

    /** Ohne Geburtstag liefert Teacher.ageOn() -1, es gibt also keine Altersentlastung. */
    static final LocalDate NO_AGE_RELIEF = null;
    /** Vor beiden Semestern 50 bzw. 51 Jahre alt: 4 % Altersentlastung in beiden Semestern. */
    static final LocalDate AGE_RELIEF_4 = LocalDate.of(1975, 1, 1);
    /** Vor beiden Semestern 54 bzw. 55 Jahre alt: 8 % Altersentlastung in beiden Semestern. */
    static final LocalDate AGE_RELIEF_8 = LocalDate.of(1971, 1, 1);
    /** Wird zwischen den beiden Stichtagen 50: 0 % im ersten, 4 % im zweiten Semester. */
    static final LocalDate AGE_RELIEF_0_4 = LocalDate.of(1975, 10, 15);

    /** Stichtage der Altersentlastung sind damit der 31.07.2025 und der 31.01.2026. */
    private static final int GRADUATION_YEAR = 2026;

    static CalculationFixture create(LocalDate birthday) {
        return new CalculationFixture(birthday);
    }

    /** Lektionenbasiert, Saldo-Reihenfolge 1, IPB-Korrektur erlaubt. Ist damit der Standardtyp. */
    final PayrollType gym;
    /** Lektionenbasiert, Saldo-Reihenfolge 2, IPB-Korrektur erlaubt. */
    final PayrollType fms;
    /** Prozentbasiert, Saldo-Reihenfolge 3, keine IPB-Korrektur. */
    final PayrollType percentBased;
    final SchoolYear schoolYear;
    final Teacher teacher;
    final Employment employment;

    private CalculationFixture(LocalDate birthday) {
        gym = new PayrollType("GYM2-4", "Gymnasium 2-4", 1, true, 1, true);
        fms = new PayrollType("FMS", "Fachmittelschule", 2, true, 2, true);
        percentBased = new PayrollType("DIV", "Diverses", 3, false, 3, false);

        schoolYear = new SchoolYear(GRADUATION_YEAR, 1);
        schoolYear.setCode("2025/26");
        schoolYear.setDescription("Schuljahr 2025/26");
        schoolYear.setWeeks(38);
        SchoolYearTestAccess.putWeeklyLessons(schoolYear, gym, WEEKLY_LESSONS_GYM);
        SchoolYearTestAccess.putWeeklyLessons(schoolYear, fms, WEEKLY_LESSONS_FMS);

        teacher = new Teacher(1);
        teacher.setBirthday(birthday);

        employment = new Employment(1, schoolYear, teacher);
    }

    CalculationFixture paymentTarget(double percent1, double percent2) {
        employment.setPayment1(percent1);
        employment.setPayment2(percent2);
        return this;
    }

    CalculationLessons lessons() {
        return new CalculationLessons(employment, payrollTypes());
    }

    CalculationLessonsAgeReliefIncluded lessonsAgeReliefIncluded() {
        return new CalculationLessonsAgeReliefIncluded(employment, payrollTypes());
    }

    CalculationLessons2 lessons2() {
        return new CalculationLessons2(employment, payrollTypes());
    }

    PoolEntry poolEntry(PayrollType payrollType, double percent1, double percent2) {
        PoolEntry result = new PoolEntry(1, schoolYear);
        result.setType(new PoolType(false, "POOL", "Pooleintrag", 1, payrollType));
        result.setDescription("Testeintrag");
        result.setTeacher(teacher);
        result.setPercent1(percent1);
        result.setPercent2(percent2);
        return result;
    }

    private Stream<PayrollType> payrollTypes() {
        return Stream.of(gym, fms, percentBased);
    }
}