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
package org.wso2.maven.p2.publish;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;

import java.io.File;
import java.net.URL;

/**
 * @goal publish-product
 */
public class PublishProductMojo extends AbstractMojo {
	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;
	/**
	 * Metadata repository name
	 *     @parameter
	 */
	private URL metadataRepository;
	/**
	 * Artifact repository name
	 *      @parameter
	 */
	private URL artifactRepository;

    /**
	 * executable
	 *      @parameter
	 */
	private String executable;

    /**
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
    private UnArchiver deflater;


	/**
	 * The product configuration, a .product file. This file manages all aspects
	 * of a product definition from its constituent plug-ins to configuration
	 * files to branding.
	 *
	 * @parameter expression="${productConfiguration}"
	 */
	private File productConfigurationFile;
	/**
     * Parsed product configuration file
     */
    private ProductConfiguration productConfiguration;


    /** @component */
    private P2ApplicationLauncher launcher;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     *
     * @parameter expression="${p2.timeout}"
     */
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
        P2ApplicationLauncher launcher = this.launcher;

        launcher.setWorkingDirectory(project.getBasedir());
        launcher.setApplicationName("org.eclipse.equinox.p2.publisher.ProductPublisher");

        launcher.addArguments(
                "-metadataRepository", metadataRepository.toString(),
                "-artifactRepository", metadataRepository.toString(),
                "-productFile", productConfigurationFile.getCanonicalPath(),
                "-executables", executable.toString(),
                "-publishArtifacts",
                "-configs", "gtk.linux.x86",
                "-flavor", "tooling",
                "-append");

        int result = launcher.execute(forkedProcessTimeoutInSeconds);
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
	}



}