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
package ch.kinet.pensen.calculation;

import ch.kinet.Json;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterValue;
import java.util.HashMap;
import java.util.Map;

public final class Payroll extends ItemList<Payroll.Item> implements Json {

    static Payroll create(int percentDecimals) {
        return new Payroll(percentDecimals);
    }

    private static final String JSON_LESSONS1 = "lessons1";
    private static final String JSON_LESSONS2 = "lessons2";
    private static final String JSON_PAYROLL_TYPE = "payrollType";
    private static final String JSON_PERCENT1 = "percent1";
    private static final String JSON_PERCENT2 = "percent2";
    private static final String JSON_PERCENT_DECIMALS = "percentDecimals";
    private static final String JSON_TOTAL = "total";

    private final Map<PayrollType, Item> itemMap = new HashMap<>();
    private final SemesterValue totalPercent = SemesterValue.create();
    private final int percentDecimals;

    private Payroll(int percentDecimals) {
        this.percentDecimals = percentDecimals;
    }

    public Item getItem(PayrollType type) {
        return itemMap.get(type);
    }

    public SemesterValue percent() {
        return SemesterValue.copy(totalPercent);
    }

    public int percentDecimals() {
        return percentDecimals;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject total = JsonObject.create();
        total.put(JSON_PERCENT1, totalPercent.semester1());
        total.put(JSON_PERCENT2, totalPercent.semester2());
        JsonObject result = super.toJsonTerse();
        result.put(JSON_PERCENT_DECIMALS, percentDecimals);
        result.put(JSON_TOTAL, total);
        return result;
    }

    void add(PayrollType type, SemesterValue lessons, SemesterValue percent) {
        Item item = itemMap.get(type);
        if (item == null) {
            item = new Item(type);
            add(item);
            itemMap.put(type, item);
        }

        item.lessons.add(lessons);
        item.percent.add(percent);
        this.totalPercent.add(percent);
    }

    public static final class Item implements Comparable<Item>, Json {

        private final PayrollType type;
        private final SemesterValue lessons = SemesterValue.create();
        private final SemesterValue percent = SemesterValue.create();

        public Item(PayrollType type) {
            this.type = type;
        }

        @Override
        public int compareTo(Item other) {
            return type.compareTo(other.type);
        }

        public String description() {
            return type.getDescription();
        }

        public SemesterValue lessons() {
            return SemesterValue.copy(lessons);
        }

        public SemesterValue percent() {
            return SemesterValue.copy(percent);
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.putTerse(JSON_PAYROLL_TYPE, type);
            result.put(JSON_LESSONS1, lessons.semester1());
            result.put(JSON_PERCENT1, percent.semester1());
            result.put(JSON_LESSONS2, lessons.semester2());
            result.put(JSON_PERCENT2, percent.semester2());
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }

        public PayrollType type() {
            return type;
        }
    }
}
