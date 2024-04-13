/*
 * Copyright (C) 2022 - 2024 by Sebastian Forster, Stefan Rothe
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

import ch.kinet.Binary;
import ch.kinet.Data;
import ch.kinet.Entity;
import ch.kinet.JsonObject;
import ch.kinet.reflect.PropertyInitializer;

public final class Division extends Entity {

    public static final String DB_CODE = "Code";
    public static final String DB_DESCRIPTION = "Description";
    public static final String DB_GROUPING = "Grouping";
    public static final String DB_HEAD_NAME = "HeadName";
    public static final String DB_HEAD_SIGNATURE = "HeadSignature";
    public static final String DB_HEAD_TITLE = "HeadTitle";
    public static final String DB_LOGO = "Logo";
    public static final String JSON_CODE = "code";
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_GROUPING = "grouping";
    public static final String JSON_HEAD_NAME = "headName";
    public static final String JSON_HEAD_SIGNATURE = "headSignature";
    public static final String JSON_HEAD_TITLE = "headTitle";
    public static final String JSON_LOGO = "logo";
    private String code;
    private String description;
    private String grouping;
    private String headName;
    private Binary headSignature;
    private String headTitle;
    private Binary logo;

    @PropertyInitializer({DB_ID})
    public Division(int id) {
        super(id);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getGrouping() {
        return grouping;
    }

    public String getHeadName() {
        return headName;
    }

    public Binary getHeadSignature() {
        return headSignature;
    }

    public String getHeadTitle() {
        return headTitle;
    }

    public Binary getLogo() {
        return logo;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGrouping(String grouping) {
        this.grouping = grouping;
    }

    public void setHeadName(String headName) {
        this.headName = headName;
    }

    public void setHeadSignature(Binary headSignature) {
        this.headSignature = headSignature;
    }

    public void setHeadTitle(String headTitle) {
        this.headTitle = headTitle;
    }

    public void setLogo(Binary logo) {
        this.logo = logo;
    }

    @Override
    public JsonObject toJsonTerse() {
        JsonObject result = super.toJsonTerse();
        result.put(JSON_CODE, code);
        result.put(JSON_DESCRIPTION, description);
        return result;
    }

    @Override
    public JsonObject toJsonVerbose() {
        JsonObject result = toJsonTerse();
        result.put(JSON_HEAD_NAME, headName);
        result.put(JSON_GROUPING, grouping);
        result.put(JSON_HEAD_TITLE, headTitle);
        result.putTerse(JSON_HEAD_SIGNATURE, Data.png(headSignature, "Unterschrift_" + code + ".png"));
        result.putTerse(JSON_LOGO, Data.png(logo, "logo_" + code + ".png"));
        return result;
    }
}
