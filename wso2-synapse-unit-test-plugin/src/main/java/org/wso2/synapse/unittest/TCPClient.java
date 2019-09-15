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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP client for initializing the socket and sending and receiving data through the socket.
 */
class TCPClient {

    private Socket clientSocket;
    private OutputStream outputStream;
    private static Log log;

    /**
     * Initializing the TCP socket.
     *
     * @param synapseHost socket initializing host
     * @param synapsePort socket initializing port
     */
    TCPClient(String synapseHost, String synapsePort) {

        try {
            clientSocket = new Socket(synapseHost, Integer.parseInt(synapsePort));
            if (getLog().isDebugEnabled()) {
                getLog().debug("TCP socket connection has been established");
            }

        } catch (IOException e) {
            getLog().error("Error in initializing the socket", e);
        }
    }

    /**
     * Method of receiving response from the synapse unit test agent.
     *
     * @return response from the unit testing server
     */
    String readData() {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Waiting for synapse unit test agent response");
        }

        try (InputStream inputStream = clientSocket.getInputStream();
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {

            return (String) objectInputStream.readObject();
        } catch (Exception e) {
            getLog().error("Error in getting response from the synapse unit test agent", e);
            return null;
        }
    }

    /**
     * Method of sending the artifact and test data to the synapse unit test agent.
     *
     * @param messageToBeSent deployable JSON object with artifact and test case data
     */
    void writeData(String messageToBeSent) {

        try {
            outputStream = clientSocket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(messageToBeSent);
            outputStream.flush();

            if (getLog().isDebugEnabled()) {
                getLog().debug("Artifact configurations and test cases data sent to the synapse agent successfully");
            }
        } catch (IOException e) {
            getLog().error("Error while sending deployable data to the synapse agent ", e);
        }
    }

    /**
     * Method of closing connection of TCP.
     */
    void closeSocket() {
        try {
            outputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            getLog().error("Error while closing TCP client socket connection", e);
        }
    }

    /**
     * Method of initiating logger.
     */
    private static Log getLog() {
        if (log == null) {
            log = new SystemStreamLog();
        }

        return log;
    }
}
