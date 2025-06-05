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

import ch.kinet.BaseData;
import ch.kinet.http.Request;
import ch.kinet.http.Response;

public abstract class AbstractRequestHandler {

    public abstract void initialize();

    public abstract Response handleRequest(Request request, Authorisation authorisation, String resourceId);

    protected final <T extends BaseData> T getData(Class<T> clazz) {
        return DB.getDataManager().getData(clazz);
    }
}
