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

import ch.kinet.JsonObject;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;

public final class ConfigResource extends GlobalResource {

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
    protected Response get(Authorisation auth, Query query) {
        JsonObject msal = JsonObject.create();
        msal.put("clientId", clientId);
        msal.put("tenantId", tenantId);

        JsonObject result = JsonObject.create();
        result.put("msal", msal);
        result.put("percentDecimals", percentDecimals);
        result.put("tokenRefreshOffsetMinutes", 10);
        result.put("version", Version.VERSION);
        return Response.json(result);
    }
}
