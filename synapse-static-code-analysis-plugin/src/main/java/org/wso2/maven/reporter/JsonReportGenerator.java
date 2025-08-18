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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonReportGenerator extends AbstractGenerator {

    private static final String REPORT_GENERATOR_NAME = "JSON Report Generator";
    private static final String REPORT_FILE_NAME = "synapse-report.json";

    @Override
    public String getName() {

        return REPORT_GENERATOR_NAME;
    }

    @Override
    public void generateReport(ReporterContext reporterContext) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();

        // Tool metadata
        ObjectNode toolNode = mapper.createObjectNode();
        toolNode.put(Constant.NAME, Constant.TOOL_NAME);
        toolNode.put(Constant.VERSION, Constant.TOOL_VERSION);
        toolNode.put(Constant.URL, Constant.TOOL_REPO_URL);
        root.set(Constant.TOOL, toolNode);

        // Report generation time
        root.put(Constant.GENERATED_AT, Instant.now().toString());

        // Group issues by file
        Map<String, ArrayNode> issuesByFile = new LinkedHashMap<>();

        // Counters for summary
        int totalIssues = 0;
        int errors = 0;
        int warnings = 0;
        int info = 0;

        for (SyntaxError issue : reporterContext.getSyntaxErrors()) {
            String file = issue.getFilePath().replace('\\', '/');

            // Create issues array for file if not present
            ArrayNode fileIssues = issuesByFile.computeIfAbsent(file, k -> mapper.createArrayNode());

            ObjectNode issueNode = mapper.createObjectNode();
            issueNode.put(Constant.ID, issue.getCode() != null ? issue.getCode().getLeft() : Constant.UNKNOWN);
            issueNode.put(Constant.SEVERITY, issue.getSeverity().name().toLowerCase());
            issueNode.put(Constant.MESSAGE, issue.getMessage());

            // Add line and column if available
            if (issue.getRange() != null && issue.getRange().getStart() != null) {
                issueNode.put(Constant.LINE, issue.getRange().getStart().getLine() + 1);
                issueNode.put(Constant.COLUMN, issue.getRange().getStart().getCharacter() + 1);
            }

            fileIssues.add(issueNode);

            // Count severity
            totalIssues++;
            switch (issue.getSeverity()) {
                case Error:
                    errors++;
                    break;
                case Warning:
                    warnings++;
                    break;
                case Information:
                    info++;
                    break;
                default:
                    break;
            }
        }

        // Add files array
        ArrayNode filesArray = mapper.createArrayNode();
        for (Map.Entry<String, ArrayNode> entry : issuesByFile.entrySet()) {
            ObjectNode fileNode = mapper.createObjectNode();
            fileNode.put(Constant.FILE_PATH, entry.getKey());
            fileNode.set(Constant.ISSUES, entry.getValue());
            filesArray.add(fileNode);
        }
        root.set(Constant.FILES, filesArray);

        // Summary
        ObjectNode summaryNode = mapper.createObjectNode();
        summaryNode.put(Constant.TOTAL_ISSUES, totalIssues);
        summaryNode.put(Constant.ERRORS, errors);
        summaryNode.put(Constant.WARNINGS, warnings);
        summaryNode.put(Constant.INFO, info);
        root.set(Constant.SUMMARY, summaryNode);

        Path filePath = Path.of(reporterContext.getReportOutputDirectory(), REPORT_FILE_NAME);
        try (Writer writer = getFileWriter(filePath)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, root);
        }
    }
}
