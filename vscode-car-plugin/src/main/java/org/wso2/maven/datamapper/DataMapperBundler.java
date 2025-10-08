/*
 * Copyright (c) 2024, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.datamapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.wso2.maven.CARMojo;

import static org.wso2.maven.MavenUtils.setupInvoker;

public class DataMapperBundler {
    private final CARMojo mojoInstance;
    private final String sourceDirectory;
    private final String resourcesDirectory;
    private final String projectDirectory;
    private final Invoker invoker;

    public DataMapperBundler(CARMojo mojoInstance, String projectDirectory, String sourceDirectory,
                             String resourcesDirectory) {
        this.mojoInstance = mojoInstance;
        this.sourceDirectory = sourceDirectory;
        this.resourcesDirectory = resourcesDirectory;
        this.projectDirectory = projectDirectory;
        this.invoker = new DefaultInvoker();
    }

    /**
     * Orchestrates the process of bundling all data mappers found within the specified resources directory.
     * This includes setting up the Maven invoker, installing Node.js and NPM, running npm install, and
     * executing the bundling process for each data mapper.
     * It also handles the creation and cleanup of necessary artifacts.
     *
     * @throws DataMapperException if any step in the bundling process fails.
     * @throws MojoExecutionException if an error occurs while executing the Maven invoker.
     */
    public void bundleDataMapper() throws DataMapperException, MojoExecutionException {
        String oldDataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_PATH;
        String newDataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_NAME;
        String dataMappersCachePath = getDataMappersCachePath().toString();
        List<Path> dataMappers = listSubDirectories(oldDataMapperDirectoryPath,false);
        dataMappers.addAll(listSubDirectories(newDataMapperDirectoryPath,false));
        List<Path> dataMappersCache = listSubDirectories(dataMappersCachePath,true);

        if (dataMappers.isEmpty()) {
            // No data mappers to bundle
            return;
        }

        List<Path> nonCachedDataMappers = getNonCacheDataMappers(dataMappers, dataMappersCache);

        if (nonCachedDataMappers.isEmpty()) {
            copyDataMapperFilesToTarget();
            mojoInstance.logInfo("All data mappers are cached, skipping bundling.");
            return; // All data mappers are cached, no need to bundle
        }
    
        appendDataMapperLogs();
        setupInvoker(invoker, projectDirectory);

        if (!isDmResourcesExist()) {
            mojoInstance.logInfo("Could not find the resources needed for data mapper bundling. " + "Starting the resources creation process.");
            createDataMapperArtifacts();
            installNodeAndNPM();
        }else{
            mojoInstance.logInfo("Resources for data mapper bundling found. Skipping the resources creation process.");
        }

        runNpmInstall();
        configureNpm();
        bundleDataMappers(nonCachedDataMappers);
        generateDataMapperSchemas(nonCachedDataMappers);
        copyDataMapperFilesToTarget();
        copyDataMappersToCache();
    }

    /**
     * Checks if all necessary resources for data mapper bundling exist in the global cache directory.
     *
     * @return true if all necessary resources exist, false otherwise.
     */
    public boolean isDmResourcesExist() {
        
        Path globalCacheDir = getDataMapperBundlingCachePath();

        return Files.exists(globalCacheDir) &&
               Files.exists(globalCacheDir.resolve(Constants.DATA_MAPPER_CACHE_NODE_MODULES)) &&
               Files.exists(globalCacheDir.resolve(Constants.DATA_MAPPER_CACHE_NODE)) &&
               Files.exists(globalCacheDir.resolve(Constants.POM_FILE_NAME)) &&
               Files.exists(globalCacheDir.resolve(Constants.PACKAGE_JSON_FILE_NAME)) &&
               Files.exists(globalCacheDir.resolve(Constants.PACKAGE_LOCK_JSON)) &&
               Files.exists(globalCacheDir.resolve(Constants.SCHEMA_GENERATOR));
    }

    /**
     * Deletes the generated data mapper artifacts.
     *
     * @throws DataMapperException if an error occurs while deleting the generated data mapper artifacts.
     */
    public void deleteGeneratedDatamapperArtifacts() throws DataMapperException {
        String olDataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_PATH;
        String newDataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_NAME;
        List<Path> dataMappers = listSubDirectories(olDataMapperDirectoryPath,false);
        dataMappers.addAll(listSubDirectories(newDataMapperDirectoryPath, false));

        if (dataMappers.isEmpty()) {
            // No data mappers to delete
            return;
        }

        for (Path dataMapper : dataMappers) {
            String dataMapperName = dataMapper.getFileName().toString();
            Path tsFilePath = dataMapper.resolve(dataMapperName + ".ts");
            if (Files.notExists(tsFilePath)) {
                return;
            }
            Path bundledJsFilePath = Paths.get(dataMapper + File.separator + dataMapperName + ".dmc");
            Path inputSchemaFilePath = Paths.get(dataMapper + File.separator + dataMapperName + "_inputSchema.json");
            Path outputSchemaFilePath = Paths.get(dataMapper + File.separator + dataMapperName + "_outputSchema.json");
            try {
                Files.deleteIfExists(bundledJsFilePath);
                Files.deleteIfExists(inputSchemaFilePath);
                Files.deleteIfExists(outputSchemaFilePath);
            } catch (IOException e) {
                throw new DataMapperException("Failed to delete generated data mapper artifacts.", e);
            }
        }
    }
    
    /**
     * Installs Node.js and NPM using Maven.
     *
     * @throws DataMapperException if the Maven invocation fails.
     */
    private void installNodeAndNPM() throws DataMapperException {
        InvocationRequest request = createBaseRequest();
        mojoInstance.logInfo("Installing Node and NPM");
        request.setBaseDirectory(Paths.get(projectDirectory).toFile());
        request.setGoals(Collections.singletonList(Constants.INSTALL_NODE_AND_NPM_GOAL + " -DinstallDirectory=" + getDataMapperBundlingCachePath()));
        setNodeAndNpmProperties(request);
    
        executeRequest(request, "Node and NPM installation failed.");
    }
    
    /**
     * Runs 'npm install' to install dependencies.
     *
     * @throws DataMapperException if the Maven invocation fails.
     */
     private void runNpmInstall() throws DataMapperException {
        InvocationRequest request = createBaseRequest();
        mojoInstance.logInfo("Running npm install");
        request.setBaseDirectory(Paths.get(projectDirectory).toFile());
        request.setGoals(Collections.singletonList(Constants.NPM_GOAL));
        setNpmInstallProperties(request);
    
        executeRequest(request, "npm install failed.");
    }

    /**
     * Runs 'npm config set scripts-prepend-node-path auto' to configure node for npm.
     *
     * @throws DataMapperException
     */
    private void configureNpm() throws DataMapperException {

        InvocationRequest request = createBaseRequest();
        mojoInstance.logInfo("Configuring npm");
        request.setBaseDirectory(Paths.get(projectDirectory).toFile());
        request.setGoals(Collections.singletonList(Constants.NPM_GOAL));

        Properties properties = new Properties();
        properties.setProperty("arguments", Constants.PREPEND_NODE_CONFIG);
        properties.setProperty("workingDirectory", getDataMapperBundlingCachePath().toString());
        request.setProperties(properties);
        executeRequest(request, "npm configuration failed.");
    }
    
    /**
     * Iterates over each data mapper directory provided in the list and attempts to bundle them individually.
     * If any bundling fails, it stops further processing.
     *
     * @param dataMappers List of paths to data mapper directories.
     * @throws DataMapperException if the bundling process for any data mapper fails.
     */
    private void bundleDataMappers(List<Path> dataMappers) throws DataMapperException {
        createConfigJson();
        for (Path dataMapper : dataMappers) {
            if (!bundleSingleDataMapper(dataMapper)) {
                return;
            }
        }
        mojoInstance.logInfo("Data mapper bundling completed successfully");
    }

    /**
     * Iterates over each data mapper directory provided in the list and generates the input and output
     * schema for each data mapper.
     */
    private void generateDataMapperSchemas(List<Path> dataMappers) throws DataMapperException {
        createConfigJsonForSchemaGeneration();
        for (Path dataMapper : dataMappers) {
            generateDataMapperSchema(dataMapper.toAbsolutePath());
        }
    }

    /**
     * Copies the data mapper files to the target directory.
     * This might be utilized for the unit tests
     *
     * @throws DataMapperException if an error occurs while removing the bundling artifacts.
     */
    private void copyDataMapperFilesToTarget() throws DataMapperException {
        Path oldDataMapperPath = Paths.get(resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_PATH);
        Path newDataMapperPath = Paths.get(resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_NAME);
        try {
            if (Files.exists(oldDataMapperPath)) {
                FileUtils.copyDirectory(oldDataMapperPath.toFile(),
                        Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator +
                                Constants.DATA_MAPPER_DIR_NAME).toFile());
            }
            if (Files.exists(newDataMapperPath)) {
                FileUtils.copyDirectory(newDataMapperPath.toFile(),
                        Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator +
                                Constants.DATA_MAPPER_DIR_NAME).toFile());
            }
        } catch (IOException e) {
            throw new DataMapperException("Failed to copy data mapper files to target directory.", e);
        }
    }

    /**
     * Handles the bundling process for a single data mapper.
     *
     * @param dataMapper The path to the data mapper directory.
     * @return true if the bundling is successful, false otherwise.
     * @throws DataMapperException if any step in the bundling process fails.
     */
    private boolean bundleSingleDataMapper(Path dataMapper) throws DataMapperException {
        cleanUpBundlingResources();
        copyTsFiles(dataMapper);
        String dataMapperName = dataMapper.getFileName().toString();
        mojoInstance.logInfo("Bundling data mapper: " + dataMapperName);
        createWebpackConfig(dataMapperName);
        Path cacheSrcDir = getDataMapperBundlingCachePath().resolve(Constants.SRC_DIR);
        Path originalTsFile = cacheSrcDir.resolve(dataMapperName + ".ts");
        if (!Files.exists(originalTsFile)) {
            throw new DataMapperException("TypeScript file not found: " + originalTsFile);
        }
        Path backupOriginalFile = null;
        try {
            String content = Files.readString(originalTsFile);

            // Replace dmUtils.getPropertyValue(...) with DM_PROPERTIES.<SCOPE>['<key>']
            Pattern pattern = Pattern.compile(
                    "dmUtils\\s*\\.\\s*getPropertyValue\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\)",
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(content);
            StringBuffer updatedContent = new StringBuffer();
            while (matcher.find()) {
                String scope = matcher.group(1).trim().toUpperCase();
                String key = matcher.group(2).trim();
                String replacement = "DM_PROPERTIES." + scope + "['" + key + "']";
                matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(updatedContent);

            // Backup the original .ts file
            backupOriginalFile = Files.createTempFile("dm_backup_", "_" + dataMapperName + ".ts");
            Files.copy(originalTsFile, backupOriginalFile, StandardCopyOption.REPLACE_EXISTING);

            // Write the modified version to the cached original path
            Files.writeString(originalTsFile, updatedContent.toString(), StandardOpenOption.TRUNCATE_EXISTING);
            mojoInstance.logInfo("Temporary modifications applied to: " + originalTsFile);

            InvocationRequest request = createBaseRequest();
            Path globalCacheDir = getDataMapperBundlingCachePath();
            Path pomPath = globalCacheDir.resolve(Constants.POM_FILE_NAME);
            request.setBaseDirectory(Paths.get(projectDirectory).toFile());
            request.setGoals(Collections.singletonList(Constants.NPM_RUN_BUILD_GOAL + " -f " + pomPath
                            + " -Dexec.executable=\"" + getNpmExecutablePath() + "\""
                            + " -Dexec.args=\"" + Constants.RUN_BUILD + " " + Constants.PREPEND_NODE_CONFIG_FLAG + "\""));

            executeRequest(request, "Failed to bundle data mapper: " + dataMapperName);

            mojoInstance.logInfo("Bundle completed for data mapper: " + dataMapperName);
            Path bundledJsFilePath = cacheSrcDir.resolve(dataMapperName + ".dmc");
            appendMapFunction(dataMapper.toString(), dataMapperName, bundledJsFilePath.toString());
            copyGenerateDataMapperFile(bundledJsFilePath.toString(), dataMapper);

        } catch (Exception e) {
            throw new DataMapperException("Failed to process TypeScript file: " + dataMapperName, e);
        } finally {
            if (backupOriginalFile != null && Files.exists(backupOriginalFile)) {
                try {
                    Files.copy(backupOriginalFile, originalTsFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(backupOriginalFile);
                    mojoInstance.logInfo("Restored original file: " + originalTsFile);
                } catch (IOException ex) {
                    mojoInstance.logWarn("Failed to restore original TypeScript file: " + originalTsFile);
                }
            }
            cleanUpBundlingResources();
            removeWebpackConfig();
        }
        return true;
    }

    /**
     * Generates the input and output schema for a single data mapper.
     *
     * @param dataMapper The path to the data mapper directory.
     */
    private void generateDataMapperSchema(Path dataMapper) throws DataMapperException {
        String dataMapperName = dataMapper.getFileName().toString();
        mojoInstance.logInfo("Generating schema for data mapper: " + dataMapperName);
        InvocationRequest request = createBaseRequest();
        Path globalCacheDir = getDataMapperBundlingCachePath();
        Path pomPath = globalCacheDir.resolve(Constants.POM_FILE_NAME);
        request.setBaseDirectory(Paths.get(projectDirectory).toFile());
        request.setGoals(Collections.singletonList(Constants.NPM_RUN_BUILD_GOAL + " -f " + pomPath
                + " -Dexec.executable=\"" + getNpmExecutablePath() + "\""
                + " -Dexec.args=\"" + Constants.RUN_GENERATE + " " + dataMapper + File.separator
                + dataMapperName + ".ts" + " " + Constants.PREPEND_NODE_CONFIG_FLAG + "\""));

        executeRequest(request, "Failed to bundle data mapper: " + dataMapperName);
    }

    private String getNpmExecutablePath() {

        String osName = System.getProperty("os.name").toLowerCase();
        String npmExecutable = osName.contains("win") ? "npm.cmd" : "npm";
        return getDataMapperBundlingCachePath().resolve(Constants.DATA_MAPPER_CACHE_NODE).resolve(npmExecutable).toString();
    }
    
    /**
     * Creates a base Maven invocation request.
     *
     * @return The configured invocation request.
     */
    private InvocationRequest createBaseRequest() {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setInputStream(new ByteArrayInputStream(new byte[0])); // Avoid interactive mode
        return request;
    }
    
    /**
     * Sets properties for Node.js and NPM installation.
     *
     * @param request The Maven invocation request to configure.
     */
    private void setNodeAndNpmProperties(InvocationRequest request) {
        Properties properties = new Properties();
        properties.setProperty("nodeVersion", Constants.NODE_VERSION);
        properties.setProperty("npmVersion", Constants.NPM_VERSION);
        request.setProperties(properties);
    }
    
    /**
     * Sets properties for the 'npm install' command.
     *
     * @param request The Maven invocation request to configure.
     */
    private void setNpmInstallProperties(InvocationRequest request) {
        Properties properties = new Properties();
        properties.setProperty("arguments", Constants.NPM_INSTALL);
        properties.setProperty("workingDirectory", getDataMapperBundlingCachePath().toString());
        request.setProperties(properties);
    }
    
    /**
     * Executes a Maven invocation request and logs any errors.
     *
     * @param request The Maven invocation request to execute.
     * @param errorMessage The error message to log if the execution fails.
     * @throws DataMapperException if the execution encounters an exception.
     */
    private void executeRequest(InvocationRequest request, String errorMessage) throws DataMapperException{
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                mojoInstance.logError(errorMessage);
                if (result.getExecutionException() != null) {
                    mojoInstance.logError(result.getExecutionException().getMessage());
                }
                throw new DataMapperException(errorMessage);
            }
        } catch (MavenInvocationException e) {
            throw new DataMapperException(errorMessage, e);
        }
    }

    /**
     * Creates necessary artifacts for data mapper bundling.
     * 
     * @throws DataMapperException if an error occurs while creating the artifacts.
     */
    private void createDataMapperArtifacts() throws DataMapperException {
        mojoInstance.logInfo("Creating data mapper artifacts");
        ensureDataMapperBundlingCacheExists();
        createPomFile();
        createPackageJson();
        createPackageLockJson();
        createSchemaGenerator();
    }

    /**
     * Logs the start of the data mapper bundling process.
     */
    private void appendDataMapperLogs() {
        mojoInstance.getLog().info("------------------------------------------------------------------------");
        mojoInstance.getLog().info("Bundling Data Mapper");
        mojoInstance.getLog().info("------------------------------------------------------------------------");
    }

    /**
     * Creates a Maven POM file for the data mapper project.
     * @throws DataMapperException if an error occurs while creating the POM file.
     */
    private void createPomFile() throws DataMapperException {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.example</groupId>\n" +
            "    <artifactId>data-mapper-bundler</artifactId>\n" +
            "    <version>1.0-SNAPSHOT</version>\n" +
            "</project>";
    
        Path pomPath = getDataMapperBundlingCachePath().resolve(Constants.POM_FILE_NAME);
        try {
            Files.write(pomPath, pomContent.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new DataMapperException("Failed to create pom.xml file.", e);
        }
    }

    /**
     * Copy schemaGenerator.ts from resources to target directory.
     */
    public void createSchemaGenerator() throws DataMapperException {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Constants.SCHEMA_GENERATOR);
            String targetPath = getDataMapperBundlingCachePath().resolve(Constants.SCHEMA_GENERATOR).toString();
            File targetFile = new File(targetPath);
            FileUtils.copyInputStreamToFile(inputStream, targetFile);
        } catch (IOException e) {
            throw new DataMapperException("Failed to read schemaGenerator.ts file.", e);
        }
    }

    /**
     * Copy package-lock.json from resources to the target directory.
     */
    private void createPackageLockJson() throws DataMapperException {

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Constants.PACKAGE_LOCK_JSON);
            String targetPath = getDataMapperBundlingCachePath().resolve(Constants.PACKAGE_LOCK_JSON).toString();
            File targetFile = new File(targetPath);
            FileUtils.copyInputStreamToFile(inputStream, targetFile);
        } catch (IOException e) {
            throw new DataMapperException("Failed to read package-lock.json file.", e);
        }
    }

    /**
     * Creates a package.json file for managing npm packages.
     * @throws DataMapperException if an error occurs while creating the package.json file.
     */
    private void createPackageJson() throws DataMapperException {
        String packageJsonContent = "{\n" +
                "    \"name\": \"data-mapper-bundler\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"scripts\": {\n" +
                "        \"build\": \"tsc && webpack\",\n" +
                "        \"generate\": \" tsc -p . && node schemaGenerator.js \"\n" +
                "    },\n" +
                "    \"devDependencies\": {\n" +
                "        \"typescript\": \"^4.4.2\",\n" +
                "        \"webpack\": \"^5.52.0\",\n" +
                "        \"webpack-cli\": \"^4.8.0\",\n" +
                "        \"ts-loader\": \"^9.2.3\"\n" +
                "    }\n" +
                "}";

        Path packageJsonPath = getDataMapperBundlingCachePath().resolve(Constants.PACKAGE_JSON_FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(packageJsonPath.toFile())) {
            fileWriter.write(packageJsonContent);
        }
        catch (IOException e) {
            throw new DataMapperException("Failed to create package.json file.", e);
        }
    }

    /**
     * Creates a TypeScript configuration file (tsconfig.json).
     * @throws DataMapperException if an error occurs while creating the tsconfig.json file.
     */
    private void createConfigJson() throws DataMapperException {
        String tsConfigContent = "{\n" +
                "    \"compilerOptions\": {\n" +
                "        \"outDir\": \"./target\",\n" +
                "        \"module\": \"commonjs\",\n" +
                "        \"target\": \"es5\",\n" +
                "        \"sourceMap\": true\n" +
                "    },\n" +
                "    \"include\": [\n" +
                "        \"./src/**/*\"\n" +
                "    ]\n" +
                "}";
    
        Path tsConfigPath = getDataMapperBundlingCachePath().resolve(Constants.TS_CONFIG_FILE_NAME);
            try (FileWriter fileWriter = new FileWriter(tsConfigPath.toFile())) {
                fileWriter.write(tsConfigContent);
            } catch (IOException e) {
                throw new DataMapperException("Failed to create tsconfig.json file.", e);
            }
        }

    /**
     * Creates a TypeScript configuration file (tsconfig.json).
     * @throws DataMapperException if an error occurs while creating the tsconfig.json file.
     */
    private void createConfigJsonForSchemaGeneration() throws DataMapperException {
        String tsConfigContent = "{\n" +
                "    \"include\": [     \"schemaGenerator.ts\" ]," +
                "    \"exclude\": [     \"node_modules\" ]," +
                "    \"compilerOptions\": {\n" +
                "        \"module\": \"commonjs\",\n" +
                "        \"target\": \"es5\",\n" +
                "        \"sourceMap\": true,\n" +
                "        \"esModuleInterop\": true\n" +
                "    }\n" +
                "}";

        Path tsConfigPath = getDataMapperBundlingCachePath().resolve(Constants.TS_CONFIG_FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(tsConfigPath.toFile())) {
            fileWriter.write(tsConfigContent);
        } catch (IOException e) {
            throw new DataMapperException("Failed to create tsconfig.json file.", e);
        }
    }

    /**
     * Creates a webpack configuration file for the data mapper.
     *
     * @param dataMapperName The name of the data mapper.
     * @throws DataMapperException if an error occurs while creating the webpack.config.js file.
     */
    private void createWebpackConfig(String dataMapperName) throws DataMapperException {
        String webPackConfigContent = "const path = require(\"path\");\n" +
                "module.exports = {\n" +
                "    entry: \"./src/" + dataMapperName + ".ts\",\n" +
                "    module: {\n" +
                "        rules: [\n" +
                "            {\n" +
                "                test: /\\.tsx?$/,\n" +
                "                use: \"ts-loader\",\n" +
                "                exclude: /node_modules/,\n" +
                "            }\n" +
                "        ],\n" +
                "    },\n" +
                "    resolve: {\n" +
                "        extensions: [\".ts\", \".js\"],\n" +
                "    },\n" +
                "    output: {\n" +
                "        filename: \"./src/" + dataMapperName + ".dmc\",\n" +
                "        path: path.resolve(__dirname),\n" +
                "        iife: false,\n" +
                "        library: 'DataMapper', \n" +
                "        libraryTarget: 'var',\n" +
                "    },\n" +
                "    mode: \"production\",\n" +
                "};";
    
        Path webpackConfigPath = getDataMapperBundlingCachePath().resolve(Constants.WEBPACK_CONFIG_FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(webpackConfigPath.toFile())) {
            fileWriter.write(webPackConfigContent);
        } catch (IOException e) {
            throw new DataMapperException("Failed to create webpack.config.js file.", e);
        }
    }

    /**
     * Appends the function to call the mapFunction inside webpack bundled file.
     * @param tsFolder The path to the data mapper directory.
     * @param datamapperName The name of the data mapper.
     * @param dmcPath The path to the data mapper configuration file.
     */
    private void appendMapFunction(String tsFolder, String datamapperName, String dmcPath) {
        String mapFunction = generateMapFunction(tsFolder + File.separator + datamapperName + ".ts");
        try (FileWriter fileWriter = new FileWriter(dmcPath, true)) {
            fileWriter.write(mapFunction);
        } catch (IOException e) {
            mojoInstance.logError("Failed to append map function to the bundled js file.");
        }
    }

    /**
    * Generates the map function to be appended to the bundled js file.
    * @param tsPath The path to the data mapper configuration file.
    * @return The generated map function.
    */
    private String generateMapFunction(String tsPath) {

        String functionName = "";
        String inputVariable = "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(tsPath))))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("*") && line.contains("functionName") && line.contains("map_")) {
                    functionName = line.split(":")[1].trim();
                }
                if (line.startsWith("*") && line.contains("inputVariable")) {
                    inputVariable = line.split(":")[1].trim();
                }
            }
        } catch (IOException e) {
            mojoInstance.logError("Failed to read the file.");
        }

        String mapFunction = "\n\nfunction mapFunction(input) {\n" +
                "    return DataMapper.mapFunction(input);\n" +
                "}\n\n";

        if (!functionName.isEmpty() && !inputVariable.isEmpty()) {
            mapFunction += "function " + functionName + "() {\n" +
                    "    return mapFunction(" + inputVariable + ");\n" +
                    "}\n";
        }
        return mapFunction;
    }

    /**
     * Lists subdirectories in a given directory path.
     *
     * @param directory The directory path to list subdirectories from.
     * @return A list of paths representing subdirectories.
     * @throws DataMapperException if an error occurs while listing the subdirectories.
     */
    private List<Path> listSubDirectories(String directory, boolean isCached) throws DataMapperException {
        Path dirPath = Paths.get(directory);
        List<Path> subDirectories = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.equals(dirPath) && isDataMapperDirectory(path)) {
                    subDirectories.add(path);
                }
            }
        } catch (NoSuchFileException e) {
            String logMessage = isCached ? "data mapper cache directory not found" : "datamapper directory not found";
            mojoInstance.logInfo(logMessage);
        } catch (IOException e) {
            throw new DataMapperException("Failed to find data mapper directories.", e);
        }
        return subDirectories;
    }

    private boolean isDataMapperDirectory(Path path) {

        String dirName = path.getFileName().toString();
        return Files.exists(Paths.get(path.toString(), dirName + ".ts"));
    }

    /**
     * Copies TypeScript files from the source directory to the cache/src directory.
     *
     * @param sourceDir The source directory containing TypeScript files.
     * @throws DataMapperException if an error occurs while copying the TypeScript files.
     */
    private void copyTsFiles(final Path sourceDir) throws DataMapperException {
        final Path destDir = getDataMapperBundlingCachePath().resolve(Constants.SRC_DIR);

        try {
            Files.createDirectories(destDir);
            final List<Path> fileList = new ArrayList<>();
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".ts")) {
                        fileList.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path sourcePath : fileList) {
                Path destPath = destDir.resolve(sourceDir.relativize(sourcePath));
                try {
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new DataMapperException("Failed to copy data mapper file: " + sourcePath, e);
                }
            }
        } catch (IOException e) {
            throw new DataMapperException("Failed to copy data mapper files.", e);
        }
    }

    /**
     * Writes the schema content to the relevant registry location.
     *
     * @param content The content to write to the file.
     * @param path The path of the file to write to.
     * @throws DataMapperException if an error occurs while writing to the file.
     */
    private void writeSchemaToFile(String content, String path) throws DataMapperException {
        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write(content);
        } catch (IOException e) {
            throw new DataMapperException("Failed to create file: " + path, e);
        }
    }

    /**
     * Copies the bundled JavaScript file and schema to the specified destination directory.
     *
     * @param sourceFile The source file path of the bundled JavaScript file/json schema file.
     * @param destinationDir The destination directory to copy the file to.
     * @throws DataMapperException if an error occurs while copying the bundled JavaScript file.
     */
    private void copyGenerateDataMapperFile(String sourceFile, Path destinationDir) throws DataMapperException {
        mojoInstance.logInfo("Copying bundled js file to registry");
        Path sourcePath = Paths.get(sourceFile);
        Path destPath = destinationDir.resolve(sourcePath.getFileName());

        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataMapperException("Failed to copy bundled js file to registry.", e);
        }
    }

    /**
     * Ensures that the data mapper bundling cache directory exists.
     * @throws DataMapperException if an error occurs while creating the data-mapper artifacts directory.
     */
    private void ensureDataMapperBundlingCacheExists() throws DataMapperException {
        Path dataMapperPath = getDataMapperBundlingCachePath();
        if (!Files.exists(dataMapperPath)) {
            try {
                Files.createDirectories(dataMapperPath);
            } catch (IOException e) {
                throw new DataMapperException("Failed to create data-mapper artifacts directory: " + dataMapperPath, e);
            }
        }
    }

    /**
     * Removes the webpack configuration file.
     * @throws DataMapperException if an error occurs while removing the webpack configuration file.
     */
    private void removeWebpackConfig() throws DataMapperException {
        Path filePath = getDataMapperBundlingCachePath().resolve(Constants.WEBPACK_CONFIG_FILE_NAME);
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new DataMapperException("Error while removing webpack.config.js file.", e);
        }
    }

    private boolean isInsideSourceDirectory(File file) {
        Path sourcePath = Paths.get(sourceDirectory).toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();

        return filePath.startsWith(sourcePath);
    }

    /**
     * Checks if all .ts files in the data mapper folder are present and identical in the cached data mapper folder.
     *
     * @param dataMapperFolder The path to the original data mapper folder.
     * @param cachedDataMapperFolder The path to the cached data mapper folder.
     * @return true if all .ts files are present and identical, false otherwise.
     * @throws DataMapperException if an error occurs while accessing the files or calculating checksums.
     */
    private boolean checkAllTsFilesCached(Path dataMapperFolder, Path cachedDataMapperFolder) throws DataMapperException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataMapperFolder, "*.ts")) {
            for (Path tsFile : stream) {
                Path cachedTsFile = cachedDataMapperFolder.resolve(tsFile.getFileName());
                if (!Files.exists(cachedTsFile) || !compareTwoChecksums(tsFile, cachedTsFile)) {
                    return false;
                }
            }
        } catch (IOException e) {
            throw new DataMapperException("Failed to check .ts files in cache.", e);
        }
        return true;
    }

    /**
     * Compares the checksums of two files to determine if they are identical.
     *
     * @param filePath1 The path to the first file.
     * @param filePath2 The path to the second file.
     * @return true if the checksums are identical, false otherwise.
     * @throws DataMapperException if an error occurs during checksum calculation.
     */
    private boolean compareTwoChecksums(Path filePath1, Path filePath2) throws DataMapperException {
        String checksum1 = Utils.getFileChecksum(filePath1);
        String checksum2 = Utils.getFileChecksum(filePath2);
        return checksum1.equals(checksum2);
    }

    /**
     * Compares the provided data mappers with the cached data mappers and returns a list of data mappers
     * that are not present in the cache or have changed (based on checksum comparison).
     * If a cached data mapper matches, it is restored to the resources directory.
     *
     * @param dataMappers List of data mapper directories to check.
     * @param cachedDataMappers List of cached data mapper directories.
     * @return List of data mappers that are not cached or have changed.
     * @throws DataMapperException if an error occurs during checksum comparison or restoration.
     */
    private List<Path> getNonCacheDataMappers(List<Path> dataMappers, List<Path> cachedDataMappers) throws DataMapperException {
        List<Path> nonCacheDataMappers = new ArrayList<>();
        if (cachedDataMappers.isEmpty()) {
            return dataMappers;
        }
        for (Path dataMapper : dataMappers) {
            String dataMapperName = dataMapper.getFileName().toString();
            boolean isCached = false;

            for (Path cachedDataMapper : cachedDataMappers) {
                if (cachedDataMapper.getFileName().toString().equals(dataMapperName)) {
                    if (checkAllTsFilesCached(dataMapper, cachedDataMapper)) {
                        restoreDataMapperToResourcesFromCache(dataMapper, cachedDataMapper);
                        isCached = true;
                        break;
                    }
                }
            }
            if (!isCached) {
                nonCacheDataMappers.add(dataMapper);
            }
        }
        return nonCacheDataMappers;
    }
            
    /**
     * Returns the path to the user's data mappers cache directory for this project.
     *
     * @return The path to the data mappers cache directory.
     */
    private Path getDataMappersCachePath() {
        String projectId = new File(sourceDirectory).getName() + "_" + Utils.getHash(sourceDirectory);
        return Path.of(System.getProperty(Constants.USER_HOME), Constants.WSO2_MI, Constants.DATA_MAPPER,
                Constants.DATA_MAPPERS_CACHE_DIR, projectId);
    }

    /**
     * Returns the path to the global data mapper bundling cache directory.
     *
     * @return The path to the data mapper bundling cache directory.
     */
    private Path getDataMapperBundlingCachePath() {
        return Path.of(System.getProperty(Constants.USER_HOME), Constants.WSO2_MI, Constants.DATA_MAPPER, Constants.DATA_MAPPER_BUNDLING_CACHE_DIR);
    }

    /**
     * Restores a cached data mapper files(except .ts files) to the resources directory of the project.
     * @param cachedDataMapperPath The path to the cached data mapper directory.
     * @throws DataMapperException if an error occurs while restoring the data mapper.
     */
    private void restoreDataMapperToResourcesFromCache(Path dataMapper, Path cachedDataMapperPath) throws DataMapperException {
        try {
            if (Files.notExists(dataMapper)) {
                Files.createDirectories(dataMapper);
            }
            // Exclude .ts files
            FileFilter excludeTsFiles = file -> !file.getName().endsWith(".ts");
            FileUtils.copyDirectory(
                cachedDataMapperPath.toFile(),
                dataMapper.toFile(),
                excludeTsFiles
            );
            mojoInstance.getLog().info("Data mapper : " + cachedDataMapperPath.getFileName() + " restored from cache to resources directory");
        } catch (IOException e) {
            throw new DataMapperException("Failed to restore data mapper from cache to resources directory.", e);
        }
    }

    /**
     * Clears the project's data mappers cache directory and copies all data mapper directories
     * from the target directory to the cache directory. Ensures the cache directory exists and is empty before copying.
     *
     * @throws DataMapperException if an error occurs while cleaning or copying data mappers to the cache directory.
     */
    private void copyDataMappersToCache() throws DataMapperException {
        Path cachePath = getDataMappersCachePath();
        try {
            if (Files.notExists(cachePath)) {
                Files.createDirectories(cachePath);
            } else {
                FileUtils.cleanDirectory(cachePath.toFile());
            }
            Path targetPath = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator
                    + Constants.DATA_MAPPER_DIR_NAME);
            FileUtils.copyDirectory(targetPath.toFile(), cachePath.toFile());
            mojoInstance.getLog().info("Data mappers copied to cache directory successfully");
        } catch (IOException e) {
            throw new DataMapperException("Failed to clean or copy data mappers to cache directory.", e);
        }
    }


    /**
     * Cleans up the resources used during the data mapper bundling process.
     * Deletes the 'src' and 'target' directories inside the data mapper bundling cache directory.
     */
    private void cleanUpBundlingResources() {
        Path srcPath = getDataMapperBundlingCachePath().resolve(Constants.SRC_DIR);
        Path targetPath = getDataMapperBundlingCachePath().resolve(Constants.TARGET_DIR_NAME);

        try {
            if (Files.exists(srcPath)) {
                FileUtils.deleteDirectory(srcPath.toFile());
            }
            if (Files.exists(targetPath)) {
                FileUtils.deleteDirectory(targetPath.toFile());
            }
        } catch (IOException e) {
            mojoInstance.logError("Failed to clean up bundling resources: " + e.getMessage());
        }

    }
}
