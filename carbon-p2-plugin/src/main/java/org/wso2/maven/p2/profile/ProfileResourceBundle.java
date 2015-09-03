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

package org.wso2.maven.p2.profile;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;

import java.net.URL;
import java.util.ArrayList;

public class ProfileResourceBundle {

    private String destination;
    private String profile;
    private URL metadataRepository;
    private URL artifactRepository;
    private ArrayList features;
    private boolean deleteOldProfileFiles;
    private MavenProject project;
    private MavenProjectHelper projectHelper;
    private P2ApplicationLauncher launcher;
    private int forkedProcessTimeoutInSeconds;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
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

    public ArrayList getFeatures() {
        return features;
    }

    public void setFeatures(ArrayList features) {
        this.features = features;
    }

    public boolean isDeleteOldProfileFiles() {
        return deleteOldProfileFiles;
    }

    public void setDeleteOldProfileFiles(boolean deleteOldProfileFiles) {
        this.deleteOldProfileFiles = deleteOldProfileFiles;
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
}
