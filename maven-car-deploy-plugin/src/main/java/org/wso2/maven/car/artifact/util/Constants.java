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

public class Constants {

    public static final int CLIENT_CONNECTION_TIMEOUT = 5000;
    public static final int CLIENT_READ_TIMEOUT = 5000;
    public static final String CONFIG_OPERATION_DEPLOY = "deploy";
    public static final String CONFIG_OPERATION_UNDEPLOY = "undeploy";
    public static final String MI_SERVER = "mi";
    public static final String EI_SERVER = "ei";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BASIC = "Basic ";
    public static final String ACCESS_TOKEN = "AccessToken";
}
