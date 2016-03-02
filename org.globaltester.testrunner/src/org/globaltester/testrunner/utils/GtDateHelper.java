package org.globaltester.testrunner.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.globaltester.logging.logger.GTLogger;

public class GtDateHelper {
	//default time format is same as default logging time format
	static final String gtDefaultDateFormat = GTLogger.DEFAULT_DATE_FORMAT;
	
	
	public static String getCurrentTimeString(){
		SimpleDateFormat defaultSdf = new SimpleDateFormat(gtDefaultDateFormat);
		return defaultSdf.format(Calendar.getInstance().getTime());
	}
}
