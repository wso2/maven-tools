/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

/**
 * Constants used in the connector plugin.
 */
public class Constants {

    // File and directory names
    public static final String DEFAULT_TARGET_FOLDER = "target";
    public static final String CONNECTOR_XML = "connector.xml";
    public static final String COMPONENT_XML = "component.xml";
    public static final String DEPENDENCY_XML = "dependency.xml";
    public static final String CLASSES = "classes";
    public static final String ICON_SMALL_GIF = "icon-small.gif";
    public static final String ICON_SMALL_PNG = "icon-small.png";
    public static final String ICON = "icon";
    public static final String CONFIG_DIR = "config";
    public static final String UISCHEMA_DIR = "uischema";
    public static final String OUTPUTSCHEMA_DIR = "outputschema";
    
    // File extensions
    public static final String XML_EXTENSION = ".xml";
    public static final String JSON_EXTENSION = ".json";
    
    // XML elements and attributes
    public static final String PARAMETER_ELEMENT = "parameter";
    public static final String NAME_ATTRIBUTE = "name";
    
    // JSON keys
    public static final String CONNECTOR_NAME = "connectorName";
    public static final String OPERATION_NAME = "operationName";
    public static final String CONNECTION_NAME = "connectionName";
    public static final String ELEMENTS = "elements";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String ATTRIBUTE_TYPE = "attribute";
    
    // Operation names
    public static final String INIT_OPERATION = "init";
}
