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
import java.util.List;

public class ConnectorArtifactHandler implements ArtifactHandler {

    private final DockerMojo dockerMojo;
    private final Path tmpCarbonHomeDir;

    public ConnectorArtifactHandler(DockerMojo dockerMojo, Path tmpCarbonHomeDir) {
        this.dockerMojo = dockerMojo;
        this.tmpCarbonHomeDir = tmpCarbonHomeDir;
    }

    @Override
    public void copyArtifacts(Application application) {
        Path destArtifactDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.SYNAPSE_LIBS);
        try {
            Files.createDirectories(destArtifactDirPath);
        } catch (IOException e) {
            dockerMojo.logError(String.format("Error occurred while creating the directory: %s. error: %s",
                    destArtifactDirPath, e.getMessage()));
        }

        List<Dependency> dependencies = application.getApplicationArtifact().getDependencies();

        dependencies.forEach(dependency -> {
            if (HandlerConstants.SYNAPSE_LIBRARY_TYPE.equals(dependency.getArtifact().getType())) {
                deployArtifacts(dependency.getArtifact(), tmpCarbonHomeDir);
            }
        });
    }

    private void deployArtifacts(Artifact artifact, Path tmpCarbonHomeDir) {
        if (HandlerUtils.validateArtifact(artifact) && artifact.getFile() != null) {
            // Copy mediator artifacts to the tmp docker directory
            Path destArtifactDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.SYNAPSE_LIBS);
            Path srcArtifactPath = Paths.get(artifact.getExtractedPath()).resolve(artifact.getFile());
            Path destArtifactPath = destArtifactDirPath.resolve(artifact.getFile());

            try {
                dockerMojo.getLog().debug("Copying from: " + srcArtifactPath + " to: " + destArtifactPath);
                Files.createDirectories(destArtifactDirPath);
                Files.copy(srcArtifactPath, destArtifactPath);
            } catch (Exception e) {
                dockerMojo.logError("Error occurred while copying artifact: " + artifact.getFile());
            }

            // Create import file for the connector inside synapse-configs/import directory
            // The import filename should be {org.wso2.carbon.connector}<CONNECTOR_NAME>.xml format
            // The content should be as follows
            //    <import xmlns="http://ws.apache.org/ns/synapse"
            //            name=<CONNECTOR_NAME>
            //            package="org.wso2.carbon.connector"
            //            status="enabled"/>
            Path importDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.SYNAPSE_CONFIGS_DIR_PATH).resolve(HandlerConstants.SYNAPSE_IMPORTS_FOLDER);
            String importFilename = "{org.wso2.carbon.connector}" + artifact.getConnectorName() + ".xml";
            Path importFilePath = tmpCarbonHomeDir.resolve(HandlerConstants.SYNAPSE_CONFIGS_DIR_PATH).resolve(HandlerConstants.SYNAPSE_IMPORTS_FOLDER)
                    // The connector name is the short name of the connector. e.g: name:file-connector -> file
                    .resolve(importFilename);

            String importFileContent = "<import xmlns=\"http://ws.apache.org/ns/synapse\" name=\"" + artifact.getConnectorName() + "\" package=\"org.wso2.carbon.connector\" status=\"enabled\"/>";

            try {
                dockerMojo.getLog().debug("Creating import file: " + importFilePath + " with content: " + importFileContent);
                Files.createDirectories(importDirPath);
                Files.write(importFilePath, importFileContent.getBytes());
            } catch (Exception e) {
                dockerMojo.logError("Error occurred while writing import file: " + importFilename);
            }
        }
    }
}
