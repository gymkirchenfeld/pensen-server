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
 */package ch.kinet.pensen.server;

import ch.kinet.Mailer;
import ch.kinet.SshConnection;
import ch.kinet.Util;
import ch.kinet.sql.DbSpec;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the application configuration.
 */
public final class Configuration {

    private static final String CLIENT_FEATURES = "client.features";
    private static final String DB_NAME = "db.name";
    private static final String DB_PORT = "db.port";
    private static final String DB_SERVER = "db.server";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_SCHEMA = "db.schema";
    private static final String DB_USER = "db.user";
    private static final String HTTP_PORT = "http.port";
    private static final String TEST_ENABLED = "test.enabled";
    private static final String TEST_MAIL_TO = "test.mailto";
    private static final String MICROSOFT_CLIENT = "microsoft.client";
    private static final String MICROSOFT_TENANT = "microsoft.tenant";
    private static final String PERCENT_DECIMALS = "percentDecimals";
    private static final String PROXY_SERVER = "proxy.server";
    private static final String PROXY_USER = "proxy.user";
    private static final String SERVER_WORKER_THREADS = "server.workerthreads";
    private static final String SERVER_IO_THREADS = "server.iothreads";
    private static final String SMTP_FROM = "smtp.from";
    private static final String SMTP_PORT = "smtp.port";
    private static final String SMTP_SERVER = "smtp.server";
    private static final String SUPPORT_MAIL = "support.mail";
    private final String dbSchema;
    private final DbSpec dbSpec;
    private final Properties properties;
    private SshConnection sshConnection;

    private static final Configuration INSTANCE = new Configuration();

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private Configuration() {
        String configFile = System.getProperty("config.file");
        System.out.println("Loading configuration from " + configFile);
        properties = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        dbSchema = getString(DB_SCHEMA);
        dbSpec = DbSpec.create(DbSpec.Dbms.Postgresql);
        dbSpec.setDatabase(getString(DB_NAME));
        dbSpec.setPort(getInt(DB_PORT, 5432));
        dbSpec.setDbServer(getString(DB_SERVER));
        dbSpec.setUserName(getString(DB_USER));
        String password = getString(DB_PASSWORD);
        if (!Util.isEmpty(password)) {
            dbSpec.setPassword(password.toCharArray());
        }

        dbSpec.setSslEnabled(true);

        String proxyServer = getString(PROXY_SERVER);
        if (!Util.isEmpty(proxyServer)) {
            System.out.println("Using tunnel via " + proxyServer);
            sshConnection = new SshConnection(getString(PROXY_USER), proxyServer);

            int dbTunnelPort = dbSpec.getPort() + 1;
            sshConnection.addTunnel(dbTunnelPort, dbSpec.getDbServer(), dbSpec.getPort());
            dbSpec.setPort(dbTunnelPort);
            dbSpec.setDbServer("localhost");
            sshConnection.connect();
        }
    }

    public Mailer createMailer(String mailFrom) {
        if (mailFrom == null) {
            mailFrom = getString(SMTP_FROM);
        }

        return Mailer.createMailer(getString(SMTP_SERVER), getInt(SMTP_PORT, 25), mailFrom);
    }

    public String getClientFeatures() {
        return getString(CLIENT_FEATURES);
    }

    public DbSpec getDbSpec() {
        return dbSpec;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public int getHttpPort() {
        return getInt(HTTP_PORT, 9001);
    }

    public String getMicrosoftClient() {
        return getString(MICROSOFT_CLIENT);
    }

    public String getMicrosoftTenant() {
        return getString(MICROSOFT_TENANT);
    }

    public int getPercentDecimals() {
        return getInt(PERCENT_DECIMALS, 3);
    }

    public int getServerIoThreads() {
        return getInt(SERVER_IO_THREADS, 2);
    }

    public int getServerWorkerThreads() {
        return getInt(SERVER_WORKER_THREADS, 10);
    }

    public String getSupportMail() {
        return getString(SUPPORT_MAIL);
    }

    public String getTestMailRecipient() {
        return getString(TEST_MAIL_TO);
    }

    public boolean isTestSystem() {
        return getBoolean(TEST_ENABLED);
    }

    private boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    private int getInt(String key, int defaultValue) {
        return Util.parseInt(properties.getProperty(key), defaultValue);
    }

    private String getString(String key) {
        String result = properties.getProperty(key);
        if (Util.isEmpty(result)) {
            return "";
        }

        result = result.trim();
        int len = result.length();
        if (result.charAt(0) == '"' && result.charAt(len - 1) == '"') {
            return result.substring(1, len - 1);
        }

        return result;
    }
}
