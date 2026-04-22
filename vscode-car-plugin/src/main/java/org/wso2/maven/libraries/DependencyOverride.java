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

/**
 * Represents a single dependency override entry in connector-config.json.
 * <p>
 * A match is attempted first by {@code connectionType} (if present in the descriptor.yml
 * dependency), then by {@code groupId + artifactId}. Any field left null falls through to
 * the descriptor.yml default.
 */
public class DependencyOverride {

    /** Matches the {@code connectionType} field in descriptor.yml. Optional. */
    private String connectionType;

    /** Override groupId. Null means keep the descriptor.yml default. */
    private String groupId;

    /** Override artifactId. Null means keep the descriptor.yml default. */
    private String artifactId;

    /** Override version. Null means keep the descriptor.yml default. */
    private String version;

    /**
     * When true, this dependency is excluded from the CAR entirely.
     * Mutually exclusive with version/groupId/artifactId overrides.
     */
    private boolean omit;

    /**
     * Absolute path to a local JAR file. When set, Maven resolution is skipped and this JAR
     * is copied directly into the CAR lib directory.
     */
    private String localPath;

    public String getConnectionType() {
        return connectionType;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public boolean isOmit() {
        return omit;
    }

    public String getLocalPath() {
        return localPath;
    }
}
