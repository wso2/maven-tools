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
package org.wso2.maven.p2.generate.feature;

import org.apache.maven.plugin.MojoExecutionException;

public class Property {
	/**
     * property key
     *
     * @parameter
     * @required
     */
	private String key;
	
	/**
     * property value
     *
     * @parameter 
     * @required
     */
	private String value;
	
	
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getKey() {
		return key;
	}
	
	public static Property getProperty(String advicePropertyDefinition) throws MojoExecutionException{
		if (advicePropertyDefinition.trim().equalsIgnoreCase("")) throw new MojoExecutionException("Invalid advice property definition.");
		String[] propertyDefs = advicePropertyDefinition.split(":");
		Property property = new Property();
		if (propertyDefs.length>1){
			property.setKey(propertyDefs[0]);
			property.setValue(propertyDefs[1]);
		}else
			throw new MojoExecutionException("Invalid advice property definition: "+advicePropertyDefinition);
		return property;
	}
}
