/*
 * Copyright (C) 2022 - 2026 by Sebastian Forster, Stefan Rothe
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
package ch.kinet.pensen.data;

import ch.kinet.Entity;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class Curriculum extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_CODE = "Code";
    public static final String DB_DESCRIPTION = "Description";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_CODE = "code";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_GRADES = "grades";
    private final List<Grade> grades = new ArrayList<>();
    private boolean archived;
    private String code;
    private String description;

    @PropertyInitializer({DB_ID})
    public Curriculum(int id) {
        super(id);
    }

    public boolean containsGrade(Grade grade) {
        return grades.contains(grade);
    }

    public boolean isArchived() {
        return archived;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Grade gradeFor(SchoolYear schoolYear, int graduationYear) {
        int yearsToGraduation = graduationYear - schoolYear.getGraduationYear() + 1;
        int size = grades.size();
        int index = size - yearsToGraduation;
        if (0 <= index && index < size) {
            return grades.get(index);
        }
        else {
            return null;
        }
    }

    public Stream<Grade> grades() {
        return grades.stream();
    }

    public Grade nextGrade(Grade grade) {
        int index = grades.indexOf(grade);
        if (index == -1 || index >= grades.size() - 1) {
            return null;
        }

        return grades.get(index + 1);
    }

    public Grade previousGrade(Grade grade) {
        int index = grades.indexOf(grade);
        if (index <= 0) {
            return null;
        }

        return grades.get(index - 1);
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_GRADES, JsonArray.createTerse(grades.stream()));
        return result;
    }

    @Override
    public String toString() {
        return getCode();
    }

    void addGrade(Grade grade) {
        grades.add(grade);
    }

    void sortGrades() {
        Collections.sort(grades);
    }
}
