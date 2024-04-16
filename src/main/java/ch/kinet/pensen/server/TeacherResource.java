/*
 * Copyright (C) 2022 - 2024 by Sebastian Forster, Stefan Rothe
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
package ch.kinet.pensen.server;

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Gender;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.SchoolYear;
import ch.kinet.pensen.data.SubjectCategory;
import ch.kinet.pensen.data.Teacher;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class TeacherResource extends EntityResource<Teacher> {

    private static final String QUERY_EMPLOYED = "employed";
    private static final String QUERY_SCHOOL_YEAR = "schoolYear";
    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        if (!query.hasKey(QUERY_SCHOOL_YEAR)) {
            return Response.jsonArrayVerbose(pensenData.streamTeachers());
        }

        SchoolYear schoolYear = pensenData.getSchoolYearById(query.getInt(QUERY_SCHOOL_YEAR, -1));
        if (schoolYear == null) {
            return Response.notFound();
        }

        boolean active = query.getBoolean(QUERY_EMPLOYED, true);
        if (active) {
            return Response.jsonArrayVerbose(pensenData.loadTeachersForSchoolYear(schoolYear));
        }

        Set<Teacher> activeTeachers = pensenData.loadTeachersForSchoolYear(schoolYear).collect(Collectors.toSet());
        return Response.jsonArrayVerbose(pensenData.streamTeachers().filter(teacher -> !activeTeachers.contains(teacher)));
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        JsonObject result = object.toJsonVerbose();
        for (String detail : query.getStrings("detail")) {
            switch (detail) {
                case "history":
                    result.put("history", JsonArray.createTerse(pensenData.loadTeacherHistory(object)));
                    break;
            }

        }

        return Response.jsonVerbose(result);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        Gender gender = pensenData.getGenderById(data.getObjectId(Teacher.JSON_GENDER, -1));
        if (gender == null) {
            return Response.badRequest();
        }

        LocalDate birthday = data.getLocalDate(Teacher.JSON_BIRTHDAY);
        String code = data.getString(Teacher.JSON_CODE);
        String email = data.getString(Teacher.JSON_EMAIL);
        String employeeNumber = data.getString(Teacher.JSON_EMPLOYEE_NUMBER);
        String firstName = data.getString(Teacher.JSON_FIRST_NAME);
        String lastName = data.getString(Teacher.JSON_LAST_NAME);
        String title = data.getString(Teacher.JSON_TITLE);

        Teacher result = pensenData.createTeacher(birthday, code, email, employeeNumber, firstName, lastName, title);
        Set<SubjectCategory> departments = parseSubjectCategories(data.getArray(Teacher.JSON_DEPARTMENTS));
        pensenData.updateTeacherDepartments(result, departments);
        return Response.created();
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isEditAllowed();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean archived = data.getBoolean(Teacher.JSON_ARCHIVED, false);
        LocalDate birthday = data.getLocalDate(Teacher.JSON_BIRTHDAY);
        String code = data.getString(Teacher.JSON_CODE);
        String email = data.getString(Teacher.JSON_EMAIL);
        String firstName = data.getString(Teacher.JSON_FIRST_NAME);
        Gender gender = pensenData.getGenderById(data.getObjectId(Teacher.JSON_GENDER, -1));
        String lastName = data.getString(Teacher.JSON_LAST_NAME);
        String title = data.getString(Teacher.JSON_TITLE);
        String employeeNumber = data.getString(Teacher.JSON_EMPLOYEE_NUMBER);

        if (data.hasKey(Teacher.JSON_DEPARTMENTS)) {
            Set<SubjectCategory> departments = parseSubjectCategories(data.getArray(Teacher.JSON_DEPARTMENTS));
            pensenData.updateTeacherDepartments(object, departments);
        }

        Set<String> changed = new HashSet<>();
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(Teacher.DB_ARCHIVED);
        }

        if (!Util.equal(object.getBirthday(), birthday)) {
            object.setBirthday(birthday);
            // TODO: Set employments dirty
            changed.add(Teacher.DB_BIRTHDAY);
        }

        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(Teacher.DB_CODE);
        }

        if (!Util.equal(object.getEmail(), email)) {
            object.setEmail(email);
            changed.add(Teacher.DB_EMAIL);
        }

        if (!Util.equal(object.getFirstName(), firstName)) {
            object.setFirstName(firstName);
            changed.add(Teacher.DB_FIRST_NAME);
        }

        if (!Util.equal(object.getGender(), gender)) {
            object.setGender(gender);
            changed.add(Teacher.DB_GENDER);
        }

        if (!Util.equal(object.getLastName(), lastName)) {
            object.setLastName(lastName);
            changed.add(Teacher.DB_LAST_NAME);
        }

        if (!Util.equal(object.getTitle(), title)) {
            object.setTitle(title);
            changed.add(Teacher.DB_TITLE);
        }

        if (!Util.equal(object.getEmployeeNumber(), employeeNumber)) {
            object.setEmployeeNumber(employeeNumber);
            changed.add(Teacher.DB_EMPLOYEE_NUMBER);
        }

        pensenData.updateTeacher(object, changed);
        return Response.noContent();
    }

    private Set<SubjectCategory> parseSubjectCategories(JsonArray json) {
        Set<SubjectCategory> result = new HashSet<>();
        if (json == null) {
            return result;
        }

        for (int i = 0; i < json.length(); ++i) {
            SubjectCategory subjectCategory = pensenData.getSubjectCategoryById(json.getObjectId(i, -1));
            if (subjectCategory != null) {
                result.add(subjectCategory);
            }
        }

        return result;
    }

    @Override
    protected Teacher loadObject(int id) {
        return pensenData.getTeacherById(id);
    }
}
