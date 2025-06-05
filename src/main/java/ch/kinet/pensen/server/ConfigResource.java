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
import ch.kinet.http.Query;
import ch.kinet.http.Response;

public final class ConfigResource extends GlobalResource {

    private static final String JSON_CLIENT_ID = "clientId";
    private static final String JSON_MSAL = "msal";
    private static final String JSON_PERCENT_DECIMALS = "percentDecimals";
    private static final String JSON_TENANT_ID = "tenantId";
    private static final String JSON_TOKEN_REFRESH_OFFSET_MINUTES = "tokenRefreshOffsetMinutes";
    private static final String JSON_VERSION = "version";
    private String clientId;
    private String tenantId;
    private int percentDecimals;

    @Override
    public void initialize() {
        Configuration config = Configuration.getInstance();
        clientId = config.getMicrosoftClient();
        tenantId = config.getMicrosoftTenant();
        percentDecimals = config.getPercentDecimals();
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return true;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        JsonObject msal = JsonObject.create();
        msal.put(JSON_CLIENT_ID, clientId);
        msal.put(JSON_TENANT_ID, tenantId);

        JsonObject result = JsonObject.create();
        result.put(JSON_MSAL, msal);
        result.put(JSON_PERCENT_DECIMALS, percentDecimals);
        result.put(JSON_TOKEN_REFRESH_OFFSET_MINUTES, 10);
        result.put(JSON_VERSION, Version.VERSION);
        return Response.jsonVerbose(result);
    }
}
