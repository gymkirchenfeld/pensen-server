/*
 * Copyright (C) 2022 - 2023 by Stefan Rothe
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

import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;

public final class PayrollType extends LookupValue {

    public static final String DB_SALDO_RESOLVING_ORDER = "SaldoResolvingOrder";
    public static final String DB_LESSON_BASED = "LessonBased";
    public static final String JSON_SALDO_RESOLVING_ORDER = "saldoResolvingOrder";
    public static final String JSON_LESSON_BASED = "lessonBased";
    private final boolean lessonBased;
    private final int saldoResolvingOrder;

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID, DB_LESSON_BASED, DB_SALDO_RESOLVING_ORDER})
    public PayrollType(String code, String description, int id, boolean lessonBased, int saldoResolvingOrder) {
        super(code, description, id);
        this.lessonBased = lessonBased;
        this.saldoResolvingOrder = saldoResolvingOrder;
    }

    public int getSaldoResolvingOrder() {
        return saldoResolvingOrder;
    }

    public boolean isLessonBased() {
        return lessonBased;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_LESSON_BASED, lessonBased);
        result.put(JSON_SALDO_RESOLVING_ORDER, saldoResolvingOrder);
        return result;
    }
}
