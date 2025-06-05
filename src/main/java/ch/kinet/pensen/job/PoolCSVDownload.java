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
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.Teacher;
import ch.kinet.pensen.server.Authorisation;
import java.util.stream.Stream;

public final class PoolCSVDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;

    public PoolCSVDownload() {
        super("Poolliste");
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
        pensenData.loadPoolEntries(schoolYear).forEachOrdered(entry -> {
            Teacher teacher = entry.getTeacher();
            csv.append(teacher.getFirstName());
            csv.append(teacher.getLastName());
            csv.append(teacher.getCode());
            csv.append(entry.getDescription());
            csv.append(entry.getType().getDescription());
            csv.append(entry.getPercent1());
            csv.append(entry.getPercent2());
        });
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        return Stream.of(
            "Vorname",
            "Nachname",
            "Kürzel",
            "Beschreibung",
            "Typ",
            "Prozent 1. Semester",
            "Prozent 2. Semester"
        );
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Pooleinträge_");
        result.append(schoolYear.getCode());
        result.append(".csv");
        return result.toString();
    }
}
