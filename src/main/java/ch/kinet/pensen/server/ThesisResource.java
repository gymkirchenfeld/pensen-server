/*
 * Copyright (C) 2023 - 2024 by Sebastian Forster, Stefan Rothe
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
package ch.kinet.pensen.server;

import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.data.ThesisType;
import ch.kinet.pensen.data.ValueMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ThesisResource extends ObjectResource {

    private static final String JSON_ID = "id";
    private static final String JSON_SCHOOL_YEAR = "schoolYear";
    private static final String JSON_TEACHER = "teacher";
    private static final String JSON_THESIS_COUNTS = "thesisCounts";
    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
    private PensenData pensenData;
    private SchoolYear schoolYear;
    private Teacher teacher;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        JsonObject result = toJson(schoolYear, teacher);
        JsonObject counts = JsonObject.create();
        result.put(JSON_THESIS_COUNTS, counts);
        pensenData.loadThesisEntries(schoolYear, teacher).forEachOrdered(
            entry -> counts.put(String.valueOf(entry.getType().getId()), entry.getCount())
        );

        return Response.json(result);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        if (!query.hasKey(QUERY_SCHOOL_YEAR)) {
            return Response.badRequest();
        }

        SchoolYear schoolYearFilter = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYearFilter == null) {
            return Response.notFound();
        }

        Map<Teacher, JsonObject> map = pensenData.loadEmployments(schoolYearFilter, null).collect(
            Collectors.toMap(Employment::getTeacher, item -> toJson(schoolYearFilter, item.getTeacher()))
        );

        pensenData.loadThesisEntries(schoolYearFilter).forEachOrdered(entry -> {
            if (map.containsKey(entry.getTeacher())) {
                String key = String.valueOf(entry.getType().getId());
                map.get(entry.getTeacher()).getObject(JSON_THESIS_COUNTS).put(key, entry.getCount());
            }
        });

        return Response.jsonArrayTerse(map.keySet().stream().sorted().map(teacher -> map.get(teacher)));
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        if (schoolYear.isArchived()) {
            return Response.badRequest("Abschlussarbeiten in archivierten Schuljahren können nicht verändert werden.");
        }

        ValueMap<ThesisType> thesisCounts = ValueMap.parseJson(data, JSON_THESIS_COUNTS, pensenData.streamThesisTypes(), 0);
        pensenData.saveThesisEntries(schoolYear, teacher, thesisCounts);
        pensenData.recalculateBalance(schoolYear, teacher);
        return Response.noContent();
    }

    @Override
    protected Response parseResourceId(String resourceId) {
        String[] parts = resourceId.split("-");
        if (parts.length != 2) {
            return Response.badRequest();
        }

        schoolYear = pensenData.getSchoolYearById(Util.parseInt(parts[0], -1));
        teacher = pensenData.getTeacherById(Util.parseInt(parts[1], -1));
        if (schoolYear == null || teacher == null) {
            return Response.notFound();
        }

        return null;
    }

    private JsonObject toJson(SchoolYear schoolYear, Teacher teacher) {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, resourceId(schoolYear, teacher));
        result.putTerse(JSON_SCHOOL_YEAR, schoolYear);
        result.putTerse(JSON_TEACHER, teacher);
        result.putTerse(JSON_THESIS_COUNTS, JsonObject.create());
        return result;
    }

    private String resourceId(SchoolYear schoolYear, Teacher teacher) {
        StringBuilder result = new StringBuilder();
        result.append(schoolYear.getId());
        result.append("-");
        result.append(teacher.getId());
        return result.toString();
    }
}
