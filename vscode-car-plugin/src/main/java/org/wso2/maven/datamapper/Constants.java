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

import java.io.File;

class Constants {
    static final String REGISTRY_DIR_NAME = "registry";
    static final String GOV_DIR_NAME = "gov";
    static final String DATA_MAPPER_DIR_NAME = "datamapper";
    static final String TARGET_DIR_NAME = "target";
    static final String PACKAGE_JSON_FILE_NAME = "package.json";
    static final String TS_CONFIG_FILE_NAME = "tsconfig.json";
    static final String WEBPACK_CONFIG_FILE_NAME = "webpack.config.js";
    static final String POM_FILE_NAME = "pom.xml";
    static final String BUNDLED_JS_FILE_NAME = "bundle.js";
    static final String OS_WINDOWS = "windows";
    static final String NODE_VERSION = "v14.17.3";
    static final String NPM_VERSION = "6.14.13";
    static final String FRONTEND_MVN_PLUGIN_GROUP_ID = "com.github.eirslett";
    static final String FRONTEND_MVN_PLUGIN_ARTIFACT_ID = "frontend-maven-plugin";
    static final String FRONTEND_MVN_PLUGIN_VERSION = "1.12.0";
    static final String INSTALL_NODE_AND_NPM = "install-node-and-npm";
    static final String NPM_INSTALL = "install";
    static final String BUILD_COMMAND = "build";
    static final String RUN_BUILD = "run build";
    static final String RUN_GENERATE = "run generate";
    static final String NPM_COMMAND = "npm";
    static final String EXEC_MVN_PLUGIN_GROUP_ID = "org.codehaus.mojo";
    static final String EXEC_MVN_PLUGIN_ARTIFACT_ID = "exec-maven-plugin";
    static final String EXEC_MVN_PLUGIN_VERSION = "1.6.0";
    static final String EXEC_COMMAND = "exec";
    static final String SCHEMA_GENERATOR = "schemaGenerator.ts";
    static final String DATA_MAPPER_DIR_PATH = REGISTRY_DIR_NAME + File.separator
        + GOV_DIR_NAME + File.separator + DATA_MAPPER_DIR_NAME;
    static final String INSTALL_NODE_AND_NPM_GOAL = FRONTEND_MVN_PLUGIN_GROUP_ID + ":"
        + FRONTEND_MVN_PLUGIN_ARTIFACT_ID + ":" + FRONTEND_MVN_PLUGIN_VERSION + ":" + INSTALL_NODE_AND_NPM;
    static final String NPM_GOAL = FRONTEND_MVN_PLUGIN_GROUP_ID + ":"
        + FRONTEND_MVN_PLUGIN_ARTIFACT_ID + ":" + FRONTEND_MVN_PLUGIN_VERSION + ":" + NPM_COMMAND;
    static final String EXEC_GOAL = EXEC_MVN_PLUGIN_GROUP_ID + ":"
        + EXEC_MVN_PLUGIN_ARTIFACT_ID + ":" + EXEC_MVN_PLUGIN_VERSION + ":" + EXEC_COMMAND;
    static final String NPM_RUN_BUILD_GOAL = EXEC_GOAL + "@" + BUILD_COMMAND;
    public static final String PREPEND_NODE_CONFIG = "config set scripts-prepend-node-path auto";

    private Constants() {
    }
}
