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
package ch.kinet.pensen.server;

import ch.kinet.JsonObject;
import ch.kinet.SetComparison;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import java.util.HashSet;
import java.util.Set;

public final class CurriculumResource extends EntityResource<Curriculum> {

    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        if (data.hasKey("id")) {
            Curriculum original = pensenData.getCurriculumById(data.getInt("id", -1));
            if (original == null) {
                return Response.notFound();
            }

            System.out.println("Copy curriculum " + original.getId());
            return Response.json(pensenData.copyCurriculum(original));
        }

        String code = data.getString(Curriculum.JSON_CODE);
        String description = data.getString(Curriculum.JSON_DESCRIPTION);

        Curriculum result = pensenData.createCurriculum(code, description);
        result.setGrades(pensenData.parseGrades(data.getArray(Curriculum.JSON_GRADES)));
        pensenData.updateCurriculum(result, Util.createSet(Curriculum.DB_GRADES));
        return Response.json(result);
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
    protected Response list(Authorisation auth, Query query) {
        return Response.jsonVerbose(pensenData.streamCurriculums());
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        Set<String> changed = new HashSet<>();
        boolean archived = data.getBoolean(Curriculum.JSON_ARCHIVED, false);
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(Curriculum.DB_ARCHIVED);
        }

        String code = data.getString(Curriculum.JSON_CODE);
        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(Curriculum.DB_CODE);
        }

        String description = data.getString(Curriculum.JSON_DESCRIPTION);
        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(Curriculum.DB_DESCRIPTION);
        }

        Set<Grade> grades = pensenData.parseGrades(data.getArray(Curriculum.JSON_GRADES));
        SetComparison<Grade> changes = SetComparison.create(Util.createSet(object.grades()), grades);
        if (changes.hasChanges()) {
            object.setGrades(grades);
            changed.add(Curriculum.DB_GRADES);
        }

        pensenData.updateCurriculum(object, changed);
        return Response.json(data);
    }

    @Override
    protected Curriculum loadObject(int id) {
        return pensenData.getCurriculumById(id);
    }
}
