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

import org.wso2.maven.DockerMojo;
import org.wso2.maven.metadata.Application;
import org.wso2.maven.metadata.Artifact;
import org.wso2.maven.metadata.Dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SynapseArtifactHandler implements ArtifactHandler {

    private final DockerMojo dockerMojo;
    private final Path tmpCarbonHomeDir;

    public SynapseArtifactHandler(DockerMojo dockerMojo, Path tmpCarbonHomeDir) {
        this.dockerMojo = dockerMojo;
        this.tmpCarbonHomeDir = tmpCarbonHomeDir;
    }

    @Override
    public void copyArtifacts(Application application) {
        List<Dependency> dependencies = application.getApplicationArtifact().getDependencies();
        Map<String, List<Dependency>> orderedArtifactsWithTypes = getOrderedArtifactsWithTypes(dependencies);

        orderedArtifactsWithTypes.forEach((type, deps) -> {
            deployArtifacts(type, deps, tmpCarbonHomeDir);
        });
    }

    private void deployArtifacts(String type, List<Dependency> deps, Path tmpCarbonHomeDir) {
        deps.forEach(dep -> {
            Artifact artifact = dep.getArtifact();
            String artifactType = artifact.getType();
            String artifactDirName = getArtifactDirName(artifactType);

            if (!HandlerUtils.validateArtifact(artifact) || artifactDirName == null) {
                return;
            }

            // Copy capp artifacts to the tmp docker directory
            Path destArtifactDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.SYNAPSE_CONFIGS_DIR_PATH).resolve(artifactDirName);
            Path srcArtifactPath = Paths.get(artifact.getExtractedPath()).resolve(artifact.getFile());
            Path destArtifactPath = destArtifactDirPath.resolve(artifact.getFile());

            try {
                dockerMojo.getLog().debug("Copying from: " + srcArtifactPath + " to: " + destArtifactPath);
                Files.createDirectories(destArtifactDirPath);
                Files.copy(srcArtifactPath, destArtifactPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                dockerMojo.logError("Error occurred while copying artifact: " + artifact.getFile());
            }
        });
    }

    private String getArtifactDirName(String artifactType) {
        switch (artifactType) {
            case HandlerConstants.SEQUENCE_TYPE:
                return HandlerConstants.SEQUENCES_FOLDER;
            case HandlerConstants.ENDPOINT_TYPE:
                return HandlerConstants.ENDPOINTS_FOLDER;
            case HandlerConstants.PROXY_SERVICE_TYPE:
                return HandlerConstants.PROXY_SERVICES_FOLDER;
            case HandlerConstants.LOCAL_ENTRY_TYPE:
                return HandlerConstants.LOCAL_ENTRIES_FOLDER;
            case HandlerConstants.EVENT_SOURCE_TYPE:
                return HandlerConstants.EVENTS_FOLDER;
            case HandlerConstants.TASK_TYPE:
                return HandlerConstants.TASKS_FOLDER;
            case HandlerConstants.MESSAGE_STORE_TYPE:
                return HandlerConstants.MESSAGE_STORE_FOLDER;
            case HandlerConstants.MESSAGE_PROCESSOR_TYPE:
                return HandlerConstants.MESSAGE_PROCESSOR_FOLDER;
            case HandlerConstants.API_TYPE:
                return HandlerConstants.APIS_FOLDER;
            case HandlerConstants.TEMPLATE_TYPE:
                return HandlerConstants.TEMPLATES_FOLDER;
            case HandlerConstants.INBOUND_ENDPOINT_TYPE:
                return HandlerConstants.INBOUND_ENDPOINT_FOLDER;
            default:
                return null;
        }
    }

    private Map<String, List<Dependency>> getOrderedArtifactsWithTypes(List<Dependency> dependencies) {
        Map<String, List<Dependency>> artifactTypeMap = new LinkedHashMap<>();

        // Initialize the map with all possible artifact types
        String[] artifactTypes = {
                HandlerConstants.MEDIATOR_TYPE,
                HandlerConstants.SEQUENCE_TYPE,
                HandlerConstants.ENDPOINT_TYPE,
                HandlerConstants.PROXY_SERVICE_TYPE,
                HandlerConstants.LOCAL_ENTRY_TYPE,
                HandlerConstants.EVENT_SOURCE_TYPE,
                HandlerConstants.TASK_TYPE,
                HandlerConstants.MESSAGE_STORE_TYPE,
                HandlerConstants.MESSAGE_PROCESSOR_TYPE,
                HandlerConstants.API_TYPE,
                HandlerConstants.TEMPLATE_TYPE,
                HandlerConstants.INBOUND_ENDPOINT_TYPE,
                HandlerConstants.OTHER_TYPE
        };

        for (String type : artifactTypes) {
            artifactTypeMap.put(type, new ArrayList<>());
        }

        // Categorize artifacts based on the artifact type
        for (Dependency dep : dependencies) {
            String type = dep.getArtifact().getType();
            artifactTypeMap.getOrDefault(type, artifactTypeMap.get(HandlerConstants.OTHER_TYPE)).add(dep);
        }

        return artifactTypeMap;
    }
}
