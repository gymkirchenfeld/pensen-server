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

import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;

public final class WorkloadResource extends GlobalResource {

    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
    private static final String QUERY_TEACHER = "teacher";
    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.notFound();
        }

        Teacher teacher = pensenData.getTeacherById(query.getInt(QUERY_TEACHER, -1));
        if (teacher == null) {
            return Response.jsonArrayTerse(pensenData.loadEmployments(schoolYear, null)
                .map(employment -> pensenData.loadWorkload(employment))
            );
        }
        else {
            Employment employment = pensenData.loadEmployment(schoolYear, teacher);
            if (employment == null) {
                return Response.notFound();
            }

            return Response.jsonVerbose(pensenData.loadWorkload(employment));
        }
    }
}
