/*
 * Copyright (C) 2023 - 2025 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.CalculationMode;
import ch.kinet.pensen.data.PensenData;

public final class CalculationModeResource extends EntityResource<CalculationMode> {

    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isListAllowed(Authorisation authorisation, Query query) {
        return authorisation.isAuthenticated();
    }

    @Override
    protected Response list(Authorisation authorisation, Query query) {
        return Response.jsonArrayTerse(pensenData.streamCalculationModes());
    }

    @Override
    protected CalculationMode loadObject(int id) {
        return pensenData.getCalculationModeById(id);
    }
}
