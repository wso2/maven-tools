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

    /**
     * Sets the P2ApplicationLauncher's arguments to generate P2 repository. For this scenario both metadata repository
     * and artifact repository are same.
     *
     * @param sourceDir              the location of the update site
     * @param metadataRepoLocation   the URI to the metadata repository where the installable units should be published
     * @param metadataRepositoryName metadata repository name
     * @param repositoryName         name of the artifact repository where the artifacts should be published
     * @throws MojoExecutionException
     */
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

    /**
     * Sets the P2ApplicationLauncher's arguments and configure it to categorizing a set of Installable Units in a given
     * repository.
     *
     * @param metadataRepositoryLocation a comma separated list of metadata repository URLs where the software to be
     *                                   installed can be found.
     * @param categoryDefinitionFile     The category file which drives the categorization of installable units in the
     *                                   repository
     * @throws MojoExecutionException
     */
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

    /**
     * Sets the P2ApplicationLauncher's arguments to install features.
     *
     * @param metadataRepositoryLocation a comma separated list of metadata repository URLs where the software to be
     *                                   installed can be found.
     * @param artifactRepositoryLocation a comma separated list of artifact repository URLs where the software artifacts
     *                                   can be found.
     * @param installUIs                 a comma separated list of IUs to install. Each entry in the list is in the form
     *                                   <id> [ '/' <version> ]. If you are looking to install a feature, the identifier
     *                                   of the feature has to be suffixed with ".feature.group".
     * @param destination                the path of a folder in which the targeted product is located.
     * @param profile                    the profile id containing the description of the targeted product. This ID is
     *                                   defined by the eclipse.p2.profile property contained in the config.ini of the
     *                                   targeted product.
     * @throws MojoExecutionException
     */
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