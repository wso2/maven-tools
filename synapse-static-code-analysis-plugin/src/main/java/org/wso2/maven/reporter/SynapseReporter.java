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

package org.wso2.maven.reporter;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.wso2.maven.VerboseLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SynapseReporter implements Reporter {

    private final ReporterContext reporterContext;
    private final List<Generator> reportGenerators = new ArrayList<>();
    private final VerboseLogger verboseLogger;

    public SynapseReporter(String projectPath, String reportOutputDirectory, VerboseLogger verboseLogger) {

        this.reporterContext = new ReporterContext(projectPath, reportOutputDirectory);
        this.verboseLogger = verboseLogger;
    }

    @Override
    public void reportSyntaxError(String filePath, org.eclipse.lsp4j.Diagnostic diagnostic) {

        if (!DiagnosticSeverity.Hint.equals(diagnostic.getSeverity())) {
            reporterContext.addSyntaxError(new SyntaxError(filePath, diagnostic));
        }
    }

    @Override
    public void generateReport() {

        for (Generator generator : reportGenerators) {
            try {
                generator.generateReport(reporterContext);
            } catch (IOException e) {
                verboseLogger.error("Error generating report using " + generator.getName(), e);
            }
        }
    }

    @Override
    public void registerReportGenerator(Generator generator) {

        reportGenerators.add(generator);
    }

    public List<SyntaxError> getSyntaxErrors() {

        return reporterContext.getSyntaxErrors();
    }

    public String getReportOutputDirectory() {

        return reporterContext.getReportOutputDirectory();
    }

    @Override
    public ReporterContext getReporterContext() {

        return reporterContext;
    }
}
