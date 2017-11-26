package com.cyfonly.flogger.strategy;

import com.cyfonly.flogger.constants.Constant;
import com.cyfonly.flogger.utils.CommUtil;
import com.cyfonly.flogger.utils.TimeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志管理线程
 *
 * @author yunfeng.cheng
 * @version 2015/10/31
 */
public class LogManager extends Thread {

    /**
     * 单例
     */
    private static LogManager instance = null;

    /**
     * 日志文件列表
     */
    private Map<String, LogFileItem> logFileMap = new ConcurrentHashMap<String, LogFileItem>();

    /**
     * 日志写入的间隔时间
     */
    public final static long WRITE_LOG_INV_TIME = CommUtil.getConfigByLong("WRITE_LOG_INV_TIME", 1000);

    /**
     * 单个日志文件的大小(默认为10M)
     */
    public final static long SINGLE_LOG_FILE_SIZE = CommUtil.getConfigByLong("SINGLE_LOG_FILE_SIZE", 1024 * 1024 * 10);

    /**
     * 缓存大小(默认10KB)
     */
    public final static long SINGLE_LOG_CACHE_SIZE = CommUtil.getConfigByLong("SINGLE_LOG_CACHE_SIZE", 1024 * 10);

    /**
     * 是否运行
     */
    private boolean bIsRun = true;

    public LogManager() {

    }

    /**
     * 日志单线程初始化
     * 获得日志管理类单例
     */
    public synchronized static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
            instance.setName("FLogger");//设置线程名称
            instance.start();//启动线程
        }
        return instance;
    }

    /**
     * 添加日志
     *
     * @param logFileName 日志文件名称
     * @param logMsg      日志内容
     */
    public void addLog(String logFileName, StringBuffer logMsg) {
        //获得单个日志文件的信息
        LogFileItem lfi = logFileMap.get(logFileName);
        if (lfi == null) {
            synchronized (this) {
                lfi = logFileMap.get(logFileName);
                if (lfi == null) {
                    lfi = new LogFileItem();//当前正使用的日志缓存：currLogBuff = 'A'
                    lfi.logFileName = logFileName;
                    lfi.nextWriteTime = System.currentTimeMillis() + WRITE_LOG_INV_TIME;//下一次写入时间：当前时间+写入间隔
                    logFileMap.put(logFileName, lfi);//放入日志文件列表
                }
            }
        }
        //同步单个文件的日志
        synchronized (lfi) {
            if (lfi.currLogBuff == 'A') {
                lfi.alLogBufA.add(logMsg);
            } else {
                lfi.alLogBufB.add(logMsg);
            }
            //当前已缓存大小
            lfi.currCacheSize += CommUtil.StringToBytes(logMsg.toString()).length;
        }
    }

    /**
     * 线程方法
     */
    public void run() {
        int i = 0;
        while (bIsRun) {
            try {
                //输出到文件
                flush(false);
                //重新获取日志级别
                if (i++ % 100 == 0) {
                    Constant.CFG_LOG_LEVEL = CommUtil.getConfigByString("LOG_LEVEL", "0,1,2,3,4");
                    i = 1;
                }
            } catch (Exception e) {
                System.out.println("开启日志服务错误...");
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭方法
     */
    public void close() {
        bIsRun = false;
        try {
            flush(true);
        } catch (Exception e) {
            System.out.println("关闭日志服务错误...");
            e.printStackTrace();
        }
    }

    /**
     * 输出缓存的日志到文件
     *
     * @param bIsForce 是否强制将缓存中的日志输出到文件
     */
    private void flush(boolean bIsForce) throws IOException {
        long currTime = System.currentTimeMillis();
        for (String s : logFileMap.keySet()) {
            LogFileItem lfi = logFileMap.get(s);
            /*
             * 输出缓存日志到文件的条件
             * 1.当前时间大于等于线程的下一次执行时间
             * 2.已缓存大小大于等于线程缓存上限
             * 3.是否强制输出
             */
            if (currTime >= lfi.nextWriteTime || SINGLE_LOG_CACHE_SIZE <= lfi.currCacheSize || bIsForce) {
                //获得需要进行输出的缓存列表
                ArrayList<StringBuffer> alWrtLog;
                synchronized (lfi) {
                    if (lfi.currLogBuff == 'A') {
                        alWrtLog = lfi.alLogBufA;
                        lfi.currLogBuff = 'B';
                    } else {
                        alWrtLog = lfi.alLogBufB;
                        lfi.currLogBuff = 'A';
                    }
                    lfi.currCacheSize = 0;
                }
                //创建日志文件
                createLogFile(lfi);
                //输出日志
                int iWriteSize = writeToFile(lfi.fullLogFileName, alWrtLog);
                lfi.currLogSize += iWriteSize;
            }
        }
    }

    /**
     * 创建日志文件
     *
     * @param lfi
     */
    private void createLogFile(LogFileItem lfi) {
        //当前系统日期
        String currPCDate = TimeUtil.getPCDate('-');

        //判断日志root路径是否存在，不存在则先创建
        File rootDir = new File(Constant.CFG_LOG_PATH);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            rootDir.mkdirs();
        }

        //如果超过单个文件大小，则拆分文件
        if (lfi.fullLogFileName != null && lfi.fullLogFileName.length() > 0 && lfi.currLogSize >= LogManager.SINGLE_LOG_FILE_SIZE) {
            File oldFile = new File(lfi.fullLogFileName);
            if (oldFile.exists()) {
                String newFileName = Constant.CFG_LOG_PATH + "/" + lfi.lastPCDate + "/" + lfi.logFileName + "_" + TimeUtil.getPCDate() + "_" + TimeUtil.getCurrTime() + ".log";
                File newFile = new File(newFileName);
                boolean flag = oldFile.renameTo(newFile);
                System.out.println("日志已自动备份为 " + newFile.getName() + (flag ? "成功!" : "失败!"));
                lfi.fullLogFileName = "";
                lfi.currLogSize = 0;
            }
        }
        //创建文件
        if (lfi.fullLogFileName == null || lfi.fullLogFileName.length() <= 0 || !lfi.lastPCDate.equals(currPCDate)) {
            String sDir = Constant.CFG_LOG_PATH + "/" + currPCDate;
            File file = new File(sDir);
            if (!file.exists()) {
                file.mkdir();
            }
            lfi.fullLogFileName = sDir + "/" + lfi.logFileName + ".log";
            lfi.lastPCDate = currPCDate;

            file = new File(lfi.fullLogFileName);
            if (file.exists()) {
                lfi.currLogSize = file.length();
            } else {
                lfi.currLogSize = 0;
            }
        }
    }

    /**
     * 输出日志到文件
     *
     * @param sFullFileName 完整的日志文件名称
     * @param sbLogMsg      日志文件内容
     * @return 返回输出内容大小
     */
    private int writeToFile(String sFullFileName, ArrayList<StringBuffer> sbLogMsg) throws IOException {
        int size = 0;
        OutputStream fout = null;
        try {
            fout = new FileOutputStream(sFullFileName, true);
            for (StringBuffer logMsg : sbLogMsg) {
                byte[] tmpBytes = CommUtil.StringToBytes(logMsg.toString());
                if (tmpBytes != null) {
                    fout.write(tmpBytes);
                    size += tmpBytes.length;
                }
            }
            fout.flush();
            sbLogMsg.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
        return size;
    }

}
