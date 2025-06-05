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
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.server.Authorisation;
import java.util.stream.Stream;

public final class EmploymentCSVDownload extends JobImplementation {

    private Division division;
    private PensenData pensenData;
    private SchoolYear schoolYear;

    public EmploymentCSVDownload() {
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
        division = pensenData.getDivisionById(data.getObjectId("division", -1));
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Account creator, JobCallback callback) {
        CsvWriter csv = CsvWriter.create(createHeaders());
        callback.step();
        pensenData.loadEmployments(schoolYear, division).forEachOrdered(employment -> {
            Teacher teacher = employment.getTeacher();
            csv.append(teacher.getId());
            csv.append(teacher.getTitle());
            csv.append(teacher.getFirstName());
            csv.append(teacher.getLastName());
            csv.append(teacher.getCode());
            csv.append(employment.getDivision().getCode());
            csv.append(teacher.getEmail());
            csv.append(teacher.getEmployeeNumber());
            csv.append(employment.isTemporary() ? "ja" : "nein");
            csv.append(employment.ageReliefFactor(SemesterEnum.First));
            csv.append(employment.ageReliefFactor(SemesterEnum.Second));
            csv.append(Format.percent(employment.getEmploymentMin(), false));
            csv.append(Format.percent(employment.getEmploymentMax(), false));
            csv.append(Format.percent(employment.getPayment1(), false));
            csv.append(Format.percent(employment.getPayment2(), false));
            csv.append(Format.percent(employment.getOpeningBalance(), false));
            csv.append(Format.percent(employment.getClosingBalance() - employment.getOpeningBalance(), false));
            csv.append(Format.percent(employment.getClosingBalance(), false));
        });
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        return Stream.of(
            "Id",
            "Titel",
            "Vorname",
            "Nachname",
            "Kürzel",
            "Abteilung",
            "E-Mail-Adresse",
            "Mitarbeiternummer",
            "befristet",
            "Altersentlastung S1",
            "Altersentlastung S2",
            "Verfügung min.",
            "Verfügung max.",
            "Auszahlung S1",
            "Auszahlung S2",
            "IPB-Anfangssaldo",
            "IPB-Veränderung",
            "IPB-Schlusssaldo"
        );
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Lehrpersonen_");
        result.append(schoolYear.getCode());
        if (division != null) {
            result.append('_');
            result.append(division.getCode());
        }

        result.append(".csv");
        return result.toString();
    }
}
