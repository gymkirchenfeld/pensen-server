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

import java.util.HashMap;
import java.util.Map;

public final class Routes {

    private final Map<String, Class<? extends AbstractRequestHandler>> resourceClasses = new HashMap<>();

    private static final Routes INSTANCE = new Routes();

    static Class<? extends AbstractRequestHandler> getResource(String endpoint) {
        return INSTANCE.resourceClasses.get(endpoint);
    }

    private Routes() {
        addResource("calculationmode", ch.kinet.pensen.server.CalculationModeResource.class);
        addResource("config", ch.kinet.pensen.server.ConfigResource.class);
        addResource("course", ch.kinet.pensen.server.CourseResource.class);
        addResource("coursetable", ch.kinet.pensen.server.CourseTableResource.class);
        addResource("curriculum", ch.kinet.pensen.server.CurriculumResource.class);
        addResource("defaultlessons", ch.kinet.pensen.server.DefaultLessonsResource.class);
        addResource("division", ch.kinet.pensen.server.DivisionResource.class);
        addResource("employment", ch.kinet.pensen.server.EmploymentResource.class);
        addResource("job", ch.kinet.pensen.server.JobResource.class);
        addResource("gender", ch.kinet.pensen.server.GenderResource.class);
        addResource("grade", ch.kinet.pensen.server.GradeResource.class);
        addResource("note", ch.kinet.pensen.server.NoteResource.class);
        addResource("payrolltype", ch.kinet.pensen.server.PayrollTypeResource.class);
        addResource("poolentry", ch.kinet.pensen.server.PoolEntryResource.class);
        addResource("pooltype", ch.kinet.pensen.server.PoolTypeResource.class);
        addResource("posting", ch.kinet.pensen.server.PostingResource.class);
        addResource("postingtype", ch.kinet.pensen.server.PostingTypeResource.class);
        addResource("profile", ch.kinet.pensen.server.ProfileResource.class);
        addResource("schoolclass", ch.kinet.pensen.server.SchoolClassResource.class);
        addResource("schoolyear", ch.kinet.pensen.server.SchoolYearResource.class);
        addResource("settings", ch.kinet.pensen.server.SettingsResource.class);
        addResource("subject", ch.kinet.pensen.server.SubjectResource.class);
        addResource("subjectcategory", ch.kinet.pensen.server.SubjectCategoryResource.class);
        addResource("subjecttype", ch.kinet.pensen.server.SubjectTypeResource.class);
        addResource("teacher", ch.kinet.pensen.server.TeacherResource.class);
        addResource("thesis", ch.kinet.pensen.server.ThesisResource.class);
        addResource("thesistype", ch.kinet.pensen.server.ThesisTypeResource.class);
        addResource("workload", ch.kinet.pensen.server.WorkloadResource.class);
    }

    private void addResource(String endpoint, Class<? extends AbstractRequestHandler> resourceClass) {
        resourceClasses.put(endpoint, resourceClass);
    }
}
