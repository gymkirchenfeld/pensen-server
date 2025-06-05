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

import ch.kinet.Mail;
import ch.kinet.Util;
import ch.kinet.http.Request;
import ch.kinet.http.RequestHandler;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.sql.StatementPreparationException;
import ch.kinet.webtoken.JJWT;
import ch.kinet.webtoken.MicrosoftKeys;
import ch.kinet.webtoken.Token;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Server implements RequestHandler {

    private final MicrosoftKeys keys = MicrosoftKeys.create(Configuration.getInstance().getMicrosoftTenant());

    public static void main(final String[] args) {
        int port = Configuration.getInstance().getHttpPort();
        ch.kinet.http.Server.start(port, new Server());
    }

    @Override
    public Response handleRequest(Request request) {
        Authorisation authorisation = authenticate(request.getAuthorisation());
        if (authorisation == null) {
            return Response.unauthorized();
        }

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
        catch (StatementPreparationException ex) {
            System.err.println("DB connection probably lost, shutting down.");
            ex.printStackTrace(System.err);
            System.exit(1);
            response = Response.internalServerError();
        }
        catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            response = Response.internalServerError();
            System.err.println("REQUEST: " + request.getPath());
            ex.printStackTrace(System.err);
        }

        return response;
    }

    @Override
    public void handleException(Throwable exception) {
        logException(exception);
        sendExceptionMail(exception);

    }

    private Authorisation authenticate(String authorisation) {
        if (!Util.startsWith(authorisation, "Bearer ")) {
            return new Authorisation(null);
        }

        Token token = JJWT.parseToken(authorisation.substring(7), keys);
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
                Account account = DB.getDataManager().getData(PensenData.class).getAccountByName(token.getAccountName());
                return account == null ? null : new Authorisation(account);
            default:
                System.out.println("Unknown token status.");
                return null;
        }
    }

    private void logException(Throwable exception) {
        exception.printStackTrace(System.err);
    }

    private void sendExceptionMail(Throwable exception) {
        String supportMail = Configuration.getInstance().getSupportMail();
        try (StringWriter out = new StringWriter()) {
            try (PrintWriter writer = new PrintWriter(out)) {
                exception.printStackTrace(writer);
                writer.flush();
                Mail mail = Mail.create();
                mail.addTo(Configuration.getInstance().getSupportMail());
                mail.setBody(out.toString());
                mail.setSubject("Interner Fehler in Pensenmanager");
                Configuration.getInstance().createMailer(null).sendMail(mail);
            }
        }
        catch (RuntimeException | IOException ex) {
            System.err.println("Cannot send exception mail to " + supportMail + ".");
            logException(ex);
        }
    }
}
