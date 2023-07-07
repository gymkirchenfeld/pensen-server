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

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.Posting;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;

public final class CalculationPercent extends Calculation {

    private final PayrollMap payrollMap = PayrollMap.create();
    private final SemesterValue totalPercent = SemesterValue.create();

    CalculationPercent(Employment employment) {
        super(employment, "Pensum: Pool");
    }

    @Override
    void addToPayroll(PayrollType type, SemesterEnum semester, double value) {
        // Intern werden alle Berechnungen in Prozent durchgeführt
        if (type.lessonBased()) {
            value = type.lessonsToPercent(value);
        }

        // Intern werden alle Berechnungen inklusive Altersentlastung durchgeführt
        value = employment.withAgeRelief(semester, value);
        payrollMap.add(type, semester, value);
        totalPercent.add(semester, value);
    }

    @Override
    double calculatePayment() {
        return payroll.percent().mean();
    }

    @Override
    void calculatePayroll() {
        // Differenz zwischen Auszahlungsziel und tatsächlichem Pensum berechnen
        SemesterValue diff = employment.paymentTarget().map(
            (s, payment) -> payment - totalPercent.get(s)
        );
        // Differenz in vorgegebener Reihenfolge bei verschiedenen Teilanstellungen verbuchen
        payrollMap.types().sorted(SALDO_RESOLVING_ORDER).forEachOrdered(type -> {
            SemesterValue percent = payrollMap.get(type).map((s, p) -> {
                // Berechne Prozentwert inklusive Altersentlastung
                double result = p;
                // Addiere die Differenz zwischen Auszahlung und Pensum
                result += diff.get(s);
                if (result < 0) {
                    // negatives Pensum kann nicht gemeldet werden, buche auf nächste Teilanstellung
                    diff.set(s, result);
                    result = 0;
                }
                else {
                    // Differenz konnte verbucht werden
                    diff.set(s, 0);
                }

                return result;
            });

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
        final double ageRelief = percent * ageReliefFactor / 100.0;
        postings.addDetail(posting, payrollType, 0, percent, ageRelief);
    }

    @Override
    double poolPercent(SemesterEnum semester, double percent) {
        return percent;
    }
}
