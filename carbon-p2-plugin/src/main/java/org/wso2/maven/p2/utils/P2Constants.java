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
package org.wso2.maven.p2.utils;

public class P2Constants {
    public static final String[] OSGI_FILES = new String[]{"org.eclipse.equinox:org.eclipse.equinox.common",
            "org.eclipse.equinox:org.eclipse.equinox.simpleconfigurator",
            "org.eclipse.equinox:org.eclipse.equinox.ds",
            "org.eclipse.equinox:org.eclipse.equinox.launcher",
            "org.eclipse.equinox:org.eclipse.equinox.util",
            "org.eclipse.osgi:org.eclipse.osgi.services",
            "org.eclipse.osgi:org.eclipse.osgi",
            "org.eclipse.equinox:org.eclipse.equinox.app",
            "org.eclipse.equinox:org.eclipse.equinox.concurrent",
            "org.eclipse.equinox:org.eclipse.equinox.frameworkadmin",
            "org.eclipse.equinox:org.eclipse.equinox.frameworkadmin.equinox",
            "org.eclipse.equinox:org.eclipse.equinox.preferences",
            "org.eclipse.equinox:org.eclipse.equinox.registry",
            "org.eclipse.equinox:org.eclipse.equinox.security",
            "org.eclipse.equinox:org.eclipse.equinox.simpleconfigurator.manipulator",
            "org.eclipse.core:org.eclipse.core.commands",
            "org.eclipse.core:org.eclipse.core.contenttype",
            "org.eclipse.core:org.eclipse.core.expressions",
            "org.eclipse.core:org.eclipse.core.jobs",
            "org.eclipse.core:org.eclipse.core.net",
            "org.eclipse.core:org.eclipse.core.runtime",
            "org.eclipse.core:org.eclipse.core.runtime.compatibility.auth",
            "org.eclipse.ecf:org.eclipse.ecf",
            "org.eclipse.ecf:org.eclipse.ecf.filetransfer",
            "org.eclipse.ecf:org.eclipse.ecf.identity",
            "org.eclipse.ecf:org.eclipse.ecf.provider.filetransfer",
            "org.eclipse:org.sat4j.core",
            "org.eclipse:org.sat4j.pb",
            "org.eclipse:com.ibm.icu",
            "org.eclipse.equinox:org.eclipse.equinox.p2.artifact.repository",
            "org.eclipse.equinox:org.eclipse.equinox.p2.console",
            "org.eclipse.equinox:org.eclipse.equinox.p2.core",
            "org.eclipse.equinox:org.eclipse.equinox.p2.director",
            "org.eclipse.equinox:org.eclipse.equinox.p2.engine",
            "org.eclipse.equinox:org.eclipse.equinox.p2.exemplarysetup",
            "org.eclipse.equinox:org.eclipse.equinox.p2.garbagecollector",
            "org.eclipse.equinox:org.eclipse.equinox.p2.jarprocessor",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata.repository",
            "org.eclipse.equinox:org.eclipse.equinox.p2.publisher",
            "org.eclipse.equinox:org.eclipse.equinox.p2.touchpoint.eclipse",
            "org.eclipse.equinox:org.eclipse.equinox.p2.touchpoint.natives",
            "org.eclipse.equinox:org.eclipse.equinox.p2.updatechecker",
            "org.eclipse.equinox:org.eclipse.equinox.p2.director.app",
            "org.eclipse.equinox:org.eclipse.equinox.p2.directorywatcher",
            "org.eclipse.equinox:org.eclipse.equinox.p2.extensionlocation",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata.generator",
            "org.eclipse.equinox:org.eclipse.equinox.p2.reconciler.dropins",
            "org.eclipse.equinox:org.eclipse.equinox.p2.repository.tools",
            "org.eclipse.equinox:org.eclipse.equinox.p2.repository",
            "org.eclipse.equinox:org.eclipse.equinox.p2.updatesite"};

