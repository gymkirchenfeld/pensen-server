/*
 * Copyright (C) 2023 - 2024 by Stefan Rothe
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

import ch.kinet.reflect.PropertyInitializer;

public final class LessonType extends LookupValue {

    public enum Enum {
        none,
        noLessons,
        continuation,
        start,
        startOptional,
        startRepeating
    }

    public static Enum toEnum(LessonType object) {
        return object == null ? Enum.none : Enum.valueOf(object.getCode());
    }

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID})
    public LessonType(String code, String description, int id) {
        super(code, description, id);
    }
}
