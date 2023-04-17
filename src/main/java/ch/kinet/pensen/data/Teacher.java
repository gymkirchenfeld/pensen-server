/*
 * Copyright (C) 2022 - 2023 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Date;
import ch.kinet.Entity;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Teacher extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_BIRTHDAY = "Birthday";
    public static final String DB_CODE = "Code";
    public static final String DB_EMAIL = "Email";
    public static final String DB_EMPLOYEE_NUMBER = "EmployeeNumber";
    public static final String DB_FIRST_NAME = "FirstName";
    public static final String DB_GENDER = "Gender";
    public static final String DB_LAST_NAME = "LastName";
    public static final String DB_TITLE = "Title";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_BIRTHDAY = "birthday";
    public static final String JSON_CODE = "code";
    public static final String JSON_DEPARTMENTS = "departments";
    public static final String JSON_EMAIL = "email";
    public static final String JSON_EMPLOYEE_NUMBER = "employeeNumber";
    public static final String JSON_FIRST_NAME = "firstName";
    public static final String JSON_GENDER = "gender";
    public static final String JSON_LAST_NAME = "lastName";
    public static final String JSON_TITLE = "title";

    private final SortedSet<SubjectCategory> departments = new TreeSet<>();
    private boolean archived;
    private Date birthday;
    private String code;
    private String email;
    private String employeeNumber;
    private String firstName;
    private Gender gender;
    private String lastName;
    private String title;

    @PropertyInitializer({DB_ID})
    public Teacher(int id) {
        super(id);
    }

    public final int ageOn(Date date) {
        if (getBirthday() == null) {
            return -1;
        }

        int result = date.getYear() - birthday.getYear();
        if ((birthday.getMonth() > date.getMonth()) ||
            (birthday.getMonth() == date.getMonth() && birthday.getDay() > date.getDay())) {
            result--;
        }

        return result;
    }

    public final Date getBirthday() {
        return birthday;
    }

    public final String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public final String getEmployeeNumber() {
        return employeeNumber;
    }

    public final String getFirstName() {
        return firstName;
    }

    public Gender getGender() {
        return gender;
    }

    public final String getLastName() {
        return lastName;
    }

    public final String getTitle() {
        return title;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CODE, code);
        result.put(JSON_FIRST_NAME, firstName);
        result.put(JSON_LAST_NAME, lastName);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.put(JSON_BIRTHDAY, birthday);
        result.put(JSON_DEPARTMENTS, JsonArray.createTerse(departments.stream()));
        result.put(JSON_EMAIL, email);
        result.put(JSON_EMPLOYEE_NUMBER, employeeNumber);
        result.putTerse(JSON_GENDER, gender);
        result.put(JSON_TITLE, title);
        return result;
    }

    @Persistence(ignore = true)
    SortedSet<SubjectCategory> getDepartments() {
        return departments;
    }

    @Override
    protected int doCompare(Entity entity) {
        int result = 0;
        if (entity instanceof Teacher) {
            Teacher other = (Teacher) entity;
            result = Util.compare(code, other.code);
        }

        if (result == 0) {
            result = super.doCompare(entity);
        }

        return result;
    }
}
