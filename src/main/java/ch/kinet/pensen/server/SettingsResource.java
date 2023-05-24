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
package ch.kinet.pensen.server;

import ch.kinet.JsonObject;
import ch.kinet.Util;
import ch.kinet.http.Query;
import ch.kinet.http.Response;
import ch.kinet.pensen.data.Authorisation;
import ch.kinet.pensen.data.PensenData;
import ch.kinet.pensen.data.Settings;
import java.util.HashSet;
import java.util.Set;

public final class SettingsResource extends GlobalResource {

    private PensenData pensenData;

    @Override
    public void initialize() {
        pensenData = getData(PensenData.class);
    }

    @Override
    protected boolean isGetAllowed(Authorisation authorisation, Query query) {
        return authorisation != null;
    }

    @Override
    protected Response get(Authorisation authorisation, Query query) {
        return Response.jsonVerbose(pensenData.loadSettings(authorisation));
    }

    @Override
    protected boolean isPostAllowed(Authorisation authorisation, JsonObject data) {
        return authorisation != null && authorisation.isAdmin();
    }

    @Override
    protected Response post(Authorisation authorisation, JsonObject data) {
        Settings settings = pensenData.loadSettings(authorisation);
        Set<String> changed = new HashSet<>();
        String mailBody = data.getString(Settings.JSON_MAIL_BODY);
        if (!Util.equal(settings.getMailBody(), mailBody)) {
            settings.setMailBody(mailBody);
            changed.add(Settings.DB_MAIL_BODY);
        }

        String mailFrom = data.getString(Settings.JSON_MAIL_FROM);
        if (!Util.equal(settings.getMailFrom(), mailFrom)) {
            settings.setMailFrom(mailFrom);
            changed.add(Settings.DB_MAIL_FROM);
        }

        String mailSubject = data.getString(Settings.JSON_MAIL_SUBJECT);
        if (!Util.equal(settings.getMailSubject(), mailSubject)) {
            settings.setMailSubject(mailSubject);
            changed.add(Settings.DB_MAIL_SUBJECT);
        }

        pensenData.updateSettings(settings, changed);
        return Response.jsonVerbose(data);
    }
}
