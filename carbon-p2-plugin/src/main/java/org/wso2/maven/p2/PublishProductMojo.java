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


import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.eclipse.ProductPublisherApplication;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

@Mojo(name = "publish-product")
public class PublishProductMojo extends AbstractMojo {
	
	//See https://wiki.eclipse.org/Equinox/p2/Publisher#Product_Publisher

	@Parameter(property = "project", required = true)
	protected MavenProject project;
	/**
	 * Metadata repository name
	 */
	@Parameter
	private URL metadataRepository;
	/**
	 * Artifact repository name
	 */
	@Parameter
	private URL artifactRepository;

    /**
	 * executable
	 */
	@Parameter
	private String executable;

    /**
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
	@Component(role=org.codehaus.plexus.archiver.UnArchiver.class, hint="zip")
    private UnArchiver deflater;

	/**
	 * The product configuration, a .product file. This file manages all aspects
	 * of a product definition from its constituent plug-ins to configuration
	 * files to branding.
	 */
	@Parameter(property = "productConfiguration")
	private File productConfigurationFile;
	
    @Component
	private IProvisioningAgent agent;

	public void execute() throws MojoExecutionException, MojoFailureException {
		agent.getService(IArtifactRepositoryManager.class); //force init P2 services
		
		try {

			publishProduct();

        }catch (Exception e) {
			throw new MojoExecutionException("Cannot generate P2 metadata", e);
		}
	}

	private void publishProduct() throws Exception{
        ProductPublisherApplication application = new ProductPublisherApplication();
        Object result = application.run(getPublishProductConfigurations());
        if (result != IApplication.EXIT_OK ) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
	}
	
	private String[] getPublishProductConfigurations() throws Exception {
    	String[] result = new String[] {
    			"-ar",String.format("%s", metadataRepository.toURI()),
        		"-mr",String.format("%s", metadataRepository.toURI()),
        		"-productFile",productConfigurationFile.getCanonicalPath(),
    			"-executables",executable.toString(),
    			"-configs","gtk.linux.x86",
    			"-flavor","tooling",
    			"-append"
    	};
    	return result; 
    }



}
