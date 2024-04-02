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
package org.wso2.maven.p2.generate.feature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.maven.p2.generate.utils.FileManagementUtil;
import org.wso2.maven.p2.generate.utils.MavenUtils;
import org.wso2.maven.p2.generate.utils.P2Utils;
import org.wso2.maven.p2.generate.utils.PropertyReplacer;
import org.wso2.maven.p2.generate.utils.RepoSystemHolder;

/**
 * Write environment information for the current build to file.
 */
@Mojo(name = "p2-feature-gen", defaultPhase = LifecyclePhase.PACKAGE)
public class FeatureGenMojo extends AbstractMojo {

	/**
	 * feature id
	 */
	@Parameter(required = true)
	private String id;

	// See https://stackoverflow.com/questions/28361289/for-a-maven-3-plugin-what-is-the-latest-way-to-resolve-a-artifact
	// See https://blog.sonatype.com/2011/01/how-to-use-aether-in-maven-plugins/
	// See https://wiki.eclipse.org/Aether/Using_Aether_in_Maven_Plugins
	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 */
	@Component
	private RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	/**
	 * The project's remote repositories to use for the resolution of project
	 * dependencies.
	 */
	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> remoteProjectRepos;

	/**
	 * The project's remote repositories to use for the resolution of plugins and
	 * their dependencies.
	 */
	@Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
	private List<RemoteRepository> remotePluginRepos;

	/**
	 * version
	 */
	@Parameter(defaultValue = "${project.version}")
	private String version;

	/**
	 * label of the feature
	 */
	@Parameter(defaultValue = "${project.name}")
	private String label;

	/**
	 * description of the feature
	 */
	@Parameter(defaultValue = "${project.description}")
	private String description;

	/**
	 * provider name
	 */
	@Parameter(defaultValue = "%providerName")
	private String providerName;

	/**
	 * copyright
	 */
	@Parameter(defaultValue = "%copyright")
	private String copyright;

	/**
	 * license URL
	 */
	@Parameter(defaultValue = "%licenseURL")
	private String licenceUrl;

	/**
	 * license
	 */
	@Parameter(defaultValue = "%license")
	private String licence;

	/**
	 * path to manifest file
	 */
	@Parameter
	private File manifest;

	/**
	 * path to properties file
	 */
	@Parameter
	private File propertiesFile;

	/**
	 * list of properties precedence over propertiesFile
	 */
	@Parameter
	private Properties properties;

	/**
	 * Collection of bundles
	 */
	@Parameter
	private ArrayList bundles;

	/**
	 * Collection of import bundles
	 */
	@Parameter
	private ArrayList importBundles;

	/**
	 * Collection of required Features
	 */
	@Parameter
	private ArrayList importFeatures;

	/**
	 * Collection of required Features
	 */
	@Parameter
	private ArrayList includedFeatures;

	/**
	 * define advice file content
	 */
	@Parameter
	private AdviceFile adviceFile;

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	/**
	 * Maven ProjectHelper.
	 */
	@Component
	private MavenProjectHelper projectHelper;

	private ArrayList<Bundle> processedBundles;
	private ArrayList<ImportBundle> processedImportBundles;
	private ArrayList<ImportFeature> processedImportfeatures;
	private ArrayList<Property> processedAdviceProperties;
	private ArrayList<IncludedFeature> processedIncludedFeatures;

	private File destFolder;
	private File featureBaseDir;
	private File featuresDir;
	private File FOLDER_FEATURES_FEATURE;
	private File pluginsDir;
	private File FOLDER_RESOURCES;
	private File FILE_FEATURE_XML;
	private File FILE_P2_INF;
	private File FILE_FEATURE_PROPERTIES;
	private File FILE_FEATURE_MANIFEST;
	private File FILE_FEATURE_ZIP;

