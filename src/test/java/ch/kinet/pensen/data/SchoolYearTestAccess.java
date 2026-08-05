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

/**
 * Gibt Tests Zugriff auf paketprivate Methoden von SchoolYear. Die Wochenlektionen werden im
 * Produktivcode ausschliesslich beim Laden aus der Datenbank gesetzt, für Tests brauchen wir
 * sie aber ohne Datenbank.
 */
public final class SchoolYearTestAccess {

    public static void putWeeklyLessons(SchoolYear schoolYear, PayrollType payrollType, double lessons) {
        schoolYear.putWeeklyLessons(payrollType, lessons);
    }

    private SchoolYearTestAccess() {
    }
}