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

import com.google.gson.Gson;
import org.wso2.maven.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and queries {@code connector-config.json} from the project source tree.
 * <p>
 * Expected location: {@code src/main/wso2mi/resources/connectors/connector-config.json} relative to the project base directory.
 */
public class ConnectorConfigReader {

    private static final Logger LOGGER = Logger.getLogger(ConnectorConfigReader.class.getName());

    private ConnectorConfigReader() {
    }

    /**
     * Reads and parses {@code connector-config.json} from the given project base directory.
     *
     * @param projectBasePath absolute path to the Maven project root (where pom.xml lives)
     * @return parsed {@link ConnectorConfig}, or {@code null} if the file does not exist or cannot be parsed
     */
    public static ConnectorConfig read(String projectBasePath) {

        File configFile = new File(projectBasePath, Constants.CONNECTOR_CONFIG_FILE);
        if (!configFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(configFile)) {
            ConnectorConfig config = new Gson().fromJson(reader, ConnectorConfig.class);
            LOGGER.log(Level.INFO, "Loaded connector-config.json from: " + configFile.getAbsolutePath());
            return config;
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to read connector-config.json: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds the best-matching {@link DependencyOverride} for the given dependency.
     * <p>
     * Matching priority:
     * <ol>
     *   <li>Match by {@code connectionType} if non-null and the override has a matching connectionType.</li>
     *   <li>Match by {@code groupId + artifactId} if the override has no connectionType but coordinates match.</li>
     * </ol>
     *
     * @param config             the parsed connector config (may be null)
     * @param connectorArtifactId Maven artifactId of the connector ZIP (e.g. "mi-connector-file")
     * @param connectionType     connectionType from descriptor.yml (may be null)
     * @param groupId            dependency groupId from descriptor.yml
     * @param artifactId         dependency artifactId from descriptor.yml
     * @return the matching override, or {@code null} if none found
     */
    public static DependencyOverride findOverride(ConnectorConfig config, String connectorArtifactId,
                                                  String connectionType, String groupId, String artifactId) {

        if (config == null || config.getConnectors() == null) {
            return null;
        }
        ConnectorDependencyConfig connectorCfg = config.getConnectors().get(connectorArtifactId);
        if (connectorCfg == null) {
            return null;
        }
        List<DependencyOverride> overrides = connectorCfg.getDependencies();
        if (overrides == null || overrides.isEmpty()) {
            return null;
        }

        // First pass: match by connectionType
        if (connectionType != null) {
            for (DependencyOverride override : overrides) {
                if (connectionType.equalsIgnoreCase(override.getConnectionType())) {
                    return override;
                }
            }
        }

        // Second pass: match by groupId + artifactId (for overrides without connectionType)
        if (groupId != null && artifactId != null) {
            for (DependencyOverride override : overrides) {
                if (override.getConnectionType() == null
                        && groupId.equals(override.getGroupId())
                        && artifactId.equals(override.getArtifactId())) {
                    return override;
                }
            }
        }
        return null;
    }
}
