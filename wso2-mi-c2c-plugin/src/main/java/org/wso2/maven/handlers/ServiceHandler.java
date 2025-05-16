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

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.wso2.maven.CloudMojo;
import org.wso2.maven.KubernetesConstants;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.ServiceModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates kubernetes service from annotations.
 */
public class ServiceHandler extends AbstractArtifactHandler {

    public ServiceHandler(CloudMojo cloudMojo) {
        super(cloudMojo);
    }

    private void generate(List<ServiceModel> serviceModels) throws KubernetesPluginException {
        if (serviceModels.isEmpty()) {
            return;
        }
        int count = 0;
        ServiceModel commonService = new ServiceModel();
        final DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
        commonService.addLabel(KubernetesConstants.KUBERNETES_SELECTOR_KEY, dataHolder.getOutputName());
        commonService.setSelector(dataHolder.getOutputName());
        if (deploymentModel.getInternalDomainName() != null) {
            commonService.setName(deploymentModel.getInternalDomainName());
        } else {
            commonService.setName(KubernetesUtils.getValidName(deploymentModel.getName()
                    .replace(KubernetesConstants.DEPLOYMENT_POSTFIX, "") + KubernetesConstants.SVC_POSTFIX));
        }
        List<ServicePort> servicePorts = new ArrayList<>();
        for (ServiceModel serviceModel : serviceModels) {
            count++;
            if (null == serviceModel.getPortName()) {
                serviceModel.setPortName(KubernetesUtils.getValidName("port-" + count + "-" + commonService.getName()));
            }
            ServicePortBuilder servicePortBuilder = new ServicePortBuilder()
                    .withName(serviceModel.getPortName())
                    .withProtocol(KubernetesConstants.KUBERNETES_SVC_PROTOCOL)
                    .withPort(serviceModel.getPort())
                    .withNewTargetPort(serviceModel.getTargetPort());
            servicePorts.add(servicePortBuilder.build());
            // check whether the container port already exists in the deployment model
            boolean containerPortExists = false;
            for (ContainerPort containerPort : deploymentModel.getPorts()) {
                if (Objects.equals(containerPort.getName(), serviceModel.getPortName())) {
                    containerPortExists = true;
                    break;
                }
            }
            if (!containerPortExists) {
                ContainerPort containerPort = new ContainerPortBuilder()
                        .withName(serviceModel.getPortName())
                        .withContainerPort(serviceModel.getTargetPort())
                        .withProtocol(KubernetesConstants.KUBERNETES_SVC_PROTOCOL)
                        .build();
                deploymentModel.addPort(containerPort);
            }
        }

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(commonService.getName())
                .withNamespace(dataHolder.getNamespace())
                .addToLabels(commonService.getLabels())
                .endMetadata()
                .withNewSpec()
                .withPorts(servicePorts)
                .addToSelector(KubernetesConstants.KUBERNETES_SELECTOR_KEY, commonService.getSelector())
                .withSessionAffinity(commonService.getSessionAffinity())
                .withType(commonService.getServiceType())
                .endSpec()
                .build();
        try {
            String serviceYAML = KubernetesUtils.asYaml(service);
            String outputFileName = KubernetesConstants.SVC_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = service.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(serviceYAML, outputFileName);
        } catch (IOException e) {
            throw new KubernetesPluginException("Error while generating yaml file for service.", e);
        }

    }

    @Override
    public void createArtifacts() throws KubernetesPluginException {
        generate(dataHolder.getServiceModelList());
        cloudMojo.logInfo("\t@kubernetes:Service");
    }

}
