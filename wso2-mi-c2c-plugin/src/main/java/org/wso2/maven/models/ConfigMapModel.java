/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.models;

import java.util.Map;

public class ConfigMapModel extends KubernetesModel {
    private Map<String, String> data;
    private String mountPath;
    private boolean readOnly;
    private boolean isMIConf;
    private int defaultMode;
    private boolean dir;

    public ConfigMapModel() {
        this.readOnly = true;
        this.dir = false;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isMIConf() {
        return isMIConf;
    }

    public void setMIConf(boolean MIConf) {
        isMIConf = MIConf;
    }

    public int getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(int defaultMode) {
        this.defaultMode = defaultMode;
    }

    public boolean isDir() {
        return dir;
    }

    public void setDir(boolean dir) {
        this.dir = dir;
    }
}
