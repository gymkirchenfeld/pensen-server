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


public final class SubjectFixture {

    public static final Subject B = subject(1, "B", "Biologie", 1);
    public static final Subject BG = subject(2, "BG", "Bildnerisches Gestalten", 2);
    public static final Subject D = subject(4, "D", "Deutsch", 4);
    public static final Subject M = subject(11, "M", "Mathematik", 11);
    public static final Subject KS = subject(25, "KS", "Klassenstunde", 25);
    public static final Subject SFPP = subject(41, "SF PP", "SF Pädagogik/Psychologie", 41);
    public static final Subject EFPP = subject(70, "EF PP", "EF Pädagogik/Psychologie", 70);
    public static final Subject BFSAPY = subject(137, "BFSA PY", "Berufsfeld SA Psychologie", 137);
    public static final Subject BFSAPE = subject(143, "BFSA PE", "Berufsfeld SA PE", 143);


    private static Subject subject(int id, String code, String description, int sortOrder) {
        Subject result = new Subject(id);
        result.setCode(code);
        result.setDescription(description);
        result.setSortOrder(sortOrder);
        result.setArchived(false);
        result.setCrossClass(false);
        return result;
    }

    private SubjectFixture() {
    }
}
