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

import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ch.kinet.pensen.calculation.CalculationFixture.AGE_RELIEF_8;
import static ch.kinet.pensen.calculation.CalculationFixture.NO_AGE_RELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vergleicht die drei lektionenbasierten Berechnungsmodi anhand von Fällen, in denen sie sich
 * gleich bzw. unterschiedlich verhalten müssen.
 * <p>
 * Die Lektionen werden direkt über {@code sumPayrollLessons} eingespeist statt über
 * {@code addCourse}, weil ein vollständiger Kurs Division, Fach und Klassen mitbringen würde,
 * ohne dass die Berechnung davon abhinge. Nebeneffekt: die Zeile "Unterricht" der
 * Zusammenfassung bleibt in diesen Tests leer.
 */
class CalculationModesTest {

    private static final double DELTA = 1e-9;

    // ------------------------------------------------------------------------------------------
    // Fälle, in denen alle drei Modi übereinstimmen müssen
    // ------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Pensum trifft das Auszahlungsziel exakt: alle Modi gleich")
    void exactPaymentTargetIsIdenticalInAllModes() {
        // 10.94 Lektionen sind bei 21.88 Wochenlektionen exakt 50 %, es gibt also keine Differenz
        // zu verbuchen.
        assertExactHit(exactHitWorkload(Mode.LESSONS));
        assertExactHit(exactHitWorkload(Mode.AGE_RELIEF_INCLUDED));
        assertExactHit(exactHitWorkload(Mode.LESSONS2));
    }

    @Test
    @DisplayName("Pooleintrag ohne Altersentlastung: alle Modi gleich")
    void poolEntryWithoutAgeReliefIsIdenticalInAllModes() {
        assertPoolLine(poolWorkload(Mode.LESSONS, NO_AGE_RELIEF), 10.0, 10.0);
        assertPoolLine(poolWorkload(Mode.AGE_RELIEF_INCLUDED, NO_AGE_RELIEF), 10.0, 10.0);
        assertPoolLine(poolWorkload(Mode.LESSONS2, NO_AGE_RELIEF), 10.0, 10.0);
    }

    // ------------------------------------------------------------------------------------------
    // Altersentlastung bei Pooleinträgen
    // ------------------------------------------------------------------------------------------

    /*
     * Ein Pooleintrag von 10 % bei 8 % Altersentlastung. Am Kirchenfeld sind Pooleinträge
     * inklusive Altersentlastung erfasst, die anderen Modi rechnen die Altersentlastung
     * zusätzlich auf.
     */

    @Test
    @DisplayName("Pooleintrag mit AE: CalculationLessons rechnet die AE zusätzlich auf")
    void poolEntryWithAgeReliefInCalculationLessons() {
        assertPoolLine(poolWorkload(Mode.LESSONS, AGE_RELIEF_8), 10.0, 10.8);
    }

    @Test
    @DisplayName("Pooleintrag mit AE: CalculationLessons2 verhält sich wie CalculationLessons")
    void poolEntryWithAgeReliefInCalculationLessons2() {
        assertPoolLine(poolWorkload(Mode.LESSONS2, AGE_RELIEF_8), 10.0, 10.8);
    }

    @Test
    @DisplayName("Pooleintrag mit AE: CalculationLessonsAgeReliefIncluded rechnet die AE heraus")
    void poolEntryWithAgeReliefInCalculationLessonsAgeReliefIncluded() {
        assertPoolLine(poolWorkload(Mode.AGE_RELIEF_INCLUDED, AGE_RELIEF_8), 10.0 / 1.08, 10.0);
    }

    // ------------------------------------------------------------------------------------------
    // Rundung bei Unterbeschäftigung
    // ------------------------------------------------------------------------------------------

    /*
     * Ausgangslage: 10.94 Lektionen GYM2-4 (= 50 %), Auszahlungsziel 60 %, keine
     * Altersentlastung. Die Differenz von 10 % entspricht 2.188 Lektionen, das Ergebnis ist
     * also in jedem Modus rundungsbehaftet: 13.13 Lektionen sind 60.00914076782450 %.
     */

    @Test
    @DisplayName("Unterbeschäftigung: CalculationLessons rundet Prozente auf drei Dezimalstellen")
    void underEmploymentInCalculationLessons() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(60, 60);
        Workload workload = feedLessons(fixture.lessons(), fixture.gym, 10.94);

