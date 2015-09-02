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
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;
import org.wso2.maven.p2.BundleArtifact;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.repo.utils.RepoBeanGeneratorUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.wso2.maven.p2.utils.P2Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class RepoGenerator extends Generator {

    private final RepositoryResourceBundle resourceBundle;
    private final MavenProject project;

    private ArrayList processedFeatureArtifacts;
    private ArrayList processedBundleArtifacts;

    private ArrayList processedP2LauncherFiles;
    private File targetDir;
    private File tempDir;
    private File sourceDir;
    private File p2AgentDir;


    private File REPO_GEN_LOCATION;

    private File categoryDeinitionFile;

    private File ARCHIVE_FILE;

    private static final String PUBLISHER_APPLICATION = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";

    public RepoGenerator(RepositoryResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.project = this.resourceBundle.getProject();
    }

    @Override
    public void generate() throws MojoExecutionException, MojoFailureException {
        generateBeansFromInputs();
        setupTempOutputFolderStructure();
        unzipFeaturesToOutputFolder();

        createRepo();
        performMopUp();
    }

    private void generateBeansFromInputs() throws MojoExecutionException {
        RepoBeanGeneratorUtils beanGenerator = new RepoBeanGeneratorUtils(this.resourceBundle);
        processedFeatureArtifacts = beanGenerator.getProcessedFeatureArtifacts();
        processedBundleArtifacts = beanGenerator.getProcessedBundleArtifacts();
    }

    public void createRepo() throws MojoExecutionException, MojoFailureException {
        try {
            copyBundleArtifacts();
            copyProjectResources();
            this.getLog().info("Running Equinox P2 Publisher Application for Repository Generation");
            generateRepository();
            this.getLog().info("Running Equinox P2 Category Publisher Application for the Generated Repository");
            updateRepositoryWithCategories();
            archiveRepo();
        } catch (Exception e) {
            this.getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Copy maven project resources located in the resources folder into mata repository.
     *
     * @throws MojoExecutionException
     */
    private void copyProjectResources() throws MojoExecutionException {
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
                            FileManagementUtil.copyDirectory(resourceFolder, REPO_GEN_LOCATION);
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
                    }
                }
            }
        }
    }


    private void generateRepository() throws Exception {

        P2ApplicationLauncher launcher = resourceBundle.getLauncher();

        launcher.setWorkingDirectory(project.getBasedir());
        launcher.setApplicationName(PUBLISHER_APPLICATION);

        addArguments(launcher);


        int result = launcher.execute(resourceBundle.getForkedProcessTimeoutInSeconds());
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    private void addArguments(P2ApplicationLauncher launcher) throws IOException {
        launcher.addArguments("-source", sourceDir.getAbsolutePath(), //
                "-metadataRepository", resourceBundle.getMetadataRepository().toString(), //
                "-metadataRepositoryName", getRepositoryName(), //
                "-artifactRepository", resourceBundle.getMetadataRepository().toString(), //
                "-artifactRepositoryName", getRepositoryName(), //
                "-publishArtifacts",
                "-publishArtifactRepository",
                "-compress",
                "-append");
    }

    private void unzipFeaturesToOutputFolder() throws MojoExecutionException {
        ArrayList processedFeatureArtifacts = this.processedFeatureArtifacts;//getProcessedFeatureArtifacts();
        if (processedFeatureArtifacts == null) return;
        for (Iterator iterator = processedFeatureArtifacts.iterator(); iterator
                .hasNext(); ) {
            FeatureArtifact featureArtifact = (FeatureArtifact) iterator.next();
            try {
                getLog().info("Extracting feature " + featureArtifact.getGroupId() + ":" + featureArtifact.getArtifactId());
                FileManagementUtil.unzip(featureArtifact.getArtifact().getFile(), sourceDir);
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: " + featureArtifact.toString(), e);
            }
        }
    }

    private void copyBundleArtifacts() throws MojoExecutionException {
        ArrayList processedBundleArtifacts = this.processedBundleArtifacts;
        if (processedBundleArtifacts == null) {
            return;
        }
        File pluginsDir = new File(sourceDir, "plugins");
        for (Iterator iterator = processedBundleArtifacts.iterator(); iterator
                .hasNext(); ) {
            BundleArtifact bundleArtifact = (BundleArtifact) iterator.next();
            try {
                File file = bundleArtifact.getArtifact().getFile();
                FileManagementUtil.copy(file, new File(pluginsDir, file.getName()));
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: " + bundleArtifact.toString(), e);
            }
        }
    }

    private void archiveRepo() throws MojoExecutionException {
        if (resourceBundle.isArchive()) {
            getLog().info("Generating repository archive...");
            FileManagementUtil.zipFolder(REPO_GEN_LOCATION.toString(), ARCHIVE_FILE.toString());
            getLog().info("Repository Archive: " + ARCHIVE_FILE.toString());
            FileManagementUtil.deleteDirectories(REPO_GEN_LOCATION);
        }
    }


    private void setupTempOutputFolderStructure() throws MojoExecutionException {
        try {
            targetDir = new File(project.getBasedir(), "target");
            String timestampVal = String.valueOf((new Date()).getTime());
            tempDir = new File(targetDir, "tmp." + timestampVal);
            sourceDir = new File(tempDir, "featureExtract");
            sourceDir.mkdirs();

            //Some weird shit is going on. Check this part : 01/09/2015
            if (resourceBundle.getArtifactRepository() != null) {
                resourceBundle.setMetadataRepository(resourceBundle.getArtifactRepository());
            }
            if (resourceBundle.getMetadataRepository() != null) {
                resourceBundle.setArtifactRepository(resourceBundle.getMetadataRepository());
            }
            if (resourceBundle.getMetadataRepository() == null) {
                File repo = new File(targetDir, project.getArtifactId() + "_" + project.getVersion());
                resourceBundle.setMetadataRepository(repo.toURL());
                resourceBundle.setArtifactRepository(repo.toURL());
            }

            REPO_GEN_LOCATION = new File(resourceBundle.getMetadataRepository().getFile().replace("/", File.separator));
            ARCHIVE_FILE = new File(targetDir, project.getArtifactId() + "_" + project.getVersion() + ".zip");
            categoryDeinitionFile = File.createTempFile("equinox-p2", "category");
        } catch (IOException e) {
            throw new MojoExecutionException("Error occurred while creating output folder structure", e);
        }
    }

    private void updateRepositoryWithCategories() throws Exception {
        if (isCategoriesAvailable()) {
            P2Utils.createCategoryFile(project, resourceBundle.getCategories(), categoryDeinitionFile,
                    resourceBundle.getArtifactFactory(), resourceBundle.getRemoteRepositories(),
                    resourceBundle.getLocalRepository(), resourceBundle.getResolver());
            P2ApplicationLauncher launcher = resourceBundle.getLauncher();
            launcher.setWorkingDirectory(project.getBasedir());
            launcher.setApplicationName("org.eclipse.equinox.p2.publisher.CategoryPublisher");
            launcher.addArguments("-metadataRepository", resourceBundle.getMetadataRepository().toString(),
                    "-categoryDefinition", categoryDeinitionFile.toURI().toString(),
                    "-categoryQualifier",
                    "-compress",
                    "-append");

            int result = launcher.execute(resourceBundle.getForkedProcessTimeoutInSeconds());
            if (result != 0) {
                throw new MojoFailureException("P2 publisher return code was " + result);
            }
        }
    }

    private boolean isCategoriesAvailable() {
        return !(resourceBundle.getCategories() == null || resourceBundle.getCategories().size() == 0);
    }

    private void performMopUp() {
        try {
            // we want this temp file, in order to debug some errors. since this is in target, it will
            // get removed in the next build cycle.
            // FileUtils.deleteDirectory(tempDir);
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
