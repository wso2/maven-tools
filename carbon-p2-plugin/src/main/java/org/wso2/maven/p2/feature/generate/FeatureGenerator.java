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

package org.wso2.maven.p2.feature.generate;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.maven.p2.beans.CarbonArtifact;
import org.wso2.maven.p2.exceptions.CarbonArtifactNotFoundException;
import org.wso2.maven.p2.exceptions.MissingRequiredPropertyException;
import org.wso2.maven.p2.exceptions.OSGIInformationExtractionException;
import org.wso2.maven.p2.feature.generate.utils.FeatureFileGeneratorUtils;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.DependencyResolver;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * FeatureGenerator takes parameters from the pom.xml and generates the feature.
 */
public class FeatureGenerator {

    private final FeatureResourceBundle resourceBundle;
    private final MavenProject project;
    private final MavenProjectHelper projectHelper;

    private File rowOutputFolder;
    private File featureIdFolder;
    private File pluginsFolder;
    private File featureXmlFile;
    private File p2InfFile;
    private File featurePropertyFile;
    private File featureManifestFile;
    private File featureZipFile;

    private HashMap<String, CarbonArtifact> dependentBundles;
    private HashMap<String, CarbonArtifact> dependentFeatures;

    private Log log;


    /**
     * Constructor for the FeatureGenerator.
     * Takes FeatureResourceBundle as a param to set private fields.
     *
     * @param resourceBundle FeatureResourceBundle
     */
    public FeatureGenerator(FeatureResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.log = resourceBundle.getLog();
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
        try {
            resolveMavenProjectDependencies();
            populateRequiredArtifactData();
            setupTempOutputFolderStructure();
            copyFeatureResources();
            generateFeatureOutputFiles();
            copyAllIncludedArtifacts();
            createFeatureArchive();
            deployArtifact();
            performMopUp();
        } catch (IOException | TransformerException | ParserConfigurationException | SAXException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (OSGIInformationExtractionException | CarbonArtifactNotFoundException |
                MissingRequiredPropertyException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Traverse through dependency and dependencyManagement section and populate the project dependencies into
     * internal bean structures
     *
     * @throws IOException
     * @throws OSGIInformationExtractionException
     */
    private void resolveMavenProjectDependencies() throws IOException, OSGIInformationExtractionException {
        this.log.info("Inspecting maven dependencies.");
        List<HashMap<String, CarbonArtifact>> artifacts = DependencyResolver.getDependenciesForProject(project,
                resourceBundle.getRepositorySystem(),
                resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository());
        dependentBundles = artifacts.get(0);
        dependentFeatures = artifacts.get(1);
    }

    /**
     * Cross check the given project dependencies with the bundles and features specified in the maven plugin
     * configuration and fill specified bundles and features with extra details taken from project dependencies.
     *
     * @throws CarbonArtifactNotFoundException
     */
    private void populateRequiredArtifactData() throws CarbonArtifactNotFoundException {
        populateBundleDataFromCache();
        populateFeatureDataFromCache();
    }

    /**
     * The generated bundle beans using the configuration fed into the plugin contain only fewer details. Thus traverse
     * through the maven dependencies specified in the pom and update the bundle beans with extra details gathered from
     * dependencies.
     *
     * @throws CarbonArtifactNotFoundException
     */
    private void populateBundleDataFromCache() throws CarbonArtifactNotFoundException {
        for (Bundle bundle : resourceBundle.getBundles()) {
            String key = bundle.getSymbolicName() + "_" + bundle.getVersion();
            CarbonArtifact artifact = dependentBundles.get(key);
            if (artifact == null) {
                throw new CarbonArtifactNotFoundException("Bundle " + key + " is not found in project dependency list");
            }
            artifact.copyTo(bundle);
        }
    }

    /**
     * The generated feature beans using the configuration fed into the plugin contain only fewer details. Thus traverse
     * through the maven dependencies specified in the pom and update the feature beans with extra details gathered from
     * dependencies.
     *
     * @throws CarbonArtifactNotFoundException
     */
    private void populateFeatureDataFromCache() throws CarbonArtifactNotFoundException {
        for (Feature feature : resourceBundle.getIncludeFeatures()) {
            String key = feature.getId() + ".feature" + "_" + feature.getVersion();
            CarbonArtifact artifact = dependentFeatures.get(key);
            if (artifact == null) {
                throw new
                        CarbonArtifactNotFoundException("Feature " + key + " is not found in project dependency list");
            }
            artifact.copyTo(feature);
        }
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
            TransformerException, MissingRequiredPropertyException {
        FeatureFileGeneratorUtils.createFeatureXml(resourceBundle, featureXmlFile);
        FeatureFileGeneratorUtils.createPropertiesFile(resourceBundle, featurePropertyFile);
        FeatureFileGeneratorUtils.createManifestMFFile(resourceBundle, featureManifestFile);
        FeatureFileGeneratorUtils.createP2Inf(resourceBundle, p2InfFile);
    }

    /**
     * Set up the temporary output folder structure.
     */
    private void setupTempOutputFolderStructure() throws IOException {
        this.log.info("Setting up folder structure");
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
        copyIncludedFeatures();
    }

    /**
     * Copy bundles into plugins folder.
     *
     * @throws IOException
     */
    private void copyBundles() throws IOException {
        List<Bundle> bundles = resourceBundle.getBundles();
        if (bundles.size() > 0) {
            this.log.info("Copying bundle dependencies");
            for (Bundle bundle : bundles) {
                try {
                    this.log.info("   " + bundle.toOSGIString());
                    String bundleName = bundle.getSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
                    FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsFolder, bundleName));
                } catch (IOException e) {
                    throw new IOException("Unable copy dependency: " + bundle.getArtifactId(), e);
                }
            }
        }
    }

    /**
     * Copying includedFeatures into the output folder.
     *
     * @throws IOException
     */
    private void copyIncludedFeatures() throws IOException {
        List<Feature> features = resourceBundle.getIncludeFeatures();
        if (features.size() > 0) {
            for (Feature includedFeature : features) {
                try {
                    this.log.info("Extracting feature " + includedFeature.getGroupId() + ":" +
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
     */
    private void createFeatureArchive() {
        this.log.info("Generating feature archive: " + featureZipFile.getAbsolutePath());
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
                this.log.info("   " + resource.getDirectory());
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
            this.log.warn(new IOException("Unable complete mop up operation", e));
        }
    }
}
