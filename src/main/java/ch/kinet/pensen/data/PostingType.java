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

import ch.kinet.reflect.PropertyInitializer;

public final class PostingType extends LookupValue {

    public static final String DB_PAYROLL_TYPE = "PayrollType";
    public static final String DB_PERCENT = "Percent";
    private final PayrollType payrollType;
    private final boolean percent;

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID, DB_PAYROLL_TYPE, DB_PERCENT})
    public PostingType(String code, String description, int id, PayrollType payrollType, boolean percent) {
        super(code, description, id);
        this.payrollType = payrollType;
        this.percent = percent;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public boolean isPercent() {
        return percent;
    }
}
