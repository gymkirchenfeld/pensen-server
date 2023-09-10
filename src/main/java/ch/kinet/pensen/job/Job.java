/*
 * Copyright (C) 2022 - 2023 by Stefan Rothe
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

import ch.kinet.Data;
import ch.kinet.Json;
import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.Log;
import ch.kinet.Util;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.server.DB;

public final class Job implements Json, JobCallback {

    public static final String JSON_ERROR = "error";
    public static final String JSON_ID = "id";
    public static final String JSON_LOG = "log";
    public static final String JSON_NAME = "name";
    public static final String JSON_PRODUCT = "product";
    public static final String JSON_PROGRESS = "progress";
    public static final String JSON_RUNNING = "running";
    public static final String JSON_TITLE = "title";
    private Authorisation creator;
    private final boolean global;
    private final int id;
    private final Log log = Log.create();
    private final JobImplementation implementation;
    private final Object lock = new Object();
    private long totalCount;
    private int doneCount;
    private RuntimeException exception;
    private String productId;
    private Thread thread;

    public Job(int id, boolean global, JobImplementation implementation) {
        this.global = global;
        this.id = id;
        this.implementation = implementation;
        this.doneCount = 0;
        this.totalCount = 1;
    }

    public Authorisation getCreator() {
        return creator;
    }

    public int getJobId() {
        return id;
    }

    public RuntimeException getException() {
        synchronized (lock) {
            return exception;
        }
    }

    public String getProductId() {
        synchronized (lock) {
            return productId;
        }
    }

    public boolean isAllowed(Authorisation authorisation) {
        if (global) {
            return authorisation != null;
        }
        else {
            return Util.equal(authorisation, creator);
        }
    }

    public boolean isFinished() {
        synchronized (lock) {
            return thread == null;
        }
    }

    public boolean isGlobal() {
        return global;
    }

    public boolean start(Authorisation creator, JsonObject data) {
        if (!isFinished()) {
            return false;
        }

        synchronized (lock) {
            log.clear();
            this.creator = creator;
            implementation.initialize(DB.getDataManager());
            if (!implementation.parseData(data)) {
                return false;
            }

            doneCount = 0;
            totalCount = implementation.getStepCount();
            exception = null;
            thread = new JobThread(this, implementation);
            thread.start();
            return true;
        }
    }

    @Override
    public void step() {
        synchronized (lock) {
            ++doneCount;
        }
    }

    @Override
    public final void info(String message, Object... args) {
        synchronized (lock) {
            log.info(message, args);
        }
    }

    @Override
    public JsonObject toJsonTerse() {
        synchronized (lock) {
            JsonObject result = JsonObject.create();
            result.put(JSON_ID, id);
            result.put(JSON_NAME, implementation.getName());
            result.put(JSON_TITLE, implementation.getTitle());
            result.put(JSON_PRODUCT, productId);
            if (implementation.hasError()) {
                result.put(JSON_ERROR, implementation.getErrorMessage());
            }

            result.put(JSON_RUNNING, !isFinished());
            if (!isFinished()) {
                result.put(JSON_PROGRESS, getProgress());
            }

            result.put(JSON_LOG, JsonArray.createVerbose(log.streamEntries()));
            return result;
        }
    }

    @Override
    public JsonObject toJsonVerbose() {
        return toJsonTerse();
    }

    public void succeeded(Data result) {
        synchronized (lock) {
            this.exception = null;
            if (result != null) {
                this.productId = DB.getFileStorage().addTemporaryFile(result);
            }

            this.thread = null;
        }
    }

    public void failed(RuntimeException exception) {
        synchronized (lock) {
            this.exception = exception;
            this.implementation.setErrorMessage(exception.toString());
            exception.printStackTrace();
            this.productId = null;
            this.thread = null;
        }
    }

    private long getProgress() {
        return 100 * doneCount / totalCount;
    }
}
