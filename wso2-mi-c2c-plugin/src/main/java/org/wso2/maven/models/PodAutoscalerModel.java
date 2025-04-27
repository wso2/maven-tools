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

import java.util.HashMap;

public class PodAutoscalerModel extends KubernetesModel{
    private int minReplicas;
    private int maxReplicas;
    private int cpuPercentage;
    private int memoryPercentage;
    private String deployment;

    public PodAutoscalerModel() {
        this.cpuPercentage = 50;
        labels = new HashMap<>();
    }

    public void addLabel(String key, String value) {
        this.labels.put(key, value);
    }

    public int getMinReplicas() {
        return minReplicas;
    }

    public void setMinReplicas(int minReplicas) {
        this.minReplicas = minReplicas;
    }

    public int getMaxReplicas() {
        return maxReplicas;
    }

    public void setMaxReplicas(int maxReplicas) {
        this.maxReplicas = maxReplicas;
    }

    public int getCpuPercentage() {
        return cpuPercentage;
    }

    public void setCpuPercentage(int cpuPercentage) {
        this.cpuPercentage = cpuPercentage;
    }

    public int getMemoryPercentage() {
        return memoryPercentage;
    }

    public void setMemoryPercentage(int memoryPercentage) {
        this.memoryPercentage = memoryPercentage;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }
}
