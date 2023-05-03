/*
 * Copyright (C) 2023 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Util;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.Posting;
import ch.kinet.pensen.data.SemesterEnum;
import static ch.kinet.pensen.data.SemesterEnum.First;
import ch.kinet.pensen.data.SemesterValue;

public final class CalculationHistoric extends Calculation {

    private final PayrollType defaultType;
    private final PayrollMap payrollMap = PayrollMap.create();
    private final SemesterValue totalPercent = SemesterValue.create();

    CalculationHistoric(Employment employment, PayrollType defaultType) {
        super(employment, poolTitle(employment));
        this.defaultType = defaultType;
    }

    @Override
    void addToPayroll(PayrollType type, SemesterEnum semester, double value) {
        // Alle Berechnungen werden in Prozent durchgeführt
        if (type.lessonBased()) {
            value = type.lessonsToPercent(value);
        }

        totalPercent.add(semester, value);
        payrollMap.add(type, semester, value);
    }

    @Override
    double calculatePayment() {
        return employment.payment().mean();
    }

    @Override
    void calculatePayroll() {
        payrollMap.ensureType(defaultType);
        // Differenz zwischen Auszahlung und tatsächlichem Pensum berechnen
        SemesterValue diff = employment.payment().map(
            (s, payment) -> payment - employment.withAgeRelief(s, totalPercent.get(s))
        );

        payrollMap.types().forEachOrdered(type -> {
            // Berechne Prozentwert inklusive Altersentlastung
            SemesterValue percent = payrollMap.get(type).map((s, p) -> employment.withAgeRelief(s, p));
            // Am Kirchenfeld wurde die Differenz zwischen Pensum und Auszahlung immer
            // mit der Teilanstellung Unterricht GYM2-4 verrechnet.
            if (Util.equal(defaultType, type)) {
                percent.add(diff);
            }

            SemesterValue lessons = SemesterValue.create();
            if (type.lessonBased()) {
                // aus Prozentwert wieder Lektionen berechnen (für Buchung in SAP)
                lessons = percent.map((s, p) -> type.percentToLessons(employment.withoutAgeRelief(s, p)));
            }

            // Runde Lektionen auf zwei Dezimalstellen
            lessons = lessons.map((s, l) -> Math.round(l * 100) / 100.0);
            // Runde Prozente auf drei Dezimalstellen
            percent = percent.map((s, l) -> Math.round(l * 1000) / 1000.0);

            payroll.add(type, lessons, percent);
        });

    }

    @Override
    void handlePostingDetailLessons(Posting posting, PayrollType payrollType, double lessons) {
        if (lessons == 0) {
            return;
        }

        final double ageReliefFactor = employment.ageReliefFactor(posting.semester());
        final double percentWithoutAgeRelief = payrollType.lessonsToPercent(lessons) / employment.getSchoolYear().getWeeks();
        final double ageRelief = percentWithoutAgeRelief * ageReliefFactor / 100.0;
        postings.addDetail(posting, payrollType, lessons, percentWithoutAgeRelief, ageRelief);
    }

    @Override
    void handlePostingDetailPercent(Posting posting, PayrollType payrollType, double percent) {
        if (percent == 0) {
            return;
        }

        final double ageReliefFactor = employment.ageReliefFactor(posting.semester());
        // Am Kirchenfeld wurden früher Einzelbuchungen in Prozent inkl. AE erfasst
        percent = employment.withoutAgeRelief(posting.semester(), percent);
        final double ageRelief = percent * ageReliefFactor / 100.0;
        postings.addDetail(posting, payrollType, 0, percent, ageRelief);
    }

    @Override
    double poolPercent(SemesterEnum semester, double percent) {
        // Am Kirchenfeld wurden früher Pooleinträge inkl. AE erfasst
        return percent / (1.0 + employment.ageReliefFactor(semester) / 100);
    }

    private static String poolTitle(Employment employment) {
        return employment.ageReliefFactor(First) > 0 ? "Pensum: Pool (inkl. AE)" : "Pensum: Pool";
    }
}
