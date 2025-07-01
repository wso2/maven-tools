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
import org.eclipse.lemminx.customservice.synapse.ConnectorStatusNotification;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;

import java.util.concurrent.CompletableFuture;

public class MockSynapseLanguageClientAPI implements SynapseLanguageClientAPI {

    @Override
    public void addConnectorStatus(ConnectorStatusNotification connectorStatusNotification) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public void removeConnectorStatus(ConnectorStatusNotification connectorStatusNotification) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public void tryoutLog(String s) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public void telemetryEvent(Object o) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public void showMessage(MessageParams messageParams) {

        // This is a mock implementation, so we do not perform any action here.
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams showMessageRequestParams) {

        return null;
    }

    @Override
    public void logMessage(MessageParams messageParams) {

        // This is a mock implementation, so we do not perform any action here.
    }
}
