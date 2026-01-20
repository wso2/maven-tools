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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.wso2.synapse.unittest.summarytable.ConsoleDataTable;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mojo(name = "synapse-unit-test")
public class UnitTestCasesMojo extends AbstractMojo {

    @Parameter(property = "testCasesFilePath")
    private String testCasesFilePath;

    @Parameter(property = "testCaseName")
    private String synapseTestCaseName;

    @Parameter(property = "server")
    private SynapseServer server;

    @Parameter(property = "mavenTestSkip")
    private String mavenTestSkip;

    private static final String LOCAL_SERVER = "local";
    private static final String REMOTE_SERVER = "remote";
    private static final String WIN_LAUNCHER  = "micro-integrator.bat";
    private static final String UNIX_LAUNCHER = "micro-integrator.sh";
    private final String baseUrl = "https://mi-distribution.wso2.com/";

    private Date timeStarted;
    private String serverHost;
    private String serverPort;
    private boolean isUnitTestAgentStartTheServer = false;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private boolean overallTestFailure = false;

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
                stopTestingServer();
                throw new MojoExecutionException("Exception occurred while running test cases: " + e.getMessage());
            } finally {
                if (isUnitTestAgentStartTheServer) {
                    stopTestingServer();
                }
            }
        }
    }

    /**
     * Check test parameters before start.
     *
     * @throws IOException if error occurred while reading test files
     */
    private void checkTestParameters(String projectRootPath) throws IOException {

        if (server.getServerType() == null) {
            getLog().error("Please enter -DtestServerType=<local/remote> parameter value to execute tests");
            throw new IOException("Test parameters are not found");
        }

        if (server.getServerType() != null && server.getServerType().equals(LOCAL_SERVER)) {
            if (!isServerPathConfigured()) {
                Path jsonFile = Paths.get(projectRootPath, ".vscode", "settings.json");
                if (Files.exists(jsonFile)) {
                    readServerPath(jsonFile);
                }
                if (!isServerPathConfigured()) {
                    if (StringUtils.isBlank(server.getServerDownloadLink()) &&
                            StringUtils.isNotBlank(server.getServerVersion())) {
                        int serverVersion = Integer.parseInt(server.getServerVersion().replaceAll("\\.", ""));
                        URL downloadUrl;
                        if (serverVersion == 440) {
                            downloadUrl = new URL(baseUrl + server.getServerVersion() +
                                    Constants.SLASH_WSO2_MI_WITH_DASH + server.getServerVersion() + Constants.UPDATED +
                                    Constants.ZIP);
                        } else if (serverVersion <= 420) {
                            getLog().error("Please enter -DtestServerPath=<path> parameter " +
                                    "value to execute tests");
                            throw new IOException("Test parameters are not found");
                        } else {
                            downloadUrl = new URL(baseUrl + server.getServerVersion() +
                                    Constants.SLASH_WSO2_MI_WITH_DASH + server.getServerVersion() + Constants.ZIP);
                        }
                        server.setServerDownloadLink(downloadUrl.toString());
                        server.setServerPath(Paths.get(getUserHome(), Constants.WSO2_MI, Constants.MICRO_INTEGRATOR,
                                Constants.WSO2_MI_WITH_DASH + server.getServerVersion()).toString());
                    } else {
                        getLog().error("Please enter -DtestServerPath=<path> parameter " +
                                "value to execute tests");
                        throw new IOException("Test parameters are not found");
                    }
                }
            }
            setServerPath(server.getServerPath());
        }

        if (server.getServerType() != null && server.getServerType().equals(REMOTE_SERVER)
                && server.getServerHost() == null) {
            getLog().error("Please enter -DtestServerHost=<host-ip> parameter value to execute tests");
            throw new IOException("Test parameters are not found");
        }
    }


    private boolean isServerPathConfigured() {
        return StringUtils.isNotBlank(server.getServerPath()) && !("/").equals(server.getServerPath().trim());
    }

    private void readServerPath(Path filePath) {
        Pattern extractor = Pattern.compile(
                "\"(" + Pattern.quote("MI.SERVER_PATH") + ")\"\\s*:\\s*\"([^\"]+)\"");
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            Optional<String> filename = br.lines()
                    .map(String::trim)
                    .map(extractor::matcher)
                    .filter(Matcher::find)
                    .map(m -> m.group(2))
                    .findFirst();

            filename.ifPresent(s -> server.setServerPath(s));
        } catch (IOException e) {
            // Skip if settings.json is missing or unreadable;
            // the serverPath can still be configured via pom.xml
        }
    }

    private void setServerPath(String basePath) {
        if (!(basePath.endsWith(".bat") || basePath.endsWith(".sh"))) {
            basePath = Paths.get(basePath, "bin",
                            System.getProperty("os.name").toLowerCase().contains("windows") ? WIN_LAUNCHER :
                                    UNIX_LAUNCHER).toString();
        }
        server.setServerPath(basePath);
    }

    /**
     * Execution method of Mojo class.
     *
     * @throws IOException if error occurred while reading test files
     */
    private void testCaseRunner() throws Exception {
        List<String> synapseTestCasePaths = getTestCasesFileNamesWithPaths(testCasesFilePath);

        if (synapseTestCasePaths.isEmpty()) {
            getLog().info("Project does not include any unit tests.");
            return;
        }

        getLog().info("Detect " + synapseTestCasePaths.size() + " Synapse test case files to execute");
        getLog().info("");

        //start the synapse engine with enable the unit test agent
        if (synapseTestCasePaths.size() > 0) {
            String projectRootPath = synapseTestCasePaths.get(0).split("src")[0];
            checkTestParameters(projectRootPath);
            if (server.getServerType().equalsIgnoreCase(LOCAL_SERVER) && server.getServerPath() != null &&
                    (server.getServerPath().equalsIgnoreCase("/") ||
                            !Files.exists(Paths.get(server.getServerPath())))) {
                try {
                    getLog().info("Setting up a local server because no valid server path was provided.");
                    setupLocalServer(projectRootPath);
                } catch (Exception e) {
                    getLog().error("Error in setting up the local server", e);
                    throw new MojoExecutionException("Error in setting up the local server", e);
                }
            }
            startTestingServer();
        }

        Map<String, String> testSummaryData = new HashMap<>();
        for (String synapseTestCaseFile : synapseTestCasePaths) {

            String responseFromUnitTestFramework = UnitTestClient.executeTests
                    (synapseTestCaseFile, serverHost, serverPort, synapseTestCaseName);

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
        if (!testSummaryData.isEmpty()) {
            Date timeStop = new Date();
            long duration = timeStop.getTime() - timeStarted.getTime();
            generateUnitTestReport(testSummaryData, duration);
            writeUnitTestReportToFile(testSummaryData, duration);
        }
        if (overallTestFailure) {
            throw new IOException("Overall unit test failed");
        }
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
            if (!(new File(testFolderPath).exists())) {
                return fileNamesWithPaths;
            }
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
    private void generateUnitTestReport(Map<String, String> summaryData, long duration) throws IOException {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("U N I T - T E S T  R E P O R T");
        getLog().info("------------------------------------------------------------------------");

        getLog().info("Start Time: " + dateFormat.format(timeStarted));

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

            //generate test summary detail table (PASS / FAILURE)
            String[] summaryHeadersList = {"  TEST CASE  ", "  DEPLOYMENT  ", "  MEDIATION  ", "  ASSERTION  "};
            List<List<String>> testSummaryDataList = getTestCaseWiseSummary(summaryJson);
            printDetailedTable(testSummaryDataList, 4, summaryHeadersList);
            //generate coverage report if exists
            generateCoverageReport(summaryJson);
            //generate test failure table if exists
            boolean isOverallTestFailed = generateTestFailureTable(summaryJson);
            testFailedSuccessList.add(isOverallTestFailed);
        }
        //check overall result of the unit test
        if (testFailedSuccessList.contains(true)) {
            overallTestFailure = true;
        }
    }

    /**
     * Write the unit test report to the file.
     *
     * @param summaryData summary data of the unit test received from synapse server.
     */
    private void writeUnitTestReportToFile(Map<String, String> summaryData, long duration) {
        File targetFolder = new File(Paths.get("target").toUri());
        if (!targetFolder.exists()) {
            targetFolder.mkdir();
        }
        File reportFile = new File(Paths.get("target", Constants.REPORT_FILE_NAME).toUri());
        if (reportFile.exists()) {
            reportFile.delete();
        }
        JsonObject finalSummary = new JsonObject();
        for (Map.Entry<String, String> summary : summaryData.entrySet()) {
            String testFileName = summary.getKey();
            JsonObject summaryJson = new JsonParser().parse(summary.getValue()).getAsJsonObject();
            finalSummary.add(testFileName, summaryJson);
        }
        finalSummary.addProperty("Time elapsed (ms)", duration);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(finalSummary);
        try (FileWriter myWriter = new FileWriter(reportFile)) {
            myWriter.write(prettyJson);
        } catch (IOException e) {
            getLog().error("Error in writing the unit test report to the file", e);
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
        if (serverPort == null || serverPort.isEmpty()) {
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

            //check unit testing port availability
            if (!checkPortAvailability(Integer.parseInt(serverPort))) {
                getLog().error("Another process has already occupied the port - " + serverPort);
                throw new IOException("Another process has already occupied the port - " + serverPort);
            }

            //log the server output
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(processor.getInputStream()));
            String line = null;
            while ((line = inputBuffer.readLine()) != null) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(line);
                }
                if (line.contains("Synapse unit testing agent has been established")) {
                    isUnitTestAgentStartTheServer = true;
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
     * @return list array of processes data
     */
    private List<List<String>> getTestCaseWiseSummary(JsonObject jsonSummary) {
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

        return allTestSummary;
    }

    /**
     * Generate coverage report from the test summary.
     *
     * @param jsonSummary test summary as a json
     */
    private void generateCoverageReport(JsonObject jsonSummary) {
        try {
            if (!hasCoverageData(jsonSummary)) {
                return;
            }

            JsonObject coverageData = jsonSummary.getAsJsonObject(Constants.MEDIATOR_COVERAGE);
            
            if (coverageData.has(Constants.PRIMARY_ARTIFACT) && 
                !coverageData.get(Constants.PRIMARY_ARTIFACT).isJsonNull()) {
                
                getLog().info("");
                getLog().info("***** Unit Test Coverage Summary *****");
                getLog().info("");
                
                JsonObject primaryArtifact = coverageData.getAsJsonObject(Constants.PRIMARY_ARTIFACT);
                printPrimaryArtifactCoverage(primaryArtifact);
                
                if (coverageData.has(Constants.SUPPORTING_ARTIFACTS) && 
                    !coverageData.get(Constants.SUPPORTING_ARTIFACTS).isJsonNull()) {
                    JsonArray supportingArtifacts = coverageData.getAsJsonArray(Constants.SUPPORTING_ARTIFACTS);
                    
                    if (supportingArtifacts.size() > 0) {
                        getLog().info("");
                        getLog().info("  Referenced Artifact Coverage:");
                        getLog().info("");
                        
                        for (int i = 0; i < supportingArtifacts.size(); i++) {
                            try {
                                JsonObject artifact = supportingArtifacts.get(i).getAsJsonObject();
                                printSupportingArtifactCoverage(artifact);
                            } catch (Exception e) {
                                if (getLog().isDebugEnabled()) {
                                    getLog().debug("Error processing supporting artifact at index " + i + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
                
                getLog().info("");
                getLog().info(">> For detailed coverage report, see: target" + System.getProperty(Constants.FILE_SEPARATOR) + 
                        Constants.REPORT_FILE_NAME);
                getLog().info("");
            }
        } catch (Exception e) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Error generating coverage report: " + e.getMessage());
            }
        }
    }

    /**
     * Check if coverage data is available in the test summary.
     *
     * @param jsonSummary test summary as a json
     * @return true if coverage data exists, false otherwise
     */
    private boolean hasCoverageData(JsonObject jsonSummary) {
        if (jsonSummary == null || !jsonSummary.has(Constants.MEDIATOR_COVERAGE) || 
            jsonSummary.get(Constants.MEDIATOR_COVERAGE).isJsonNull()) {
            return false;
        }
        
        JsonObject coverageData = jsonSummary.getAsJsonObject(Constants.MEDIATOR_COVERAGE);
        return coverageData.has(Constants.PRIMARY_ARTIFACT) && 
               !coverageData.get(Constants.PRIMARY_ARTIFACT).isJsonNull();
    }

    /**
     * Print primary artifact coverage information.
     *
     * @param artifact artifact json object
     */
    private void printPrimaryArtifactCoverage(JsonObject artifact) {
        if (artifact == null || !artifact.has(Constants.ARTIFACT_TYPE) || 
            !artifact.has(Constants.ARTIFACT_NAME) || !artifact.has(Constants.COVERAGE_PERCENTAGE)) {
            return;
        }
        
        String artifactName = artifact.get(Constants.ARTIFACT_NAME).getAsString();
        String coveragePercentage = artifact.get(Constants.COVERAGE_PERCENTAGE).getAsString();

        getLog().info("  Test Suite Coverage for " + artifactName + " : " + coveragePercentage + "%");
    }

    /**
     * Print supporting artifact coverage information.
     *
     * @param artifact artifact json object
     */
    private void printSupportingArtifactCoverage(JsonObject artifact) {
        if (artifact == null || !artifact.has(Constants.ARTIFACT_TYPE) || 
            !artifact.has(Constants.ARTIFACT_NAME) || !artifact.has(Constants.COVERAGE_PERCENTAGE)) {
            return;
        }
        
        String artifactType = artifact.get(Constants.ARTIFACT_TYPE).getAsString();
        String artifactName = artifact.get(Constants.ARTIFACT_NAME).getAsString();
        String coveragePercentage = artifact.get(Constants.COVERAGE_PERCENTAGE).getAsString();

        getLog().info("    • " + artifactName + " (" + artifactType + ") - " + coveragePercentage + "%");
    }

    /**
     * Generate failure detailed table data.
     *
     * @param jsonSummary test summary as a json
     * @return if failed occurred or not
     */
    private boolean generateTestFailureTable(JsonObject jsonSummary) {
        boolean isFailureOccurred = false;

        List<List<String>> errorRowsList = new ArrayList<>();
        List<List<String>> assertErrors = new ArrayList<>();
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
                    JsonArray failureAssertions = testJsonObject.getAsJsonArray(Constants.FAILURE_ASSERTIONS);

                    // failureAssertions will null for MI 1.1.0 or below versions
                    if (failureAssertions == null) {
                        failureSummary.add(Constants.TEST_CASE_VALUE +
                                testJsonObject.get(Constants.TEST_CASE_NAME).getAsString());
                        failureSummary.add(Constants.TWO_SPACES + Constants.ASSERTION_PHASE);
                        failureSummary.add(testJsonObject.get(Constants.EXCEPTION).getAsString());
                        errorRowsList.add(failureSummary);
                    } else {
                        for (int item = 0; item < failureAssertions.size(); item++) {
                            JsonObject assertFailures = failureAssertions.get(item).getAsJsonObject();

                            //Add test assertion failure abstract details
                            List<String> assertionSummary = new ArrayList<>();
                            String testCaseName = Constants.TEST_CASE_VALUE +
                                    testJsonObject.get(Constants.TEST_CASE_NAME).getAsString();
                            assertionSummary.add(testCaseName);
                            assertionSummary.add(Constants.TWO_SPACES + Constants.ASSERTION_PHASE);
                            assertionSummary.add(assertFailures.get(Constants.ASSERTION_MESSAGE).getAsString());
                            errorRowsList.add(assertionSummary);

                            //Add test assertion failure full details
                            List<String> failureAssertionInDetail = new ArrayList<>();
                            failureAssertionInDetail.add(testCaseName);
                            failureAssertionInDetail.add(assertFailures.get(Constants.ASSERTION_TYPE).getAsString() + " - "
                                    + assertFailures.get(Constants.ASSERTION_EXPRESSION).getAsString());

                            String assertionErrorMessage = "Actual Response: " + Constants.NEW_LINE_SEPARATOR
                                    + splitLongStrings(assertFailures.get(Constants.ASSERTION_ACTUAL).getAsString())
                                    + Constants.NEW_LINE_SEPARATOR;

                            if (!assertFailures.get(Constants.ASSERTION_EXPECTED).isJsonNull()) {
                                assertionErrorMessage += "Expected Response: " + Constants.NEW_LINE_SEPARATOR
                                        + splitLongStrings(assertFailures.get(Constants.ASSERTION_EXPECTED).getAsString());
                            }

                            if (!assertFailures.get(Constants.ASSERTION_DESCRIPTION).isJsonNull()) {
                                assertionErrorMessage += Constants.NEW_LINE_SEPARATOR + "Description: "
                                        + Constants.NEW_LINE_SEPARATOR
                                        + splitLongStrings(assertFailures.get(Constants.ASSERTION_DESCRIPTION).getAsString());
                            }

                            failureAssertionInDetail.add(assertionErrorMessage);
                            assertErrors.add(failureAssertionInDetail);
                        }
                    }
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
            String[] errorHeadersList = {"  TEST CASE  ", "  FAILURE STATE  ", "      EXCEPTION / ERROR MESSAGE     "};

            printDetailedTable(errorRowsList, 3, errorHeadersList);

            //generate test assertion failure detail table
            if (!assertErrors.isEmpty()) {
                getLog().info("Failed Assertion(s): ");
                String[] assertErrorHeadersList = {"  TEST CASE  ", "  ASSERT EXPRESSION  ", "      FAILURE     "};

                printDetailedTable(assertErrors, 3, assertErrorHeadersList);
            }
        }

        return isFailureOccurred;
    }

    /**
     * Split lengthy strings.
     *
     * @param text string which need to make it as multiline
     * @return multiline string
     */
    private String splitLongStrings(String text) {
        if (text.length() < 100) {
            return text + Constants.NEW_LINE_SEPARATOR;
        }

        int maxStringLength = 100;
        StringBuilder multilineConvertedString = new StringBuilder();
        for (int start = 0; start < text.length(); start += maxStringLength) {
            String currentLine = text.substring(start, Math.min(text.length(), start + maxStringLength));
            if (currentLine.contains(Constants.NEW_LINE_SEPARATOR)) {
                multilineConvertedString.append(currentLine);
            } else {
                multilineConvertedString.append(currentLine).append(Constants.NEW_LINE_SEPARATOR);
            }
        }

        return multilineConvertedString.toString();
    }

    /**
     * Generate detail tables in unit testing report.
     *
     * @param detailList list of unit test details
     * @param columnSize number of columns in the table
     * @param errorHeadersList header titles of the table
     */
    private void printDetailedTable(List<List<String>> detailList, int columnSize, String[] errorHeadersList) {
        String[][] errorRowsArray = new String[detailList.size()][columnSize];
        for (int x = 0 ; x < detailList.size(); x++) {
            List<String> innerList = detailList.get(x);
            String[] innerArray = new String[columnSize];
            innerArray = innerList.toArray(innerArray);
            errorRowsArray[x] = innerArray;
        }

        String errorTableString = ConsoleDataTable.of(errorHeadersList, errorRowsArray);

        String[] errorTableLines;
        if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {
            String windowsDefaultLineSeparator = System.getProperty(Constants.LINE_SEPARATOR_WIN);
            if (windowsDefaultLineSeparator == null) windowsDefaultLineSeparator = Constants.NEW_LINE_SEPARATOR;
            errorTableLines = errorTableString.split(windowsDefaultLineSeparator);
        } else {
            errorTableLines = errorTableString.split(System.getProperty(Constants.LINE_SEPARATOR));
        }
        for (String line : errorTableLines) {
            getLog().info(line);
        }
        getLog().info("");
    }

    private void setupLocalServer(String projectRootPath) throws Exception {
            URL downloadUrl = new URL(baseUrl + server.getServerVersion() + Constants.SLASH_WSO2_MI_WITH_DASH +
                    server.getServerVersion() + Constants.ZIP);
            String userHome = getUserHome();
            Path miDownloadPath = Paths.get(userHome, Constants.WSO2_MI, Constants.MICRO_INTEGRATOR);
            Path fullFilePath = Paths.get(miDownloadPath.toString(), Constants.WSO2_MI_WITH_DASH +
                    server.getServerVersion());
            Path zipFilePath = Paths.get(fullFilePath + Constants.ZIP);

            if (Files.notExists(fullFilePath) && Files.notExists(zipFilePath)) {
                if (isServerDownloadable(server.getServerVersion(), Constants.MI_4_3_0)) {
                    getLog().info("Downloading wso2mi-" + server.getServerVersion() + " server to \"" +
                            miDownloadPath + "\" for executing unit tests...");
                    if (Files.notExists(miDownloadPath)) {
                        Files.createDirectories(miDownloadPath);
                    }
                    downloadMiPack(downloadUrl.toString(), zipFilePath.toString());
                } else {
                    getLog().error("No downloadable server pack found for version: " + server.getServerVersion());
                }
            }

            if (Files.notExists(fullFilePath) && Files.exists(zipFilePath)) {
                unzipMiPack(zipFilePath.toString(), miDownloadPath.toString());
            }
            if (Files.exists(fullFilePath)) {
                FileUtils.copyDirectory(new File(Paths.get(projectRootPath, "deployment", "libs").toString()),
                        new File(Paths.get(miDownloadPath.toString(), Constants.WSO2_MI_WITH_DASH +
                                server.getServerVersion(), "lib").toString()));
            } else {
                throw new MojoExecutionException("No valid WSO2 Micro Integrator server path was found.");
            }

            String scriptPath;
            if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {
                scriptPath = Paths.get(miDownloadPath.toString(), Constants.WSO2_MI_WITH_DASH +
                        server.getServerVersion(), "bin", "micro-integrator.bat").toString();
            } else {
                scriptPath = Paths.get(miDownloadPath.toString(), Constants.WSO2_MI_WITH_DASH +
                        server.getServerVersion(), "bin", "micro-integrator.sh").toString();

            }
            File file = new File(scriptPath);
            file.setExecutable(true);
            server.setServerPath(scriptPath);
    }

    private String getUserHome() {
        if (System.getProperty(Constants.OS_TYPE).toLowerCase().contains(Constants.OS_WINDOWS)) {
            return System.getenv(Constants.USER_PROFILE);
        } else {
            return System.getenv(Constants.HOME);
        }
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            getLog().error("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private void downloadMiPack(String downloadFileUrl, String outputFile) {
        try {
            Process p = new ProcessBuilder("curl", "-f", "-L", "-o", outputFile, downloadFileUrl)
                    .inheritIO().start();
            int exit = p.waitFor();
            if (exit == 0) {
                getLog().info("MI pack downloaded successfully to " + outputFile);
            } else {
                String errorMsg = new BufferedReader(new InputStreamReader(p.getErrorStream()))
                        .lines().collect(Collectors.joining(System.lineSeparator()));
                getLog().error("Unsuccessful download. Error: " + errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            getLog().error("An error occurred when downloading MI pack: " + e.getMessage());
        }
    }

    private void unzipMiPack(String fullFilePath, String miDownloadPath)  {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(fullFilePath)))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String name = zipEntry.getName();
                if (name.startsWith("__MACOSX/") || name.contains("/._") || name.startsWith("._")) {
                    zipEntry = zipInputStream.getNextEntry();
                    continue;
                }

                File newFile = newFile(new File(miDownloadPath), zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        getLog().error("Failed to create directory for unzipping pack: " + newFile);
                    }
                } else {
                    // Fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        getLog().error("Failed to create directory for unzipping pack: " + parent);
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            getLog().error("An error occurred when unzipping MI pack: ", e);
        }
    }

    private boolean isServerDownloadable(String currentVersion, String targetVersion) {

        ComparableVersion current = new ComparableVersion(currentVersion);
        ComparableVersion target = new ComparableVersion(targetVersion);
        return current.compareTo(target) >= 0;
    }
}
