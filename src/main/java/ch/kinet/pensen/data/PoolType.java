/*
 * Copyright (C) 2011 - 2023 by Stefan Rothe
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

public final class PoolType extends LookupValue {

    public static final String DB_AUTO_COPY = "AutoCopy";
    public static final String DB_PAYROLL_TYPE = "PayrollType";
    private final boolean autoCopy;
    private final PayrollType payrollType;

    @PropertyInitializer({DB_AUTO_COPY, DB_CODE, DB_DESCRIPTION, DB_ID, DB_PAYROLL_TYPE})
    public PoolType(boolean autoCopy, String code, String description, int id, PayrollType payrollType) {
        super(code, description, id);
        this.autoCopy = autoCopy;
        this.payrollType = payrollType;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public boolean isAutoCopy() {
        return autoCopy;
    }
}
