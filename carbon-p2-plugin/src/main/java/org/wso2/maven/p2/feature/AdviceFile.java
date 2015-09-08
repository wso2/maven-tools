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
package org.wso2.maven.p2.feature;

import java.util.ArrayList;

/**
 * Bean representing advice file properties. This bean needs to reside in the same package as the mojo class which
 * uses this.
 */
public class AdviceFile {

    private ArrayList properties;

    public ArrayList getProperties() {
        return this.properties;
    }

    public void setProperties(ArrayList properties) {
        this.properties = properties;
    }
}
