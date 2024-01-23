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

import ch.kinet.Date;
import ch.kinet.Json;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.Posting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Postings extends ItemList<Postings.Item> {

    static Postings create() {
        return new Postings();
    }

    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_END_DATE = "endDate";
    private static final String JSON_LESSONS = "lessons";
    private static final String JSON_PAYROLL_TYPE = "payrollType";
    private static final String JSON_PERCENT = "percent";
    private static final String JSON_START_DATE = "startDate";
    private static final String JSON_TOTAL = "total";
    private static final String JSON_TOTAL_PERCENT = "totalPercent";

    private final Map<Posting, Item> itemMap = new HashMap<>();

    private Postings() {
    }

    public void addItem(Posting posting) {
        final Item item = new Item(posting);
        itemMap.put(posting, item);
        add(item);
    }

    public void addDetail(Posting posting, PayrollType type, double lessons, double percent, double ageRelief, double weeklyLessons) {
        Item item = itemMap.get(posting);
        item.add(type, lessons, percent, ageRelief, weeklyLessons);
    }

    public double totalPercent() {
        return items().collect(Collectors.summingDouble(item -> item.totalPercent()));
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject total = JsonObject.create();
        total.put(JSON_PERCENT, totalPercent());

        JsonObject result = super.toJsonTerse();
        result.put(JSON_TOTAL, total);
        return result;
    }

    public static final class Item implements Comparable<Item>, Json {

        private final List<Detail> details = new ArrayList<>();
        private final Posting posting;
        private double totalPercent;

        private Item(Posting posting) {
            this.posting = posting;
        }

        @Override
        public int compareTo(Item other) {
            return posting.compareTo(other.posting);
        }

        public String description() {
            return posting.getDescription();
        }

        public Date endDate() {
            return posting.getEndDate();
        }

        public Date startDate() {
            return posting.getStartDate();
        }

        public Stream<Detail> streamDetails() {
            return details.stream();
        }

        public double totalPercent() {
            return totalPercent;
        }

        void add(PayrollType payrollType, double lessons, double percent, double ageRelief, double weeklyLessons) {
            details.add(new Detail(payrollType, lessons, percent, ageRelief, weeklyLessons));
            totalPercent += percent + ageRelief;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.put(JSON_DESCRIPTION, posting.getDescription());
            result.put(JSON_END_DATE, posting.getEndDate());
            result.put(JSON_START_DATE, posting.getStartDate());
            result.put(JSON_TOTAL_PERCENT, totalPercent);
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }

    }

    public static final class Detail implements Json {

        private final PayrollType payrollType;
        private final double ageRelief;
        private final double lessons;
        private final double percent;
        private final double weeklyLessons;

        private Detail(PayrollType payrollType, double lessons, double percent, double ageRelief, double weeklyLessons) {
            this.payrollType = payrollType;
            this.lessons = lessons;
            this.percent = percent;
            this.ageRelief = ageRelief;
            this.weeklyLessons = weeklyLessons;
        }

        public double ageRelief() {
            return ageRelief;
        }

        public double lessons() {
            return lessons;
        }

        public PayrollType payrollType() {
            return payrollType;
        }

        public double percentWithAgeRelief() {
            return percent + ageRelief;
        }

        public double percentWithoutAgeRelief() {
            return percent;
        }

        public double weeklyLessons() {
            return weeklyLessons;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.putTerse(JSON_PAYROLL_TYPE, payrollType);
            result.put(JSON_LESSONS, lessons);
            result.put(JSON_PERCENT, percent);
            return result;
        }

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }
    }
}
