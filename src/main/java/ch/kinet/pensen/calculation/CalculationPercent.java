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
import ch.kinet.pensen.data.SemesterValue;
import java.util.Comparator;

public final class CalculationPercent extends Calculation {

    private static final Comparator<PayrollType> SALDO_RESOLVING_ORDER =
        (PayrollType o1, PayrollType o2) -> Util.compare(o1.getSaldoResolvingOrder(), o2.getSaldoResolvingOrder());
    private final PayrollMap payrollMap = PayrollMap.create();
    private final SemesterValue totalPercent = SemesterValue.create();

    CalculationPercent(Employment employment) {
        super(employment, "Pensum: Pool");
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
        return payroll.percent().mean();
    }

    @Override
    void calculatePayroll() {
        // Differenz zwischen Auszahlung und tatsächlichem Pensum berechnen
        SemesterValue diff = employment.payment().map(
            (s, payment) -> payment - employment.withAgeRelief(s, totalPercent.get(s))
        );

        payrollMap.types().sorted(SALDO_RESOLVING_ORDER).forEachOrdered(type -> {
            SemesterValue percent = payrollMap.get(type).map((s, p) -> {
                // Berechne Prozentwert inklusive Altersentlastung
                double result = employment.withAgeRelief(s, p);
                // Addiere die Differenz zwischen Auszahlung und Pensum
                result += diff.get(s);
                if (result < 0) {
                    // negatives Pensum kann nicht gemeldet werden, buche auf nächste Teilanstellung
                    diff.set(s, -result);
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
