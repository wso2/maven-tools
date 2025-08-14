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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.wso2.maven.Constant;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

public class SARIFReportGenerator extends AbstractGenerator {

    private static final String REPORT_GENERATOR_NAME = "SARIF Report Generator";
    private static final String SCHEMA_URL = "https://json.schemastore.org/sarif-2.1.0.json";
    private static final String SARIF_VERSION = "2.1.0";
    private static final String SYNTAX_ERROR_CODE = "SYNTAX_ERROR";
    private static final String REPORT_FILE_NAME = "synapse-report.sarif";

    @Override
    public String getName() {

        return REPORT_GENERATOR_NAME;
    }

    @Override
    public void generateReport(ReporterContext reporterContext) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put(Constant.SCHEMA, SCHEMA_URL);
        root.put(Constant.VERSION, SARIF_VERSION);
        ArrayNode runs = mapper.createArrayNode();
        root.set(Constant.RUNS, runs);

        ObjectNode run = mapper.createObjectNode();
        runs.add(run);

        // Tool Info
        ObjectNode tool = mapper.createObjectNode();
        ObjectNode driver = mapper.createObjectNode();
        driver.put(Constant.NAME, Constant.TOOL_NAME);
        driver.put(Constant.VERSION, Constant.TOOL_VERSION);
        driver.put(Constant.INFORMATION_URI, Constant.TOOL_REPO_URL);
        tool.set(Constant.DRIVER, driver);
        run.set(Constant.TOOL, tool);

        // Results
        ArrayNode results = mapper.createArrayNode();
        run.set(Constant.RESULTS, results);

        for (SyntaxError issue : reporterContext.getSyntaxErrors()) {
            String filePathNormalized = issue.getFilePath().replace('\\', '/');

            ObjectNode result = mapper.createObjectNode();
            result.put(Constant.RULE_ID, issue.getCode() != null ? issue.getCode().getLeft() : SYNTAX_ERROR_CODE);
            result.put(Constant.LEVEL, issue.getSeverity().name().toLowerCase());
            result.put(Constant.KIND, Constant.FAIL);
            result.set(Constant.MESSAGE, mapper.createObjectNode().put(Constant.TEXT, issue.getMessage()));

            ObjectNode location = mapper.createObjectNode();
            ObjectNode physicalLocation = mapper.createObjectNode();
            ObjectNode artifactLocation = mapper.createObjectNode();
            artifactLocation.put(Constant.URI, filePathNormalized);
            physicalLocation.set(Constant.ARTIFACT_LOCATION, artifactLocation);

            ObjectNode region = mapper.createObjectNode();
            region.put(Constant.START_LINE, issue.getRange().getStart().getLine() + 1);
            region.put(Constant.START_COLUMN, issue.getRange().getStart().getCharacter() + 1);
            region.put(Constant.END_LINE, issue.getRange().getEnd().getLine() + 1);
            region.put(Constant.END_COLUMN, issue.getRange().getEnd().getCharacter() + 1);
            physicalLocation.set(Constant.REGION, region);

            location.set(Constant.PHYSICAL_LOCATION, physicalLocation);
            ArrayNode locations = mapper.createArrayNode();
            locations.add(location);

            result.set(Constant.LOCATIONS, locations);
            results.add(result);
        }

        Path filePath = Path.of(reporterContext.getReportOutputDirectory(), REPORT_FILE_NAME);
        try (Writer fileWriter = getFileWriter(filePath)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fileWriter, root);
        }
    }
}
