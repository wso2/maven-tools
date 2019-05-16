/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import org.apache.maven.plugins.annotations.Parameter;

import java.io.Serializable;

/**
 * Task used for merge json
 */
public class Task implements Serializable {

    @Parameter(property = "target")
    private String target;
    @Parameter(property = "base")
    private String base;
    @Parameter(property = "include")
    private String[] include;
    @Parameter(property = "mergeChildren")
    private boolean mergeChildren = false;

    public String getTarget() {

        return target;
    }

    public void setTarget(String target) {

        this.target = target;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public boolean isMergeChildren() {
        return mergeChildren;
    }

    public void setMergeChildren(boolean mergeChildren) {
        this.mergeChildren = mergeChildren;
    }
}
