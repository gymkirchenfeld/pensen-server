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

import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;

public final class SubjectType extends LookupValue {

    public static final String DB_SORT_ORDER = "SortOrder";
    public static final String JSON_SORT_ORDER = "sortOrder";
    private final int sortOrder;

    @PropertyInitializer({DB_CODE, DB_DESCRIPTION, DB_ID, DB_SORT_ORDER})
    public SubjectType(String code, String description, int id, int sortOrder) {
        super(code, description, id);
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_SORT_ORDER, sortOrder);
        return result;
    }
}
