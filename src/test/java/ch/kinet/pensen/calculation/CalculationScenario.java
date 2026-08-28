/*
 * Copyright (C) 2026 by Johannes Lieberherr (ttools gmbh)
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

import ch.kinet.pensen.data.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class CalculationScenario {

    static final String IPB_CORRECTION_PREFIX = "IPB-Korrektur ";

    private static final int DEFAULT_GRADUATION_YEAR = 2027;
    private static final int DEFAULT_WEEKS = 38;

    static Builder create(String name) {
        return new Builder(name);
    }

    private final String name;
    private final int graduationYear;
    private final int weeks;
    private final LocalDate birthday;
    private final double payment1;
    private final double payment2;
    private final List<CourseSpec> courses;
    private final List<PoolSpec> poolEntries;
    private final List<ThesisSpec> thesisEntries;

    private CalculationScenario(Builder builder) {
        name = builder.name;
        graduationYear = builder.graduationYear;
        weeks = builder.weeks;
        birthday = builder.birthday;
        payment1 = builder.payment1;
        payment2 = builder.payment2;
        courses = new ArrayList<>(builder.courses);
        poolEntries = new ArrayList<>(builder.poolEntries);
        thesisEntries = new ArrayList<>(builder.thesisEntries);
    }

    String name() {
        return name;
    }

    Workload calculate(CalculationMode.Enum mode) {
        SchoolYear schoolYear = new SchoolYear(graduationYear, 1);
        // CalculationMode.toEnum liest den Code, er muss dem Namen des Enum-Werts entsprechen.
        schoolYear.setCalculationMode(new CalculationMode(mode.name(), mode.name(), mode.ordinal()));
        schoolYear.setWeeks(weeks);
        PayrollTypeFixture.putWeeklyLessons(schoolYear);

        Teacher teacher = new Teacher(1);
        teacher.setBirthday(birthday);
        Employment employment = new Employment(1, schoolYear, teacher);
        employment.setPayment1(payment1);
        employment.setPayment2(payment2);

        Calculation calculation = Calculation.create(employment, PayrollTypeFixture.stream());
        int id = 1;
        for (CourseSpec spec : courses) {
            calculation.addCourse(spec.toCourse(id++, schoolYear, teacher));
        }

        id = 1;
        for (PoolSpec spec : poolEntries) {
            calculation.addPoolEntry(spec.toPoolEntry(id++, schoolYear, teacher));
        }

        for (ThesisSpec spec : thesisEntries) {
            calculation.addThesisEntry(spec.toThesisEntry(schoolYear, teacher));
        }

        return calculation.createWorkload();
    }

    static final class Builder {

        private final String name;
        private final List<CourseSpec> courses = new ArrayList<>();
        private final List<PoolSpec> poolEntries = new ArrayList<>();
        private final List<ThesisSpec> thesisEntries = new ArrayList<>();
        private final Map<CalculationMode.Enum, Expectations> expectationsPerMode = new LinkedHashMap<>();
        private List<Expectations> current;
        private int graduationYear = DEFAULT_GRADUATION_YEAR;
        private int weeks = DEFAULT_WEEKS;
        private LocalDate birthday;
        private double payment1;
        private double payment2;

        private Builder(String name) {
            this.name = name;
        }

        Builder graduationYear(int graduationYear) {
            this.graduationYear = graduationYear;
            return this;
        }

        Builder weeks(int weeks) {
            this.weeks = weeks;
            return this;
        }

        Builder birthday(int year, int month, int day) {
            birthday = LocalDate.of(year, month, day);
            return this;
        }

        Builder payment(double payment1, double payment2) {
            this.payment1 = payment1;
            this.payment2 = payment2;
            return this;
        }

        Builder course(Grade grade, Subject subject, double lessons1, double lessons2) {
            courses.add(new CourseSpec(grade, subject, lessons1, lessons2));
            return this;
        }

        Builder pool(PoolType poolType, double percent1, double percent2) {
            poolEntries.add(new PoolSpec(poolType, percent1, percent2));
            return this;
        }

        Builder thesis(ThesisType thesisType, double count) {
            thesisEntries.add(new ThesisSpec(thesisType, count));
            return this;
        }

        Builder mode(CalculationMode.Enum... modes) {
            if (modes.length == 0) {
                throw new IllegalArgumentException("Mindestens ein CalculationMode erforderlich.");
            }

            current = new ArrayList<>();
            for (CalculationMode.Enum mode : modes) {
                if (expectationsPerMode.containsKey(mode)) {
                    throw new IllegalArgumentException("CalculationMode mehrfach deklariert: " + mode);
                }

                Expectations expectations = new Expectations();
                expectationsPerMode.put(mode, expectations);
                current.add(expectations);
            }

            return this;
        }

        Builder row(PayrollType type, double lessons1, double percent1, double lessons2, double percent2) {
            return row(type.getDescription(), lessons1, percent1, lessons2, percent2);
        }

        Builder ipbRow(PayrollType type, double lessons1, double percent1, double lessons2, double percent2) {
            return row(IPB_CORRECTION_PREFIX + type.getDescription(), lessons1, percent1, lessons2, percent2);
        }

        Builder total(double percent1, double percent2) {
            requireMode();
            current.forEach(e -> {
                e.totalPercent1 = percent1;
                e.totalPercent2 = percent2;
            });
            return this;
        }

        private Builder row(String description, double lessons1, double percent1, double lessons2, double percent2) {
            requireMode();
            current.forEach(e -> e.rows.add(new RowExpectation(description, lessons1, percent1, lessons2, percent2)));
            return this;
        }

        Stream<CalculationTestCase> cases() {
            if (expectationsPerMode.isEmpty()) {
                throw new IllegalStateException("Szenario ohne CalculationMode: " + name);
            }

            CalculationScenario scenario = new CalculationScenario(this);
            return expectationsPerMode.entrySet().stream()
                    .map(entry -> new CalculationTestCase(scenario, entry.getKey(), entry.getValue()));
        }

        private void requireMode() {
            if (current == null) {
                throw new IllegalStateException("Erwartung ohne vorangehendes mode(...): " + name);
            }
        }
    }

    static final class Expectations {

        final List<RowExpectation> rows = new ArrayList<>();
        Double totalPercent1;
        Double totalPercent2;
    }

    static final class RowExpectation {

        final String description;
        final double lessons1;
        final double percent1;
        final double lessons2;
        final double percent2;

        private RowExpectation(String description, double lessons1, double percent1,
                               double lessons2, double percent2) {
            this.description = description;
            this.lessons1 = lessons1;
            this.percent1 = percent1;
            this.lessons2 = lessons2;
            this.percent2 = percent2;
        }
    }

    private static final class CourseSpec {

        private final Grade grade;
        private final Subject subject;
        private final double lessons1;
        private final double lessons2;

        private CourseSpec(Grade grade, Subject subject, double lessons1, double lessons2) {
            this.grade = grade;
            this.subject = subject;
            this.lessons1 = lessons1;
            this.lessons2 = lessons2;
        }

        private Course toCourse(int id, SchoolYear schoolYear, Teacher teacher) {
            Course result = new Course(false, grade, id, schoolYear, subject);
            result.setTeachers1(Stream.of(teacher));
            result.setTeachers2(Stream.of(teacher));
            result.setLessons1(lessons1);
            result.setLessons2(lessons2);
            return result;
        }
    }

    private static final class PoolSpec {

        private final PoolType poolType;
        private final double percent1;
        private final double percent2;

        private PoolSpec(PoolType poolType, double percent1, double percent2) {
            this.poolType = poolType;
            this.percent1 = percent1;
            this.percent2 = percent2;
        }

        private PoolEntry toPoolEntry(int id, SchoolYear schoolYear, Teacher teacher) {
            PoolEntry result = new PoolEntry(id, schoolYear);
            result.setType(poolType);
            result.setTeacher(teacher);
            result.setDescription(poolType.getDescription());
            result.setPercent1(percent1);
            result.setPercent2(percent2);
            return result;
        }
    }

    private static final class ThesisSpec {

        private final ThesisType thesisType;
        private final double count;

        private ThesisSpec(ThesisType thesisType, double count) {
            this.thesisType = thesisType;
            this.count = count;
        }

        private ThesisEntry toThesisEntry(SchoolYear schoolYear, Teacher teacher) {
            return new ThesisEntry(count, schoolYear, teacher, thesisType);
        }
    }
}
