/*
 * Copyright (C) 2022 - 2025 by Sebastian Forster, Stefan Rothe
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

/**
 * Base class for global resources. Global reports don't depend on specific objects and do not accept a resource
 * identifier. Global reports can be customized using HTTP query parameters.
 */
public abstract class GlobalResource extends AbstractRequestHandler {

    @Override
    public final Response handleRequest(Request<Authorisation> request, String resourceId) {
        if (!Util.isEmpty(resourceId)) {
            return Response.badRequest();
        }

        Query query = request.getQuery();
        Authorisation authorisation = request.getAuthorisation();
        switch (request.getMethod()) {
            case Get:
                return handleGet(authorisation, query);
            case Post:
                return handlePost(authorisation, request.getBody().toJsonTerse());
            default:
                return Response.methodNotAllowed();
        }
    }

    protected boolean isAllowed(Authorisation authorisation) {
        return false;
    }

    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return isAllowed(authorisation);
    }

    protected Response get(Authorisation authorisation, Query query) {
        return Response.badRequest();
    }

    protected boolean isPostAllowed(Authorisation authorisation, JsonObject data) {
        return isAllowed(authorisation);
    }

    protected Response post(Authorisation authorisation, JsonObject data) {
        return Response.badRequest();
    }

    private Response handleGet(Authorisation authorisation, Query query) {
        if (!isGetAllowed(authorisation, query)) {
            return Response.forbidden();
        }

        return get(authorisation, query);
    }

    private Response handlePost(Authorisation authorisation, JsonObject data) {
        if (!isPostAllowed(authorisation, data)) {
            return Response.forbidden();
        }

        return post(authorisation, data);
    }
}
