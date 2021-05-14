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

package org.wso2.maven.car.artifact.api;

import org.json.JSONObject;
import org.wso2.maven.car.artifact.exception.CAppMgtServiceStubException;

import java.io.File;

/**
 * Describes methods that communicate with the MI server.
 */
public interface CAppMgtApiHelperService {

    JSONObject doAuthenticate(String serverUrl, String username, String password)
            throws CAppMgtServiceStubException;

    boolean deployCApp(File capp, String accessToken, String serverUrl) throws CAppMgtServiceStubException;

    boolean unDeployCApp(String accessToken, String serverUrl, String cAppName)
            throws CAppMgtServiceStubException;
}
