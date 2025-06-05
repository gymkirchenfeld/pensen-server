/*
 * Copyright (C) 2022 - 2025 by Sebastian Forster, Stefan Rothe
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
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.PropertyInitializer;

public final class Account extends Entity {

    public static final String DB_EDIT_ALLOWED = "EditAllowed";
    public static final String DB_GRANT_ALLOWED = "GrantAllowed";
    public static final String DB_NAME = "Name";
    public static final String JSON_EDIT_ALLOWED = "editAllowed";
    public static final String JSON_GRANT_ALLOWED = "grantAllowed";
    public static final String JSON_NAME = "accountName";
    private final String name;
    private boolean editAllowed;
    private boolean grantAllowed;

    @PropertyInitializer({DB_ID, DB_NAME})
    public Account(int id, String name) {
        super(id);
        this.name = name;
    }

    @Override
    public int compareTo(Entity entity) {
        if (entity instanceof Account) {
            Account other = (Account) entity;
            return Util.compare(name, other.name);
        }

        return super.compareTo(entity);
    }

    public String getName() {
        return name;
    }

    public boolean isEditAllowed() {
        return editAllowed;
    }

    public boolean isGrantAllowed() {
        return grantAllowed;
    }

    public void setEditAllowed(boolean editAllowed) {
        this.editAllowed = editAllowed;
    }

    public void setGrantAllowed(boolean grantAllowed) {
        this.grantAllowed = grantAllowed;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_NAME, name);
        result.put(JSON_GRANT_ALLOWED, grantAllowed);
        result.put(JSON_EDIT_ALLOWED, editAllowed);
        return result;
    }
}
