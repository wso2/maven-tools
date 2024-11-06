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

package org.wso2.maven.handler;

import edu.emory.mathcs.backport.java.util.Collections;
import org.wso2.maven.DockerMojo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HandlerFactory {

    private final List<ArtifactHandler> artifactHandlers = new ArrayList<>();

    public HandlerFactory(DockerMojo dockerMojo, Path tmpCarbonHomeDirectory) {
        artifactHandlers.add(new LibArtifactHandler(dockerMojo, tmpCarbonHomeDirectory));
        artifactHandlers.add(new SynapseArtifactHandler(dockerMojo, tmpCarbonHomeDirectory));
        artifactHandlers.add(new DataserviceArtifactHandler(dockerMojo, tmpCarbonHomeDirectory));
        artifactHandlers.add(new ConnectorArtifactHandler(dockerMojo, tmpCarbonHomeDirectory));
        artifactHandlers.add(new RegistryResourceHandler(dockerMojo, tmpCarbonHomeDirectory));
    }

    public List<ArtifactHandler> getArtifactHandlers() {
        return Collections.unmodifiableList(artifactHandlers);
    }
}
