/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.registry.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RegistryElement {

    private String path;

    private List<RegistryProperty> properties = new ArrayList<RegistryProperty>();

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public List<RegistryProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public void addProperty(RegistryProperty property) {
        this.properties.add(property);
    }

    public void removeProperty(RegistryProperty property) {
        this.properties.remove(property);
    }
}
