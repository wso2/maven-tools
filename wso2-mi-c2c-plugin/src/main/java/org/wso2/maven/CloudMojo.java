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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.wso2.maven.exceptions.DockerGenException;
import org.wso2.maven.exceptions.KubernetesPluginException;
import org.wso2.maven.models.KubernetesContext;
import org.wso2.maven.models.KubernetesDataHolder;
import org.wso2.maven.models.ServiceModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.maven.Constants.CAR_FILE_EXTENSION;
import static org.wso2.maven.Constants.CLOUD_TOML_FILE;
import static org.wso2.maven.Constants.CONF_DIR;
import static org.wso2.maven.Constants.DEPLOYMENT_DIR;
import static org.wso2.maven.Constants.DEPLOYMENT_TOML_FILE;
import static org.wso2.maven.Constants.DOCKERFILE_FILE;
import static org.wso2.maven.Constants.DOCKER_DIR;
import static org.wso2.maven.Constants.EXTRACTED_DIR;
import static org.wso2.maven.Constants.REPOSITORY_DIR;
import static org.wso2.maven.Constants.RESOURCES_DIR;
import static org.wso2.maven.Constants.SECURITY_DIR;
import static org.wso2.maven.Constants.TMP_CARBONAPPS_DIR;
import static org.wso2.maven.Constants.TMP_CARBON_HOME;

/**
 * Maven Mojo for generating Docker and Kubernetes artifacts.
 *
 * @goal mi-cloud-deployment
 * @phase package
 **/
public class CloudMojo extends AbstractMojo {
    public static final int HTTP_DEFAULT_PORT = 8290;
    public static final int HTTPS_DEFAULT_PORT = 8253;
    public static final int PORT_DEFAULT_OFFSET = 10;

    /**
     * The Maven Project Object
     *
     * @parameter property="project"
     * @required
     */
    MavenProject project;

    /**
     * The location of the archive file
     *
     * @parameter property="archiveLocation"
     */
    String archiveLocation;

    /**
     * The name of the archive file
     *
     * @parameter property="archiveName"
     */
    String archiveName;

    /**
     * The build options. Possible values: docker, k8s
     *
     * @parameter property="buildOptions"
     */
    String buildOptions = "docker";

    private static final String TMP_DIRECTORY_PATH = System.getProperty("java.io.tmpdir");

    public void logError(String message) {
        getLog().error(message);
    }

    public void logInfo(String message) {
        getLog().info(message);
    }

    public String getArchiveName() {
        return archiveName == null ? project.getArtifactId() + "_" + project.getVersion()
                + CAR_FILE_EXTENSION : archiveName;
    }

    public Path getProjectRoot() {
        return project.getBasedir().toPath();
    }

