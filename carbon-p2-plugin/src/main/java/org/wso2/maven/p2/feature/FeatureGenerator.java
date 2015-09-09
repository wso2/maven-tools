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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.exceptions.ArtifactVersionNotFoundException;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;
import org.wso2.maven.p2.exceptions.OSGIInformationExtractionException;
import org.wso2.maven.p2.feature.utils.FeatureBeanGeneratorUtils;
import org.wso2.maven.p2.feature.utils.FeatureFileGeneratorUtils;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
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
    //private ArrayList<Bundle> processedImportBundles;
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
    public FeatureGenerator(FeatureResourceBundle resourceBundle) {
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
    @Override
    public void generate() throws MojoExecutionException, MojoFailureException {
        //Generates bean classes from the xml configuration provided through the pom.xml in plugin configuration
        try {
            generateBeansFromInputs();
            setupTempOutputFolderStructure();
            copyFeatureResources();
            generateFeatureOutputFiles();
            copyAllIncludedArtifacts();
            createFeatureArchive();
            deployArtifact();
            performMopUp();
        } catch (IOException | TransformerException | ParserConfigurationException | SAXException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (InvalidBeanDefinitionException | ArtifactVersionNotFoundException |
                OSGIInformationExtractionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Generates internal bean objects from the plugin configuration passed in through the pom.xml.
     *
     * @throws InvalidBeanDefinitionException
     * @throws ArtifactVersionNotFoundException
     * @throws OSGIInformationExtractionException
     * @throws IOException
     */
    private void generateBeansFromInputs() throws InvalidBeanDefinitionException, ArtifactVersionNotFoundException,
            OSGIInformationExtractionException, IOException {
        FeatureBeanGeneratorUtils paramProcessor = new FeatureBeanGeneratorUtils(this.resourceBundle);
        getLog().info("Processing bundles");

        processedBundles = paramProcessor.getProcessedBundlesList();
//      Had a confusion whether import bundles are actually needed during the code review 01/09/2015. Thus commented this.
//        getLog().info("Processing import bundles");
//        processedImportBundles = paramProcessor.getProcessedImportBundlesList();
        getLog().info("Processing import features");
        ArrayList<ImportFeature> processedImportFeatures = paramProcessor.getProcessedImportFeaturesList();
        getLog().info("Processing include features");
        processedIncludedFeatures = paramProcessor.getIncludedFeatures();
        getLog().info("Processing advice properties");
        ArrayList<Property> processedAdviceProperties = paramProcessor.getProcessedAdviceProperties();

        resourceBundle.setProcessedBundles(processedBundles);
//      Had a confusion whether import bundles are actually needed during the code review 01/09/2015. Thus commented this.
//        resourceBundle.setProcessedImportBundles(processedImportBundles);
        resourceBundle.setProcessedImportFeatures(processedImportFeatures);
        resourceBundle.setProcessedIncludedFeatures(processedIncludedFeatures);
        resourceBundle.setProcessedAdviceProperties(processedAdviceProperties);
    }

    /**
     * Generates feature.xml, features.properties, manifest file for the feature and p2inf file.
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    private void generateFeatureOutputFiles() throws IOException, ParserConfigurationException, SAXException,
            TransformerException {
        FeatureFileGeneratorUtils.createFeatureXml(resourceBundle, featureXmlFile);
        FeatureFileGeneratorUtils.createPropertiesFile(resourceBundle, featurePropertyFile);
        FeatureFileGeneratorUtils.createManifestMFFile(resourceBundle, featureManifestFile);
        FeatureFileGeneratorUtils.createP2Inf(resourceBundle, p2InfFile);
    }

    /**
     * Set up the temporary output folder structure.
     */
    private void setupTempOutputFolderStructure() throws IOException {
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
        if (!featureMetaInfFolder.mkdirs()) {
            throw new IOException("Unable to create folder " + featureMetaInfFolder.getAbsolutePath());
        }
        if (!pluginsFolder.mkdirs()) {
            throw new IOException("Unable to create folder " + pluginsFolder.getAbsolutePath());
        }

    }

    /**
     * Copy all the dependencies into output folder.
     *
     * @throws IOException
     */
    private void copyAllIncludedArtifacts() throws IOException {
        copyBundles();
//      Had a confusion whether import bundles are actually needed during the code review 01/09/2015. Thus commented this.
//        copyImportBundles();
        copyIncludedFeatures();
    }

    /**
     * Copy bundles into plugins folder.
     *
     * @throws IOException
     */
    private void copyBundles() throws IOException {
        ArrayList<Bundle> processedBundlesList = processedBundles;
        if (processedBundlesList != null) {
            getLog().info("Copying bundle dependencies");
            for (Bundle bundle : processedBundlesList) {
                try {
                    getLog().info("   " + bundle.toOSGIString());
                    String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
                    FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsFolder, bundleName));
                } catch (IOException e) {
                    throw new IOException("Unable copy dependency: " + bundle.getArtifactId(), e);
                }
            }
        }
    }

//      Had a confusion whether import bundles are actually needed during the code review 01/09/2015. Thus commented this.
//    /**
//     * Copy import bundles into plugins folder.
//     *
//     * @throws MojoExecutionException
//     */
//    private void copyImportBundles() throws MojoExecutionException {
//        ArrayList<Bundle> processedImportBundlesList = processedImportBundles;
//        if (processedImportBundlesList != null) {
//            getLog().info("Copying import bundle dependencies");
//            for (Bundle bundle : processedImportBundlesList) {
//                try {
//                    if (!bundle.isExclude()) {
//                        getLog().info("   " + bundle.toOSGIString());
//                        String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
//                        FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsFolder, bundleName));
//                    }
//                } catch (IOException e) {
//                    throw new MojoExecutionException("Unable copy import dependency: " + bundle.getArtifactId(), e);
//                }
//            }
//        }
//    }

    /**
     * Copying includedFeatures into the output folder.
     *
     * @throws IOException
     */
    private void copyIncludedFeatures() throws IOException {
        if (processedIncludedFeatures != null) {
            for (IncludedFeature includedFeature : processedIncludedFeatures) {
                try {
                    getLog().info("Extracting feature " + includedFeature.getGroupId() + ":" +
                            includedFeature.getArtifactId());
                    FileManagementUtil.unzip(includedFeature.getArtifact().getFile(), rowOutputFolder);
                } catch (IOException e) {
                    throw new IOException("Error occurred when extracting the Feature Artifact: " +
                            includedFeature.getGroupId() + ":" + includedFeature.getArtifactId(), e);
                }
            }
        }
    }

    /**
     * Zip the created features folder.
     *
     */
    private void createFeatureArchive() {
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
     *
     * @throws IOException
     */
    private void copyFeatureResources() throws IOException {
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
                            if (!toPath.mkdirs()) {
                                throw new IOException("Unable create directory: " + toPath.getAbsolutePath());
                            }
                        } else {
                            FileManagementUtil.copy(fromPath, toPath);
                        }
                    } catch (IOException e) {
                        throw new IOException("Unable copy resources: " + resource.getDirectory(), e);
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
        } catch (IOException e) {
            getLog().warn(new IOException("Unable complete mop up operation", e));
        }
    }
}
