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

package org.wso2.maven.syntax.validator;

import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.connectors.AbstractConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.SchemaGenerate;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.parser.ConnectorDownloadManager;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class SchemaLoader {

    private static final Path CONNECTOR_XSD_PATH = Path.of("mediators").resolve("connectors.xsd");

    public static Path loadSchema(String projectPath)
            throws IOException, URISyntaxException, InvalidConfigurationException {

        Path catalogPath = Utils.copyXSDFiles(projectPath);
        loadConnectorSchema(projectPath, catalogPath);
        return catalogPath;
    }

    private static void loadConnectorSchema(String projectPath, Path catalogPath) throws InvalidConfigurationException {

        ConnectorHolder connectorHolder = loadConnectors(projectPath);

        //Generate xsd schema for the available connectors and write it to the schema file.
        String connectorPath = catalogPath.resolve(CONNECTOR_XSD_PATH).toString();
        SchemaGenerate.generate(connectorHolder, connectorPath);
    }

    private static ConnectorHolder loadConnectors(String projectPath) throws InvalidConfigurationException {

        ConnectorDownloadManager.downloadConnectors(projectPath);

        ConnectorHolder connectorHolder = ConnectorHolder.getInstance();
        AbstractConnectorLoader connectorLoader =
                new ConnectorLoader(new MockSynapseLanguageClientAPI(), connectorHolder, new InboundConnectorHolder());
        connectorLoader.init(projectPath);
        connectorLoader.loadConnector();
        return connectorHolder;
    }
}
