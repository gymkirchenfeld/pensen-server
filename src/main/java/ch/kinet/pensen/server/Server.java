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

import ch.kinet.Binary;
import ch.kinet.JsonObject;
import ch.kinet.Mail;
import ch.kinet.http.Data;
import ch.kinet.http.Query;
import ch.kinet.http.Request;
import ch.kinet.http.Response;
import ch.kinet.http.Status;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;

public final class Server implements HttpHandler {

    private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final Map<String, String> defaultHeaders = createDefaultHeaders();

    public static void main(final String[] args) {
        int port = Configuration.getInstance().getHttpPort();
        Undertow server = Undertow.builder()
            .addHttpListener(port, "localhost")
            .setHandler(new Server())
            .build();
        server.start();
    }

    private static Map<String, String> createDefaultHeaders() {
        Map<String, String> result = new HashMap<>();
        result.put("Access-Control-Allow-Headers", "Authorization");
        result.put("Access-Control-Allow-Methods", "DELETE,GET,POST,PUT");
        result.put("Access-Control-Allow-Origin", "*");
        result.put("Access-Control-Max-Age", "0");
        result.put("Content-Security-Policy", "base-uri 'none'; connect-src 'none'; default-src 'none'; form-action 'none'; frame-ancestors 'none'; script-src 'none'");
        result.put("Strict-Transport-Security", "max-age=15552000; includeSubDomains; preload");
        return result;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();
        Response response;
        try {
            String method = exchange.getRequestMethod().toString();
            switch (method) {
                case METHOD_DELETE:
                    response = handleDelete(exchange);
                    break;
                case METHOD_POST:
                    response = handlePost(exchange);
                    break;
                case METHOD_GET:
                    response = handleGet(exchange);
                    break;
                case METHOD_OPTIONS:
                    response = Response.ok();
                    break;
                case METHOD_PUT:
                    response = handlePut(exchange);
                    break;
                default:
                    response = Response.methodNotAllowed();
                    break;
            }

        }
        catch (RuntimeException ex) {
            ex.printStackTrace();
            sendErrorNotification(ex);
            response = Response.internalServerError();
        }

        translateResponse(exchange, response);
        exchange.endExchange();
    }

    private Response handleDelete(HttpServerExchange exchange) {
        Request r = Request.createDelete(parseAuthorisation(exchange), exchange.getRequestPath(), parseQuery(exchange));
        return Router.getInstance().handleRequest(r);
    }

    private Response handleGet(HttpServerExchange exchange) {
        Request r = Request.createGet(parseAuthorisation(exchange), exchange.getRequestPath(), parseQuery(exchange));
        return Router.getInstance().handleRequest(r);
    }

    private Response handlePost(HttpServerExchange exchange) {
        JsonObject body = parseBody(exchange);
        if (body == null) {
            return Response.badRequest("Invalid body.");
        }

        Request r = Request.createPost(parseAuthorisation(exchange), exchange.getRequestPath(), body);
        return Router.getInstance().handleRequest(r);
    }

    private Response handlePut(HttpServerExchange exchange) {
        JsonObject body = parseBody(exchange);
        if (body == null) {
            return Response.badRequest("Invalid body.");
        }

        Request r = Request.createPut(parseAuthorisation(exchange), exchange.getRequestPath(), body);
        return Router.getInstance().handleRequest(r);
    }

    private String parseAuthorisation(HttpServerExchange exchange) {
        return exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
    }

    private JsonObject parseBody(HttpServerExchange exchange) {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getInputStream()));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            return JsonObject.create(builder.toString());
        }
        catch (org.json.JSONException ex) {
            return null;
        }
        catch (IOException ex) {
            return null;
        }
    }

    private Query parseQuery(HttpServerExchange exchange) {
        Map<String, String[]> queryParameters = new HashMap<>();
        for (Map.Entry<String, Deque<String>> entry : exchange.getQueryParameters().entrySet()) {
            queryParameters.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }

        return Query.create(queryParameters);
    }

    private void sendErrorNotification(Throwable exception) {
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
            catch (MessagingException ex) {
                // ignore
            }
        }
        catch (RuntimeException ex) {
            // ignore exception
        }
    }

    private void translateResponse(HttpServerExchange exchange, Response response) {
        HeaderMap headers = exchange.getResponseHeaders();
        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            headers.add(new HttpString(entry.getKey()), entry.getValue());
        }

        Data body = response.getBody();
        exchange.setStatusCode(response.getStatus());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, body.getMimeType());

        try {
            Binary bytes = body.getData();
            if (bytes != null && !bytes.isNull()) {
                exchange.getOutputStream().write(bytes.toBytes());
            }
        }
        catch (IOException ex) {
            exchange.setStatusCode(Status.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
            return;
        }

        // required!
        exchange.endExchange();
    }
}
