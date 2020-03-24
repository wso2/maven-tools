/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.config.mapper;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.wso2.config.mapper.model.Context;
import org.wso2.config.mapper.util.FileUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

@Mojo(name = "config-mapper-parser")
public class ConfigMapParserMojo extends AbstractMojo {

    @Parameter(property = "miVersion")
    private String miVersion;

    /**
     * Execution method of Mojo class.
     *
     * @throws MojoExecutionException if error occurred while running the plugin
     */
    public void execute() throws MojoExecutionException {
        try {
            //download templates folder to the resource folder
            boolean isDownloaded = downloadTemplates();
            if (!isDownloaded) {
                return;
            }
            System.setProperty("avoidResolvingEnvAndSysVariables", "true");
            String templatePath = ConfigMapParserConstants.RESOURCES_PATH + File.separator + "templates";
            File deplymentTomlFile = new File(ConfigMapParserConstants.DEPLOYMENT_TOML_PATH);
            boolean isDeploymentTomlFileExist = deplymentTomlFile.exists();

            File templateDirectory = new File(templatePath);
            boolean isTemplateDirectoryExist = templateDirectory.exists();

            boolean isParsedSuccessfully = false;
            if (isDeploymentTomlFileExist && isTemplateDirectoryExist) {
                getLog().info("ConfigParser for deployment.toml file has been started");
                runConfigMapParser(deplymentTomlFile, templatePath);
                isParsedSuccessfully = true;
                getLog().info("ConfigParser successfully parsed the deployment.toml file");
            } else {
                getLog().warn("Required files not found for the Config Parser: deployment.toml " +
                        "file or template files");
            }

            //append output files to the Dockerfile
            if (isParsedSuccessfully) {
                List<String> parsedOutputFileList = new ArrayList<>();
                listFilesForFolder(new File(ConfigMapParserConstants.PARSER_OUTPUT_PATH), parsedOutputFileList);
                updateDockerFile(parsedOutputFileList);
                getLog().info("Dockerfile successfully updated with the config files");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while parsing the deployment.toml file \n" + e);
        }
    }

    /**
     * Download templates files from host and added to the resources.
     *
     * @return templates successfully downloaded or not
     * @throws ZipException while extracting the zip file
     * @throws ConfigParserException while clearing the resources directory
     */
    private boolean downloadTemplates() throws ConfigParserException, ZipException {
        boolean isDownloaded = true;
        String sourceURL = ConfigMapParserConstants.TEMPLATES_URL + miVersion + "/" +
                ConfigMapParserConstants.TEMPLATES_ZIP_FILE;

        //clear the resources directory
        FileUtils.deleteDirectory(new File(ConfigMapParserConstants.RESOURCES_PATH));
        File tempFile = new File(ConfigMapParserConstants.RESOURCES_PATH);
        if (!tempFile.mkdirs()) {
            return false;
        }

        try (InputStream inputStream = new URL(sourceURL).openStream()) {
            if (inputStream.available() == 0) {
                isDownloaded = false;
            } else {
                String templateZipLocation = ConfigMapParserConstants.RESOURCES_PATH + File.separator
                        + ConfigMapParserConstants.TEMPLATES_ZIP_FILE;
                Files.copy(inputStream, Paths.get(templateZipLocation));
                ZipFile zipFile = new ZipFile(templateZipLocation);
                zipFile.extractAll(ConfigMapParserConstants.RESOURCES_PATH);

                File templateZipFile = new File(templateZipLocation);
                if (!templateZipFile.delete()) {
                    getLog().warn("Templates zip file can not delete from the resource path");
                }
            }
        } catch (IOException e) {
            isDownloaded = false;
            getLog().error("Error while downloading the templates for config mapper", e);
        }

        return isDownloaded;
    }

    /**
     * Append the list of parsed files to the Dockerfile.
     *
     * @param deploymentToml deployment toml file
     * @param templatePath path for the template files
     * @throws ConfigParserException while parsing the configurations
     * @throws IOException while parsing the IO operations
     */
    private void runConfigMapParser(File deploymentToml, String templatePath) throws ConfigParserException, IOException {
        Context context = new Context();
        ConfigParser.ConfigPaths.setPaths(ConfigMapParserConstants.DEPLOYMENT_TOML_PATH, templatePath,
                ConfigMapParserConstants.PARSER_OUTPUT_PATH);
        Map<String,String> configMap = ConfigParser.parse(context);
        Map<String,String> metaDataMap = new HashMap<>();

        //clear the CarbonHome directory
        FileUtils.deleteDirectory(new File(ConfigMapParserConstants.PARSER_OUTPUT_PATH));

        for (Map.Entry<String,String> entry : configMap.entrySet()) {
            String fileSource = entry.getValue();
            String filePath = entry.getKey();
            String fileFromOutput = ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator + filePath;

            File tempFile = new File(fileFromOutput);
            tempFile.getParentFile().mkdirs();
            tempFile.createNewFile();
            Files.write(Paths.get(fileFromOutput), fileSource.getBytes());
            metaDataMap.put(filePath, DigestUtils.md5Hex(entry.getValue()));
        }

        //add deployment.toml file md5Hex to the metaDataMap
        String tomlFileSource = new String(Files.readAllBytes(Paths.get(ConfigMapParserConstants.DEPLOYMENT_TOML_PATH)),
                StandardCharsets.UTF_8);
        String tomlOutputPath = ConfigMapParserConstants.CONF_DIR + File.separator
                + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH;
        metaDataMap.put(tomlOutputPath, DigestUtils.md5Hex(tomlFileSource));

        //copy deployment.toml file to the CarbonHome/conf directory
        String confDirPath = ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
                + ConfigMapParserConstants.CONF_DIR + File.separator + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH;
        File destinationFile = new File(confDirPath);
        Files.copy(deploymentToml.toPath(), destinationFile.toPath());

        //create and copy .metadata folder to CarbonHome
        generateMetadataFolder(metaDataMap);
    }

    /**
     * Generate .metadata_config.properties file and set properties.
     *
     * @param metaDataMap parser generated metadata map
     * @throws IOException while writing to the metadata_template.properties
     */
    private void generateMetadataFolder(Map<String,String> metaDataMap) throws IOException {
        String templateFilePath = ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
                + ConfigMapParserConstants.METADATA_DIR + File.separator
                + ConfigMapParserConstants.METADATA_CONFIG_PROPERTIES_FILE;

        //create the metadata_config.properties
        File tempFile = new File(templateFilePath);
        if (!tempFile.getParentFile().mkdirs() || !tempFile.createNewFile()) {
            throw new IOException("Creating .metadata directory in " + ConfigMapParserConstants.PARSER_OUTPUT_PATH
                    + " path failed");
        }

        try (OutputStream output = new FileOutputStream(templateFilePath)) {
            Properties prop = new Properties();

            // set the properties value
            for (Map.Entry<String,String> entry : metaDataMap.entrySet()) {
                prop.setProperty(entry.getKey(), entry.getValue());
            }

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException e) {
            throw new IOException("Exception while writing to the "
                    + ConfigMapParserConstants.METADATA_CONFIG_PROPERTIES_FILE + "\n" + e);
        }
    }

    /**
     * Append the list of parsed files to the Dockerfile.
     *
     * @param parsedOutputFileList list of parsed files
     * @throws IOException while writing to the Dockerfile
     */
    private void updateDockerFile(List<String> parsedOutputFileList) throws IOException {
        String dockerFileBaseEntries = getBaseImageInDockerfile();
        StringBuilder builder = new StringBuilder(dockerFileBaseEntries);

        builder.append(System.lineSeparator()).append(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_BEGIN)
                .append(System.lineSeparator());
        String splitPattern = Pattern.quote(System.getProperty("file.separator"));
        for (String filePath : parsedOutputFileList) {
            String[] filePathSeparateList = filePath.split(splitPattern);
            StringBuilder innerBuilder = new StringBuilder();
            for (int x = 1; x < filePathSeparateList.length - 1 ; x++) {
                innerBuilder.append(filePathSeparateList[x]).append(ConfigMapParserConstants.PATH_SEPARATOR);
            }
            innerBuilder.append(filePathSeparateList[filePathSeparateList.length - 1]);
            builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).append(filePath.replaceAll(splitPattern,
                    ConfigMapParserConstants.PATH_SEPARATOR)).append(ConfigMapParserConstants.DOCKER_MI_DIR_PATH)
                    .append(innerBuilder.toString());
            builder.append(System.lineSeparator());
        }

        //copy .metadata folder to the repository/resources/conf directory
        builder.append(ConfigMapParserConstants.DOCKER_MAKE_DIR + ConfigMapParserConstants.METADATA_DIR_PATH);
        builder.append(System.lineSeparator());
        builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).append(ConfigMapParserConstants.PARSER_OUTPUT_PATH)
                .append(ConfigMapParserConstants.PATH_SEPARATOR).append(ConfigMapParserConstants.METADATA_DIR)
                .append(ConfigMapParserConstants.PATH_SEPARATOR)
                .append(ConfigMapParserConstants.METADATA_CONFIG_PROPERTIES_FILE)
                .append(ConfigMapParserConstants.METADATA_DIR_PATH).append(System.lineSeparator());
        builder.append(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_END).append(System.lineSeparator());

