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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.model.ProductConfiguration;
import org.wso2.maven.p2.generate.utils.FileManagementUtil;
import org.wso2.maven.p2.generate.utils.P2Constants;

@Mojo(name = "materialize-product")
public class MaterializeProductMojo extends AbstractMojo {
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
     * The product configuration, a .product file. This file manages all aspects
     * of a product definition from its constituent plug-ins to configuration
     * files to branding.
     */
	@Parameter(property = "productConfiguration")
    private File productConfigurationFile;

	@Parameter
    private URL targetPath;
    /**
     * Parsed product configuration file
     */
    private ProductConfiguration productConfiguration;

    /**
     * The new profile to be created during p2 Director install &
     * the default profile for the the application which is set in config.ini
     */
    @Parameter(property = "profile")
    private String profile;


    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(property = "p2.timeout")
    private int forkedProcessTimeoutInSeconds;
    
    @Component
	private IProvisioningAgent agent;

    public void execute() throws MojoExecutionException, MojoFailureException {
    	agent.getService(IArtifactRepositoryManager.class); //force init P2 services
    	
        try {
            if (profile == null){
                profile = P2Constants.DEFAULT_PROFILE_ID;
            }
            deployRepository();
            //updating profile's config.ini p2.data.area property using relative path
            File profileConfigIni = FileManagementUtil.getProfileConfigIniFile(targetPath.getPath(), profile);
            FileManagementUtil.changeConfigIniProperty(profileConfigIni, "eclipse.p2.data.area", "@config.dir/../../p2/");
        }catch (Exception e) {
            throw new MojoExecutionException("Cannot generate P2 metadata", e);
        }
    }
    
    private void setPropertyIfNotNull( Properties properties, String key, String value )
    {
        if ( value != null )
        {
            properties.setProperty( key, value );
        }
    }
    
    //Who calls this private method?
    private void regenerateCUs()
            throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Regenerating config.ini" );
        Properties props = new Properties();
        String id = productConfiguration.getId();

        setPropertyIfNotNull(props, "osgi.bundles", getFeaturesOsgiBundles());
        setPropertyIfNotNull( props, "osgi.bundles.defaultStartLevel", "4" );
        if(profile == null){
            profile = "profile";
        }
        setPropertyIfNotNull( props, "eclipse.p2.profile", profile);
        setPropertyIfNotNull( props, "eclipse.product", id );
        setPropertyIfNotNull( props, "eclipse.p2.data.area", "@config.dir/../p2/");
        setPropertyIfNotNull( props, "eclipse.application", productConfiguration.getApplication() );




        File configsFolder = new File( targetPath.toString(), "configuration" );
        configsFolder.mkdirs();

        File configIni = new File( configsFolder, "config.ini" );
        try
        {
            FileOutputStream fos = new FileOutputStream( configIni );
            props.store( fos, "Product Runtime Configuration File" );
            fos.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating .eclipseproduct file.", e );
        }

    }

    private String getFeaturesOsgiBundles() {
        String bundles = "org.eclipse.equinox.common@2:start," +
                "org.eclipse.update.configurator@3:start," +
                "org.eclipse.core.runtime@start,org.eclipse.equinox.ds@1:start," +
                "org.eclipse.equinox.simpleconfigurator@1:start" ;
        return bundles;
    }

    private void deployRepository() throws Exception{
        
    	productConfiguration = ProductConfiguration.read( productConfigurationFile );
    	regenerateCUs();
    	
        DirectorApplication director = new DirectorApplication();
        
        Object result = director.run(getDeployRepositoryConfigurations());

        if (result != IApplication.EXIT_OK) {
        	throw new MojoFailureException("P2 publisher return code was " + result);
        }

    }
    
    private String[] getDeployRepositoryConfigurations() {
    	//The set of configurations is defined within the org.eclipse.equinox.internal.p2.director.app.DirectorApplication class
    	//as OPTION_<something> CommandLineOption objects
    	String[] result = new String[] {
    			"-metadatarepository", metadataRepository.toExternalForm(),
    			"-artifactrepository", artifactRepository.toExternalForm(),
    		    "-installIU",productConfiguration.getId(),
	    		"-profile",profile,
	    		"-bundlepool",targetPath.toExternalForm(),
	    		//to support shared installation in carbon
	    		"-shared",targetPath.toExternalForm() + File.separator + "p2",
	            //target is set to a separate directory per Profile
	            "-destination",targetPath.toExternalForm() + File.separator + profile,
	    		"-p2.os","linux",
	    		"-p2.ws","gtk",
	    		"-p2.arch","x86",
    		"-roaming"
    	};
    	return result; 
    }

    

}
