/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.maven.p2.beans;

/**
 * Bean class representing an ImportFeature.
 */
public class ImportFeature {

    private String featureId;
    private String featureVersion;
    private String compatibility;
    private boolean isOptional;

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

    public void setFeatureVersion(String version) {
        featureVersion = version;
//        if (featureVersion == null || featureVersion.equals("")) {
//            featureVersion = BundleUtils.getOSGIVersion(version);
//        }
    }

    public String getFeatureVersion() {
        return featureVersion;
    }
}
