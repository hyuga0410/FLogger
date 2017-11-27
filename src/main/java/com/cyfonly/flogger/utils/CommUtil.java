package com.cyfonly.flogger.utils;

import com.cyfonly.flogger.constants.Constant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * 公用工具类
 *
 * @author yunfeng.cheng
 * @version 2015/10/31
 */
public class CommUtil {

    /**
     * 配置文件名:flogger配置文件
     */
    private static final String CONFIG_FILE_NAME = "flogger.properties";

    /**
     * 配置map
     */
    private static HashMap<String, Object[]> propsMap = new HashMap<>();

    /**
     * 从配置文件中取得 String 值，若无则返回默认值
     *
     * @param keyName      属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getConfigByString(String keyName, String defaultValue) {
        //String value = getConfig(keyName);
        String value = ResourceBundle.getBundle("flogger").getString(keyName);
        if (value.length() > 0) {
            return value.trim();
        } else {
            return defaultValue;
        }
    }

    /**
     * 从配置文件中取得 int 值，若无（或解析异常）则返回默认值
     *
     * @param keyName      属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static int getConfigByInt(String keyName, int defaultValue) {
        //String value = getConfig(keyName);
        String value = ResourceBundle.getBundle("flogger").getString(keyName);
        if (value.length() > 0) {
            try {
                return Integer.parseInt(value.trim());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * 从配置文件中取得 long 值，若无（或解析异常）则返回默认值
     *
     * @param keyName      属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static long getConfigByLong(String keyName, long defaultValue) {
        String value = ResourceBundle.getBundle("flogger").getString(keyName);
        //String value = getConfig(keyName);
        if (value.length() > 0) {
            try {
                return Long.parseLong(value.trim());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * 从配置文件中取得 boolean 值，若无则返回默认值
     *
     * @param keyName      属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static boolean getConfigByBoolean(String keyName, boolean defaultValue) {
        //String value = getConfig(keyName);
        String value = ResourceBundle.getBundle("flogger").getString(keyName);
        if (value.length() > 0) {
            return Boolean.parseBoolean(value.trim());
        } else {
            return defaultValue;
        }
    }

    /**
     * 从配置文件中读取字符串的值
     * 配置文件查找顺序：
     * 1-项目根路径
     * 2-src/main/resources
     *
     * @param keyName 属性名
     * @return 属性值
     */
    private static String getConfig(String keyName) {
        Properties props = null;
        boolean bIsNeedLoadCfg = false;

        String filePath = CONFIG_FILE_NAME;
        File cfgFile = new File(filePath);
        if (!cfgFile.exists()) {
            try {
                URL resource = CommUtil.class.getClassLoader().getResource(filePath);
                if (resource != null) {
                    filePath = resource.getPath();
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
            cfgFile = new File(filePath);
            if (!cfgFile.exists()) {
                return null;
            }
        }

        Object[] arrs = propsMap.get(filePath);
        if (arrs == null) {
            bIsNeedLoadCfg = true;
        } else {
            Long lastModify = (Long) arrs[0];
            if (lastModify != cfgFile.lastModified()) {
                bIsNeedLoadCfg = true;
            } else {
                props = (Properties) arrs[1];
            }
        }

        if (bIsNeedLoadCfg) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(cfgFile);
                props = new Properties();
                props.load(fis);
                propsMap.put(filePath, new Object[]{cfgFile.lastModified(), props});
            } catch (Exception e) {
                return "";
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return props.getProperty(keyName, "");
    }

    /**
     * 将字符串转为字节数组
     *
     * @param str 源字符串
     * @return 字节数组
     */
    public static byte[] StringToBytes(String str) {
        try {
            if (str == null || str.length() <= 0) {
                return new byte[0];
            } else {
                return str.getBytes(Constant.CFG_CHARSET_NAME);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将异常的堆栈信息转为字符串
     *
     * @param e 异常
     * @return 异常的字符串描述
     */
    public static String getExpStack(Exception e) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bo);
        e.printStackTrace(pw);
        pw.flush();
        pw.close();
        return bo.toString();
    }

}
