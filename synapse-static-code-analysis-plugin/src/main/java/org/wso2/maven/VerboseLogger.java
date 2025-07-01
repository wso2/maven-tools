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

import org.apache.maven.plugin.logging.Log;

import java.util.logging.LogManager;

public class VerboseLogger {

    private final Log log;
    private final boolean verbose;

    public VerboseLogger(Log log, boolean verbose) {

        LogManager.getLogManager().reset();
        this.log = log;
        this.verbose = verbose;
    }

    public void info(String message) {

        if (verbose) {
            log.info(message);
        }
    }

    public void warn(String message) {

        if (verbose) {
            log.warn(message);
        }
    }

    public void error(String message) {

        if (verbose) {
            log.error(message);
        }
    }

    public void error(String message, Throwable e) {

        if (verbose) {
            log.error(message, e);
        }
    }

    public void debug(String message) {

        if (verbose) {
            log.debug(message);
        }
    }

    public boolean isVerbose() {

        return verbose;
    }
}
