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
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.SemesterValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class Payroll extends ItemList<Payroll.Item> implements Json {

    static Payroll create(int percentDecimals) {
        return new Payroll(percentDecimals);
    }

    private static final String JSON_CORRECTION = "correction";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_ITEMS = "items";
    private static final String JSON_LESSONS1 = "lessons1";
    private static final String JSON_LESSONS2 = "lessons2";
    private static final String JSON_PARENT_PAYROLL_TYPE = "parentPayrollType";
    private static final String JSON_PAYROLL_TYPE = "payrollType";
    private static final String JSON_PERCENT1 = "percent1";
    private static final String JSON_PERCENT2 = "percent2";
    private static final String JSON_PERCENT_DECIMALS = "percentDecimals";
    private static final String JSON_TOTAL = "total";

    private final Map<PayrollType, Item> itemMap = new HashMap<>();
    private Map<SemesterEnum, IpbCorrectionData> ipbCorrectionDataPerSemester = new HashMap<>();
    private final SemesterValue totalPercent = SemesterValue.create();
    private final int percentDecimals;

    private Payroll(int percentDecimals) {
        this.percentDecimals = percentDecimals;
    }

    public Stream<DisplayItem> displayItems() {
        IpbCorrectionData c1 = ipbCorrectionDataPerSemester.get(SemesterEnum.First);
        IpbCorrectionData c2 = ipbCorrectionDataPerSemester.get(SemesterEnum.Second);

        if (c1 == null && c2 == null) {
            return items().map(item -> new RegularDisplayItem(item, item.lessons(), item.percent()));
        }

        PayrollType parentType1 = c1 != null ? c1.type() : null;
        PayrollType parentType2 = c2 != null ? c2.type() : null;

        List<DisplayItem> result = new ArrayList<>();

        items().forEachOrdered(item -> {
            PayrollType type = item.type();
            boolean isParentType1 = type.equals(parentType1);
            boolean isParentType2 = type.equals(parentType2);

            double l1 = isParentType1 ? c1.lessonsWithoutCorrection() : item.lessons().semester1();
            double l2 = isParentType2 ? c2.lessonsWithoutCorrection() : item.lessons().semester2();
            double p1 = isParentType1 ? c1.percentWithoutCorrection() : item.percent().semester1();
            double p2 = isParentType2 ? c2.percentWithoutCorrection() : item.percent().semester2();
            result.add(new RegularDisplayItem(item, SemesterValue.create(l1, l2), SemesterValue.create(p1, p2)));

            if (isParentType1 || isParentType2) {
                String desc = "IPB-Korrektur " + type.getDescription();
                if (isParentType1 && isParentType2) {
                    result.add(new CorrectionDisplayItem(
                            type, desc,
                            SemesterValue.create(-c1.ipbCorrectionLessons(), -c2.ipbCorrectionLessons()),
                            SemesterValue.create(-c1.ipbCorrectionPercent(), -c2.ipbCorrectionPercent())
                    ));
                } else if (isParentType1) {
                    result.add(new CorrectionDisplayItem(
                            type, desc,
                            SemesterValue.create(-c1.ipbCorrectionLessons(), 0.0),
                            SemesterValue.create(-c1.ipbCorrectionPercent(), 0.0)
                    ));
                } else {
                    result.add(new CorrectionDisplayItem(
                            type, desc,
                            SemesterValue.create(0.0, -c2.ipbCorrectionLessons()),
                            SemesterValue.create(0.0, -c2.ipbCorrectionPercent())
                    ));
                }
            }
        });

        return result.stream();
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
        JsonObject result = JsonObject.create();
        result.put(JSON_ITEMS, JsonArray.createVerbose(displayItems()));
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

    public IpbCorrectionData getIpbCorrection(SemesterEnum semester) {
        return ipbCorrectionDataPerSemester.get(semester);
    }

    public boolean hasIpbCorrection() {
        return !ipbCorrectionDataPerSemester.isEmpty();
    }

    void setIpbCorrectionDataPerSemester(Map<SemesterEnum, IpbCorrectionData> ipbCorrectionDataPerSemester) {
        this.ipbCorrectionDataPerSemester = ipbCorrectionDataPerSemester;
    }

    public abstract static class DisplayItem implements Json {

        public abstract String description();

        public abstract SemesterValue lessons();

        public abstract SemesterValue percent();

        @Override
        public JsonObject toJsonVerbose() {
            return toJsonTerse();
        }
    }

    private static final class RegularDisplayItem extends DisplayItem {

        private final Item item;
        private final SemesterValue lessons;
        private final SemesterValue percent;

        RegularDisplayItem(Item item, SemesterValue lessons, SemesterValue percent) {
            this.item = item;
            this.lessons = lessons;
            this.percent = percent;
        }

        @Override
        public String description() {
            return item.description();
        }

        @Override
        public SemesterValue lessons() {
            return lessons;
        }

        @Override
        public SemesterValue percent() {
            return percent;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.putTerse(JSON_PAYROLL_TYPE, item.type());
            result.put(JSON_DESCRIPTION, item.description());
            result.put(JSON_LESSONS1, lessons.semester1());
            result.put(JSON_PERCENT1, percent.semester1());
            result.put(JSON_LESSONS2, lessons.semester2());
            result.put(JSON_PERCENT2, percent.semester2());
            result.put(JSON_CORRECTION, false);
            return result;
        }
    }

    private static final class CorrectionDisplayItem extends DisplayItem {

        private final PayrollType parentPayrollType;
        private final String description;
        private final SemesterValue lessons;
        private final SemesterValue percent;

        CorrectionDisplayItem(PayrollType parentPayrollType, String description,
                              SemesterValue lessons, SemesterValue percent) {
            this.parentPayrollType = parentPayrollType;
            this.description = description;
            this.lessons = lessons;
            this.percent = percent;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public SemesterValue lessons() {
            return lessons;
        }

        @Override
        public SemesterValue percent() {
            return percent;
        }

        @Override
        public JsonObject toJsonTerse() {
            JsonObject result = JsonObject.create();
            result.putTerse(JSON_PARENT_PAYROLL_TYPE, parentPayrollType);
            result.put(JSON_DESCRIPTION, description);
            result.put(JSON_LESSONS1, lessons.semester1());
            result.put(JSON_PERCENT1, percent.semester1());
            result.put(JSON_LESSONS2, lessons.semester2());
            result.put(JSON_PERCENT2, percent.semester2());
            result.put(JSON_CORRECTION, true);
            return result;
        }
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

    public static final class IpbCorrectionData {
        private static final String JSON_IPB_CORRECTION_PAYROLL_TYPE = "ipbCorrectionPayrollType";
        private static final String JSON_IPB_CORRECTION_LESSONS = "ipbCorrectionLessons";
        private static final String JSON_IPB_CORRECTION_PERCENT = "ipbCorrectionPercent";
        private static final String JSON_LESSONS_WITHOUT_CORRECTION = "lessonsWithoutCorrection";
        private static final String JSON_PERCENT_WITHOUT_CORRECTION = "percentWithoutCorrection";

        private final PayrollType payrollType;
        private final double ipbCorrectionLessons;
        private final double ipbCorrectionPercent;
        private final double lessonsWithoutCorrection;
        private final double percentWithoutCorrection;

        public IpbCorrectionData(PayrollType payrollType,
                                 double ipbCorrectionLessons, double ipbCorrectionPercent,
                                 double lessonsWithoutCorrection, double percentWithoutCorrection) {
            this.payrollType = payrollType;
            this.ipbCorrectionLessons = ipbCorrectionLessons;
            this.ipbCorrectionPercent = ipbCorrectionPercent;
            this.lessonsWithoutCorrection = lessonsWithoutCorrection;
            this.percentWithoutCorrection = percentWithoutCorrection;
        }

        public PayrollType type() {
            return payrollType;
        }

        public double ipbCorrectionLessons() {
            return ipbCorrectionLessons;
        }

        public double ipbCorrectionPercent() {
            return ipbCorrectionPercent;
        }

        public double lessonsWithoutCorrection() {
            return lessonsWithoutCorrection;
        }

        public double percentWithoutCorrection() {
            return percentWithoutCorrection;
        }
    }
}