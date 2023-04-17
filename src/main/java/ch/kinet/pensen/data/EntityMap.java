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
import ch.kinet.Json;
import ch.kinet.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Deprecated
public final class EntityMap<T extends Entity> implements Json {

    private final Map<T, Double> map;

    public static <T extends Entity> EntityMap<T> create() {
        return new EntityMap<>(new HashMap<>());
    }

    public static <T extends Entity> EntityMap<T> parseJson(JsonObject data, String attribute, Stream<T> keys,
                                                            double defaultValue) {
        Map<T, Double> result = new HashMap<>();
        if (!data.hasKey(attribute)) {
            return new EntityMap<>(result);
        }

        JsonObject jsonMap = data.getObject(attribute);
        if (jsonMap == null) {
            return new EntityMap<>(result);
        }

        keys.forEachOrdered(key -> {
            String jsonKey = String.valueOf(key.getId());
            if (jsonMap.hasKey(jsonKey)) {
                result.put(key, jsonMap.getDouble(jsonKey, defaultValue));
            }
        });

        return new EntityMap<>(result);
    }

    public static <T extends Entity> EntityMap<T> parseList(List<Double> list, Stream<T> keys) {
        Map<T, Double> result = new HashMap<>();
        keys.forEachOrdered(key -> {
            int index = key.getId() - 1;
            if (list.size() > index) {
                result.put(key, list.get(index));
            }
            else {
                result.put(key, 0.0);
            }
        });

        return new EntityMap<>(result);
    }

    private EntityMap(Map<T, Double> map) {
        this.map = map;
    }

    public ValueMap<T> toValueMap() {
        ValueMap<T> result = ValueMap.create(map.keySet().stream());
        stream().forEachOrdered(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    public double get(T key) {
        Double result = map.get(key);
        return result == null ? -1 : result;
    }

    public Stream<Map.Entry<T, Double>> stream() {
        return map.entrySet().stream();
    }

    public void put(T key, double value) {
        map.put(key, value);
    }

    public List<Double> toList() {
        List<Double> result = new ArrayList<>();
        for (Map.Entry<T, Double> entry : map.entrySet()) {
            int index = entry.getKey().getId() - 1;
            while (index >= result.size()) {
                result.add(0.0);
            }

            result.set(index, entry.getValue());
        }

        return result;
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
