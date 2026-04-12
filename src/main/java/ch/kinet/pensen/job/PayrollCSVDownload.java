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
import ch.kinet.csv.CsvWriter;
import ch.kinet.pensen.calculation.Payroll;
import ch.kinet.pensen.calculation.Workload;
import ch.kinet.pensen.calculation.Workloads;
import ch.kinet.pensen.data.*;
import ch.kinet.pensen.server.Authorisation;

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
        return authorisation.isAuthenticated();
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
    public void run(Account creator, JobCallback callback) {
        boolean calculationModeIsLessons2 = CalculationMode.toEnum(schoolYear.getCalculationMode()) == CalculationMode.Enum.lessons2;
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
            if (calculationModeIsLessons2) {
                csv.append(roundPercent(workload.payroll().percent().get(semester), 2));
            } else {
                csv.append(roundPercent(workload.payroll().percent().get(semester)));
            }
            csv.append(roundPercent(workload.getClosingBalance()));
            if (calculationModeIsLessons2) {
                Payroll.IpbCorrectionData ipbCorrection = payroll.getIpbCorrection(semester);
                pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                    if (payrollType.isLessonBased()) {
                        if (ipbCorrection != null && payrollType.equals(ipbCorrection.type())) {
                            csv.append(ipbCorrection.lessonsWithoutCorrection());
                            csv.append(workload.getEmployment().withoutAgeRelief(semester, ipbCorrection.percentWithoutCorrection()));
                        } else {
                            Payroll.Item item = payroll.getItem(payrollType);
                            if (item == null) {
                                csv.append();
                                csv.append();
                            } else {
                                csv.append(item.lessons().get(semester));
                                csv.append(workload.getEmployment().withoutAgeRelief(semester, item.percent().get(semester)));
                            }
                        }
                    }
                });
                pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                    if (!payrollType.isLessonBased()) {
                        Payroll.Item item = payroll.getItem(payrollType);
                        if (item == null) {
                            csv.append();
                        } else {
                            csv.append(workload.getEmployment().withoutAgeRelief(semester, item.percent().get(semester)));
                        }
                    }
                });
                pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                    if (payrollType.isIpbCorrectionAllowed()) {
                        if (ipbCorrection != null && payrollType.equals(ipbCorrection.type())) {
                            csv.append(ipbCorrection.ipbCorrectionLessons());
                        } else {
                            csv.append();
                        }
                    }
                });
            } else {
                pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                    Payroll.Item item = payroll.getItem(payrollType);
                    if (payrollType.isLessonBased()) {
                        if (item == null) {
                            csv.append();
                        } else {
                            csv.append(item.lessons().get(semester));
                        }
                    }

                    if (item == null) {
                        csv.append();
                    } else {

                        csv.append(item.percent().get(semester));
                    }
                });
            }
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
        result.add("IPB-Saldo Ende SJ");
        if (CalculationMode.toEnum(schoolYear.getCalculationMode()) == CalculationMode.Enum.lessons2) {
            pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                if (payrollType.isLessonBased()) {
                    result.add("S " + payrollType.getCode() + " L");
                    result.add("S " + payrollType.getCode() + " %");
                }
            });
            pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                if (!payrollType.isLessonBased()) {
                    result.add("S " + payrollType.getCode() + " %");
                }
            });
            pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                if (payrollType.isIpbCorrectionAllowed()) {
                    result.add("S IPBKorr " + payrollType.getCode() + " L");
                }
            });
        } else {
            pensenData.streamPayrollTypes().forEachOrdered(payrollType -> {
                if (payrollType.isLessonBased()) {
                    result.add(payrollType.getCode() + " L");
                }
                result.add(payrollType.getCode() + " %");
            });
        }
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
        return roundPercent(percent, 3);
    }

    private double roundPercent(double percent, int n) {
        double factor = Math.pow(10, n);
        return Math.round(percent * factor) / factor;
    }
}
