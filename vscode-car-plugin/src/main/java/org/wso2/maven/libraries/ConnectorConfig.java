/*
 * Copyright (c) 2025, WSO2 LLC (http://www.wso2.com).
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

package org.wso2.maven.libraries;

import java.util.Map;

/**
 * Root model for {@code src/main/wso2mi/connector-config.json}.
 * The {@code connectors} map is keyed by the connector Maven artifactId.
 */
public class ConnectorConfig {

    private String version;

    /**
     * When true, no driver JARs will be packed into the CAR for any connector.
     * Overrides all per-connector and per-dependency settings.
     */
    private Boolean omitAllDrivers;

    /**
     * When true, no connector ZIPs will be packed into the CAR.
     */
    private Boolean omitAllConnectors;

    /** Key: connector artifactId (e.g. "mi-connector-file"). */
    private Map<String, ConnectorDependencyConfig> connectors;

    public String getVersion() {
        return version;
    }

    public Boolean getOmitAllDrivers() {
        return omitAllDrivers;
    }

    public Boolean getOmitAllConnectors() {
        return omitAllConnectors;
    }

    public Map<String, ConnectorDependencyConfig> getConnectors() {
        return connectors;
    }
}
