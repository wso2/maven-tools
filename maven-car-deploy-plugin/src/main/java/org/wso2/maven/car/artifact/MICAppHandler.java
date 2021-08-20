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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.json.JSONObject;
import org.wso2.maven.car.artifact.impl.CAppMgtApiHelperServiceImpl;

import java.io.File;

/**
 * This class implements CAppHandler to implement the CApp deploy/undeploy logic for MI Servers.
 */
public class MICAppHandler implements CAppHandler {

    private final CAppMgtApiHelperServiceImpl capppMgtApiHelperServiceImpl;
    private final Log logger;

    public MICAppHandler(Log logger) {

        this.logger = logger;
        this.capppMgtApiHelperServiceImpl = new CAppMgtApiHelperServiceImpl();
    }

    @Override
    public void deployCApp(String username, String password, String serverUrl, File carFile) throws Exception {

        JSONObject resObj = capppMgtApiHelperServiceImpl.doAuthenticate(serverUrl, username, password);
        if (resObj != null) {
            logger.info("Authentication to " + serverUrl + " successful.");
            String accessToken = resObj.getString("AccessToken");
            if (accessToken != null && !accessToken.equals("")) {
                if (capppMgtApiHelperServiceImpl.deployCApp(carFile, accessToken, serverUrl)) {
                    logger.info("Uploaded " + carFile.getName() + " to " + serverUrl + " ...");
                }
            }
        }
    }

    @Override
    public void unDeployCApp(String username, String password, String serverUrl, MavenProject project)
            throws Exception {

        JSONObject resObj = capppMgtApiHelperServiceImpl.doAuthenticate(serverUrl, username, password);
        if (resObj != null) {
            logger.info("Authentication to " + serverUrl + " successful.");
            String accessToken = resObj.getString("AccessToken");
            if (accessToken != null && !accessToken.equals("")) {
                if (capppMgtApiHelperServiceImpl.unDeployCApp(accessToken, serverUrl,
                        project.getArtifactId() + "_" + project.getVersion())) {
                    logger.info("Located the C-App " + project.getArtifactId() +
                            "_" + project.getVersion() + " and undeployed ...");
                }
            }
        }
    }
}
