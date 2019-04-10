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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */
@Mojo(name = "SynapseUnitTest")
public class UnitTestCasesMojo extends AbstractMojo {

    @Parameter(property = "testCasesFilePath")
    private String testCasesFilePath;

    @Parameter(property = "synapseServer")
    private SynapseServer synapseServer;

    /**
     * Execution method of Mojo class.
     */
    public void execute() throws MojoExecutionException {

        try {
            appendTestLogs();
            startTestingServer();
            testCaseRunner();
        } catch (Exception e) {
            throw new MojoExecutionException("Exception occurred while running test cases " + e.getMessage());
        } finally {
            stopTestingServer();
        }
    }

    /**
     * Execution method of Mojo class.
     */
    private void testCaseRunner() throws IOException {

        ArrayList<String> synapseTestCasePaths = getTestCasesFileNamesWithPaths(testCasesFilePath);

        getLog().info("Detect " + synapseTestCasePaths.size() + " Synapse test case files to execute\n");

        for (String synapseTestCaseFile : synapseTestCasePaths) {

            String responseFromUnitTestFramework = UnitTestClient.executeTests
                    (synapseTestCaseFile, synapseServer.getHost(), synapseServer.getUnitTestPort());

            if (responseFromUnitTestFramework != null) {
                isAssertResults(responseFromUnitTestFramework, synapseTestCaseFile);
            } else {
                throw new IOException("Test case parsing failed ");
            }
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
        File folder = new File(testFileFolder);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                String filename = file.getName();
                if (filename.endsWith(".xml") || filename.endsWith(".XML")) {

                    fileNamesWithPaths.add(testFileFolder + File.separator + filename);
                }
            }
        }

        return fileNamesWithPaths;
    }

    /**
     * Assert the expected value with receiving response from unit test client.
     *
     * @param response            response from client
     * @param synapseTestCaseFile test-case file
     */
    private void isAssertResults(String response, String synapseTestCaseFile) throws IOException {
        String expectedResponse = "{\"test-cases\":\"SUCCESS\"}";

        if (!response.equals(expectedResponse)) {
            throw new IOException("Test Cases are failed - " + response);
        }

        getLog().info("SynapseTestCaseFile " + synapseTestCaseFile + " passed successfully");
        getLog().info(" ");
    }

    /**
     * Append log of UNIT-TEST when testing started.
     */
    private void appendTestLogs() {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("U N I T - T E S T S");
        getLog().info("------------------------------------------------------------------------");

    }

    /**
     * Start the Unit testing agent server if user defined it in configuration.
     */
    private void startTestingServer() throws IOException {

        String unitTestPort = synapseServer.getUnitTestPort();
        //set default port in client side whether receiving port is empty
        if(unitTestPort == null || unitTestPort.isEmpty()) {
            synapseServer.setUnitTestPort("9008");
            unitTestPort = synapseServer.getUnitTestPort();
        }

        //check already has unit testing server
        if (!synapseServer.getLocalServer().isEmpty()) {
            String synapseServerPath = synapseServer.getLocalServer().get(0);

            //execute local unit test server by given path and port
            String[] cmd = { synapseServerPath, "-DsynapseTest", "-DsynapseTestPort="+unitTestPort};
            Process processor = Runtime.getRuntime().exec(cmd);

            getLog().info("Starting unit testing agent of path - " + synapseServerPath);
            getLog().info("Waiting for testing agent initialization");
            getLog().info("Start logging the server initialization \n");

            //log the server output
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(processor.getInputStream()));

            //check port availability - unit test server started or not
            boolean isServerNotStarted = true;

            //set timeout time to 120 seconds
            long timeoutExpiredMs = System.currentTimeMillis() + 120000;
            while (isServerNotStarted) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isServerNotStarted = checkPortAvailability(Integer.parseInt(unitTestPort));
                getLog().info(inputBuffer.readLine());

                if (waitMillis <= 0) {
                    // timeout expired
                    throw new IOException("Connection refused for service in port - " + unitTestPort);
                }
            }

            inputBuffer.close();
            getLog().info("Unit testing agent started\n");

        } else {
            getLog().info("Testing will execute with already started unit testing agent");
        }

    }

    /**
     * Stop the Unit testing agent server.
     */
    private void stopTestingServer() {

        String unitTestPort = synapseServer.getUnitTestPort();

        if (!synapseServer.getLocalServer().isEmpty()) {
            try {
                getLog().info("Stopping unit testing agent runs on port " + unitTestPort);
                BufferedReader bufferReader;

                //check running operating system
                if (System.getProperty("os.name").toLowerCase().contains("win")) {

                    Runtime runTime = Runtime.getRuntime();
                    Process proc =
                            runTime.exec("cmd /c netstat -ano | findstr " + unitTestPort);

                    bufferReader = new BufferedReader(new
                            InputStreamReader(proc.getInputStream()));

                    String stream = bufferReader.readLine();
                    if (stream != null) {
                        int index = stream.lastIndexOf(' ');
                        String pid = stream.substring(index);

                        getLog().info("Unit testing agent runs with PID of " + pid);
                        runTime.exec("cmd /c Taskkill /PID" + pid + " /T /F");
                    }

                } else {
                    Process pr = Runtime.getRuntime().exec("lsof -t -i:" + unitTestPort);
                    bufferReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    String pid = bufferReader.readLine();

                    getLog().info("Unit testing agent runs with PID of " + pid);
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }

                bufferReader.close();
                getLog().info("Unit testing agent stopped");

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
            socket.connect(new InetSocketAddress(synapseServer.getHost(), port));
            isPortAvailable = false;
        } catch (IOException e) {
            isPortAvailable = true;
        }

        return isPortAvailable;
    }
}
