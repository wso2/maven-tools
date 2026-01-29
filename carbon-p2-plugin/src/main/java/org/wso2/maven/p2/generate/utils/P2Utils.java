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
package org.wso2.maven.p2.generate.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.maven.p2.CatFeature;
import org.wso2.maven.p2.Category;
import org.wso2.maven.p2.EquinoxLauncher;
import org.wso2.maven.p2.P2Profile;
import org.wso2.maven.p2.generate.feature.Bundle;

public class P2Utils {
	private static String[] matchList=new String[]{"perfect","equivalent","compatible","greaterOrEqual","patch", "optional"};

	public static void setupLauncherLocation(P2Profile p2Profile, File p2LauncherDir, File p2LauncherPluginDir, EquinoxLauncher equinoxLauncher) throws MojoExecutionException {
        try {
            FileManagementUtil.unzip(p2Profile.getArtifact().getFile(), p2LauncherDir);
            String[] plugins = p2LauncherPluginDir.list();
            if (plugins == null) {
            	throw new MojoExecutionException("Unable to list launcher plugins in " + p2LauncherPluginDir);
            }
            boolean found = false;
            for (String plugin : plugins) {
                if (equinoxLauncher.getLauncherJar().equals(plugin))
                    found = true;
            }

            if (!found) {
                File[] listFiles = p2LauncherPluginDir.listFiles();
                if (listFiles == null) {
                	throw new MojoExecutionException("Unable to list launcher plugins in " + p2LauncherPluginDir);
                }
                for (File file : listFiles) {
                	if (!file.isFile()) {
                		continue;
                	}
                	try (JarFile jarFile = new JarFile(file)) {
	                    String symbolicName = jarFile.getManifest().getMainAttributes().getValue(Bundle.BUNDLE_SYMBOLIC_NAME);
	                    if (symbolicName != null && symbolicName.equals(equinoxLauncher.getLauncherJar())) {
	                        equinoxLauncher.setLauncherJar(file.getName());
	                        found = true;
	                        break;
	                    }
                	}
                }
                if (!found)
                    throw new MojoExecutionException("Lanucher jar was not found: " + equinoxLauncher.getLauncherJar());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to setup p2 launcher location", e);
        }
    }

    public static ArrayList getProcessedP2LanucherFiles(ArrayList processedP2LauncherFiles, EquinoxLauncher equinoxLauncher, MavenProject project, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException {
        if (processedP2LauncherFiles != null)
            return processedP2LauncherFiles;
        processedP2LauncherFiles = new ArrayList();
        Iterator iter = equinoxLauncher.getLauncherFiles().iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            Bundle b;
            
            if (obj instanceof Bundle) {
                b = (Bundle) obj;
            } else if (obj instanceof String) {
                b = Bundle.getBundle(obj.toString());
            } else {
            	throw new MojoExecutionException("Unknown launcher file definition: " + obj);
            }
                
            try {
                b.resolveVersion(project);
            } catch (MojoExecutionException e) {
                b.setVersion(P2Constants.getDefaultVersion(b.getGroupId(), b.getArtifactId()));
                if (b.getVersion() == null)
                    throw e;
            }
            b.setArtifact(MavenUtils.getResolvedArtifact(b, artifactFactory, remoteRepositories, localRepository, resolver));
            processedP2LauncherFiles.add(b);
        }
        return processedP2LauncherFiles;
    }

    public static File[] getEquinoxLogFiles(File equinoxLaunchLocation) {
        File configurationFolder = new File(equinoxLaunchLocation, "configuration");
        if (!configurationFolder.isDirectory()) {
        	return new File[0];
        }
        return configurationFolder.listFiles((dir, name) ->
        	name.endsWith(".log") && new File(dir, name).isFile());
    }

    public static int getLastIndexOfProperties(File p2InfFile) throws IOException {
        int min = -1;
        if (p2InfFile.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(p2InfFile));
                String line;
                while ((line = in.readLine()) != null) {
                	if (line.trim().isEmpty()) {
                		continue;
                	}
                	String[] split = line.split("=", 2);
                	if (split.length < 2) {
                		continue;
                	}
                	String[] split2 = split[0].split(Pattern.quote("."));
                	if (split2.length < 2) {
                		continue;
                	}
                	if (split2[0].equalsIgnoreCase("properties")) {
                		try {
                			int index = Integer.parseInt(split2[1]);
                			if (index > min) {
                				min = index;
                			}
                		} catch (NumberFormatException ignored) {
                			// skip malformed line
                		}
                	}
                }
            } catch (FileNotFoundException e) {
                throw e;
            } finally {
                if (in != null) in.close();
            }
        }
        return min;
    }

    public static String getEquinoxLauncherJarLocation(File p2AgentDir) throws Exception {
        File p2AgentPluginsDir = new File(p2AgentDir, "plugins");
        if(!p2AgentPluginsDir.isDirectory()){
            throw new Exception("Please specify a valid location of a P2 Agent Distribution");
        }

        File[] listFiles = p2AgentPluginsDir.listFiles();
        if(listFiles == null){
            throw new Exception("Please specify a valid location of a P2 Agent Distribution");
        }

        for (File file : listFiles) {
        	if (!file.isFile()) {
        		continue;
        	}
        	try (JarFile jarFile = new JarFile(file)) {
	            String symbolicName = jarFile.getManifest().getMainAttributes().getValue(Bundle.BUNDLE_SYMBOLIC_NAME);
	            if (symbolicName != null && symbolicName.equals("org.eclipse.equinox.launcher")) {
	                return file.getAbsolutePath();
	            }
        	}
        }
        //launcher jar is not found. 
        throw new Exception("Please specify a valid location of a P2 Agent Distribution");
    }
    
    public static boolean isMatchString(String matchStr){
    	for (String match : matchList) {
			if (matchStr.equalsIgnoreCase(match)){
				return true;
			}
		}
    	return false;
    }
    
    public static String getMatchRule(String matchStr){
    	if (isPatch(matchStr))
    		return "perfect";
    	for (String match : matchList) {
			if (matchStr.equalsIgnoreCase(match)){
				return match;
			}
		}  
    	return null;
    }
    
    public static boolean isPatch(String matchStr){
    	return matchStr.equalsIgnoreCase("patch");
    }
    
    public static void createCategoryFile(MavenProject project, ArrayList categories, File categoryFile, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver)throws Exception {
    	
    	Map featureCategories=new HashMap();
    	
    	Document doc = MavenUtils.getManifestDocument();
    	Element rootElement = doc.getDocumentElement();
    	
        if (rootElement == null) {
            rootElement = doc.createElement("site");
            doc.appendChild(rootElement);
        }

    	for (Object object : categories) {
			if (object instanceof Category){
				Category cat=(Category)object;
				Element categoryDef = doc.createElement("category-def");
				categoryDef.setAttribute("name", cat.getId());
				categoryDef.setAttribute("label", cat.getLabel());
				rootElement.appendChild(categoryDef);
				Element descriptionElement = doc.createElement("description");
				descriptionElement.setTextContent(cat.getDescription());
				categoryDef.appendChild(descriptionElement);
				ArrayList<CatFeature> processedFeatures = cat.getProcessedFeatures(project, artifactFactory, remoteRepositories, localRepository, resolver);
				for (CatFeature feature : processedFeatures) {
					if (!featureCategories.containsKey(feature.getId()+feature.getVersion())){
						ArrayList list = new ArrayList();
						featureCategories.put((feature.getId()+feature.getVersion()), list);
						list.add(feature);
					}
					ArrayList list = (ArrayList)featureCategories.get(feature.getId()+feature.getVersion());
					list.add(cat.getId());
				}
			}
		}
    	
    	for (Object key : featureCategories.keySet()) {
    		Object object = featureCategories.get(key);
			if (object instanceof List){
				List list=(List)object;
				CatFeature feature=(CatFeature)list.get(0);
				list.remove(0);
				
				Element featureDef = doc.createElement("feature");
				featureDef.setAttribute("id", feature.getId());
				featureDef.setAttribute("version", Bundle.getOSGIVersion(feature.getVersion()));
				for (Object catId : list) {
					Element category = doc.createElement("category");
					category.setAttribute("name", catId.toString());
					featureDef.appendChild(category);
				}
				rootElement.appendChild(featureDef);
			}
		}
    	
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            //TransformerFactory should disallow external access to avoid XXE/SSRF vectors.
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer;
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(categoryFile);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create feature manifest", e);
        }
    }
}
