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
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.PoolEntry;
import ch.kinet.pensen.data.PoolType;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import java.util.HashSet;
import java.util.Set;

public final class PoolEntryResource extends EntityResource<PoolEntry> {

    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
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
    protected Response list(Authorisation authorisation, Query query) {
        SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.badRequest();
        }

        return Response.jsonArrayTerse(pensenData.loadPoolEntries(schoolYear));
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
        String description = data.getString(PoolEntry.JSON_DESCRIPTION);
        double percent1 = data.getDouble(PoolEntry.JSON_PERCENT_1);
        double percent2 = data.getDouble(PoolEntry.JSON_PERCENT_2);
        if (percent1 < 0 || percent2 < 0) {
            return Response.badRequest("Negative Poolbuchungen sind nicht erlaubt.");
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(data.getObjectId(PoolEntry.JSON_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.badRequest("Ein Schuljahr muss ausgewählt werden.");
        }

        if (schoolYear.isArchived()) {
            return Response.badRequest("Zu archivierten Schuljahren können keine Buchungen hinzugefügt werden.");
        }

        Teacher teacher = pensenData.getTeacherById(data.getObjectId(PoolEntry.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest("Eine Lehrperson muss ausgewählt werden.");
        }

        PoolType type = pensenData.getPoolTypeById(data.getObjectId(PoolEntry.JSON_TYPE, -1));
        if (type == null) {
            return Response.badRequest("Ein Typ muss ausgewählt werden.");
        }

        pensenData.createPoolEntry(description, percent1, percent2, schoolYear, teacher, type);
        pensenData.recalculateBalance(schoolYear, teacher);
        return Response.created();
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        if (object.getSchoolYear().isArchived()) {
            return Response.badRequest("Buchungen in archivierten Schuljahren können nicht verändert werden.");
        }

        String description = data.getString(PoolEntry.JSON_DESCRIPTION);
        double percent1 = data.getDouble(PoolEntry.JSON_PERCENT_1);
        double percent2 = data.getDouble(PoolEntry.JSON_PERCENT_2);
        if (percent1 < 0 || percent2 < 0) {
            return Response.badRequest("Negative Prozentangaben sind nicht erlaubt.");
        }

        Teacher teacher = pensenData.getTeacherById(data.getObjectId(PoolEntry.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest("Eine Lehrperson muss ausgewählt werden.");
        }

        PoolType type = pensenData.getPoolTypeById(data.getObjectId(PoolEntry.JSON_TYPE, -1));
        if (type == null) {
            return Response.badRequest("Ein Typ muss ausgewählt werden.");
        }

        Set<String> changed = new HashSet<>();
        boolean recalculate = false;
        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(PoolEntry.DB_DESCRIPTION);
        }

        if (!Util.equal(object.getPercent1(), percent1)) {
            object.setPercent1(percent1);
            changed.add(PoolEntry.DB_PERCENT_1);
            recalculate = true;
        }

        if (!Util.equal(object.getPercent2(), percent2)) {
            object.setPercent2(percent2);
            changed.add(PoolEntry.DB_PERCENT_2);
            recalculate = true;
        }

        if (!Util.equal(object.getTeacher(), teacher)) {
            // recalculate balance of previous teacher
            pensenData.recalculateBalance(object.getSchoolYear(), object.getTeacher());
            object.setTeacher(teacher);
            changed.add(PoolEntry.DB_TEACHER);
            recalculate = true;
        }

        if (!Util.equal(object.getType(), type)) {
            object.setType(type);
            changed.add(PoolEntry.DB_TYPE);
        }

        pensenData.updatePoolEntry(object, changed);
        if (recalculate) {
            pensenData.recalculateBalance(object.getSchoolYear(), object.getTeacher());
        }

        return Response.noContent();
    }

    @Override
    protected boolean isDeleteAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response delete(Authorisation authorisation) {
        if (object.getSchoolYear().isArchived()) {
            return Response.forbidden();
        }

        pensenData.deletePoolEntry(object);
        return Response.noContent();
    }

    @Override
    protected PoolEntry loadObject(int id) {
        return pensenData.loadPoolEntry(id);
    }
}
