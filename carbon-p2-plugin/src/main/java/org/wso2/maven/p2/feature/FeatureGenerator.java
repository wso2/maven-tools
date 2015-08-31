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

package org.wso2.maven.p2.feature;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.feature.utils.InputParamProcessor;
import org.wso2.maven.p2.feature.utils.OutputFileGeneratorUtils;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FeatureGenerator takes parameters from the pom.xml and generates the feature.
 */
public class FeatureGenerator extends Generator {

    private final FeatureResourceBundle resourceBundle;
    private final MavenProject project;
    private final MavenProjectHelper projectHelper;

    private ArrayList<Bundle> processedBundles;
    private ArrayList<Bundle> processedImportBundles;
    private ArrayList<IncludedFeature> processedIncludedFeatures;

    private File rowOutputFolder;
    private File featureIdFolder;
    private File pluginsFolder;
    private File featureXmlFile;
    private File p2InfFile;
    private File featurePropertyFile;
    private File featureManifestFile;
    private File featureZipFile;


    /**
     * Constructor for the FeatureGenerator.
     * Takes FeatureResourceBundle as a param to set private fields.
     *
     * @param resourceBundle FeatureResourceBundle
     */
    public FeatureGenerator(FeatureResourceBundle resourceBundle, Log logger) {
        super(logger);
        this.resourceBundle = resourceBundle;
        this.project = resourceBundle.getProject();
        this.projectHelper = resourceBundle.getProjectHelper();
    }

    /**
     * Generates the Feature. This overrides the parent generate method of Generator abstract class.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void generate() throws MojoExecutionException, MojoFailureException {
        processInputs();
        createAndSetupPaths();
        copyResources();
        OutputFileGeneratorUtils.createFeatureXml(resourceBundle, featureXmlFile);
        OutputFileGeneratorUtils.createPropertiesFile(resourceBundle, featurePropertyFile);
        OutputFileGeneratorUtils.createManifestMFFile(resourceBundle, featureManifestFile);
        OutputFileGeneratorUtils.createP2Inf(resourceBundle, p2InfFile);
        copyAllDependencies();
        createArchive();
        deployArtifact();
        performMopUp();
    }

    private void processInputs() throws MojoExecutionException {
        InputParamProcessor paramProcessor = new InputParamProcessor(this.resourceBundle);
        getLog().info("Processing bundles");
        processedBundles = paramProcessor.getProcessedBundlesList();
        getLog().info("Processing import bundles");
        processedImportBundles = paramProcessor.getProcessedImportBundlesList();
        getLog().info("Processing import features");
        ArrayList<ImportFeature> processedImportFeatures = paramProcessor.getProcessedImportFeaturesList();
        getLog().info("Processing include features");
        processedIncludedFeatures = paramProcessor.getIncludedFeatures();
        getLog().info("Processing advice properties");
        ArrayList<Property> processedAdviceProperties = paramProcessor.getProcessedAdviceProperties();

        resourceBundle.setProcessedBundles(processedBundles);
        resourceBundle.setProcessedImportBundles(processedImportBundles);
        resourceBundle.setProcessedImportFeatures(processedImportFeatures);
        resourceBundle.setProcessedIncludedFeatures(processedIncludedFeatures);
        resourceBundle.setProcessedAdviceProperties(processedAdviceProperties);
    }

    private void createAndSetupPaths() {
        getLog().info("Setting up folder structure");
        File destFolder = new File(project.getBasedir(), "target");
        rowOutputFolder = new File(destFolder, "raw");
        File featuresParentDir = new File(rowOutputFolder, "features");
        featureIdFolder = new File(featuresParentDir, resourceBundle.getId() + "_" +
                BundleUtils.getOSGIVersion(resourceBundle.getVersion()));
        pluginsFolder = new File(rowOutputFolder, "plugins");
        File featureMetaInfFolder = new File(featureIdFolder, "META-INF");
        featureXmlFile = new File(featureIdFolder, "feature.xml");
        featurePropertyFile = new File(featureIdFolder, "feature.properties");
        p2InfFile = new File(featureIdFolder, "p2.inf");
        featureManifestFile = new File(featureMetaInfFolder, "MANIFEST.MF");
        featureZipFile = new File(destFolder, project.getArtifactId() + "-" + project.getVersion() + ".zip");
        featureMetaInfFolder.mkdirs();
        pluginsFolder.mkdirs();
    }

    /**
     * Copy all the dependencies into output folder.
     *
     * @throws MojoExecutionException
     */
    private void copyAllDependencies() throws MojoExecutionException {
        copyBundles();
        copyImportBundles();
        copyIncludedFeatures();
    }

