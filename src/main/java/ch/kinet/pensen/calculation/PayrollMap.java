/*
 * Copyright (C) 2023 by Stefan Rothe
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
import ch.kinet.pensen.data.SemesterValue;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

final class PayrollMap {

    static PayrollMap create() {
        return new PayrollMap();
    }

    private final Map<SemesterEnum, Map<PayrollType, Double>> payrollMap = new HashMap<>();
    private final SortedSet<PayrollType> payrollTypes = new TreeSet<>();

    private PayrollMap() {
        for (SemesterEnum semester : SemesterEnum.values()) {
            payrollMap.put(semester, new HashMap<>());
        }
    }

    void add(PayrollType type, SemesterEnum semester, double value) {
        payrollTypes.add(type);
        Map<PayrollType, Double> semesterMap = payrollMap.get(semester);
        if (semesterMap.containsKey(type)) {
            value += semesterMap.get(type);
        }

        semesterMap.put(type, value);
    }

    SemesterValue get(PayrollType type) {
        return SemesterValue.create(get(SemesterEnum.First, type), get(SemesterEnum.Second, type));
    }

    double get(SemesterEnum semester, PayrollType type) {
        Map<PayrollType, Double> semesterMap = payrollMap.get(semester);
        return semesterMap.containsKey(type) ? semesterMap.get(type) : 0.0;
    }

    Stream<PayrollType> types() {
        return payrollTypes.stream();
    }
}
