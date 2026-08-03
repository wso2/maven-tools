/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.wso2.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class responsible for validating connector parameters.
 * It verifies that all parameters in operation XML files have
 * corresponding entries in the UI schema files.
 */
public class ConnectorValidator {
    private final File resourcesDirectory;
    private final String connectorName;
    private final AbstractMojo mojo;

    /**
     * Constructor for the ConnectorValidator.
     *
     * @param resourcesDirectory The resources directory containing connector files.
     * @param connectorName      The name of the connector.
     * @param mojo              The mojo for logging.
     */
    public ConnectorValidator(File resourcesDirectory, String connectorName, AbstractMojo mojo) {
        this.resourcesDirectory = resourcesDirectory;
        this.connectorName = connectorName;
        this.mojo = mojo;
    }

    /**
     * Validates the connector parameters.
     *
     * @throws MojoExecutionException If validation fails.
     */
    public void validate() throws MojoExecutionException {
        mojo.getLog().info("Validating connector parameters for " + connectorName);

        try {
            File operationsDir = new File(resourcesDirectory, Constants.CONFIG_DIR);
            if (!operationsDir.exists() || !operationsDir.isDirectory()) {
                throw new MojoExecutionException("Operations directory not found at: " + operationsDir.getAbsolutePath());
            }

            File uiSchemaDir = new File(resourcesDirectory, Constants.UISCHEMA_DIR);
            if (!uiSchemaDir.exists() || !uiSchemaDir.isDirectory()) {
                throw new MojoExecutionException("UI Schema directory not found at: " + uiSchemaDir.getAbsolutePath());
            }

            // First validate UI schema files structure
            UISchemaValidator uiSchemaValidator = new UISchemaValidator(uiSchemaDir, connectorName, mojo);
            uiSchemaValidator.validate();

            // Then validate parameter mapping between operations and UI schemas
            Map<String, Set<String>> operationParameters = extractOperationParameters(resourcesDirectory);
            Map<String, Set<String>> uiSchemaParameters = extractUISchemaParameters(uiSchemaDir);

            validateParameters(operationParameters, uiSchemaParameters);
            
            // Validate output schemas for operations
            validateOutputSchemas(operationParameters);
            
            // Generate documentation
            File targetDirectory = new File(resourcesDirectory.getParentFile().getParentFile().getParentFile(), 
                    Constants.DEFAULT_TARGET_FOLDER);
            DocumentationGenerator docGenerator = new DocumentationGenerator(resourcesDirectory, connectorName, mojo, 
                    targetDirectory);
            docGenerator.generateDocs();

        } catch (Exception e) {
            throw new MojoExecutionException("Connector validation failed", e);
        }
    }

    /**
     * Extracts parameters from operation XML files recursively scanning through all subdirectories.
     *
     * @param resourcesDir The resources directory containing connector files.
     * @return Map of operation names to their parameters.
     * @throws ParserConfigurationException If parser configuration fails.
     * @throws IOException                  If IO operations fail.
     * @throws SAXException                 If parsing fails.
     */
    private Map<String, Set<String>> extractOperationParameters(File resourcesDir)
            throws ParserConfigurationException, IOException, SAXException {
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Map<String, Set<String>> operationParameters = new HashMap<String, Set<String>>();

        // Get all XML files recursively
        List<File> xmlFiles = findXmlFiles(resourcesDir);
        
        for (File xmlFile : xmlFiles) {
            // Skip component.xml
            if (xmlFile.getName().equalsIgnoreCase(Constants.COMPONENT_XML)) {
                continue;
            }
            
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            // Extract operation name from filename (without extension)
            String operationName = xmlFile.getName().replaceAll("\\" + Constants.XML_EXTENSION + "$", "");
            Set<String> parameters = new HashSet<String>();

            // Extract parameters from <parameter> elements
            NodeList parameterNodes = document.getElementsByTagName(Constants.PARAMETER_ELEMENT);
            for (int i = 0; i < parameterNodes.getLength(); i++) {
                Node paramNode = parameterNodes.item(i);
                if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element paramElement = (Element) paramNode;
                    String paramName = paramElement.getAttribute(Constants.NAME_ATTRIBUTE);
                    if (paramName != null && !paramName.isEmpty()) {
                        parameters.add(paramName);
                    }
                }
            }

            if (!parameters.isEmpty()) {
                operationParameters.put(operationName, parameters);
                mojo.getLog().debug("Operation: " + operationName + " - Parameters: " + parameters);
                mojo.getLog().debug("Found in file: " + xmlFile.getAbsolutePath());
            }
        }
        
