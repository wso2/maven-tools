/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.metadata;

import java.util.ArrayList;
import java.util.List;

public class Artifact {
    private String name;
    private String version;
    private String type;
    private String mainSequence;
    private String serverRole;
    private String file;
    private String extractedPath;
    private List<Dependency> dependencies = new ArrayList<>();
    private List<Artifact> subArtifacts = new ArrayList<>();

    private int unresolvedDepCount = 0;

    public String getName() {
        return name;
    }

    public String getConnectorName() {
        if (name != null && name.endsWith("-connector")) {
            return name.substring(0, name.length() - 10);
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency dep) {
        this.dependencies.add(dep);
        unresolvedDepCount++;
    }

    public void removeDependency(Dependency dep) {
        this.dependencies.remove(dep);
        unresolvedDepCount--;
    }

    public void resolveDependency() {
        unresolvedDepCount--;
    }

    public int getUnresolvedDepCount() {
        return unresolvedDepCount;
    }

    public String getMainSequence() {
        return mainSequence;
    }

    public void setMainSequence(String mainSequence) {
        this.mainSequence = mainSequence;
    }

    public String getServerRole() {
        return serverRole;
    }

    public void setServerRole(String serverRole) {
        this.serverRole = serverRole;
    }

    public String getExtractedPath() {
        return extractedPath;
    }
    public void setExtractedPath(String extractedPath) {
        this.extractedPath = extractedPath;
    }
    public List<Artifact> getSubArtifacts() {
        return subArtifacts;
    }
    public void addSubArtifact(Artifact subArtifact) {
        this.subArtifacts.add(subArtifact);
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
