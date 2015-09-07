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

package org.wso2.maven.p2.repo;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.FeatureArtifact;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.commons.P2ApplicationLaunchManager;
import org.wso2.maven.p2.repo.utils.RepoBeanGeneratorUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.wso2.maven.p2.utils.P2Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RepoGenerator takes parameters from the pom.xml and generates the repository.
 */
public class RepoGenerator extends Generator {

    private final RepositoryResourceBundle resourceBundle;
    private final MavenProject project;

    private ArrayList<FeatureArtifact> processedFeatureArtifacts;
    private ArrayList<Bundle> processedBundleArtifacts;

    private File tempDir;
    private File sourceDir;

    private File repoGenerationLocation;
    private File archiveFile;
    private File categoryDefinitionFile;

    /**
     * The features and bundles publisher application (org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher) is
     * a command line application that is capable of generating metadata (p2 repositories) from pre-built Eclipse
     * bundles and features.
     */
    private static final String PUBLISHER_APPLICATION = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";

    /**
     * The category publisher application (org.eclipse.equinox.p2.publisher.CategoryPublisher) is a command line
     * application that is capable of categorizing a set of Installable Units in a given repository.
     */
    private static final String CATEGORY_PUBLISHER_APPLICATION = "org.eclipse.equinox.p2.publisher.CategoryPublisher";

    private P2ApplicationLaunchManager p2LaunchManager;

    public RepoGenerator(RepositoryResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.project = this.resourceBundle.getProject();
        p2LaunchManager = new P2ApplicationLaunchManager(resourceBundle.getLauncher());
    }

    @Override
    public void generate() throws MojoExecutionException, MojoFailureException {
        generateBeansFromInputs();
        setupTempOutputFolderStructure();
        unzipFeaturesToOutputFolder();
        copyBundleArtifactsToOutputFolder();
        copyProjectResourcesToOutputFolder();
        generateRepository();
        updateRepositoryWithCategories();
        archiveGeneratedRepo();
        performMopUp();
    }

    private void generateBeansFromInputs() throws MojoExecutionException {
        getLog().info("Generating beans from input configuration");
        RepoBeanGeneratorUtils beanGenerator = new RepoBeanGeneratorUtils(this.resourceBundle);
        processedFeatureArtifacts = beanGenerator.getProcessedFeatureArtifacts();
        processedBundleArtifacts = beanGenerator.getProcessedBundleArtifacts();
    }

