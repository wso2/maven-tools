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

import io.fabric8.kubernetes.api.model.EnvVar;

import java.util.List;
import java.util.Map;

public abstract class KubernetesModel {
    private String version;
    protected String name;
    protected Map<String, String> labels;
    protected Map<String, String> annotations;
    protected List<EnvVar> envVars;

    public void addLabel(String key, String value) {
        this.labels.put(key, value);
    }

    public void addEnv(EnvVar envVar) {
        envVars.add(envVar);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<EnvVar> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(List<EnvVar> envVars) {
        this.envVars = envVars;
    }
}
