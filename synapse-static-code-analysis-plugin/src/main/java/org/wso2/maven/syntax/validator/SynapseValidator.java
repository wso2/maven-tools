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

import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.diagnostics.XMLValidator;
import org.eclipse.lemminx.extensions.contentmodel.uriresolver.XMLCatalogResolverExtension;
import org.eclipse.lemminx.extensions.xerces.LSPXMLEntityResolver;
import org.eclipse.lemminx.services.extensions.diagnostics.DiagnosticsResult;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.wso2.maven.Constant;
import org.wso2.maven.VerboseLogger;
import org.wso2.maven.reporter.Reporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SynapseValidator {

    private static final CancelChecker NULL_CANCEL_CHECKER = () -> {

    };

    private final String projectPath;
    private final String[] filesToInclude;
    private final Reporter reporter;
    private VerboseLogger verboseLogger;

    public SynapseValidator(String projectPath, String[] filesToInclude, Reporter reporter) {

        this.projectPath = projectPath;
        this.filesToInclude = filesToInclude;
        this.reporter = reporter;
    }

    public void validate()
            throws IOException, URISyntaxException, InvalidConfigurationException {

        verboseLogger.info("Starting Synapse XML validation for project: " + projectPath);
        verboseLogger.info("Loading XSD schema catalog for project");
        Path catalogPath = SchemaLoader.loadSchema(projectPath);
        XMLCatalogResolverExtension xmlCatalogResolverExtension = new XMLCatalogResolverExtension();
        xmlCatalogResolverExtension.setCatalogs(new String[]{catalogPath.resolve(Constant.CATALOG_XML).toString()});
        URIResolverExtensionManager uriResolverExtensionManager = new URIResolverExtensionManager();
        uriResolverExtensionManager.registerResolver(xmlCatalogResolverExtension);
        ContentModelManager contentModelManager = new ContentModelManager(uriResolverExtensionManager);
        List<Path> artifactFiles = filterArtifactFiles();
        for (Path artifactFile : artifactFiles) {
            verboseLogger.info("Validating syntax for file: " + artifactFile);
            DiagnosticsResult diagnosticsResult = new DiagnosticsResult(null);
            DOMDocument domDocument =
                    DOMUtils.loadDocument(artifactFile.toUri().toString(), uriResolverExtensionManager);

            XMLEntityResolver entityResolver = domDocument.getResolverExtensionManager();
            LSPXMLEntityResolver entityResolverWrapper = new LSPXMLEntityResolver(entityResolver,
                    diagnosticsResult);

            XMLValidator.doDiagnostics(domDocument, entityResolverWrapper, diagnosticsResult, null, contentModelManager,
                    NULL_CANCEL_CHECKER);
            verboseLogger.info("Diagnostics for file " + artifactFile + ": " + diagnosticsResult);
            Path relativePath = Path.of(projectPath).relativize(artifactFile);
            diagnosticsResult.forEach((diagnostic -> reporter.reportSyntaxError(relativePath.toString(), diagnostic)));
        }
    }

    private List<Path> filterArtifactFiles() {

        List<Path> artifactFiles = new ArrayList<>();
        for (String filePath : filesToInclude) {
            if (Path.of(filePath).startsWith(Constant.ARTIFACT_FOLDER)) {
                artifactFiles.add(Path.of(projectPath, filePath).toAbsolutePath());
            }
        }
        if (artifactFiles.isEmpty()) {
            verboseLogger.warn("No artifact files found in the project path: " + projectPath);
        }
        return artifactFiles;
    }

    public void setVerboseLogger(VerboseLogger verboseLogger) {

        this.verboseLogger = verboseLogger;
    }
}
