/*
* Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Implementation of wso2-release:rollback goal. This will revert modified artifact.xml files to
 * previous development version. This has to be executed prior to release:rollback.
 */
@Mojo(name="rollback")
public class RollbackReleaseMojo extends AbstractMavenReleaseMojo {

    protected static final String RELEASE_BACKUP_SFX = ".releaseBackup";
    protected static final String GOAL_NAME = "rollback";

    @Override protected String getGoal() {
        return GOAL_NAME;
    }

    @Override protected String getDryRunFilePrefix() {
        return null;
    }

    @Override protected boolean isInDryRunMode() {
        return false;
    }

    @Override protected String getCommitMessage(Properties releaseProperties) {
        return "rollback the release of " + releaseProperties.getProperty(PROP_SCM_TAG);
    }

    @Override protected String getNewVersion(File artifactXml) throws IOException, XmlPullParserException {
        // Read the backup pom file created by maven-release-plugin.
        File releaseBackupPOM = new File(artifactXml.getParent() + File.separator + POM_XML +
                RELEASE_BACKUP_SFX);
        if (releaseBackupPOM.exists()) {
            MavenProject mavenProjectBackup = getMavenProject(releaseBackupPOM);
            return mavenProjectBackup.getVersion();
        } else {
            log.error("Cannot find " + releaseBackupPOM.getPath() +
                    " file. Make sure you have invoked this goal before invoking" +
                    " release:rollback of maven-release-plugin.");
            return null;
        }
    }
}
