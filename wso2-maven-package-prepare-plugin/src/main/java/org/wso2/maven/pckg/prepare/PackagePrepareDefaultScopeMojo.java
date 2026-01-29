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

package org.wso2.maven.pckg.prepare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.wso2.maven.core.utils.MavenConstants;
import org.wso2.maven.core.utils.MavenUtils;

/**
 * This Maven Mojo is used to change all dependencies to default scope.
 * 
 * @since 1.0.0
 */
@Mojo(name="default-scope", requiresProject = true, aggregator = true)
public class PackagePrepareDefaultScopeMojo extends AbstractMojo {
	private static final String USER_CONSENT_PROMPTER = "Continue? [Y/n]";
	private static final String USER_CONSENT_YES = "y";
	private static final String USER_CONSENT_NO = "n";
	private static final String USER_CONSENT_DEFAULT = "";

	private final Log log = getLog();

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	@Parameter(property = "dryRun", defaultValue = "false")
	private boolean dryRun;

	@Parameter(property = "updateDependencies", defaultValue = "false")
	private boolean updateDependencies;

	/**
	 * Prompter component for user input
	 */
	@Inject
	private Prompter prompter;

	private List<MavenProject> cappMavenProjects;
	private boolean isDebugEnabled;

	public void execute() throws MojoExecutionException, MojoFailureException {
		isDebugEnabled = log.isDebugEnabled();

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

			// Update pom files of capp projects only
			filterAllCappProjects(mavenProjects);
			if (!updateCappDependencies()) {
				log.error("Plugin execution terminated");
				throw new MojoFailureException("Plugin execution terminated");
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
		List<MavenProject> projectList = new ArrayList<>();

		for (String module : modules) {
			if (isDebugEnabled) {
				log.debug("Reading module: " + module);
			}

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

	private void filterAllCappProjects(List<MavenProject> mavenProjects) {
		cappMavenProjects = new ArrayList<>();

		for (MavenProject mavenProject : mavenProjects) {
			String packaging = mavenProject.getPackaging();
			if (packaging != null && packaging.equals(MavenConstants.CAPP_PACKAGING)) {
				if (isDebugEnabled) {
					log.debug("Identified the composite application project: " + mavenProject.getGroupId() + ":"
							+ mavenProject.getArtifactId() + ":" + mavenProject.getVersion());
				}
				cappMavenProjects.add(mavenProject);
			}
		}
	}

	private boolean updateCappDependencies() {
		for (MavenProject cappMavenProject : cappMavenProjects) {
			log.info("About to update: " + cappMavenProject.getFile().getAbsolutePath());
			log.warn("All dependencies will be converted to default scope");

			// If updateDependencies parameter is provided in command line
			if (updateDependencies) {
				if (!updatePomFile(cappMavenProject)) {
					return false;
				}
			} else {
				// Ask user consent to proceed
				String userInput = "";
				do {
					try {
						userInput = prompter.prompt(USER_CONSENT_PROMPTER);
					} catch (PrompterException e) {
						log.error("Failed to get user input while updating dependencies", e);
						return false;
					}
				} while (!(USER_CONSENT_DEFAULT.equalsIgnoreCase(userInput)
						|| USER_CONSENT_YES.equalsIgnoreCase(userInput) || USER_CONSENT_NO.equalsIgnoreCase(userInput)));

				// Empty input is considered as default (Y)
				if (USER_CONSENT_NO.equalsIgnoreCase(userInput)) {
					log.info("Skipped updating file: " + cappMavenProject.getFile().getAbsolutePath());
				} else {
					if (!updatePomFile(cappMavenProject)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean updatePomFile(MavenProject cappMavenProject) {
		@SuppressWarnings("unchecked")
		List<Dependency> dependencies = cappMavenProject.getDependencies();
		for (Dependency dependency : dependencies) {
			// Set default scope
			dependency.setScope(null);
			dependency.setSystemPath(null);
		}

		try {
			MavenUtils.saveMavenProject(cappMavenProject, cappMavenProject.getFile());
		} catch (Exception e) {
			log.error("Failed to save the pom file", e);
			return false;
		}
		log.info("All dependencies were converted to default scope");
		return true;
	}
}
