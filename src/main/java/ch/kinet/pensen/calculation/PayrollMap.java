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

import ch.kinet.Util;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

final class PayrollMap {

    static final Comparator<PayrollType> SALDO_RESOLVING_ORDER =
        (PayrollType o1, PayrollType o2) -> Util.compare(o1.getSaldoResolvingOrder(), o2.getSaldoResolvingOrder());

    static PayrollMap create(Stream<PayrollType> payrollTypes) {
        return new PayrollMap(payrollTypes);
    }

    private final Map<PayrollType, SemesterValue> payrollMap = new HashMap<>();
    private final SortedSet<PayrollType> payrollTypes = new TreeSet<>();

    private PayrollMap(Stream<PayrollType> payrollTypes) {
        Optional<PayrollType> defaultType = payrollTypes.sorted(SALDO_RESOLVING_ORDER).findFirst();
        if (defaultType.isPresent()) {
            // Die Teilanstellung GYM2-4 muss zwingend vorhanden sein, um die Differenz buchen zu können.
            ensureType(defaultType.get());
        }
    }

    void add(PayrollType type, SemesterEnum semester, double value) {
        ensureType(type);
        payrollMap.get(type).add(semester, value);
    }

    void ensureType(PayrollType type) {
        payrollTypes.add(type);
        if (!payrollMap.containsKey(type)) {
            payrollMap.put(type, SemesterValue.create());
        }
    }

    SemesterValue get(PayrollType type) {
        return SemesterValue.create(get(SemesterEnum.First, type), get(SemesterEnum.Second, type));
    }

    double get(SemesterEnum semester, PayrollType type) {
        if (!payrollMap.containsKey(type)) {
            return 0.0;
        }

        return payrollMap.get(type).get(semester);
    }

    Stream<PayrollType> types() {
        return payrollTypes.stream().sorted(SALDO_RESOLVING_ORDER);
    }
}
