/*
 * Copyright (C) 2022 - 2025 by Sebastian Forster, Stefan Rothe
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
import ch.kinet.pensen.data.Curriculum;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolClass;
import ch.kinet.pensen.data.SchoolYear;
import java.util.HashSet;
import java.util.Set;

public final class SchoolClassResource extends EntityResource<SchoolClass> {

    private static final String QUERY_GRADE = "grade";
    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        Grade grade = null;
        if (!query.hasKey(QUERY_SCHOOL_YEAR)) {
            return Response.jsonArrayVerbose(pensenData.streamSchoolClasses());
        }

        if (query.hasKey(QUERY_GRADE)) {
            grade = pensenData.getGradeById(query.getInt(QUERY_GRADE, -1));
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.notFound();
        }

        return Response.jsonArrayTerse(pensenData.streamSchoolClassesFor(schoolYear, grade));
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(object);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation.isEditAllowed();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        String code = data.getString(SchoolClass.JSON_CODE);
        Curriculum curriculum = pensenData.getCurriculumById(data.getObjectId(SchoolClass.JSON_CURRICULUM, -1));
        if (curriculum == null) {
            return Response.badRequest();
        }

        Division division = pensenData.getDivisionById(data.getObjectId(SchoolClass.JSON_DIVISION, -1));
        if (division == null) {
            return Response.badRequest();
        }

        int graduationYear = data.getInt(SchoolClass.JSON_GRADUATION_YEAR);

        object = pensenData.createSchoolClass(code, curriculum, division, graduationYear);
        return Response.createdJsonVerbose(object);
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation.isEditAllowed();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean archived = data.getBoolean(SchoolClass.JSON_ARCHIVED, false);
        String code = data.getString(SchoolClass.JSON_CODE);
        Division division = pensenData.getDivisionById(data.getObjectId(SchoolClass.JSON_DIVISION, -1));
        if (division == null) {
            return Response.badRequest();
        }

        Set<String> changed = new HashSet<>();
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(SchoolClass.DB_ARCHIVED);
        }

        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(SchoolClass.DB_CODE);
        }

        if (!Util.equal(object.getDivision(), division)) {
            object.setDivision(division);
            changed.add(SchoolClass.DB_DIVISION);
        }

        pensenData.updateSchoolClass(object, changed);
        return Response.noContent();
    }

    @Override
    protected boolean isDeleteAllowed(Authorisation authorisation) {
        return authorisation.isEditAllowed();
    }

    @Override
    protected Response delete(Authorisation authorisation) {
        System.out.println("Deleting schoolClass");
        if (pensenData.deleteSchoolClass(object)) {
            return Response.ok();
        }
        else {
            return Response.badRequest();
        }
    }

    @Override
    protected SchoolClass loadObject(int id) {
        return pensenData.getSchoolClassById(id);
    }
}
