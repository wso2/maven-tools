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

package org.wso2.maven.handlers;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.wso2.maven.CloudMojo;
import org.wso2.maven.KubernetesConstants;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.ConfigMapModel;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.PersistentVolumeClaimModel;
import org.wso2.maven.models.SecretModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.wso2.maven.KubernetesConstants.DEPLOYMENT_FILE_POSTFIX;
import static org.wso2.maven.KubernetesConstants.REGISTRY_SEPARATOR;

/**
 * Generates kubernetes deployment from annotations.
 */
public class DeploymentHandler extends AbstractArtifactHandler {

    public DeploymentHandler(CloudMojo cloudMojo) {
        super(cloudMojo);
    }

    private List<VolumeMount> populateVolumeMounts(DeploymentModel deploymentModel) {
        List<VolumeMount> volumeMounts = new ArrayList<>();
        volumeMounts.addAll(KubernetesUtils.generateSecretVolumeMounts(deploymentModel.getSecretModels()));
        volumeMounts.addAll(generateConfigMapVolumeMounts(deploymentModel.getConfigMapModels()));
        for (PersistentVolumeClaimModel volumeClaimModel : deploymentModel.getVolumeClaimModels()) {
            VolumeMount volumeMount = new VolumeMountBuilder()
                    .withMountPath(volumeClaimModel.getMountPath())
                    .withName(volumeClaimModel.getName() + "-volume")
                    .withReadOnly(volumeClaimModel.isReadOnly())
                    .build();
            volumeMounts.add(volumeMount);
        }
        return volumeMounts;
    }

    private Collection<? extends VolumeMount> generateConfigMapVolumeMounts(Set<ConfigMapModel> configMapModels) {
        List<VolumeMount> volumeMounts = new ArrayList<>();
        for (ConfigMapModel configMapModel : configMapModels) {
            final String mountPath = configMapModel.getMountPath();
            VolumeMountBuilder volumeMountBuilder = new VolumeMountBuilder()
                    .withMountPath(mountPath)
                    .withName(configMapModel.getName() + "-volume")
                    .withReadOnly(configMapModel.isReadOnly());

            if ((!configMapModel.isDir()) && (!configMapModel.isMIConf())) {
                volumeMountBuilder.withSubPath(KubernetesUtils.getFileNameOfConfigMap(configMapModel));
            }
            volumeMounts.add(volumeMountBuilder.build());
        }
        return volumeMounts;
    }

    private Container generateContainer(DeploymentModel deploymentModel, List<ContainerPort> containerPorts) {
        String dockerRegistry = deploymentModel.getRegistry();
        String deploymentImageName = deploymentModel.getImage();
        if (null != dockerRegistry && !"".equals(dockerRegistry)) {
            deploymentImageName = dockerRegistry + REGISTRY_SEPARATOR + deploymentImageName;
        }
        Lifecycle preStop = new LifecycleBuilder()
                .withNewPreStop()
                .withNewExec()
                .withCommand("sleep", "15")
                .endExec()
                .endPreStop()
                .build();

        return new ContainerBuilder()
                .withName(deploymentModel.getName())
                .withImage(deploymentImageName)
                .withPorts(containerPorts)
                .withEnv(deploymentModel.getEnvVars())
                .withVolumeMounts(populateVolumeMounts(deploymentModel))
                .withLivenessProbe(deploymentModel.getLivenessProbe())
                .withReadinessProbe(deploymentModel.getReadinessProbe())
                .withResources(deploymentModel.getResourceRequirements())
                .withLifecycle(preStop)
                .build();
    }

