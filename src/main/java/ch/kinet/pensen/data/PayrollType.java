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
    public static final String DB_WEEKLY_LESSONS = "WeeklyLessons";
    public static final String JSON_SALDO_RESOLVING_ORDER = "saldoResolvingOrder";
    public static final String JSON_WEEKLY_LESSONS = "weeklyLessons";
    private final int saldoResolvingOrder;
    private final double weeklyLessons;

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID, DB_SALDO_RESOLVING_ORDER, DB_WEEKLY_LESSONS})
    public PayrollType(String code, String description, int id, int saldoResolvingOrder, double weeklyLessons) {
        super(code, description, id);
        this.saldoResolvingOrder = saldoResolvingOrder;
        this.weeklyLessons = weeklyLessons;
    }

    public int getSaldoResolvingOrder() {
        return saldoResolvingOrder;
    }

    public double getWeeklyLessons() {
        return weeklyLessons;
    }

    public boolean lessonBased() {
        return weeklyLessons != 0;
    }

    public double lessonsToPercent(double lessons) {
        if (weeklyLessons == 0) {
            return 0;
        }

        return lessons * 100 / weeklyLessons;
    }

    public double percentToLessons(double percent) {
        if (weeklyLessons == 0) {
            return 0;
        }

        return percent * weeklyLessons / 100;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_SALDO_RESOLVING_ORDER, saldoResolvingOrder);
        result.put(JSON_WEEKLY_LESSONS, weeklyLessons);
        return result;
    }
}
