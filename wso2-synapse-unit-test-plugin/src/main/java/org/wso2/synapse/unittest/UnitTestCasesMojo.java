/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.wso2.synapse.unittest.summarytable.ConsoleDataTable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Mojo(name = "synapse-unit-test")
public class UnitTestCasesMojo extends AbstractMojo {

    @Parameter(property = "testCasesFilePath")
    private String testCasesFilePath;

    @Parameter(property = "server")
    private SynapseServer server;

    @Parameter(property = "mavenTestSkip")
    private String mavenTestSkip;

    private static final String LOCAL_SERVER = "local";
    private static final String REMOTE_SERVER = "remote";

    private Date timeStarted;
    private String serverHost;
    private String serverPort;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Execution method of Mojo class.
     *
     * @throws MojoExecutionException if error occurred while running the plugin
     */
    public void execute() throws MojoExecutionException {
        if (!Boolean.parseBoolean(mavenTestSkip)) {
            try {
                timeStarted = new Date();
                appendTestLogs();
                testCaseRunner();
            } catch (Exception e) {
                throw new MojoExecutionException("Exception occurred while running test cases " + e.getMessage());
            } finally {
                stopTestingServer();
            }
        }
    }

    /**
     * Check test parameters before start.
     *
     * @throws IOException if error occurred while reading test files
     */
    private void checkTestParameters() throws IOException {
        boolean isParameterNotFound = false;

        if (server.getServerType() == null) {
            isParameterNotFound = true;
            getLog().error("Please enter -DtestServerType=<local/remote> parameter value to execute tests");
        }

        if (server.getServerType() != null && server.getServerType().equals(LOCAL_SERVER)
                && server.getServerPath() == null) {
            isParameterNotFound = true;
            getLog().error("Please enter -DtestServerPath=<path> parameter value to execute tests");
        }

        if (server.getServerType() != null && server.getServerType().equals(REMOTE_SERVER)
                && server.getServerHost() == null) {
            isParameterNotFound = true;
            getLog().error("Please enter -DtestServerHost=<host-ip> parameter value to execute tests");
        }

        if (server.getServerPort() == null) {
            isParameterNotFound = true;
            getLog().error("Please enter -DtestServerPort=<host-ip> parameter value to execute tests");
        }

        if (isParameterNotFound) {
            throw new IOException("Test parameters not found");
        }
    }

    /**
     * Execution method of Mojo class.
     *
     * @throws IOException if error occurred while reading test files
     */
    private void testCaseRunner() throws IOException {
        List<String> synapseTestCasePaths = getTestCasesFileNamesWithPaths(testCasesFilePath);

        getLog().info("Detect " + synapseTestCasePaths.size() + " Synapse test case files to execute");
        getLog().info("");

        //start the synapse engine with enable the unit test agent
        if (synapseTestCasePaths.size() > 0) {
            checkTestParameters();
            startTestingServer();
        }

        Map<String, String> testSummaryData = new HashMap<>();
        for (String synapseTestCaseFile : synapseTestCasePaths) {

            String responseFromUnitTestFramework = UnitTestClient.executeTests
                    (synapseTestCaseFile, serverHost, serverPort);

            if (responseFromUnitTestFramework != null
                    && !responseFromUnitTestFramework.equals(Constants.NO_TEST_CASES)) {
                testSummaryData.put(synapseTestCaseFile, responseFromUnitTestFramework);
            } else {
                getLog().info("No test cases found in " + synapseTestCaseFile + " unit test suite");
                getLog().info("");
                continue;
            }

            getLog().info("SynapseTestCaseFile " + synapseTestCaseFile + " tested successfully");
        }

        getLog().info("");
        generateUnitTestReport(testSummaryData);
    }

