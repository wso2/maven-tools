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

package org.wso2.maven.p2.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;

public class PropertyUtils {

    /**
     * Generates a Property bean from the given advice property definition
     *
     * @param advicePropertyDefinition String definition for a property
     * @return Property generated from the string definition
     * @throws InvalidBeanDefinitionException
     */
    public static Property getProperty(String advicePropertyDefinition) throws InvalidBeanDefinitionException {
        String[] propertyDefs = advicePropertyDefinition.split(":");
        Property property = new Property();

        if (propertyDefs.length > 1) {
            property.setKey(propertyDefs[0]);
            property.setValue(propertyDefs[1]);
        } else {
            throw new InvalidBeanDefinitionException("Invalid advice property definition: " + advicePropertyDefinition);
        }
        return property;
    }
}
