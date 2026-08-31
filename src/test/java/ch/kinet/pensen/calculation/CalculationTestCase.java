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

import ch.kinet.pensen.calculation.CalculationScenario.Expectations;
import ch.kinet.pensen.calculation.CalculationScenario.RowExpectation;
import ch.kinet.pensen.data.CalculationMode;
import ch.kinet.pensen.job.Format;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class CalculationTestCase {

    private static final Map<CalculationMode.Enum, Integer> PERCENT_DECIMALS = Map.of(
            CalculationMode.Enum.lessons, 2,
            CalculationMode.Enum.lessons2, 2,
            CalculationMode.Enum.lessonsAgeReliefIncluded, 2,
            CalculationMode.Enum.lessons2AgeReliefIncluded, 2,
            CalculationMode.Enum.percent, 3,
            CalculationMode.Enum.percentAgeReliefIncluded, 3
    );

    private final CalculationScenario scenario;
    private final CalculationMode.Enum mode;
    private final Expectations expectations;

    CalculationTestCase(CalculationScenario scenario, CalculationMode.Enum mode, Expectations expectations) {
        this.scenario = scenario;
        this.mode = mode;
        this.expectations = expectations;
    }

    Workload calculate() {
        return scenario.calculate(mode);
    }

    void verify(Payroll payroll) {
        List<Payroll.DisplayItem> actual = payroll.displayItems().collect(Collectors.toList());
        List<RowExpectation> expected = expectations.rows;
        int decimals = payroll.percentDecimals();

        List<Executable> checks = new ArrayList<>();
        checks.add(() -> assertEquals(PERCENT_DECIMALS.get(mode), decimals,
                "Nachkommastellen der Prozentwerte"));
        checks.add(() -> assertEquals(descriptions(expected), actual.stream()
                        .map(Payroll.DisplayItem::description).collect(Collectors.toList()),
                "Zeilen der Pensenmeldung"));

        int rows = Math.min(expected.size(), actual.size());
        for (int i = 0; i < rows; i++) {
            int index = i;
            checks.add(() -> verifyRow(index, expected.get(index), actual.get(index), decimals));
        }

        checks.add(() -> verifyTotal(payroll, decimals));
        assertAll(toString(), checks);
    }

    @Override
    public String toString() {
        return scenario.name() + " / " + mode.name();
    }

    private void verifyRow(int index, RowExpectation expected, Payroll.DisplayItem actual, int decimals) {
        String label = "Zeile " + (index + 1) + " (" + expected.description + ")";
        assertAll(
                () -> assertEquals(lessons(expected.lessons1), lessons(actual.lessons().semester1()),
                        label + ": Lektionen 1. Semester"),
                () -> assertEquals(percent(expected.percent1, decimals), percent(actual.percent().semester1(), decimals),
                        label + ": Prozent 1. Semester"),
                () -> assertEquals(lessons(expected.lessons2), lessons(actual.lessons().semester2()),
                        label + ": Lektionen 2. Semester"),
                () -> assertEquals(percent(expected.percent2, decimals), percent(actual.percent().semester2(), decimals),
                        label + ": Prozent 2. Semester")
        );
    }

    private void verifyTotal(Payroll payroll, int decimals) {
        if (expectations.totalPercent1 == null) {
            return;
        }

        assertAll(
                () -> assertEquals(percent(expectations.totalPercent1, decimals),
                        percent(payroll.percent().semester1(), decimals),
                        "Total: Prozent 1. Semester"),
                () -> assertEquals(percent(expectations.totalPercent2, decimals),
                        percent(payroll.percent().semester2(), decimals),
                        "Total: Prozent 2. Semester")
        );
    }

    private static String lessons(double value) {
        return Format.lessons(value);
    }

    private static String percent(double value, int decimals) {
        return Format.percent(value, false, decimals);
    }

    private static List<String> descriptions(List<RowExpectation> rows) {
        return rows.stream().map(row -> row.description).collect(Collectors.toList());
    }
}
