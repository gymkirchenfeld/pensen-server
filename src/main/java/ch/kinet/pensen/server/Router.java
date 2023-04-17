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
package ch.kinet.pensen.server;

import ch.kinet.Util;
import ch.kinet.http.Request;
import ch.kinet.http.Response;
import ch.kinet.jjwt.JJWT;
import ch.kinet.jjwt.Token;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Router {

    private static final Router INSTANCE = new Router();
    private final MicrosoftKeys keys = MicrosoftKeys.create(Configuration.getInstance().getMicrosoftTenant());

    public static Router getInstance() {
        return INSTANCE;
    }

    private Router() {
    }

    public Response handleRequest(Request request) {
        Authorisation authorisation = authenticate(request.getAuthorisation());
        String[] pathParts = request.getPath().split("/");
        // part 0 is empty, since the path starts with an /
        if (pathParts.length < 2) {
            return Response.badRequest("Invalid resource name.");
        }

        String resourceName = pathParts[1];
        String resourceId = null;
        if (pathParts.length == 3) {
            resourceId = pathParts[2];
        }

        String error = null;
        Class<? extends AbstractRequestHandler> resourceClass = Routes.getResource(resourceName);
        if (resourceClass == null) {
            return Response.badRequest("Invalid resource name.");
        }

        Response response;
        try {
            AbstractRequestHandler requestHandler = resourceClass.getDeclaredConstructor().newInstance();
            requestHandler.initialize();
            response = requestHandler.handleRequest(request, authorisation, resourceId);
        }
        catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            error = sw.toString();
            response = Response.internalServerError();
            System.err.println("REQUEST: " + request.getPath());
            ex.printStackTrace(System.err);
        }

        return response;
    }

    private Authorisation authenticate(String authorisation) {
        if (!Util.startsWith(authorisation, "Bearer ")) {
            return null;
        }

        Token token = JJWT.parseToken(authorisation.substring(7), keys);
        if (Configuration.getInstance().isDebugEnabled()) {
            System.out.println("Received token");
            System.out.println(token);
        }

        switch (token.getStatus()) {
            case Expired:
                System.out.println("Token is expired.");
                return null;
            case InvalidSignature:
                System.out.println("Token has an invalid signature.");
                return null;
            case Malformed:
                System.out.println("Token is malformed.");
                return null;
            case Unsupported:
                System.out.println("Token is not supported.");
                return null;
            case Valid:
                return DB.getDataManager().getData(PensenData.class).getAuthorisationByName(token.getAccountName());
            default:
                System.out.println("Unknown token status.");
                return null;
        }
    }
}
