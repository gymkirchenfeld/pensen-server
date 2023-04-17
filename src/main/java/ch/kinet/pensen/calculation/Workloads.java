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
package ch.kinet.pensen.calculation;

import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import java.util.Map;
import java.util.stream.Stream;

public final class Workloads {

    public static Workloads create(SchoolYear schoolYear, Map<Teacher, Workload> map) {
        return new Workloads(schoolYear, map);
    }

    private final Map<Teacher, Workload> map;
    private final SchoolYear schoolYear;

    private Workloads(SchoolYear schoolYear, Map<Teacher, Workload> map) {
        this.schoolYear = schoolYear;
        this.map = map;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public Workload getWorkload(Teacher teacher) {
        return map.get(teacher);
    }

    public int size() {
        return map.size();
    }

    public Stream<Teacher> teachers() {
        return map.keySet().stream().sorted();
    }
}
