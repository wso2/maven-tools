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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.wso2.maven.CloudMojo;
import org.wso2.maven.KubernetesConstants;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.SecretModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.wso2.maven.Constants.MI_CONFIG_FILES;

/**
 * Generates kubernetes secret.
 */
public class SecretHandler extends AbstractArtifactHandler {

    public SecretHandler(CloudMojo cloudMojo) {
        super(cloudMojo);
    }

    private void generate(SecretModel secretModel) throws KubernetesPluginException {
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withNamespace(dataHolder.getNamespace())
                .withName(secretModel.getName())
                .endMetadata()
                .withData(secretModel.getData())
                .build();
        try {
            String secretContent = KubernetesUtils.asYaml(secret);
            String outputFileName = KubernetesConstants.SECRET_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = secret.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(secretContent, outputFileName);
        } catch (IOException e) {
            throw new KubernetesPluginException("Error while generating secret artifact", e);
        }
    }

    @Override
    public void createArtifacts() throws KubernetesPluginException {
        Collection<SecretModel> secretModels = dataHolder.getSecretModelSet();
        StringBuilder configTomlEnv = new StringBuilder();
        for (SecretModel secretModel : secretModels) {
            if (secretModel.isBallerinaConf()) {
                configTomlEnv.append(getBALConfigFiles(secretModel));
            }
            generate(secretModel);
        }

        if (configTomlEnv.length() > 0) {
            DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
            List<EnvVar> envVars = deploymentModel.getEnvVars();
            if (isBalConfigFilesEnvExist(envVars)) {
                for (EnvVar envVar : envVars) {
                    if (envVar.getName().equals(MI_CONFIG_FILES)) {
                        String value = envVar.getValue() + configTomlEnv;
                        envVar.setValue(value);
                    }
                }
            } else {
                EnvVar ballerinaConfEnv = new EnvVarBuilder()
                        .withName(MI_CONFIG_FILES)
                        .withValue(configTomlEnv.toString())
                        .build();
                deploymentModel.addEnv(ballerinaConfEnv);
                dataHolder.setDeploymentModel(deploymentModel);
            }
        }
        cloudMojo.logInfo("\t@kubernetes:Secret");
    }

    private String getBALConfigFiles(SecretModel secretModel) {
        StringBuilder configPaths = new StringBuilder();
        for (String key : secretModel.getData().keySet()) {
            configPaths.append(secretModel.getMountPath()).append(key).append(":");
        }
        return configPaths.toString();
    }
    
    private boolean isBalConfigFilesEnvExist(List<EnvVar> envVars) {
        for (EnvVar envVar : envVars) {
            if (envVar.getName().equals("BAL_CONFIG_FILES")) {
                return true;
            }
        }
        return false;
    }
}
