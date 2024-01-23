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
import ch.kinet.pensen.data.PoolType;
import ch.kinet.pensen.data.SemesterValue;

public final class Pool extends ItemList<Pool.Item> {

    static Pool create(String title) {
        return new Pool(title);
    }

    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_PERCENT1 = "percent1";
    private static final String JSON_PERCENT2 = "percent2";
    private static final String JSON_TITLE = "title";
    private static final String JSON_TOTAL = "total";
    private static final String JSON_TYPE = "type";

    private final String title;
    private final SemesterValue totalPercent = SemesterValue.create();

    private Pool(String title) {
        this.title = title;
    }

    public SemesterValue percent() {
        return SemesterValue.copy(totalPercent);
    }

    public String title() {
        return title;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject jsonTotal = JsonObject.create();
        jsonTotal.put(JSON_DESCRIPTION, "Total");
        jsonTotal.put(JSON_PERCENT1, totalPercent.semester1());
        jsonTotal.put(JSON_PERCENT2, totalPercent.semester2());

        JsonObject result = super.toJsonTerse();
        result.put(JSON_TITLE, title);
        result.putTerse(JSON_TOTAL, jsonTotal);
        return result;
    }

    void addItem(String description, PoolType type, SemesterValue percent) {
        add(new Item(description, type, percent));
        totalPercent.add(percent);
    }

    public static final class Item implements Comparable<Item>, Json {

        private final String description;
        private final PoolType type;
        private final SemesterValue percent;

        private Item(String description, PoolType type, SemesterValue percent) {
            this.description = description;
            this.type = type;
            this.percent = SemesterValue.copy(percent);
        }

        @Override
        public int compareTo(Item other) {
            return type.compareTo(other.type);
        }

        public String description() {
            return description;
        }

        public double percent1() {
            return percent.semester1();
        }

        public double percent2() {
            return percent.semester2();
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_DESCRIPTION, description);
            result.put(JSON_PERCENT1, percent.semester1());
            result.put(JSON_PERCENT2, percent.semester2());
            if (type != null) {
                result.putTerse(JSON_TYPE, type);
            }

            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }

        public PoolType type() {
            return type;
        }
    }
}
