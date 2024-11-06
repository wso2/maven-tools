/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.handler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class HandlerConstants {

    // Application artifact types
    public static final String SEQUENCE_TYPE = "synapse/sequence";
    public static final String ENDPOINT_TYPE = "synapse/endpoint";
    public static final String PROXY_SERVICE_TYPE = "synapse/proxy-service";
    public static final String LOCAL_ENTRY_TYPE = "synapse/local-entry";
    public static final String EVENT_SOURCE_TYPE = "synapse/event-source";
    public static final String TASK_TYPE = "synapse/task";
    public static final String MESSAGE_STORE_TYPE = "synapse/message-store";
    public static final String MESSAGE_PROCESSOR_TYPE="synapse/message-processors";
    public static final String MEDIATOR_TYPE = "lib/synapse/mediator";
    public static final String API_TYPE = "synapse/api";
    public static final String TEMPLATE_TYPE = "synapse/template";
    public static final String INBOUND_ENDPOINT_TYPE = "synapse/inbound-endpoint";
    public static final String SYNAPSE_LIBRARY_TYPE = "synapse/lib";
    public static final String DATA_SERVICE_TYPE = "service/dataservice";
    public static final String REGISTRY_RESOURCE_TYPE = "registry/resource";
    public static final String OTHER_TYPE = "other";

    // Deployment folders for synapse artifacts
    public static final String SEQUENCES_FOLDER = "sequences";
    public static final String ENDPOINTS_FOLDER = "endpoints";
    public static final String PROXY_SERVICES_FOLDER = "proxy-services";
    public static final String LOCAL_ENTRIES_FOLDER = "local-entries";
    public static final String EVENTS_FOLDER = "event-sources";
    public static final String TASKS_FOLDER = "tasks";
    public static final String MESSAGE_STORE_FOLDER="message-stores";
    public static final String MESSAGE_PROCESSOR_FOLDER="message-processors";
    public static final String SYNAPSE_IMPORTS_FOLDER = "imports";
    public static final String APIS_FOLDER = "api";
    public static final String TEMPLATES_FOLDER = "templates";
    public static final String MEDIATORS_FOLDER = "class-mediators";
    public static final String INBOUND_ENDPOINT_FOLDER = "inbound-endpoints";
    public static final String SYNAPSE_LIBS = "synapse-libs";
    public static final String REGISTRY_RESOURCES_FOLDER = "registry";
    public static final String RESOURCES_FOLDER = "resources";

    // {CARBON_HOME}/synapse-configs/
    static final Path SYNAPSE_CONFIGS_DIR_PATH = Paths.get("synapse-configs");
}
