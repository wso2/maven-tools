/*
 *
 *  * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.maven.car.artifact.impl;

import feign.Response;
import feign.RetryableException;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.wso2.maven.car.artifact.api.CAppMgtApiHelperService;
import org.wso2.maven.car.artifact.exception.CAppMgtServiceStubException;
import org.wso2.maven.car.artifact.util.HTTPSClientUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CAppMgtApiHelperServiceImpl implements CAppMgtApiHelperService {

    private String getAsString(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream);
    }

    @Override
    public JSONObject doAuthenticate(String serverUrl, String username, String password)
            throws CAppMgtServiceStubException {
        try {
            Response response = HTTPSClientUtil.doAuthenticate(serverUrl, username, password);
            if (response.status() == 200 && response.body() != null) {
                return new JSONObject(getAsString(response.body().asInputStream()));
            }
            throw new CAppMgtServiceStubException("Failed to authenticate. " + response.reason());
        } catch (RetryableException e) {
            throw new CAppMgtServiceStubException (
                    String.format("Cannot connect to the server node %s.", serverUrl), e);
        } catch (IOException e) {
            throw new CAppMgtServiceStubException ("Failed to read the response body.", e);
        }
    }

    @Override
    public boolean deployCApp(File capp, String accessToken, String serverUrl) throws CAppMgtServiceStubException {
        try {
            Response response = HTTPSClientUtil.deployCApp(capp, accessToken, serverUrl);
            if (response.status() == 200) {
                return true;
            }
            throw new CAppMgtServiceStubException("Could not deploy the capp to the server." + response.reason());
        } catch (RetryableException e) {
            throw new CAppMgtServiceStubException (
                    String.format("Cannot connect to the server node %s.", serverUrl), e);
        }
    }

    @Override
    public boolean unDeployCApp(String accessToken, String serverUrl, String cAppName)
            throws CAppMgtServiceStubException {
        try {
            Response response = HTTPSClientUtil.unDeployCApp(accessToken, serverUrl, cAppName);
            if (response.status() == 200) {
                return true;
            }
            throw new CAppMgtServiceStubException("Could not unDeploy the capp to the server." + response.reason());
        } catch (RetryableException e) {
            throw new CAppMgtServiceStubException (
                    String.format("Cannot connect to the server node %s.", serverUrl), e);
        }
    }

}
