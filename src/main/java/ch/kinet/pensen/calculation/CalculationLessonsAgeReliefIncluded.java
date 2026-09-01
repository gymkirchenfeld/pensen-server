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
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;

import java.util.stream.Stream;

public final class CalculationLessonsAgeReliefIncluded extends CalculationLessons {

    CalculationLessonsAgeReliefIncluded(Employment employment, Stream<PayrollType> payrollTypes) {
        super(employment, payrollTypes, AgeReliefIncludedSupport.poolTitle(employment));
    }

    @Override
    void calculatePayroll() {
        // Differenz zwischen Auszahlungsziel und tatsächlichem Pensum berechnen
        SemesterValue diff = employment.paymentTarget().map(
                (s, payment) -> payment - totalPercent.get(s)
        );
        // Differenz in vorgegebener Reihenfolge bei verschiedenen Teilanstellungen verbuchen
        payrollMap.types().forEachOrdered(type -> {
            SemesterValue percent = payrollMap.get(type).map((s, p) -> {
                // Berechne Prozentwert inklusive Altersentlastung
                double result = p;
                // Addiere die Differenz zwischen Auszahlung und Pensum
                result += diff.get(s);
                if (result < 0) {
                    // negatives Pensum kann nicht gemeldet werden, buche auf nächste Teilanstellung
                    diff.set(s, result);
                    result = 0;
                } else {
                    // Differenz konnte verbucht werden
                    diff.set(s, 0);
                }

                return result;
            });

            SemesterValue lessons = SemesterValue.create();
            if (type.isLessonBased()) {
                // aus Prozentwert wieder Lektionen berechnen (für Buchung in SAP)
                lessons = percent.map((s, p) -> percentToLessons(type, employment.withoutAgeRelief(s, p)));
                // Runde Lektionen auf zwei Dezimalstellen
                lessons = lessons.map((s, l) -> Math.round(l * 100) / 100.0);
                // Gerundete Lektionen wider in Prozent umrechnen
                percent = lessons.map((s, l) -> employment.withAgeRelief(s, lessonsToPercent(type, l)));
            }

            // Runde Prozente auf zwei Dezimalstellen
            percent = percent.map((s, l) -> Math.round(l * 100) / 100.0);
            payroll.add(type, lessons, percent);
        });

    }

    @Override
    double postingDetailInputPercent(SemesterEnum semester, double percent) {
        return AgeReliefIncludedSupport.postingDetailPercent(employment, semester, percent);
    }

    @Override
    double poolPercent(SemesterEnum semester, double percent) {
        return AgeReliefIncludedSupport.poolPercent(employment, semester, percent);
    }
}