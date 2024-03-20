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
package org.wso2.maven.p2;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.p2.publisher.eclipse.ProductPublisherApplication;
import org.eclipse.tycho.model.ProductConfiguration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "publish-product")
public class PublishProductMojo extends AbstractMojo {

	@Parameter(name = "project", property = "project", required = true)
	protected MavenProject project;
	/**
	 * Metadata repository name
	 */
	@Parameter(name = "metadataRepository")
	private URL metadataRepository;
	/**
	 * Artifact repository name
	 */
	@Parameter(name = "artifactRepository")
	private URL artifactRepository;

    /**
	 * executable
	 */
	@Parameter(name = "executable")
	private String executable;

	@Component(role = org.codehaus.plexus.archiver.UnArchiver.class, hint = "zip")
    private UnArchiver deflater;


	/**
	 * The product configuration, a .product file. This file manages all aspects
	 * of a product definition from its constituent plug-ins to configuration
	 * files to branding.
	 */
	@Parameter(name = "productConfigurationFile", property = "productConfiguration")
	private File productConfigurationFile;
	/**
     * Parsed product configuration file
     */
    private ProductConfiguration productConfiguration;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
	@Parameter(name = "forkedProcessTimeoutInSeconds", property = "p2.timeout")
    private int forkedProcessTimeoutInSeconds;


	public void execute() throws MojoExecutionException, MojoFailureException {
		try {

			publishProduct();

        }catch (Exception e) {
			throw new MojoExecutionException("Cannot generate P2 metadata", e);
		}
	}

	private void publishProduct()  throws Exception{

        productConfiguration = ProductConfiguration.read( productConfigurationFile );
		List<String> arguments = new ArrayList<>();

        arguments.add("-metadataRepository");
		arguments.add(metadataRepository.toString());
		arguments.add("-artifactRepository");
		arguments.add(metadataRepository.toString());
		arguments.add("-productFile");
		arguments.add(productConfigurationFile.getCanonicalPath());
//		arguments.add("-executables");
//		arguments.add(executable.toString());
		arguments.add("-publishArtifacts");
		arguments.add("-configs");
		arguments.add("gtk.linux.x86");
		arguments.add("-flavor");
		arguments.add("tooling");
		arguments.add("-append");

		Object result = getPublisherApplication().run(arguments.toArray(String[]::new));
		if (result != IApplication.EXIT_OK) {
			throw new MojoFailureException("P2 publisher return code was " + result);
		}
	}

	protected ProductPublisherApplication getPublisherApplication() {
		return new ProductPublisherApplication();
	}

}