    public void execute() throws MojoExecutionException {
        //check whether the build option is docker or k8s
        if (StringUtils.isEmpty(buildOptions)) {
            throw new MojoExecutionException("Build option is not provided. Please provide a build option.");
        } else if (!"docker".equals(buildOptions) && !"k8s".equals(buildOptions)) {
            throw new MojoExecutionException(String.format(
                    "Unsupported build option : %s. Supported build options are `docker`, `k8s`", buildOptions));
        }
        // Prepare project artifacts
        prepareProjectArtifacts();

        KubernetesDataHolder dataHolder = KubernetesContext.getInstance().getDataHolder();
        dataHolder.setSourceRoot(project.getBasedir().toPath());
        dataHolder.setOutputName(project.getName());
        dataHolder.setProjectArtifactId(project.getArtifactId());
        dataHolder.setProjectVersion(project.getVersion());

        String basedir = project.getBasedir().toString();
        Path cloudTomlPath = Paths.get(basedir, DEPLOYMENT_DIR, CLOUD_TOML_FILE);
        logInfo("Resolved Cloud config file path: " + cloudTomlPath);
        // if exists parse the cloud.toml file and create memory map
        File file = cloudTomlPath.toFile();
        TomlParseResult tomlResults = null;
        if (file.exists()) {
            logInfo("Cloud config file exists in the given path: " + cloudTomlPath);
            tomlResults = parseCloudToml(cloudTomlPath);
            logInfo("Cloud configs: " + tomlResults.toMap());
        }
        dataHolder.setTomlParseResult(tomlResults);
        setArtifactOutputPath(dataHolder);

        Path deploymentTomlPath = Paths.get(basedir, DEPLOYMENT_DIR, DEPLOYMENT_TOML_FILE);
        Integer offset = readServerOffset(deploymentTomlPath);
        addServices(dataHolder, offset);
        ArtifactManager artifactManager = new ArtifactManager(this, tomlResults);
        artifactManager.populateDeploymentModel();
        try {
            artifactManager.createArtifacts(buildOptions);
        } catch (DockerGenException | KubernetesPluginException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Integer readServerOffset(Path deploymentTomlPath) throws MojoExecutionException {
        try {
            TomlParseResult result = Toml.parse(Files.readString(deploymentTomlPath));
            if (result.hasErrors()) {
                throw new MojoExecutionException("Error parsing deployment.toml file: " + result.errors());
            }

            return result.getLong("server.offset") == null ? null :
                    Objects.requireNonNull(result.getLong("server.offset")).intValue();
        } catch (IOException e) {
            logError("Error while reading the deployment.toml file: " + e.getMessage());
            throw new MojoExecutionException("Error while reading the deployment.toml file", e);
        }
    }

    private void addServices(KubernetesDataHolder dataHolder, Integer offset) {
        int httpServicePort = HTTP_DEFAULT_PORT;
        int httpsServicePort = HTTPS_DEFAULT_PORT;
        if (offset != null) {
            httpServicePort = HTTP_DEFAULT_PORT - PORT_DEFAULT_OFFSET + offset;
            httpsServicePort = HTTPS_DEFAULT_PORT - PORT_DEFAULT_OFFSET + offset;
        }
        // Service model for HTTP service
        ServiceModel httpServiceModel = new ServiceModel();
        httpServiceModel.setPort(httpServicePort);
        httpServiceModel.setTargetPort(httpServicePort);
        httpServiceModel.setPortName("http");
        httpServiceModel.setServiceType("NodePort");
        httpServiceModel.setProtocol("TCP");
        dataHolder.addServiceModel(httpServiceModel);

        // Service model for HTTPS service
        ServiceModel httpsServiceModel = new ServiceModel();
        httpsServiceModel.setPort(httpsServicePort);
        httpsServiceModel.setTargetPort(httpsServicePort);
        httpsServiceModel.setPortName("https");
        httpsServiceModel.setServiceType("NodePort");
        httpsServiceModel.setProtocol("TCP");
        dataHolder.addServiceModel(httpsServiceModel);
    }

    private void setArtifactOutputPath(KubernetesDataHolder dataHolder) throws MojoExecutionException {
        String basedir = project.getBasedir().toString();
        Path dockerOutputPath = Paths.get(basedir, Constants.DEFAULT_TARGET_DIR, Constants.TARGET_DOCKER_DIR);
        try {
            Files.createDirectories(dockerOutputPath);
        } catch (IOException e) {
            logError("Error creating Docker output directory: " + e.getMessage());
            throw new MojoExecutionException("Error creating Docker output directory", e);
        }
        dataHolder.setDockerArtifactOutputPath(dockerOutputPath);

        Path kubernetesOutputPath = Paths.get(basedir, Constants.DEFAULT_TARGET_DIR,
                Constants.TARGET_KUBERNETES_DIR);
        try {
            Files.createDirectories(kubernetesOutputPath);
        } catch (IOException e) {
            logError("Error creating Kubernetes output directory: " + e.getMessage());
            throw new MojoExecutionException("Error creating Kubernetes output directory", e);
        }
        dataHolder.setK8sArtifactOutputPath(kubernetesOutputPath);
    }

    private TomlParseResult parseCloudToml(Path cloudTomlPath) throws MojoExecutionException {
        try {
            String tomlContent = Files.readString(cloudTomlPath);
            TomlParseResult result = Toml.parse(tomlContent);
            if (result.hasErrors()) {
                throw new IOException("Error parsing TOML: " + result.errors());
            }
            return result;
        } catch (IOException e) {
            logError("Error while parsing the cloud.toml file: " + e.getMessage());
            throw new MojoExecutionException("Error while parsing the cloud.toml file", e);
        }
    }

    private void prepareProjectArtifacts() throws MojoExecutionException {
        logInfo("Preparing project artifacts.");
        String basedir = project.getBasedir().toString();

        // with archiveLocation and archiveName compute the resolvedArchiveFilePath and check whether it exists in the given path
        // if exists extract the archive file in to the target folder
        archiveLocation = StringUtils.isEmpty(archiveLocation) ? basedir + File.separator +
                Constants.DEFAULT_TARGET_DIR : archiveLocation;
        Path resolvedArchiveFilePath = Paths.get(archiveLocation).resolve(getArchiveName());
        logInfo("Resolved Archive File Path: " + resolvedArchiveFilePath);
        File file = resolvedArchiveFilePath.toFile();
        if (!file.exists()) {
            logError("Archive file does not exist in the given path: " + resolvedArchiveFilePath);
            throw new MojoExecutionException("Archive file does not exist in the given path: " + resolvedArchiveFilePath);
        }
        logInfo("Archive file exists in the given path: " + resolvedArchiveFilePath);
        Path miHomePath = Paths.get(basedir, Constants.DEFAULT_TARGET_DIR, Constants.TARGET_DOCKER_DIR, TMP_CARBON_HOME);
        Path extractedCarbonApp = extractCarbonApp(resolvedArchiveFilePath, miHomePath);
        logInfo("Extracted Carbon App Path: " + extractedCarbonApp);

        // Copy resources to the target directory.
        copyResourcesToTarget(miHomePath);
    }

    private Path extractCarbonApp(Path resolvedArchiveFilePath, Path carbonHomePath) throws MojoExecutionException {
        // Create a new directory named carbonapps in the /target/docker/carbon-home directory
        Path carbonappsDir = carbonHomePath.resolve(TMP_CARBONAPPS_DIR);
        try {
            Files.createDirectories(carbonappsDir);
        } catch (IOException e) {
            logError("Error creating extraction directory: " + e.getMessage());
            throw new MojoExecutionException("Error creating extraction directory", e);
        }

        // Copy the archive file into the /target/docker/carbon-home/carbonapps directory
        logInfo("Copying the archive file into the new directory: " + carbonappsDir);
        try {
            Files.copy(resolvedArchiveFilePath, carbonappsDir.resolve(resolvedArchiveFilePath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while copying the archive file", e);
        }

        // Create a new directory to extract the archive file
        Path extractionDir = carbonappsDir.resolve(EXTRACTED_DIR).resolve(resolvedArchiveFilePath.getFileName().toString().replace(CAR_FILE_EXTENSION, ""));

        // Extract the archive file into the /target/tmp_docker/carbon-home/carbonapps directory
        logInfo("Extracting the archive file into the new directory: " + extractionDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resolvedArchiveFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, extractionDir);
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            logError("Error while extracting the archive file: " + e.getMessage());
            throw new MojoExecutionException("Error while extracting the archive file", e);
        }
        return extractionDir;
    }

    private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }

    /**
     * Copy the original docker folder to the target directory.
     *
     * @throws MojoExecutionException while copying the resources
     */
    private void copyResourcesToTarget(Path tmpCarbonHomeDirectory) throws MojoExecutionException {
        try {
            String projectLocation = project.getBasedir().getAbsolutePath();
            Path projectDeploymentDir = Paths.get(projectLocation, DEPLOYMENT_DIR);
            Path projectDockerDir = projectDeploymentDir.resolve(DOCKER_DIR);
            File sourceDir = projectDockerDir.resolve(RESOURCES_DIR).toFile();
            File targetDir = tmpCarbonHomeDirectory.resolve(REPOSITORY_DIR).resolve(RESOURCES_DIR)
                    .resolve(SECURITY_DIR).toFile();
            FileUtils.copyDirectory(sourceDir, targetDir);
            FileUtils.copyFile(projectDockerDir.resolve(DOCKERFILE_FILE).toFile(),
                    Paths.get(projectLocation, Constants.DEFAULT_TARGET_DIR, Constants.TARGET_DOCKER_DIR,
                            Constants.DOCKER_FILE).toFile());
            FileUtils.copyFile(projectDeploymentDir.resolve(DEPLOYMENT_TOML_FILE).toFile(),
                        tmpCarbonHomeDirectory.resolve(CONF_DIR).resolve(DEPLOYMENT_TOML_FILE).toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while parsing the deployment.toml file \n" + e);
        }
    }
}
