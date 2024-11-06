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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class LibArtifactHandler implements ArtifactHandler {

    private final DockerMojo dockerMojo;
    private final Path tmpCarbonHomeDir;

    private static final String LIBS_DIR = "lib";

    public LibArtifactHandler(DockerMojo dockerMojo, Path tmpCarbonHomeDir) {
        this.dockerMojo = dockerMojo;
        this.tmpCarbonHomeDir = tmpCarbonHomeDir;
    }
    @Override
    public void copyArtifacts(Application application) {
        List<Dependency> dependencies = application.getApplicationArtifact().getDependencies();

        dependencies.forEach(dependency -> {
            if (HandlerConstants.MEDIATOR_TYPE.equals(dependency.getArtifact().getType())) {
                deployArtifacts(dependency.getArtifact(), tmpCarbonHomeDir);
            }
        });
    }

    private void deployArtifacts(Artifact artifact, Path tmpCarbonHomeDir) {
        if (HandlerUtils.validateArtifact(artifact) && artifact.getFile() != null) {
            // Copy mediator artifacts to the tmp docker directory
            Path destArtifactDirPath = tmpCarbonHomeDir.resolve(LIBS_DIR);
            Path srcArtifactPath = Paths.get(artifact.getExtractedPath()).resolve(artifact.getFile());
            Path destArtifactPath = destArtifactDirPath.resolve(artifact.getFile());

            try {
                dockerMojo.getLog().debug("Copying from: " + srcArtifactPath + " to: " + destArtifactPath);
                Files.createDirectories(destArtifactDirPath);
                Files.copy(srcArtifactPath, destArtifactPath, StandardCopyOption.REPLACE_EXISTING);

            } catch (Exception e) {
                dockerMojo.logError("Error occurred while copying artifact: " + artifact.getFile());
            }
        }
    }
}
