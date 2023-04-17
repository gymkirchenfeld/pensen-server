/*
 * Copyright (C) 2022 by Stefan Rothe
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
package ch.kinet.pensen.job;

import ch.kinet.DataManager;
import ch.kinet.JsonObject;
import ch.kinet.http.Data;
import ch.kinet.pensen.data.Authorisation;

public abstract class JobImplementation {

    private String errorMessage;
    private final String title;
    private Data product;

    protected JobImplementation(String title) {
        this.title = title;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public final String getName() {
        return getClass().getSimpleName();
    }

    public final Data getProduct() {
        return product;
    }

    public final String getTitle() {
        return title;
    }

    public final boolean hasError() {
        return errorMessage != null;
    }

    public abstract void initialize(DataManager dataManager);

    public abstract boolean isAllowed(Authorisation authorisation);

    public abstract boolean parseData(JsonObject data);

    public abstract long getStepCount();

    public abstract void run(Authorisation creator, JobCallback callback);

    protected final void setProduct(Data product) {
        this.product = product;
    }

    protected final void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
