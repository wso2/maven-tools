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

package org.wso2.maven.car.artifact.impl;

import feign.Client;
import feign.Feign;
import feign.Request;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import org.wso2.maven.car.artifact.HTTPClientBuilderService;

public class HTTPClientBuilderServiceImpl implements HTTPClientBuilderService {

    public Client newDefaultClientInstance() {
        return new Client.Default(null, null);
    }

    @Override
    public <T> T buildWithBasicAuth(String username, String password, int connectTimeoutMillis, int readTimeoutMillis,
                       Class<T> target, String url) {
        return Feign.builder().requestInterceptor(new BasicAuthRequestInterceptor(username, password))
                .encoder(new Encoder.Default()).decoder(new Decoder.Default())
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .client(newDefaultClientInstance())
                .target(target, url);
    }

    @Override
    public <T> T buildWithJWTAndFormEncoder(String accessToken, int connectTimeoutMillis,
                                      int readTimeoutMillis, Class<T> target, String url) {
        return Feign.builder().requestInterceptor(new JWTAuthRequestInterceptor(accessToken))
                .encoder(new FormEncoder())
                .decoder(new Decoder.Default())
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .client(newDefaultClientInstance())
                .target(target, url);
    }

    @Override
    public <T> T buildWithJWT(String accessToken, int connectTimeoutMillis, int readTimeoutMillis, Class<T> target,
                              String url) {
        return Feign.builder().requestInterceptor(new JWTAuthRequestInterceptor(accessToken))
                .encoder(new Encoder.Default()).decoder(new Decoder.Default())
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .client(newDefaultClientInstance())
                .target(target, url);
    }

}
