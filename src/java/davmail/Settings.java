package davmail;

import davmail.ui.tray.DavGatewayTray;

import java.util.Properties;
import java.io.*;

import org.apache.log4j.*;

/**
 * Settings facade
 */
public class Settings {
    private Settings() {
    }

    private static final Properties SETTINGS = new Properties();
    private static String configFilePath;
    private static boolean isFirstStart;

    public static synchronized void setConfigFilePath(String value) {
        configFilePath = value;
    }

    public static synchronized boolean isFirstStart() {
        return isFirstStart;
    }

    public static synchronized void load(InputStream inputStream) throws IOException {
        SETTINGS.load(inputStream);
    }

    public static synchronized void load() {
        FileInputStream fileInputStream = null;
        try {
            if (configFilePath == null) {
                //noinspection AccessOfSystemProperties
                configFilePath = System.getProperty("user.home") + "/.davmail.properties";
            }
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                fileInputStream = new FileInputStream(configFile);
                load(fileInputStream);
            } else {
                isFirstStart = true;

                // first start : set default values, ports above 1024 for linux
                SETTINGS.put("davmail.url", "http://exchangeServer/exchange/");
                SETTINGS.put("davmail.popPort", "1110");
                SETTINGS.put("davmail.imapPort", "1143");
                SETTINGS.put("davmail.smtpPort", "1025");
                SETTINGS.put("davmail.caldavPort", "1080");
                SETTINGS.put("davmail.ldapPort", "1389");
                SETTINGS.put("davmail.keepDelay", "30");
                SETTINGS.put("davmail.sentKeepDelay", "90");
                SETTINGS.put("davmail.caldavPastDelay", "90");
                SETTINGS.put("davmail.allowRemote", Boolean.FALSE.toString());
                SETTINGS.put("davmail.bindAddress", "");
                SETTINGS.put("davmail.enableProxy", Boolean.FALSE.toString());
                SETTINGS.put("davmail.proxyHost", "");
                SETTINGS.put("davmail.proxyPort", "");
                SETTINGS.put("davmail.proxyUser", "");
                SETTINGS.put("davmail.proxyPassword", "");
                SETTINGS.put("davmail.server", Boolean.FALSE.toString());
                SETTINGS.put("davmail.server.certificate.hash", "");
                SETTINGS.put("davmail.ssl.keystoreType", "");
                SETTINGS.put("davmail.ssl.keystoreFile", "");
                SETTINGS.put("davmail.ssl.keystorePass", "");
                SETTINGS.put("davmail.ssl.keyPass", "");
                SETTINGS.put("davmail.ssl.pkcs11Library", "");
                SETTINGS.put("davmail.ssl.pkcs11Config", "");

                // logging
                SETTINGS.put("log4j.rootLogger", Level.WARN.toString());
                SETTINGS.put("log4j.logger.davmail", Level.DEBUG.toString());
                SETTINGS.put("log4j.logger.httpclient.wire", Level.WARN.toString());
                SETTINGS.put("log4j.logger.org.apache.commons.httpclient", Level.WARN.toString());
                SETTINGS.put("log4j.logFilePath", "");
                save();
            }
        } catch (IOException e) {
            DavGatewayTray.error(new BundleMessage("LOG_UNABLE_TO_LOAD_SETTINGS"), e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    DavGatewayTray.debug(new BundleMessage("LOG_ERROR_CLOGING_CONFIG_FILE"), e);
                }
            }
        }
        updateLoggingConfig();
    }

    public static void updateLoggingConfig() {
        String logFilePath = Settings.getProperty("davmail.logFilePath");
        // use default log file path on Mac OS X
        if (logFilePath == null && System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
            logFilePath = System.getProperty("user.home") + "/Library/Logs/DavMail/davmail.log";
        }

        Logger rootLogger = Logger.getRootLogger();
        try {
            if (logFilePath != null && logFilePath.length() > 0) {
                File logFile = new File(logFilePath);
                // create parent directory if needed
                File logFileDir = logFile.getParentFile();
                if (logFileDir != null && !logFileDir.exists()) {
                    if (!logFileDir.mkdirs()) {
                        DavGatewayTray.error(new BundleMessage("LOG_UNABLE_TO_CREATE_LOG_FILE_DIR"));
                        throw new IOException();
                    }
                }
            } else {
                logFilePath = "davmail.log";
            }
            // Build file appender
            RollingFileAppender fileAppender = ((RollingFileAppender) rootLogger.getAppender("FileAppender"));
            if (fileAppender == null) {
                fileAppender = new RollingFileAppender();
                fileAppender.setName("FileAppender");
                fileAppender.setMaxBackupIndex(2);
                fileAppender.setMaxFileSize("1MB");
                fileAppender.setLayout(new PatternLayout("%d{ISO8601} %-5p [%t] %c %x - %m%n"));
            }
            fileAppender.setFile(logFilePath, true, false, 8192);
            rootLogger.addAppender(fileAppender);
        } catch (IOException e) {
            DavGatewayTray.error(new BundleMessage("LOG_UNABLE_TO_SET_LOG_FILE_PATH"));
        }

        // update logging levels
        Settings.setLoggingLevel("rootLogger", Settings.getLoggingLevel("rootLogger"));
        Settings.setLoggingLevel("davmail", Settings.getLoggingLevel("davmail"));
        Settings.setLoggingLevel("httpclient.wire", Settings.getLoggingLevel("httpclient.wire"));
        Settings.setLoggingLevel("org.apache.commons.httpclient", Settings.getLoggingLevel("org.apache.commons.httpclient"));
    }

    public static synchronized void save() {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(configFilePath);
            SETTINGS.store(fileOutputStream, "DavMail settings");
        } catch (IOException e) {
            DavGatewayTray.error(new BundleMessage("LOG_UNABLE_TO_STORE_SETTINGS"), e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    DavGatewayTray.debug(new BundleMessage("LOG_ERROR_CLOSING_CONFIG_FILE"), e);
                }
            }
        }
        updateLoggingConfig();
    }

    public static synchronized String getProperty(String property) {
        return SETTINGS.getProperty(property);
    }

    public static synchronized void setProperty(String property, String value) {
        if (value != null) {
            SETTINGS.setProperty(property, value);
        } else {
            SETTINGS.setProperty(property, "");
        }
    }

    public static synchronized int getIntProperty(String property) {
        return getIntProperty(property, 0);
    }

    public static synchronized int getIntProperty(String property, int defaultValue) {
        int value = defaultValue;
        try {
            String propertyValue = SETTINGS.getProperty(property);
            if (propertyValue != null && propertyValue.length() > 0) {
                value = Integer.parseInt(propertyValue);
            }
        } catch (NumberFormatException e) {
            DavGatewayTray.error(new BundleMessage("LOG_INVALID_SETTING_VALUE", property), e);
        }
        return value;
    }

    public static synchronized boolean getBooleanProperty(String property) {
        String propertyValue = SETTINGS.getProperty(property);
        return Boolean.parseBoolean(propertyValue);
    }

    protected static String getLoggingPrefix(String category) {
        String prefix;
        if ("rootLogger".equals(category)) {
            prefix = "log4j.";
        } else {
            prefix = "log4j.logger.";
        }
        return prefix;
    }

    public static synchronized Level getLoggingLevel(String category) {
        String prefix = getLoggingPrefix(category);
        String currentValue = SETTINGS.getProperty(prefix + category);

        if (currentValue != null && currentValue.length() > 0) {
            return Level.toLevel(currentValue);
        } else if ("rootLogger".equals(category)) {
            return Logger.getRootLogger().getLevel();
        } else {
            return Logger.getLogger(category).getLevel();
        }
    }

    public static synchronized void setLoggingLevel(String category, Level level) {
        String prefix = getLoggingPrefix(category);
        SETTINGS.setProperty(prefix + category, level.toString());
        if ("rootLogger".equals(category)) {
            Logger.getRootLogger().setLevel(level);
        } else {
            Logger.getLogger(category).setLevel(level);
        }
    }

    public static synchronized void saveProperty(String property, String value) {
        Settings.load();
        Settings.setProperty(property, value);
        Settings.save();
    }

}
