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

import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.CalculationMode;
import ch.kinet.pensen.data.PayrollType;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.ValueMap;
import java.util.HashSet;
import java.util.Set;

public final class SchoolYearResource extends EntityResource<SchoolYear> {

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
        return Response.jsonArrayVerbose(pensenData.streamSchoolYears());
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
        return authorisation != null && authorisation.isEditAllowed();
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

        object = pensenData.createSchoolYear(calculationMode, code, description, graduationYear, weeks);
        ValueMap<PayrollType> weeklyLessons;
        if (data.hasKey(SchoolYear.JSON_WEEKLY_LESSONS)) {
            weeklyLessons = ValueMap.parseJson(data, SchoolYear.JSON_WEEKLY_LESSONS, pensenData.streamPayrollTypes(), 0);
        }
        else {
            // copy from previous school year
            SchoolYear previous = object.previous();
            weeklyLessons = ValueMap.create();
            pensenData.streamPayrollTypes().forEachOrdered(
                payrollType -> weeklyLessons.put(payrollType, previous.weeklyLessons(payrollType))
            );
        }

        pensenData.saveWeeklyLessons(object, weeklyLessons);
        return Response.createdJsonVerbose(object);
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean archived = data.getBoolean(SchoolYear.JSON_ARCHIVED, false);
        String code = data.getString(SchoolYear.JSON_CODE);
        String description = data.getString(SchoolYear.JSON_DESCRIPTION);
        boolean finalised = data.getBoolean(SchoolYear.JSON_FINALISED, false);
        CalculationMode calculationMode = pensenData.getCalculationModeById(data.getObjectId(SchoolYear.JSON_CALCULATION_MODE, -1));
        if (calculationMode == null) {
            return Response.badRequest();
        }

        int weeks = data.getInt(SchoolYear.JSON_WEEKS);
        if (weeks <= 0) {
            return Response.badRequest();
        }

        Set<String> changed = new HashSet<>();
        boolean recalculate = false;
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(SchoolYear.DB_ARCHIVED);
        }

        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(SchoolYear.DB_CODE);
        }

        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(SchoolYear.DB_DESCRIPTION);
        }

        if (!Util.equal(object.isFinalised(), finalised)) {
            object.setFinalised(finalised);
            changed.add(SchoolYear.DB_FINALISED);
        }

        if (!Util.equal(object.getCalculationMode(), calculationMode)) {
            object.setCalculationMode(calculationMode);
            changed.add(SchoolYear.DB_CALCULATION_MODE);
            recalculate = true;
        }

        if (!Util.equal(object.getWeeks(), weeks)) {
            object.setWeeks(weeks);
            changed.add(SchoolYear.DB_WEEKS);
            recalculate = true;
        }

        pensenData.updateSchoolYear(object, changed);
        ValueMap<PayrollType> weeklyLessons = ValueMap.parseJson(data, SchoolYear.JSON_WEEKLY_LESSONS, pensenData.streamPayrollTypes(), 0);
        pensenData.saveWeeklyLessons(object, weeklyLessons);
        if (recalculate) {
            pensenData.recalculateBalance(object);
        }

        return Response.noContent();
    }

    @Override
    protected SchoolYear loadObject(int id) {
        return pensenData.getSchoolYearById(id);
    }
}