    /**
     * Copy maven project resources located in the resources folder into mata repository.
     *
     * @throws MojoExecutionException
     */
    private void copyProjectResourcesToOutputFolder() throws MojoExecutionException {
        List resources = project.getResources();
        if (resources != null) {
            getLog().info("Copying resources");
            for (Object obj : resources) {
                if (obj instanceof Resource) {
                    Resource resource = (Resource) obj;
                    try {
                        File resourceFolder = new File(resource.getDirectory());
                        if (resourceFolder.exists()) {
                            getLog().info("   " + resource.getDirectory());
                            FileManagementUtil.copyDirectory(resourceFolder, repoGenerationLocation);
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
                    }
                }
            }
        }
    }

    /**
     * Generate the repository by calling P2ApplicationLauncher.
     *
     * @throws MojoExecutionException
     */
    private void generateRepository() throws MojoExecutionException {
        getLog().info("Running Equinox P2 Publisher Application for Repository Generation");
        p2LaunchManager.setWorkingDirectory(project.getBasedir());
        p2LaunchManager.setApplicationName(PUBLISHER_APPLICATION);
        p2LaunchManager.addRepoGenerationArguments(sourceDir.getAbsolutePath(), resourceBundle.getMetadataRepository().
                toString(), getRepositoryName(), getRepositoryName());
        p2LaunchManager.generateRepo(resourceBundle.getForkedProcessTimeoutInSeconds());
        getLog().info("Completed running Equinox P2 Publisher Application for Repository Generation");
    }

    /**
     * Unzip the given feature zip files into the output folder which will ultimately converted into P2 repo.
     *
     * @throws MojoExecutionException
     */
    private void unzipFeaturesToOutputFolder() throws MojoExecutionException {
        ArrayList<FeatureArtifact> processedFeatureArtifacts = this.processedFeatureArtifacts;
        for (FeatureArtifact featureArtifact : processedFeatureArtifacts) {
            try {
                getLog().info("Extracting feature " + featureArtifact.getGroupId() + ":" + featureArtifact.getArtifactId());
                FileManagementUtil.unzip(featureArtifact.getArtifact().getFile(), sourceDir);
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: " + featureArtifact.toString(), e);
            }
        }
    }

    /**
     * Copy artfacts into the repository folder.
     *
     * @throws MojoExecutionException
     */
    private void copyBundleArtifactsToOutputFolder() throws MojoExecutionException {
        ArrayList<Bundle> processedBundleArtifacts = this.processedBundleArtifacts;
        if (processedBundleArtifacts.size() > 0) {
            getLog().info("Copying bundle artifacts.");
        }
        File pluginsDir = new File(sourceDir, "plugins");
        for (Bundle bundleArtifact : processedBundleArtifacts) {
            try {
                getLog().info("Copying bundle artifact:" + bundleArtifact.getBundleSymbolicName());
                File file = bundleArtifact.getArtifact().getFile();
                FileManagementUtil.copy(file, new File(pluginsDir, file.getName()));
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: " + bundleArtifact.toString(), e);
            }
        }
    }

    /**
     * Creates a zip archive from the generated repository and delete the repo.
     *
     * @throws MojoExecutionException
     */
    private void archiveGeneratedRepo() throws MojoExecutionException {
        if (resourceBundle.isArchive()) {
            getLog().info("Generating repository archive...");
            FileManagementUtil.zipFolder(repoGenerationLocation.toString(), archiveFile.toString());
            getLog().info("Repository Archive: " + archiveFile.toString());
            FileManagementUtil.deleteDirectories(repoGenerationLocation);
        }
    }

    private void setupTempOutputFolderStructure() throws MojoExecutionException {
        try {
            File targetDir = new File(project.getBasedir(), "target");
            String timestampVal = String.valueOf((new Date()).getTime());
            tempDir = new File(targetDir, "tmp." + timestampVal);
            sourceDir = new File(tempDir, "featureExtract");
            if (!sourceDir.mkdirs()) {
                throw new MojoExecutionException("Error occurred while creating output folder structure");
            }

            //Noted a weird assignment of values to metadataRepository and artifactRepository in the previous code.
            //Kept it as it is.
            if (resourceBundle.getArtifactRepository() != null) {
                resourceBundle.setMetadataRepository(resourceBundle.getArtifactRepository());
            }
            if (resourceBundle.getMetadataRepository() != null) {
                resourceBundle.setArtifactRepository(resourceBundle.getMetadataRepository());
            }
            if (resourceBundle.getMetadataRepository() == null) {
                File repo = new File(targetDir, project.getArtifactId() + "_" + project.getVersion());
                resourceBundle.setMetadataRepository(repo.toURI().toURL());
                resourceBundle.setArtifactRepository(repo.toURI().toURL());
            }

            repoGenerationLocation = new File(resourceBundle.getMetadataRepository().getFile().replace("/", File.separator));
            archiveFile = new File(targetDir, project.getArtifactId() + "_" + project.getVersion() + ".zip");
            categoryDefinitionFile = File.createTempFile("equinox-p2", "category");
        } catch (IOException e) {
            throw new MojoExecutionException("Error occurred while creating output folder structure", e);
        }
    }

    /**
     * Update the generated repository with categories.
     *
     * @throws MojoExecutionException
     */
    private void updateRepositoryWithCategories() throws MojoExecutionException {
        if (isCategoriesAvailable()) {
            getLog().info("Running Equinox P2 Category Publisher Application for the Generated Repository");
            P2Utils.createCategoryFile(project, resourceBundle.getCategories(), categoryDefinitionFile);

            p2LaunchManager.setWorkingDirectory(project.getBasedir());
            p2LaunchManager.setApplicationName(CATEGORY_PUBLISHER_APPLICATION);
            p2LaunchManager.addUpdateRepoWithCategoryArguments(resourceBundle.getMetadataRepository().toString(),
                    categoryDefinitionFile.toURI().toString());

            p2LaunchManager.generateRepo(resourceBundle.getForkedProcessTimeoutInSeconds());
            getLog().info("Completed running Equinox P2 Category Publisher Application for the Generated Repository");
        }
    }

    private boolean isCategoriesAvailable() {
        return !(resourceBundle.getCategories() == null || resourceBundle.getCategories().size() == 0);
    }

    /**
     * Delete the temporary folder.
     */
    private void performMopUp() {
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (Exception e) {
            getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
        }
    }

    public String getRepositoryName() {
        if (resourceBundle.getName() == null) {
            return project.getArtifactId();
        } else {
            return resourceBundle.getName();
        }
    }

}
