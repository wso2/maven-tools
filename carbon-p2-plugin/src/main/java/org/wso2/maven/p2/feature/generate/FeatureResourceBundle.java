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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
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
    private URL propertiesFile;
    private Properties properties;
    private List<Bundle> bundles;
    private List<Feature> importFeatures;
    private List<Feature> includedFeatures;
    private List<Advice> adviceFileContent;

    private RepositorySystem repositorySystem;
    private MavenProject project;
    private ArtifactRepository localRepository;
    private MavenProjectHelper projectHelper;
    private List<ArtifactRepository> remoteRepositories;

    private Log log;

    public String getId() {
        if(id.endsWith(".feature")) {
            return id.substring(0, id.indexOf(".feature"));
        }
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

    public URL getPropertyFile() {
        return propertiesFile;
    }

    public void setPropertyFile(URL propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }

    public List<Bundle> getBundles() {
        if(this.bundles == null) {
            return new ArrayList<>();
        }
        return this.bundles;
    }
    public List<Feature> getImportFeatures() {
        return importFeatures;
    }

    public void setImportFeatures(List<Feature> importFeatures) {
        this.importFeatures = importFeatures;
    }

    public List<Feature> getIncludeFeatures() {
        if(includedFeatures == null) {
            return new ArrayList<>();
        }
        return includedFeatures;
    }

    public void setIncludeFeatures(List<Feature> includedFeatures) {
        this.includedFeatures = includedFeatures;
    }

    public List<Advice> getAdviceFileContent() {
        return adviceFileContent;
    }

    public void setAdviceFileContent(List<Advice> adviceFileContent) {
        this.adviceFileContent = adviceFileContent;
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

    public File getPropertyFileInResourceDir() {
        File propertyFile = new File(getProject().getBasedir() + "/src/main/resources/feature.properties");
        return  propertyFile;
    }
}
