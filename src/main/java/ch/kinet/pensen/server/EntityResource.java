/*
 * Copyright (C) 2023 by Sebastian Forster, Stefan Rothe
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
 */package ch.kinet.pensen.server;

import ch.kinet.Entity;
import ch.kinet.Util;
import ch.kinet.http.Response;

public abstract class EntityResource<T extends Entity> extends ObjectResource {

    protected T object;

    @Override
    protected final Response parseResourceId(String resourceId) {
        int id = Util.parseInt(resourceId, -1);
        if (id < 0) {
            return Response.badRequest("Invalid resource identifier.");
        }

        object = loadObject(id);
        if (object == null) {
            return Response.notFound();
        }

        return null;
    }

    /**
     * Loads the object with the specified ID. If <code>null</code> is returned, a <i>not found</i> response is sent to
     * the client.
     *
     * @param id the object ID
     * @return the corresponding object or <code>null</code>
     */
    protected abstract T loadObject(int id);
}