    /**
     * Get saved SynapseTestcaseFiles from the given destination.
     *
     * @param testFileFolder current files location
     * @return list of files with their file paths
     */
    private ArrayList<String> getTestCasesFileNamesWithPaths(String testFileFolder) {
        ArrayList<String> fileNamesWithPaths = new ArrayList<>();

        if (testFileFolder.endsWith(Constants.XML_EXTENSION)) {
            fileNamesWithPaths.add(testFileFolder);

        } else if (testFileFolder.endsWith(Constants.TEST_FOLDER_EXTENSION)) {
            String testFolderPath = testFileFolder.split("\\$")[0];
            File folder = new File(testFolderPath);
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                String filename = file.getName();
                if (filename.endsWith(Constants.XML_EXTENSION)) {
                    fileNamesWithPaths.add(testFolderPath + filename);
                }
            }
        }

        return fileNamesWithPaths;
    }

    /**
     * Append log of UNIT-TESTS when testing started.
     */
    private void appendTestLogs() {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("U N I T - T E S T S");
        getLog().info("------------------------------------------------------------------------");

    }

    /**
     * Generate unit test report.
     *
     * @param summaryData summary data of the unit test received from synapse server
     */
    private void generateUnitTestReport(Map<String, String> summaryData) throws IOException {
        if (summaryData.isEmpty()) {
            return;
        }

        getLog().info("------------------------------------------------------------------------");
        getLog().info("U N I T - T E S T  R E P O R T");
        getLog().info("------------------------------------------------------------------------");

        getLog().info("Start Time: " + dateFormat.format(timeStarted));


        Date timeStop = new Date();
        long duration = timeStop.getTime() - timeStarted.getTime();
        getLog().info("Test Run Duration: " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");
        getLog().info("Test Summary: ");
        getLog().info("");

        List<Boolean> testFailedSuccessList = new ArrayList<>();
        for (Map.Entry<String, String> summary : summaryData.entrySet()) {
            String fileSeperatePattern = Pattern.quote(System.getProperty(Constants.FILE_SEPARATOR));
            String[] filePathParams = summary.getKey().split(fileSeperatePattern);
            String testName = filePathParams[filePathParams.length - 1];
            getLog().info("Test Suite Name: " + testName);
            getLog().info("==============================================");

            //convert response from synapse engine as a json object
            JsonObject summaryJson = new JsonParser().parse(summary.getValue()).getAsJsonObject();

            //calculate pass and failure test counts
            Map.Entry<String, String> testFailPassCounts = getPassFailureTestCaseCounts(summaryJson);
            String passTestCaseCount = testFailPassCounts.getKey();
            String failureTestCaseCount = testFailPassCounts.getValue();

            getLog().info("Pass Test Cases: " + passTestCaseCount);
            getLog().info("Failure Test Cases: " + failureTestCaseCount);
            getLog().info("");

            String[] summaryHeadersList = {"  TEST CASE  ", "  DEPLOYMENT  ", "  MEDIATION  ", "  ASSERTION  "};
            String[][] testSummaryDataList = getTestCaseWiseSummary(summaryJson);
            String summaryTableString = ConsoleDataTable.of(summaryHeadersList, testSummaryDataList);

            String[] tableOutputLines;
            if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {
                String windowsDefaultLineSeparator = System.getProperty(Constants.LINE_SEPARATOR_WIN);
                if (windowsDefaultLineSeparator == null) windowsDefaultLineSeparator = "\n";
                tableOutputLines = summaryTableString.split(windowsDefaultLineSeparator);
            } else {
                tableOutputLines = summaryTableString.split(System.getProperty(Constants.LINE_SEPARATOR));
            }

            for (String line : tableOutputLines) {
                getLog().info(line);
            }
            getLog().info("");

            //generate test failure table if exists
            boolean isOverallTestFailed = generateTestFailureTable(summaryJson);
            testFailedSuccessList.add(isOverallTestFailed);
        }

        //check overall result of the unit test
        if (testFailedSuccessList.contains(true)) {
            throw new IOException("Overall unit test failed");
        }
    }

    /**
     * Start the Unit testing agent server if user defined it in configuration.
     *
     * @throws IOException if error occurred while starting the synapse server
     */
    private void startTestingServer() throws IOException {

        serverPort = server.getServerPort();
        //set default port in client side whether receiving port is empty
        if(serverPort == null || serverPort.isEmpty()) {
            server.setServerPort("9008");
            serverPort = server.getServerPort();
        }

        //check already has unit testing server
        if (server.getServerType().equals(LOCAL_SERVER)) {
            serverHost = "127.0.0.1";
            String synapseServerPath = server.getServerPath();

            //execute local unit test server by given path and port
            String[] cmd = { synapseServerPath, "-DsynapseTest", "-DsynapseTestPort=" + serverPort};
            Process processor = Runtime.getRuntime().exec(cmd);

            getLog().info("Starting unit testing agent of path - " + synapseServerPath);
            getLog().info("Waiting for testing agent initialization");
            getLog().info("");

            //log the server output
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(processor.getInputStream()));
            String line = null;
            while ((line = inputBuffer.readLine()) != null) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(line);
                }
                if (line.contains("Synapse unit testing agent has been established")) {
                    break;
                }
            }

            //check port availability - unit test server started or not
            boolean isServerNotStarted = true;

            //set timeout time to 120 seconds
            long timeoutExpiredMs = System.currentTimeMillis() + 120000;
            while (isServerNotStarted) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isServerNotStarted = checkPortAvailability(Integer.parseInt(serverPort));

                if (waitMillis <= 0) {
                    // timeout expired
                    processor.destroyForcibly();
                    throw new IOException("Connection refused for service in port - " + serverPort);
                }
            }

            inputBuffer.close();

        } else if (server.getServerType().equals(REMOTE_SERVER)) {
            serverHost = server.getServerHost();
        } else {
            getLog().info("Given server type " + server.getServerType() + "is not an expected type");
        }
    }

    /**
     * Stop the Unit testing agent server.
     */
    private void stopTestingServer() {
        if (server.getServerType() != null && server.getServerType().equals(LOCAL_SERVER)) {
            try {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Stopping unit testing agent runs on port " + serverPort);
                }
                BufferedReader bufferReader;

                //check running operating system
                if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {

                    Runtime runTime = Runtime.getRuntime();
                    Process proc =
                            runTime.exec("cmd /c netstat -ano | findstr " + serverPort);

                    bufferReader = new BufferedReader(new
                            InputStreamReader(proc.getInputStream()));

                    String stream = bufferReader.readLine();
                    if (stream != null) {
                        int index = stream.lastIndexOf(' ');
                        String pid = stream.substring(index);

                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Unit testing agent runs with PID of " + pid);
                        }

                        runTime.exec("cmd /c Taskkill /PID" + pid + " /T /F");
                    }

                } else {
                    Process pr = Runtime.getRuntime().exec("lsof -t -i:" + serverPort);
                    bufferReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    String pid = bufferReader.readLine();

                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Unit testing agent runs with PID of " + pid);
                    }
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }

                bufferReader.close();
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Unit testing agent stopped");
                }

            } catch (Exception e) {
                getLog().error("Error in closing the server", e);
            }
        }
    }

    /**
     * Check port availability.
     *
     * @param port port which want to check availability
     * @return if available true else false
     */
    private boolean checkPortAvailability(int port) {
        boolean isPortAvailable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, port));
            isPortAvailable = false;
        } catch (IOException e) {
            isPortAvailable = true;
        }

        return isPortAvailable;
    }

    /**
     * Get pass and failure tests among all the test cases.
     *
     * @param jsonSummary test summary as a json
     * @return map of test cases
     */
    private Map.Entry<String, String> getPassFailureTestCaseCounts(JsonObject jsonSummary) {
        String passTestCount;
        String failureTestCount;

        if (jsonSummary.get(Constants.MEDIATION_STATUS) != null &&
                jsonSummary.get(Constants.MEDIATION_STATUS).getAsString().equals(Constants.PASSED_KEY) &&
                jsonSummary.get(Constants.TEST_CASES) != null &&
                jsonSummary.get(Constants.TEST_CASES).getAsJsonArray().size() > 0) {
            JsonArray testCases = jsonSummary.get(Constants.TEST_CASES).getAsJsonArray();

            int passCount = 0;
            int failureCount = 0;
            for (int x = 0; x < testCases.size(); x++) {
                JsonObject testJsonObject = testCases.get(x).getAsJsonObject();

                if (testJsonObject.get(Constants.ASSERTION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                    passCount++;
                } else {
                    failureCount++;
                }
            }
            passTestCount = String.valueOf(passCount);
            failureTestCount = String.valueOf(failureCount);

        } else {
            passTestCount = "N/A";
            failureTestCount = "N/A";
        }

        return new AbstractMap.SimpleEntry<>(passTestCount, failureTestCount);
    }

    /**
     * Get test cases wise summary from the whole test summary.
     *
     * @param jsonSummary test summary as a json
     * @return string array of processes data
     */
    private String[][] getTestCaseWiseSummary(JsonObject jsonSummary) {
        List<List<String>> allTestSummary = new ArrayList<>();

        if (jsonSummary.get(Constants.TEST_CASES) != null &&
                jsonSummary.get(Constants.TEST_CASES).getAsJsonArray().size() > 0) {
            JsonArray testCases = jsonSummary.get(Constants.TEST_CASES).getAsJsonArray();

            int x;
            for (x = 0; x < testCases.size(); x++) {
                List<String> testSummary = new ArrayList<>();
                JsonObject testJsonObject = testCases.get(x).getAsJsonObject();

                testSummary.add(Constants.TEST_CASE_VALUE + testJsonObject.get(Constants.TEST_CASE_NAME).getAsString());
                testSummary.add(Constants.TWO_SPACES + jsonSummary.get(Constants.DEPLOYMENT_STATUS).getAsString());
                testSummary.add(Constants.TWO_SPACES + testJsonObject.get(Constants.MEDIATION_STATUS).getAsString());
                testSummary.add(Constants.TWO_SPACES + testJsonObject.get(Constants.ASSERTION_STATUS).getAsString());

                allTestSummary.add(testSummary);
            }

            if (!jsonSummary.get(Constants.MEDIATION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                List<String> testSummary = new ArrayList<>();
                testSummary.add(Constants.TEST_CASE_VALUE + jsonSummary.get(Constants.CURRENT_TESTCASE).getAsString());
                testSummary.add(Constants.TWO_SPACES + jsonSummary.get(Constants.DEPLOYMENT_STATUS).getAsString());
                testSummary.add(Constants.TWO_SPACES + jsonSummary.get(Constants.MEDIATION_STATUS).getAsString());
                testSummary.add(Constants.TWO_SPACES + Constants.SKIPPED_KEY);
                allTestSummary.add(testSummary);
            }

        } else {
            List<String> testSummary = new ArrayList<>();
            testSummary.add(Constants.TEST_CASE_VALUE + Constants.SUITE);
            testSummary.add(Constants.TWO_SPACES + jsonSummary.get(Constants.DEPLOYMENT_STATUS).getAsString());
            testSummary.add(Constants.TWO_SPACES + jsonSummary.get(Constants.MEDIATION_STATUS).getAsString());
            testSummary.add(Constants.TWO_SPACES + Constants.SKIPPED_KEY);

            allTestSummary.add(testSummary);
        }

        String[][] allSummaryArray = new String[allTestSummary.size()][4];
        for (int x = 0 ; x < allTestSummary.size(); x++) {
            List<String> innerList = allTestSummary.get(x);
            String[] innerArray = new String[4];
            innerArray = innerList.toArray(innerArray);
            allSummaryArray[x] = innerArray;
        }
        return allSummaryArray;
    }

    /**
     * Generate failure detailed table data.
     *
     * @param jsonSummary test summary as a json
     * @return if failed occurred or not
     */
    private boolean generateTestFailureTable(JsonObject jsonSummary) {
        boolean isFailureOccurred = false;
        String[] errorHeadersList = {"  TEST CASE  ", "  FAILURE STATE  ", "      EXCEPTION / ERROR MESSAGE     "};

        List<List<String>> errorRowsList = new ArrayList<>();
        if (jsonSummary.get(Constants.TEST_CASES) != null &&
                jsonSummary.get(Constants.TEST_CASES).getAsJsonArray().size() > 0) {
            JsonArray testCases = jsonSummary.get(Constants.TEST_CASES).getAsJsonArray();

            for (int x = 0; x < testCases.size(); x++) {
                List<String> failureSummary = new ArrayList<>();
                JsonObject testJsonObject = testCases.get(x).getAsJsonObject();

                if (!testJsonObject.get(Constants.MEDIATION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                    isFailureOccurred = true;
                    failureSummary.add(Constants.TEST_CASE_VALUE +
                            testJsonObject.get(Constants.TEST_CASE_NAME).getAsString());
                    failureSummary.add(Constants.TWO_SPACES + Constants.MEDIATION_PHASE);
                    failureSummary.add(testJsonObject.get(Constants.EXCEPTION).getAsString());
                    errorRowsList.add(failureSummary);
                } else if (!testJsonObject.get(Constants.ASSERTION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                    isFailureOccurred = true;
                    failureSummary.add(Constants.TEST_CASE_VALUE +
                            testJsonObject.get(Constants.TEST_CASE_NAME).getAsString());
                    failureSummary.add(Constants.TWO_SPACES + Constants.ASSERTION_PHASE);
                    failureSummary.add(testJsonObject.get(Constants.EXCEPTION).getAsString());
                    errorRowsList.add(failureSummary);
                }
            }

            if (!jsonSummary.get(Constants.MEDIATION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                isFailureOccurred = true;
                List<String> failureSummary = new ArrayList<>();
                failureSummary.add(Constants.TEST_CASE_VALUE +
                        jsonSummary.get(Constants.CURRENT_TESTCASE).getAsString());
                failureSummary.add(Constants.TWO_SPACES + Constants.MEDIATION_PHASE);
                failureSummary.add(jsonSummary.get(Constants.MEDIATION_EXECEPTION).getAsString());
                errorRowsList.add(failureSummary);
            }

        } else {
            List<String> testSummary = new ArrayList<>();

            if (!jsonSummary.get(Constants.DEPLOYMENT_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                isFailureOccurred = true;
                testSummary.add(Constants.TEST_CASE_VALUE + Constants.SUITE);
                testSummary.add(Constants.TWO_SPACES + Constants.DEPLOYMENT_PHASE);
                if (jsonSummary.get(Constants.DEPLOYMENT_EXCEPTION) != null) {
                    testSummary.add(jsonSummary.get(Constants.DEPLOYMENT_EXCEPTION).getAsString());
                } else {
                    testSummary.add(jsonSummary.get(Constants.DEPLOYMENT_DESCRIPTION).getAsString());
                }
                errorRowsList.add(testSummary);
            } else if (!jsonSummary.get(Constants.MEDIATION_STATUS).getAsString().equals(Constants.PASSED_KEY)) {
                isFailureOccurred = true;
                testSummary.add(Constants.TEST_CASE_VALUE + jsonSummary.get(Constants.CURRENT_TESTCASE).getAsString());
                testSummary.add(Constants.TWO_SPACES + Constants.MEDIATION_PHASE);
                testSummary.add(jsonSummary.get(Constants.MEDIATION_EXECEPTION).getAsString());
                errorRowsList.add(testSummary);
            }
        }

        if (isFailureOccurred) {
            getLog().info("Failed Test Case(s): ");

            String[][] errorRowsArray = new String[errorRowsList.size()][3];
            for (int x = 0 ; x < errorRowsList.size(); x++) {
                List<String> innerList = errorRowsList.get(x);
                String[] innerArray = new String[3];
                innerArray = innerList.toArray(innerArray);
                errorRowsArray[x] = innerArray;
            }

            String errorTableString = ConsoleDataTable.of(errorHeadersList, errorRowsArray);

            String[] errorTableLines;
            if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {
                String windowsDefaultLineSeparator = System.getProperty(Constants.LINE_SEPARATOR_WIN);
                if (windowsDefaultLineSeparator == null) windowsDefaultLineSeparator = "\n";
                errorTableLines = errorTableString.split(windowsDefaultLineSeparator);
            } else {
                errorTableLines = errorTableString.split(System.getProperty(Constants.LINE_SEPARATOR));
            }
            for (String line : errorTableLines) {
                getLog().info(line);
            }
            getLog().info("");
        }

        return isFailureOccurred;
    }
}
