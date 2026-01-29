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

package org.wso2.maven.plugin.connector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.wso2.developerstudio.eclipse.utils.data.ITemporaryFileTag;
import org.wso2.maven.capp.model.Artifact;
import org.wso2.maven.capp.mojo.AbstractPOMGenMojo;
import org.wso2.maven.capp.utils.CAppMavenUtils;
import org.wso2.maven.capp.utils.WSO2MavenPluginConstantants;
import org.wso2.maven.core.utils.MavenConstants;
import org.wso2.maven.esb.ESBArtifact;
import org.wso2.maven.esb.utils.ESBMavenUtils;

/**
 * This is the Maven Mojo used for generating a pom for a connector artifact
 */
@Mojo(name="pom-gen")
public class ConnectorPOMGenMojo extends AbstractPOMGenMojo {

	@Parameter(defaultValue = "${project}")
    public MavenProject project;

    /**
     * Maven ProjectHelper.
     */
	@Inject
    public MavenProjectHelper projectHelper;

    /**
     * The path of the location to output the pom
     */
	@Parameter(defaultValue = "${project.build.directory}/artifacts")
    public File outputLocation;

    /**
     * The resulting extension of the file
     */
    @Parameter
    public File artifactLocation;

    /**
     * POM location for the module project
     */
    @Parameter(defaultValue = "${project.build.directory}/pom.xml")
    public File moduleProject;

    /**
     * Group id to use for the generated pom
     */
    @Parameter
    public String groupId;

    /**
     * Comma separated list of "artifact_type=extension" to be used when creating dependencies for other capp artifacts
     */
    @Parameter
    public String typeList;

    private static final String ARTIFACT_TYPE = "synapse/lib";

    private List<ESBArtifact> retrieveArtifacts() {

        return ESBMavenUtils.retrieveArtifacts(getArtifactLocation());
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        // Retrieving all the existing Artifacts for the given Maven project
        List<ESBArtifact> artifacts = retrieveArtifacts();

        // Artifact list
        List<Artifact> mappedArtifacts = new ArrayList<Artifact>();

        // Mapping ESBArtifacts to C-App artifacts
        for (ESBArtifact esbArtifact : artifacts) {
            Artifact artifact = new Artifact();
            artifact.setName(esbArtifact.getName());
            artifact.setVersion(esbArtifact.getVersion());
            artifact.setType(esbArtifact.getType());
            artifact.setServerRole(esbArtifact.getServerRole());
            artifact.setFile(esbArtifact.getFile());
            artifact.setSource(new File(getArtifactLocation(), "artifact.xml"));
            mappedArtifacts.add(artifact);
        }
        // Calling the process artifacts method of super type to continue the sequence.
        super.processArtifacts(mappedArtifacts);
    }

    protected void copyResources(MavenProject project, File projectLocation, Artifact artifact) throws IOException {

        ITemporaryFileTag newTag = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createNewTempTag();
        File connectorArtifact = artifact.getFile();
        FileUtils.copyFile(connectorArtifact, new File(projectLocation, artifact.getFile().getName()));
        newTag.clearAndEnd();
    }

    protected void addPlugins(MavenProject artifactMavenProject, Artifact artifact) {

        Plugin plugin = CAppMavenUtils.createPluginEntry(artifactMavenProject, MavenConstants.WSO2_MAVEN_GROUPID,
                "wso2-esb-connector-plugin", WSO2MavenPluginConstantants.WSO2_ESB_CONNECTOR_VERSION, true);
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        // add configuration
        Xpp3Dom aritfact = CAppMavenUtils.createConfigurationNode(configuration, "artifact");
        aritfact.setValue(artifact.getFile().getName());
    }

    protected String getArtifactType() {

        return ARTIFACT_TYPE;
    }
}