	private boolean isPropertiesLoadedFromFile = false;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getProcessedBundlesList();
		getProcessedImportBundlesList();
		getProcessedImportFeaturesList();
		getProcessedAdviceProperties();
		createAndSetupPaths();
		copyResources();
		createFeatureXml();
		createPropertiesFile();
		createManifestMFFile();
		createP2Inf();
		copyAllDependencies();
		createArchive();
		deployArtifact();
		performMopUp();
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	private ArrayList<Bundle> getProcessedBundlesList() throws MojoExecutionException {
		if (processedBundles != null)
			return processedBundles;
		if (bundles == null || bundles.size() == 0)
			return null;
		processedBundles = new ArrayList<Bundle>();
		RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, Stream.concat(remotePluginRepos.stream(), remoteProjectRepos.stream()).collect(Collectors.toList()));
		Iterator iter = bundles.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			Bundle b;
			if (obj instanceof Bundle) {
				b = (Bundle) obj;
				//this.getLog().info("[getProcessedBundlesList] Bundle = "+b.toOSGIString());
			} else if (obj instanceof String) {
				//this.getLog().info("[getProcessedBundlesList] String1 = "+obj.toString());
				b = Bundle.getBundle(obj.toString());
			} else
				b = (Bundle) obj;
			b.resolveVersion(project);
			b.setArtifact(MavenUtils.getResolvedArtifact(repoRefs, b));
			processedBundles.add(b);
		}
		return processedBundles;
	}

	private ArrayList<ImportBundle> getProcessedImportBundlesList() throws MojoExecutionException {
		if (processedImportBundles != null)
			return processedImportBundles;
		if (importBundles == null || importBundles.size() == 0)
			return null;
		processedImportBundles = new ArrayList<ImportBundle>();
		RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, Stream.concat(remotePluginRepos.stream(), remoteProjectRepos.stream()).collect(Collectors.toList()));
		Iterator iter = importBundles.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			ImportBundle b;
			if (obj instanceof ImportBundle) {
				b = (ImportBundle) obj;
				this.getLog().info("[getProcessedImportBundlesList] String1 = "+obj.toString());
			} else if (obj instanceof String) {
				b = ImportBundle.getBundle(obj.toString());
				this.getLog().info("[getProcessegetProcessedImportBundlesListdBundlesList] String1 = "+b.toOSGIString());
			} else
				b = (ImportBundle) obj;
			b.resolveVersion(project);
			if (!b.isExclude()) {
				b.setArtifact(MavenUtils.getResolvedArtifact(repoRefs, b));
			}else
				b.resolveOSGIInfo();
			processedImportBundles.add(b);
		}
		return processedImportBundles;
	}

	private ArrayList<ImportFeature> getProcessedImportFeaturesList() throws MojoExecutionException {
		if (processedImportfeatures != null)
			return processedImportfeatures;
		if (importFeatures == null || importFeatures.size() == 0)
			return null;
		processedImportfeatures = new ArrayList<ImportFeature>();
		Iterator iter = importFeatures.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			ImportFeature f;
			if (obj instanceof ImportFeature) {
				f = (ImportFeature) obj;
				//this.getLog().info("[getProcessedImportFeaturesList] String1 = "+f.toString());
			} else if (obj instanceof String) {
				f = ImportFeature.getFeature(obj.toString());
				//this.getLog().info("[getProcessedImportFeaturesList] String2 = "+f.toString());
			} else
				f = (ImportFeature) obj;
			f.setFeatureVersion(project.getVersion());
			processedImportfeatures.add(f);
		}
		return processedImportfeatures;
	}

	private ArrayList<IncludedFeature> getIncludedFeatures() throws MojoExecutionException {
		if (processedIncludedFeatures != null)
			return processedIncludedFeatures;

		if (includedFeatures == null || includedFeatures.size() == 0)
			return null;
		
		RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, Stream.concat(remotePluginRepos.stream(), remoteProjectRepos.stream()).collect(Collectors.toList()));
		processedIncludedFeatures = new ArrayList<IncludedFeature>(includedFeatures.size());
		for (Object obj : includedFeatures) {
			if (obj instanceof String) {
				IncludedFeature includedFeature = IncludedFeature.getIncludedFeature((String) obj);
				if (includedFeature != null) {
					includedFeature.setFeatureVersion(project.getVersion());
					DefaultArtifact artifact = new DefaultArtifact(includedFeature.getGroupId(), includedFeature.getArtifactId(), org.apache.maven.artifact.Artifact.SCOPE_RUNTIME, "zip", includedFeature.getArtifactVersion());
					includedFeature.setArtifact(
							MavenUtils.getResolvedArtifact(this.getLog(), repoRefs, artifact));
					processedIncludedFeatures.add(includedFeature);
				}
			}
		}
		return processedIncludedFeatures;
	}

	private void createAndSetupPaths() {
		destFolder = new File(project.getBasedir(), "target");
		featureBaseDir = new File(destFolder, "raw");
		featuresDir = new File(featureBaseDir, "features");
		FOLDER_FEATURES_FEATURE = new File(featuresDir, id + "_" + Bundle.getOSGIVersion(getVersion()));
		pluginsDir = new File(featureBaseDir, "plugins");
		FOLDER_RESOURCES = new File(project.getBasedir(), "src");
		File FOLDER_FEATURES_FEATURE_META_INF = new File(FOLDER_FEATURES_FEATURE, "META-INF");
		FILE_FEATURE_XML = new File(FOLDER_FEATURES_FEATURE, "feature.xml");
		FILE_FEATURE_PROPERTIES = new File(FOLDER_FEATURES_FEATURE, "feature.properties");
		FILE_P2_INF = new File(FOLDER_FEATURES_FEATURE, "p2.inf");
		FILE_FEATURE_MANIFEST = new File(FOLDER_FEATURES_FEATURE_META_INF, "MANIFEST.MF");
		FILE_FEATURE_ZIP = new File(destFolder, project.getArtifactId() + "-" + project.getVersion() + ".zip");
		FOLDER_FEATURES_FEATURE_META_INF.mkdirs();
		pluginsDir.mkdirs();
	}

	private Document getManifestDocument() throws MojoExecutionException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			throw new MojoExecutionException("Unable to load feature manifest", e1);
		}
		Document document;
		if (getManifest() != null && getManifest().exists()) {
			try {
				document = documentBuilder.parse(new FileInputStream(getManifest()));
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to load feature manifest", e);
			}
		} else
			document = documentBuilder.newDocument();
		return document;
	}

	private void createFeatureXml() throws MojoExecutionException {
		getLog().info("Generating feature manifest");
		Document document = getManifestDocument();
		Element rootElement = document.getDocumentElement();
		if (rootElement == null) {
			rootElement = document.createElement("feature");
			document.appendChild(rootElement);
		}
		if (!rootElement.hasAttribute("id"))
			rootElement.setAttribute("id", id);
		if (!rootElement.hasAttribute("label"))
			rootElement.setAttribute("label", getLabel());
		if (!rootElement.hasAttribute("version"))
			rootElement.setAttribute("version", Bundle.getOSGIVersion(getVersion()));
		if (!rootElement.hasAttribute("provider-name"))
			rootElement.setAttribute("provider-name", getProviderName());
		NodeList descriptionTags = rootElement.getElementsByTagName("description");
		Node description;
		if (descriptionTags.getLength() == 0) {
			description = document.createElement("description");
			description.setTextContent(getDescription());
			rootElement.appendChild(description);
		} else
			description = descriptionTags.item(0);

		NodeList copyrightTags = rootElement.getElementsByTagName("copyright");
		Node copyright;
		if (copyrightTags.getLength() == 0) {
			copyright = document.createElement("copyright");
			copyright.setTextContent(getCopyright());
			rootElement.appendChild(copyright);
		} else
			copyright = copyrightTags.item(0);

		NodeList licenseTags = rootElement.getElementsByTagName("license");
		Node license;
		if (licenseTags.getLength() == 0) {
			license = document.createElement("license");
			((Element) license).setAttribute("url", getLicenceUrl());
			license.setTextContent(getLicence());
			rootElement.appendChild(license);
		} else
			license = licenseTags.item(0);

		ArrayList<Object> processedMissingPlugins = getMissingPlugins(document);
		ArrayList<Object> processedMissingImportPlugins = getMissingImportPlugins(document);
		ArrayList<Object> processedMissingImportFeatures = getMissingImportFeatures(document);
		ArrayList<IncludedFeature> includedFeatures = getIncludedFeatures();

		if (processedMissingPlugins != null) {
			for (Iterator<Object> iterator = processedMissingPlugins.iterator(); iterator.hasNext();) {
				Bundle bundle = (Bundle) iterator.next();
				Element plugin = document.createElement("plugin");
				plugin.setAttribute("id", bundle.getBundleSymbolicName());
				plugin.setAttribute("version", bundle.getBundleVersion());
				plugin.setAttribute("unpack", "false");
				rootElement.appendChild(plugin);
			}
		}

		if (processedMissingImportPlugins != null || processedMissingImportFeatures != null) {
			NodeList requireNodes = document.getElementsByTagName("require");
			Node require;
			if (requireNodes == null || requireNodes.getLength() == 0) {
				require = document.createElement("require");
				rootElement.appendChild(require);
			} else
				require = requireNodes.item(0);
			if (processedMissingImportPlugins != null) {
				for (Iterator<Object> iterator = processedMissingImportPlugins.iterator(); iterator.hasNext();) {
					ImportBundle bundle = (ImportBundle) iterator.next();
					Element plugin = document.createElement("import");
					plugin.setAttribute("plugin", bundle.getBundleSymbolicName());
					plugin.setAttribute("version", bundle.getBundleVersion());
					plugin.setAttribute("match", P2Utils.getMatchRule(bundle.getCompatibility()));
					require.appendChild(plugin);
				}
			}
			if (processedMissingImportFeatures != null) {
				for (Object processedMissingImportFeature : processedMissingImportFeatures) {
					ImportFeature feature = (ImportFeature) processedMissingImportFeature;
					if (!feature.isOptional()) {
						Element plugin = document.createElement("import");
						plugin.setAttribute("feature", feature.getFeatureId());
						plugin.setAttribute("version", feature.getFeatureVersion());
						if (P2Utils.isPatch(feature.getCompatibility()))
							plugin.setAttribute("patch", "true");
						else
							plugin.setAttribute("match", P2Utils.getMatchRule(feature.getCompatibility()));
						require.appendChild(plugin);
					}
				}
			}
		}

		if (includedFeatures != null) {
			for (IncludedFeature includedFeature : includedFeatures) {
				Element includeElement = document.createElement("includes");
				includeElement.setAttribute("id", includedFeature.getFeatureID());
				includeElement.setAttribute("version", includedFeature.getFeatureVersion());
				includeElement.setAttribute("optional", Boolean.toString(includedFeature.isOptional()));
				rootElement.appendChild(includeElement);
			}
		}

		if (processedMissingImportFeatures != null) {
			for (Object processedMissingImportFeature : processedMissingImportFeatures) {
				ImportFeature feature = (ImportFeature) processedMissingImportFeature;
				if (feature.isOptional()) {
					Element includeElement = document.createElement("includes");
					includeElement.setAttribute("id", feature.getFeatureId());
					includeElement.setAttribute("version", feature.getFeatureVersion());
					includeElement.setAttribute("optional", Boolean.toString(feature.isOptional()));
					rootElement.appendChild(includeElement);
				}
			}
		}

		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(FILE_FEATURE_XML);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to create feature manifest", e);
		}
	}

	private ArrayList<Object> getMissingPlugins(Document document) throws MojoExecutionException {
		HashMap<String, Bundle> missingPlugins = new HashMap<String, Bundle>();
		ArrayList<Bundle> processedBundlesList = getProcessedBundlesList();
		if (processedBundlesList == null)
			return null;
		for (Iterator<Bundle> iterator = processedBundlesList.iterator(); iterator.hasNext();) {
			Bundle bundle = iterator.next();
			missingPlugins.put(bundle.getArtifactId(), bundle);
		}
		NodeList existingPlugins = document.getDocumentElement().getElementsByTagName("plugin");
		for (int i = 0; i < existingPlugins.getLength(); i++) {
			Node node = existingPlugins.item(i);
			Node namedItem = node.getAttributes().getNamedItem("id");
			if (namedItem != null && namedItem.getTextContent() != null
					&& missingPlugins.containsKey(namedItem.getTextContent())) {
				missingPlugins.remove(namedItem.getTextContent());
			}
		}
		return returnArrayList(missingPlugins.values().toArray());
	}

	private void createPropertiesFile() throws MojoExecutionException {
		Properties props = getProperties();
		if (props == null)
			return;
		if (!props.isEmpty())
			try {
				getLog().info("Generating feature properties");
				props.store(new FileOutputStream(FILE_FEATURE_PROPERTIES), "Properties of " + id);
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to create the feature properties", e);
			}
	}

	private void createManifestMFFile() throws MojoExecutionException {
		try {
			getLog().info("Generating MANIFEST.MF");
			BufferedWriter out = new BufferedWriter(new FileWriter(FILE_FEATURE_MANIFEST));
			out.write("Manifest-Version: 1.0\n\n");
			out.close();
		} catch (Exception e) {// Catch exception if any
			throw new MojoExecutionException("Unable to create manifest file", e);
		}
	}

	private void createP2Inf() throws MojoExecutionException {
		List<String> p2infStringList = null;
		ArrayList<Property> list = getProcessedAdviceProperties();

		if (FILE_P2_INF.exists()) {
			p2infStringList = readAdviceFile(FILE_P2_INF.getAbsolutePath()); // In memory storage of current p2.inf content
			getLog().info("Updating Advice file (p2.inf)");
		} else {
			getLog().info("Generating Advice file (p2.inf)");
		}
		
		try (   //Use try-with-resources to manage autoclosing of writers
				FileWriter fw = new FileWriter(FILE_P2_INF.getAbsolutePath()); 
				BufferedWriter out = new BufferedWriter(fw)
			){
			// re-writing the already available p2.inf lines
			Properties properties = new Properties();
			properties.setProperty("feature.version", Bundle.getOSGIVersion(getVersion()));
			if (p2infStringList != null && p2infStringList.size() > 0) {
				for (String str : p2infStringList) {
					// writing the strings after replacing ${feature.version}
					out.write(PropertyReplacer.replaceProperties(str, properties) + "\n"); 
				}
			}
			if (list.size() == 0)
				return; 
			int nextIndex = P2Utils.getLastIndexOfProperties(FILE_P2_INF) + 1;
			for (Object category : list) {
				Property cat = (Property) category;
				out.write("\nproperties." + nextIndex + ".name=" + cat.getKey());
				out.write("\nproperties." + nextIndex + ".value=" + cat.getValue());
				nextIndex++;
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to create/open p2.inf file", e);
		}
	}

	private List<String> readAdviceFile(String absolutePath) throws MojoExecutionException {
		List<String> advices = new ArrayList<>();
		
		try (FileReader fr = new FileReader(absolutePath); BufferedReader br = new BufferedReader(fr)){
			br.lines().forEach(advices::add);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("Unable to create/open p2.inf file", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Error while reading from p2.inf file", e);
		} 

		return advices;
	}

	private ArrayList<Object> getMissingImportPlugins(Document document) throws MojoExecutionException {
		HashMap<String, ImportBundle> missingImportPlugins = new HashMap<String, ImportBundle>();
		ArrayList<ImportBundle> processedImportBundlesList = getProcessedImportBundlesList();
		if (processedImportBundlesList == null)
			return null;
		for (Iterator<ImportBundle> iterator = processedImportBundlesList.iterator(); iterator.hasNext();) {
			ImportBundle bundle = iterator.next();
			missingImportPlugins.put(bundle.getArtifactId(), bundle);
		}
		NodeList requireNodeList = document.getDocumentElement().getElementsByTagName("require");
		if (requireNodeList == null || requireNodeList.getLength() == 0)
			return returnArrayList(missingImportPlugins.values().toArray());
		Node requireNode = requireNodeList.item(0);
		if (requireNode instanceof Element) {
			Element requireElement = (Element) requireNode;
			NodeList importNodes = requireElement.getElementsByTagName("import");
			if (importNodes == null)
				return returnArrayList(missingImportPlugins.values().toArray());
			for (int i = 0; i < importNodes.getLength(); i++) {
				Node node = importNodes.item(i);
				Node namedItem = node.getAttributes().getNamedItem("plugin");
				if (namedItem != null && namedItem.getTextContent() != null
						&& missingImportPlugins.containsKey(namedItem.getTextContent())) {
					missingImportPlugins.remove(namedItem.getTextContent());
				}
			}
		}
		return returnArrayList(missingImportPlugins.values().toArray());
	}

	private ArrayList<Object> getMissingImportFeatures(Document document) throws MojoExecutionException {
		HashMap<String, ImportFeature> missingImportFeatures = new HashMap<String, ImportFeature>();
		ArrayList<ImportFeature> processedImportFeaturesList = getProcessedImportFeaturesList();
		if (processedImportFeaturesList == null)
			return null;
		for (Iterator<ImportFeature> iterator = processedImportFeaturesList.iterator(); iterator.hasNext();) {
			ImportFeature feature = iterator.next();
			missingImportFeatures.put(feature.getFeatureId(), feature);
		}
		NodeList requireNodeList = document.getDocumentElement().getElementsByTagName("require");
		if (requireNodeList == null || requireNodeList.getLength() == 0)
			return returnArrayList(missingImportFeatures.values().toArray());
		Node requireNode = requireNodeList.item(0);
		if (requireNode instanceof Element) {
			Element requireElement = (Element) requireNode;
			NodeList importNodes = requireElement.getElementsByTagName("import");
			if (importNodes == null)
				return returnArrayList(missingImportFeatures.values().toArray());
			for (int i = 0; i < importNodes.getLength(); i++) {
				Node node = importNodes.item(i);
				Node namedItem = node.getAttributes().getNamedItem("feature");
				if (namedItem != null && namedItem.getTextContent() != null
						&& missingImportFeatures.containsKey(namedItem.getTextContent())) {
					missingImportFeatures.remove(namedItem.getTextContent());
				}
			}
		}
		return returnArrayList(missingImportFeatures.values().toArray());
	}

	private ArrayList<Object> returnArrayList(Object[] arr) {
		ArrayList<Object> arrayList = new ArrayList<Object>();
		for (Object object : arr) {
			arrayList.add(object);
		}
		return arrayList;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setCopyright(String copyrite) {
		this.copyright = copyrite;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setLicenceUrl(String licenceUrl) {
		this.licenceUrl = licenceUrl;
	}

	public String getLicenceUrl() {
		return licenceUrl;
	}

	public void setLicence(String licence) {
		this.licence = licence;
	}

	public String getLicence() {
		return licence;
	}

	public void setManifest(File manifest) {
		this.manifest = manifest;
	}

	public File getManifest() {
		return manifest;
	}

	public void setPropertiesFile(File propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	public File getPropertiesFile() {
		return propertiesFile;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() throws MojoExecutionException {
		if (!isPropertiesLoadedFromFile) {
			isPropertiesLoadedFromFile = true;
			if (getPropertiesFile() != null && getPropertiesFile().exists()) {
				Properties props = new Properties();
				try {
					props.load(new FileInputStream(getPropertiesFile()));
				} catch (Exception e) {
					throw new MojoExecutionException("Unable to load the given properties file", e);
				}
				if (properties != null) {
					for (Object key : properties.keySet().toArray()) {
						props.setProperty(key.toString(), properties.getProperty(key.toString()));
					}
				}
				setProperties(props);
			}
		}
		return properties;
	}

	private ArrayList<Property> getProcessedAdviceProperties() throws MojoExecutionException {
		if (processedAdviceProperties != null)
			return processedAdviceProperties;
		processedAdviceProperties = new ArrayList<Property>();
		
		if (adviceFile != null && adviceFile.getProperties() != null) {
			for (Object property : adviceFile.getProperties()) {
				Property prop = null;
				if (property instanceof Property) {
					prop = (Property) property;
					//this.getLog().info("[getProcessedAdviceProperties] String2 = "+prop.toString());
				} else if (property instanceof String) {
					prop = Property.getProperty(property.toString());
					//this.getLog().info("[getProcessedAdviceProperties] String2 = "+prop.toString());
				} else
					throw new MojoExecutionException("Unknown advice property definition: " + property.toString());
				processedAdviceProperties.add(prop);
			}
		}
		return processedAdviceProperties;
	}

	private void copyAllDependencies() throws MojoExecutionException {
		ArrayList<Bundle> processedBundlesList = getProcessedBundlesList();
		if (processedBundlesList != null) {
			getLog().info("Copying bundle dependencies");
			for (Iterator<Bundle> iterator = processedBundlesList.iterator(); iterator.hasNext();) {
				Bundle bundle = iterator.next();
				try {
					getLog().info("   " + bundle.toOSGIString());
					String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
					FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsDir, bundleName));
				} catch (IOException e) {
					throw new MojoExecutionException("Unable copy dependency: " + bundle.getArtifactId(), e);
				}
			}
		}
		ArrayList<ImportBundle> processedImportBundlesList = getProcessedImportBundlesList();
		if (processedImportBundlesList != null) {
			getLog().info("Copying import bundle dependencies");
			for (Iterator<ImportBundle> iterator = processedImportBundlesList.iterator(); iterator.hasNext();) {
				ImportBundle bundle = iterator.next();
				try {
					if (!bundle.isExclude()) {
						getLog().info("   " + bundle.toOSGIString());
						String bundleName = bundle.getBundleSymbolicName() + "-" + bundle.getBundleVersion() + ".jar";
						FileUtils.copyFile(bundle.getArtifact().getFile(), new File(pluginsDir, bundleName));
					}
				} catch (IOException e) {
					throw new MojoExecutionException("Unable copy import dependency: " + bundle.getArtifactId(), e);
				}
			}
		}

		// Copying includedFeatures
		if (processedIncludedFeatures != null) {
			for (IncludedFeature includedFeature : processedIncludedFeatures) {
				try {
					getLog().info("Extracting feature " + includedFeature.getGroupId() + ":"
							+ includedFeature.getArtifactId());
					FileManagementUtil.unzip(includedFeature.getArtifact().getFile(), featureBaseDir);
				} catch (Exception e) {
					throw new MojoExecutionException("Error occured when extracting the Feature Artifact: "
							+ includedFeature.getGroupId() + ":" + includedFeature.getArtifactId(), e);
				}
			}
		}

	}

	private void createArchive() throws MojoExecutionException {
		getLog().info("Generating feature archive: " + FILE_FEATURE_ZIP.getAbsolutePath());
		FileManagementUtil.zipFolder(featureBaseDir.getAbsolutePath(), FILE_FEATURE_ZIP.getAbsolutePath());
	}

	private void deployArtifact() {
		if (FILE_FEATURE_ZIP != null && FILE_FEATURE_ZIP.exists()) {
			project.getArtifact().setFile(FILE_FEATURE_ZIP);
			projectHelper.attachArtifact(project, "zip", null, FILE_FEATURE_ZIP);
		}
	}

	private void copyResources() throws MojoExecutionException {

		// The following code was taken from the maven bundle plugin and updated suit
		// the purpose
		List<Resource> resources = project.getResources();
		for (Resource resource : resources) {
			String sourcePath = resource.getDirectory();
			if (new File(sourcePath).exists()) {
				DirectoryScanner scanner = new DirectoryScanner();
				scanner.setBasedir(resource.getDirectory());
				if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
					scanner.setIncludes((String[]) resource.getIncludes().toArray(new String[] {}));
				} else {
					scanner.setIncludes(new String[] { "**/**" });
				}

				List<String> excludes = resource.getExcludes();
				if (excludes != null && !excludes.isEmpty()) {
					scanner.setExcludes((String[]) excludes.toArray(new String[] {}));
				}

				scanner.addDefaultExcludes();
				scanner.scan();

				List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());
				getLog().info("   " + resource.getDirectory());
				for (String name : includedFiles) {
					File fromPath = new File(sourcePath, name);
					File toPath = new File(FOLDER_FEATURES_FEATURE, name);

					try {
						if (fromPath.isDirectory() && !toPath.exists()) {
							toPath.mkdirs();
						} else {
							FileManagementUtil.copy(fromPath, toPath);
						}
					} catch (IOException e) {
						throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
					}
				}
			}
		}
	}

	private void performMopUp() {
		try {
			FileUtils.deleteDirectory(featureBaseDir);
		} catch (Exception e) {
			getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
		}
	}
}
