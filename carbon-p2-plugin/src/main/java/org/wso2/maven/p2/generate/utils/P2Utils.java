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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.maven.p2.CatFeature;
import org.wso2.maven.p2.Category;
import org.wso2.maven.p2.EquinoxLauncher;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.P2Profile;
import org.wso2.maven.p2.generate.feature.Bundle;

public class P2Utils {
	private static String[] matchList=new String[]{"perfect","equivalent","compatible","greaterOrEqual","patch", "optional"};

	public static void setupLauncherLocation(P2Profile p2Profile, File p2LauncherDir, File p2LauncherPluginDir, EquinoxLauncher equinoxLauncher) throws MojoExecutionException {
        try {
            FileManagementUtil.unzip(p2Profile.getArtifact().getFile(), p2LauncherDir);
            String[] plugins = p2LauncherPluginDir.list();
            boolean found = false;
            for (String plugin : plugins) {
                if (equinoxLauncher.getLauncherJar().equals(plugin))
                    found = true;
            }

            if (!found) {
                File[] listFiles = p2LauncherPluginDir.listFiles();
                for (File file : listFiles) {
                    JarFile jarFile = new JarFile(file);
                    String symbolicName = jarFile.getManifest().getMainAttributes().getValue(Bundle.BUNDLE_SYMBOLIC_NAME);
                    if (symbolicName != null && symbolicName.equals(equinoxLauncher.getLauncherJar())) {
                        equinoxLauncher.setLauncherJar(file.getName());
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new MojoExecutionException("Lanucher jar was not found: " + equinoxLauncher.getLauncherJar());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to setup p2 launcher location", e);
        }
    }
	
	//TODO: Where is this static method called from?
    public static ArrayList<Bundle> getProcessedP2LanucherFiles(ArrayList<Bundle> processedP2LauncherFiles, EquinoxLauncher equinoxLauncher, MavenProject project,RepositorySystem repoSystem, RepositorySystemSession repoSession, List<RemoteRepository> remoteRepositories) throws MojoExecutionException {
        if (processedP2LauncherFiles != null)
            return processedP2LauncherFiles;
        processedP2LauncherFiles = new ArrayList<Bundle>();
        RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, remoteRepositories);
        Iterator iter = equinoxLauncher.getLauncherFiles().iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            Bundle b;
            if (obj instanceof FeatureArtifact) {
                b = (Bundle) obj;
            } else if (obj instanceof String) {
                b = Bundle.getBundle(obj.toString());
            } else
                b = (Bundle) obj;
            try {
                b.resolveVersion(project);
            } catch (MojoExecutionException e) {
                b.setVersion(P2Constants.getDefaultVersion(b.getGroupId(), b.getArtifactId()));
                if (b.getVersion() == null)
                    throw e;
            }
            b.setArtifact(MavenUtils.getResolvedArtifact(repoRefs, b));
            processedP2LauncherFiles.add(b);
        }
        return processedP2LauncherFiles;
    }

    public static File[] getEquinoxLogFiles(File equinoxLaunchLocation) {
        File configurationFolder = new File(equinoxLaunchLocation, "configuration");
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (dir.isFile() && name.endsWith(".log"));
            }
        };
        return configurationFolder.listFiles(filter);
    }

    public static int getLastIndexOfProperties(File p2InfFile) throws IOException {
        int min = -1;
        if (p2InfFile.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(p2InfFile));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] split = line.split("=");
                    String[] split2 = split[0].split(Pattern.quote("."));
                    if (split2[0].equalsIgnoreCase("properties")) {
                        int index = Integer.parseInt(split2[1]);
                        if (index > min)
                            min = index;
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
            try (JarFile jarFile = new JarFile(file)){
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
    
    public static void createCategoryFile(MavenProject project, ArrayList categories, File categoryFile) throws MojoExecutionException {
    	
    	Map<String, List<CatFeature>> featureCategories=new HashMap<>();
    	
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
				ArrayList<CatFeature> processedFeatures = cat.getProcessedFeatures(project);
				for (CatFeature feature : processedFeatures) {
					List<CatFeature> list = null;
					if (!featureCategories.containsKey(feature.getId()+feature.getVersion())){
						//No value for "feature.getId()+feature.getVersion()",
						//Add the value as a List containing only the current CatFeature
						list = new ArrayList<>();
						list.add(feature);
						featureCategories.put((feature.getId()+feature.getVersion()), list);
					} else {
						//Value found for "feature.getId()+feature.getVersion()",
						//Add the current CatFeature to the existing CatFeatures
						list = featureCategories.get(feature.getId()+feature.getVersion());
						list.add(feature);
					}
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
