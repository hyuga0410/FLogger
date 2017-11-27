package com.cyfonly.flogger.strategy;

import java.util.ArrayList;

/**
 * 日志结构
 * @author yunfeng.cheng
 * @version 2015/10/31
 */
class LogFileItem {

	/** 不包括路径，不带扩展名的日志文件名称 如：MsgInner */
	String logFileName = "";

	/** 包括路径的完整日志名称 */
	String fullLogFileName = "";

	/** 当前日志文件大小 */
	long currLogSize = 0;

	/** 当前正在使用的日志缓存 */
	char currLogBuff = 'A';

	/** 日志缓存列表A */
	ArrayList<StringBuffer> alLogBufA = new ArrayList<>();

	/** 日志缓存列表B */
	ArrayList<StringBuffer> alLogBufB = new ArrayList<>();

	/** 下次日志输出到文件时间 */
	long nextWriteTime = 0;

	/** 上次写入时的日期 */
	String lastPCDate = "";

	/** 当前已缓存大小 */
	long currCacheSize = 0;

}
