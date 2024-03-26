/*
 * Copyright (C) 2023 - 2024 by Sebastian Forster, Stefan Rothe
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

import java.util.function.BiFunction;

public final class SemesterValue {

    public static SemesterValue copy(SemesterValue source) {
        return new SemesterValue(source.semester1, source.semester2);
    }

    public static SemesterValue create(double semester1, double semester2) {
        return new SemesterValue(semester1, semester2);
    }

    public static SemesterValue create() {
        return new SemesterValue(0.0, 0.0);
    }

    private double semester1;
    private double semester2;

    private SemesterValue(double semester1, double semester2) {
        this.semester1 = semester1;
        this.semester2 = semester2;
    }

    public void add(SemesterEnum semester, double value) {
        switch (semester) {
            case First:
                semester1 += value;
                break;
            case Second:
                semester2 += value;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void add(SemesterValue value) {
        this.semester1 += value.semester1;
        this.semester2 += value.semester2;
    }

    public void add(double semester1, double semester2) {
        this.semester1 += semester1;
        this.semester2 += semester2;
    }

    public double get(SemesterEnum semester) {
        switch (semester) {
            case First:
                return semester1;
            case Second:
                return semester2;
            default:
                throw new IllegalArgumentException();
        }
    }

    public double mean() {
        return (semester1 + semester2) / 2.0;
    }

    public double semester1() {
        return semester1;
    }

    public double semester2() {
        return semester2;
    }

    public void set(double semester1, double semester2) {
        this.semester1 = semester1;
        this.semester2 = semester2;
    }

    public void set(SemesterEnum semester, double value) {
        switch (semester) {
            case First:
                semester1 = value;
                break;
            case Second:
                semester2 = value;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public SemesterValue map(BiFunction<SemesterEnum, Double, Double> action) {
        return SemesterValue.create(
            action.apply(SemesterEnum.First, semester1),
            action.apply(SemesterEnum.Second, semester2)
        );
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(");
        result.append(semester1);
        result.append(", ");
        result.append(semester2);
        result.append(")");
        return result.toString();
    }
}
