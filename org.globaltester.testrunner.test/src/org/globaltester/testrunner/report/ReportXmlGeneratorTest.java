package org.globaltester.testrunner.report;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportXmlGeneratorTest {
	@Test
	public void checkTestCaseIds(){
		assertEquals("ISO7816_H_1", ReportXmlGenerator.formattedTestCaseId("ISO7816_H_1"));
		assertEquals("LDS_E_2", ReportXmlGenerator.formattedTestCaseId("LDS_E_2"));
		assertEquals("EAC2 ISO7816_H_1", ReportXmlGenerator.formattedTestCaseId("EAC2_ISO7816_H_1"));
		assertEquals("EAC2 ISO7816_U_1_a", ReportXmlGenerator.formattedTestCaseId("EAC2_ISO7816_U_1_a"));
		assertEquals("EAC2 DATA_A_1a", ReportXmlGenerator.formattedTestCaseId("EAC2_DATA_A_1a"));
		assertEquals("EAC2 EIDDATA_B_1", ReportXmlGenerator.formattedTestCaseId("EAC2_EIDDATA_B_1"));
		assertEquals("CFG.EAC.ISO7816.E18", ReportXmlGenerator.formattedTestCaseId("CFG.EAC.ISO7816.E18"));
		assertEquals("ESIGN ISO7816_S_1", ReportXmlGenerator.formattedTestCaseId("ESIGN_ISO7816_S_1"));
		assertEquals("Set_04", ReportXmlGenerator.formattedTestCaseId("Set_04"));
		assertEquals("CERTS.PA", ReportXmlGenerator.formattedTestCaseId("CERTS.PA"));
		assertEquals("Gen_Certs", ReportXmlGenerator.formattedTestCaseId("Gen_Certs"));
		assertEquals("Generate_ALL_signedDataEFs", ReportXmlGenerator.formattedTestCaseId("Generate_ALL_signedDataEFs"));
		assertEquals("R_eID_1.2.1", ReportXmlGenerator.formattedTestCaseId("R_eID_1.2.1"));
		assertEquals("TS_CA_1.2.1_d", ReportXmlGenerator.formattedTestCaseId("TS_CA_1.2.1_d"));
		assertEquals("7816_B_5", ReportXmlGenerator.formattedTestCaseId("7816_B_5"));
		assertEquals("7816_B_5 ePassport", ReportXmlGenerator.formattedTestCaseId("7816_B_5 ePassport"));
		assertEquals("7816_B_5 IDL", ReportXmlGenerator.formattedTestCaseId("7816_B_5 IDL"));
		assertEquals("LDS_A_04", ReportXmlGenerator.formattedTestCaseId("LDS_A_04"));
		assertEquals("SE ISO7816_SecBAP_10", ReportXmlGenerator.formattedTestCaseId("SE_ISO7816_SecBAP_10"));
		assertEquals("SE LDS_DG5_004", ReportXmlGenerator.formattedTestCaseId("SE_LDS_DG5_004"));
		assertEquals("ISO7816_T_02 ePassport ECDH 3DES", ReportXmlGenerator.formattedTestCaseId("ISO7816_T_02 ePassport ECDH 3DES"));
		assertEquals("ICAO_p3 ISO_T_08 ECDH 3DES BAC ePassport", ReportXmlGenerator.formattedTestCaseId("ICAO_p3_ISO_T_08 ECDH 3DES BAC ePassport"));
	}
}
