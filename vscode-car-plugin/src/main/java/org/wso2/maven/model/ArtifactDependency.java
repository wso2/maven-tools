/*
* Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.maven.model;

/**
 * Class that represents the artifact dependencies included in artifacts.xml in .car.
 */
public class ArtifactDependency {
    private String artifact;
    private String version;
    private String serverRole;
    private Boolean include;

    public ArtifactDependency(String artifact, String version, String serverRole, Boolean include) {
        this.artifact = artifact;
        this.version = version;
        this.serverRole = serverRole;
        this.include = include;
    }

    /**
     * @return the artifact
     */
    public String getArtifact() {
        return artifact;
    }

    /**
     * @param artifact of the dependency
     */
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    /**
     * @return the verison
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version of the dependency
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the serverRole
     */
    public String getServerRole() {
        return serverRole;
    }

    /**
     * @param serverRole of the dependency
     */
    public void setServerRole(String serverRole) {
        this.serverRole = serverRole;
    }

    /**
     * @return include
     */
    public Boolean getInclude() {
        return include;
    }

    /**
     * @param include of the dependency
     */
    public void setInclude(Boolean include) {
        this.include = include;
    }
}
