/*
 * Copyright (C) 2023 by Sebastian Forster, Stefan Rothe
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
import ch.kinet.Json;
import ch.kinet.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ValueMap<T extends Entity> implements Json {

    public static final <T extends Entity> ValueMap<T> create(Stream<T> keys) {
        return new ValueMap<>(keys);
    }

    public static <T extends Entity> ValueMap<T> parseJson(JsonObject data, String attribute, Stream<T> keys,
                                                           double defaultValue) {
        ValueMap<T> result = create(keys);
        if (!data.hasKey(attribute)) {
            return result;
        }

        JsonObject jsonMap = data.getObject(attribute);
        if (jsonMap == null) {
            return result;
        }

        result.map.keySet().forEach(key -> {
            String jsonKey = String.valueOf(key.getId());
            if (jsonMap.hasKey(jsonKey)) {
                result.put(key, jsonMap.getDouble(jsonKey, defaultValue));
            }
        });

        return result;
    }

    private final Map<T, Double> map = new HashMap<>();

    private ValueMap(Stream<T> keys) {
        keys.forEachOrdered(key -> map.put(key, 0.0));
    }

    public void put(T key, double value) {
        map.put(key, value);
    }

    public Stream<Map.Entry<T, Double>> stream() {
        return map.entrySet().stream();
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        stream().forEachOrdered(entry -> {
            result.put(String.valueOf(entry.getKey().getId()), entry.getValue());
        });

        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        return toJsonTerse();
    }
}
