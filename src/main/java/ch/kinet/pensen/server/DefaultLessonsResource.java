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
package ch.kinet.pensen.server;

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.DefaultLessons;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.EntityMap;
import ch.kinet.pensen.data.PensenData;

public final class DefaultLessonsResource extends EntityResource<DefaultLessons> {

    private PensenData pensenData;

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
        return Response.json(object);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        Curriculum curriculum = pensenData.getCurriculumById(query.getInt("curriculum", -1));
        if (curriculum == null) {
            return Response.badRequest("missing curriculum");
        }

        Division division = pensenData.getDivisionById(query.getInt("division", -1));
        JsonObject result = JsonObject.create();
        result.put("grades", JsonArray.createTerse(curriculum.grades()));
        result.put("items", JsonArray.createTerse(pensenData.loadDefaultLessons(curriculum, division)));
        return Response.json(result);
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        object.setLessonMap1(EntityMap.parseJson(data, DefaultLessons.JSON_LESSONS_1, object.getCurriculum().grades(), -1));
        object.setLessonMap2(EntityMap.parseJson(data, DefaultLessons.JSON_LESSONS_2, object.getCurriculum().grades(), -1));
        pensenData.updateDefaultLessons(object);
        return Response.noContent();
    }

    @Override
    protected DefaultLessons loadObject(int id) {
        return pensenData.loadDefaultLessons(id);
    }
}
