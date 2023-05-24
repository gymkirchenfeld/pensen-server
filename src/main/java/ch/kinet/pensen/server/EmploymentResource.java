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
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import java.util.HashSet;
import java.util.Set;

public final class EmploymentResource extends EntityResource<Employment> {

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
        if (query.hasKey("schoolYear")) {
            SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt("schoolYear", -1));
            if (schoolYear == null) {
                return Response.notFound();
            }

            return Response.jsonTerse(pensenData.loadEmployments(schoolYear, null));
        }

        if (query.hasKey("teacher")) {
            Teacher teacher = pensenData.getTeacherById(query.getInt("teacher", -1));
            if (teacher == null) {
                return Response.notFound();
            }

            return Response.jsonTerse(pensenData.loadTeacherHistory(teacher));
        }

        return Response.badRequest();
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
        Division division = pensenData.getDivisionById(data.getObjectId(Employment.JSON_DIVISION, -1));
        if (division == null) {
            return Response.badRequest();
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(data.getObjectId(Employment.JSON_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.badRequest();
        }

        if (schoolYear.isArchived()) {
            return Response.badRequest("Zu archivierten Schuljahren können keine Anstellungen hinzugefügt werden.");
        }

        Teacher teacher = pensenData.getTeacherById(data.getObjectId(Employment.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest();
        }

        String comments = data.getString(Employment.JSON_COMMENTS);
        double employmentMax = data.getDouble(Employment.JSON_EMPLOYMENT_MAX);
        double employmentMin = data.getDouble(Employment.JSON_EMPLOYMENT_MIN);
        if (employmentMin < 0 || employmentMax < 0) {
            return Response.badRequest("Negative Anstellungsprozente sind nicht erlaubt.");
        }

        double payment1 = data.getDouble(Employment.JSON_PAYMENT1);
        double payment2 = data.getDouble(Employment.JSON_PAYMENT2);
        if (payment1 < 0 || payment2 < 0) {
            return Response.badRequest("Negative Aunzahlungsprozente sind nicht erlaubt.");
        }

        boolean temporary = data.getBoolean(Employment.JSON_TEMPORARY, false);
        return Response.jsonVerbose(pensenData.createEmployment(
            schoolYear, teacher, division, employmentMax, employmentMin, payment1, payment2, temporary, comments
        ));
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        if (object.getSchoolYear().isArchived()) {
            return Response.badRequest("Anstellungen in archivierten Schuljahren können nicht verändert werden.");
        }

        String comments = data.getString(Employment.JSON_COMMENTS);
        Division division = pensenData.getDivisionById(data.getObjectId(Employment.JSON_DIVISION, -1));
        if (division == null) {
            return Response.badRequest();
        }

        double employmentMax = data.getDouble(Employment.JSON_EMPLOYMENT_MAX);
        double employmentMin = data.getDouble(Employment.JSON_EMPLOYMENT_MIN);
        if (employmentMin < 0 || employmentMax < 0) {
            return Response.badRequest("Negative Anstellungsprozente sind nicht erlaubt.");
        }

        double payment1 = data.getDouble(Employment.JSON_PAYMENT1);
        double payment2 = data.getDouble(Employment.JSON_PAYMENT2);
        if (payment1 < 0 || payment2 < 0) {
            return Response.badRequest("Negative Aunzahlungsprozente sind nicht erlaubt.");
        }

        boolean temporary = data.getBoolean(Employment.JSON_TEMPORARY, false);

        Set<String> changed = new HashSet<>();
        boolean recalculate = false;
        if (!Util.equal(object.getComments(), comments)) {
            object.setComments(comments);
            changed.add(Employment.DB_COMMENTS);
        }

        if (!Util.equal(object.getDivision(), division)) {
            object.setDivision(division);
            changed.add(Employment.DB_DIVISION);
        }

        if (!Util.equal(object.getEmploymentMax(), employmentMax)) {
            object.setEmploymentMax(employmentMax);
            changed.add(Employment.DB_EMPLOYMENT_MAX);
        }

        if (!Util.equal(object.getEmploymentMin(), employmentMin)) {
            object.setEmploymentMin(employmentMin);
            changed.add(Employment.DB_EMPLOYMENT_MIN);
        }

        if (!Util.equal(object.getPayment1(), payment1)) {
            object.setPayment1(payment1);
            changed.add(Employment.DB_PAYMENT1);
            recalculate = true;
        }

        if (!Util.equal(object.getPayment2(), payment2)) {
            object.setPayment2(payment2);
            changed.add(Employment.DB_PAYMENT2);
            recalculate = true;
        }

        if (!Util.equal(object.isTemporary(), temporary)) {
            object.setTemporary(temporary);
            changed.add(Employment.DB_TEMPORARY);
        }

        pensenData.updateEmployment(object, changed);
        if (recalculate) {
            pensenData.recalculateBalance(object);
        }

        return Response.jsonVerbose(data);
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

        pensenData.deleteEmployment(object);
        return Response.noContent();
    }

    @Override
    protected Employment loadObject(int id) {
        return pensenData.loadEmployment(id);
    }
}
