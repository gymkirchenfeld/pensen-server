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
import ch.kinet.pensen.data.Course;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SubjectCategory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SubjectCourseDownload extends JobImplementation {

    private final SortedSet<String> divisionGrouping;
    private final Map<String, Map<SubjectCategory, List<Course>>> map = new HashMap<>();
    private final List<Course> crossClassCourses = new ArrayList<>();
    private PensenData pensenData;
    private SchoolYear schoolYear;
    private int steps = 1;

    public SubjectCourseDownload() {
        super("Lektionenzuteilungen nach Fachgebiet");
        divisionGrouping = new TreeSet<>();
    }

    @Override
    public void initialize(DataManager dataManager) {
        pensenData = dataManager.getData(PensenData.class);
        pensenData.streamDivisions().forEachOrdered(division -> {
            String grouping = division.getGrouping();
            divisionGrouping.add(grouping);
            Map<SubjectCategory, List<Course>> subMap = new HashMap<>();
            pensenData.streamSubjectCategories().forEachOrdered(subjectCategory -> {
                subMap.put(subjectCategory, new ArrayList<>());
                steps += 1;
            });
            map.put(grouping, subMap);
        });
    }

    @Override
    public boolean isAllowed(Authorisation authorisation) {
        return authorisation != null;
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
        return steps;
    }

    @Override
    public void run(Authorisation creator, JobCallback callback) {
        Document pdf = Document.createLandscape(fileName());
        SubjectCoursePDFGenerator.writePDF(pdf, "Gesamtschulische Kurse", crossClassCourses, false);
        callback.step();
        divisionGrouping.forEach(grouping -> {
            pensenData.streamSubjectCategories().forEachOrdered(subjectCategory -> {
                List<Course> courses = map.get(grouping).get(subjectCategory);
                if (!courses.isEmpty()) {
                    SubjectCoursePDFGenerator.writePDF(pdf, title(grouping, subjectCategory), courses, false);
                }

                callback.step();
            });
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

    private void loadMap() {
        pensenData.loadAllCourses(schoolYear).forEachOrdered(course -> {
            if (course.isCrossClass()) {
                crossClassCourses.add(course);
            }
            else {
                course.divisions().forEachOrdered(division -> {
                    map.get(division.getGrouping()).get(course.getSubject().getCategory()).add(course);
                });
            }
        });
    }

    private static String title(String grouping, SubjectCategory subjectCategory) {
        StringBuilder result = new StringBuilder();
        result.append(subjectCategory.getDescription());
        result.append(' ');
        result.append(grouping);
        return result.toString();
    }
}
