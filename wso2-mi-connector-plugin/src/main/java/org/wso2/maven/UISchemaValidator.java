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
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class responsible for validating UI schema JSON files.
 * It verifies the structure and content of UI schema files against the specification.
 */
public class UISchemaValidator {
    private final File uiSchemaDir;
    private final String connectorName;
    private final AbstractMojo mojo;
    
    // Valid input types
    private static final Set<String> VALID_INPUT_TYPES = new HashSet<>(Arrays.asList(
            "string", "stringOrExpression", "combo", "comboOrExpression", "boolean", "integer", "expressionTextArea",
            "connection", "textAreaOrExpression", "booleanOrExpression", "checkbox", "integerOrExpression"));
    
    // Element types
    private static final String ATTRIBUTE_TYPE = "attribute";
    private static final String ATTRIBUTE_GROUP_TYPE = "attributeGroup";
    
    /**
     * Constructor for the UISchemaValidator.
     *
     * @param uiSchemaDir The directory containing UI schema JSON files.
     * @param connectorName The name of the connector.
     * @param mojo The mojo for logging.
     */
    public UISchemaValidator(File uiSchemaDir, String connectorName, AbstractMojo mojo) {
        this.uiSchemaDir = uiSchemaDir;
        this.connectorName = connectorName;
        this.mojo = mojo;
    }

