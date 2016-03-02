package org.globaltester.testrunner.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GtDateHelperTest {
	@Test
	public void testGetCurrentTimeString() {
		String dateString = GtDateHelper.getCurrentTimeString();
		assertTrue("Date format is correct", dateString.matches("\\d{8}_\\d{6}"));
	}
}
