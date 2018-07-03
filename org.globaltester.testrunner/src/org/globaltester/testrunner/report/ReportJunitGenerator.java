package org.globaltester.testrunner.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.testrunner.Activator;
import org.osgi.framework.Bundle;

/**
 * Implements a TestReport that is compatible with the report format produced by
 * JUnit and therefore can be evaluated by external tools, i.e. Jenkins.
 * 
 * @author amay
 * 
 */
public class ReportJunitGenerator {

	/**
	 * Generate a JUnit representation of this report and write it to disk
	 * 
	 * @param report
	 * @throws IOException
	 */
	public static void writeJUnitReport(TestReport report) throws IOException {
		// write the xml report to disk in order to transform it later
		ReportXmlGenerator.writeXmlReport(report);

		// get source and target for this transformation from report
		Source src = new StreamSource(report.getFileName("xml"));
		File destFile = new File(report.getFileName("junit"));

		// get XSLT-Stylesheet
		Bundle curBundle = Platform.getBundle(Activator.PLUGIN_ID);
		URL url = FileLocator.find(curBundle, new Path("/"), null);
		IPath styleSheetPath = new Path(FileLocator.toFileURL(url).getPath()).append("stylesheets/report/report2junit.xsl");
		File styleSheet = styleSheetPath.toFile();

		// transform the xml report
		TransformerFactory factory = TransformerFactory.newInstance();
		try (FileOutputStream outputStream = new FileOutputStream(destFile)){
			Transformer transformer = factory.newTransformer(new StreamSource(styleSheet));
				Result res = new StreamResult(outputStream);
				transformer.transform(src, res);
		} catch (IOException | TransformerException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}
		
	}

}
