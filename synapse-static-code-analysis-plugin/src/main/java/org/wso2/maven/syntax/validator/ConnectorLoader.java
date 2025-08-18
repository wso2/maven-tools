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

import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.AbstractConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectorLoader extends AbstractConnectorLoader {

    private static final String CONNECTOR_EXTRACT_FOLDER = "extracted-resources";
    private static final Logger log = Logger.getLogger(ConnectorLoader.class.getName());

    public ConnectorLoader(SynapseLanguageClientAPI languageClient, ConnectorHolder connectorHolder,
                           InboundConnectorHolder inboundConnectorHolder) {

        super(languageClient, connectorHolder, inboundConnectorHolder);
    }

    @Override
    protected File getConnectorExtractFolder() {

        return Path.of(projectUri, Constant.TARGET, CONNECTOR_EXTRACT_FOLDER, Constant.CONNECTORS).toFile();
    }

    @Override
    protected void copyToProjectIfNeeded(List<File> connectorZips) {

        // No need to copy connectors to the project as this is for the syntax validation purpose only.
    }

    @Override
    protected boolean canContinue(File connectorExtractFolder) {

        try {
            if (!connectorExtractFolder.exists()) {
                return connectorExtractFolder.mkdirs();
            }
            return true;
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create connector extract folder", e);
            return false;
        }
    }

    @Override
    protected void cleanOldConnectors(File connectorExtractFolder, List<File> connectorZips) {

        // No need to clean old connectors as this is for the syntax validation purpose only.
    }

    private Path getConnectorDownloadPath() {

        String projectId = new File(projectUri).getName() + "_" + Utils.getHash(projectUri);
        return Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI,
                Constant.CONNECTORS, projectId, Constant.DOWNLOADED);
    }

    @Override
    protected void setConnectorsZipFolderPath(String projectRoot) {

        connectorsZipFolderPath.add(Path.of(projectRoot, Constant.SRC, Constant.MAIN, Constant.WSO2MI,
                Constant.RESOURCES, Constant.CONNECTORS).toString());
        connectorsZipFolderPath.add(getConnectorDownloadPath().toString());
    }
}
