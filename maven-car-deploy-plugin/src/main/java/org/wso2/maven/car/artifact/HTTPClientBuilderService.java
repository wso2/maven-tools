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

package org.wso2.maven.car.artifact;

import feign.Client;

public interface HTTPClientBuilderService {

    /**
     * Function to create new Feign client instance.
     *
     * @return Feign Client instance
     */
    Client newDefaultClientInstance();

    /**
     * Function to build Feign client factory.
     *
     * @param username User name to be used in auth header
     * @param password Password to be used in auth header
     * @param connectTimeoutMillis Connection timeout of the request
     * @param readTimeoutMillis Read timeout of the request
     * @param target target service stubs to be used by the factory
     * @param url Base url of the API to be created
     * @return Feign client factory
     */
    <T> T buildWithBasicAuth(String username, String password, int connectTimeoutMillis,
                int readTimeoutMillis, Class<T> target, String url);

    /**
     * Function to build Feign client factory with FormEncoder.
     *
     * @param accessToken accessToken to be used in auth header
     * @param connectTimeoutMillis Connection timeout of the request
     * @param readTimeoutMillis Read timeout of the request
     * @param target target service stubs to be used by the factory
     * @param url Base url of the API to be created
     * @return Feign client factory
     */
    <T> T buildWithJWTAndFormEncoder(String accessToken, int connectTimeoutMillis,
                               int readTimeoutMillis, Class<T> target, String url);

    /**
     * Function to build Feign client factory.
     *
     * @param accessToken accessToken to be used in auth header
     * @param connectTimeoutMillis Connection timeout of the request
     * @param readTimeoutMillis Read timeout of the request
     * @param target target service stubs to be used by the factory
     * @param url Base url of the API to be created
     * @return Feign client factory
     */
    <T> T buildWithJWT(String accessToken, int connectTimeoutMillis,
                             int readTimeoutMillis, Class<T> target, String url);

}
