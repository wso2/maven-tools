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
 * Class responsible for hold SynapseServer data.
 */
public class SynapseServer {

    private String testServerType;
    private String testServerHost;
    private String testServerPort;
    private String testServerPath;

    /**
     * Get server type.
     *
     * @return type of the server
     */
    String getServerType() {
        return testServerType;
    }

    /**
     * Get server host.
     *
     * @return host of the server
     */
    String getServerHost() {
        return testServerHost;
    }

    /**
     * Get server port.
     *
     * @return port of the server
     */
    String getServerPort() {
        return testServerPort;
    }

    /**
     * Set server type.
     *
     * @param type of the server
     */
    public void setServerType(String type) {
        this.testServerType = type;
    }

    /**
     * Set server host.
     *
     * @param host of the server
     */
    public void setServerHost(String host) {
        this.testServerHost = host;
    }

    /**
     * Set server port.
     *
     * @param port of the server
     */
    public void setServerPort(String port) {
        this.testServerPort = port;
    }

    /**
     * Get local server details.
     *
     * @return local server details
     */
    String getServerPath() {
        return testServerPath;
    }

    /**
     * Set local server details.
     *
     * @param localServer local server details
     */
    public void setServerPath(String localServer) {
        this.testServerPath = localServer;
    }
}
