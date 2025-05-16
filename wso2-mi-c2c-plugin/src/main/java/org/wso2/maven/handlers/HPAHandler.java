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

import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricSpec;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricSpecBuilder;
import org.tomlj.TomlParseResult;
import org.wso2.maven.CloudMojo;
import org.wso2.maven.KubernetesConstants;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.PodAutoscalerModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates kubernetes Horizontal Pod Autoscaler from annotations.
 */
public class HPAHandler extends AbstractArtifactHandler {

    public HPAHandler(CloudMojo cloudMojo) {
        super(cloudMojo);
    }

    private void generate(PodAutoscalerModel podAutoscalerModel) throws KubernetesPluginException {
        List<MetricSpec> metrics = new ArrayList<>();
        metrics.add(new MetricSpecBuilder()
                .withType("Resource")
                .withNewResource()
                .withName("cpu")
                .withNewTarget()
                .withType("Utilization")
                .withAverageUtilization(podAutoscalerModel.getCpuPercentage())
                .endTarget()
                .endResource()
                .build());
        if (podAutoscalerModel.getMemoryPercentage() != 0) {
            metrics.add(new MetricSpecBuilder()
                    .withType("Resource")
                    .withNewResource()
                    .withName("memory")
                    .withNewTarget()
                    .withType("Utilization")
                    .withAverageUtilization(podAutoscalerModel.getMemoryPercentage())
                    .endTarget()
                    .endResource()
                    .build());
        }
        HorizontalPodAutoscaler horizontalPodAutoscaler = new HorizontalPodAutoscalerBuilder()
                .withNewMetadata()
                .withName(podAutoscalerModel.getName())
                .withNamespace(dataHolder.getNamespace())
                .withLabels(podAutoscalerModel.getLabels())
                .endMetadata()
                .withNewSpec()
                .withMaxReplicas(podAutoscalerModel.getMaxReplicas())
                .withMinReplicas(podAutoscalerModel.getMinReplicas())
                .withMetrics(metrics)
                .withNewScaleTargetRef("apps/v1", "Deployment", podAutoscalerModel.getDeployment())
                .endSpec()
                .build();
        try {
            String hpaContent = KubernetesUtils.asYaml(horizontalPodAutoscaler);
            String outputFileName = KubernetesConstants.HPA_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = horizontalPodAutoscaler.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(hpaContent, outputFileName);
        } catch (IOException e) {
            throw new KubernetesPluginException("Error while generating HPA artifact", e);
        }
    }

    private void resolveToml(PodAutoscalerModel hpa) {
        TomlParseResult ballerinaCloud = dataHolder.getTomlParseResult();
        if (ballerinaCloud != null) {
            final String autoscaling = "cloud.deployment.autoscaling.";
            hpa.setMaxReplicas(Math.toIntExact(ballerinaCloud.getLong(autoscaling + "max_replicas",
                    hpa::getMaxReplicas)));
            hpa.setMinReplicas(Math.toIntExact(ballerinaCloud.getLong(autoscaling + "min_replicas",
                    hpa::getMinReplicas)));
            hpa.setCpuPercentage(Math.toIntExact(ballerinaCloud.getLong(autoscaling + "cpu",
                    hpa::getCpuPercentage)));
            hpa.setMemoryPercentage(Math.toIntExact(ballerinaCloud.getLong(autoscaling + "memory", () -> 0)));
        }
    }

    @Override
    public void createArtifacts() throws KubernetesPluginException {
        DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
        PodAutoscalerModel podAutoscalerModel = deploymentModel.getPodAutoscalerModel();
        if (!isHPAEnabled(podAutoscalerModel)) {
            return;
        }
        String integrationAppName = dataHolder.getOutputName();
        podAutoscalerModel.addLabel(KubernetesConstants.KUBERNETES_SELECTOR_KEY, integrationAppName);
        podAutoscalerModel.setDeployment(deploymentModel.getName());
        if (podAutoscalerModel.getMaxReplicas() == 0) {
            podAutoscalerModel.setMaxReplicas(deploymentModel.getReplicas() + 1);
        }
        if (podAutoscalerModel.getMinReplicas() == 0) {
            podAutoscalerModel.setMinReplicas(deploymentModel.getReplicas());
        }
        if (podAutoscalerModel.getName() == null || podAutoscalerModel.getName().isEmpty()) {
            podAutoscalerModel.setName(KubernetesUtils.getValidName(integrationAppName + KubernetesConstants.HPA_POSTFIX));
        }
        resolveToml(podAutoscalerModel);
        generate(podAutoscalerModel);
        cloudMojo.logInfo("\t@kubernetes:HPA");
    }

    private boolean isHPAEnabled(PodAutoscalerModel podAutoscalerModel) {
        if (podAutoscalerModel == null) {
            return false;
        }
        TomlParseResult ballerinaCloud = dataHolder.getTomlParseResult();
        if (ballerinaCloud == null) {
            return true; //since the default is hpa enabled
        }
        return ballerinaCloud.getBoolean("cloud.deployment.autoscaling.enable", () -> true);
    }
}
