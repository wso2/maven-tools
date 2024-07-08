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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.function.Predicate;

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

    public DataMapperBundler(CARMojo mojoInstance, String resourcesDirectory) {
        this.mojoInstance = mojoInstance;
        this.resourcesDirectory = resourcesDirectory;
    }

    public void bundleDataMapper() {
        appendDataMapperLogs();
        String mavenHome = getMavenHome();

        if (mavenHome == null) {
            throw new RuntimeException("Could not determine Maven home.");
        }

        ensureDataMapperDirectoryExists();
        createPackageJson();
        createConfigJson();

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));
        invoker.setOutputHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String line) {
                // Do nothing to suppress output
            }
        });

        InvocationRequest request = new DefaultInvocationRequest();
        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path pomPath = baseDir.resolve("./pom.xml");
        request.setPomFile(pomPath.toFile());

        try {
            // Install Node and NPM
            request.setGoals(Collections.singletonList("com.github.eirslett:frontend-maven-plugin:1.12.0:install-node-and-npm"));
            Properties properties = new Properties();
            properties.setProperty("nodeVersion", "v14.17.3"); // Ensure the version is prefixed with 'v'
            properties.setProperty("npmVersion", "6.14.13");
            request.setProperties(properties);
            InvocationResult result = invoker.execute(request);

            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Node and NPM installation failed.");
            }

            // Run npm install
            request = new DefaultInvocationRequest();
            request.setGoals(Collections.singletonList("com.github.eirslett:frontend-maven-plugin:1.12.0:npm"));
            properties = new Properties();
            properties.setProperty("arguments", "install");
            request.setProperties(properties);
            result = invoker.execute(request);

            if (result.getExitCode() != 0) {
                throw new IllegalStateException("NPM install failed.");
            }

            String DataMapperDirectoryPath = resourcesDirectory + File.separator + Constants.DATA_MAPPER_DIR_PATH;

            List<Path> dataMappers = listSubDirectories(DataMapperDirectoryPath);

            for (Path dataMapper : dataMappers) {
                copyTsFiles(dataMapper);

                createWebpackConfig(dataMapper.getFileName().toString());

                // Step 3: Run npm run build
                request = new DefaultInvocationRequest();
                request.setGoals(Collections.singletonList("org.codehaus.mojo:exec-maven-plugin:1.6.0:exec@build -Dexec.executable=npm -Dexec.args=\"run build\""));
                result = invoker.execute(request);

                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("npm run build failed.");
                }

                copyBundledJsFile("./target/bundle.js", dataMapper);

                removeTsFiles();
                removeWebpackConfig();
            }

            System.out.println("All steps executed successfully.");

        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Append log of data-mapper bundling process.
     */
    private void appendDataMapperLogs() {
        mojoInstance.getLog().info("------------------------------------------------------------------------");
        mojoInstance.getLog().info("Bundling Data Mapper");
        mojoInstance.getLog().info("------------------------------------------------------------------------");
    }

    private static void createPackageJson() {
        String jsonContent = "{\n" +
                "    \"name\": \"typescript-project\",\n" +
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

        String fileName = "package.json";

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(jsonContent);
            System.out.println("Successfully wrote " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createConfigJson() {
        String jsonContent = "{\n" +
                "    \"compilerOptions\": {\n" +
                "        \"outDir\": \"./target\",\n" +
                "        \"module\": \"commonjs\",\n" +
                "        \"target\": \"es5\",\n" +
                "        \"sourceMap\": true\n" +
                "    },\n" +
                "    \"include\": [\n" +
                "        \"./data-mapper/**/*\"\n" +
                "    ]\n" +
                "}";

        String fileName = "tsconfig.json";

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(jsonContent);
            System.out.println("Successfully wrote " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createWebpackConfig(String dataMapperName) {
        String jsContent = "const path = require(\"path\");\n" +
                "module.exports = {\n" +
                "    entry: \"./data-mapper/" + dataMapperName + ".ts\",\n" +
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
                "        extensions: [\".tsx\", \".ts\", \".js\"],\n" +
                "    },\n" +
                "    output: {\n" +
                "        filename: \"bundle.js\",\n" +
                "        path: path.resolve(__dirname, \"target\"),\n" +
                "    },\n" +
                "};";

        String fileName = "webpack.config.js";

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(jsContent);
            System.out.println("Successfully wrote " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Path> listSubDirectories(String directory) {
        Path dirPath = Paths.get(directory);
        List<Path> subDirectories = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.equals(dirPath)) {
                    subDirectories.add(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return subDirectories;
    }

    private static void copyTsFiles(final Path sourceDir) {
        final Path destDir = Paths.get("./data-mapper");

        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.filter(new Predicate<Path>() {
                @Override
                public boolean test(Path file) {
                    return file.toString().endsWith(".ts");
                }
            }).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path sourcePath) {
                    Path destPath = destDir.resolve(sourceDir.relativize(sourcePath));
                    try {
                        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Copied: " + sourcePath + " to " + destPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyBundledJsFile(String sourceFile, Path destinationDir) {
        Path sourcePath = Paths.get(sourceFile);
        Path destPath = destinationDir.resolve(sourcePath.getFileName());

        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied: " + sourcePath + " to " + destPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureDataMapperDirectoryExists() {
        Path dataMapperPath = Paths.get("./data-mapper");
        if (!Files.exists(dataMapperPath)) {
            try {
                Files.createDirectories(dataMapperPath);
                System.out.println("Directory created: " + dataMapperPath);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create directory: " + dataMapperPath);
            }
        }
    }

    private static void removeTsFiles() {
        Path dirPath = Paths.get("./data-mapper");

        try (Stream<Path> stream = Files.walk(dirPath)) {
            stream.filter(new Predicate<Path>() {
                @Override
                public boolean test(Path file) {
                    return file.toString().endsWith(".ts");
                }
            }).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path tsFile) {
                    try {
                        Files.delete(tsFile);
                        System.out.println("Deleted: " + tsFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void removeWebpackConfig() {
        Path filePath = Paths.get("webpack.config.js");
        try {
            Files.delete(filePath);
            System.out.println("Deleted: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getMavenHome() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
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
            e.printStackTrace();
        }
        return null;
    }
}
