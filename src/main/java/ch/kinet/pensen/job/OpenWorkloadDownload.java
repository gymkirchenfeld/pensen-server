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
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SubjectCategory;
import ch.kinet.pensen.server.Authorisation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OpenWorkloadDownload extends JobImplementation {

    private Map<SubjectCategory, List<Course>> map;
    private PensenData pensenData;
    private SchoolYear schoolYear;

    public OpenWorkloadDownload() {
        super("Nicht zugeteilte Lektionen nach Fachgebiet");
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
        map = pensenData.streamSubjectCategories().collect(Collectors.toMap(Function.identity(), item -> new ArrayList<>()));
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation.isAuthenticated();
    }

    @Override
    public boolean parseData(JsonObject data) {
        schoolYear = pensenData.getSchoolYearById(data.getObjectId("schoolYear", -1));
        if (schoolYear != null) {
            loadMap();
        }

        return schoolYear != null;
    }

    @Override
    public long getStepCount() {
        return map.size() + 1;
    }

    @Override
    public void run(Account creator, JobCallback callback) {
        Document pdf = Document.createPortrait(fileName());
        callback.step();
        pensenData.streamSubjectCategories()
            .forEachOrdered(subjectCategory -> {
                List<Course> courses = map.get(subjectCategory);
                if (!courses.isEmpty()) {
                    SubjectCoursePDFGenerator.writePDF(pdf, subjectCategory.getDescription(), courses, true);
                }

                callback.step();
            });
        setProduct(pdf.toData());
    }

    private String fileName() {
        StringBuilder result = new StringBuilder();
        result.append("Nicht_zugeteilte_Pensen_");
        result.append(schoolYear.getCode());
        result.append(".pdf");
        return result.toString();
    }

    private void loadMap() {
        pensenData.loadAllCourses(schoolYear)
            .filter(course -> course.open())
            .forEachOrdered(course -> map.get(course.getSubject().getCategory()).add(course));
    }
}
