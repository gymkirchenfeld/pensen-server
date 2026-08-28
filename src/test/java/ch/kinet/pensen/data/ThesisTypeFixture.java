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


public final class ThesisTypeFixture {

    public static final ThesisType MA = new ThesisType("MA", "GYM/BME Maturaarbeit", 1, PayrollTypeFixture.G2, 1);
    public static final ThesisType MZ = new ThesisType("MZ", "GYM/BME Maturaarbeit Zweitbeurteilung", 2, PayrollTypeFixture.G2, 0.5);
    public static final ThesisType SA = new ThesisType("SA", "FMS Abschlussarbeit", 3, PayrollTypeFixture.F, 1);
    public static final ThesisType FMA = new ThesisType("FM", "FMS Fachmaturaarbeit", 4, PayrollTypeFixture.F, 1);

    private ThesisTypeFixture() {
    }
}
