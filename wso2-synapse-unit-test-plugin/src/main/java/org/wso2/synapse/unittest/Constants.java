/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.synapse.unittest;

/**
 * Constants for unit testing framework for synapse.
 */
class Constants {

    /**
     * private constructor of the UnitTestClient.
     */
    private Constants() {
    }

    static final String ARTIFACTS = "artifacts";
    static final String ARTIFACT = "artifact";
    static final String TEST_CASES_TAG = "test-cases";
    static final String TEST_ARTIFACT = "test-artifact";
    static final String SUPPORTIVE_ARTIFACTS = "supportive-artifacts";
    static final String REGISTRY_RESOURCES = "registry-resources";
    static final String CONNECTOR_RESOURCES = "connector-resources";
    static final String MOCK_SERVICES = "mock-services";
    static final String RELATIVE_PREVIOUS = "..";

    static final String DEPLOYMENT_STATUS = "deploymentStatus";
    static final String DEPLOYMENT_EXCEPTION = "deploymentException";
    static final String DEPLOYMENT_DESCRIPTION = "deploymentDescription";
    static final String MEDIATION_STATUS = "mediationStatus";
    static final String MEDIATION_EXECEPTION = "mediationException";
    static final String CURRENT_TESTCASE = "currentTestCase";
    static final String TEST_CASES = "testCases";
    static final String ASSERTION_STATUS = "assertionStatus";
    static final String TEST_CASE_VALUE = "Test Case - ";
    static final String SUITE = "Suite";
    static final String EXCEPTION = "exception";
    static final String FAILURE_ASSERTIONS = "failureAssertions";
    static final String ASSERTION_EXPRESSION = "assertionExpression";
    static final String ASSERTION_ACTUAL = "actual";
    static final String ASSERTION_EXPECTED = "expected";
    static final String ASSERTION_MESSAGE = "message";
    static final String ASSERTION_TYPE = "assertionType";
    static final String ASSERTION_DESCRIPTION = "assertionDescription";

    static final String PASSED_KEY = "PASSED";
    static final String FAILED_KEY = "FAILED";
    static final String SKIPPED_KEY = "SKIPPED";
    static final String DEPLOYMENT_PHASE = "DEPLOYMENT";
    static final String MEDIATION_PHASE = "MEDIATION";
    static final String ASSERTION_PHASE = "ASSERTION";
    static final String TEST_CASE_NAME = "testCaseName";

    static final String TWO_SPACES = "   ";
    static final String XML_EXTENSION = ".xml";
    static final String FILE_SEPARATOR = "file.separator";
    static final String LINE_SEPARATOR = "line.separator";
    static final String LINE_SEPARATOR_WIN = "\\r?\\n";
    static final String TEST_FOLDER_EXTENSION = "${testFile}";
    static final String NO_TEST_CASES = "no-test-cases";
    static final String OS_TYPE = "os.name";
    static final String OS_WINDOWS = "win";
    static final String NEW_LINE_SEPARATOR = "\n";
    static final String REPORT_FILE_NAME = "unit-test-report.json";
    static final String USER_PROFILE = "USERPROFILE";
    static final String HOME = "HOME";
    static final String M2 = ".m2";
    static final String REPOSITORY = "repository";
    static final String ORG = "org";
    static final String WSO2 = "wso2";
    static final String EI = "ei";
    static final String WSO2_MI = "wso2mi";
    static final String WSO2_MI_WITH_DASH = "wso2mi-";
    static final String ZIP = ".zip";
}
