/*
 * Copyright (C) 2023 by Sebastian Forster, Stefan Rothe
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

public final class CalculationMode extends LookupValue {

    private static final String LESSONS = "L";
    private static final String LESSONS_AGE_RELIEF_INCLUDED = "LAI";
    private static final String PERCENT_AGE_RELIEF_INCLUDED = "PAI";
    private static final String PERCENT = "P";

    public enum Enum {
        None(null),
        Lessons(LESSONS),
        LessonsAgeReliefIncluded(LESSONS_AGE_RELIEF_INCLUDED),
        PercentAgeReliefIncluded(PERCENT_AGE_RELIEF_INCLUDED),
        Percent(PERCENT);

        private final String code;

        private Enum(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    public static Enum toEnum(CalculationMode object) {
        if (object == null) {
            return Enum.None;
        }

        switch (object.getCode()) {
            case LESSONS:
                return Enum.Lessons;
            case LESSONS_AGE_RELIEF_INCLUDED:
                return Enum.LessonsAgeReliefIncluded;
            case PERCENT_AGE_RELIEF_INCLUDED:
                return Enum.PercentAgeReliefIncluded;
            case PERCENT:
                return Enum.Percent;
            default:
                return Enum.None;
        }
    }

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID})
    public CalculationMode(String code, String description, int id) {
        super(code, description, id);
    }
}
