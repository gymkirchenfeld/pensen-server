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

import java.util.Map;
import java.util.stream.Stream;

public final class PayrollTypeFixture {

    public static final PayrollType G2 = new PayrollType("G2", "Unterricht GYM2-4", 2, true, 1, true);
    public static final PayrollType G1 = new PayrollType("G1", "Unterricht GYM1", 1, true, 2, false);
    public static final PayrollType F = new PayrollType("F", "Fachmittelschule", 3, true, 3, true);
    public static final PayrollType BM = new PayrollType("BM", "BME Matur", 4, true, 4, true);
    public static final PayrollType BP = new PayrollType("BP", "BME Passerelle", 5, true, 5, true);

    public static final PayrollType GK2 = new PayrollType("GK2", "Klassenlehrkraft GYM2-4", 7, true, 51, false);
    public static final PayrollType GK1 = new PayrollType("GK1", "Klassenlehrkraft GYM1", 6, true, 52, false);
    public static final PayrollType FK = new PayrollType("FK", "Klassenlehrkraft FMS", 11, true, 53, false);

    public static final PayrollType PS = new PayrollType("PS", "Pool für Spezialaufgaben", 8, false, 101, false);
    public static final PayrollType PL = new PayrollType("PL", "Pool für Schulleitungsaufgaben", 9, false, 102, false);
    public static final PayrollType PX = new PayrollType("PX", "Sonderpool", 10, false, 103, false);


    public static final Map<PayrollType, Double> WEEKLY_LESSONS = Map.ofEntries(
            Map.entry(G2, 23.0),
            Map.entry(G1, 28.0),
            Map.entry(F, 24.0),
            Map.entry(BM, 23.0),
            Map.entry(BP, 23.0),
            Map.entry(GK2, 20.0),
            Map.entry(GK1, 20.0),
            Map.entry(FK, 20.0)
    );

    public static Stream<PayrollType> stream() {
        return Stream.of(G2, G1, F, BM, BP, GK2, GK1, FK, PS, PL, PX);
    }

    public static void putWeeklyLessons(SchoolYear schoolYear) {
        WEEKLY_LESSONS.forEach(schoolYear::putWeeklyLessons);
    }

    private PayrollTypeFixture() {
    }
}
