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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.tycho.model.ProductConfiguration;
import org.wso2.maven.p2.generate.utils.FileManagementUtil;
import org.wso2.maven.p2.generate.utils.P2Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Mojo(name = "materialize-product")
public class MaterializeProductMojo extends AbstractMojo {

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
     * The product configuration, a .product file. This file manages all aspects
     * of a product definition from its constituent plug-ins to configuration
     * files to branding.
     */
    @Parameter(name = "productConfigurationFile", property = "productConfiguration")
    private File productConfigurationFile;

    @Parameter(name = "targetPath")
    private URL targetPath;
    /**
     * Parsed product configuration file
     */
    private ProductConfiguration productConfiguration;

    /**
     * The new profile to be created during p2 Director install &
     * the default profile for the the application which is set in config.ini
     */
    @Parameter(name = "profile", property = "profile")
    private String profile;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(name = "forkedProcessTimeoutInSeconds", property = "p2.timeout")
    private int forkedProcessTimeoutInSeconds;

    public void execute() throws MojoExecutionException, MojoFailureException {
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
        List<String> arguments = new ArrayList<>();

        arguments.add("-metadataRepository");
        arguments.add(metadataRepository.toExternalForm());
        arguments.add("-artifactRepository");
        arguments.add(metadataRepository.toExternalForm());
        arguments.add("-installIU");
        arguments.add(productConfiguration.getId());
        arguments.add("-profileProperties");
        arguments.add("org.eclipse.update.install.features=true");
        arguments.add("-profile");
        arguments.add(profile.toString());
        arguments.add("-bundlepool");
        arguments.add(targetPath.toExternalForm());
                //to support shared installation in carbon
        arguments.add("-shared");
        arguments.add(targetPath.toExternalForm() + File.separator + "p2");
                //target is set to a separate directory per Profile
        arguments.add("-destination");
        arguments.add(targetPath.toExternalForm() + File.separator + profile);
        arguments.add("-p2.os");
        arguments.add("linux");
        arguments.add("-p2.ws");
        arguments.add("gtk");
        arguments.add("-p2.arch");
        arguments.add("x86");
        arguments.add("-roaming");

        Object result = getPublisherApplication().run(arguments.toArray(String[]::new));
        if (result != IApplication.EXIT_OK) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }

    }

    protected DirectorApplication getPublisherApplication() {
        return new DirectorApplication();
    }

    private void setPropertyIfNotNull( Properties properties, String key, String value )
    {
        if ( value != null )
        {
            properties.setProperty( key, value );
        }
    }

}
