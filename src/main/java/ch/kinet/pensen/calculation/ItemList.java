/*
 * Copyright (C) 2023 by Stefan Rothe
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ItemList<T extends Json> implements Json {

    private static final String JSON_ITEMS = "items";
    private final List<T> items = new ArrayList<>();

    public final boolean isEmpty() {
        return items.isEmpty();
    }

    public final Stream<T> items() {
        return items.stream();
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ITEMS, JsonArray.createVerbose(items.stream()));
        return result;
    }

    @Override
    public final JsonObject toJsonVerbose() {
        return toJsonTerse();
    }

    final void add(T item) {
        items.add(item);
    }
}
