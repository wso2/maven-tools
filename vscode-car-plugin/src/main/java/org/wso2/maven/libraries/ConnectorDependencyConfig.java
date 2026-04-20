/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.maven.libraries;

import java.util.List;

/**
 * Holds the configuration for a single connector in connector-config.json.
 */
public class ConnectorDependencyConfig {

    /**
     * When true, this connector's ZIP will not be packed into the CAR.
     */
    private boolean omit;

    /**
     * When true, no driver JARs for this connector will be packed into the CAR.
     */
    private boolean omitAllDrivers;

    private List<DependencyOverride> dependencies;

    public boolean isOmit() {
        return omit;
    }

    public boolean isOmitAllDrivers() {
        return omitAllDrivers;
    }

    public List<DependencyOverride> getDependencies() {
        return dependencies;
    }
}
