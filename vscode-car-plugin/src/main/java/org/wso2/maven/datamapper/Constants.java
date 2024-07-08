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

import java.io.File;

class Constants {
    static final String REGISTRY_DIR_NAME = "registry";
    static final String GOV_DIR_NAME = "gov";
    static final String DATA_MAPPER_DIR_NAME = "datamapper";
    static final String DATA_MAPPER_DIR_PATH = REGISTRY_DIR_NAME + File.separator
            + GOV_DIR_NAME + File.separator + DATA_MAPPER_DIR_NAME;

    private Constants() {
    }
}
