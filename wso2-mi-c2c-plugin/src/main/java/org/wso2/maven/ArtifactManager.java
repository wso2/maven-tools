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

package org.wso2.maven;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.wso2.maven.exceptions.DockerGenException;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.handlers.ConfigMapHandler;
import org.wso2.maven.handlers.DeploymentHandler;
import org.wso2.maven.handlers.HPAHandler;
import org.wso2.maven.handlers.HandlerFactory;
import org.wso2.maven.handlers.SecretHandler;
import org.wso2.maven.handlers.ServiceHandler;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.DockerModel;
import org.wso2.maven.models.KubernetesContext;
import org.wso2.maven.models.KubernetesDataHolder;
import org.wso2.maven.models.ServiceModel;
import org.wso2.maven.utils.DockerImageName;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.maven.utils.KubernetesUtils.isBlank;

public class ArtifactManager {

    private static final Map<String, String> instructions = new LinkedHashMap<>();
    private final CloudMojo cloudMojo;
    private final TomlParseResult cloudConfig;
    private final KubernetesDataHolder kubernetesDataHolder = KubernetesContext.getInstance().getDataHolder();

    ArtifactManager(CloudMojo cloudMojo, TomlParseResult cloudConfig) {
        this.cloudMojo = cloudMojo;
        this.cloudConfig = cloudConfig;
    }

    /**
     * Generates artifacts according to the cloud parameter in build options.
     *
     * @param buildOptions Value of cloud field in build option.
     * @throws DockerGenException, KubernetesPluginException if an error occurs while generating artifacts
     */
    public void createArtifacts(String buildOptions) throws DockerGenException, KubernetesPluginException {
        createDockerArtifacts();
        if ("k8s".equals(buildOptions)) {
            createKubernetesArtifacts();
        }
    }


    private void createDockerArtifacts() throws DockerGenException {
        cloudMojo.logInfo("\nGenerating Docker artifacts\n");
        DockerModel dockerModel = getDockerModel();
        String imageName = isBlank(dockerModel.getRegistry()) ?
                dockerModel.getName() + ":" + dockerModel.getTag() :
                dockerModel.getRegistry() + "/" + dockerModel.getName() + ":" + dockerModel.getTag();
        cloudMojo.logInfo("Docker image: " + imageName);
        dockerModel.setName(imageName);

        // validate docker image name
        DockerImageName.validate(dockerModel.getName());
        // get the docker output path
        Path dockerDir = kubernetesDataHolder.getDockerArtifactOutputPath();

        cloudMojo.logInfo("building docker image `" + dockerModel.getName() + "` from directory `" + dockerDir + "`.");
        ProcessBuilder pb = new ProcessBuilder("docker", "build", "--no-cache", "--force-rm", "--build-arg",
                "BASE_IMAGE=" + dockerModel.getBaseImage() , "-t", dockerModel.getName(), dockerDir.toFile().toString());
        pb.inheritIO();

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new DockerGenException("docker build failed. refer to the build log");
            }

        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new DockerGenException("docker build failed", e);
        }
        String dockerRunCommand = "docker run -d " + generatePortInstruction(dockerModel.getPorts())
                + dockerModel.getName();
        instructions.put("Execute the below command to run the generated Docker image: ",
                "\t" + dockerRunCommand);
        printInstructions();
    }

    private void createKubernetesArtifacts() throws KubernetesPluginException {
        setDefaultKubernetesInstructions();
        cloudMojo.logInfo("\nGenerating k8s artifacts\n");
        CloudTomlResolver.resolveToml(kubernetesDataHolder.getDeploymentModel());
        new HandlerFactory(cloudMojo).createArtifacts();
        printInstructions();
    }

    public DockerModel getDockerModel() throws DockerGenException {
        TomlTable containerTable = cloudConfig != null ? cloudConfig.getTable("container.image"): null;

        if (containerTable == null) {
            cloudMojo.logInfo("container.image table configuration not found in Cloud.toml, using defaults");
        }
        List<ServiceModel> serviceModels = kubernetesDataHolder.getServiceModelList();
        DeploymentModel deploymentModel = kubernetesDataHolder.getDeploymentModel();

        for (ServiceModel serviceModel : serviceModels) {
            ContainerPort containerPort = new ContainerPortBuilder()
                    .withName(serviceModel.getPortName())
                    .withContainerPort(serviceModel.getTargetPort())
                    .withProtocol(KubernetesConstants.KUBERNETES_SVC_PROTOCOL)
                    .build();
            deploymentModel.addPort(containerPort);
        }

        KubernetesUtils.resolveDockerToml();
        return kubernetesDataHolder.getDockerModel();
    }

    public void populateDeploymentModel() {
        DeploymentModel deploymentModel = kubernetesDataHolder.getDeploymentModel();
        String deploymentName = kubernetesDataHolder.getOutputName();
        if (KubernetesUtils.isBlank(deploymentModel.getName())) {
            if (deploymentName != null) {
                deploymentModel.setName(KubernetesUtils.getValidName(deploymentName)
                        + KubernetesConstants.DEPLOYMENT_POSTFIX);
            }
        }
        deploymentModel.addLabel(KubernetesConstants.KUBERNETES_SELECTOR_KEY, deploymentName);
        kubernetesDataHolder.setDeploymentModel(deploymentModel);
    }

    private void setDefaultKubernetesInstructions() {
        instructions.put("Execute the below command to deploy the Kubernetes artifacts: ",
                "\tkubectl apply -f " + this.kubernetesDataHolder.getK8sArtifactOutputPath().toAbsolutePath());
        if (!kubernetesDataHolder.getServiceModelList().isEmpty()) {
            instructions.put("Execute the below command to access service via NodePort: ",
                    "\tkubectl expose deployment " + this.kubernetesDataHolder.getDeploymentModel().getName() +
                            " --type=NodePort --name=" + kubernetesDataHolder.getDeploymentModel().getName()
                            .replace(KubernetesConstants.DEPLOYMENT_POSTFIX, "-svc-local"));
        }
    }

    private String generatePortInstruction(Set<Integer> ports) {
        if (ports.isEmpty()) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        for (Integer port : ports) {
            output.append("-p ").append(port).append(":").append(port).append(" ");
        }
        return output.toString();
    }

    private void printInstructions() {
        for (Map.Entry<String, String> instruction : instructions.entrySet()) {
            cloudMojo.logInfo(instruction.getKey());
            cloudMojo.logInfo(instruction.getValue());
            cloudMojo.logInfo("");
        }
    }
}
