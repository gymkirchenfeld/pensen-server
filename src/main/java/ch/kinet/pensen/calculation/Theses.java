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
package ch.kinet.pensen.calculation;

import ch.kinet.Json;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.ThesisType;

public final class Theses extends ItemList<Theses.Item> {

    static Theses create() {
        return new Theses();
    }

    private static final String JSON_COUNT = "count";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_PERCENT = "percent";
    private static final String JSON_PERCENT_EACH = "percentEach";
    private static final String JSON_TOTAL = "total";

    private double percent;

    private Theses() {
    }

    public double percent() {
        return percent;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject total = JsonObject.create();
        total.put(JSON_DESCRIPTION, "Total");
        total.put(JSON_PERCENT, percent);

        JsonObject result = super.toJsonTerse();
        result.put(JSON_TOTAL, total);
        return result;
    }

    void addItem(ThesisType type, double count, double percent) {
        Item item = new Item(type, count, percent);
        this.percent += percent;
        add(item);
    }

    public static final class Item implements Json {

        private final double count;
        private final double percent;
        private final ThesisType type;

        private Item(ThesisType type, double count, double percent) {
            this.count = count;
            this.percent = percent;
            this.type = type;
        }

        public double count() {
            return count;
        }

        public double percent() {
            return percent;
        }

        public double percentEach() {
            return type.getPercent();
        }

        public ThesisType type() {
            return type;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_COUNT, count);
            result.put(JSON_DESCRIPTION, type.getDescription());
            result.put(JSON_PERCENT, percent);
            result.put(JSON_PERCENT_EACH, type.getPercent());
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }
    }
}
