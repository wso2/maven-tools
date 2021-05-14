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

package org.wso2.maven.car.artifact.util;

import org.wso2.maven.car.artifact.CAppMgtServiceStub;
import org.wso2.maven.car.artifact.impl.HTTPClientBuilderServiceImpl;

import static org.wso2.maven.car.artifact.util.Constants.CLIENT_CONNECTION_TIMEOUT;
import static org.wso2.maven.car.artifact.util.Constants.CLIENT_READ_TIMEOUT;

public class CAppMgtHandlerFactory {

    private static HTTPClientBuilderServiceImpl instance = new HTTPClientBuilderServiceImpl();

    private CAppMgtHandlerFactory(){}

    /**
     * Returns an HTTPS client for communicating with the MI server with basic auth.
     *
     * @param serverUrl HTTPS URL of the MI server.
     * @param username Username.
     * @param password Password.
     * @return ErrorHandlerServiceStub instance which functions as the HTTPS client.
     */
    public static CAppMgtServiceStub getCAppMgtHttpsClient(String serverUrl, String username,
                                                                           String password) {
        return instance.buildWithBasicAuth(username, password,
                CLIENT_CONNECTION_TIMEOUT, CLIENT_READ_TIMEOUT, CAppMgtServiceStub.class, serverUrl);
    }

    /**
     * Returns an HTTPS client for communicating with the MI server with JWT and form encoder to handle multipart data.
     *
     * @param serverUrl HTTPS URL of the MI server.
     * @param accessToken access token.
     * @return ErrorHandlerServiceStub instance which functions as the HTTPS client.
     */
    public static CAppMgtServiceStub getCAppMgtHttpsClient2(String serverUrl, String accessToken) {
        return instance.buildWithJWTAndFormEncoder(accessToken,
                CLIENT_CONNECTION_TIMEOUT, CLIENT_READ_TIMEOUT, CAppMgtServiceStub.class, serverUrl);
    }

    /**
     * Returns an HTTPS client for communicating with the MI server with JWT token.
     *
     * @param serverUrl HTTPS URL of the MI server.
     * @param accessToken access token.
     * @return ErrorHandlerServiceStub instance which functions as the HTTPS client.
     */
    public static CAppMgtServiceStub getCAppMgtHttpsClient3(String serverUrl, String accessToken) {
        return instance.buildWithJWT(accessToken,
                CLIENT_CONNECTION_TIMEOUT, CLIENT_READ_TIMEOUT, CAppMgtServiceStub.class, serverUrl);
    }

}