        return operationParameters;
    }

    /**
     * Recursively finds all XML files in a directory and its subdirectories.
     *
     * @param dir The directory to search in.
     * @return List of XML files.
     */
    private List<File> findXmlFiles(File dir) {
        List<File> xmlFiles = new ArrayList<>();
        
        if (!dir.isDirectory()) {
            return xmlFiles;
        }
        
        // Get immediate files in the directory
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(Constants.XML_EXTENSION)) {
                    xmlFiles.add(file);
                } else if (file.isDirectory()) {
                    // Skip uischema directory as it contains JSON files, not operations
                    if (!Constants.UISCHEMA_DIR.equals(file.getName())) {
                        xmlFiles.addAll(findXmlFiles(file));
                    }
                }
            }
        }
        
        return xmlFiles;
    }

    /**
     * Extracts parameters from UI schema files.
     *
     * @param uiSchemaDir The directory containing UI schema files.
     * @return Map of operation names to their UI schema parameters.
     * @throws IOException If IO operations fail.
     */
    private Map<String, Set<String>> extractUISchemaParameters(File uiSchemaDir)
            throws IOException {
        
        Map<String, Set<String>> uiSchemaParameters = new HashMap<String, Set<String>>();

        // Get all JSON files
        File[] jsonFiles = uiSchemaDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().toLowerCase().endsWith(Constants.JSON_EXTENSION);
            }
        });

        if (jsonFiles != null) {
            for (File jsonFile : jsonFiles) {
                try {
                    // Read JSON content
                    String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(content);
                    
                    // If parsing succeeds without exception, it's valid JSON
                    mojo.getLog().debug("Successfully validated JSON syntax for: " + jsonFile.getName());
                    
                    if (!jsonElement.isJsonObject()) {
                        mojo.getLog().warn("UI schema file is not a JSON object: " + jsonFile.getName());
                        continue;
                    }
                    
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    // Extract operation name from filename (without extension)
                    String filenameOperation = jsonFile.getName().replaceAll("\\" + Constants.JSON_EXTENSION + "$", "");
                    Set<String> parameters = new HashSet<String>();

                    // Check if operationName in JSON matches the filename
                    String jsonOperationName = null;
                    if (jsonObject.has(Constants.OPERATION_NAME)) {
                        jsonOperationName = jsonObject.get(Constants.OPERATION_NAME).getAsString();
                        if (!filenameOperation.equals(jsonOperationName)) {
                            mojo.getLog().warn("Operation name mismatch in " + jsonFile.getName() + 
                                              ": filename indicates '" + filenameOperation + 
                                              "' but JSON contains '" + jsonOperationName + "'");
                        }
                    }
                    
                    String operationName = jsonOperationName != null ? jsonOperationName : filenameOperation;

                    // Extract parameters recursively
                    extractParametersFromJson(jsonObject, parameters);

                    uiSchemaParameters.put(operationName, parameters);
                    mojo.getLog().debug("UI Schema: " + operationName + " - Parameters: " + parameters);
                    
                } catch (Exception e) {
                    mojo.getLog().error("Invalid JSON in UI schema file: " + jsonFile.getName() + " - " + e.getMessage());
                    // Continue with the next file, this one is invalid
                }
            }
        }
        
        return uiSchemaParameters;
    }
    
    /**
     * Recursively extracts parameters from JSON structure.
     * 
     * @param jsonElement The JSON element to extract parameters from
     * @param parameters Set to collect parameter names
     */
    private void extractParametersFromJson(JsonElement jsonElement, Set<String> parameters) {
        if (!jsonElement.isJsonObject()) {
            return;
        }
        
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        
        // Check if this is an attribute with value containing name
        if (jsonObject.has(Constants.TYPE) && Constants.ATTRIBUTE_TYPE.equals(jsonObject.get(Constants.TYPE).getAsString()) 
                && jsonObject.has(Constants.VALUE)) {
            JsonObject value = jsonObject.getAsJsonObject(Constants.VALUE);
            if (value.has(Constants.NAME_ATTRIBUTE)) {
                String paramName = value.get(Constants.NAME_ATTRIBUTE).getAsString();
                if (paramName != null && !paramName.isEmpty()) {
                    parameters.add(paramName);
                }
            }
        }
        
        // Recursively process all JSON objects in this object
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            JsonElement element = entry.getValue();
            if (element.isJsonObject()) {
                extractParametersFromJson(element, parameters);
            } else if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement item = array.get(i);
                    extractParametersFromJson(item, parameters);
                }
            }
        }
    }

    /**
     * Validates that all operation parameters have corresponding UI schema entries.
     *
     * @param operationParameters Map of operation names to their parameters.
     * @param uiSchemaParameters  Map of operation names to their UI schema parameters.
     * @throws MojoExecutionException If validation fails.
     */
    private void validateParameters(Map<String, Set<String>> operationParameters, 
                                   Map<String, Set<String>> uiSchemaParameters) throws MojoExecutionException {
        
        StringBuilder errorBuilder = new StringBuilder();
        
        for (Map.Entry<String, Set<String>> entry : operationParameters.entrySet()) {
            String operationName = entry.getKey();
            Set<String> params = entry.getValue();
            
            // Check if there's a corresponding UI schema file
            if (!uiSchemaParameters.containsKey(operationName)) {
                mojo.getLog().warn("Missing UI schema file for operation: " + operationName);
                continue;
            }
            
            Set<String> uiParams = uiSchemaParameters.get(operationName);
            
            // Check for parameters in operation XML that are missing in UI schema
            for (String param : params) {
                if (!uiParams.contains(param)) {
                    errorBuilder.append("Parameter '")
                              .append(param)
                              .append("' in operation '")
                              .append(operationName)
                              .append("' is missing from UI schema\n");
                }
            }
        }
        
        if (errorBuilder.length() > 0) {
            throw new MojoExecutionException("Connector parameter validation failed:\n" + errorBuilder.toString());
        }
        
        mojo.getLog().info("Connector parameter validation successful");
    }

    /**
     * Validates output schema files for each operation.
     * Checks if there is a valid output schema JSON file for each operation except "init".
     *
     * @param operationParameters Map of operation names to their parameters.
     * @throws MojoExecutionException If validation fails.
     */
    private void validateOutputSchemas(Map<String, Set<String>> operationParameters) throws MojoExecutionException {
        mojo.getLog().info("Validating output schemas for operations");
        
        File outputSchemaDir = new File(resourcesDirectory, Constants.OUTPUTSCHEMA_DIR);
        if (!outputSchemaDir.exists() || !outputSchemaDir.isDirectory()) {
            mojo.getLog().warn("Output schema directory not found at: " + outputSchemaDir.getAbsolutePath());
            return;
        }

        StringBuilder errorBuilder = new StringBuilder();
        
        for (String operationName : operationParameters.keySet()) {
            // Skip validation for init operation
            if (Constants.INIT_OPERATION.equalsIgnoreCase(operationName)) {
                continue;
            }
            
            File outputSchemaFile = new File(outputSchemaDir, operationName + Constants.JSON_EXTENSION);
            if (!outputSchemaFile.exists()) {
                errorBuilder.append("Missing output schema for operation: ")
                          .append(operationName)
                          .append("\n");
                continue;
            }
            
            try {
                String content = new String(Files.readAllBytes(outputSchemaFile.toPath()), StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(content);
                
                // Ensure it parses as valid JSON
                if (jsonElement.isJsonObject()) {
                    mojo.getLog().debug("Valid output schema found for operation: " + operationName);
                } else {
                    errorBuilder.append("Invalid output schema format for operation: ")
                              .append(operationName)
                              .append(". Expected a JSON object.")
                              .append("\n");
                }
            } catch (Exception e) {
                errorBuilder.append("Invalid JSON in output schema for operation: ")
                          .append(operationName)
                          .append(". Error: ")
                          .append(e.getMessage())
                          .append("\n");
            }
        }
        
        if (errorBuilder.length() > 0) {
            throw new MojoExecutionException("Output schema validation failed:\n" + errorBuilder.toString());
        }
        
        mojo.getLog().info("Output schema validation successful");
    }
}
