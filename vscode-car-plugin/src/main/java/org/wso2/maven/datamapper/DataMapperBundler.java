/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.maven.datamapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.wso2.maven.CARMojo;

public class DataMapperBundler {
    private final CARMojo mojoInstance;
    private final String resourcesDirectory;
    private final Invoker invoker;

    public DataMapperBundler(CARMojo mojoInstance, String resourcesDirectory) {
        this.mojoInstance = mojoInstance;
        this.resourcesDirectory = resourcesDirectory;
        this.invoker = new DefaultInvoker();
    }

    /**
     * Bundles all data mappers found in the specified resources directory.
     */
    public void bundleDataMapper() {
        String dataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_PATH;
        List<Path> dataMappers = listSubDirectories(dataMapperDirectoryPath);
    
        if (dataMappers.isEmpty()) {
            // No data mappers to bundle
            return;
        }
    
        appendDataMapperLogs();
        String mavenHome = getMavenHome();
    
        if (mavenHome == null) {
            mojoInstance.logError("Could not determine Maven home.");
            return;
        }
    
        createDataMapperArtifacts();
        setupInvoker(mavenHome);
    
        if (!installNodeAndNPM()) {
            return;
        }
    
        if (!runNpmInstall()) {
            return;
        }
    
        bundleDataMappers(dataMappers);
        removeBundlingArtifacts();
    }
    