        Payroll.Item item = workload.payroll().getItem(fixture.gym);
        assertEquals(13.13, item.lessons().semester1(), DELTA);
        assertEquals(60.009, item.percent().semester1(), DELTA);
        // Die Rundung auf drei Dezimalstellen passt nicht zu percentDecimals() == 2, das dem
        // Frontend gemeldet wird.
        assertEquals(2, workload.payroll().percentDecimals());
    }

    @Test
    @DisplayName("Unterbeschäftigung: CalculationLessonsAgeReliefIncluded rundet auf zwei Dezimalstellen")
    void underEmploymentInCalculationLessonsAgeReliefIncluded() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(60, 60);
        Workload workload = feedLessons(fixture.lessonsAgeReliefIncluded(), fixture.gym, 10.94);

        Payroll.Item item = workload.payroll().getItem(fixture.gym);
        assertEquals(13.13, item.lessons().semester1(), DELTA);
        assertEquals(60.01, item.percent().semester1(), DELTA);
    }

    @Test
    @DisplayName("Unterbeschäftigung: CalculationLessons2 rundet die Korrekturlektionen, nicht die Prozente")
    void underEmploymentInCalculationLessons2() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(60, 60);
        Workload workload = feedLessons(fixture.lessons2(), fixture.gym, 10.94);

        // Die Differenz ist negativ, also wird gemäss Excel-Vorlage aufgerundet (weg von null).
        Payroll.IpbCorrectionData correction = workload.payroll().getIpbCorrection(SemesterEnum.First);
        assertEquals(-2.19, correction.ipbCorrectionLessons());

        Payroll.Item item = workload.payroll().getItem(fixture.gym);
        assertEquals(13.13, item.lessons().semester1(), DELTA);
        assertEquals(60.00914076782450, item.percent().semester1(), DELTA);
    }

    // ------------------------------------------------------------------------------------------
    // Überbeschäftigung mit zwei Teilanstellungen
    // ------------------------------------------------------------------------------------------

    /*
     * 4 Lektionen GYM2-4 (18.28 %) und 8 Lektionen FMS (35.56 %) bei einem Auszahlungsziel von
     * 10 %. Die zu verbuchende Differenz ist grösser als das Pensum der ersten Teilanstellung.
     */

    @Test
    @DisplayName("Überbeschäftigung: CalculationLessons bucht negatives Pensum auf die nächste Teilanstellung")
    void overEmploymentInCalculationLessons() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(10, 10);
        assertCarriedOver(fixture, feedOverEmployment(fixture.lessons(), fixture));
    }

    @Test
    @DisplayName("Überbeschäftigung: CalculationLessonsAgeReliefIncluded bucht ebenfalls um")
    void overEmploymentInCalculationLessonsAgeReliefIncluded() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(10, 10);
        assertCarriedOver(fixture, feedOverEmployment(fixture.lessonsAgeReliefIncluded(), fixture));
    }

    @Test
    @DisplayName("Überbeschäftigung: CalculationLessons2 meldet stattdessen negative Lektionen")
    void overEmploymentInCalculationLessons2() {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(10, 10);
        Workload workload = feedOverEmployment(fixture.lessons2(), fixture);

        // Die IPB-Korrektur landet auf der Teilanstellung mit den meisten Lektionen, nicht auf
        // der Teilanstellung mit der niedrigsten Saldo-Reihenfolge.
        Payroll.IpbCorrectionData correction = workload.payroll().getIpbCorrection(SemesterEnum.First);
        assertEquals(fixture.fms, correction.type());
        assertEquals(9.86, correction.ipbCorrectionLessons());

        // GYM2-4 bleibt unangetastet, FMS wird negativ: CalculationLessons2 kappt nicht bei 0.
        Payroll.Item gym = workload.payroll().getItem(fixture.gym);
        assertEquals(4.0, gym.lessons().semester1(), DELTA);
        Payroll.Item fms = workload.payroll().getItem(fixture.fms);
        assertEquals(-1.86, fms.lessons().semester1(), DELTA);
        assertTrue(fms.percent().semester1() < 0, "FMS-Prozente müssten negativ sein");

        // Dadurch wird das Auszahlungsziel knapp verfehlt.
        assertEquals(10.014868982327845, workload.payment(), DELTA);
    }

    // ------------------------------------------------------------------------------------------
    // IPB-Korrektur von CalculationLessons2
    // ------------------------------------------------------------------------------------------

    @Test
    @DisplayName("IPB-Korrektur: Rundungsartefakt von Gleitkommazahlen wird vermieden")
    void ipbCorrectionAvoidsFloatingPointRoundingArtefact() {
        /*
         * Der Fall, den das Vorrunden auf acht Dezimalstellen in calculateLessonDifference
         * abfängt: 16.5 Lektionen GYM2-4 bei einem Auszahlungsziel von 50 %. Da 10.94 Lektionen
         * exakt 50 % sind, beträgt die IPB-Korrektur exakt 16.5 - 10.94 = 5.56 Lektionen. Als
         * double ist der Zwischenwert aber 5.559999999999999, ohne das Vorrunden würde
         * ABRUNDEN() daraus 5.55 machen.
         */
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(50, 50);
        Workload workload = feedLessons(fixture.lessons2(), fixture.gym, 16.5);

        Payroll.IpbCorrectionData correction = workload.payroll().getIpbCorrection(SemesterEnum.First);
        assertEquals(fixture.gym, correction.type());
        assertEquals(5.56, correction.ipbCorrectionLessons());
        assertEquals(16.5, correction.lessonsWithoutCorrection(), DELTA);
        assertEquals(75.41133455210237, correction.percentWithoutCorrection(), DELTA);

        Payroll.Item item = workload.payroll().getItem(fixture.gym);
        assertEquals(10.94, item.lessons().semester1(), DELTA);
        assertEquals(50.0, item.percent().semester1(), DELTA);
        assertEquals(50.0, workload.payment(), DELTA);
    }

    // ------------------------------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------------------------------

    private enum Mode {
        LESSONS,
        AGE_RELIEF_INCLUDED,
        LESSONS2
    }

    private static Calculation create(CalculationFixture fixture, Mode mode) {
        switch (mode) {
            case LESSONS:
                return fixture.lessons();
            case AGE_RELIEF_INCLUDED:
                return fixture.lessonsAgeReliefIncluded();
            case LESSONS2:
                return fixture.lessons2();
            default:
                throw new IllegalArgumentException();
        }
    }

    private static Workload feedLessons(Calculation calculation, PayrollType type, double lessons) {
        calculation.sumPayrollLessons(type, SemesterEnum.First, lessons, 0);
        calculation.sumPayrollLessons(type, SemesterEnum.Second, lessons, 0);
        return calculation.createWorkload();
    }

    private static Workload feedOverEmployment(Calculation calculation, CalculationFixture fixture) {
        calculation.sumPayrollLessons(fixture.gym, SemesterEnum.First, 4, 0);
        calculation.sumPayrollLessons(fixture.gym, SemesterEnum.Second, 4, 0);
        calculation.sumPayrollLessons(fixture.fms, SemesterEnum.First, 8, 0);
        calculation.sumPayrollLessons(fixture.fms, SemesterEnum.Second, 8, 0);
        return calculation.createWorkload();
    }

    private static Workload exactHitWorkload(Mode mode) {
        CalculationFixture fixture = CalculationFixture.create(NO_AGE_RELIEF).paymentTarget(50, 50);
        return feedLessons(create(fixture, mode), fixture.gym, 10.94);
    }

    private static Workload poolWorkload(Mode mode, java.time.LocalDate birthday) {
        CalculationFixture fixture = CalculationFixture.create(birthday);
        Calculation calculation = create(fixture, mode);
        calculation.addPoolEntry(fixture.poolEntry(fixture.percentBased, 10, 10));
        return calculation.createWorkload();
    }

    private static void assertExactHit(Workload workload) {
        // In diesem Szenario ist GYM2-4 die einzige Teilanstellung mit einem Pensum.
        Payroll.Item item = workload.payroll().items().findFirst().orElseThrow(AssertionError::new);
        assertEquals(10.94, item.lessons().semester1(), DELTA);
        assertEquals(10.94, item.lessons().semester2(), DELTA);
        assertEquals(50.0, item.percent().semester1(), DELTA);
        assertEquals(50.0, item.percent().semester2(), DELTA);
        assertEquals(50.0, workload.payment(), DELTA);
    }

    private static void assertCarriedOver(CalculationFixture fixture, Workload workload) {
        Payroll.Item gym = workload.payroll().getItem(fixture.gym);
        assertEquals(0.0, gym.lessons().semester1(), DELTA);
        assertEquals(0.0, gym.percent().semester1(), DELTA);
        Payroll.Item fms = workload.payroll().getItem(fixture.fms);
        assertEquals(2.25, fms.lessons().semester1(), DELTA);
        assertEquals(10.0, fms.percent().semester1(), DELTA);
        assertEquals(10.0, workload.payment(), DELTA);
    }

    private static void assertPoolLine(Workload workload, double expectedPercent, double expectedWithAgeRelief) {
        Summary.Item item = workload.summary().items()
            .filter(candidate -> "Pool".equals(candidate.description()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Keine Pool-Zeile in der Zusammenfassung"));
        assertEquals(expectedPercent, item.percent1(), DELTA);
        assertEquals(expectedPercent, item.percent2(), DELTA);
        assertEquals(expectedWithAgeRelief, item.percentWithAgeRelief(), DELTA);
    }
}