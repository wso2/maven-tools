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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import org.apache.commons.codec.binary.Base64;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.ConfigMapModel;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.KubernetesContext;
import org.wso2.maven.models.KubernetesDataHolder;
import org.wso2.maven.models.PersistentVolumeClaimModel;
import org.wso2.maven.models.SecretModel;
import org.wso2.maven.utils.KubernetesUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wso2.maven.KubernetesConstants.DEPLOYMENT_POSTFIX;
import static org.wso2.maven.KubernetesConstants.MI_CONF_FILE_NAME;
import static org.wso2.maven.KubernetesConstants.CONFIG_MAP_POSTFIX;
import static org.wso2.maven.KubernetesConstants.MIN_CPU;
import static org.wso2.maven.KubernetesConstants.MIN_MEMORY;
import static org.wso2.maven.KubernetesConstants.MI_CONF_MOUNT_PATH;
import static org.wso2.maven.KubernetesConstants.MI_CONF_SECRETS_MOUNT_PATH;
import static org.wso2.maven.KubernetesConstants.MI_HOME;
import static org.wso2.maven.KubernetesConstants.MI_TMP_HOME;
import static org.wso2.maven.KubernetesConstants.SECRET_POSTFIX;
import static org.wso2.maven.utils.KubernetesUtils.getValidName;
import static org.wso2.maven.utils.KubernetesUtils.isBlank;

public class CloudTomlResolver {

    private CloudTomlResolver() {
    }

    public static final String CLOUD_DEPLOYMENT = "cloud.deployment.";

    public static final KubernetesDataHolder dataHolder = KubernetesContext.getInstance().getDataHolder();

    public static void resolveToml(DeploymentModel deploymentModel) throws KubernetesPluginException {

        TomlParseResult cloudConfig = dataHolder.getTomlParseResult();
        if (cloudConfig != null) {
            // Deployment configs
            resolveDeploymentToml(deploymentModel, cloudConfig);

            // Resolve settings
            resolveSettingsToml(cloudConfig);

            // Resources
            resolveResourcesToml(deploymentModel, cloudConfig);

            // Env vars
            resolveEnvToml(deploymentModel, cloudConfig);

            // Config.toml files
            resolveConfigMapToml(cloudConfig);
            resolveConfigSecretToml(cloudConfig);

            // Config files
            resolveConfigFilesToml(deploymentModel, cloudConfig);

            // Secret files
            resolveSecretToml(deploymentModel, cloudConfig);

            // Resolve Volumes
            resolveVolumes(deploymentModel, cloudConfig);
        }

    }

    private static void resolveSettingsToml(TomlParseResult cloudConfig) {
        dataHolder.setSingleYaml(cloudConfig.getBoolean("settings.singleYAML", () -> true));
    }

    private static void resolveVolumes(DeploymentModel deploymentModel, TomlParseResult cloudConfig) {
        if (!cloudConfig.contains("cloud.deployment.storage.volumes") || !cloudConfig.isArray("cloud.deployment.storage.volumes")) {
            return;
        }
        List<Object> volumes = Objects.requireNonNull(cloudConfig.getArray("cloud.deployment.storage.volumes")).toList();
        Set<PersistentVolumeClaimModel> persistentVolumeClaimModels = new HashSet<>();
        volumes.forEach(volume -> {
            if (volume instanceof TomlTable) {
                TomlTable volumeTable = (TomlTable) volume;
                PersistentVolumeClaimModel pv = new PersistentVolumeClaimModel();
                pv.setName(volumeTable.getString("name"));
                pv.setMountPath(volumeTable.getString("local_path"));
                pv.setVolumeClaimSizeAmount(volumeTable.getString("size"));
                persistentVolumeClaimModels.add(pv);
            }
        });
        deploymentModel.setVolumeClaimModels(persistentVolumeClaimModels);
    }

    private static void resolveSecretToml(DeploymentModel deploymentModel, TomlParseResult cloudConfig) throws KubernetesPluginException {
        if (!cloudConfig.contains("cloud.secret.files") || !cloudConfig.isArray("cloud.secret.files")) {
            return;
        }
        List<Object> secrets = Objects.requireNonNull(cloudConfig.getArray("cloud.secret.files")).toList();

        if (!secrets.isEmpty()) {
            final String deploymentName = deploymentModel.getName().replace(DEPLOYMENT_POSTFIX, "");

            for (int i = 0, secretsSize = secrets.size(); i < secretsSize; i++) {
                if (secrets.get(i) instanceof TomlTable) {
                    TomlTable secret = (TomlTable) secrets.get(i);
                    Path path = Paths.get(Objects.requireNonNull(secret.getString("file")));
                    int finalI = i;
                    Path mountPath = Paths.get(Objects.requireNonNull(secret.getString( "mount_dir",
                            () -> getSecretMountPath(finalI))));
                    final Path fileName = validatePaths(path, mountPath);
                    File file = path.toFile();
                    KubernetesUtils.validateFileExistence(file);
                    SecretModel secretModel = new SecretModel();
                    if (!mountPath.isAbsolute()) {
                        mountPath = Paths.get(MI_TMP_HOME, mountPath.toString());
                    }

                    String mountPathSr = mountPath.toString();
                    if (file.isDirectory()) {
                        secretModel.setDir(true);
                    } else {
                        mountPathSr = getModifiedMountPath(mountPath.toString(), fileName.toString());
                    }

                    secretModel.setName(deploymentName + "-" + getValidName(fileName.toString()) + SECRET_POSTFIX + i);
                    secretModel.setData(getData(path, true));
                    secretModel.setMountPath(mountPathSr);
                    dataHolder.addSecrets(Collections.singleton(secretModel));
                }
            }
        }
    }


