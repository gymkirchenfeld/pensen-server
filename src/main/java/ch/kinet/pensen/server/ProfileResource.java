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

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;

public final class ProfileResource extends GlobalResource {

    private static final String JSON_FEATURES = "features";
    private String[] clientFeatures;

    @Override
    public void initialize() {
        Configuration config = Configuration.getInstance();
        clientFeatures = config.getClientFeatures().split(",");
    }

    @Override
    protected boolean isGetAllowed(Authorisation auth, Query query) {
        return auth != null;
    }

    @Override
    protected Response get(Authorisation auth, Query query) {
        JsonArray features = JsonArray.create();
        for (String feature : clientFeatures) {
            if (!Util.isEmpty(feature)) {
                features.add(feature);
            }
        }

        JsonObject result = JsonObject.create();
        result.put(JSON_FEATURES, features);
        return Response.jsonVerbose(result);
    }
}
