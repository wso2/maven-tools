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

package org.wso2.maven.registry;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.wso2.maven.capp.model.Artifact;
import org.wso2.maven.registry.beans.RegistryCollection;
import org.wso2.maven.registry.beans.RegistryElement;
import org.wso2.maven.registry.beans.RegistryItem;
import org.wso2.maven.registry.utils.GeneralProjectMavenUtils;

/**
 * This is the Maven Mojo used for copying the registry resources to the output directory in the resource-process phase.
 */
@Mojo(name = "copy-registry-dependencies")
public class RegistryResourceProcessMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * Maven ProjectHelper.
     */
	@Inject
    private MavenProjectHelper projectHelper;

    /**
     * The path of the location to output the pom
     *
     * @parameter expression="${project}/build-artifacts"
     */
    private File outputLocation;

    /**
     * The resulting extension of the file
     */
    @Parameter
    private File artifactLocation;

    /**
     * POM location for the module project
     *
     * @parameter expression="${project.build.directory}/pom.xml"
     */
    private File moduleProject;

    /**
     * Group id to use for the generated pom
     */
    @Parameter
    private String groupId;

    private static final String ARTIFACT_TYPE = "registry/resource";

    private List<RegistryArtifact> artifacts;

    private Map<Artifact, RegistryArtifact> artifactToRegArtifactMap;

    private List<RegistryArtifact> retrieveArtifacts() {

        return GeneralProjectMavenUtils.retrieveArtifacts(getArtifactLocation());
    }

    public File getArtifactLocation() {

        return artifactLocation;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        //Retrieving all the existing ESB Artifacts for the given Maven project
        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Starting Artifacts list retrieval process.");
        }

        artifacts = retrieveArtifacts();

        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Artifacts list retrieval completed");
        }

        //Artifact list
        List<Artifact> mappedArtifacts = new ArrayList<Artifact>();
        //Initializing Artifacts to Registry Artifacts Map.
        artifactToRegArtifactMap = new Hashtable<Artifact, RegistryArtifact>();

        //Mapping ESBArtifacts to C-App artifacts so that we can reuse the maven-sequence-plugin
        for (RegistryArtifact registryArtifact : artifacts) {
            Artifact artifact = new Artifact();
            artifact.setName(registryArtifact.getName());
            artifact.setVersion(registryArtifact.getVersion());
            artifact.setType(registryArtifact.getType());
            artifact.setServerRole(registryArtifact.getServerRole());
            artifact.setFile("registry-info.xml");
            artifact.setSource(new File(getArtifactLocation(), "artifact.xml"));
            mappedArtifacts.add(artifact);

            //Add the mapping between C-App Artifact and Registry Artifact
            artifactToRegArtifactMap.put(artifact, registryArtifact);
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Artifact model mapping completed");
        }

        //Calling the process artifacts method of super type to continue the sequence.
        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Starting Artifact Processing");
        }

        cleanOutputLocation(getOutputLocation());
        processArtifacts(mappedArtifacts);
    }

    public MavenProject getProject() {

        return project;
    }

    private void copyResources(File projectLocation, Artifact artifact) throws IOException {
        //POM file and Registry-info.xml in the outside

        //Creating the registry info file outdide
        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Starting to process the artifact copy process");
        }

        File regInfoFile = new File(projectLocation, "registry-info.xml");
        RegistryInfo regInfo = new RegistryInfo();
        regInfo.setSource(regInfoFile);

        //Filling info sections
        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Starting generation of Registry Resource Metadata");
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    new Time(System.currentTimeMillis()) + " Reusing the previously collected Artifacts details.");
        }

        RegistryArtifact mappedRegistryArtifact = artifactToRegArtifactMap.get(artifact);
        if (mappedRegistryArtifact != null) {
            if (getLog().isDebugEnabled()) {
                getLog().debug(new Time(System.currentTimeMillis())
                                       + " C-App artifact to Registry Artifact Mapping available.");
            }
            //This is the correct registry artifact for this C-App artifact.
            List<RegistryElement> allRegistryItems = mappedRegistryArtifact.getAllRegistryItems();
            for (RegistryElement registryItem : allRegistryItems) {
                regInfo.addESBArtifact(registryItem);
            }
        } else {
            if (getLog().isDebugEnabled()) {
                getLog().debug(new Time(System.currentTimeMillis())
                                       + " C-App artifact to Registry Artifact Mapping not available.");
            }

            for (RegistryArtifact registryArtifact : artifacts) {
                if (registryArtifact.getName().equalsIgnoreCase(artifact.getName()) &&
                        this.getProject().getVersion().equalsIgnoreCase(artifact.getVersion()) &&
                        registryArtifact.getType().equalsIgnoreCase(artifact.getType()) &&
                        registryArtifact.getServerRole().equalsIgnoreCase(artifact.getServerRole())) {
                    //This is the correct registry artifact for this artifact:Yes this is reverse artifact to
                    // registry artifact mapping
                    List<RegistryElement> allRegistryItems = registryArtifact.getAllRegistryItems();
                    for (RegistryElement registryItem : allRegistryItems) {
                        regInfo.addESBArtifact(registryItem);
                    }
                    break;
                }
            }
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    new Time(System.currentTimeMillis()) + " Registry Resource Metadata collection is complete.");
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    new Time(System.currentTimeMillis()) + " Starting serialization of Registry Resource Metadata");
        }
        try {
            regInfo.toFile();
        } catch (Exception ignored) {
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    new Time(System.currentTimeMillis()) + " Completed serialization of Registry Resource Metadata");
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Start copying the Registry Resource Process");
        }

        List<RegistryElement> allESBArtifacts = regInfo.getAllESBArtifacts();
        for (RegistryElement registryItem : allESBArtifacts) {
            File file = null;
            if (registryItem instanceof RegistryItem) {
                file =
                        new File(artifact.getSource().getParentFile().getPath(),
                                ((RegistryItem) registryItem).getFile());
                ((RegistryItem) registryItem).setFile(file.getName());
            } else if (registryItem instanceof RegistryCollection) {
                file =
                        new File(artifact.getSource().getParentFile().getPath(),
                                ((RegistryCollection) registryItem).getDirectory());
                ((RegistryCollection) registryItem).setDirectory(file.getName());
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug(
                        new Time(System.currentTimeMillis()) + "  Metadata processing complete. Copying artifacts.");
            }

            // If resource is a file
            if (file.isFile()) {
                File processedFile = processTokenReplacement(file);
                FileUtils.copy(processedFile, new File(projectLocation, "resources" + File.separator + file.getName()));
            } else {
                FileUtils.copyDirectory(file, new File(projectLocation, "resources" + File.separator +
                                file.getName()));
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug(new Time(System.currentTimeMillis()) + " Artifact Copying complete.");
            }

            try {
                regInfo.toFile();
            } catch (Exception ignored) {
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug(new Time(System.currentTimeMillis()) + " Metadata file serialization completed.");
            }
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug(new Time(System.currentTimeMillis()) + " Artifact copy process is completed");
        }
    }

    protected String getArtifactType() {

        return ARTIFACT_TYPE;
    }

    protected void processArtifacts(List<Artifact> artifacts) throws MojoExecutionException {

        if (artifacts.isEmpty()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug(System.currentTimeMillis() + " Artifacts list is empty. Nothing to process");
            }
        } else {
            for (Artifact artifact : artifacts) {

                String artifactType = artifact.getType() != null ? artifact.getType() : getArtifactType();

                File projectLocation = new File(getOutputLocation()
                        + File.separator + getArtifactPostFix(artifactType),
                        artifact.getName());

                try {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(new Time(System.currentTimeMillis()) + " copying resources...");
                    }
                    getLog().info("\tcopying resources...");

                    copyResources(projectLocation, artifact);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error copying resource for artifact '"
                                    + artifact.getName() + "'", e);
                }
            }
        }
    }

    private String getArtifactPostFix(String artifactType) {

        return artifactType.substring(artifactType.indexOf("/") + 1);

    }

    /**
     * Filter and replace maven placeholders in artifacts with respective property values
     *
     * @param file
     * @return
     * @throws IOException
     */
    protected File processTokenReplacement(File file) throws IOException {

        if (file.exists()) {
            Properties mavenProperties = getProject().getModel().getProperties();

            String fileContent = org.wso2.developerstudio.eclipse.utils.file.FileUtils.getContentAsString(file);
            String newFileContent = replaceTokens(fileContent, mavenProperties);
            File tempFile = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createTempFile();
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.writeContent(tempFile, newFileContent);
            return tempFile;
        }
        return file;
    }

    /**
     * Replace artifact tokens with maven properties.
     *
     * @param content
     * @param mavenProperties
     * @return
     */
    public String replaceTokens(String content, Properties mavenProperties) {

        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String match = matcher.group(0).replaceAll("^\\$\\{", "");
            match = match.replaceAll("\\}$", "");
            String value = (String) mavenProperties.get(match);
            if (value != null && !value.trim().equals("")) {
                matcher.appendReplacement(sb, value);
                getLog().info("Replacing the token: " + match + " with value: " + value);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public File getOutputLocation() {

        if (!outputLocation.exists()) {
            outputLocation.mkdirs();
        }
        return outputLocation;
    }

    private static boolean cleanOutputLocation(File dir) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = cleanOutputLocation(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            if (dir.list().length == 0) {
                return dir.delete();
            }
        } else {
            if (!dir.getName().equalsIgnoreCase("pom.xml")) {
                return dir.delete();
            }
        }
        return true;
    }

}
