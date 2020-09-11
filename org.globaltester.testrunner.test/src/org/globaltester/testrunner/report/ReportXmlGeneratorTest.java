package org.globaltester.testrunner.report;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportXmlGeneratorTest {
	@Test
	public void checkTestCaseIds(){
		assertEquals("ISO7816 H_1", ReportXmlGenerator.formattedTestCaseId("ISO7816_H_1"));
		assertEquals("LDS E_2", ReportXmlGenerator.formattedTestCaseId("LDS_E_2"));
		assertEquals("EAC2_ISO7816 H_1", ReportXmlGenerator.formattedTestCaseId("EAC2_ISO7816_H_1"));
		assertEquals("EAC2_ISO7816 U_1 a", ReportXmlGenerator.formattedTestCaseId("EAC2_ISO7816_U_1_a"));
		assertEquals("EAC2_DATA A_1a", ReportXmlGenerator.formattedTestCaseId("EAC2_DATA_A_1a"));
		assertEquals("EAC2_EIDDATA B_1", ReportXmlGenerator.formattedTestCaseId("EAC2_EIDDATA_B_1"));
		assertEquals("CFG.EAC.ISO7816.E18", ReportXmlGenerator.formattedTestCaseId("CFG.EAC.ISO7816.E18"));
		assertEquals("ESIGN_ISO7816 S_1", ReportXmlGenerator.formattedTestCaseId("ESIGN_ISO7816_S_1"));
		assertEquals("Set_04", ReportXmlGenerator.formattedTestCaseId("Set_04"));
		assertEquals("CERTS.PA", ReportXmlGenerator.formattedTestCaseId("CERTS.PA"));
		assertEquals("Gen_Certs", ReportXmlGenerator.formattedTestCaseId("Gen_Certs"));
		assertEquals("Generate_ALL_signedDataEFs", ReportXmlGenerator.formattedTestCaseId("Generate_ALL_signedDataEFs"));
		assertEquals("R eID_1.2.1", ReportXmlGenerator.formattedTestCaseId("R_eID_1.2.1"));
		assertEquals("TS CA_1.2.1_d", ReportXmlGenerator.formattedTestCaseId("TS_CA_1.2.1_d"));
		assertEquals("7816 B_5", ReportXmlGenerator.formattedTestCaseId("7816_B_5"));
		assertEquals("7816 B_5 ePassport", ReportXmlGenerator.formattedTestCaseId("7816_B_5_ePassport"));
		assertEquals("7816 B_5 IDL", ReportXmlGenerator.formattedTestCaseId("7816_B_5_IDL"));
		assertEquals("LDS A_04", ReportXmlGenerator.formattedTestCaseId("LDS_A_04"));
		assertEquals("SE_ISO7816 SecBAP_10", ReportXmlGenerator.formattedTestCaseId("SE_ISO7816_SecBAP_10"));
		assertEquals("SE_LDS DG5_004", ReportXmlGenerator.formattedTestCaseId("SE_LDS_DG5_004"));
		assertEquals("ISO7816 T_02 ePassport_ECDH_3DES", ReportXmlGenerator.formattedTestCaseId("ISO7816_T_02_ePassport_ECDH_3DES"));
	}
}
