package org.wso2.maven.bpel.artifact;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.wso2.maven.bpel.artifact.utils.FileUtils;

/**
 * Create a bpel artifact from Maven project
 *
 * @goal bpel
 * @phase package
 * @description build an bpel artifact
 */
public class BPELMojo extends AbstractMojo {

	/**
	 * @parameter default-value="${project.basedir}"
	 */
	private File path;
	
	/**
	 * @parameter default-value="zip"
	 */
	private String type;

	/**
	 * @parameter default-value="false"
	 */
	private boolean enableArchive;

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject mavenProject;

	/**
	 * Maven ProjectHelper.
	 * 
	 * @component
	 */
	private MavenProjectHelper projectHelper;
	
	private static final String BPEL_CONTENT_DIR = "bpelContent";

	public void execute() throws MojoExecutionException, MojoFailureException {
		File project = path;
		File bpelContentDir = new File(project, BPEL_CONTENT_DIR);

		File replacedDir;
		try {
			replacedDir = replaceMavenTokens(bpelContentDir);
		} catch (IOException e) {
			throw new MojoFailureException("Error occured while replacing maven tokens", e);
		}
		createZip(replacedDir);
	}

	private File replaceMavenTokens(File project) throws IOException {
		File tempDir = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createTempDirectory();
		org.wso2.developerstudio.eclipse.utils.file.FileUtils.copyDirectory(project, tempDir);

		List<File> filesListInFolder = FileUtils.getAllFilesPresentInFolder(tempDir);
		for (File file : filesListInFolder) {
			File replacedFile = processTokenReplacement(file);
			if (replacedFile != null) {
				org.wso2.developerstudio.eclipse.utils.file.FileUtils.copy(replacedFile, file);
			}
		}
		return tempDir;
	}

	private File processTokenReplacement(File file) throws IOException {
		if (file.exists()) {
			Properties mavenProperties = mavenProject.getModel().getProperties();

			String fileContent = org.wso2.developerstudio.eclipse.utils.file.FileUtils.getContentAsString(file);
			String newFileContent = stringReplace(fileContent, mavenProperties);
			File tempFile = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createTempFile();
			org.wso2.developerstudio.eclipse.utils.file.FileUtils.writeContent(tempFile, newFileContent);
			return tempFile;
		}
		return file;
	}

	private String stringReplace(String content, Properties mavenProperties) {
		StringBuffer sb = new StringBuffer();
		Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String match = matcher.group(0).replaceAll("^\\$\\{", "");
			match = match.replaceAll("\\}$", "");
			String value = (String) mavenProperties.get(match);
			if (value != null && !value.trim().equals("")) {
				matcher.appendReplacement(sb, value);
				getLog().info("Replacing the token: " + match + " with value: " + value);
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public void createZip(File project) throws MojoExecutionException {
		try {
			String artifactType = getType();
			String artifactName=mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + "." + artifactType; 
			File archive = FileUtils.createArchive(path, project, artifactName);
			if (archive != null && archive.exists()) {
				mavenProject.getArtifact().setFile(archive);
			} else {
				throw new MojoExecutionException(archive + " is null or doesn't exist");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error while creating bpel archive",e);
		}

	}

	public String getBPELProjectName(File project) {
		List<File> fileList = FileUtils.getAllFilesPresentInFolder(project);
		String bpelProjectName = project.getName();
		for (int i = 0; i < fileList.size(); i++) {
			File file = fileList.get(i);
			if (!file.isDirectory()) {
				try {
					if (file.getName().toLowerCase().endsWith(".bpel")) {
						bpelProjectName = file.getParent();
						return bpelProjectName;
					}
				} catch (Exception e) {
				}
			}
		}
		return bpelProjectName;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setEnableArchive(boolean enableArchive) {
		this.enableArchive = enableArchive;
	}

	public boolean isEnableArchive() {
		return enableArchive;
	}

}
