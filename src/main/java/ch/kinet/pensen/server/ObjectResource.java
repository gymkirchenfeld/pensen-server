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
package ch.kinet.pensen.server;

import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Request;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;

/**
 * Base class for object resources. An object resource usually represents an entity, i.e. a collection of objects
 * identified by an id.
 */
public abstract class ObjectResource extends AbstractRequestHandler {

    @Override
    public final Response handleRequest(Request request, Authorisation authorisation, String resourceId) {
        JsonObject body = request.getBody();
        Query query = request.getQuery();
        switch (request.getMethod()) {
            case Post:
                return handleCreate(authorisation, body);
            case Get:
                if (Util.isEmpty(resourceId)) {
                    return handleList(authorisation, query);
                }
        }

        Response response = parseResourceId(resourceId);
        if (response != null) {
            return response;
        }

        switch (request.getMethod()) {
            case Delete:
                return handleDelete(authorisation);
            case Get:
                return handleGet(authorisation, query);
            case Put:
                return handleUpdate(authorisation, body);
            default:
                return Response.methodNotAllowed();
        }
    }

    protected boolean isAllowed(Authorisation authorisation) {
        return false;
    }

    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return isAllowed(authorisation);
    }

    protected Response list(Authorisation authorisation, Query query) {
        return Response.methodNotAllowed();
    }

    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return isAllowed(authorisation);
    }

    protected Response get(Authorisation authorisation, Query query) {
        return Response.methodNotAllowed();
    }

    protected boolean isCreateAllowed(Authorisation authorisation, JsonObject data) {
        return isAllowed(authorisation);
    }

    protected Response create(Authorisation authorisation, JsonObject data) {
        return Response.methodNotAllowed();
    }

    protected boolean isUpdateAllowed(Authorisation authorisation, JsonObject data) {
        return isAllowed(authorisation);
    }

    protected Response update(Authorisation authorisation, JsonObject data) {
        return Response.methodNotAllowed();
    }

    protected boolean isDeleteAllowed(Authorisation authorisation) {
        return isAllowed(authorisation);
    }

    protected Response delete(Authorisation authorisation) {
        return Response.methodNotAllowed();
    }

    /**
     * Parses the resource ID.
     *
     * @param resourceId the ID string to be parsed
     * @return <tt>null</tt> or an error response
     */
    protected abstract Response parseResourceId(String resourceId);

    private Response handleList(Authorisation authorisation, Query query) {
        if (!isListAllowed(authorisation, query)) {
            return Response.forbidden();
        }

        return list(authorisation, query);
    }

    private Response handleGet(Authorisation authorisation, Query query) {
        if (!isGetAllowed(authorisation, query)) {
            return Response.forbidden();
        }

        return get(authorisation, query);
    }

    private Response handleCreate(Authorisation authorisation, JsonObject json) {
        if (!isCreateAllowed(authorisation, json)) {
            return Response.forbidden();
        }

        return create(authorisation, json);
    }

    private Response handleUpdate(Authorisation authorisation, JsonObject json) {
        if (!isUpdateAllowed(authorisation, json)) {
            return Response.forbidden();
        }

        return update(authorisation, json);
    }

    private Response handleDelete(Authorisation authorisation) {
        if (!isDeleteAllowed(authorisation)) {
            return Response.forbidden();
        }

        return delete(authorisation);
    }
}
