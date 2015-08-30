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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.wso2.maven.p2.beans.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Bean class containing all the parameters entered to the mojo through plugin configuration.
 * The purpose of this class is to make any configuration property accessible from any class by simply passing this
 * bean as a parameter.
 */
public class FeatureResourceBundle {

    private String id;
    private String version;
    private String label;
    private String description;
    private String providerName;
    private String copyright;
    private String licenceUrl;
    private String licence;
    private File manifest;
    private File propertiesFile;
    private Properties properties;
    private ArrayList bundles;
    private ArrayList importBundles;
    private ArrayList importFeatures;
    private ArrayList includedFeatures;
    private AdviceFile adviceFile;

    private ArrayList<Bundle> processedBundles;

    private ArrayList<Bundle> processedImportBundles;
    private ArrayList<ImportFeature> processedImportFeatures;
    private ArrayList<Property> processedAdviceProperties;
    private ArrayList<IncludedFeature> processedIncludedFeatures;
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;
    private java.util.List remoteRepositories;
    private MavenProject project;
    private MavenProjectHelper projectHelper;
    private Log log;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getLicenceUrl() {
        return licenceUrl;
    }

    public void setLicenceUrl(String licenceUrl) {
        this.licenceUrl = licenceUrl;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public File getManifest() {
        return manifest;
    }

    public void setManifest(File manifest) {
        this.manifest = manifest;
    }

    public File getPropertiesFile() {
        return propertiesFile;
    }

    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public ArrayList getBundles() {
        return bundles;
    }

    public void setBundles(ArrayList bundles) {
        this.bundles = bundles;
    }

    public ArrayList getImportBundles() {
        return importBundles;
    }

    public void setImportBundles(ArrayList importBundles) {
        this.importBundles = importBundles;
    }

    public ArrayList getImportFeatures() {
        return importFeatures;
    }

    public void setImportFeatures(ArrayList importFeatures) {
        this.importFeatures = importFeatures;
    }

    public ArrayList getIncludedFeatures() {
        return includedFeatures;
    }

    public void setIncludedFeatures(ArrayList includedFeatures) {
        this.includedFeatures = includedFeatures;
    }

    public AdviceFile getAdviceFile() {
        return adviceFile;
    }

    public void setAdviceFile(AdviceFile adviceFile) {
        this.adviceFile = adviceFile;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public void setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactResolver getResolver() {
        return resolver;
    }

    public void setResolver(ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public List getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(List remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    public void setProjectHelper(MavenProjectHelper projectHelper) {
        this.projectHelper = projectHelper;
    }

    public void setLog(Log logger) {
        this.log = logger;
    }

    public Log getLog() {
        return this.log;
    }

    public ArrayList<Bundle> getProcessedBundles() {
        return processedBundles;
    }

    public void setProcessedBundles(ArrayList<Bundle> processedBundles) {
        this.processedBundles = processedBundles;
    }

    public ArrayList<Bundle> getProcessedImportBundles() {
        return processedImportBundles;
    }

    public void setProcessedImportBundles(ArrayList<Bundle> processedImportBundles) {
        this.processedImportBundles = processedImportBundles;
    }

    public ArrayList<ImportFeature> getProcessedImportFeatures() {
        return processedImportFeatures;
    }

    public void setProcessedImportFeatures(ArrayList<ImportFeature> processedImportFeatures) {
        this.processedImportFeatures = processedImportFeatures;
    }

    public ArrayList<Property> getProcessedAdviceProperties() {
        return processedAdviceProperties;
    }

    public void setProcessedAdviceProperties(ArrayList<Property> processedAdviceProperties) {
        this.processedAdviceProperties = processedAdviceProperties;
    }

    public ArrayList<IncludedFeature> getProcessedIncludedFeatures() {
        return processedIncludedFeatures;
    }

    public void setProcessedIncludedFeatures(ArrayList<IncludedFeature> processedIncludedFeatures) {
        this.processedIncludedFeatures = processedIncludedFeatures;
    }
}
