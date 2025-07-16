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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.maven;

import java.io.File;

public class Constants {
    public static final String ARTIFACTS = "artifacts";
    public static final String ARTIFACT = "artifact";
    static final String KEY = "key";
    static final String API_DIR_NAME = "apis";
    static final String ENDPOINTS_DIR_NAME = "endpoints";
    static final String INBOUND_DIR_NAME = "inbound-endpoints";
    static final String LOCAL_ENTRIES_DIR_NAME = "local-entries";
    static final String MSG_PROCESSORS_DIR_NAME = "message-processors";
    static final String MSG_STORES_DIR_NAME = "message-stores";
    static final String PROXY_SERVICES_DIR_NAME = "proxy-services";
    static final String SEQUENCES_DIR_NAME = "sequences";
    static final String TASKS_DIR_NAME = "tasks";
    static final String TEMPLATES_DIR_NAME = "templates";
    static final String DATASOURCE_DIR_NAME = "data-sources";
    static final String CONF_DIR_NAME = "conf";
    static final String DATASERVICES_DIR_NAME = "data-services";
    static final String METADATA_DIR_NAME = "metadata";
    static final String REGISTRY_DIR_NAME = "registry";
    static final String API_TYPE = "synapse/api";
    static final String ENDPOINT_TYPE = "synapse/endpoint";
    static final String INBOUND_TYPE = "synapse/inbound-endpoint";
    static final String LOCAL_ENTRY_TYPE = "synapse/local-entry";
    static final String MESSAGE_PROCESSOR_TYPE = "synapse/message-processors";
    static final String MESSAGE_STORE_TYPE = "synapse/message-store";
    static final String PROXY_SERVICE_TYPE = "synapse/proxy-service";
    static final String SEQUENCE_TYPE = "synapse/sequence";
    static final String TASK_TYPE = "synapse/task";
    static final String TEMPLATE_TYPE = "synapse/template";
    static final String DATASOURCE_TYPE = "datasource/datasource";
    static final String DATASERVICE_TYPE = "service/dataservice";
    static final String REG_RESOURCE_TYPE = "registry/resource";
    static final String PROPERTY_TYPE = "config/property";
    static final String CONNECTOR_TYPE = "synapse/lib";
    static final String METADATA_TYPE = "synapse/metadata";
    static final String CAPP_TYPE = "carbon/application";
    static final String CLASS_MEDIATOR_TYPE = "lib/synapse/mediator";
    static final String CONNECTOR_DEPENDENCY_TYPE = "lib/connector/dependency";
    static final String ARTIFACTS_FOLDER_PATH = "src" + File.separator + "main" + File.separator
            + "wso2mi" + File.separator + "artifacts";
    static final String SERVER_ROLE_EI = "EnterpriseIntegrator";
    static final String SERVER_ROLE_DSS = "EnterpriseServiceBus";
    static final String GOV_REG_PREFIX = "/_system/governance";
    static final String CONF_REG_PREFIX = "/_system/config";
    static final String GOV_MI_RESOURCES_PREFIX = "/_system/governance/mi-resources";
    static final String GOV_FOLDER = "gov";
    static final String CONF_FOLDER = "conf";
    static final String REG_INFO_FILE = "registry-info.xml";
    static final String TYPE = "type";
    public static final String CAR_TYPE = "car";
    public static final String SERVER_ROLE = "serverRole";
    static final String DESCRIPTION = "description";
    static final String FILE = "file";
    static final String ITEM = "item";
    static final String COLLECTION = "collection";
    static final String DIRECTORY = "directory";
    static final String PATH = "path";
    public static final String INCLUDE = "include";
    static final String RESOURCES = "resources";
    static final String ARCHIVE_EXCEPTION_MSG = "Error occurred while creating CAR file.";
    static final String ARTIFACT_XML = "artifact.xml";
    static final String PROPERTY_FILE = "config.properties";
    static final String PROPERTY_FILE_NAME = "config";
    static final String EMPTY_STRING = "";
    static final String MAIN_SEQUENCE = "mainSequence";
    static final String TEMP_TARGET_DIR_NAME = "tmp";
    static final String API_DEFINITION_DIR = "api-definitions";
    static final String OS_WINDOWS = "windows";
    public static final String POM_FILE = "pom.xml";
    public static final String DEFAULT_TARGET_FOLDER = "target";
    public static final String DEPENDENCY = "dependency";
    public static final String DESCRIPTOR_YAML = "descriptor.yml";
    public static final String EXTRACTED_CONNECTORS = "extracted-connectors";
    public static final String LIBS = "libs";
    public static final String REPOSITORIES = "repositories";
    public static final String DEPENDENCIES = "dependencies";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String GROUP_ID = "groupId";
    public static final String VERSION = "version";
    public static final String PROXY = "proxy";
    public static final String PROXY_FILE_NAME_SUFFIX = "_proxy_metadata.yaml";
    public static final String DATA_SERVICE_FILE_NAME_SUFFIX = "_data_service_metadata.yaml";
    public static final String PROXY_WITH_UNDERSCORE = "_proxy";
    public static final String DATA_SERVICE_WITH_UNDERSCORE = "_data_service";
    public static final String API = "api";
    public static final String DATA_SERVICE = "dataService";
    public static final String ZIP_EXTENSION = ".zip";
    public static final String CAR_EXTENSION = ".car";
    public static final String CLASS_MEDIATORS = "_class_mediators";
    public static final String CONNECTOR_XML = "connector.xml";
    public static final String COMPONENT = "component";
    public static final String NAME = "name";
    public static final String PACKAGE = "package";
    public static final String CONNECTOR = "connector";
    public static final String CONNECTORS_DIR_NAME = "connectors";
    public static final String INBOUND_CONNECTORS_DIR_NAME = "inbound-connectors";
    public static final String INBOUND_CONNECTORS_PREFIX = "mi-inbound-";
    public static final String CONNECTION_TYPE = "connectionType";
    public static final String ID = "id";
    public static final String PROJECT = "project";
    public static final String PROPERTIES = "properties";
    public static final String FAT_CAR_ENABLE = "fat.car.enable";
    public static final String PROJECT_RUNTIME_VERSION = "project.runtime.version";
    public static final String RUNTIME_VERSION_440 = "4.4.0";
    public static final String LOCAL_ENTRIES_FOLDER_PATH = ARTIFACTS_FOLDER_PATH + File.separator + LOCAL_ENTRIES_DIR_NAME;
    public static final String RESOURCES_FOLDER_PATH = "src" + File.separator + "main" + File.separator
            + "wso2mi" + File.separator + "resources";
    public static final String FAT_CAR_ENABLE_PROPERTY = "fat.car.enable";
    public static final String DESCRIPTOR_XML = "descriptor.xml";
    public static final String CONFIG_PROPERTIES_FILE = "config.properties";
    public static final String ARTIFACTS_XML_FILE = "artifacts.xml";
    public static final String METADATA_XML_FILE = "metadata.xml";
    public static final String CONFIG_DIR_PREFIX = "config_";
    public static final String METADATA_DIR = "metadata";
    public static final String HYPHEN = "-";
    public static final String UNDERSCORE = "_";
    public static final String COLON = ":";
    public static final Character DOT_CHAR = '.';
    public static final String USER_HOME = "user.home";
    public static final String REPOSITORY = "repository";
    public static final String M2 = ".m2";

    private Constants() {
    }
}
