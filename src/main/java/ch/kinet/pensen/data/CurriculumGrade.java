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

import ch.kinet.reflect.PropertyInitializer;

public final class CurriculumGrade {

    public static final String DB_CURRICULUM = "Curriculum";
    public static final String DB_GRADE = "Grade";
    private final Curriculum curriculum;
    private final Grade grade;

    @PropertyInitializer({DB_CURRICULUM, DB_GRADE})
    public CurriculumGrade(Curriculum curriculum, Grade grade) {
        this.curriculum = curriculum;
        this.grade = grade;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public Grade getGrade() {
        return grade;
    }
}
