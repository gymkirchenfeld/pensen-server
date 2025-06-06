/*
 * Copyright (C) 2022 - 2025 by Stefan Rothe
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
import ch.kinet.pensen.job.Job;
import ch.kinet.pensen.job.JobData;

public final class JobResource extends ObjectResource {

    private JobData jobData;
    private Job job;

    @Override
    public void initialize() {
        jobData = getData(JobData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        return Response.jsonArrayTerse(jobData.getJobs());
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return job.isAllowed(authorisation);
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        if (job.isFinished() && !job.isGlobal()) {
            jobData.deleteJob(job);
        }

        return Response.jsonVerbose(job);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        job = jobData.createJob(authorisation, data.getString(Job.JSON_NAME));
        if (job == null) {
            return Response.badRequest("Invalid job name.");
        }

        if (!job.isFinished()) {
            return Response.badRequest("Job is already running.");
        }

        if (job.start(authorisation.getAccount(), data)) {
            return Response.jsonVerbose(job);
        }
        else {
            return Response.badRequest("Invalid parameters.");
        }
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation.isEditAllowed();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        if (job.start(authorisation.getAccount(), data)) {
            return Response.noContent();
        }
        else {
            return Response.badRequest();
        }
    }

    @Override
    protected Response parseResourceId(String resourceId) {
        job = jobData.getJobById(Util.parseInt(resourceId, -1));
        if (job == null) {
            return Response.notFound();
        }

        return null;
    }
}