    private List<Volume> populateVolume(DeploymentModel deploymentModel) {
        List<Volume> volumes = new ArrayList<>();
        for (SecretModel secretModel : deploymentModel.getSecretModels()) {
            Volume volume = new VolumeBuilder()
                    .withName(secretModel.getName() + "-volume")
                    .withNewSecret()
                    .withSecretName(secretModel.getName())
                    .endSecret()
                    .build();

            if (secretModel.getDefaultMode() > 0) {
                volume.getSecret().setDefaultMode(secretModel.getDefaultMode());
            }
            volumes.add(volume);
        }
        for (ConfigMapModel configMapModel : deploymentModel.getConfigMapModels()) {
            Volume volume = new VolumeBuilder()
                    .withName(configMapModel.getName() + "-volume")
                    .withNewConfigMap()
                    .withName(configMapModel.getName())
                    .endConfigMap()
                    .build();

            if (configMapModel.getDefaultMode() > 0) {
                volume.getConfigMap().setDefaultMode(configMapModel.getDefaultMode());
            }
            volumes.add(volume);
        }
        for (PersistentVolumeClaimModel volumeClaimModel : deploymentModel.getVolumeClaimModels()) {
            Volume volume = new VolumeBuilder()
                    .withName(volumeClaimModel.getName() + "-volume")
                    .withNewPersistentVolumeClaim()
                    .withClaimName(volumeClaimModel.getName())
                    .endPersistentVolumeClaim()
                    .build();
            volumes.add(volume);
        }
        return volumes;
    }

    private List<LocalObjectReference> getImagePullSecrets(DeploymentModel deploymentModel) {
        List<LocalObjectReference> imagePullSecrets = new ArrayList<>();
        for (String imagePullSecret : deploymentModel.getImagePullSecrets()) {
            imagePullSecrets.add(new LocalObjectReferenceBuilder().withName(imagePullSecret).build());
        }
        return imagePullSecrets;
    }

    /**
     * Generate kubernetes deployment definition from annotation.
     *
     * @param deploymentModel @{@link DeploymentModel} definition
     * @throws KubernetesPluginException If an error occurs while generating artifact.
     */
    private void generate(DeploymentModel deploymentModel) throws KubernetesPluginException {
        List<ContainerPort> containerPorts = null;
        if (deploymentModel.getPorts() != null) {
            containerPorts = deploymentModel.getPorts();
        }
        Container container = generateContainer(deploymentModel, containerPorts);
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(deploymentModel.getName())
                .withLabels(deploymentModel.getLabels())
                .withAnnotations(deploymentModel.getAnnotations())
                .withNamespace(dataHolder.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels(deploymentModel.getLabels())
                .endSelector()
                .withStrategy(deploymentModel.getStrategy())
                .withReplicas(deploymentModel.getReplicas())
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(deploymentModel.getLabels())
                .addToAnnotations(deploymentModel.getPodAnnotations())
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withImagePullSecrets(getImagePullSecrets(deploymentModel))
                .withVolumes(populateVolume(deploymentModel))
                .withNodeSelector(deploymentModel.getNodeSelector())
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        try {
            String deploymentContent = KubernetesUtils.asYaml(deployment);
            String outputFileName = DEPLOYMENT_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = deployment.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(deploymentContent, outputFileName);
        } catch (IOException e) {
            throw new KubernetesPluginException("Error while generating yaml file for deployment.", e);
        }
    }

    public void createArtifacts() throws KubernetesPluginException {
        DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
        deploymentModel.setPodAutoscalerModel(dataHolder.getPodAutoscalerModel());
        deploymentModel.setSecretModels(dataHolder.getSecretModelSet());
        deploymentModel.setConfigMapModels(dataHolder.getConfigMapModelSet());
        if (null != deploymentModel.getLivenessProbe() &&
                deploymentModel.getLivenessProbe().getHttpGet().getPort().getIntVal() == 0) {
            //set first port as liveness port
            if (deploymentModel.getPorts().isEmpty()) {
                throw new KubernetesPluginException("No ports defined for the deployment");
            }
            deploymentModel.getLivenessProbe().getHttpGet().setPort(new
                    IntOrString(deploymentModel.getPorts().iterator().next().getContainerPort()));
        }

        if (null != deploymentModel.getReadinessProbe() &&
                deploymentModel.getReadinessProbe().getHttpGet().getPort().getIntVal() == 0) {
            //set first port as readiness port
            if (deploymentModel.getPorts().isEmpty()) {
                throw new KubernetesPluginException("No ports defined for the deployment");
            }
            deploymentModel.getReadinessProbe().getHttpGet().setPort(new
                    IntOrString(deploymentModel.getPorts().iterator().next().getContainerPort()));
        }
        KubernetesUtils.resolveDockerToml();
        generate(deploymentModel);
        cloudMojo.logInfo("\t@kubernetes:Deployment");
    }
}

