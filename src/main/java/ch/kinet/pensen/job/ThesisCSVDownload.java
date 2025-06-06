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
import ch.kinet.pensen.data.ThesisType;
import ch.kinet.pensen.data.ValueMap;
import ch.kinet.pensen.server.Authorisation;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class ThesisCSVDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;

    public ThesisCSVDownload() {
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
        SortedMap<Teacher, ValueMap<ThesisType>> map = new TreeMap<>();
        pensenData.loadEmployments(schoolYear, null).forEachOrdered(employment -> {
            map.put(employment.getTeacher(), ValueMap.create());
        });

        pensenData.loadThesisEntries(schoolYear).forEachOrdered(entry -> {
            Teacher teacher = entry.getTeacher();
            if (map.containsKey(teacher)) {
                map.get(teacher).put(entry.getType(), entry.getCount());
            }
        });

        pensenData.loadEmployments(schoolYear, null).forEachOrdered(employment -> {
            Teacher teacher = employment.getTeacher();
            csv.append(teacher.getFirstName());
            csv.append(teacher.getLastName());
            csv.append(teacher.getCode());
            csv.append(employment.getDivision().getCode());
            pensenData.streamThesisTypes().forEach(thesisType -> {
                Optional<Double> count = map.get(teacher).get(thesisType);
                if (count.isPresent()) {
                    csv.append(count.get());
                }
                else {
                    csv.append();
                }
            });
        });
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        return Stream.concat(
            Stream.of(
                "Vorname",
                "Nachname",
                "Kürzel",
                "Abteilung"
            ),
            pensenData.streamThesisTypes().map(thesisType -> thesisType.getDescription())
        );
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Abschlussarbeiten_");
        result.append(schoolYear.getCode());
        result.append(".csv");
        return result.toString();
    }
}
