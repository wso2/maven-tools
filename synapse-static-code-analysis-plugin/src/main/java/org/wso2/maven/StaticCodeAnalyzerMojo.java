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

package org.wso2.maven;

import freemarker.template.TemplateException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.wso2.maven.reporter.Generator;
import org.wso2.maven.reporter.GeneratorFactory;
import org.wso2.maven.reporter.ReporterContext;
import org.wso2.maven.reporter.SynapseReporter;
import org.wso2.maven.syntax.validator.SynapseValidator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Mojo(name = "analyze")
public class StaticCodeAnalyzerMojo extends AbstractMojo {

    @Parameter(property = "synapse.analyzer.basedir", defaultValue = "${project.basedir}")
    private File basedir;

    // Where to write report files
    @Parameter(property = "synapse.analyzer.outputDirectory", defaultValue = "target/static-analysis-report")
    private String outputDirectory;

    @Parameter(property = "synapse.analyzer.reportFormats", defaultValue = "html")
    private String[] reportFormats;

    // Whether to fail the build if any issues are found above threshold
    @Parameter(property = "synapse.analyzer.failBuildOnIssues", defaultValue = "false")
    private boolean failBuildOnIssues;

    // Skip running the analysis
    @Parameter(property = "synapse.analyzer.skip", defaultValue = "false")
    private boolean skip;

    // Filter which files to include in analysis
    @Parameter(property = "synapse.analyzer.includeFiles")
    private String[] includeFiles;

    // Filter which files to exclude from analysis
    @Parameter(property = "synapse.analyzer.excludeFiles")
    private String[] excludeFiles;

    @Parameter(property = "synapse.analyzer.verbose", defaultValue = "false")
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        VerboseLogger verboseLogger = new VerboseLogger(getLog(), verbose);
        if (skip) {
            getLog().info("Skipping Synapse static code analysis as per configuration.");
            return;
        }
        getLog().info("Starting Synapse static code analysis for project: " + basedir);
        verboseLogger.info("Verbose mode is enabled. Detailed logs will be printed.");

        try {
            String[] filesToAnalyze = collectFilesToAnalyze(verboseLogger);
            SynapseReporter synapseReporter = new SynapseReporter(basedir.getPath(), outputDirectory, verboseLogger);
            configureReporters(verboseLogger, synapseReporter);
            validateSyntax(filesToAnalyze, verboseLogger, synapseReporter);

            synapseReporter.generateReport();
            failOnCondition(synapseReporter.getReporterContext());

        } catch (IOException | URISyntaxException | TemplateException e) {
            throw new MojoExecutionException("Error during Synapse static code analysis", e);
        } catch (InvalidConfigurationException e) {
            throw new MojoExecutionException("Invalid configuration for Synapse static code analysis", e);
        }
    }

    private void configureReporters(VerboseLogger verboseLogger, SynapseReporter synapseReporter) {

        for (String reportFormat : reportFormats) {
            try {
                Generator generator = GeneratorFactory.createGenerator(reportFormat);
                synapseReporter.registerReportGenerator(generator);
                verboseLogger.info("Registered report generator for format: " + reportFormat);
            } catch (org.wso2.maven.InvalidConfigurationException e) {
                verboseLogger.error("Invalid configuration for report format: " + reportFormat, e);
            }
        }
    }

    private String[] collectFilesToAnalyze(VerboseLogger verboseLogger) {

        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(basedir);
        if (includeFiles != null && includeFiles.length > 0) {
            directoryScanner.setIncludes(includeFiles);
        } else {
            directoryScanner.setIncludes(new String[]{"**/*"});
        }
        if (excludeFiles != null && excludeFiles.length > 0) {
            directoryScanner.setExcludes(excludeFiles);
        }
        verboseLogger.info("Scanning for files to analyze in directory: " + basedir);
        directoryScanner.scan();
        verboseLogger.info("Files included in analysis: " + String.join(", ", directoryScanner.getIncludedFiles()));
        return directoryScanner.getIncludedFiles();
    }

    private void validateSyntax(String[] filesToAnalyze, VerboseLogger verboseLogger, SynapseReporter synapseReporter)
            throws TemplateException, IOException, URISyntaxException, InvalidConfigurationException {

        SynapseValidator synapseValidator = new SynapseValidator(basedir.getPath(), filesToAnalyze, synapseReporter);
        synapseValidator.setVerboseLogger(verboseLogger);
        synapseValidator.validate();
    }

    private void failOnCondition(ReporterContext reporterContext) throws MojoFailureException {

        if (failBuildOnIssues && reporterContext.hasCriticalIssues()) {
            StringBuilder errorMessage = new StringBuilder("Synapse static code analysis found issues:\n");
            reporterContext.getSyntaxErrors()
                    .forEach(error -> errorMessage.append(error.toErrorMessage()).append("\n"));
            throw new MojoFailureException(errorMessage.toString());
        }
    }
}