    /**
     * Validates all UI schema files in the directory.
     *
     * @throws MojoExecutionException If validation fails.
     */
    public void validate() throws MojoExecutionException {
        mojo.getLog().info("Validating UI schema files for " + connectorName);
        
        if (!uiSchemaDir.exists() || !uiSchemaDir.isDirectory()) {
            throw new MojoExecutionException("UI Schema directory not found at: " + uiSchemaDir.getAbsolutePath());
        }
        
        Map<String, File> operationSchemaFiles = new HashMap<>();
        Map<String, File> connectionSchemaFiles = new HashMap<>();
        
        // Get all JSON files
        File[] jsonFiles = uiSchemaDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().toLowerCase().endsWith(".json");
            }
        });
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            throw new MojoExecutionException("No UI schema files found in: " + uiSchemaDir.getAbsolutePath());
        }
        
        // Categorize files
        for (File jsonFile : jsonFiles) {
            try {
                String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(content);
                
                if (!jsonElement.isJsonObject()) {
                    mojo.getLog().error("Invalid JSON structure in file: " + jsonFile.getName());
                    continue;
                }
                
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                
                // Check if it's a connection or operation schema
                if (jsonObject.has("connectionName")) {
                    String connectionName = jsonObject.get("connectionName").getAsString();
                    connectionSchemaFiles.put(connectionName, jsonFile);
                } else if (jsonObject.has("operationName")) {
                    String operationName = jsonObject.get("operationName").getAsString();
                    operationSchemaFiles.put(operationName, jsonFile);
                } else {
                    mojo.getLog().error("Schema file missing required fields: " + jsonFile.getName());
                }
                
            } catch (Exception e) {
                throw new MojoExecutionException("Error processing file " + jsonFile.getName(), e);
            }
        }
        
        // Validate all files
        validateConnectionSchemas(connectionSchemaFiles);
        validateOperationSchemas(operationSchemaFiles);
        
        mojo.getLog().info("UI schema validation completed successfully");
    }
    
    /**
     * Validates connection schema files.
     *
     * @param connectionSchemaFiles Map of connection name to JSON file
     * @throws MojoExecutionException If validation fails
     */
    private void validateConnectionSchemas(Map<String, File> connectionSchemaFiles) throws MojoExecutionException {
        for (Map.Entry<String, File> entry : connectionSchemaFiles.entrySet()) {
            String connectionName = entry.getKey();
            File jsonFile = entry.getValue();
            
            try {
                String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonObject schema = parser.parse(content).getAsJsonObject();
                
                // Validate required fields
                validateRequiredField(schema, "connectorName", jsonFile.getName());
                validateRequiredField(schema, "connectionName", jsonFile.getName());
                validateRequiredField(schema, "title", jsonFile.getName());
                validateRequiredField(schema, "elements", jsonFile.getName());
                
                // Check connector name matches
                if (!connectorName.equals(schema.get("connectorName").getAsString())) {
                    mojo.getLog().warn("Connector name mismatch in " + jsonFile.getName() + ": expected '" + 
                                      connectorName + "' but found '" + schema.get("connectorName").getAsString() + "'");
                }
                
                // Validate elements structure
                if (schema.has("elements") && schema.get("elements").isJsonArray()) {
                    validateElements(schema.get("elements").getAsJsonArray(), jsonFile.getName());
                } else {
                    throw new MojoExecutionException("Invalid elements structure in " + jsonFile.getName());
                }
                
            } catch (Exception e) {
                throw new MojoExecutionException("Error validating connection schema " + jsonFile.getName(), e);
            }
        }
    }
    
    /**
     * Validates operation schema files.
     *
     * @param operationSchemaFiles Map of operation name to JSON file
     * @throws MojoExecutionException If validation fails
     */
    private void validateOperationSchemas(Map<String, File> operationSchemaFiles) throws MojoExecutionException {
        for (Map.Entry<String, File> entry : operationSchemaFiles.entrySet()) {
            String operationName = entry.getKey();
            File jsonFile = entry.getValue();
            
            try {
                String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonObject schema = parser.parse(content).getAsJsonObject();
                
                // Validate required fields
                validateRequiredField(schema, "connectorName", jsonFile.getName());
                validateRequiredField(schema, "operationName", jsonFile.getName());
                validateRequiredField(schema, "title", jsonFile.getName());
                validateRequiredField(schema, "elements", jsonFile.getName());
                
                // Check connector name matches
                if (!connectorName.equals(schema.get("connectorName").getAsString())) {
                    mojo.getLog().warn("Connector name mismatch in " + jsonFile.getName() + ": expected '" + 
                                      connectorName + "' but found '" + schema.get("connectorName").getAsString() + "'");
                }
                
                // Check filename matches operation name
                String filenameBase = jsonFile.getName().replaceAll("\\.json$", "");
                if (!filenameBase.equals(operationName)) {
                    mojo.getLog().warn("Filename doesn't match operationName in " + jsonFile.getName() + 
                                      ": filename indicates '" + filenameBase + "' but JSON contains '" + operationName + "'");
                }
                
                // Validate elements structure
                if (schema.has("elements") && schema.get("elements").isJsonArray()) {
                    validateElements(schema.get("elements").getAsJsonArray(), jsonFile.getName());
                } else {
                    throw new MojoExecutionException("Invalid elements structure in " + jsonFile.getName());
                }
                
            } catch (Exception e) {
                throw new MojoExecutionException("Error validating operation schema " + jsonFile.getName(), e);
            }
        }
    }
    
    /**
     * Validates elements array in UI schema.
     *
     * @param elements The elements JSON array
     * @param filename The filename for error reporting
     * @throws MojoExecutionException If validation fails
     */
    private void validateElements(JsonArray elements, String filename) throws MojoExecutionException {
        for (int i = 0; i < elements.size(); i++) {
            JsonElement element = elements.get(i);
            
            if (!element.isJsonObject()) {
                throw new MojoExecutionException("Element at index " + i + " is not a JSON object in " + filename);
            }
            
            JsonObject obj = element.getAsJsonObject();
            
            // Check type field
            if (!obj.has("type")) {
                throw new MojoExecutionException("Element at index " + i + " missing 'type' field in " + filename);
            }
            
            String type = obj.get("type").getAsString();
            
            // Validate based on type
            if (ATTRIBUTE_TYPE.equals(type)) {
                validateAttributeElement(obj, i, filename);
            } else if (ATTRIBUTE_GROUP_TYPE.equals(type)) {
                validateAttributeGroupElement(obj, i, filename);
            } else {
                throw new MojoExecutionException("Unknown element type '" + type + "' at index " + i + " in " + filename);
            }
        }
    }
    
    /**
     * Validates an attribute element.
     *
     * @param element The attribute element JSON object
     * @param index The index for error reporting
     * @param filename The filename for error reporting
     * @throws MojoExecutionException If validation fails
     */
    private void validateAttributeElement(JsonObject element, int index, String filename) throws MojoExecutionException {
        // Check value field
        if (!element.has("value") || !element.get("value").isJsonObject()) {
            throw new MojoExecutionException("Attribute at index " + index + " missing or invalid 'value' field in " + filename);
        }
        
        JsonObject value = element.get("value").getAsJsonObject();
        
        // Check required attribute fields
        validateRequiredField(value, "name", "attribute at index " + index + " in " + filename);
        validateRequiredField(value, "displayName", "attribute at index " + index + " in " + filename);
        validateRequiredField(value, "inputType", "attribute at index " + index + " in " + filename);
        
        // Validate input type
        String inputType = value.get("inputType").getAsString();
        if (!VALID_INPUT_TYPES.contains(inputType)) {
            throw new MojoExecutionException("Invalid inputType '" + inputType + "' at index " + index + " in " + filename);
        }
        
        // Check for additional required fields based on input type
        if ("combo".equals(inputType) || "comboOrExpression".equals(inputType)) {
            if (!value.has("comboValues") || !value.get("comboValues").isJsonArray()) {
                throw new MojoExecutionException("Combo input type requires comboValues array at index " + index + " in " + filename);
            }
        }
        
        if ("connection".equals(inputType)) {
            if (!value.has("allowedConnectionTypes") || !value.get("allowedConnectionTypes").isJsonArray()) {
                throw new MojoExecutionException("Connection input type requires allowedConnectionTypes array at index " + 
                                               index + " in " + filename);
            }
        }
        
        // Validate validation field if present
        if (value.has("validation")) {
            String validation = value.get("validation").getAsString();
            if ("custom".equalsIgnoreCase(validation) && !value.has("validationRegEx")) {
                throw new MojoExecutionException("Custom validation requires validationRegEx field at index " + 
                                               index + " in " + filename);
            }
        }
    }
    
    /**
     * Validates an attribute group element.
     *
     * @param element The attribute group element JSON object
     * @param index The index for error reporting
     * @param filename The filename for error reporting
     * @throws MojoExecutionException If validation fails
     */
    private void validateAttributeGroupElement(JsonObject element, int index, String filename) throws MojoExecutionException {
        // Check value field
        if (!element.has("value") || !element.get("value").isJsonObject()) {
            throw new MojoExecutionException("AttributeGroup at index " + index + " missing or invalid 'value' field in " + 
                                           filename);
        }
        
        JsonObject value = element.get("value").getAsJsonObject();
        
        // Check required fields
        validateRequiredField(value, "groupName", "attributeGroup at index " + index + " in " + filename);
        validateRequiredField(value, "elements", "attributeGroup at index " + index + " in " + filename);
        
        // Recursively validate nested elements
        if (value.has("elements") && value.get("elements").isJsonArray()) {
            validateElements(value.get("elements").getAsJsonArray(), filename);
        } else {
            throw new MojoExecutionException("Invalid elements structure in attributeGroup at index " + index + " in " + 
                                           filename);
        }
    }
    
    /**
     * Validates that a required field exists in a JSON object.
     *
     * @param obj The JSON object to check
     * @param fieldName The required field name
     * @param context Context information for error reporting
     * @throws MojoExecutionException If the field is missing
     */
    private void validateRequiredField(JsonObject obj, String fieldName, String context) throws MojoExecutionException {
        if (!obj.has(fieldName)) {
            throw new MojoExecutionException("Required field '" + fieldName + "' missing in " + context);
        }
    }
}
