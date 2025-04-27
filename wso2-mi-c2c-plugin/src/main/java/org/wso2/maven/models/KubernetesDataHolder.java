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

import org.tomlj.TomlParseResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to store kubernetes models.
 */
public class KubernetesDataHolder {
    private DeploymentModel deploymentModel;
    private PodAutoscalerModel podAutoscalerModel;
    private List<ServiceModel> serviceModelList;
    private Set<SecretModel> secretModelSet;
    private Set<ConfigMapModel> configMapModelSet;
    private Path k8sArtifactOutputPath;
    private Path dockerArtifactOutputPath;
    private String namespace;
    private Path sourceRoot;
    private TomlParseResult tomlParseResult;
    private boolean singleYaml;
    private String outputName;
    private DockerModel dockerModel;

    private String projectArtifactId;
    private String projectVersion;

    KubernetesDataHolder() {
        this.serviceModelList = new ArrayList<>();
        this.secretModelSet = new HashSet<>();
        this.configMapModelSet = new HashSet<>();
        this.deploymentModel = new DeploymentModel();
        this.dockerModel = new DockerModel();
        this.tomlParseResult = null;
        this.singleYaml = true;
    }

    public void addSecrets(Set<SecretModel> secrets) {
        this.secretModelSet.addAll(secrets);
    }

    public void addConfigMaps(Set<ConfigMapModel> configMaps) {
        this.configMapModelSet.addAll(configMaps);
    }

    public void addServiceModel(ServiceModel serviceModel) {
        this.serviceModelList.add(serviceModel);
    }

    public DeploymentModel getDeploymentModel() {
        return deploymentModel;
    }

    public void setDeploymentModel(DeploymentModel deploymentModel) {
        this.deploymentModel = deploymentModel;
    }

    public PodAutoscalerModel getPodAutoscalerModel() {
        return podAutoscalerModel;
    }

    public void setPodAutoscalerModel(PodAutoscalerModel podAutoscalerModel) {
        this.podAutoscalerModel = podAutoscalerModel;
    }

    public List<ServiceModel> getServiceModelList() {
        return serviceModelList;
    }

    public void setServiceModelList(List<ServiceModel> serviceModelList) {
        this.serviceModelList = serviceModelList;
    }

    public Set<SecretModel> getSecretModelSet() {
        return secretModelSet;
    }

    public void setSecretModelSet(Set<SecretModel> secretModelSet) {
        this.secretModelSet = secretModelSet;
    }

    public Set<ConfigMapModel> getConfigMapModelSet() {
        return configMapModelSet;
    }

    public void setConfigMapModelSet(Set<ConfigMapModel> configMapModelSet) {
        this.configMapModelSet = configMapModelSet;
    }

    public Path getK8sArtifactOutputPath() {
        return k8sArtifactOutputPath;
    }

    public void setK8sArtifactOutputPath(Path k8sArtifactOutputPath) {
        this.k8sArtifactOutputPath = k8sArtifactOutputPath;
    }

    public Path getDockerArtifactOutputPath() {
        return dockerArtifactOutputPath;
    }

    public void setDockerArtifactOutputPath(Path dockerArtifactOutputPath) {
        this.dockerArtifactOutputPath = dockerArtifactOutputPath;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Path getSourceRoot() {
        return sourceRoot;
    }

    public void setSourceRoot(Path sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    public TomlParseResult getTomlParseResult() {
        return tomlParseResult;
    }

    public void setTomlParseResult(TomlParseResult tomlParseResult) {
        this.tomlParseResult = tomlParseResult;
    }

    public boolean isSingleYaml() {
        return singleYaml;
    }

    public void setSingleYaml(boolean singleYaml) {
        this.singleYaml = singleYaml;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public DockerModel getDockerModel() {
        return dockerModel;
    }

    public void setDockerModel(DockerModel dockerModel) {
        this.dockerModel = dockerModel;
    }

    public String getProjectArtifactId() {
        return projectArtifactId;
    }

    public void setProjectArtifactId(String projectArtifactId) {
        this.projectArtifactId = projectArtifactId;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }
}
