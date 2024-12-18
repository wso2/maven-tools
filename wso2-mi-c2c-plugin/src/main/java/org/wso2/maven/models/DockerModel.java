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

package org.wso2.maven.models;

import java.util.Set;

import static org.wso2.maven.Constants.WSO2_MI_DEFAULT_BASE_IMAGE;

/**
 * Docker annotations model class.
 */
public class DockerModel {

    private String name;
    private String registry;
    private String tag;
    private String baseImage;

    private Set<Integer> ports;

    public DockerModel() {
        // Initialize with default values except for image name
        this.tag = "latest";
        this.baseImage = WSO2_MI_DEFAULT_BASE_IMAGE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Set<Integer> getPorts() {
        return ports;
    }

    public void setPorts(Set<Integer> ports) {
        this.ports = ports;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    @Override
    public String toString() {
        return "DockerModel{" +
                "name='" + name + '\'' +
                ", registry='" + registry + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}
