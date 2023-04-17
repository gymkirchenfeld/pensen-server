/*
 * Copyright (C) 2022 - 2023 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Entity;
import ch.kinet.reflect.PropertyInitializer;

public final class Authorisation extends Entity {

    public static final String DB_ACCOUNT_NAME = "AccountName";
    public static final String DB_ADMIN = "Admin";
    private final String accountName;
    private final boolean admin;

    @PropertyInitializer({DB_ACCOUNT_NAME, DB_ADMIN, DB_ID})
    public Authorisation(String accountName, boolean admin, int id) {
        super(id);
        this.accountName = accountName;
        this.admin = admin;
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean isAdmin() {
        return admin;
    }
}
