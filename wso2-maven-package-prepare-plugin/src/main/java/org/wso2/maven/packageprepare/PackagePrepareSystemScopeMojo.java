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

package org.wso2.maven.packageprepare;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.wso2.maven.core.utils.MavenConstants;
import org.wso2.maven.core.utils.MavenUtils;
import org.wso2.maven.packageprepare.util.PackagePrepareUtils;

/**
 * This Maven Mojo is used to change all dependencies to system scope.
 * 
 * @goal system-scope
 * @requiresProject
 * @aggregator
 * @since 1.0.0
 * 
 */
public class PackagePrepareSystemScopeMojo extends AbstractMojo {

	private final Log log = getLog();

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${dryRun}" default-value="false"
	 */
	private boolean dryRun;

	/**
	 * Prompter component for user input
	 * 
	 * @component
	 */
	private Prompter prompter;

	private List<MavenProject> cappMavenProjects;
	private Map<String, String> dependencySystemPathMap;

	public void execute() throws MojoExecutionException, MojoFailureException {
		log.info("MavenPackagePrepare plugin execution started");
		if (dryRun) {
			log.warn(" **** wso2-maven-package-prepare-plugin does not support dryRun mode **** ");
			throw new MojoExecutionException(
					" **** wso2-maven-package-prepare-plugin does not support dryRun mode **** ");
		}

		@SuppressWarnings("unchecked")
		List<String> modules = project.getModules();

		if (null != modules && !modules.isEmpty()) {
			// Aggregate dependencies from sub modules only if this is a maven
			// multi-module project
			List<MavenProject> mavenProjects = getMavenProjects(modules);
			aggregateDependencies(mavenProjects);

			// Update pom files of capp projects only
			filterAllCappProjects(mavenProjects);
			if (!updateAllDependencies()) {
				log.error("Failed to update POM");
				throw new MojoFailureException("Failed to update POM");
			}
		} else {
			log.error("\"" + project.getBasedir() + "\"" + " does not contain a Maven Multi-Module project. "
					+ "Please execute this plugin on the root of a Maven Multi-Module project");
			throw new MojoExecutionException("\"" + project.getBasedir() + "\""
					+ " does not contain a Maven Multi-Module project. "
					+ "Please execute this plugin on the root of a Maven Multi-Module project");
		}
	}

	private List<MavenProject> getMavenProjects(List<String> modules) {
		List<MavenProject> projectList = new ArrayList<MavenProject>();

		for (String module : modules) {
			StringBuilder modulePomFilePath = new StringBuilder();
			modulePomFilePath.append(module);
			modulePomFilePath.append(File.separator);
			modulePomFilePath.append(MavenConstants.POM_FILE_NAME);
			File modulePomFile = new File(modulePomFilePath.toString());
			if (!modulePomFile.exists()) {
				log.error("Cannot find a file in location: " + modulePomFilePath);
				continue;
			}

			MavenProject parsedMavenProject;
			try {
				parsedMavenProject = MavenUtils.getMavenProject(modulePomFile);
			} catch (MojoExecutionException e) {
				log.error("Failed to parse pom file of the module: " + module, e);
				continue;
			}
			parsedMavenProject.setFile(modulePomFile);
			projectList.add(parsedMavenProject);
		}
		return projectList;
	}

	private void aggregateDependencies(List<MavenProject> mavenProjects) {
		dependencySystemPathMap = new HashMap<String, String>();

		for (MavenProject mavenProject : mavenProjects) {
			String packaging = mavenProject.getPackaging();
			// CAPP projects are ignored.
			if (packaging == null || !packaging.equals(MavenConstants.CAPP_PACKAGING)) {
				try {
					dependencySystemPathMap.putAll(PackagePrepareUtils.getDependencyData(mavenProject));
				} catch (FactoryConfigurationError | Exception e) {
					log.error("Failed to retrieve dependencies from project: " + mavenProject.getGroupId() + ":"
							+ mavenProject.getArtifactId() + ":" + mavenProject.getVersion(), e);
				}
			}
		}
	}

	private void filterAllCappProjects(List<MavenProject> mavenProjects) {
		cappMavenProjects = new ArrayList<MavenProject>();

		for (MavenProject mavenProject : mavenProjects) {
			String packaging = mavenProject.getPackaging();
			if (packaging != null && packaging.equals(MavenConstants.CAPP_PACKAGING)) {
				log.debug("Identified the composite application project: " + mavenProject.getGroupId() + ":"
						+ mavenProject.getArtifactId() + ":" + mavenProject.getVersion());
				cappMavenProjects.add(mavenProject);
			}
		}
	}

	private boolean updateAllDependencies() {
		for (MavenProject cappMavenProject : cappMavenProjects) {
			log.info("About to update: " + cappMavenProject.getFile().getAbsolutePath());
			log.info("All dependencies will be converted to system scope");

			// Ask user consent to proceed
			String userInput = "";
			do {
				try {
					userInput = prompter.prompt("Continue? (Y/n)");
				} catch (PrompterException e) {
					log.error("Failed to get user input", e);
					return false;
				}
			} while (!("y".equalsIgnoreCase(userInput) || "n".equalsIgnoreCase(userInput)));

			if ("n".equalsIgnoreCase(userInput)) {
				log.info("Plugin execution terminated");
				return false;
			} else {
				@SuppressWarnings("unchecked")
				List<Dependency> dependencies = cappMavenProject.getDependencies();
				for (Dependency dependency : dependencies) {
					dependency.setScope(Artifact.SCOPE_SYSTEM);
					String absoluteSystemPath = resolveDependencySystemPath(dependency);
					if (!absoluteSystemPath.equals("")) {
						// Derive system path relative to this CAPP project
						StringBuilder systemPathBuilder = new StringBuilder();
						systemPathBuilder.append(MavenConstants.MAVEN_BASE_DIR_PREFIX);
						systemPathBuilder.append(File.separator);
						systemPathBuilder.append(FileUtils.getRelativePath(new File(cappMavenProject.getBasedir()
								.toString()), new File(absoluteSystemPath)));

						dependency.setSystemPath(systemPathBuilder.toString());
					} else {
						// Reason is logged in a previous step
						return false;
					}
				}
				try {
					MavenUtils.saveMavenProject(cappMavenProject, cappMavenProject.getFile());
				} catch (Exception e) {
					log.error("Failed to save the pom file", e);
					return false;
				}
				log.info("All dependencies were converted into system scope");
			}
		}
		return true;
	}

	private String resolveDependencySystemPath(Dependency dependency) {
		StringBuilder dependencyStringBuilder = new StringBuilder();
		dependencyStringBuilder.append(dependency.getGroupId());
		dependencyStringBuilder.append(":");
		dependencyStringBuilder.append(dependency.getArtifactId());
		dependencyStringBuilder.append(":");
		dependencyStringBuilder.append(dependency.getVersion());
		String dependencyString = dependencyStringBuilder.toString();

		String systemPath = dependencySystemPathMap.get(dependencyString);
		if (systemPath != null) {
			return systemPath;
		} else {
			log.error("Could not resolve dependency: " + dependencyString + " from any sub module");
			return "";
		}
	}
}
