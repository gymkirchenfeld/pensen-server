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
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Curriculum;
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
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation auth, Query query) {
        return Response.jsonArrayVerbose(pensenData.streamCurriculums());
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(object);
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean archived = data.getBoolean(Curriculum.JSON_ARCHIVED, false);
        String code = data.getString(Curriculum.JSON_CODE);
        String description = data.getString(Curriculum.JSON_DESCRIPTION);

        Set<String> changed = new HashSet<>();
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(Curriculum.DB_ARCHIVED);
        }

        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(Curriculum.DB_CODE);
        }

        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(Curriculum.DB_DESCRIPTION);
        }

        pensenData.updateCurriculum(object, changed);
        return Response.noContent();
    }

    @Override
    protected Curriculum loadObject(int id) {
        return pensenData.getCurriculumById(id);
    }
}
