/*
 * Copyright (C) 2022 by Sebastian Forster, Stefan Rothe
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

public enum SemesterEnum {

    First(1), Second(2);

    public static SemesterEnum parseId(int id) {
        for (SemesterEnum value : values()) {
            if (value.getId() == id) {
                return value;
            }
        }

        return null;
    }

    private final int id;

    private SemesterEnum(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
