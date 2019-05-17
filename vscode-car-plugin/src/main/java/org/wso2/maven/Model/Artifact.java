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

package org.wso2.maven.Model;

/**
 * Class that represents the artifact in WSO2 ESB project.
 */
public class Artifact {

    private String name;
    private String version;
    private String serverRole;
    private String type;
    private String file;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name of the artifact
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version of the artifact
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
     * @param serverRole of the artifact
     */
    public void setServerRole(String serverRole) {
        this.serverRole = serverRole;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type of the artifact
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the file location
     */
    public String getFile() {
        return file;
    }

    /**
     * @param file location of the artifact
     */
    public void setFile(String file) {
        this.file = file;
    }
}
