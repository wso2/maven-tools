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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Bean class containing all the parameters entered to the mojo through plugin configuration.
 * The purpose of this class is to make any configuration property accessible from any class by simply passing this
 * bean as a parameter.
 */
public class RepositoryResourceBundle {

    private String name;
    private URL metadataRepository;
    private URL artifactRepository;

    private ArrayList featureArtifacts;

    private ArrayList bundleArtifacts;
    private ArrayList categories;
    /**
     * flag indicating whether the artifacts should be published to the repository. When this flag is not set,
     * the actual bytes underlying the artifact will not be copied, but the repository index will be created.
     * When this option is not specified, it is recommended to set the artifactRepository to be in the same location
     * as the source (-source)
     *
     */
    private boolean publishArtifacts;

    /**
     * Type of Artifact (War,Jar,etc)
     */
    private boolean publishArtifactRepository;

    private MavenProject project;


    private boolean archive;

    private RepositorySystem repositorySystem;
    private ArtifactRepository localRepository;
    private List<ArtifactRepository> remoteRepositories;
    private P2ApplicationLauncher launcher;
    private int forkedProcessTimeoutInSeconds;

    private Log log;

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public URL getMetadataRepository() {
        return metadataRepository;
    }

    public void setMetadataRepository(URL metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    public URL getArtifactRepository() {
        return artifactRepository;
    }

    public void setArtifactRepository(URL artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    public ArrayList getFeatureArtifacts() {
        return featureArtifacts;
    }

    public void setFeatureArtifacts(ArrayList featureArtifacts) {
        this.featureArtifacts = featureArtifacts;
    }

    public ArrayList getBundleArtifacts() {
        return bundleArtifacts;
    }

    public void setBundleArtifacts(ArrayList bundleArtifacts) {
        this.bundleArtifacts = bundleArtifacts;
    }

    public ArrayList getCategories() {
        return categories;
    }

    public void setCategories(ArrayList categories) {
        this.categories = categories;
    }

    public boolean isPublishArtifacts() {
        return publishArtifacts;
    }

    public void setPublishArtifacts(boolean publishArtifacts) {
        this.publishArtifacts = publishArtifacts;
    }

    public boolean isPublishArtifactRepository() {
        return publishArtifactRepository;
    }

    public void setPublishArtifactRepository(boolean publishArtifactRepository) {
        this.publishArtifactRepository = publishArtifactRepository;
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
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

    public P2ApplicationLauncher getLauncher() {
        return launcher;
    }

    public void setLauncher(P2ApplicationLauncher launcher) {
        this.launcher = launcher;
    }

    public int getForkedProcessTimeoutInSeconds() {
        return forkedProcessTimeoutInSeconds;
    }

    public void setForkedProcessTimeoutInSeconds(int forkedProcessTimeoutInSeconds) {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

}
