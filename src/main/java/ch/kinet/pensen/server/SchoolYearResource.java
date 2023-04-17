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
import ch.kinet.pensen.data.CalculationMode;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import java.util.HashSet;
import java.util.Set;

public final class SchoolYearResource extends EntityResource<SchoolYear> {

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
        String code = data.getString(SchoolYear.JSON_CODE);
        String description = data.getString(SchoolYear.JSON_DESCRIPTION);
        int graduationYear = data.getInt(SchoolYear.JSON_GRADUATION_YEAR, -1);
        if (graduationYear < 1900) {
            return Response.badRequest();
        }

        CalculationMode calculationMode = pensenData.getCalculationModeById(data.getObjectId(SchoolYear.JSON_CALCULATION_MODE, -1));
        if (calculationMode == null) {
            return Response.badRequest();
        }

        int weeks = data.getInt(SchoolYear.JSON_WEEKS, -1);
        if (weeks <= 0) {
            return Response.badRequest();
        }

        return Response.json(pensenData.createSchoolYear(calculationMode, code, description, graduationYear, weeks));
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
        return Response.jsonVerbose(pensenData.streamSchoolYears());
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        Set<String> changed = new HashSet<>();
        boolean recalculate = false;

        boolean archived = data.getBoolean(SchoolYear.JSON_ARCHIVED, false);
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(SchoolYear.DB_ARCHIVED);
        }

        String code = data.getString(SchoolYear.JSON_CODE);
        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(SchoolYear.DB_CODE);
        }

        String description = data.getString(SchoolYear.JSON_DESCRIPTION);
        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(SchoolYear.DB_DESCRIPTION);
        }

        boolean finalised = data.getBoolean(SchoolYear.JSON_FINALISED, false);
        if (!Util.equal(object.isFinalised(), finalised)) {
            object.setFinalised(finalised);
            changed.add(SchoolYear.DB_FINALISED);
        }

        CalculationMode calculationMode = pensenData.getCalculationModeById(data.getObjectId(SchoolYear.JSON_CALCULATION_MODE, -1));
        if (calculationMode == null) {
            return Response.badRequest();
        }

        if (!Util.equal(object.getCalculationMode(), calculationMode)) {
            object.setCalculationMode(calculationMode);
            changed.add(SchoolYear.DB_CALCULATION_MODE);
            recalculate = true;
        }

        int weeks = data.getInt(SchoolYear.JSON_WEEKS);
        if (weeks <= 0) {
            return Response.badRequest();
        }

        if (!Util.equal(object.getWeeks(), weeks)) {
            object.setWeeks(weeks);
            changed.add(SchoolYear.DB_WEEKS);
            recalculate = true;
        }

        pensenData.updateSchoolYear(object, changed);
        if (recalculate) {
            pensenData.recalculateBalance(object);
        }

        return Response.json(data);
    }

    @Override
    protected SchoolYear loadObject(int id) {
        return pensenData.getSchoolYearById(id);
    }
}
