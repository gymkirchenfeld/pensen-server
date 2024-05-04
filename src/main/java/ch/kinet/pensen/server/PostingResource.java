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
import ch.kinet.pensen.data.Posting;
import ch.kinet.pensen.data.PostingType;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.data.ValueMap;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class PostingResource extends EntityResource<Posting> {

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

        return Response.jsonArrayTerse(pensenData.loadPostings(schoolYear));
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        JsonObject result = object.toJsonVerbose();
        result.put(Posting.JSON_DETAILS, pensenData.loadPostingDetails(object).toJsonVerbose());
        return Response.jsonVerbose(result);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        String description = data.getString(Posting.JSON_DESCRIPTION);
        LocalDate startDate = data.getDate(Posting.JSON_START_DATE);
        if (startDate == null) {
            return Response.badRequest("Bitte ein gültiges Anfangsdatum auswählen.");
        }

        LocalDate endDate = data.getDate(Posting.JSON_END_DATE);
        SchoolYear schoolYear = pensenData.getSchoolYearById(data.getObjectId(Posting.JSON_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.badRequest("Bitte ein Schuljahr auswählen.");
        }

        if (schoolYear.isArchived()) {
            return Response.badRequest("Zu archivierten Schuljahren können keine Buchungen hinzugefügt werden.");
        }

        Teacher teacher = pensenData.getTeacherById(data.getObjectId(Posting.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest("Bitte eine Lehrperson auswählen.");
        }

        Posting result = pensenData.createPosting(description, endDate, schoolYear, startDate, teacher);
        ValueMap<PostingType> details = ValueMap.parseJson(data, Posting.JSON_DETAILS, pensenData.streamPostingTypes(), 0);
        pensenData.savePostingDetails(result, details);
        pensenData.recalculateBalance(schoolYear, teacher);
        return Response.created();
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        if (object.getSchoolYear().isArchived()) {
            return Response.badRequest("Buchungen in archivierten Schuljahren können nicht verändert werden.");
        }

        String description = data.getString(Posting.JSON_DESCRIPTION);
        LocalDate startDate = data.getDate(Posting.JSON_START_DATE);
        LocalDate endDate = data.getDate(Posting.JSON_END_DATE);
        Teacher teacher = pensenData.getTeacherById(data.getObjectId(Posting.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest();
        }

        Set<String> changed = new HashSet<>();
        boolean recalculate = false;
        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(Posting.DB_DESCRIPTION);
        }

        if (!Util.equal(object.getStartDate(), startDate)) {
            object.setStartDate(startDate);
            changed.add(Posting.DB_START_DATE);
            recalculate = true;
        }

        if (!Util.equal(object.getEndDate(), endDate)) {
            object.setEndDate(endDate);
            changed.add(Posting.DB_END_DATE);
        }

        if (!Util.equal(object.getTeacher(), teacher)) {
            // recalculate balance of previous teacher
            pensenData.recalculateBalance(object.getSchoolYear(), object.getTeacher());
            object.setTeacher(teacher);
            changed.add(Posting.DB_TEACHER);
            recalculate = true;
        }

        pensenData.updatePosting(object, changed);
        ValueMap<PostingType> details = ValueMap.parseJson(data, Posting.JSON_DETAILS, pensenData.streamPostingTypes(), 0);
        pensenData.savePostingDetails(object, details);
        if (recalculate) {
            pensenData.recalculateBalance(object.getSchoolYear(), object.getTeacher());
        }

        return Response.noContent();
    }

    @Override
    protected boolean isDeleteAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response delete(Authorisation authorisation) {
        if (object.getSchoolYear().isArchived()) {
            return Response.forbidden();
        }

        pensenData.deletePosting(object);
        return Response.noContent();
    }

    @Override
    protected Posting loadObject(int id) {
        return pensenData.loadPosting(id);
    }
}
