/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.handlers;

import org.wso2.maven.CloudMojo;
import org.wso2.maven.exceptions.KubernetesPluginException;

import java.util.LinkedHashSet;
import java.util.Set;

public class HandlerFactory {

    Set<AbstractArtifactHandler> handlers = new LinkedHashSet<>();

    public HandlerFactory(CloudMojo cloudMojo) {
        handlers.add(new ServiceHandler(cloudMojo));
        handlers.add(new ConfigMapHandler(cloudMojo));
        handlers.add(new SecretHandler(cloudMojo));
        handlers.add(new DeploymentHandler(cloudMojo));
        handlers.add(new HPAHandler(cloudMojo));
    }

    public void createArtifacts() throws KubernetesPluginException {
        for (AbstractArtifactHandler handler : handlers) {
            handler.createArtifacts();
        }
    }
}
