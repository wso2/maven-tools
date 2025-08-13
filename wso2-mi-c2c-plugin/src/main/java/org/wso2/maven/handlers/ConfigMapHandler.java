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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import org.wso2.maven.CloudMojo;
import org.wso2.maven.KubernetesConstants;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.ConfigMapModel;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.IOException;
import java.util.Collection;

import static org.wso2.maven.Constants.MI_CONFIG_FILES;

/**
 * Generates kubernetes Config Map.
 */
public class ConfigMapHandler extends AbstractArtifactHandler {

    public ConfigMapHandler(CloudMojo cloudMojo) {
        super(cloudMojo);
    }

    private void generate(ConfigMapModel configMapModel) throws KubernetesPluginException {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(configMapModel.getName())
                .withNamespace(dataHolder.getNamespace())
                .endMetadata()
                .withData(configMapModel.getData())
                .build();
        try {
            String configMapContent = KubernetesUtils.asYaml(configMap);
            String outputFileName = KubernetesConstants.CONFIG_MAP_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = configMap.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(configMapContent, outputFileName);
        } catch (IOException e) {
            throw new KubernetesPluginException("Error while generating config map artifact", e);
        }
    }

    public void createArtifacts() throws KubernetesPluginException {
        //Config Map
        Collection<ConfigMapModel> configMapModels = dataHolder.getConfigMapModelSet();
        StringBuilder configTomlEnv = new StringBuilder();
        for (ConfigMapModel configMapModel : configMapModels) {
            if (configMapModel.isMIConf()) {
                configTomlEnv.append(getMIConfigFiles(configMapModel));
            }
            generate(configMapModel);
        }
        
        if (configTomlEnv.length() > 0) {
            EnvVar ballerinaConfEnv = new EnvVarBuilder()
                    .withName(MI_CONFIG_FILES)
                    .withValue(configTomlEnv.toString())
                    .build();
            DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
            deploymentModel.addEnv(ballerinaConfEnv);
            dataHolder.setDeploymentModel(deploymentModel);

        }
        cloudMojo.logInfo("\t@kubernetes:ConfigMap");
    }

    private String getMIConfigFiles(ConfigMapModel configMapModel) {
        StringBuilder configPaths = new StringBuilder();
        for (String key : configMapModel.getData().keySet()) {
            configPaths.append(configMapModel.getMountPath()).append(key).append(":");
        }
        return configPaths.toString();
    }
}