    /**
     * Sets up the Maven Invoker with the specified Maven home directory.
     *
     * @param mavenHome The path to the Maven home directory.
     */
    private void setupInvoker(String mavenHome) {
        invoker.setMavenHome(new File(mavenHome));
        invoker.setOutputHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String line) {
                if (!line.contains("BUILD SUCCESS")) {
                    System.out.println(line);
                }
            }
        });
    }
    
    /**
     * Installs Node.js and NPM using Maven.
     *
     * @return true if installation is successful, false otherwise.
     */
    private boolean installNodeAndNPM() {
        InvocationRequest request = createBaseRequest();
        mojoInstance.logInfo("Installing Node and NPM");
        request.setGoals(Collections.singletonList(Constants.INSTALL_NODE_AND_NPM_GOAL));
        setNodeAndNpmProperties(request);
    
        return executeRequest(request, "Node and NPM installation failed.");
    }
    
    /**
     * Runs 'npm install' to install dependencies.
     *
     * @return true if the command executes successfully, false otherwise.
     */
    private boolean runNpmInstall() {
        InvocationRequest request = createBaseRequest();
        mojoInstance.logInfo("Running npm install");
        request.setGoals(Collections.singletonList(Constants.NPM_GOAL));
        setNpmInstallProperties(request);
    
        return executeRequest(request, "npm install failed.");
    }
    
    /**
     * Bundles each data mapper found in the list.
     *
     * @param dataMappers List of paths to data mapper directories.
     */
    private void bundleDataMappers(List<Path> dataMappers) {
        for (Path dataMapper : dataMappers) {
            if (!bundleSingleDataMapper(dataMapper)) {
                return;
            }
        }
        mojoInstance.logInfo("Data mapper bundling completed successfully");
    }
    
    /**
     * Handles the bundling process for a single data mapper.
     *
     * @param dataMapper The path to the data mapper directory.
     * @return true if the bundling is successful, false otherwise.
     */
    private boolean bundleSingleDataMapper(Path dataMapper) {
        copyTsFiles(dataMapper);
        String dataMapperName = dataMapper.getFileName().toString();
        mojoInstance.logInfo("Bundling data mapper: " + dataMapperName);
        createWebpackConfig(dataMapperName);

        Path npmDirectory = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME);
    
        InvocationRequest request = createBaseRequest();
        request.setBaseDirectory(npmDirectory.toFile());
        request.setGoals(Collections.singletonList(Constants.NPM_RUN_BUILD_GOAL
                + " -Dexec.executable=" + Constants.NPM_COMMAND
                + " -Dexec.args=\"" + Constants.RUN_BUILD + "\""));
    
        boolean success = executeRequest(request, "Failed to bundle data mapper: " + dataMapperName);
        if (success) {
            mojoInstance.logInfo("Bundle completed for data mapper: " + dataMapperName);
            Path bundledJsFilePath = Paths.get("." + File.separator
                    + Constants.TARGET_DIR_NAME + File.separator + "src" + File.separator + dataMapperName + ".dmc");
            copyBundledJsFile(bundledJsFilePath.toString(), dataMapper);
            removeSourceFiles();
            removeWebpackConfig();
        }
        return success;
    }
    
    /**
     * Creates a base Maven invocation request.
     *
     * @return The configured invocation request.
     */
    private InvocationRequest createBaseRequest() {
        InvocationRequest request = new DefaultInvocationRequest();
        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path pomPath = baseDir.resolve(Paths.get("." + File.separator + Constants.TARGET_DIR_NAME
            + File.separator + Constants.POM_FILE_NAME));
        request.setPomFile(pomPath.toFile());
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
        request.setProperties(properties);
    }
    
    /**
     * Executes a Maven invocation request and logs any errors.
     *
     * @param request The Maven invocation request to execute.
     * @param errorMessage The error message to log if the execution fails.
     * @return true if the execution is successful, false otherwise.
     */
    private boolean executeRequest(InvocationRequest request, String errorMessage) {
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                mojoInstance.logError(errorMessage);
                if (result.getExecutionException() != null) {
                    mojoInstance.logError(result.getExecutionException().getMessage());
                }
                return false;
            }
            return true;
        } catch (MavenInvocationException e) {
            mojoInstance.logError(errorMessage);
            mojoInstance.logError(e.getMessage());
            return false;
        }
    }

    /**
     * Creates necessary artifacts for data mapper bundling.
     */
    private void createDataMapperArtifacts() {
        mojoInstance.logInfo("Creating data mapper artifacts");
        ensureDataMapperTargetExists();
        createPomFile();
        createPackageJson();
        createConfigJson();
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
     */
    private void createPomFile() {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.example</groupId>\n" +
            "    <artifactId>data-mapper-bundler</artifactId>\n" +
            "    <version>1.0-SNAPSHOT</version>\n" +
            "</project>";
    
        Path pomPath = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator + "pom.xml");
        try {
            Files.write(pomPath, pomContent.getBytes(), StandardOpenOption.CREATE);
            mojoInstance.logInfo("pom.xml created successfully at " + pomPath);
        } catch (IOException e) {
            mojoInstance.logError("Failed to create pom.xml file.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Creates a package.json file for managing npm packages.
     */
    private void createPackageJson() {
        String packageJsonContent = "{\n" +
                "    \"name\": \"data-mapper-bundler\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"scripts\": {\n" +
                "        \"build\": \"tsc && webpack\"\n" +
                "    },\n" +
                "    \"devDependencies\": {\n" +
                "        \"typescript\": \"^4.4.2\",\n" +
                "        \"webpack\": \"^5.52.0\",\n" +
                "        \"webpack-cli\": \"^4.8.0\",\n" +
                "        \"ts-loader\": \"^9.2.3\"\n" +
                "    }\n" +
                "}";

        try (FileWriter fileWriter = new FileWriter("." + File.separator + Constants.TARGET_DIR_NAME
            + File.separator + Constants.PACKAGE_JSON_FILE_NAME)) {
            fileWriter.write(packageJsonContent);
        } catch (IOException e) {
            mojoInstance.logError("Failed to create package.json file.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Creates a TypeScript configuration file (tsconfig.json).
     */
    private void createConfigJson() {
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
    
        try (FileWriter fileWriter = new FileWriter("." + File.separator + Constants.TARGET_DIR_NAME
            + File.separator + Constants.TS_CONFIG_FILE_NAME)) {
            fileWriter.write(tsConfigContent);
        } catch (IOException e) {
            mojoInstance.logError("Failed to create tsconfig.json file.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Creates a webpack configuration file for the data mapper.
     *
     * @param dataMapperName The name of the data mapper.
     */
    private void createWebpackConfig(String dataMapperName) {
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
                "    },\n" +
                "};";
    
        try (FileWriter fileWriter = new FileWriter("." + File.separator + Constants.TARGET_DIR_NAME
            + File.separator + Constants.WEBPACK_CONFIG_FILE_NAME)) {
            fileWriter.write(webPackConfigContent);
        } catch (IOException e) {
            mojoInstance.logError("Failed to create webpack.config.js file.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Lists subdirectories in a given directory path.
     *
     * @param directory The directory path to list subdirectories from.
     * @return A list of paths representing subdirectories.
     */
    private List<Path> listSubDirectories(String directory) {
        Path dirPath = Paths.get(directory);
        List<Path> subDirectories = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.equals(dirPath)) {
                    subDirectories.add(path);
                }
            }
        } catch (NoSuchFileException e) {
            mojoInstance.logInfo("datamapper directory not found");
        } catch (IOException e) {
            mojoInstance.logError("Failed to find data mapper directories.");
            mojoInstance.logError(e.getMessage());
        }
        return subDirectories;
    }

    /**
     * Copies TypeScript files from the source directory to the target directory.
     *
     * @param sourceDir The source directory containing TypeScript files.
     */
    private void copyTsFiles(final Path sourceDir) {
        final Path destDir = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator + "src");

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
                    mojoInstance.logError("Failed to copy data mapper file: " + sourcePath);
                    mojoInstance.logError(e.getMessage());
                }
            }
        } catch (IOException e) {
            mojoInstance.logError("Failed to copy data mapper files.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Copies the bundled JavaScript file to the specified destination directory.
     *
     * @param sourceFile The source file path of the bundled JavaScript file.
     * @param destinationDir The destination directory to copy the file to.
     */
    private void copyBundledJsFile(String sourceFile, Path destinationDir) {
        mojoInstance.logInfo("Copying bundled js file to registry");
        Path sourcePath = Paths.get(sourceFile);
        Path destPath = destinationDir.resolve(sourcePath.getFileName());

        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            mojoInstance.logError("Failed to copy bundled js file to registry.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Ensures that the data mapper target directory exists.
     */
    private void ensureDataMapperTargetExists() {
        Path dataMapperPath = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME);
        if (!Files.exists(dataMapperPath)) {
            try {
                Files.createDirectories(dataMapperPath);
            } catch (IOException e) {
                mojoInstance.logError("Failed to create data-mapper artifacts directory: " + dataMapperPath);
                mojoInstance.logError(e.getMessage());
            }
        }
    }

    /**
     * Removes source files from the target directory.
     */
    private void removeSourceFiles() {
        Path dataMapperPath = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME + File.separator + "src");

        try {
            Files.walkFileTree(dataMapperPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // Delete all files
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir); // Delete directory after its contents are deleted
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            mojoInstance.logError("Error while removing data-mapper source directory.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Removes the webpack configuration file.
     */
    private void removeWebpackConfig() {
        Path filePath = Paths.get("." + File.separator + Constants.TARGET_DIR_NAME
            + File.separator + Constants.WEBPACK_CONFIG_FILE_NAME);
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            mojoInstance.logError("Error while removing webpack.config.js file.");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Retrieves the Maven home directory.
     *
     * @return The Maven home directory path, or null if it cannot be determined.
     */
    private String getMavenHome() {
        mojoInstance.logInfo("Finding maven home");

        // First try to find Maven home using system property
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome != null) {
            return mavenHome;
        }

        // Fallback: Try to find Maven home using environment variable or default paths
        mavenHome = System.getenv("M2_HOME");
        if (mavenHome != null) {
            return mavenHome;
        }

        // Fallback: Try to find Maven home using command line
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains(Constants.OS_WINDOWS)) {
            processBuilder.command("cmd.exe", "/c", "mvn -v");
        } else {
            processBuilder.command("sh", "-c", "mvn -v");
        }
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Maven home: ")) {
                    return line.split("Maven home: ")[1].trim();
                }
            }
        } catch (IOException e) {
            mojoInstance.logError("Failed to find maven home");
            mojoInstance.logError(e.getMessage());
        }
        return null;
    }

    /**
     * Cleans up the artifacts created during the data mapper bundling process.
     */
    private void removeBundlingArtifacts() {
        mojoInstance.logInfo("Cleaning up data mapper bundling artifacts");
        String[] pathsToDelete = {
            "." + File.separator + Constants.TARGET_DIR_NAME
        };

        for (String path : pathsToDelete) {
            File file = new File(path);
            deleteRecursively(file);
        }
    }

    /**
     * Recursively deletes files and directories.
     *
     * @param file The file or directory to delete.
     */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        if (!file.delete()) {
            mojoInstance.logError("Failed to delete " + file.getPath());
        }
    }
}
