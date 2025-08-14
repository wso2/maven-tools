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

import org.wso2.maven.InvalidConfigurationException;

public class GeneratorFactory {

    private static final String JSON = "json";
    private static final String SARIF = "sarif";
    private static final String HTML = "html";

    private GeneratorFactory() {
        // Private constructor to prevent instantiation
    }

    public static Generator createGenerator(String type) throws InvalidConfigurationException {

        switch (type) {
            case JSON:
                return new JsonReportGenerator();
            case SARIF:
                return new SARIFReportGenerator();
            case HTML:
                return new HTMLReportGenerator();
            default:
                throw new InvalidConfigurationException("Unsupported generator type: " + type);
        }
    }
}
