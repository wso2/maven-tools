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

package org.wso2.maven.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.ConfigMapModel;
import org.wso2.maven.models.DeploymentModel;
import org.wso2.maven.models.DockerModel;
import org.wso2.maven.models.KubernetesContext;
import org.wso2.maven.models.KubernetesDataHolder;
import org.wso2.maven.models.KubernetesModel;
import org.wso2.maven.models.SecretModel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.maven.Constants.WSO2_MI_DEFAULT_BASE_IMAGE;
import static org.wso2.maven.KubernetesConstants.DEPLOYMENT_POSTFIX;
import static org.wso2.maven.KubernetesConstants.YAML;

public class KubernetesUtils {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID));

    /* Checks if a String is empty ("") or null.
     *
     * @param str the String to check, may be null
     * @return true if the String is empty or null
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns valid kubernetes name.
     *
     * @param name actual value
     * @return valid name
     */
    public static String getValidName(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        name = name.toLowerCase(Locale.getDefault()).replaceAll("[_.]", "-")
                .replaceAll("[$]", "").replaceAll("/", "-")
                .replaceAll("--", "-");
        name = name.substring(0, Math.min(name.length(), 15));
        if (name.endsWith("-")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Read contents of a File.
     *
     * @param targetFilePath target file path
     * @throws MojoExecutionException If an error occurs when reading file
     */
    public static byte[] readFileContent(Path targetFilePath) throws KubernetesPluginException {
        File file = targetFilePath.toFile();
        // append if file exists
        if (file.exists() && !file.isDirectory()) {
            try {
                return Files.readAllBytes(targetFilePath);
            } catch (IOException e) {
                throw new KubernetesPluginException("Error while reading file: " + targetFilePath, e);
            }
        }
        throw new KubernetesPluginException("File not found: " + targetFilePath);
    }

    public static void validateFileExistence(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
    }

    public static <T> String asYaml(T object) throws KubernetesPluginException {
        try {
            return YAML_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new KubernetesPluginException("Error while converting object to yaml: " + object, e);
        }
    }

    public static void writeToFile(String context, String outputFileName) throws IOException {
        KubernetesDataHolder dataHolder = KubernetesContext.getInstance().getDataHolder();
        writeToFile(dataHolder.getK8sArtifactOutputPath(), context, outputFileName);
    }

    /**
     * Write content to a File. Create the required directories if they don't not exists.
     *
     * @param outputDir  Artifact output path.
     * @param context    Context of the file
     * @param fileSuffix Suffix for artifact.
     * @throws IOException If an error occurs when writing to a file
     */
    public static void writeToFile(Path outputDir, String context, String fileSuffix) throws IOException {
        KubernetesDataHolder dataHolder = KubernetesContext.getInstance().getDataHolder();
        final String outputName = dataHolder.getOutputName();
        Path artifactFileName = outputDir.resolve(outputName + fileSuffix);
        DeploymentModel deploymentModel = dataHolder.getDeploymentModel();
        // Priority given for job, then deployment.
        if (deploymentModel != null && dataHolder.isSingleYaml()) {
            artifactFileName = outputDir.resolve(outputName + YAML);
        }

        File newFile = artifactFileName.toFile();
        // append if file exists
        if (newFile.exists()) {
            Files.write(artifactFileName, context.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            return;
        }
        //create required directories
        if (newFile.getParentFile().mkdirs()) {
            Files.write(artifactFileName, context.getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.write(artifactFileName, context.getBytes(StandardCharsets.UTF_8));
    }

    public static Collection<? extends VolumeMount> generateSecretVolumeMounts(Set<SecretModel> secretModels) {
        List<VolumeMount> volumeMounts = new ArrayList<>();
        for (SecretModel secretModel : secretModels) {
            VolumeMountBuilder volumeMountBuilder = new VolumeMountBuilder()
                    .withMountPath(secretModel.getMountPath())
                    .withName(secretModel.getName() + "-volume")
                    .withReadOnly(secretModel.isReadOnly());
            if ((!secretModel.isDir()) && (!secretModel.isBallerinaConf())) {
                volumeMountBuilder.withSubPath(KubernetesUtils.getFileNameOfSecret(secretModel));
            }
            VolumeMount volumeMount = volumeMountBuilder.build();
            volumeMounts.add(volumeMount);
        }
        return volumeMounts;
    }

    private static String getFileNameOfSecret(SecretModel secretModel) {
        Map<String, String> data = secretModel.getData();
        return data.keySet().iterator().next();
    }

    public static String getFileNameOfConfigMap(ConfigMapModel configMapModel) {
        Map<String, String> data = configMapModel.getData();
        return data.keySet().iterator().next();
    }

    public static void resolveDockerToml() {
        KubernetesDataHolder dataHolder = KubernetesContext.getInstance().getDataHolder();
        TomlParseResult toml = dataHolder.getTomlParseResult();
        TomlTable containerTable = toml != null ? toml.getTable("container.image") : null;
        DockerModel dockerModel = dataHolder.getDockerModel();

        if (containerTable != null) {
            dockerModel
                    .setRegistry(containerTable.getString("repository", () -> "myapp"));
            dockerModel.setTag(containerTable.getString("tag", dataHolder::getProjectVersion));
            dockerModel.setName(containerTable.getString("name",
                    () -> dataHolder.getProjectArtifactId().toLowerCase()));
            dockerModel.setBaseImage(containerTable.getString("baseImage", () -> WSO2_MI_DEFAULT_BASE_IMAGE));
        } else {
            dockerModel.setName(dataHolder.getProjectArtifactId().toLowerCase());
            dockerModel.setTag(dataHolder.getProjectVersion());
            dockerModel.setRegistry("myapp");
            dockerModel.setBaseImage(WSO2_MI_DEFAULT_BASE_IMAGE);
        }
        dockerModel.setPorts(dataHolder.getDeploymentModel().getPorts().stream()
                .map(ContainerPort::getContainerPort)
                .collect(Collectors.toSet()));

        String imageName = isBlank(dockerModel.getRegistry()) ?
                dockerModel.getName() + ":" + dockerModel.getTag() :
                dockerModel.getRegistry() + "/" + dockerModel.getName() + ":" + dockerModel.getTag();
        dataHolder.getDeploymentModel().setImage(imageName);
    }
}
