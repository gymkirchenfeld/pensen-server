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

import ch.kinet.DataManager;
import ch.kinet.JsonObject;
import ch.kinet.csv.CsvWriter;
import ch.kinet.pensen.calculation.Payroll;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class PayrollCSVDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;
    private SemesterEnum semester;

    public PayrollCSVDownload() {
        super("Lehrerliste");
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
        semester = pensenData.getSemesterById(data.getInt("semester", -1));
        return schoolYear != null && semester != null;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        CsvWriter csv = CsvWriter.create(createHeaders());
        csv.setHideZero(true);
        callback.step();
        Workloads workloads = pensenData.loadWorkloads(schoolYear, null);
        workloads.teachers().forEachOrdered(teacher -> {
            Workload workload = workloads.getWorkload(teacher);
            Payroll payroll = workload.payroll();
            csv.append(teacher.getEmployeeNumber());
            csv.append(teacher.getCode());
            csv.append(teacher.getLastName());
            csv.append(teacher.getFirstName());
            csv.append(teacher.getBirthday());
            csv.append(workload.ageReliefFactor(semester));
            csv.append(roundPercent(workload.payroll().percent().get(semester)));
            pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                Payroll.Item item = payroll.getItem(payrollType);
                if (schoolYear.lessonBased(payrollType)) {
                    if (item == null) {
                        csv.append();
                    }
                    else {
                        csv.append(item.lessons().get(semester));
                    }
                }

                if (item == null) {
                    csv.append();
                }
                else {

                    csv.append(item.percent().get(semester));
                }
            });
            //
        });
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        List<String> result = new ArrayList<>();
        result.add("Nr.");
        result.add("Kürzel");
        result.add("Nachname");
        result.add("Vorname");
        result.add("Geburtsdatum");
        result.add("Altersentlastung");
        result.add("Auszahlung");
        pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
            if (schoolYear.lessonBased(payrollType)) {
                result.add(payrollType.getCode() + " L");
            }

            result.add(payrollType.getCode() + " %");
        });

        return result.stream();
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Pensenmeldung ");
        result.append(schoolYear.getCode());
        result.append("-");
        result.append(semester.getId());
        result.append(".csv");
        return result.toString();
    }

    private double roundPercent(double percent) {
        return Math.round(percent * 1000) / 1000.0;
    }
}
