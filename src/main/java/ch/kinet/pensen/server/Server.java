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

import ch.kinet.Mail;
import ch.kinet.http.Request;
import ch.kinet.http.RequestHandler;
import ch.kinet.http.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Server implements RequestHandler {

    public static void main(final String[] args) {
        int port = Configuration.getInstance().getHttpPort();
        ch.kinet.http.Server.start(port, new Server());
    }

    @Override
    public Response handleRequest(Request request) {
        return Router.getInstance().handleRequest(request);
    }

    @Override
    public void handleException(Throwable exception) {
        try {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            exception.printStackTrace(writer);
            writer.flush();
            Mail mail = Mail.create();
            mail.addTo(Configuration.getInstance().getSupportMail());
            mail.setBody(out.toString());
            mail.setSubject("Interner Fehler in Pensenmanager");
            try {
                Configuration.getInstance().createMailer(null).sendMail(mail);
            }
            catch (RuntimeException ex) {
                // ignore
            }
        }
        catch (RuntimeException ex) {
            // ignore exception
        }
    }
}
