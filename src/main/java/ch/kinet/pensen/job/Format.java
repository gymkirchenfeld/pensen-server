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
package ch.kinet.pensen.job;

import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.server.Configuration;

public final class Format {

    public static String employmentType(Employment employment) {
        return employment.isTemporary() ? "befristet" : "unbefristet";
    }

    public static String lessons(double value) {
        if (value == 0) {
            return "—";
        }

        return number(value, 2) + " L";
    }

    public static String name(Teacher teacher) {
        StringBuilder result = new StringBuilder();
        result.append(teacher.getFirstName());
        result.append(' ');
        result.append(teacher.getLastName());
        return result.toString();
    }

    public static String number(double value, int decimals) {
        return String.format("%." + decimals + "f", value);
    }

    public static String percent(double value, boolean printZero) {
        return percent(value, printZero, Configuration.getInstance().getPercentDecimals());
    }

    public static String percent(double value, boolean printZero, int decimals) {
        if (!printZero && value == 0) {
            return "";
        }

        return number(value, decimals) + "%";
    }

    public static String percentRange(double min, double max) {
        int decimalPlaces = Configuration.getInstance().getPercentDecimals();
        StringBuilder result = new StringBuilder();
        result.append(number(min, decimalPlaces));
        if (min != max) {
            result.append(" — ");
            result.append(number(max, decimalPlaces));
        }

        result.append("%");
        return result.toString();
    }

    public static String percentSemester(double sem1, double sem2) {
        int decimalPlaces = Configuration.getInstance().getPercentDecimals();
        StringBuilder result = new StringBuilder();
        result.append(number(sem1, decimalPlaces));
        if (sem1 != sem2) {
            result.append(" / ");
            result.append(number(sem2, decimalPlaces));
        }

        result.append("%");
        return result.toString();
    }

}