    public static void resolveConfigFilesToml(DeploymentModel kubernetesModel, TomlParseResult cloudConfig)
            throws KubernetesPluginException {
        if (!cloudConfig.contains("cloud.config.maps") || !cloudConfig.isArray("cloud.config.maps")) {
            return;
        }
        AtomicInteger i = new AtomicInteger();
        List<Object> configMaps = Objects.requireNonNull(cloudConfig.getArray("cloud.config.maps")).toList();
        for (Object map : configMaps) {
            if (map instanceof TomlTable) {
                TomlTable mapTable = (TomlTable) map;
                final String deploymentName = kubernetesModel.getName().replace(DEPLOYMENT_POSTFIX, "");
                Path path = Paths.get(Objects.requireNonNull(mapTable.getString("file")));
                Path mountPath = Paths.get(Objects.requireNonNull(mapTable.getString("mount_dir")));
                final Path fileName = validatePaths(path, mountPath);
                ConfigMapModel configMapModel = new ConfigMapModel();
                if (!mountPath.isAbsolute()) {
                    mountPath = Paths.get(MI_TMP_HOME, mountPath.toString());
                }
                File file = path.toFile();
                KubernetesUtils.validateFileExistence(file);
                String mountPathSr = mountPath.toString();
                if (file.isDirectory()) {
                    configMapModel.setDir(true);
                } else {
                    mountPathSr = getModifiedMountPath(mountPath.toString(), fileName.toString());
                }
                configMapModel.setName(deploymentName + "-" + getValidName(fileName.toString()) + "cfg" + i);
                configMapModel.setData(getData(path, false));
                configMapModel.setMountPath(mountPathSr);
                configMapModel.setMIConf(false);
                dataHolder.addConfigMaps(Collections.singleton(configMapModel));
            }
        }
    }


    private static Path validatePaths(Path path, Path mountPath) throws KubernetesPluginException {

        final Path homePath = Paths.get(MI_HOME);
        final Path confPath = Paths.get(MI_CONF_MOUNT_PATH);
        final Path secretPath = Paths.get(MI_CONF_SECRETS_MOUNT_PATH);
        if (mountPath.equals(homePath)) {
            throw new KubernetesPluginException("Invalid mount path: " + mountPath + ". Cannot mount to " + MI_HOME);
        }
        if (mountPath.equals(confPath)) {
            throw new KubernetesPluginException("Invalid mount path: " + mountPath + ". Cannot mount to " + MI_CONF_MOUNT_PATH);
        }
        if (mountPath.equals(secretPath)) {
            throw new KubernetesPluginException("Invalid mount path: " + mountPath + ". Cannot mount to " + MI_CONF_SECRETS_MOUNT_PATH);
        }
        final Path fileName = path.getFileName();
        if (fileName == null) {
            throw new KubernetesPluginException("Invalid file path: " + path);
        }
        return fileName;
    }

    private static void resolveConfigSecretToml(TomlParseResult cloudConfig)
            throws KubernetesPluginException {
        if (!cloudConfig.contains("cloud.config.secrets") || !cloudConfig.isArray("cloud.config.secrets")) {
            return;
        }
        AtomicInteger i = new AtomicInteger();
        List<Object> configSecrets = Objects.requireNonNull(cloudConfig.getArray("cloud.config.secrets")).toList();


        for (Object secret: configSecrets) {
            if (secret instanceof TomlTable) {
                TomlTable secretTable = (TomlTable) secret;
                SecretModel secretModel = new SecretModel();
                String defaultValue = getValidName(MI_CONF_FILE_NAME.replace(".toml", "")) + SECRET_POSTFIX;
                String name = secretTable.getString("name", () -> defaultValue);
                Path path = Paths.get(Objects.requireNonNull(secretTable.getString("file")));
                // Resolve Config.toml
                Path fileName = path.getFileName();
                if (fileName == null) {
                    throw new KubernetesPluginException("Invalid file path: " + path);
                }
                Path dataFilePath = path;
                if (!path.isAbsolute()) {
                    dataFilePath = KubernetesContext.getInstance().getDataHolder().getSourceRoot().resolve(dataFilePath)
                            .normalize();
                }

                secretModel.setName(name);
                secretModel.setData(getData(dataFilePath, true));
                secretModel.setMountPath(getSecretMountPath(i.incrementAndGet()));
                secretModel.setBallerinaConf(true);
                dataHolder.addSecrets(Collections.singleton(secretModel));
            }
        }
    }

