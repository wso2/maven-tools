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

package org.wso2.maven.p2.beans;

import org.apache.maven.artifact.Artifact;

public class CarbonArtifact {
    private String groupId;
    private String artifactId;
    private String version;
    private String symbolicName;
    private String type;
    private String bundleVersion;
    private Artifact artifact;
    private String compatibility = "equivalent";

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    /**
     * Though this is not used from the code, do not delete this as this is used by maven context to inject match.
     *
     * @param match String
     */
    public void setMatch(String match) {
        this.compatibility = match;
    }

    public <T extends CarbonArtifact> void copyTo(T item) {
        item.setArtifact(this.artifact);
        item.setVersion(this.version);
        item.setArtifactId(this.artifactId);
        item.setBundleVersion(this.bundleVersion);
        item.setGroupId(this.groupId);
        item.setSymbolicName(this.symbolicName);
        item.setType(this.type);
    }
}
