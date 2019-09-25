/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.synapse.unittest;

import static org.wso2.synapse.unittest.Constants.SKIPPED_KEY;

/**
 * Class responsible for handling the data of unit testing summary.
 */
public class TestCaseSummary {

    private String testSuiteName = "Test";
    private int testCaseCount = 0 ;
    private int passTestCount = 0;
    private int failureTestCount = 0;
    private String exception;
    private String deploymentStatus = SKIPPED_KEY;
    private String mediationStatus = SKIPPED_KEY;
    private String assertionStatus = SKIPPED_KEY;

    /**
     * Set test suite name.
     *
     * @param testSuiteName name of the suite
     */
    public void setTestSuiteName(String testSuiteName) {
        this.testSuiteName = testSuiteName;
    }

    /**
     * Get test suite name.
     *
     * @return testSuiteName
     */
    public String getTestSuiteName() {
        return testSuiteName;
    }

    /**
     * Get test case count.
     *
     * @return testCaseCount
     */
    public int getTestCaseCount() {
        return testCaseCount;
    }

    /**
     * Get passed test case count.
     *
     * @return passTestCount
     */
    public int getPassTestCount() {
        return passTestCount;
    }

    /**
     * Get failed test case count.
     *
     * @return failureTestCount
     */
    public int getFailureTestCount() {
        return failureTestCount;
    }

    /**
     * Get failed exception.
     *
     * @return exception
     */
    public String getException() {
        return exception;
    }

    /**
     * Get configuration deployment status.
     *
     * @return deploymentStatus
     */
    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    /**
     * Get configuration mediation status.
     *
     * @return mediationStatus
     */
    public String getMediationStatus() {
        return mediationStatus;
    }

    /**
     * Get test cases assertion status.
     *
     * @return assertionStatus
     */
    public String getAssertionStatus() {
        return assertionStatus;
    }

    /**
     * Set test cases count.
     *
     * @param testCaseCount test case count
     */
    public void setTestCaseCount(int testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    /**
     * Set passed test cases count.
     *
     * @param passTestCount passed test case count
     */
    public void setPassTestCount(int passTestCount) {
        this.passTestCount = passTestCount;
    }

    /**
     * Set failed test cases count.
     *
     * @param failureTestCount failed test case count
     */
    public void setFailureTestCount(int failureTestCount) {
        this.failureTestCount = failureTestCount;
    }

    /**
     * Set failed exception.
     *
     * @param exception failed exception
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * Set configuration deployment status.
     *
     * @param deploymentStatus configuration deployment status
     */
    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    /**
     * Set configuration mediation status.
     *
     * @param mediationStatus configuration mediation status
     */
    public void setMediationStatus(String mediationStatus) {
        this.mediationStatus = mediationStatus;
    }

    /**
     * Set test cases assertion status.
     *
     * @param assertionStatus test cases assertion status
     */
    public void setAssertionStatus(String assertionStatus) {
        this.assertionStatus = assertionStatus;
    }
}
