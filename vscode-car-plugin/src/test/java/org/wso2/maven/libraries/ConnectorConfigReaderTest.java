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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConnectorConfigReaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // ConnectorConfigReader.read()
    // -------------------------------------------------------------------------

    @Test
    public void testRead_ReturnsNullWhenFileAbsent() {
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());
        assertNull(config);
    }

    @Test
    public void testRead_ParsesValidJson() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        assertNotNull(config);
        assertEquals("1.0", config.getVersion());
        assertNotNull(config.getConnectors());
        ConnectorDependencyConfig fileCfg = config.getConnectors().get("mi-connector-file");
        assertNotNull(fileCfg);
        assertEquals(1, fileCfg.getDependencies().size());
        DependencyOverride override = fileCfg.getDependencies().get(0);
        assertEquals("MYSQL", override.getConnectionType());
        assertEquals("9.0.0", override.getVersion());
        assertNull(override.getGroupId());
        assertNull(override.getOmit());
    }

    @Test
    public void testRead_ReturnsNullOnMalformedJson() throws IOException {
        writeConfigFile("{ this is not valid json }");
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());
        assertNull(config);
    }

    @Test
    public void testRead_ParsesOmitTrue() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-db\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"ORACLE\", \"omit\": true }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());
        assertNotNull(config);
        DependencyOverride override = config.getConnectors().get("mi-connector-db").getDependencies().get(0);
        assertEquals(Boolean.TRUE, override.getOmit());
    }

    // -------------------------------------------------------------------------
    // ConnectorConfigReader.findOverride() — match by connectionType
    // -------------------------------------------------------------------------

    @Test
    public void testFindOverride_MatchByConnectionType() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-file", "MYSQL", "mysql", "mysql-connector-java");

        assertNotNull(result);
        assertEquals("9.0.0", result.getVersion());
    }

    @Test
    public void testFindOverride_ConnectionTypeMatchIsCaseInsensitive() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MySQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-file", "MYSQL", null, null);

        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // ConnectorConfigReader.findOverride() — match by groupId + artifactId
    // -------------------------------------------------------------------------

    @Test
    public void testFindOverride_MatchByCoordinatesWhenNoConnectionType() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-http\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"groupId\": \"com.example\", \"artifactId\": \"my-lib\", \"version\": \"2.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        // Descriptor dep has no connectionType; override matched by groupId+artifactId
        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-http", null, "com.example", "my-lib");

        assertNotNull(result);
        assertEquals("2.0.0", result.getVersion());
    }

    @Test
    public void testFindOverride_ConnectionTypeTakesPriorityOverCoordinates() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" },\n" +
                "        { \"groupId\": \"mysql\", \"artifactId\": \"mysql-connector-java\", \"version\": \"8.0.33\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        // When connectionType matches, that entry is returned first
        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-file", "MYSQL", "mysql", "mysql-connector-java");

        assertNotNull(result);
        assertEquals("9.0.0", result.getVersion()); // connectionType match wins
    }

    // -------------------------------------------------------------------------
    // ConnectorConfigReader.findOverride() — no match cases
    // -------------------------------------------------------------------------

    @Test
    public void testFindOverride_ReturnsNullForUnknownConnector() throws IOException {
        writeConfigFile(
                "{ \"version\": \"1.0\", \"connectors\": {} }"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-unknown", "MYSQL", "mysql", "mysql-connector-java");

        assertNull(result);
    }

    @Test
    public void testFindOverride_ReturnsNullWhenConfigIsNull() {
        assertNull(ConnectorConfigReader.findOverride(null, "mi-connector-file", "MYSQL", "g", "a"));
    }

    @Test
    public void testFindOverride_ReturnsNullWhenConnectionTypeMismatch() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        ConnectorConfig config = ConnectorConfigReader.read(tempFolder.getRoot().getAbsolutePath());

        DependencyOverride result = ConnectorConfigReader.findOverride(
                config, "mi-connector-file", "POSTGRES", null, null);

        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // ConnectorDependencyResolver.extractArtifactIdFromZipName()
    // -------------------------------------------------------------------------

    @Test
    public void testExtractArtifactId_StandardSemver() {
        assertEquals("mi-connector-file",       ConnectorDependencyResolver.extractArtifactIdFromZipName("mi-connector-file-4.0.36.zip"));
        assertEquals("mi-connector-amazonsqs",  ConnectorDependencyResolver.extractArtifactIdFromZipName("mi-connector-amazonsqs-2.0.2.zip"));
        assertEquals("mi-connector-http",       ConnectorDependencyResolver.extractArtifactIdFromZipName("mi-connector-http-0.1.8.zip"));
    }

    @Test
    public void testExtractArtifactId_WithoutZipExtension() {
        assertEquals("mi-connector-file", ConnectorDependencyResolver.extractArtifactIdFromZipName("mi-connector-file-4.0.36"));
    }

    @Test
    public void testExtractArtifactId_FallbackWhenNoVersionSuffix() {
        // If no semver suffix, the full name is returned unchanged
        String result = ConnectorDependencyResolver.extractArtifactIdFromZipName("custom-connector.zip");
        assertEquals("custom-connector", result);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void writeConfigFile(String content) throws IOException {
        File dir = new File(tempFolder.getRoot(), "src/main/wso2mi/resources/connectors");
        dir.mkdirs();
        try (FileWriter w = new FileWriter(new File(dir, "connector-config.json"))) {
            w.write(content);
        }
    }
}
