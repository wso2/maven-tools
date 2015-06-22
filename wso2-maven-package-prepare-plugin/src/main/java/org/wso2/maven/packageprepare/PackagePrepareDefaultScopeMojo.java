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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.wso2.maven.core.utils.MavenUtils;

/**
 * This Maven Mojo is used to change all dependencies to system scope.
 * 
 * @goal default-scope
 * @requiresProject
 * @aggregator
 * @since 1.0.0
 * 
 */
public class PackagePrepareDefaultScopeMojo extends AbstractMojo {

	private final Log log = getLog();

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject project;

	/**
	 * @parameter default-value="${project.packaging}"
	 */
	private String projectPackaging;

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

	public void execute() throws MojoExecutionException, MojoFailureException {
		
		
		/*
		 * 
		 * 
		 * 
		 * Refactor F2F with system mojo
		 * 
		 * 
		 * 
		 * 
		 * 
		 */
		
		log.info("MavenPackagePrepare plugin execution started");
		if (dryRun) {
			log.warn(" **** wso2-maven-package-prepare-plugin does not support dryRun mode **** ");
			throw new MojoExecutionException(" **** wso2-maven-package-prepare-plugin does not support dryRun mode **** ");
		}
		log.info("All dependencies will be converted to default scope");

		String userInput = "";
		do {
			try {
				userInput = prompter.prompt("Continue? (Y/n)");
			} catch (PrompterException e) {
				log.error("Failed to get user input", e);
			}
		} while (!("y".equalsIgnoreCase(userInput) || "n".equalsIgnoreCase(userInput)));

		if ("n".equalsIgnoreCase(userInput)) {
			return;
		} else {
			

			if (!projectPackaging.equals("carbon/application")) {
				log.warn("Selected directory does not contain a Composite Application project");
			}

			@SuppressWarnings("unchecked")
			List<Dependency> dependencies = project.getDependencies();
			for (Dependency dependency : dependencies) {
				// Set default scope
				dependency.setScope(null);
				dependency.setSystemPath(null);
			}

			try {
				MavenUtils.saveMavenProject(project, project.getFile());
			} catch (Exception e) {
				log.error("Failed to update the pom file");
			}

			log.info("All dependencies were converted to default scope");
		}
	}
}
