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
import ch.kinet.pensen.data.CourseTable;
import ch.kinet.pensen.data.Division;
import ch.kinet.pensen.data.Grade;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SubjectCategory;
import ch.kinet.pensen.server.Authorisation;
import java.util.stream.Stream;

public final class CourseTableCSVDownload extends JobImplementation {

    private Division division;
    private Grade grade;
    private PensenData pensenData;
    private SchoolYear schoolYear;
    private SubjectCategory subjectCategory;

    public CourseTableCSVDownload() {
        super("Planungsübersicht");
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
        grade = pensenData.getGradeById(data.getObjectId("grade", -1));
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        subjectCategory = pensenData.getSubjectCategoryById(data.getObjectId("subjectCategory", -1));
        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return 2;
    }

    @Override
    public void run(Account creator, JobCallback callback) {
        CourseTable table = pensenData.loadCourseTable(schoolYear, division, grade, subjectCategory);
        CsvWriter csv = CsvWriter.create(createHeaders(table));
        callback.step();
        table.subjects().forEachOrdered(subject -> {
            csv.append(subject.getCode());
            table.schoolClasses().forEachOrdered(schoolClass -> {
                csv.append(table.getEntry(subject, schoolClass).toString());
            });
        });
        setProduct(csv.toData(fileName()));
    }

    private Stream<String> createHeaders(CourseTable table) {
        return Stream.concat(
            Stream.of("Fach"),
            table.schoolClasses().map(schoolClass -> schoolClass.getCode())
        );
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Planungsübersicht ");
        result.append(schoolYear.getCode());
        if (division != null) {
            result.append("-");
            result.append(division.getCode());
        }

        if (grade != null) {
            result.append("-");
            result.append(grade.getCode());
        }

        if (subjectCategory != null) {
            result.append("-");
            result.append(subjectCategory.getCode());
        }

        result.append(".csv");
        return result.toString();
    }
}
