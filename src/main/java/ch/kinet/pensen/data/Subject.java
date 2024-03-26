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
package ch.kinet.pensen.data;

import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.reflect.Persistence;
import ch.kinet.reflect.PropertyInitializer;

public final class Subject extends Entity {

    public static final String DB_ARCHIVED = "Archived";
    public static final String DB_CATEGORY = "Category";
    public static final String DB_CODE = "Code";
    public static final String DB_CROSS_CLASS = "CrossClass";
    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_EVENTO_CODE = "EventoCode";
    public static final String DB_SORT_ORDER = "SortOrder";
    public static final String DB_TYPE = "Type";
    public static final String JSON_ARCHIVED = "archived";
    public static final String JSON_CATEGORY = "category";
    public static final String JSON_CODE = "code";
    public static final String JSON_CROSS_CLASS = "crossClass";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_EVENTO_CODE = "eventoCode";
    public static final String JSON_SORT_ORDER = "sortOrder";
    public static final String JSON_TYPE = "type";

    private boolean archived;
    private SubjectCategory category;
    private String code;
    private boolean crossClass;
    private String description;
    private String eventoCode;
    private int sortOrder;
    private SubjectType type;

    @PropertyInitializer({DB_ID})
    public Subject(int id) {
        super(id);
    }

    @Override
    public int compareTo(Entity entity) {
        if (entity instanceof Subject) {
            Subject other = (Subject) entity;
            return Util.compare(sortOrder, other.sortOrder);
        }
        else {
            return super.compareTo(entity);
        }
    }

    public boolean filter(SubjectCategory category) {
        return category == null || Util.equal(this.category, category);
    }

    public SubjectCategory getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getEventoCode() {
        return eventoCode;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public SubjectType getType() {
        return type;
    }

    public boolean isArchived() {
        return archived;
    }

    @Persistence(ignore = true)
    public boolean isClassLesson() {
        return code.equals("KS") || code.equals("KL");
    }

    public boolean isCrossClass() {
        return crossClass;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setCategory(SubjectCategory category) {
        this.category = category;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setCrossClass(boolean crossClass) {
        this.crossClass = crossClass;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEventoCode(String eventoCode) {
        this.eventoCode = eventoCode;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setType(SubjectType type) {
        this.type = type;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_ARCHIVED, archived);
        result.putTerse(JSON_CATEGORY, category);
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        result.put(JSON_SORT_ORDER, sortOrder);
        result.putTerse(JSON_TYPE, type);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_CROSS_CLASS, crossClass);
        result.put(JSON_EVENTO_CODE, eventoCode);
        return result;
    }

    @Override
    public String toString() {
        return getCode();
    }

    @Override
    protected int doCompare(Entity entity) {
        int result = 0;
        if (entity instanceof Subject) {
            Subject other = (Subject) entity;
            result = Util.compare(getCode(), other.getCode());
        }

        if (result == 0) {
            result = super.doCompare(entity);
        }

        return result;
    }
}
