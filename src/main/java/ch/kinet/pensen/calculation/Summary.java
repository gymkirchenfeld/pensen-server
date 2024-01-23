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
import ch.kinet.Util;

public final class Summary extends ItemList<Summary.Item> {

    static Summary create(double ageReliefFactor1, double ageReliefFactor2) {
        return new Summary(ageReliefFactor1, ageReliefFactor2);
    }

    private static final String JSON_AGE_RELIEF1 = "ageRelief1";
    private static final String JSON_AGE_RELIEF2 = "ageRelief2";
    private static final String JSON_AGE_RELIEF_FACTOR1 = "ageReliefFactor1";
    private static final String JSON_AGE_RELIEF_FACTOR2 = "ageReliefFactor2";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_PERCENT1 = "percent1";
    private static final String JSON_PERCENT2 = "percent2";
    private static final String JSON_PERCENT_WITH_AGE_RELIEF = "percentWithAgeRelief";
    private static final String JSON_TOTAL = "total";

    private final double ageReliefFactor1;
    private final double ageReliefFactor2;
    private final Item total = new Item(Integer.MAX_VALUE, "Total", 0.0, 0.0);
    private int nextId = 1;

    private Summary(double ageReliefFactor1, double ageReliefFactor2) {
        this.ageReliefFactor1 = ageReliefFactor1;
        this.ageReliefFactor2 = ageReliefFactor2;
    }

    public Item total() {
        return total;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_AGE_RELIEF_FACTOR1, ageReliefFactor1);
        result.put(JSON_AGE_RELIEF_FACTOR2, ageReliefFactor2);
        result.putTerse(JSON_TOTAL, total);
        return result;
    }

    void add(String description, double percent1, double percent2) {
        add(new Item(nextId, description, percent1, percent2));
        nextId += 1;
        total.percent1 += percent1;
        total.percent2 += percent2;

    }

    public final class Item implements Comparable<Item>, Json {

        private final int id;
        private final String description;
        private double percent1;
        private double percent2;

        public Item(int id, String description, double percent1, double percent2) {
            this.id = id;
            this.description = description;
            this.percent1 = percent1;
            this.percent2 = percent2;
        }

        @Override
        public int compareTo(Item other) {
            return Util.compare(id, other.id);
        }

        public double ageRelief1() {
            return percent1 * ageReliefFactor1 / 100;
        }

        public double ageRelief2() {
            return percent2 * ageReliefFactor2 / 100;
        }

        public String description() {
            return description;
        }

        public double percent1() {
            return percent1;
        }

        public double percent2() {
            return percent2;
        }

        public double percentWithAgeRelief() {
            return (percent1 + ageRelief1() + percent2 + ageRelief2()) / 2.0;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_DESCRIPTION, description);
            result.put(JSON_PERCENT1, percent1);
            result.put(JSON_PERCENT2, percent2);
            result.put(JSON_AGE_RELIEF1, ageRelief1());
            result.put(JSON_AGE_RELIEF2, ageRelief2());
            result.put(JSON_PERCENT_WITH_AGE_RELIEF, percentWithAgeRelief());
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }
    }
}
