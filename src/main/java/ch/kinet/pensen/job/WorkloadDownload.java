/*
 * Copyright (C) 2022 by Stefan Rothe
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
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Employment;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.calculation.Workload;

public final class WorkloadDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;
    private Workload workload;
    private Workloads workloads;

    public WorkloadDownload() {
        super("Pensenblatt exportieren");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null;
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        Teacher teacher = pensenData.getTeacherById(data.getObjectId("teacher", -1));
        Division division = pensenData.getDivisionById(data.getObjectId("division", -1));
        if (schoolYear == null) {
            return false;
        }

        if (teacher == null) {
            workloads = pensenData.loadWorkloads(schoolYear, division);
        }
        else {
            Employment employment = pensenData.loadEmployment(schoolYear, teacher);
            if (employment == null) {
                return false;
            }

            workload = pensenData.loadWorkload(employment);
        }

        return true;
    }

    @Override
    public long getStepCount() {
        return workloads == null ? 1 : workloads.size();
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        Document pdf = Document.createPortrait(fileName());
        if (workloads == null) {
            WorkloadPDFGenerator.createPDF(pdf, workload);
            PostingsPDFGenerator.createPDF(pdf, workload);
            callback.step();
        }
        else {
            workloads.teachers().forEachOrdered(teacher -> {
                Workload workload = workloads.getWorkload(teacher);
                WorkloadPDFGenerator.createPDF(pdf, workload);
                PostingsPDFGenerator.createPDF(pdf, workload);
                callback.step();
            });
        }

        setProduct(pdf.toData());
        callback.step();
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        if (workloads == null) {
            result.append("Pensenblatt_");
            result.append(workload.getTeacher().getCode());
            result.append('_');
        }
        else {
            result.append("Pensenblätter_");
        }

        result.append(schoolYear.getCode());
        result.append(".pdf");
        return result.toString();
    }
}
