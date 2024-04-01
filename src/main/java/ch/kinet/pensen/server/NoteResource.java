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
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.Note;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.Teacher;

public final class NoteResource extends EntityResource<Note> {

    private static final String QUERY_TEACHER = "teacher";
    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        Teacher teacher = pensenData.getTeacherById(query.getInt(QUERY_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest();
        }

        return Response.jsonArrayTerse(pensenData.loadNotes(teacher).sorted());
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
        Teacher teacher = pensenData.getTeacherById(data.getObjectId(Note.JSON_TEACHER, -1));
        if (teacher == null) {
            return Response.badRequest();
        }

        String text = data.getString(Note.JSON_TEXT);
        pensenData.createNote(teacher, text, authorisation.getAccountName());
        return Response.created();
    }

    @Override
    protected boolean isDeleteAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response delete(Authorisation authorisation) {
        pensenData.deleteNote(object);
        return Response.noContent();
    }

    @Override
    protected Note loadObject(int id) {
        return pensenData.loadNote(id);
    }
}
