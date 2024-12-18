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

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeploymentModel extends KubernetesModel {
    private Map<String, String> podAnnotations;
    private int replicas;
    private Probe livenessProbe;
    private Probe readinessProbe;
    private String namespace;
    private String image;
    private boolean buildImage;
    private String dockerHost;
    private String dockerCertPath;
    private List<ContainerPort> ports;
    private PodAutoscalerModel podAutoscalerModel;
    private Set<SecretModel> secretModels;
    private Set<ConfigMapModel> configMapModels;
    private Set<PersistentVolumeClaimModel> volumeClaimModels;
    private Set<String> imagePullSecrets;
    private String commandArgs;
    private String registry;
    private DeploymentStrategy strategy;
    private Map<String, String> nodeSelector;
    private String dockerConfigPath;
    private ResourceRequirements resourceRequirements;
    private String internalDomainName;

    public DeploymentModel() {
        // Initialize with default values.
        this.replicas = 1;
        this.envVars = new ArrayList<>();
        this.buildImage = true;
        this.labels = new LinkedHashMap<>();
        this.nodeSelector = new LinkedHashMap<>();
        this.ports = new ArrayList<>();
        this.secretModels = new HashSet<>();
        this.configMapModels = new HashSet<>();
        this.volumeClaimModels = new HashSet<>();
        this.imagePullSecrets = new HashSet<>();
        this.commandArgs = "";
        this.registry = "";
        Map<String, Quantity> limit = new HashMap<>();
        limit.put("cpu", new Quantity("1000m"));
        limit.put("memory", new Quantity("1Gi"));
        Map<String, Quantity> resource = new HashMap<>();
        resource.put("cpu", new Quantity("200m"));
        resource.put("memory", new Quantity("100Mi"));
        this.resourceRequirements = new ResourceRequirementsBuilder()
                .withLimits(limit)
                .withRequests(resource)
                .build();
    }

    public Map<String, String> getPodAnnotations() {
        return podAnnotations;
    }

    public void setPodAnnotations(Map<String, String> podAnnotations) {
        this.podAnnotations = podAnnotations;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public Probe getLivenessProbe() {
        return livenessProbe;
    }

    public void setLivenessProbe(Probe livenessProbe) {
        this.livenessProbe = livenessProbe;
    }

    public Probe getReadinessProbe() {
        return readinessProbe;
    }

    public void setReadinessProbe(Probe readinessProbe) {
        this.readinessProbe = readinessProbe;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isBuildImage() {
        return buildImage;
    }

    public void setBuildImage(boolean buildImage) {
        this.buildImage = buildImage;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public void setDockerCertPath(String dockerCertPath) {
        this.dockerCertPath = dockerCertPath;
    }

    public List<ContainerPort> getPorts() {
        return ports;
    }

    public void setPorts(List<ContainerPort> ports) {
        this.ports = ports;
    }

    public PodAutoscalerModel getPodAutoscalerModel() {
        return podAutoscalerModel;
    }

    public void setPodAutoscalerModel(PodAutoscalerModel podAutoscalerModel) {
        this.podAutoscalerModel = podAutoscalerModel;
    }

    public Set<SecretModel> getSecretModels() {
        return secretModels;
    }

    public void setSecretModels(Set<SecretModel> secretModels) {
        this.secretModels = secretModels;
    }

    public Set<ConfigMapModel> getConfigMapModels() {
        return configMapModels;
    }

    public void setConfigMapModels(Set<ConfigMapModel> configMapModels) {
        this.configMapModels = configMapModels;
    }

    public Set<PersistentVolumeClaimModel> getVolumeClaimModels() {
        return volumeClaimModels;
    }

    public void setVolumeClaimModels(Set<PersistentVolumeClaimModel> volumeClaimModels) {
        this.volumeClaimModels = volumeClaimModels;
    }

    public Set<String> getImagePullSecrets() {
        return imagePullSecrets;
    }

    public void setImagePullSecrets(Set<String> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    public String getCommandArgs() {
        return commandArgs;
    }

    public void setCommandArgs(String commandArgs) {
        this.commandArgs = commandArgs;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public DeploymentStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(DeploymentStrategy strategy) {
        this.strategy = strategy;
    }

    public Map<String, String> getNodeSelector() {
        return nodeSelector;
    }

    public void setNodeSelector(Map<String, String> nodeSelector) {
        this.nodeSelector = nodeSelector;
    }

    public String getDockerConfigPath() {
        return dockerConfigPath;
    }

    public void setDockerConfigPath(String dockerConfigPath) {
        this.dockerConfigPath = dockerConfigPath;
    }

    public ResourceRequirements getResourceRequirements() {
        return resourceRequirements;
    }

    public void setResourceRequirements(ResourceRequirements resourceRequirements) {
        this.resourceRequirements = resourceRequirements;
    }

    public String getInternalDomainName() {
        return internalDomainName;
    }

    public void setInternalDomainName(String internalDomainName) {
        this.internalDomainName = internalDomainName;
    }

    public void addSecretModel(SecretModel secretModel) {
        this.secretModels.add(secretModel);
    }

    public void addPort(ContainerPort containerPort) {
        this.ports.add(containerPort);
    }
}
