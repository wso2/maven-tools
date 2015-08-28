/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.commons;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * The Generator class must be extended by all the generator classes which implement the the plugin logic.
 */
public abstract class Generator {
    private Log log;

    /**
     * Constructor taking Log object as a parameter. The log is needed to log the generator activity flow.
     * @param logger
     */
    public Generator(Log logger) {
        this.log = logger;
    }

    public abstract void generate()  throws MojoExecutionException, MojoFailureException;

    protected Log getLog() {
        return this.log;
    }
}
