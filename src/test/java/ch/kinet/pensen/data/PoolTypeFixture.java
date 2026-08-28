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

import java.util.stream.Stream;

public final class PoolTypeFixture {

    public static final PoolType S = new PoolType(true, "S", "Schulpool", 1, PayrollTypeFixture.PS);
    public static final PoolType L = new PoolType(true, "L", "Schulleitung", 2, PayrollTypeFixture.PL);
    public static final PoolType U = new PoolType(true, "U", "Unterricht", 3, PayrollTypeFixture.G2);
    public static final PoolType I = new PoolType(true, "I", "IT-Betreuung", 4, PayrollTypeFixture.PS);
    public static final PoolType Z = new PoolType(false, "Z", "Spezialfinanzierung", 5, PayrollTypeFixture.PX);

    public static Stream<PoolType> stream() {
        return Stream.of(S, L, U, I, Z);
    }

    private PoolTypeFixture() {
    }
}
