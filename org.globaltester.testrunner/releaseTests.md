Releasetests TestRunner
=====================
This document describes validation tests that shall be performed on the final product artifacts immediately before publishing. These tests focus on overall product quality and completeness (e.g. inclusion/integration of required features). For a complete test coverage please also refer to tests defined in the according bundles.

1. [ ] __Perform Cheat Sheet__
Launch the GlobalTester Platform product and perform the following cheat sheet
 - [ ] "Execute TestCases as Campaign"
     - [ ] Check that the names of SampleConfigs in TestCampaignExecutions are shown
     
1. [ ] __Check TestCampaign report generation behavior__
 - [ ] Create TestCampaign and do _not_ execute it
     - [ ] Press "Generate Report" button, this should lead to an error dialog and not uncaught exceptions

1. [ ] __Check TestCampaign behavior__
Launch the GlobalTester Platform product and perform the following steps
 - [ ] Select several test cases and a test suite
 - [ ] Use the "Create TestCampaign and execute it" button from the campaign drop down menu
     - [ ] The TestCampaign is executed with expected results depending on test cases and environment
 - [ ] Right click a SampleConfiguration project and select "Create TestCampaign"
     - [ ] An error message appears
     
1. [ ] __Check TestCampaign behavior with write error__
Launch the GlobalTester Platform product and perform the following steps
 - [ ] Right click a test case and select "Create TestCampaign"
 - [ ] Execute the campaign
 - [ ] Mark the "TestResults" directory in the Campaign as read only in the file system (e.g. chmod -w TestResults)
 - [ ] Execute the campaign
     - [ ] An error message appears in the comment field alongside the test campaign result "FAILURE"
 

<p style="page-break-after: always"/>
