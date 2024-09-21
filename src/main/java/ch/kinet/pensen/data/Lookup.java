/*
 * Copyright (C) 2014 - 2024 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class Lookup<T extends LookupValue> {

    private static final String NONE_ENUM_NAME = "none";
    private final Map<String, T> byCode;
    private final Map<Integer, T> byId;
    private final List<T> list;

    public static <T extends LookupValue> Lookup<T> create() {
        return new Lookup<>();
    }

    Lookup() {
        this.list = new ArrayList<>();
        this.byCode = new HashMap<>();
        this.byId = new HashMap<>();
    }

    public void add(T item) {
        doAdd(item);
        Collections.sort(list);
    }

    public void addAll(Stream<T> stream) {
        stream.sorted().forEachOrdered(this::doAdd);
    }

    public void clear() {
        list.clear();
        byCode.clear();
        byId.clear();
    }

    public T byCode(String code) {
        if (Util.isEmpty(code)) {
            return null;
        }

        return byCode.get(code.toLowerCase());
    }

    public T byEnum(Enum value) {
        if (value == null || NONE_ENUM_NAME.equals(value.name())) {
            return null;
        }

        return byCode.get(value.name().toLowerCase());
    }

    public T byId(int id) {
        return byId.get(id);
    }

    public Stream<T> stream() {
        return list.stream();
    }

    private void doAdd(T item) {
        int id = item.getId();
        String code = item.getCode();
        if (byCode.containsKey(code)) {
            throw new IllegalArgumentException("Trying to add duplicate code " + code + " to lookup.");
        }

        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Trying to add duplicate id " + id + " to lookup.");
        }

        list.add(item);
        byCode.put(item.getCode().toLowerCase(), item);
        byId.put(item.getId(), item);
    }
}