    public static final String[] OSGI_FILES_DEFAULT_VERSION = new String[]{"org.eclipse.equinox:org.eclipse.equinox.common:3.5.0.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.simpleconfigurator:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.ds:1.1.0.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.launcher:1.0.200.v20090520",
            "org.eclipse.equinox:org.eclipse.equinox.util:1.0.100.v20090520-1800",
            "org.eclipse.osgi:org.eclipse.osgi.services:3.2.0.v20090520-1800",
            "org.eclipse.osgi:org.eclipse.osgi:3.5.0.v20090520",
            "org.eclipse.equinox:org.eclipse.equinox.app:1.2.0.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.concurrent:1.0.0.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.frameworkadmin:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.frameworkadmin.equinox:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.preferences:3.2.300.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.registry:3.4.100.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.security:1.0.100.v20090520-1800",
            "org.eclipse.equinox:org.eclipse.equinox.simpleconfigurator.manipulator:1.0.100.v20090520-1905",
            "org.eclipse.core:org.eclipse.core.commands:3.5.0.I20090429-1800",
            "org.eclipse.core:org.eclipse.core.contenttype:3.4.0.v20090429-1800",
            "org.eclipse.core:org.eclipse.core.expressions:3.4.100.v20090429-1800",
            "org.eclipse.core:org.eclipse.core.jobs:3.4.100.v20090429-1800",
            "org.eclipse.core:org.eclipse.core.net:1.2.0.I20090522-1010",
            "org.eclipse.core:org.eclipse.core.runtime:3.5.0.v20090429-1800",
            "org.eclipse.core:org.eclipse.core.runtime.compatibility.auth:3.2.100.v20090413",
            "org.eclipse.ecf:org.eclipse.ecf:3.0.0.v20090520-0800",
            "org.eclipse.ecf:org.eclipse.ecf.filetransfer:3.0.0.v20090520-0800",
            "org.eclipse.ecf:org.eclipse.ecf.identity:3.0.0.v20090520-0800",
            "org.eclipse.ecf:org.eclipse.ecf.provider.filetransfer:3.0.0.v20090520-0800",
            "org.eclipse:org.sat4j.core:2.1.0.v20090520",
            "org.eclipse:org.sat4j.pb:2.1.0.v20090520",
            "org.eclipse:com.ibm.icu:4.0.1.v20090415",
            "org.eclipse.equinox:org.eclipse.equinox.p2.artifact.repository:1.0.100.v20090520-1905-wso2v1",
            "org.eclipse.equinox:org.eclipse.equinox.p2.console:1.0.100.v20090520-1905-wso2v1",
            "org.eclipse.equinox:org.eclipse.equinox.p2.core:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.director:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.engine:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.exemplarysetup:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.garbagecollector:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.jarprocessor:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata.repository:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.publisher:1.0.0.v20090521-1912",
            "org.eclipse.equinox:org.eclipse.equinox.p2.touchpoint.eclipse:1.0.100.v20090520-1905-wso2v1",
            "org.eclipse.equinox:org.eclipse.equinox.p2.touchpoint.natives:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.updatechecker:1.1.0.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.director.app:1.0.100.v20090521-1912",
            "org.eclipse.equinox:org.eclipse.equinox.p2.directorywatcher:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.extensionlocation:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.metadata.generator:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.reconciler.dropins:1.0.100.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.repository.tools:1.0.0.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.repository:1.0.0.v20090520-1905",
            "org.eclipse.equinox:org.eclipse.equinox.p2.updatesite:1.0.100.v20090520-1905"};

    public static final String DEFAULT_PROFILE_ID = "WSO2CarbonProfile";
    public static final String SIMPLE_CONFIGURATOR = "org.eclipse.equinox.simpleconfigurator";
    public static final String PROFILE_KEY = "eclipse.p2.profile";

    public static String getDefaultVersion(String groupId, String artifactId) {
        for (String osgiFile : OSGI_FILES_DEFAULT_VERSION) {
            String[] split = osgiFile.split(":");
            if (split[0].equalsIgnoreCase(groupId) && split[1].equalsIgnoreCase(artifactId))
                return split[2];
        }
        return null;
    }
}
