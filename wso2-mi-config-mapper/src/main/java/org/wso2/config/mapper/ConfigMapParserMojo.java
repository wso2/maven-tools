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

import net.consensys.cava.toml.Toml;
import net.consensys.cava.toml.TomlParseResult;
import net.consensys.cava.toml.TomlTable;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.wso2.ciphertool.CipherTool;
import org.wso2.ciphertool.utils.Constants;
import org.wso2.config.mapper.model.Context;
import org.wso2.config.mapper.util.FileUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

@Mojo(name = "config-mapper-parser")
public class ConfigMapParserMojo extends AbstractMojo {

    @Parameter(property = "miVersion")
    private String miVersion;

    @Parameter(property = "keystoreName")
    private String keystoreName;

    @Parameter(property = "keystoreAlias")
    private String keystoreAlias;

    @Parameter(property = "keystorePassword")
    private String keystorePassword;

    @Parameter(property = "keystoreType")
    private String keystoreType;

    @Parameter(property = "cipherTransformation")
    private String cipherTransformation;

    @Parameter(property = "projectLocation")
    private String projectLocation;

    @Parameter(property = "executeCipherTool")
    private Boolean executeCipherTool;

    /**
     * Execution method of Mojo class.
     *
     * @throws MojoExecutionException if error occurred while running the plugin
     */
    public void execute() throws MojoExecutionException {

        try {
            projectLocation += File.separator;
            //download templates folder to the resource folder
            boolean isDownloaded = downloadTemplates();
            if (!isDownloaded) {
                getLog().error("Error while downloading ConfigMapper templates for version " + miVersion);
                updateDockerfileAsDefault();
                return;
            }
            System.setProperty("avoidResolvingEnvAndSysVariables", "true");
            String templatePath = projectLocation + ConfigMapParserConstants.RESOURCES_PATH
                    + File.separator + "templates";
            File deploymentTomlFile = new File(projectLocation + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH);
            boolean isDeploymentTomlFileExist = deploymentTomlFile.exists();

            File templateDirectory = new File(templatePath);
            boolean isTemplateDirectoryExist = templateDirectory.exists();

            // cipherTool will only run if user sets the property
            if (executeCipherTool) {
                initializeSystemProperties();
                if (cipherTransformation == null) {
                    // default encryption algorithm
                    cipherTransformation = "RSA/ECB/OAEPwithSHA1andMGF1Padding";
                }
                String[] args = new String[]{"-Dconfigure", "-Dorg.wso2.CipherTransformation=" + cipherTransformation};

                //hide println message logs from ciphertool
                PrintStream originalStream = System.out;
                PrintStream dummyStream = new PrintStream(new OutputStream(){
                    public void write(int b) {
                    }
                });
                System.setOut(dummyStream);
                CipherTool.main(args);
                //enable past print stream
                System.setOut(originalStream);
                getLog().info("ConfigParser successfully encrypted all the given secrets");
                getLog().info("Secret Configurations are written to the property file successfully");
                updateSecretConf();
                createPasswordTmpFile();
            }

            boolean isParsedSuccessfully = false;
            if (isDeploymentTomlFileExist && isTemplateDirectoryExist) {
                getLog().info("ConfigParser for deployment.toml file has been started");
                runConfigMapParser(deploymentTomlFile, templatePath);
                isParsedSuccessfully = true;
                getLog().info("ConfigParser successfully parsed the deployment.toml file");
            } else {
                getLog().warn("Required files not found for the Config Parser: deployment.toml " +
                        "file or template files");
            }

            //append output files to the Dockerfile
            if (isParsedSuccessfully) {
                List<String> parsedOutputFileList = new ArrayList<>();
                listFilesForFolder(new File(projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH), parsedOutputFileList);
                updateDockerFile(parsedOutputFileList, executeCipherTool);
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
        FileUtils.deleteDirectory(new File(projectLocation + ConfigMapParserConstants.RESOURCES_PATH));
        File tempFile = new File(projectLocation + ConfigMapParserConstants.RESOURCES_PATH);
        if (!tempFile.mkdirs()) {
            return false;
        }

        try (InputStream inputStream = (new URL(sourceURL).openConnection()).getInputStream()) {
            String templateZipLocation = projectLocation + ConfigMapParserConstants.RESOURCES_PATH + File.separator
                    + ConfigMapParserConstants.TEMPLATES_ZIP_FILE;
            Files.copy(inputStream, Paths.get(templateZipLocation));
            ZipFile zipFile = new ZipFile(templateZipLocation);
            zipFile.extractAll(projectLocation + ConfigMapParserConstants.RESOURCES_PATH);

            File templateZipFile = new File(templateZipLocation);
            if (!templateZipFile.delete()) {
                getLog().warn("Templates zip file can not delete from the resource path");
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
        ConfigParser.ConfigPaths.setPaths(projectLocation + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH,
                templatePath, projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH);
        Map<String,String> configMap = ConfigParser.parse(context);
        Map<String,String> metaDataMap = new HashMap<>();

        //clear the CarbonHome directory
        FileUtils.deleteDirectory(new File(projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH));

        for (Map.Entry<String,String> entry : configMap.entrySet()) {
            String fileSource = entry.getValue();
            String filePath = entry.getKey();
            String fileFromOutput = projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
                    + filePath;

            File tempFile = new File(fileFromOutput);
            tempFile.getParentFile().mkdirs();
            tempFile.createNewFile();
            Files.write(Paths.get(fileFromOutput), fileSource.getBytes());
            metaDataMap.put(filePath, DigestUtils.md5Hex(entry.getValue()));
        }

        //add deployment.toml file md5Hex to the metaDataMap
        String tomlFileSource = new String(Files.readAllBytes(
                Paths.get(projectLocation + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH)),
                StandardCharsets.UTF_8);
        String tomlOutputPath = ConfigMapParserConstants.CONF_DIR + File.separator
                + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH;
        metaDataMap.put(tomlOutputPath, DigestUtils.md5Hex(tomlFileSource));

        //copy deployment.toml file to the CarbonHome/conf directory
        String confDirPath = projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
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
        String templateFilePath = projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
                + ConfigMapParserConstants.METADATA_DIR + File.separator
                + ConfigMapParserConstants.METADATA_CONFIG_PROPERTIES_FILE;

        //create the metadata_config.properties
        File tempFile = new File(templateFilePath);
        if (!tempFile.getParentFile().mkdirs() || !tempFile.createNewFile()) {
            throw new IOException("Creating .metadata directory in " + projectLocation +
                    ConfigMapParserConstants.PARSER_OUTPUT_PATH + " path failed");
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

        //create the references.properties
        String referenceFilePath = projectLocation + ConfigMapParserConstants.PARSER_OUTPUT_PATH + File.separator
                + ConfigMapParserConstants.METADATA_DIR + File.separator
                + ConfigMapParserConstants.REFERENCES_PROPERTIES_FILE;
        File referenceFile = new File(referenceFilePath);
        if (!referenceFile.exists() && !referenceFile.createNewFile()) {
            throw new IOException("Creating " + ConfigMapParserConstants.REFERENCES_PROPERTIES_FILE
                    + " in .metadata directory path failed");
        }
    }

    /**
     * Append the list of parsed files to the Dockerfile.
     *
     * @param parsedOutputFileList list of parsed files
     * @param deploymentHasSecrets boolean flag for secrets existence
     * @throws IOException while writing to the Dockerfile
     */
    private void updateDockerFile(List<String> parsedOutputFileList, boolean deploymentHasSecrets) throws IOException {
        String dockerFileBaseEntries = getBaseImageInDockerfile();
        StringBuilder builder = new StringBuilder(dockerFileBaseEntries);

        builder.append(System.lineSeparator()).append(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_BEGIN)
                .append(System.lineSeparator());

        for (String filePath : parsedOutputFileList) {
            String[] filePathSeparateList = filePath.split(ConfigMapParserConstants.SPLIT_PATTERN);
            StringBuilder innerBuilder = new StringBuilder();
            for (int x = 1; x < filePathSeparateList.length - 1 ; x++) {
                innerBuilder.append(filePathSeparateList[x]).append(ConfigMapParserConstants.PATH_SEPARATOR);
            }
            innerBuilder.append(filePathSeparateList[filePathSeparateList.length - 1]);
            builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).append(filePath.replaceAll(
                    ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR))
                    .append(ConfigMapParserConstants.DOCKER_MI_DIR_PATH)
                    .append(innerBuilder.toString());
            builder.append(System.lineSeparator());
        }

        //copy password-tmp and secret-conf.properties files to the runtime if secrets are defined
        if (deploymentHasSecrets) {
            String secretConfLocalLocation = Paths.get(ConfigMapParserConstants.RESOURCE_DIR_PATH,
                    ConfigMapParserConstants.SECRET_CONF_FILE_NAME).toString()
                    .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR);
            String secretConfRuntimeLocation = Paths.get(ConfigMapParserConstants.DOCKER_MI_DIR_PATH,
                    ConfigMapParserConstants.CONF_DIR, Constants.SECURITY_DIR,
                    ConfigMapParserConstants.SECRET_CONF_FILE_NAME).toString()
                    .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR);
            builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).
                    append(secretConfLocalLocation).append(secretConfRuntimeLocation).append(System.lineSeparator());

            String passwordTmpLocalLocation = Paths.get(ConfigMapParserConstants.RESOURCE_DIR_PATH,
                    ConfigMapParserConstants.PASSWORD_TMP_FILE_NAME).toString()
                    .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR);
            String passwordTmpRuntimeLocation = Paths.get(ConfigMapParserConstants.DOCKER_MI_DIR_PATH,
                    ConfigMapParserConstants.PASSWORD_TMP_FILE_NAME).toString()
                    .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR);
            builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).
                    append(passwordTmpLocalLocation).append(passwordTmpRuntimeLocation).append(System.lineSeparator());
        }

        //copy .metadata folder to the repository/resources/conf directory
        builder.append(ConfigMapParserConstants.DOCKER_MAKE_DIR + ConfigMapParserConstants.METADATA_DIR_PATH);
        builder.append(System.lineSeparator());

        //copy metadata_config.properties file to the .metadata folder
        builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).append(ConfigMapParserConstants.PARSER_OUTPUT_PATH)
                .append(ConfigMapParserConstants.PATH_SEPARATOR).append(ConfigMapParserConstants.METADATA_DIR)
                .append(ConfigMapParserConstants.PATH_SEPARATOR)
                .append(ConfigMapParserConstants.METADATA_CONFIG_PROPERTIES_FILE)
                .append(ConfigMapParserConstants.METADATA_DIR_PATH).append(System.lineSeparator());

        //copy references.properties file to the .metadata folder
        builder.append(ConfigMapParserConstants.DOCKER_COPY_FILE).append(ConfigMapParserConstants.PARSER_OUTPUT_PATH)
                .append(ConfigMapParserConstants.PATH_SEPARATOR).append(ConfigMapParserConstants.METADATA_DIR)
                .append(ConfigMapParserConstants.PATH_SEPARATOR)
                .append(ConfigMapParserConstants.REFERENCES_PROPERTIES_FILE)
                .append(ConfigMapParserConstants.METADATA_DIR_PATH).append(System.lineSeparator());

        builder.append(ConfigMapParserConstants.DOCKER_FILE_AUTO_GENERATION_END).append(System.lineSeparator());
        try (InputStream inputStream = new ByteArrayInputStream(builder.toString()
                .getBytes(StandardCharsets.UTF_8));
             OutputStream outputStream = new FileOutputStream(
                     new File(projectLocation + ConfigMapParserConstants.DOCKER_FILE))) {
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
                if (fileEntry.isDirectory() && !fileEntry.getName().equals(ConfigMapParserConstants.METADATA_DIR)) {
                    listFilesForFolder(fileEntry, outputList);
                } else if (fileEntry.isFile() && !fileEntry.isHidden()) {
                    outputList.add(StringUtils.substringAfter(fileEntry.getPath(), projectLocation));
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
        try (BufferedReader bufferReader = new BufferedReader(
                new FileReader(projectLocation + ConfigMapParserConstants.DOCKER_FILE))) {
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

    /**
     * Get default/initial lines from Dockerfile.
     *
     * @throws IOException exception while reading dockerfile
     */
    private void updateDockerfileAsDefault() throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(getBaseImageInDockerfile()
                .getBytes(StandardCharsets.UTF_8));
             OutputStream outputStream = new FileOutputStream(
                     new File(projectLocation + ConfigMapParserConstants.DOCKER_FILE))) {
            IOUtils.copy(inputStream, outputStream);
            getLog().warn("Updated Dockerfile to default");
        } catch (IOException e) {
            throw new IOException("Exception while making the Dockerfile to default \n" + e);
        }
    }


    /**
     * Read deployment.toml file and return list of secrets
     *
     * @param configFilePath file path to deployment toml
     * @return Map of secrets
     */
    public Map<String, String> getSecretsFromConfiguration(String configFilePath) throws IOException {

        Map<String, String> context = new LinkedHashMap<>();

        TomlParseResult result = Toml.parse(Paths.get(configFilePath));
        TomlTable table = result.getTable(ConfigMapParserConstants.SECRET_PROPERTY_MAP_NAME);
        if (table != null) {
            table.dottedKeySet().forEach(key -> context.put(key, table.getString(key)));
        }
        return context;
    }

    /**
     * Sets system properties required by the cipherTool.
     *
     * @throws MojoExecutionException
     */
    private void initializeSystemProperties() throws MojoExecutionException {

           if (isKeystoreParametersAvailable()) {

               // Setting the property to be referred by the cipherTool to override inherent system properties
               System.setProperty(Constants.SET_EXTERNAL_SYSTEM_PROPERTY, Boolean.TRUE.toString());

               String keystoreLocation = Paths.get(projectLocation + ConfigMapParserConstants.RESOURCE_DIR_PATH,
                       keystoreName).toString();
               String secretConfFile = Paths.get(projectLocation + ConfigMapParserConstants.RESOURCE_DIR_PATH,
                       Constants.SECRET_PROPERTY_FILE).toString();
               File keystore = new File(keystoreLocation);
               if (keystore.exists()) {
                   keystoreLocation = keystore.getAbsolutePath();
               } else {
                   throw new MojoExecutionException("Keystore file is not available in " + keystoreLocation);
               }
               String cipherTextPropFile =  Constants.CONF_DIR + ConfigMapParserConstants.PATH_SEPARATOR
                       + Constants.SECURITY_DIR + ConfigMapParserConstants.PATH_SEPARATOR
                       + Constants.CIPHER_TEXT_PROPERTY_FILE;

               System.setProperty(ConfigMapParserConstants.KEY_LOCATION_PROPERTY, keystoreLocation
                       .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR));
               System.setProperty(ConfigMapParserConstants.KEY_ALIAS_PROPERTY, keystoreAlias);
               System.setProperty(ConfigMapParserConstants.KEY_TYPE_PROPERTY, keystoreType);
               System.setProperty(ConfigMapParserConstants.KEYSTORE_PASSWORD, keystorePassword);

               System.setProperty(ConfigMapParserConstants.DEPLOYMENT_CONFIG_FILE_PATH,
                       projectLocation + ConfigMapParserConstants.DEPLOYMENT_TOML_PATH);
               System.setProperty(ConfigMapParserConstants.SECRET_PROPERTY_FILE_PROPERTY, secretConfFile
                       .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR));
               System.setProperty(ConfigMapParserConstants.SECRET_FILE_LOCATION, cipherTextPropFile);
           } else {
               throw new MojoExecutionException("Keystore parameters have not been defined in pom.xml");
           }
    }

    /**
     * Checks if keystore parameters have been defined in pom.xml.
     *
     * @return boolean
     */
    private boolean isKeystoreParametersAvailable() {
        return (keystoreName != null && keystoreAlias != null && keystorePassword != null && keystoreType != null);
    }

    /**
     * Update the secret-conf.properties with keystore location.
     *
     * @throws MojoExecutionException
     */
    private void updateSecretConf() throws MojoExecutionException {

        String secretConfLocation = Paths.get(projectLocation + ConfigMapParserConstants.RESOURCE_DIR_PATH,
                ConfigMapParserConstants.SECRET_CONF_FILE_NAME).toString();
        Properties secretConfProperties = new Properties();
        try (InputStream inputStream = new FileInputStream(secretConfLocation)) {
            secretConfProperties.load(inputStream);

            // default location is ./repository/resources/security directory
            String defaultKeystoreLocation = Paths
                    .get(".", "repository", "resources", Constants.SECURITY_DIR, keystoreName).toString()
                    .replaceAll(ConfigMapParserConstants.SPLIT_PATTERN, ConfigMapParserConstants.PATH_SEPARATOR);
            secretConfProperties
                    .setProperty(ConfigMapParserConstants.SECRET_CONF_KEYSTORE_LOCATION_PROPERTY, defaultKeystoreLocation);

            // write back the properties file
            OutputStream outputStream = new FileOutputStream(secretConfLocation);
            secretConfProperties.store(outputStream, null);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while modifying secret-conf.properties file in " + secretConfLocation);
        }
    }

    /**
     * Creates password-tmp file for startup.
     *
     * @throws MojoExecutionException
     */
    private void createPasswordTmpFile() throws MojoExecutionException {

        String passwordTempFileLocation = Paths.get(projectLocation + ConfigMapParserConstants.RESOURCE_DIR_PATH,
                ConfigMapParserConstants.PASSWORD_TMP_FILE_NAME).toString();
        try (FileWriter fileWriter = new FileWriter(passwordTempFileLocation)) {
            fileWriter.write(keystorePassword);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while creating " + passwordTempFileLocation);
        }
    }
}
