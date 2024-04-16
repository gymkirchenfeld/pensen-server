/*
 * Copyright (C) 2024 by Stefan Rothe
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
import ch.kinet.pensen.data.PensenData;
import java.util.HashSet;
import java.util.Set;

public final class AuthorisationResource extends EntityResource<Authorisation> {

    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isAllowed(Authorisation authorisation) {
        return authorisation != null && authorisation.isGrantAllowed();
    }

    @Override
    protected Response list(Authorisation auth, Query query) {
        return Response.jsonArrayTerse(pensenData.streamAuthorisations().sorted());
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(object);
    }

    @Override
    protected Response create(Authorisation authorisation, JsonObject data) {
        String accountName = data.getString(Authorisation.JSON_ACCOUNT_NAME);
        boolean grantAllowed = data.getBoolean(Authorisation.JSON_GRANT_ALLOWED, false);
        boolean editAllowed = data.getBoolean(Authorisation.JSON_EDIT_ALLOWED, false);

        if (pensenData.getAuthorisationByName(accountName) != null) {
            return Response.badRequest("Ein Benutzer mit diesem Namen existiert bereits.");
        }

        pensenData.createAuthorisation(accountName, editAllowed, grantAllowed);
        return Response.created();
    }

    @Override
    protected Response update(Authorisation authorisation, JsonObject data) {
        boolean editAllowed = data.getBoolean(Authorisation.JSON_EDIT_ALLOWED, false);
        boolean grantAllowed = data.getBoolean(Authorisation.JSON_GRANT_ALLOWED, false);

        Set<String> changed = new HashSet<>();
        if (object.isEditAllowed() != editAllowed) {
            object.setEditAllowed(editAllowed);
            changed.add(Authorisation.DB_EDIT_ALLOWED);
        }

        if (object.isGrantAllowed() != grantAllowed) {
            object.setGrantAllowed(grantAllowed);
            changed.add(Authorisation.DB_GRANT_ALLOWED);
        }
        if (!changed.isEmpty()) {
            pensenData.updateAuthorisation(object, changed);
        }

        return Response.noContent();
    }

    @Override
    protected Response delete(Authorisation authorisation) {
        if (authorisation.equals(object)) {
            return Response.badRequest("Das eigene Benutzerkonto kann nicht gelöscht werden.");
        }

        pensenData.deleteAuthorisation(object);
        return Response.noContent();
    }

    @Override
    protected Authorisation loadObject(int id) {
        return pensenData.getAuthorisationById(id);
    }
}
