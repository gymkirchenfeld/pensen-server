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

import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.Timestamp;
import ch.kinet.Util;
import ch.kinet.reflect.PropertyInitializer;

public final class Note extends Entity {

    public static final String DB_CREATED_BY = "CreatedBy";
    public static final String DB_CREATED_ON = "CreatedOn";
    public static final String DB_TEACHER = "Teacher";
    public static final String DB_TEXT = "Text";
    public static final String JSON_CREATED_BY = "createdBy";
    public static final String JSON_CREATION = "creation";
    public static final String JSON_TEACHER = "teacher";
    public static final String JSON_TEXT = "text";
    private final String createdBy;
    private final Timestamp createdOn;
    private final Teacher teacher;
    private String text;

    @PropertyInitializer({DB_CREATED_BY, DB_CREATED_ON, DB_ID, DB_TEACHER})
    public Note(String createdBy, Timestamp createdOn, int id, Teacher teacher) {
        super(id);
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.teacher = teacher;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    protected int doCompare(Entity entity) {
        int result = 0;
        if (entity instanceof Note) {
            Note other = (Note) entity;
            result = -Util.compare(createdOn, other.createdOn);
        }

        if (result == 0) {
            result = super.doCompare(entity);
        }

        return result;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_ID, getId());
        result.put(JSON_CREATED_BY, createdBy);
        result.put(JSON_CREATION, createdOn);
        result.put(JSON_TEXT, text);
        return result;
    }
}
