package org.globaltester.testrunner.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.testrunner.testframework.Result.Status;

/**
 * This class implements all methods for test report in CSV format
 * 
 * @author Alexander May
 * 
 */
public class ReportCsvGenerator {
	
	/**
	 * Generate a CSV representation of this report and write it to disk
	 * 
	 * @param report
	 * @throws IOException
	 */
	public static void writeCsvReport(TestReport report) throws IOException {

		//get the product and sample id
		String platformID = report.getPlatformId();
		String sampleID = report.getSampleId();		
		
		//open the file and fill it with content
		File csvReportFile = new File(report.getFileName("csv"));
		report.getReportDir().mkdirs();
		try (PrintWriter pw = new PrintWriter(csvReportFile)) {
			for (TestReportPart curElem : report.getElements()) {
				

				String testcaseID = curElem.getID();
				
				String logLine = new String();
				logLine = logLine.concat(platformID + ";");
				logLine = logLine.concat(sampleID + ";");
				logLine = logLine.concat(testcaseID + ";");
				
				Status status = curElem.getStatus();
				if (status == Status.PASSED) {
					logLine = logLine.concat("P");
				} 
				else if (status == Status.UNDEFINED
					|| status == Status.NOT_APPLICABLE) {
					logLine = logLine.concat("N");
				} 
				else if (status == Status.FAILURE ||
						status == Status.WARNING) {
					logLine = logLine.concat("F");
				}
				
				pw.println(logLine);

			}

		}
		
		//create a copy of CSV report with name platform_sample.csv
		File destFile = new File(report.getReportDir(), platformID+"_"+sampleID+".csv");
		GtResourceHelper.copyFiles(csvReportFile, destFile);
		
	}
}
