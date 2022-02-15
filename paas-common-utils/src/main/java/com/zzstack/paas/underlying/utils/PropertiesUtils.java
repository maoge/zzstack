package com.zzstack.paas.underlying.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtils {

    private static Logger logger = LoggerFactory.getLogger(PropertiesUtils.class.getName());
    
    private static final String CONF_PATH = "conf";
    
    private Properties prop = null;

    private static HashMap<String, PropertiesUtils> prosMap;

    public static synchronized PropertiesUtils getInstance(String propName) {
        PropertiesUtils instance = null;

        if (prosMap == null) {
            prosMap = new HashMap<String, PropertiesUtils>();
        }

        instance = (PropertiesUtils) prosMap.get(propName);

        if (instance == null) {
            instance = new PropertiesUtils(propName);
            prosMap.put(propName, instance);
        }

        return instance;
    }

    private PropertiesUtils(String fileName) {
        fileName = String.format("conf/%s.properties", fileName);  // chkPropertiesName(fileName);
        this.prop = new Properties();
        loadResource(this.prop, fileName);
    }

    private void loadResource(Properties properties, String fileName) {
        InputStream istream = null;
        try {
            if (new File(fileName).exists()) {
                istream = new FileInputStream(fileName);
            } else {
                istream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            }
            this.prop.load(istream);
        } catch (Exception e) {
            logger.error(
                    PropertiesUtils.class.getName() + "there is no found resource file of the name [" + fileName + "]",
                    e);
            throw new RuntimeException("there is no found resource file of the name [" + fileName + "]", e);
        }
        closeStream(istream);
    }

    private void closeStream(InputStream istream) {
        if (istream == null)
            return;
        try {
            istream.close();
            return;
        } catch (IOException e) {
            logger.error(PropertiesUtils.class.getName(), e);
        } finally {
            try {
                if (istream != null) {
                    istream.close();
                }
            } catch (Exception e) {
                logger.error(PropertiesUtils.class.getName(), e);
            }
        }
    }

    public void addResource(String name) {
        loadResource(this.prop, name);
    }

    public Properties getProperties() {
        return this.prop;
    }

    public Object set(String key, String value) {
        return this.prop.setProperty(key, value);
    }

    public String get(String key) {
        return this.prop.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return this.prop.getProperty(key) == null ? defaultValue : this.prop.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(this.prop.getProperty(key));
    }

    public int getInt(String key, int defaultValue) {
        return this.prop.getProperty(key) == null ? defaultValue : Integer.parseInt(this.prop.getProperty(key));
    }

    public long getLong(String key) {
        return Long.parseLong(this.prop.getProperty(key));
    }

    public long getLong(String key, long defaultValue) {
        return this.prop.getProperty(key) == null ? defaultValue : Long.parseLong(this.prop.getProperty(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.prop.getProperty(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return this.prop.getProperty(key) == null ? defaultValue : Boolean.parseBoolean(this.prop.getProperty(key));
    }

    public static String chkPropertiesName(String fileName) {
        Properties props = System.getProperties();
        String separator = props.getProperty("file.separator");
        File rootDir = new File("").getAbsoluteFile();
        File binDir = new File("./bin").getAbsoluteFile();
        File confDir = new File("./conf").getAbsoluteFile();

        String nameStr = null;

        if ((fileName.contains(".")) && (fileName.endsWith(".properties"))) {
            nameStr = fileName;
        } else {
            nameStr = fileName + ".properties";
        }

        String _rootNameStr = rootDir + separator + nameStr;
        String _binNameStr = binDir + separator + nameStr;
        String _confNameStr = confDir + separator + nameStr;

        if (new File(_rootNameStr).exists())
            return _rootNameStr;
        if (new File(_binNameStr).exists())
            return _binNameStr;
        if (new File(_confNameStr).exists()) {
            return _confNameStr;
        }

        return nameStr;
    }

    public static String getConfFilePath(String fileName) {
        Properties props = System.getProperties();
        String separator = props.getProperty("file.separator");
        File rootDir = new File("").getAbsoluteFile();
        String path = rootDir.getAbsolutePath();

        if (path.endsWith("bin")) {
            path += separator + "..";
        }

        path += separator + CONF_PATH + separator + fileName;
        return path;
    }

}