        try (InputStream inputStream = new ByteArrayInputStream(builder.toString()
                .getBytes(StandardCharsets.UTF_8));
             OutputStream outputStream = new FileOutputStream(new File(ConfigMapParserConstants.DOCKER_FILE))) {
             IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new IOException("Exception while writing to the Dockerfile \n" + e);
        }
    }

    /**
     * List down the files which inside a diractory.
     *
     * @param folder folder as a File
     * @param outputList list for store outputs
     */
    private void listFilesForFolder(final File folder, List<String> outputList) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (final File fileEntry : listOfFiles) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry, outputList);
                } else if (fileEntry.isFile() && !fileEntry.isHidden()) {
                    outputList.add(fileEntry.getPath());
                }
            }
        }
    }

    /**
     * Get default/initial lines from Dockerfile.
     *
     * @return default Dockerfile entries
     * @throws IOException exception while reading dockerfile
     */
    private String getBaseImageInDockerfile() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader bufferReader = new BufferedReader(new FileReader(ConfigMapParserConstants.DOCKER_FILE))) {
            String currentLine;
            while ((currentLine = bufferReader.readLine()) != null) {
                if (currentLine.contains(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_BEGIN)) {
                    String autoGenerateCommandLine;
                    while ((autoGenerateCommandLine = bufferReader.readLine()) != null) {
                        if (autoGenerateCommandLine.contains(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_END)) {
                            break;
                        }
                    }
                } else {
                    builder.append(currentLine).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new IOException("Exception while writing to the Dockerfile \n" + e);
        }

        return builder.toString();
    }
}
