package org.globaltester.testrunner.report;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.globaltester.lib.fop.renderer.GtFopHelper;
import org.globaltester.lib.fop.renderer.PdfReportGenerationException;
import org.globaltester.testrunner.Activator;
import org.osgi.framework.Bundle;

public class ReportPdfGenerator {

	/**
	 * @throws PdfReportGenerationException 
	 * Generate a PDF representation of this report and write it to disk
	 * 
	 * @param report
	 * @throws IOException
	 * @throws TransformerException 
	 * @throws  
	 */
	public static void writePdfReport(TestReport report) throws IOException, PdfReportGenerationException {
		// write the xml report to disk in order to transform it later
		ReportXmlGenerator.writeXmlReport(report);

		// get source and target for this transformation from report
		Source src = new StreamSource(report.getFileName("xml"));
		File destFile = new File(report.getFileName("pdf"));

		// get XSLT-Stylesheet
		Bundle curBundle = Platform.getBundle(Activator.PLUGIN_ID);
		URL url = FileLocator.find(curBundle, new Path("/"), null);
		IPath styleSheetPath = new Path(FileLocator.toFileURL(url).getPath()).append("stylesheets/report/report2fo.xsl");
		File styleSheet = styleSheetPath.toFile();

		GtFopHelper.transformToPdf(src, destFile, styleSheet);
	}

}
