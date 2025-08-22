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

import ch.kinet.Mail;
import ch.kinet.http.MicrosoftKeys;
import ch.kinet.http.Request;
import ch.kinet.http.Response;
import ch.kinet.http.ServerImplementation;
import ch.kinet.pensen.data.Account;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.sql.StatementPreparationException;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.PublicKey;

public final class Server implements ServerImplementation<Authorisation> {

    private final MicrosoftKeys keys = MicrosoftKeys.create(Configuration.getInstance().getMicrosoftTenant());

    public static void main(final String[] args) {
        int port = Configuration.getInstance().getHttpPort();
        int ioThreads = Configuration.getInstance().getServerIoThreads();
        int workerThreads = Configuration.getInstance().getServerWorkerThreads();
        ch.kinet.http.Server.start(port, ioThreads, workerThreads, new Server());
    }

    @Override
    public Authorisation checkAuthorisation(Claims claims) {
        String applicationId = claims.get("aud").toString();
        if (!Configuration.getInstance().getMicrosoftClient().equals(applicationId)) {
            return null;
        }

        String accountName = claims.get("unique_name").toString();
        Account account = DB.getDataManager().getData(PensenData.class).getAccountByName(accountName);
        return account == null ? null : new Authorisation(account);
    }

    @Override
    public PublicKey getSigningKey(String keyId) {
        return keys.getSigningKey(keyId);
    }

    @Override
    public Authorisation publicAuthorisation() {
        return new Authorisation(null);
    }

    @Override
    public Response handleRequest(Request request) {
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
            response = requestHandler.handleRequest(request, resourceId);
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
