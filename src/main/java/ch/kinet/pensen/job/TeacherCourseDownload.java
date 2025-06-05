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
package ch.kinet.pensen.job;

import ch.kinet.DataManager;
import ch.kinet.JsonObject;
import ch.kinet.pdf.Document;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.server.Authorisation;

public final class TeacherCourseDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;
    private Workloads workloads;

    public TeacherCourseDownload() {
        super("Lektionenzuteilungen nach Lehrperson");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation.isAuthenticated();
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        if (schoolYear != null) {
            workloads = pensenData.loadWorkloads(schoolYear, null);
        }

        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return workloads.size();
    }

    @Override
    public void run(Account creator, JobCallback callback) {
        Document pdf = Document.createPortrait(fileName());
        workloads.teachers().forEachOrdered(teacher -> {
            TeacherCoursePDFGenerator.writePDF(pdf, workloads.getWorkload(teacher));
            callback.step();
        });
        setProduct(pdf.toData());
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Lektionenzuteilungen_");
        result.append(schoolYear.getCode());
        result.append(".pdf");
        return result.toString();
    }
}
