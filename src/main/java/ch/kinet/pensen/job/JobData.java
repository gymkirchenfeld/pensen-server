/*
 * Copyright (C) 2022 - 2023 by Stefan Rothe
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
package ch.kinet.pensen.job;

import ch.kinet.BaseData;
import ch.kinet.DataManager;
import ch.kinet.Util;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.server.DB;
import ch.kinet.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class JobData extends BaseData {

    private final Map<String, Class<? extends JobImplementation>> jobRegistry = new HashMap<>();
    private final Map<Integer, Job> jobs = new HashMap<>();
    private int nextJobId = 0;
    private PensenData pensenData;

    @Override
    protected void doInitDependencies(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    protected void doInitLookups(Connection connection) {
    }

    @Override
    protected void doInitData() {
        createGlobalJob(new CalculateBalances());
        createGlobalJob(new CheckDatabase());
        createGlobalJob(new InitializeSchoolYear());
        registerLocalJob(CourseTableCSVDownload.class);
        registerLocalJob(GroupingCSVDownload.class);
        registerLocalJob(EmploymentCSVDownload.class);
        registerLocalJob(OpenWorkloadDownload.class);
        registerLocalJob(PayrollCSVDownload.class);
        registerLocalJob(PoolCSVDownload.class);
        registerLocalJob(SubjectCourseDownload.class);
        registerLocalJob(TeacherCourseDownload.class);
        registerLocalJob(TeacherLessonSummaryCSVDownload.class);
        registerLocalJob(ThesisCSVDownload.class);
        registerLocalJob(UntisDownload.class);
        registerLocalJob(WorkloadDownload.class);
        registerLocalJob(WorkloadMail.class);
    }

    public Job createJob(String name) {
        if (Util.isEmpty(name)) {
            return null;
        }

        try {
            final Class<? extends JobImplementation> jobClass = jobRegistry.get(name);
            if (jobClass == null) {
                return null;
            }

            JobImplementation implementation = (JobImplementation) jobClass.getDeclaredConstructor().newInstance();
            implementation.initialize(DB.getDataManager());
            return doCreateJob(implementation, false);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteJob(Job job) {
        jobs.remove(job.getJobId());
    }

    public Job getJobById(int id) {
        return jobs.get(id);
    }

    public Stream<Job> getJobs() {
        return jobs.values().stream();
    }

    private void createGlobalJob(JobImplementation implementation) {
        doCreateJob(implementation, true);
    }

    private Job doCreateJob(JobImplementation implementation, boolean global) {
        Job job = new Job(nextJobId, global, implementation);
        jobs.put(nextJobId, job);
        ++nextJobId;
        return job;
    }

    private void registerLocalJob(Class<? extends JobImplementation> jobClass) {
        jobRegistry.put(jobClass.getSimpleName(), jobClass);
    }
}
