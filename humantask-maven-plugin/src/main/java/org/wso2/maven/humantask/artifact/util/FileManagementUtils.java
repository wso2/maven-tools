/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.maven.humantask.artifact.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.maven.humantask.artifact.HumanTaskPluginConstants;

public class FileManagementUtils {

	private static final Logger logger = Logger.getLogger(FileManagementUtils.class);

	/**
	 * Creates the humantask archive for the artifact
	 * @param location
	 * @param artifactLocation
	 * @param artifactName
	 * @return created humantask archive file
	 * @throws Exception
	 */
	public static File createArchive(File location, File artifactLocation, String artifactName)
			throws Exception {
		File targetFolder;
		targetFolder = new File(location.getPath(), HumanTaskPluginConstants.TARGET_FOLDER_NAME);
		File humantaskDataFolder = new File(targetFolder, HumanTaskPluginConstants.HUMANTASK_DATA_FOLDER_NAME);
		if(!humantaskDataFolder.mkdirs()){
			logger.error(HumanTaskPluginConstants.ERROR_CREATING_CORRESPONDING_ZIP_FILE);
		}
		File zipFolder = new File(humantaskDataFolder, artifactLocation.getName());
		if(!zipFolder.mkdirs()){
			logger.error(HumanTaskPluginConstants.ERROR_CREATING_CORRESPONDING_ZIP_FILE);
		};
		FileUtils.copyDirectory(artifactLocation, zipFolder);
		File zipFile = new File(targetFolder, artifactName);
		zipFolder(zipFolder.getAbsolutePath(), zipFile.toString());
		org.apache.commons.io.FileUtils.deleteDirectory(humantaskDataFolder);
		return zipFile;
	}

	/**creates a zip file of the given folder
	 * @param srcFolder
	 * @param destZipFile
	 */
	static public void zipFolder(String srcFolder, String destZipFile) {
		try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
				ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
			addFolderContentsToZip(srcFolder, zip);
			zip.flush();
		} catch (IOException ex) {
			logger.error(HumanTaskPluginConstants.ERROR_CREATING_CORRESPONDING_ZIP_FILE, ex);
		}
	}

	/**
	 * Adds a file to a zip archive in a given path
	 * @param path
	 * @param srcFile
	 * @param zip
	 */
	static private void addToZip(String path, String srcFile, ZipOutputStream zip) {

		File folder = new File(srcFile);

		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			if (!srcFile.equals(".project")) {
				byte[] buf = new byte[1024];
				int len;
				try (FileInputStream in = new FileInputStream(srcFile)) {
					String location = folder.getName();
					if (!StringUtils.isBlank(path)) {
						location = path + File.separator + folder.getName();
					}
					zip.putNextEntry(new ZipEntry(location));
					while ((len = in.read(buf)) > 0) {
						zip.write(buf, 0, len);
					}
				} catch (IOException e) {
					logger.error(HumanTaskPluginConstants.ERROR_CREATING_CORRESPONDING_ZIP_FILE, e);
				}
			}
		}
	}

	/**
	 * Adds all the contents of a folder in to a zip archive
	 * @param srcFolder
	 * @param zip
	 */
	static private void addFolderContentsToZip(String srcFolder, ZipOutputStream zip) {
		File folder = new File(srcFolder);
		String fileListArray[] = folder.list();
		int i = 0;
		if (fileListArray != null) {
			while (i < fileListArray.length) {
				addToZip(HumanTaskPluginConstants.EMPTY_STRING, srcFolder + File.separator + fileListArray[i], zip);
				i++;
			}
		}
	}

	/**
	 * Adds a folder inside a zip archive
	 * @param path
	 * @param srcFolder
	 * @param zip
	 */
	static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) {
		File folder = new File(srcFolder);
		String fileListArray[] = folder.list();
		int i = 0;
		if (fileListArray != null) {
			while (i < fileListArray.length) {
				String newPath = folder.getName();
				if (!StringUtils.isBlank(path)) {
					newPath = path + File.separator + newPath;
				}
				addToZip(newPath, srcFolder + File.separator + fileListArray[i], zip);
				i++;
			}
		}
	}
}