/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.commons;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;

import java.io.File;

/**
 * Wrapper class containing P2ApplicationLauncher which makes configuring the P2ApplicationLauncher easier.
 */
public class P2ApplicationLaunchManager {

    private final P2ApplicationLauncher launcher;

    public P2ApplicationLaunchManager(P2ApplicationLauncher launcher) {
        this.launcher = launcher;
    }

    public void setWorkingDirectory(File workingDir) {
        this.launcher.setWorkingDirectory(workingDir);
    }

    public void setApplicationName(String applicationName) {
        this.launcher.setApplicationName(applicationName);
    }

    public void addRepoGenerationArguments(String sourceDir, String metadataRepoLocation, String metadataRepositoryName,
                                           String repositoryName) throws MojoExecutionException {
        try {
            launcher.addArguments("-source", sourceDir,
                    "-metadataRepository", metadataRepoLocation,
                    "-metadataRepositoryName", metadataRepositoryName,
                    "-artifactRepository", metadataRepoLocation,
                    "-artifactRepositoryName", repositoryName,
                    "-publishArtifacts",
                    "-publishArtifactRepository",
                    "-compress",
                    "-append");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed when configuring P2ApplicationLauncher", e);
        }
    }

    public void addUpdateRepoWithCategoryArguments(String metadataRepositoryLocation, String categoryDefinitionFile)
            throws MojoExecutionException {
        try {
            launcher.addArguments("-metadataRepository", metadataRepositoryLocation,
                    "-categoryDefinition", categoryDefinitionFile,
                    "-categoryQualifier",
                    "-compress",
                    "-append");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed when configuring P2ApplicationLauncher", e);
        }
    }

    public void addArgumentsToInstallFeatures(String metadataRepositoryLocation, String artifactRepositoryLocation,
                                              String installUIs, String destination, String profile)
            throws MojoExecutionException {
        try {
            launcher.addArguments(
                    "-metadataRepository", metadataRepositoryLocation,
                    "-artifactRepository", artifactRepositoryLocation,
                    "-profileProperties", "org.eclipse.update.install.features=true",
                    "-installIU", installUIs,
                    "-bundlepool", destination,
                    //to support shared installation in carbon
                    "-shared", destination + File.separator + "p2",
                    //target is set to a separate directory per Profile
                    "-destination", destination + File.separator + profile,
                    "-profile", profile,
                    "-roaming");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed when configuring P2ApplicationLauncher", e);
        }
    }

    public void generateRepo(int forkedProcessTimeoutInSeconds) throws MojoExecutionException {
        int result = launcher.execute(forkedProcessTimeoutInSeconds);
        if (result != 0) {
            throw new MojoExecutionException("P2 publisher return code was " + result);
        }
    }

}
