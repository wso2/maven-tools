/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.maven.core.utils;

import java.io.File;

public class MavenConstants {
	public static final String WSO2_MAVEN_GROUPID = "org.wso2.maven";
	public static final String MAVEN_INBOUND_ENDPOINT_ARTIFACTID = "maven-inboundendpoint-plugin";
	
	public static final String SYNAPSE_CONFIG_TYPE = "synapse/configuration";
	public static final String SEQUENCE_TEMPLATE_TYPE = "synapse/sequenceTemplate";
	public static final String ENDPOINT_TEMPLATE_TYPE = "synapse/endpointTemplate";
	public static final String COMMON_TEMPLATE_TYPE = "synapse/template";
	public static final String INBOUND_ENDPOINT_ARTIFACT_TYPE = "synapse/inbound-endpoint";
	
	public static final String CAPP_PACKAGING = "carbon/application";
	public static final String ESB_PROJECT_TARGET_CAPP = "target" + File.separator + "capp" + File.separator
			+ "artifacts";
	public static final String MAVEN_BASE_DIR_PREFIX = "${basedir}";
	public static final String CAPP_PREFIX = "capp/";
	public static final String POM_FILE_NAME = "pom.xml";
	public static final String ARTIFACT_XML_NAME = "artifact.xml";
	public static final String ARTIFACT_TAG = "artifact";
}
