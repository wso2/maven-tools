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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.wso2.maven.Constant;
import org.wso2.maven.StaticCodeAnalyzerMojo;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HTMLReportGenerator extends AbstractGenerator {

    private static final String REPORT_GENERATOR_NAME = "HTML Report Generator";
    private static final String REPORT_TEMPLATE_NAME = "synapse-report.ftl";
    private static final String REPORT_FILE_NAME = "synapse-report.html";

    @Override
    public String getName() {

        return REPORT_GENERATOR_NAME;
    }

    @Override
    public void generateReport(ReporterContext reporterContext) {

        try {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
            configuration.setClassForTemplateLoading(StaticCodeAnalyzerMojo.class, "/");
            configuration.setDefaultEncoding(Constant.UTF_8);

            Template template = configuration.getTemplate(REPORT_TEMPLATE_NAME);

            // Prepare data model
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put(Constant.PROJECT_NAME, Path.of(reporterContext.getProjectPath()).getFileName());
            dataModel.put(Constant.GENERATED_AT, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
            dataModel.put(Constant.SYNTAX_ISSUES, reporterContext.getSyntaxErrors());

            // Write output
            Path filePath = Path.of(reporterContext.getReportOutputDirectory(), REPORT_FILE_NAME);
            try (Writer fileWriter = getFileWriter(filePath)) {
                template.process(dataModel, fileWriter);
            }
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
