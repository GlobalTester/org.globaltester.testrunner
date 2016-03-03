package org.globaltester.testrunner;

import org.globaltester.testrunner.utils.GtDateHelperTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({GtTestCampaignProjectTest.class, GtDateHelperTest.class})
public class FullTestSuite {

}
