/*
 * Copyright 2009-2010 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.maven.p2.beans;

import org.apache.maven.artifact.Artifact;
import org.wso2.maven.p2.utils.BundleUtils;

/**
 * Bean class representing an IncludedFeature.
 */
public class IncludedFeature {

    private String groupId;
    private String artifactId;
    private String artifactVersion;
    private boolean optional = false;
    private String featureID;
    private String featureVersion;
    private Artifact artifact;

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean isOptional) {
        this.optional = isOptional;
    }

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

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public String getFeatureID() {
        return featureID;
    }

    public void setFeatureId(String featureId) {
        this.featureID = featureId;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }

    public void setFeatureVersion(String version) {
        if (artifactVersion == null || artifactVersion.equals("")) {
            artifactVersion = version;
            featureVersion = BundleUtils.getOSGIVersion(version);
        }
    }
}
