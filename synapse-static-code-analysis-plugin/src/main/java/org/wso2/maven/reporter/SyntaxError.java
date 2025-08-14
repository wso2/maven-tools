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

import org.eclipse.lsp4j.Diagnostic;

public class SyntaxError extends Diagnostic {

    private String filePath;

    public SyntaxError(String filePath, org.eclipse.lsp4j.Diagnostic diagnostic) {

        this.filePath = filePath;
        this.setRange(diagnostic.getRange());
        this.setSeverity(diagnostic.getSeverity());
        this.setCode(diagnostic.getCode());
        this.setSource(diagnostic.getSource());
        this.setMessage(diagnostic.getMessage());
        this.setRelatedInformation(diagnostic.getRelatedInformation());
        this.setData(diagnostic.getData());
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath(String filePath) {

        this.filePath = filePath;
    }

    public String toErrorMessage() {

        return String.format("File: %s, Message: %s, Severity: %s, Line: %d, Column: %d", filePath, getMessage(),
                getSeverity(), getRange().getStart().getLine() + 1, getRange().getStart().getCharacter() + 1);
    }
}
