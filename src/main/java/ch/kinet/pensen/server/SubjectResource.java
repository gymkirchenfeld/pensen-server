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

import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.Subject;
import ch.kinet.pensen.data.SubjectCategory;
import ch.kinet.pensen.data.SubjectType;
import java.util.HashSet;
import java.util.Set;

public final class SubjectResource extends EntityResource<Subject> {

    private static final String QUERY_CROSS_CLASS = "crossClass";
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
        if (query.hasKey(QUERY_CROSS_CLASS)) {
            boolean crossClass = query.getBoolean(QUERY_CROSS_CLASS, true);
            return Response.jsonArrayTerse(pensenData.streamSubjects()
                .filter(item -> !item.isArchived() && (item.isCrossClass() == crossClass))
            );
        }

        return Response.jsonArrayTerse(pensenData.streamSubjects());
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(object);
    }

    @Override
    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        SubjectCategory category = pensenData.getSubjectCategoryById(data.getObjectId(Subject.JSON_CATEGORY, -1));
        if (category == null) {
            return Response.badRequest();
        }

        String code = data.getString(Subject.JSON_CODE);
        boolean crossClass = data.getBoolean(Subject.JSON_CROSS_CLASS, false);
        String description = data.getString(Subject.JSON_DESCRIPTION);
        String eventoCode = data.getString(Subject.JSON_EVENTO_CODE);
        SubjectType type = pensenData.getSubjectTypeById(data.getObjectId(Subject.JSON_TYPE, -1));
        if (type == null) {
            return Response.badRequest();
        }

        pensenData.createSubject(category, code, crossClass, description, eventoCode, type);
        return Response.noContent();
    }

    @Override
    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean archived = data.getBoolean(Subject.JSON_ARCHIVED, false);
        SubjectCategory category = pensenData.getSubjectCategoryById(data.getObjectId(Subject.JSON_CATEGORY, -1));
        if (category == null) {
            return Response.badRequest();
        }

        String code = data.getString(Subject.JSON_CODE);
        boolean crossClass = data.getBoolean(Subject.JSON_CROSS_CLASS, false);
        String description = data.getString(Subject.JSON_DESCRIPTION);
        String eventoCode = data.getString(Subject.JSON_EVENTO_CODE);
        int sortOrder = data.getInt(Subject.JSON_SORT_ORDER);
        SubjectType type = pensenData.getSubjectTypeById(data.getObjectId(Subject.JSON_TYPE, -1));
        if (type == null) {
            return Response.badRequest();
        }

        Set<String> changed = new HashSet<>();
        if (!Util.equal(object.isArchived(), archived)) {
            object.setArchived(archived);
            changed.add(Subject.DB_ARCHIVED);
        }

        if (!Util.equal(object.getCategory(), category)) {
            object.setCategory(category);
            changed.add(Subject.DB_CATEGORY);
        }

        if (!Util.equal(object.getCode(), code)) {
            object.setCode(code);
            changed.add(Subject.DB_CODE);
        }

        if (!Util.equal(object.isCrossClass(), crossClass)) {
            object.setCrossClass(crossClass);
            changed.add(Subject.DB_CROSS_CLASS);
        }

        if (!Util.equal(object.getDescription(), description)) {
            object.setDescription(description);
            changed.add(Subject.DB_DESCRIPTION);
        }

        if (!Util.equal(object.getEventoCode(), eventoCode)) {
            object.setEventoCode(eventoCode);
            changed.add(Subject.DB_EVENTO_CODE);
        }

        if (!Util.equal(object.getSortOrder(), sortOrder)) {
            object.setSortOrder(sortOrder);
            changed.add(Subject.DB_SORT_ORDER);
        }

        if (!Util.equal(object.getType(), type)) {
            object.setType(type);
            changed.add(Subject.DB_TYPE);
        }

        pensenData.updateSubject(object, changed);
        return Response.noContent();
    }

    @Override
    protected Subject loadObject(int id) {
        return pensenData.getSubjectById(id);
    }
}
