package com.zzstack.paas.underlying.firmware.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

import com.zzstack.paas.underlying.firmware.consts.CONSTS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JniTools {
    
    private static Logger logger = LoggerFactory.getLogger(JniTools.class);
    
    public static void loadFirmwareJniFile(Class<?> clazz) {
        String jniFileName = getFirmwareJniFileName();
        if (!isFileExist(jniFileName)) {
            try {
                File file = new File("." + File.separator + jniFileName);
                file.createNewFile();

                FileOutputStream os = new FileOutputStream(file);
                InputStream is = clazz.getResourceAsStream(jniFileName);
                if (is == null) {
                    os.close();
                    logger.error("jni shared library file:{} not exists ......", jniFileName);
                    return;
                }
                
                byte[] cache = new byte[1024];
                Arrays.fill(cache, (byte) 0);
                
                int realRead = is.read(cache);
                while (realRead != -1) {
                    os.write(cache, 0, realRead);
                    realRead = is.read(cache);
                }
                os.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        
        addLibraryDir(".");
    }
    
    public static void addLibraryDir(String libraryPath) {
        String split = OSTools.isLinux() ? ":" : ";";
        
        try {
            Field userPathsField = ClassLoader.class.getDeclaredField("usr_paths");
            userPathsField.setAccessible(true);
            String[] paths = (String[]) userPathsField.get(null);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paths.length; i++) {
                if (libraryPath.equals(paths[i])) {
                    continue;
                }
                
                if (sb.length() > 0)
                    sb.append(split);
                sb.append(paths[i]);
            }
            
            sb.append(split).append(libraryPath);
            System.setProperty("java.library.path", sb.toString());
        
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    public static void main(String[] args) {
        addLibraryDir(".");
    }
    
    private static boolean isFileExist(String fileName) {
        File file = new File("." + File.separator + fileName);
        return file.exists();
    }
    
    public static String getFirmwareJniFileName() {
        if (OSTools.isLinux()) {
            return CONSTS.FIRMWARE_JNI_LINUX;
        } else if (OSTools.isWindows()) {
            return CONSTS.FIRMWARE_JNI_WINDOWS;
        } else {
            logger.error("firmware shard library only surpport window and linux");
            return "";
        }
    }

}
