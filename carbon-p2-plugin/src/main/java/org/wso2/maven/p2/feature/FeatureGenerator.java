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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
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

public class FeatureGenerator extends Generator {

    private ArrayList bundles;
    private ArrayList importBundles;
    private ArrayList importFeatures;
    private ArrayList includedFeatures;

    private MavenProject project;
    private MavenProjectHelper projectHelper;

    private ArrayList<Bundle> processedBundles;
    private ArrayList<Bundle> processedImportBundles;
    private ArrayList<ImportFeature> processedImportFeatures;
    private ArrayList<Property> processedAdviceProperties;
    private ArrayList<IncludedFeature> processedIncludedFeatures;

    private File rowOutputFolder;
    private File featureIdFolder;
    private File pluginsFolder;
    private File featureXmlFile;
    private File p2InfFile;
    private File featurePropertyFile;
    private File featureManifestFile;
    private File featureZipFile;

    private FeatureResourceBundle resourceBundle;

    /**
     * Constructor for the FeatureGenerator.
     * Takes FeatureResourceBundle as a param to set private fields.
     * @param resourceBundle FeatureResourceBundle
     */
    public FeatureGenerator(FeatureResourceBundle resourceBundle, Log logger) {
        super(logger);
        this.resourceBundle = resourceBundle;
        this.bundles = resourceBundle.getBundles();
        this.importBundles = resourceBundle.getImportBundles();
        this.importFeatures = resourceBundle.getImportFeatures();
        this.includedFeatures = resourceBundle.getIncludedFeatures();
        this.project = resourceBundle.getProject();
        this.projectHelper = resourceBundle.getProjectHelper();
    }

    /**
     * This method will generate the Feature. This overrides the parent generate method of Generator abstract class.
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
        processedBundles = paramProcessor.getProcessedBundlesList(bundles, false);
        getLog().info("Processing import bundles");
        processedImportBundles = paramProcessor.getProcessedBundlesList(importBundles, true);
        getLog().info("Processing import features");
        processedImportFeatures = paramProcessor.getProcessedImportFeaturesList(importFeatures);
        getLog().info("Processing include features");
        processedIncludedFeatures = paramProcessor.getIncludedFeatures(includedFeatures);
        getLog().info("Processing advice properties");
        processedAdviceProperties = paramProcessor.getProcessedAdviceProperties();

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
        featureIdFolder = new File(featuresParentDir, resourceBundle.getId() + "_" + BundleUtils.getOSGIVersion(resourceBundle.getVersion()));
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

    private void copyAllDependencies() throws MojoExecutionException {
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

        //Copying includedFeatures
        if (processedIncludedFeatures != null) {
            for (IncludedFeature includedFeature : processedIncludedFeatures) {
                try {
                    getLog().info("Extracting feature " + includedFeature.getGroupId() + ":" +
                            includedFeature.getArtifactId());
                    FileManagementUtil.unzip(includedFeature.getArtifact().getFile(), rowOutputFolder);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error occured when extracting the Feature Artifact: " +
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

    private void copyResources() throws MojoExecutionException {
        //The following code was taken from the maven bundle plugin and updated suit the purpose
        List<Resource> resources = project.getResources();
        for (Resource resource : resources) {
            String sourcePath = resource.getDirectory();
            if (new File(sourcePath).exists()) {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(resource.getDirectory());
                if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
                    scanner.setIncludes(resource.getIncludes().toArray(new String[]{}));
                } else {
                    scanner.setIncludes(new String[]{"**/**"});
                }

                List<String> excludes = resource.getExcludes();
                if (excludes != null && !excludes.isEmpty()) {
                    scanner.setExcludes(excludes.toArray(new String[]{}));
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

        //        List resources = project.getResources();
        //        if (resources != null) {
        //            getLog().info("Copying resources");
        //            for (Object obj : resources) {
        //                if (obj instanceof Resource) {
        //                    Resource resource = (Resource) obj;
        //                    try {
        //                        File resourceFolder = new File(resource.getDirectory());
        //                        if (resourceFolder.exists()) {
        //                            getLog().info("   " + resource.getDirectory());
        //                            FileManagementUtil.copyDirectory(resourceFolder, featureIdFolder);
        //                        }
        //                    } catch (IOException e) {
        //                        throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
        //                    }
        //                }
        //            }
        //        }
    }

    private void performMopUp() {
        try {
            FileUtils.deleteDirectory(rowOutputFolder);
        } catch (Exception e) {
            getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
        }
    }
}
