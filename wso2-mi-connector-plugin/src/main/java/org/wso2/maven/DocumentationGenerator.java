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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Class responsible for generating documentation for connector operations and connections.
 * It uses the UI schema files to create markdown documentation.
 */
public class DocumentationGenerator {
    private final File resourcesDirectory;
    private final String connectorName;
    private final AbstractMojo mojo;
    private final File docsDirectory;
    
    /**
     * Constructor for the DocumentationGenerator.
     *
     * @param resourcesDirectory The resources directory containing connector files.
     * @param connectorName      The name of the connector.
     * @param mojo               The mojo for logging.
     * @param targetDirectory    The target directory where docs will be generated.
     */
    public DocumentationGenerator(File resourcesDirectory, String connectorName, AbstractMojo mojo, File targetDirectory) {
        this.resourcesDirectory = resourcesDirectory;
        this.connectorName = connectorName;
        this.mojo = mojo;
        this.docsDirectory = new File(targetDirectory, "docs");
    }
    
    /**
     * Generates documentation for the connector operations and connections.
     *
     * @throws MojoExecutionException If documentation generation fails.
     */
    public void generateDocs() throws MojoExecutionException {
        mojo.getLog().info("Generating documentation for " + connectorName + " connector");
        
        try {
            // Create docs directory if it doesn't exist
            if (!docsDirectory.exists()) {
                docsDirectory.mkdirs();
            }
            
            File uiSchemaDir = new File(resourcesDirectory, Constants.UISCHEMA_DIR);
            if (!uiSchemaDir.exists() || !uiSchemaDir.isDirectory()) {
                mojo.getLog().warn("UI Schema directory not found. Skipping documentation generation.");
                return;
            }
            
            // Load component.xml for connector description if available
            String connectorDescription = getConnectorDescription();
            
            // Parse UI schema files and organize them
            Map<String, JsonObject> operationSchemas = new HashMap<>();
            Map<String, JsonObject> connectionSchemas = new HashMap<>();
            
            File[] jsonFiles = uiSchemaDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile() && file.getName().toLowerCase().endsWith(Constants.JSON_EXTENSION);
                }
            });
            
            if (jsonFiles == null || jsonFiles.length == 0) {
                mojo.getLog().warn("No UI schema files found. Skipping documentation generation.");
                return;
            }
            
            for (File jsonFile : jsonFiles) {
                try {
                    String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                    JsonParser jsonParser = new JsonParser();
                    JsonElement jsonElement = jsonParser.parse(content);
                    
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        
                        if (jsonObject.has(Constants.CONNECTION_NAME)) {
                            String connectionName = jsonObject.get(Constants.CONNECTION_NAME).getAsString();
                            connectionSchemas.put(connectionName, jsonObject);
                        } else if (jsonObject.has(Constants.OPERATION_NAME)) {
                            String operationName = jsonObject.get(Constants.OPERATION_NAME).getAsString();
                            operationSchemas.put(operationName, jsonObject);
                        }
                    }
                } catch (Exception e) {
                    mojo.getLog().warn("Failed to process file " + jsonFile.getName() + ": " + e.getMessage());
                }
            }
            
            // Generate markdown documentation
            generateMarkdownDocs(connectorDescription, connectionSchemas, operationSchemas);
            
            mojo.getLog().info("Documentation successfully generated at: " + docsDirectory.getAbsolutePath());
            
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate documentation", e);
        }
    }
    
    /**
     * Retrieves connector description from component.xml if available.
     *
     * @return The connector description.
     */
    private String getConnectorDescription() {
        File componentXml = new File(resourcesDirectory, Constants.COMPONENT_XML);
        if (componentXml.exists()) {
            try {
                String content = new String(Files.readAllBytes(componentXml.toPath()), StandardCharsets.UTF_8);
                // Simple extraction of description, could be enhanced with proper XML parsing
                if (content.contains("<description>") && content.contains("</description>")) {
                    int start = content.indexOf("<description>") + "<description>".length();
                    int end = content.indexOf("</description>");
                    return content.substring(start, end).trim();
                }
            } catch (IOException e) {
                mojo.getLog().warn("Failed to read component.xml: " + e.getMessage());
            }
        }
        
        // Default description
        return "The " + StringUtils.capitalize(connectorName) + " connector enables you to access the " + 
               StringUtils.capitalize(connectorName) + " API functionality.";
    }
    
    /**
     * Generates markdown documentation from the UI schemas.
     *
     * @param connectorDescription The connector description.
     * @param connectionSchemas    Map of connection schemas.
     * @param operationSchemas     Map of operation schemas.
     * @throws IOException         If file operations fail.
     */
    private void generateMarkdownDocs(String connectorDescription, 
                                     Map<String, JsonObject> connectionSchemas,
                                     Map<String, JsonObject> operationSchemas) throws IOException {
        
        StringBuilder markdownBuilder = new StringBuilder();
        
        // Add header and introduction
        String connectorTitle = StringUtils.capitalize(connectorName) + " Connector Reference";
        markdownBuilder.append("# ").append(connectorTitle).append("\n\n");
        markdownBuilder.append("This documentation provides a reference guide for the ").append(StringUtils.capitalize(connectorName)).append(" Connector.\n");
        markdownBuilder.append(connectorDescription).append("\n\n");
        
        // Add connection configurations section if connections exist
        if (!connectionSchemas.isEmpty()) {
            markdownBuilder.append("## Connection Configurations\n\n");
            
            // If multiple connection types exist, explain them
            if (connectionSchemas.size() > 1) {
                markdownBuilder.append("The WSO2 MI ").append(StringUtils.capitalize(connectorName)).append(" Connector supports multiple connection types:\n\n");
            }
            
            // Add each connection type and its documentation
            for (Map.Entry<String, JsonObject> entry : connectionSchemas.entrySet()) {
                String connectionName = entry.getKey();
                JsonObject schema = entry.getValue();
                
                markdownBuilder.append("??? note \"").append(connectionName).append("\"\n");
                markdownBuilder.append("    <table>\n");
                markdownBuilder.append("        <tr>\n");
                markdownBuilder.append("            <th>Parameter Name</th>\n");
                markdownBuilder.append("            <th>Description</th>\n");
                markdownBuilder.append("            <th>Required</th>\n");
                markdownBuilder.append("        </tr>\n");
                
                // Extract parameters from the connection schema
                List<Parameter> parameters = extractParameters(schema);
                Map<String, List<Parameter>> groupedParams = groupParametersBySection(parameters);
                
                // Write parameters by group
                for (Map.Entry<String, List<Parameter>> groupEntry : groupedParams.entrySet()) {
                    String groupName = groupEntry.getKey();
                    List<Parameter> groupParams = groupEntry.getValue();
                    
                    // Add group header if it's not the default group
                    if (!groupName.equals("default")) {
                        markdownBuilder.append("        <tr>\n");
                        markdownBuilder.append("            <th colspan=\"3\">").append(groupName).append("</td>\n");
                        markdownBuilder.append("        </tr>\n");
                    }
                    
                    // Add parameters in this group
                    for (Parameter param : groupParams) {
                        markdownBuilder.append("        <tr>\n");
                        markdownBuilder.append("            <td>").append(param.displayName).append("</td>\n");
                        markdownBuilder.append("            <td>").append(param.description).append("</td>\n");
                        markdownBuilder.append("            <td>").append(param.required ? "Yes" : "Optional").append("</td>\n");
                        markdownBuilder.append("        </tr>\n");
                    }
                }
                
                markdownBuilder.append("    </table>\n\n");
            }
        }
        
        // Add operations section
        markdownBuilder.append("## Operations\n");
        markdownBuilder.append("    \n");
        markdownBuilder.append("    \n");
        
        // Add each operation and its documentation
        for (Map.Entry<String, JsonObject> entry : operationSchemas.entrySet()) {
            String operationName = entry.getKey();
            JsonObject schema = entry.getValue();
            
            // Get operation title and help text
            String title = schema.has("title") ? schema.get("title").getAsString() : StringUtils.capitalize(operationName);
            String helpText = "";
            
            // Try to get help text from schema first
            if (schema.has("help")) {
                helpText = schema.get("help").getAsString();
            } else {
                // Try to find operation description from attribute helpTip
                List<Parameter> parameters = extractParameters(schema);
                for (Parameter param : parameters) {
                    // Often the operation itself is described in one of its parameters' helpTips
                    if (param.description != null && !param.description.isEmpty() && 
                        (param.description.toLowerCase().contains(operationName.toLowerCase()) || 
                         param.name.equalsIgnoreCase(operationName))) {
                        helpText = param.description;
                        break;
                    }
                }
                
                // If still no help text, use default
                if (helpText.isEmpty()) {
                    helpText = "The " + operationName + " operation.";
                }
            }
            
            // Extract first sentence as description
            String description = extractFirstSentence(stripHtmlTags(helpText));
            
            markdownBuilder.append("??? note \"").append(operationName).append("\"\n");
            markdownBuilder.append("    ").append(description).append("\n");
            markdownBuilder.append("    <table>\n");
            markdownBuilder.append("        <tr>\n");
            markdownBuilder.append("            <th>Parameter Name</th>\n");
            markdownBuilder.append("            <th>Description</th>\n");
            markdownBuilder.append("            <th>Required</th>\n");
            markdownBuilder.append("        </tr>\n");
            
            // Extract parameters from the operation schema
            List<Parameter> parameters = extractParameters(schema);
            for (Parameter param : parameters) {
                markdownBuilder.append("        <tr>\n");
                markdownBuilder.append("            <td>").append(param.name).append("</td>\n");
                markdownBuilder.append("            <td>").append(param.description).append("</td>\n");
                markdownBuilder.append("            <td>").append(param.required ? "Yes" : "Optional").append("</td>\n");
                markdownBuilder.append("        </tr>\n");
            }
            
            markdownBuilder.append("    </table>\n\n");
            
            // Add sample configuration
            markdownBuilder.append("    **Sample configuration**\n");
            markdownBuilder.append("    \n");
            markdownBuilder.append("    ```xml\n");
            markdownBuilder.append("    <").append(connectorName).append(".").append(operationName).append(" configKey=\"CONNECTION_NAME\">\n");
            
            // Add sample parameters
            for (Parameter param : parameters) {
                markdownBuilder.append("        <").append(param.name).append(">")
                              .append(param.defaultValue != null ? param.defaultValue : "")
                              .append("</").append(param.name).append(">\n");
            }
            
            // Add response variable parameters
            markdownBuilder.append("        <responseVariable>").append(connectorName).append("_").append(operationName).append("_1</responseVariable>\n");
            markdownBuilder.append("        <overwriteBody>false</overwriteBody>\n");
            
            markdownBuilder.append("    </").append(connectorName).append(".").append(operationName).append(">\n");
            markdownBuilder.append("    ```\n");
            
            // Add sample response if not a void operation
            if (!isVoidOperation(operationName)) {
                markdownBuilder.append("    \n");
                markdownBuilder.append("    **Sample response**\n");
                markdownBuilder.append("    \n");
                markdownBuilder.append("    The response received will be stored in the variable `")
                              .append(connectorName).append("_").append(operationName).append("_1")
                              .append("` as a JSON object. The following is a sample response.\n");
                markdownBuilder.append("    \n");
                markdownBuilder.append("    ```json\n");
                markdownBuilder.append("    {\n");
                markdownBuilder.append("        \"success\":true\n");
                markdownBuilder.append("    }\n");
                markdownBuilder.append("    ```\n");
            }
            
            markdownBuilder.append("\n");
        }
        
        // Add note for examples
        markdownBuilder.append("**Note**: For more information on how this works in an actual scenario, see [")
                      .append(StringUtils.capitalize(connectorName))
                      .append(" Connector Example]({{base_path}}/reference/connectors/")
                      .append(connectorName.toLowerCase())
                      .append("-connector/")
                      .append(connectorName.toLowerCase())
                      .append("-connector-example/).");
        
        // Write the generated markdown to a file
        File markdownFile = new File(docsDirectory, connectorName.toLowerCase() + "-connector-reference.md");
        FileUtils.writeStringToFile(markdownFile, markdownBuilder.toString(), StandardCharsets.UTF_8);
    }
    
    /**
     * Extracts parameters from a UI schema.
     *
     * @param schema The UI schema.
     * @return List of parameters.
     */
    private List<Parameter> extractParameters(JsonObject schema) {
        List<Parameter> parameters = new ArrayList<>();
        
        if (schema.has(Constants.ELEMENTS) && schema.get(Constants.ELEMENTS).isJsonArray()) {
            JsonArray elements = schema.get(Constants.ELEMENTS).getAsJsonArray();
            extractParametersFromElements(elements, parameters, null);
        }
        
        return parameters;
    }
    
    /**
     * Recursively extracts parameters from UI schema elements.
     *
     * @param elements The elements array.
     * @param parameters The list to add parameters to.
     * @param currentGroup The current group name.
     */
    private void extractParametersFromElements(JsonArray elements, List<Parameter> parameters, String currentGroup) {
        for (int i = 0; i < elements.size(); i++) {
            JsonElement element = elements.get(i);
            
            if (!element.isJsonObject()) {
                continue;
            }
            
            JsonObject obj = element.getAsJsonObject();
            
            if (!obj.has(Constants.TYPE)) {
                continue;
            }
            
            String type = obj.get(Constants.TYPE).getAsString();
            
            if (Constants.ATTRIBUTE_TYPE.equals(type) && obj.has(Constants.VALUE)) {
                JsonObject value = obj.get(Constants.VALUE).getAsJsonObject();
                Parameter param = new Parameter();
                param.name = getStringValue(value, Constants.NAME_ATTRIBUTE);
                param.displayName = getStringValue(value, "displayName");
                param.description = getStringValue(value, "helpTip");
                param.defaultValue = getStringValue(value, "defaultValue");
                param.required = "true".equalsIgnoreCase(getStringValue(value, "required"));
                param.group = currentGroup != null ? currentGroup : "default";
                parameters.add(param);
            } else if ("attributeGroup".equals(type) && obj.has(Constants.VALUE)) {
                JsonObject value = obj.get(Constants.VALUE).getAsJsonObject();
                String groupName = getStringValue(value, "groupName");
                
                if (value.has(Constants.ELEMENTS) && value.get(Constants.ELEMENTS).isJsonArray()) {
                    JsonArray nestedElements = value.get(Constants.ELEMENTS).getAsJsonArray();
                    extractParametersFromElements(nestedElements, parameters, groupName);
                }
            }
        }
    }
    
    /**
     * Groups parameters by their section.
     *
     * @param parameters The list of parameters.
     * @return Map of section name to parameters in that section.
     */
    private Map<String, List<Parameter>> groupParametersBySection(List<Parameter> parameters) {
        Map<String, List<Parameter>> grouped = new LinkedHashMap<String, List<Parameter>>();
        
        for (Parameter param : parameters) {
            String group = param.group != null ? param.group : "default";
            if (!grouped.containsKey(group)) {
                grouped.put(group, new ArrayList<Parameter>());
            }
            grouped.get(group).add(param);
        }
        
        return grouped;
    }
    
    /**
     * Gets a string value from a JSON object, returning null if not found or not a string.
     *
     * @param obj The JSON object.
     * @param key The key to get.
     * @return The string value or null.
     */
    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }
        return null;
    }
    
    /**
     * Extracts the first sentence from a text.
     *
     * @param text The input text.
     * @return The first sentence.
     */
    private String extractFirstSentence(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        int endPos = text.indexOf(". ");
        if (endPos > 0) {
            return text.substring(0, endPos + 1);
        }
        
        return text;
    }
    
    /**
     * Strips HTML tags from text.
     *
     * @param html The HTML text.
     * @return The text without HTML tags.
     */
    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        // Simple HTML tag removal, can be enhanced
        return html.replaceAll("<[^>]*>", "");
    }
    
    /**
     * Checks if an operation is void (doesn't return anything).
     *
     * @param operationName The operation name.
     * @return True if the operation is void.
     */
    private boolean isVoidOperation(String operationName) {
        // List of common void operations
        List<String> voidOperations = Arrays.asList("expungeFolder");
        return voidOperations.contains(operationName);
    }
    
    /**
     * Parameter class to store parameter information.
     */
    private static class Parameter {
        String name;
        String displayName;
        String description;
        String defaultValue;
        boolean required;
        String group;
    }
}
