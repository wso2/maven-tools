/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.maven.p2.beans;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class FeatureArtifact {

    private String groupId;
    private String artifactId;
    private String version;
    private Artifact artifact;
    private String featureId;
    private String featureVersion;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void resolveVersion(MavenProject project) throws MojoExecutionException {
        if (version == null) {
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
                if (dependency.getGroupId().equalsIgnoreCase(getGroupId()) && dependency.getArtifactId().equalsIgnoreCase(getArtifactId())) {
                    setVersion(dependency.getVersion());
                }
            }
        }

        if (version == null) {
            List<Dependency> dependencies = project.getDependencyManagement().getDependencies();
            for (Dependency dependency : dependencies) {
                if (dependency.getGroupId().equalsIgnoreCase(getGroupId()) && dependency.getArtifactId().equalsIgnoreCase(getArtifactId())) {
                    setVersion(dependency.getVersion());
                }
            }
        }
        if (version == null) {
            throw new MojoExecutionException("Could not find the version for " + getGroupId() + ":" + getArtifactId());
        }
        Properties properties = project.getProperties();
        for (Object key : properties.keySet()) {
            version = version.replaceAll(Pattern.quote("${" + key + "}"), properties.get(key).toString());
        }
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureVersion(String featureVersion) {
        this.featureVersion = featureVersion;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }
}