    /**
     * Copy bundles into plugins folder.
     *
     * @throws MojoExecutionException
     */
    private void copyBundles() throws MojoExecutionException {
        ArrayList<Bundle> processedBundlesList = processedBundles;
        if (processedBundlesList != null) {
            getLog().info("Copying bundle dependencies");
            for (Bundle bundle : processedBundlesList) {
                try {
                    getLog().info("   " + bundle.toOSGIString());
                    String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
                    FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsFolder, bundleName));
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable copy dependency: " + bundle.getArtifactId(), e);
                }
            }
        }
    }

    /**
     * Copy import bundles into plugins folder.
     *
     * @throws MojoExecutionException
     */
    private void copyImportBundles() throws MojoExecutionException {
        ArrayList<Bundle> processedImportBundlesList = processedImportBundles;
        if (processedImportBundlesList != null) {
            getLog().info("Copying import bundle dependencies");
            for (Bundle bundle : processedImportBundlesList) {
                try {
                    if (!bundle.isExclude()) {
                        getLog().info("   " + bundle.toOSGIString());
                        String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
                        FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsFolder, bundleName));
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable copy import dependency: " + bundle.getArtifactId(), e);
                }
            }
        }
    }

    /**
     * Copying includedFeatures into the output folder.
     *
     * @throws MojoExecutionException
     */
    private void copyIncludedFeatures() throws MojoExecutionException {
        if (processedIncludedFeatures != null) {
            for (IncludedFeature includedFeature : processedIncludedFeatures) {
                try {
                    getLog().info("Extracting feature " + includedFeature.getGroupId() + ":" +
                            includedFeature.getArtifactId());
                    FileManagementUtil.unzip(includedFeature.getArtifact().getFile(), rowOutputFolder);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: " +
                            includedFeature.getGroupId() + ":" + includedFeature.getArtifactId(), e);
                }
            }
        }
    }

    private void createArchive() throws MojoExecutionException {
        getLog().info("Generating feature archive: " + featureZipFile.getAbsolutePath());
        FileManagementUtil.zipFolder(rowOutputFolder.getAbsolutePath(), featureZipFile.getAbsolutePath());
    }

    private void deployArtifact() {
        if (featureZipFile != null && featureZipFile.exists()) {
            project.getArtifact().setFile(featureZipFile);
            projectHelper.attachArtifact(project, "zip", null, featureZipFile);
        }
    }

    /**
     * Copy maven project resources into the output feature folder.
     * @throws MojoExecutionException
     */
    private void copyResources() throws MojoExecutionException {
        //The following code was taken from the maven bundle plugin and updated suit the purpose
        List<Resource> resources = project.getResources();
        for (Resource resource : resources) {
            String sourcePath = resource.getDirectory();
            if (new File(sourcePath).exists()) {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(resource.getDirectory());
                if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
                    scanner.setIncludes(resource.getIncludes().toArray(new String[resource.getIncludes().size()]));
                } else {
                    scanner.setIncludes(new String[]{"**/**"});
                }

                List<String> excludes = resource.getExcludes();
                if (excludes != null && !excludes.isEmpty()) {
                    scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
                }

                scanner.addDefaultExcludes();
                scanner.scan();

                List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());
                getLog().info("   " + resource.getDirectory());
                for (String name : includedFiles) {
                    File fromPath = new File(sourcePath, name);
                    File toPath = new File(featureIdFolder, name);

                    try {
                        if (fromPath.isDirectory() && !toPath.exists()) {
                            toPath.mkdirs();
                        } else {
                            FileManagementUtil.copy(fromPath, toPath);
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
                    }
                }
            }
        }
    }

    /**
     * Deletes the temp output folder.
     */
    private void performMopUp() {
        try {
            FileUtils.deleteDirectory(rowOutputFolder);
        } catch (Exception e) {
            getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
        }
    }
}
