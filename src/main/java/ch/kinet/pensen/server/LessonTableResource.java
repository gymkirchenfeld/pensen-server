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
package ch.kinet.pensen.server;

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.LessonTable;
import ch.kinet.pensen.data.LessonType;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.Subject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class LessonTableResource extends ObjectResource {

    private static final String JSON_ID = "id";
    private static final String JSON_CURRICULUM = "curriculum";
    private static final String JSON_DETAILS = "details";
    private static final String JSON_DIVISION = "division";
    private static final String JSON_SUBJECT = "subject";
    private PensenData pensenData;
    private Curriculum curriculum;
    private Division division = null;
    private Subject subject;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        curriculum = pensenData.getCurriculumById(query.getInt("curriculum", -1));
        if (curriculum == null) {
            return Response.badRequest("Bitte einen Lehrgang auswählen.");
        }

        division = pensenData.getDivisionById(query.getInt("division", -1));
        subject = pensenData.getSubjectById(query.getInt("subject", -1));
        return Response.jsonTerse(subject == null ?
            pensenData.loadLessonTable(curriculum, division) :
            loadObject()
        );
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.json(loadObject());
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        pensenData.saveLessonTableEntries(curriculum, division, subject, parseEntries(data));
        return Response.noContent();
    }

    @Override
    protected Response parseResourceId(String resourceId) {
        String[] parts = resourceId.split("-");
        if (parts.length < 2 || parts.length > 3) {
            return Response.badRequest();
        }

        curriculum = pensenData.getCurriculumById(Util.parseInt(parts[0], -1));
        subject = pensenData.getSubjectById(Util.parseInt(parts[1], -1));
        if (curriculum == null || subject == null) {
            return Response.notFound();
        }

        if (parts.length == 3) {
            division = pensenData.getDivisionById(Util.parseInt(parts[2], -1));
        }

        return null;
    }

    private JsonObject loadObject() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, resourceId(curriculum, subject, division));
        result.putTerse(JSON_CURRICULUM, curriculum);
        result.putTerse(JSON_SUBJECT, subject);
        if (division != null) {
            result.putTerse(JSON_DIVISION, division);
        }

        result.put(JSON_DETAILS, JsonArray.createTerse(pensenData.loadLessonTableEntries(curriculum, division, subject)));
        return result;
    }

    private String resourceId(Curriculum curriculum, Subject subject, Division division) {
        StringBuilder result = new StringBuilder();
        result.append(curriculum.getId());
        result.append("-");
        result.append(subject.getId());
        if (division != null) {
            result.append("-");
            result.append(division.getId());
        }

        return result.toString();
    }

    private Stream<LessonTable.Entry> parseEntries(JsonObject data) {
        Map<Grade, LessonTable.Entry> result = new HashMap<>();
        if (!data.hasKey(JSON_DETAILS)) {
            return Stream.empty();
        }

        JsonArray jsonArray = data.getArray(JSON_DETAILS);
        if (jsonArray == null) {
            return Stream.empty();
        }

        for (int i = 0; i < jsonArray.length(); ++i) {
            JsonObject item = jsonArray.getObject(i);
            Grade grade = pensenData.getGradeById(item.getObjectId(LessonTable.JSON_GRADE, -1));
            int typeId = item.getObjectId(LessonTable.JSON_TYPE, -1);
            LessonType type = pensenData.getLessonTypeById(typeId);
            double lessons1 = item.getDouble(LessonTable.JSON_LESSONS_1, 0.0);
            double lessons2 = item.getDouble(LessonTable.JSON_LESSONS_2, 0.0);
            if (grade != null && type != null) {
                LessonTable.Entry entry = LessonTable.createEntry(grade, lessons1, lessons2, type);
                result.put(grade, entry);
            }
        }

        return result.values().stream();
    }
}
