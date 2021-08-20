/*
 *
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.maven.car.artifact;

import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * This class provides an interface for all CApp handlers.
 */
public interface CAppHandler {

    /**
     * Executes the CApp deploy logic relevant to the handler.
     *
     * @param username  Username
     * @param password  Password
     * @param serverUrl Url of the server
     * @param carFile   .car file
     * @throws Exception if CApp deploy fails
     */
    void deployCApp(String username, String password, String serverUrl, File carFile) throws Exception;

    /**
     * Executes the CApp undeploy logic relevant to the handler.
     *
     * @param username  Username
     * @param password  Password
     * @param serverUrl Url of the server
     * @param project   Maven project to extract the CApp properties
     * @throws Exception if CApp undeploy fails
     */
    void unDeployCApp(String username, String password, String serverUrl, MavenProject project) throws Exception;

}
