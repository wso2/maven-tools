/*
 * Copyright (c) 2023, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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

package org.wso2.maven.inboundendpoint;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Prepare an artifact to be installed in the local Maven repository
 */
@Mojo(name="package-inbound-endpoint")
public class InboundEndpointMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * The path of the existing artifact
     */
	@Parameter(property="deploy-file.artifact", required = true)
    private File artifact;

    /**
     * The resulting extension of the file
     */
	@Parameter(property="deploy-file.extension")
    private String extension;

    /**
     * The resulting extension of the file
     */
	@Parameter(property="deploy-file.fileName")
    private String fileName;

    /**
     * If the file should be archived
     */
	@Parameter(property="deploy-file.enableArchive", defaultValue = "false")
    private boolean enableArchive;

    private File destFolder;

    public void execute() throws MojoExecutionException, MojoFailureException {

        destFolder = new File(project.getBuild().getDirectory());
        String newPath = null;

        if (fileName != null) { // if user gives a name for the file
            newPath = destFolder.getAbsolutePath() + File.separator + fileName;
        } else {
            if (extension != null) { // if user provides the extension
                String fileNameWithoutExtension = (artifact.getName().split("\\."))[0];
                newPath = destFolder.getAbsolutePath() + File.separator + fileNameWithoutExtension + "." + extension;
            } else {
                newPath = destFolder.getAbsolutePath() + File.separator + artifact.getName();
            }
        }

        File result = new File(newPath);

        if (!artifact.exists()) {
            throw new MojoExecutionException(artifact.getAbsolutePath() + " doesn't exist.");
        }

        try {
            FileUtils.copyFile(artifact, result);

        } catch (IOException e) {
            throw new MojoExecutionException("Error when copying " + artifact.getName() + " to " + result.getName()
                    + "\n" + e.getMessage());
        }

        if (result != null && result.exists()) {
            project.getArtifact().setFile(result);
        } else {
            throw new MojoExecutionException(result + " is null or doesn't exist");
        }

        if (enableArchive) {
            // TODO make the zip file
        }
    }

}
