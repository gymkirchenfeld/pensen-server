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
package ch.kinet.pensen.data;

import ch.kinet.reflect.PropertyInitializer;

public final class LessonType extends LookupValue {

    private static final String NO_LESSONS = "X";
    private static final String START = "A";
    private static final String START_OPTIONAL = "AO";
    private static final String START_REPEATING = "AW";
    private static final String CONTINUATION = "W";

    public enum Enum {
        None(null),
        NoLessons(NO_LESSONS),
        Continuation(CONTINUATION),
        Start(START),
        StartOptional(START_OPTIONAL),
        StartRepeating(START_REPEATING);

        private final String code;

        private Enum(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    public static Enum toEnum(LessonType object) {
        if (object == null) {
            return Enum.None;
        }

        switch (object.getCode()) {
            case CONTINUATION:
                return Enum.Continuation;
            case NO_LESSONS:
                return Enum.NoLessons;
            case START:
                return Enum.Start;
            case START_OPTIONAL:
                return Enum.StartOptional;
            case START_REPEATING:
                return Enum.StartRepeating;
            default:
                return Enum.None;
        }
    }

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID})
    public LessonType(String code, String description, int id) {
        super(code, description, id);
    }
}
