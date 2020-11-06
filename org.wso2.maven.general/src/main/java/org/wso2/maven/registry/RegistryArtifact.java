/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.registry;

import org.wso2.maven.registry.beans.RegistryElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegistryArtifact {

    private String name;
    private String version;
    private String serverRole;
    private String type;
    private String groupId;

//	This is the file path for the actual artifact.
//	<artifact name="testEndpoint3" version="1.0.0" type="synapse/endpoint" serverRole="EnterpriseServiceBus">
//    <file>src\main\synapse-config\endpoints\testEndpoint3.xml</file>
//    </artifact>

    private List<RegistryElement> items = new ArrayList<RegistryElement>();

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    String getServerRole() {
        return serverRole;
    }

    void setServerRole(String serverRole) {
        this.serverRole = serverRole;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    boolean isAnonymous() {
        return name != null ? false : true;
    }

    boolean addRegistryElement(RegistryElement item) {
        return items.add(item);
    }

    public boolean removeRegistryElement(RegistryElement item) {
        return items.remove(item);
    }

    List<RegistryElement> getAllRegistryItems() {
        return Collections.unmodifiableList(items);
    }

    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    String getGroupId() {
        return groupId;
    }

}
