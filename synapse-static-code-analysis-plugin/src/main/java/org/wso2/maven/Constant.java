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

import java.nio.file.Path;

public class Constant {

    // Tool metadata
    public static final String TOOL_NAME = "SynapseAnalyzer";
    public static final String TOOL_VERSION = "1.0.0";
    public static final String TOOL_REPO_URL = "https://github.com/your-org/synapse-analyzer";

    // Common constants
    public static final String SRC = "src";
    public static final String CATALOG_XML = "catalog.xml";
    public static final String PROJECT_NAME = "projectName";
    public static final String GENERATED_AT = "generatedAt";
    public static final String SYNTAX_ISSUES = "syntaxIssues";
    public static final String UTF_8 = "UTF-8";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String URL = "url";
    public static final Path ARTIFACT_FOLDER = Path.of(Constant.SRC, "main", "wso2mi", "artifacts");

    // JSON keys for issues
    public static final String TOOL = "tool";
    public static final String ID = "id";
    public static final String SEVERITY = "severity";
    public static final String MESSAGE = "message";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String LINE = "line";
    public static final String COLUMN = "column";
    public static final String FILE_PATH = "filePath";
    public static final String ISSUES = "issues";
    public static final String FILES = "files";
    public static final String TOTAL_ISSUES = "totalIssues";
    public static final String ERRORS = "errors";
    public static final String WARNINGS = "warnings";
    public static final String INFO = "info";
    public static final String SUMMARY = "summary";

    // SARIF specific constants
    public static final String SCHEMA = "$schema";
    public static final String RUNS = "runs";
    public static final String INFORMATION_URI = "informationUri";
    public static final String DRIVER = "driver";
    public static final String RESULTS = "results";
    public static final String RULE_ID = "ruleId";
    public static final String LEVEL = "level";
    public static final String KIND = "kind";
    public static final String FAIL = "fail";
    public static final String TEXT = "text";
    public static final String URI = "uri";
    public static final String ARTIFACT_LOCATION = "artifactLocation";
    public static final String START_LINE = "startLine";
    public static final String START_COLUMN = "startColumn";
    public static final String END_LINE = "endLine";
    public static final String END_COLUMN = "endColumn";
    public static final String REGION = "region";
    public static final String PHYSICAL_LOCATION = "physicalLocation";
    public static final String LOCATIONS = "locations";
}
