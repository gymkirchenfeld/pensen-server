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
package ch.kinet.pensen.data;

import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;

public final class Settings extends Entity {

    public static final String DB_ACCOUNT = "Account";
    public static final String DB_MAIL_BODY = "MailBody";
    public static final String DB_MAIL_FROM = "MailFrom";
    public static final String DB_MAIL_SUBJECT = "MailSubject";
    public static final String JSON_MAIL_BODY = "mailBody";
    public static final String JSON_MAIL_FROM = "mailFrom";
    public static final String JSON_MAIL_SUBJECT = "mailSubject";
    private final Authorisation account;
    private String mailBody;
    private String mailFrom;
    private String mailSubject;

    @PropertyInitializer({DB_ACCOUNT, DB_ID})
    public Settings(Authorisation account, int id) {
        super(id);
        this.account = account;
    }

    public Authorisation getAccount() {
        return account;
    }

    public String getMailBody() {
        return mailBody;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public void setMailBody(String mailBody) {
        this.mailBody = mailBody;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = JsonObject.create();
        result.put(JSON_MAIL_BODY, mailBody);
        result.put(JSON_MAIL_FROM, mailFrom);
        result.put(JSON_MAIL_SUBJECT, mailSubject);
        return result;
    }
}
