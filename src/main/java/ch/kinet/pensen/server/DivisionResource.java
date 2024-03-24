/*
 * Copyright (C) 2022 - 2024 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Data;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.PensenData;
import java.util.HashSet;
import java.util.Set;

public final class DivisionResource extends EntityResource<Division> {

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
        return Response.jsonArrayTerse(pensenData.streamDivisions());
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
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        String code = data.getString(Division.JSON_CODE);
        String description = data.getString(Division.JSON_DESCRIPTION);
        String grouping = data.getString(Division.JSON_GROUPING);
        String headName = data.getString(Division.JSON_HEAD_NAME);
        Data headSignature = data.getData(Division.JSON_HEAD_SIGNATURE);
        String headTitle = data.getString(Division.JSON_HEAD_TITLE);
        Data logo = data.getData(Division.JSON_LOGO);

        pensenData.createDivision(
            code, description, grouping, headName, headSignature.toBinary(), headTitle, logo.toBinary()
        );
        return Response.noContent();
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        String code = data.getString(Division.JSON_CODE);
        String description = data.getString(Division.JSON_DESCRIPTION);
        String grouping = data.getString(Division.JSON_GROUPING);
        String headName = data.getString(Division.JSON_HEAD_NAME);
        Data headSignature = data.getData(Division.JSON_HEAD_SIGNATURE);
        String headTitle = data.getString(Division.JSON_HEAD_TITLE);
        Data logo = data.getData(Division.JSON_LOGO);

        Set<String> changed = new HashSet<>();
        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(Division.DB_CODE);
        }

        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(Division.DB_DESCRIPTION);
        }

        if (!Util.equal(object.getGrouping(), grouping)) {
            object.setGrouping(grouping);
            changed.add(Division.DB_GROUPING);
        }

        if (!Util.equal(object.getHeadName(), headName)) {
            object.setHeadName(headName);
            changed.add(Division.DB_HEAD_NAME);
        }

        object.setHeadSignature(headSignature.toBinary());
        changed.add(Division.DB_HEAD_SIGNATURE);

        if (!Util.equal(object.getHeadTitle(), headTitle)) {
            object.setHeadTitle(headTitle);
            changed.add(Division.DB_HEAD_TITLE);
        }

        object.setLogo(logo.toBinary());
        changed.add(Division.DB_LOGO);

        pensenData.updateDivision(object, changed);
        return Response.noContent();
    }

    @Override
    protected Division loadObject(int id) {
        return pensenData.getDivisionById(id);
    }
}