    private static String getSecretMountPath(int secretCount) {
        if (secretCount == 0) {
            return MI_CONF_SECRETS_MOUNT_PATH + "/";
        }
        return MI_CONF_SECRETS_MOUNT_PATH + secretCount + "/";
    }

    private static Map<String, String> getData(Path dataFilePath, boolean isSecret) throws KubernetesPluginException {
        Map<String, String> dataMap = new HashMap<>();
        File file = dataFilePath.toFile();
        if (!file.isDirectory()) {
            // Read all files
            String key = String.valueOf(dataFilePath.getFileName());
            String content = getContent(KubernetesUtils.readFileContent(dataFilePath), isSecret);
            dataMap.put(key, content);
            return dataMap;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return dataMap;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                continue;
            }
            String key = f.getName();
            String content = getContent(KubernetesUtils.readFileContent(f.toPath()), isSecret);
            dataMap.put(key, content);
        }
        return dataMap;
    }

    private static String getContent(byte[] content, boolean isSecret) {
        if (isSecret) {
            return Base64.encodeBase64String(content);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private static void resolveConfigMapToml(TomlParseResult cloudConfig) throws KubernetesPluginException {
        if (!cloudConfig.contains("cloud.config.files") || !cloudConfig.isArray("cloud.config.files")) {
            return;
        }
        AtomicInteger i = new AtomicInteger();
        List<Object> configFiles = Objects.requireNonNull(cloudConfig.getArray("cloud.config.files")).toList();

        for(Object config: configFiles) {
            if (config instanceof TomlTable) {
                TomlTable configTable = (TomlTable) config;
                ConfigMapModel configMapModel = new ConfigMapModel();
                String defaultValue = getValidName(MI_CONF_FILE_NAME) + CONFIG_MAP_POSTFIX;
                String name = configTable.getString("name", () -> defaultValue);

                Path path = Paths.get(Objects.requireNonNull(configTable.getString("file")));
                // Resolve Config.toml
                Path fileName = path.getFileName();
                if (fileName == null) {
                    throw new KubernetesPluginException("Invalid file path: " + path);
                }
                Path dataFilePath = path;
                if (!path.isAbsolute()) {
                    dataFilePath = KubernetesContext.getInstance().getDataHolder().getSourceRoot().resolve(dataFilePath)
                            .normalize();
                }
                String content = new String(KubernetesUtils.readFileContent(dataFilePath), StandardCharsets.UTF_8);
                Optional<ConfigMapModel> configMap = getConfigMapModel(name);
                if (configMap.isEmpty()) {
                    configMapModel.setName(name);
                    configMapModel.setMountPath(getConfMountPath(i.incrementAndGet()));
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put(fileName.toString(), content);
                    configMapModel.setData(dataMap);
                    configMapModel.setMIConf(true);
                    configMapModel.setReadOnly(false);
                    configMapModel.setDir(false);
                    dataHolder.addConfigMaps(Collections.singleton(configMapModel));
                } else {
                    ConfigMapModel existingConfigMap = configMap.get();
                    Map<String, String> data = existingConfigMap.getData();
                    if (data.containsKey(fileName.toString())) {
                        throw new KubernetesPluginException("Duplicate ConfigMap file name found: " + fileName);
                    }
                    data.put(fileName.toString(), content);
                }
            }
        }
    }

    private static String getConfMountPath(int confCount) {

        if (confCount == 0) {
            return MI_CONF_MOUNT_PATH + "/";
        }
        return MI_CONF_MOUNT_PATH + confCount + "/";
    }

    private static Optional<ConfigMapModel> getConfigMapModel(String name) {

        Set<ConfigMapModel> configMapModelSet = dataHolder.getConfigMapModelSet();
        for (ConfigMapModel configMapModel : configMapModelSet) {
            if (configMapModel.getName().equals(name)) {
                return Optional.of(configMapModel);
            }
        }
        return Optional.empty();
    }

    private static void resolveEnvToml(DeploymentModel deploymentModel, TomlParseResult cloudConfig) {
        if (cloudConfig.contains("cloud.config.envs") || cloudConfig.isArray("cloud.config.envs")) {
            Objects.requireNonNull(cloudConfig.getArray("cloud.config.envs")).toList().forEach(env -> {
                if (env instanceof TomlTable) {
                    TomlTable envTable = (TomlTable) env;
                    EnvVar envVar = new EnvVarBuilder()
                            .withName(envTable.getString("name"))
                            .withNewValueFrom()
                            .withNewConfigMapKeyRef()
                            .withKey(envTable.getString(KubernetesConstants.KEY_REF))
                            .withName(envTable.getString("config_name"))
                            .endConfigMapKeyRef()
                            .endValueFrom()
                            .build();
                    if (isBlank(envVar.getName())) {
                        envVar.setName(envTable.getString(KubernetesConstants.KEY_REF));
                    }
                    deploymentModel.addEnv(envVar);
                }
            });
        }
        if (cloudConfig.contains("cloud.secret.envs") || cloudConfig.isArray("cloud.secret.envs")) {
            Objects.requireNonNull(cloudConfig.getArray("cloud.secret.envs")).toList().forEach(env -> {
                if (env instanceof TomlTable) {
                    TomlTable envTable = (TomlTable) env;
                    EnvVar envVar = new EnvVarBuilder()
                            .withName(envTable.getString("name"))
                            .withNewValueFrom()
                            .withNewSecretKeyRef()
                            .withKey(envTable.getString(KubernetesConstants.KEY_REF))
                            .withName(envTable.getString("secret_name"))
                            .endSecretKeyRef()
                            .endValueFrom()
                            .build();
                    if (isBlank(envVar.getName())) {
                        envVar.setName(envTable.getString(KubernetesConstants.KEY_REF));
                    }
                    deploymentModel.addEnv(envVar);
                }
            });
        }
    }

    private static void resolveDeploymentToml(DeploymentModel deploymentModel, TomlParseResult ballerinaCloud) {

        deploymentModel.setReplicas(Math.toIntExact(ballerinaCloud.getLong(CLOUD_DEPLOYMENT + "replicas",
                deploymentModel::getReplicas)));
        TomlTable probeToml = ballerinaCloud.getTable(CLOUD_DEPLOYMENT + "probes.readiness");
        if (probeToml != null) {
            deploymentModel.setReadinessProbe(resolveProbeToml(probeToml));
        }
        probeToml = ballerinaCloud.getTable(CLOUD_DEPLOYMENT + "probes.liveness");
        if (probeToml != null) {
            deploymentModel.setLivenessProbe(resolveProbeToml(probeToml));
        }
        deploymentModel.setInternalDomainName(ballerinaCloud.getString(CLOUD_DEPLOYMENT +
                "internal_domain_name"));
    }

    private static Probe resolveProbeToml(TomlTable probeToml) {
        //Resolve Probe.
        Probe probe = new ProbeBuilder().build();
        HTTPGetAction httpGet = new HTTPGetAction();
        final Long port = probeToml.getLong("port");
        if (port != null) {
            httpGet.setPort(new IntOrString(Math.toIntExact(port)));
        }
        httpGet.setPath(probeToml.getString("path"));
        probe.setInitialDelaySeconds(30);
        probe.setHttpGet(httpGet);
        return probe;
    }

    private static void resolveResourcesToml(DeploymentModel deploymentModel, TomlParseResult ballerinaCloud) {
        // Resolve resources
        Map<String, Quantity> requests = deploymentModel.getResourceRequirements().getRequests();
        String minMemory = ballerinaCloud.getString(CLOUD_DEPLOYMENT + MIN_MEMORY);
        if (minMemory != null) {
            requests.put(KubernetesConstants.MEMORY, new Quantity(minMemory));
        }
        String minCPU = ballerinaCloud.getString(CLOUD_DEPLOYMENT + MIN_CPU);
        if (minCPU != null) {
            requests.put(KubernetesConstants.CPU, new Quantity(minCPU));
        }
        Map<String, Quantity> limits = deploymentModel.getResourceRequirements().getLimits();

        String maxMemory = ballerinaCloud.getString(CLOUD_DEPLOYMENT + "max_memory");
        if (maxMemory != null) {
            limits.put(KubernetesConstants.MEMORY, new Quantity(maxMemory));
        }
        String maxCPU = ballerinaCloud.getString(CLOUD_DEPLOYMENT + "max_cpu");
        if (maxCPU != null) {
            limits.put(KubernetesConstants.CPU, new Quantity(maxCPU));
        }
        deploymentModel.getResourceRequirements().setLimits(limits);
        deploymentModel.getResourceRequirements().setRequests(requests);
    }

    private static String getModifiedMountPath(String mountDir, String fileName) {

        if (mountDir.endsWith("/")) {
            return mountDir + fileName;
        } else {
            return mountDir + "/" + fileName;
        }
    }
}
