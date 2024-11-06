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

package org.wso2.maven.metadata;

import java.util.ArrayList;
import java.util.List;

public class Application {

    private Artifact applicationArtifact;
    private String appName;
    private String appVersion;
    private String mainSequence;

    public Artifact getApplicationArtifact() {
        return applicationArtifact;
    }

    public void setApplicationArtifact(Artifact applicationArtifact) {
        this.applicationArtifact = applicationArtifact;
    }

    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public String getAppVersion() {
        return appVersion;
    }
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    public String getMainSequence() {
        return mainSequence;
    }
    public void setMainSequence(String mainSequence) {
        this.mainSequence = mainSequence;
    }
}
