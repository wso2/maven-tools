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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Bean class containing all the parameters entered to the mojo through plugin configuration. The purpose of this class
 * is to make any configuration property accessible from any class by simply passing this bean as a parameter.
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
    private List<String> bundles;
    private List<String> importFeatures;
    private List<String> includedFeatures;
    private AdviceFile adviceFile;

    private List<Bundle> processedBundles;
    private List<ImportFeature> processedImportFeatures;
    private List<Property> processedAdviceProperties;
    private List<IncludedFeature> processedIncludedFeatures;

    private RepositorySystem repositorySystem;
    private MavenProject project;
    private ArtifactRepository localRepository;
    private MavenProjectHelper projectHelper;
    private List<ArtifactRepository> remoteRepositories;

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

    public List<String> getBundles() {
        return bundles;
    }

    public void setBundles(List<String> bundles) {
        this.bundles = bundles;
    }

    public List<String> getImportFeatures() {
        return importFeatures;
    }

    public void setImportFeatures(List<String> importFeatures) {
        this.importFeatures = importFeatures;
    }

    public List<String> getIncludedFeatures() {
        return includedFeatures;
    }

    public void setIncludedFeatures(List<String> includedFeatures) {
        this.includedFeatures = includedFeatures;
    }

    public AdviceFile getAdviceFile() {
        return adviceFile;
    }

    public void setAdviceFile(AdviceFile adviceFile) {
        this.adviceFile = adviceFile;
    }

    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public void setRepositorySystem(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(List<ArtifactRepository> remoteRepositories) {
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

    public List<Bundle> getProcessedBundles() {
        return processedBundles;
    }

    public void setProcessedBundles(List<Bundle> processedBundles) {
        this.processedBundles = processedBundles;
    }

    public List<ImportFeature> getProcessedImportFeatures() {
        return processedImportFeatures;
    }

    public void setProcessedImportFeatures(List<ImportFeature> processedImportFeatures) {
        this.processedImportFeatures = processedImportFeatures;
    }

    public List<Property> getProcessedAdviceProperties() {
        return processedAdviceProperties;
    }

    public void setProcessedAdviceProperties(List<Property> processedAdviceProperties) {
        this.processedAdviceProperties = processedAdviceProperties;
    }

    public List<IncludedFeature> getProcessedIncludedFeatures() {
        return processedIncludedFeatures;
    }

    public void setProcessedIncludedFeatures(List<IncludedFeature> processedIncludedFeatures) {
        this.processedIncludedFeatures = processedIncludedFeatures;
    }
}
