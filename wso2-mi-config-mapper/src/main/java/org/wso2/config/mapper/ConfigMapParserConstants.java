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

class ConfigMapParserConstants {

    private ConfigMapParserConstants() {}

    static final String DEPLOYMENT_TOML_PATH = "deployment.toml";
    static final String PARSER_OUTPUT_PATH = "CarbonHome";
    static final String DOCKER_FILE = "Dockerfile";
    static final String METADATA_DIR = ".metadata";
    static final String CONF_DIR = "conf";
    static final String RESOURCES_PATH = "ConfigMapResources";
    static final String TEMPLATES_ZIP_FILE = "templates.zip";
    static final String METADATA_CONFIG_PROPERTIES_FILE = "metadata_config.properties";
    static final String TEMPLATES_URL = "http://product-dist.wso2.com/p2/templates/";

    static final String DOCKER_MI_DIR_PATH = " /home/wso2carbon/wso2mi/";
    static final String DOCKER_FROM_TAG = "FROM wso2/micro-integrator";
    static final String DOCKER_COMPOSITE_COPY_TAG = "COPY CompositeApps";
    static final String DOCKER_LIBS_COPY_TAG = "COPY Libs";
    static final String DOCKER_RESOURCES_COPY_TAG = "COPY Resources";
    static final String DOCKER_ENV_TAG = "ENV ";
    static final String METADATA_DIR_PATH = DOCKER_MI_DIR_PATH + "repository/resources/conf/.metadata";
    static final String DOCKER_COPY_FILE = "COPY  --chown=wso2carbon:wso2 ";
    static final String DOCKER_MAKE_DIR = "RUN mkdir";
}
