/*
 * Copyright (C) 2023 by Stefan Rothe
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
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SemesterEnum;
import ch.kinet.pensen.data.Teacher;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class TeacherLessonSummaryCSVDownload extends JobImplementation {

    private PensenData pensenData;
    private SchoolYear schoolYear;

    public TeacherLessonSummaryCSVDownload() {
        super("Unterrichtspensum");
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
        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        Map<Teacher, Entry> map = new HashMap<>();
        pensenData.loadAllCourses(schoolYear).forEachOrdered(course -> {
            course.teachers().forEachOrdered(teacher -> {
                if (!map.containsKey(teacher)) {
                    map.put(teacher, new Entry());
                }

                Entry entry = map.get(teacher);
                entry.lessons1 += course.lessonsFor(teacher, SemesterEnum.First);
                entry.lessons2 += course.lessonsFor(teacher, SemesterEnum.Second);
                entry.percent1 += course.percentFor(teacher, SemesterEnum.First);
                entry.percent2 += course.percentFor(teacher, SemesterEnum.Second);
            });
        });

        callback.step();
        CsvWriter csv = CsvWriter.create(createHeaders());
        map.keySet().stream().sorted().forEachOrdered(teacher -> {
            Entry entry = map.get(teacher);
            csv.append(teacher.getCode());
            csv.append(teacher.getLastName());
            csv.append(teacher.getFirstName());
            csv.append(roundPercent(entry.lessons1));
            csv.append(roundPercent(entry.percent1));
            csv.append(roundPercent(entry.lessons2));
            csv.append(roundPercent(entry.percent2));
        });

        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders() {
        return Stream.of(
            "Kürzel",
            "Nachname",
            "Vorname",
            "1. Semester L",
            "1. Semester %",
            "2. Semester L",
            "2. Semester %"
        );
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Unterrichtspensum ");
        result.append(schoolYear.getCode());
        result.append(".csv");
        return result.toString();
    }

    private double roundPercent(double percent) {
        return Math.round(percent * 1000) / 1000.0;
    }

    private static final class Entry {

        double lessons1;
        double percent1;
        double lessons2;
        double percent2;
    }
}
