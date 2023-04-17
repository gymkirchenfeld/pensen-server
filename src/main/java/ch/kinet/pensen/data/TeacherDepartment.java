/*
 * Copyright (C) 2022 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;

public final class TeacherDepartment {

    public static final String DB_SUBJECT_CATEGORY = "SubjectCategory";
    public static final String DB_TEACHER = "Teacher";
    private final SubjectCategory subjectCategory;
    private final Teacher teacher;

    @PropertyInitializer({DB_SUBJECT_CATEGORY, DB_TEACHER})
    public TeacherDepartment(SubjectCategory subjectCategory, Teacher teacher) {
        this.subjectCategory = subjectCategory;
        this.teacher = teacher;
    }

    @Persistence(key = true)
    public SubjectCategory getSubjectCategory() {
        return subjectCategory;
    }

    @Persistence(key = true)
    public Teacher getTeacher() {
        return teacher;
    }
}
