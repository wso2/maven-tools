/*
 * Copyright 2005,2006 WSO2, Inc. http://www.wso2.org
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

/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.developerstudio.eclipse.utils.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.wso2.developerstudio.eclipse.utils.archive.ArchiveManipulator;
import org.wso2.developerstudio.eclipse.utils.data.ITemporaryFileTag;
import org.wso2.developerstudio.eclipse.utils.internal.model.TempFileTag;

public class FileUtils{

	private static Map<String,ITemporaryFileTag> temporaryTags;
	
	
	public FileUtils(){
		super();
	}

	public static void copyFile(String src, String dest) throws Exception {
		InputStream is = null;
		FileOutputStream fos = null;

		try
		{
			is = new FileInputStream(src);
			fos = new FileOutputStream(dest);
			int c = 0;
			byte[] array = new byte[1024];
			while ((c = is.read(array)) >= 0){
				fos.write(array, 0, c);
			}
		}
		catch (Exception e)	{
			throw e;
		}
		finally	{
			try	{
				if (fos!=null) fos.close();
				if (is!=null) is.close();
			}
			catch (Exception e)	{
				throw e;
			}
		}
	}

	public static File createFileAndParentDirectories(String fileName) throws Exception {
		File file = new File(fileName);
		File parent = file.getParentFile();
		if (!parent.exists()){
			parent.mkdirs();
		}
		file.createNewFile();
		return file;
	}
	
	public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

	public static void deleteDirectories(File dir) {
		File[] children = dir.listFiles();
		for (int i = 0; i < children.length; i++){
			if (children[i].list() != null && children[i].list().length > 0){
				deleteDirectories(children[i]);
			}
			else{
				children[i].delete();
			}
		}
		dir.delete();
	}

	public static void deleteDirectories(String dir) {
		File directory = new File(dir);
		deleteDirectories(directory);
	}

	public static void createTargetFile(String sourceFileName, String targetFileName) 
							throws Exception {
		createTargetFile(sourceFileName, targetFileName, false);
	}

	public static void createTargetFile(String sourceFileName, String targetFileName, 
						boolean overwrite) throws Exception{
		File idealResultFile = new File(targetFileName);
		if (overwrite || !idealResultFile.exists())
		{
			FileUtils.createFileAndParentDirectories(targetFileName);
			FileUtils.copyFile(sourceFileName, targetFileName);
		}
	}

	public static boolean createDirectory(String directory){
		// Create a directory; all ancestor directories must exist
		boolean success = (new File(directory)).mkdir();
		if (!success) {
			// Directory creation failed
		}
		return success;  
	}

	public static boolean createDirectories(String directory){
		// Create a directory; all ancestor directories must exist
		boolean success = (new File(directory)).mkdirs();
		if (!success) {
			// Directory creation failed
		}
		return success;  
	}

	//Copies all files under srcDir to dstDir.
	// If dstDir does not exist, it will be created.
	public static void copyDirectory(File srcDir, File dstDir) throws IOException {
		if (srcDir.isDirectory()) {
			if (!dstDir.exists()) {
				dstDir.mkdirs();
			}

			String[] children = srcDir.list();
			for (int i=0; i<children.length; i++) {
				copyDirectory(new File(srcDir, children[i]),
						new File(dstDir, children[i]));
			}
		} else {
			copy(srcDir, dstDir);
		}
	}

	public static void copyDirectoryContents(File srcDir, File dstDir) throws IOException {
		File[] listFiles = srcDir.listFiles();
		for (File srcFile : listFiles) {
			if (srcFile.isDirectory()){
				copyDirectory(srcFile,new File(dstDir,srcFile.getName()));
			}else{
				copy(srcFile,new File(dstDir,srcFile.getName()));
			}
        }
	}
	
	//Copies src file to dst file.
	// If the dst file does not exist, it is created
	public static void copy(File src, File dst) throws IOException {
		if (dst.getParentFile()!=null && !dst.getParentFile().exists()){
			dst.getParentFile().mkdirs();
		}
		InputStream in = new FileInputStream(src);
		OutputStream out=new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public static String addAnotherNodeToPath(String currentPath, String newNode) {
		return currentPath + File.separator + newNode;
	}
	
	public static String addNodesToPath(String currentPath, String[] newNode) {
		StringBuffer sb=new StringBuffer();
		sb.append(currentPath);
		for (int i = 0; i < newNode.length; i++) {
			sb.append(File.separator).append(newNode[i]);
		}
		return sb.toString();
	}
	
	public static String addNodesToPath(StringBuffer currentPath, String[] pathNodes) {
		for (int i = 0; i < pathNodes.length; i++){
			currentPath.append(File.separator).append(pathNodes[i]);
		}
		return currentPath.toString();
	}
	
	public static String addNodesToURL(String currentPath, String[] newNode) {
		StringBuffer sb=new StringBuffer();
		sb.append(currentPath);
		for (int i = 0; i < newNode.length; i++) {
			sb.append("/").append(newNode[i]);
		}
		return sb.toString();
	}
	
    /**
     * Get the list of file with a prefix of <code>fileNamePrefix</code> &amp; an extension of
     * <code>extension</code>
     *
     * @param sourceDir      The directory in which to search the files
     * @param fileNamePrefix The prefix to look for
     * @param extension      The extension to look for
     * @return The list of file with a prefix of <code>fileNamePrefix</code> &amp; an extension of
     *         <code>extension</code>
     */
    public static File[] getMatchingFiles(String sourceDir, String fileNamePrefix, String extension) {
        List fileList = new ArrayList();
        File libDir = new File(sourceDir);
        String libDirPath = libDir.getAbsolutePath();
        File[] items = libDir.listFiles();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
            	if(items[i].isDirectory()){
            		getMatchingFiles(items[i].getPath(), fileNamePrefix, extension);
            	}else{
	                String item = items[i].getName();
	                if (fileNamePrefix != null && extension != null) {
	                    if (item.startsWith(fileNamePrefix) && item.endsWith(extension)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else if (fileNamePrefix == null && extension != null) {
	                    if (item.endsWith(extension)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else if (fileNamePrefix != null && extension == null) {
	                    if (item.startsWith(fileNamePrefix)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else {
	                    fileList.add(new File(libDirPath + File.separator + item));
	                }
            	}
            }
            return (File[]) fileList.toArray(new File[fileList.size()]);
        }
        return new File[0];
    }
    
    public static File[] getAllMatchingFiles(String sourceDir, String fileNamePrefix, String extension, List fileList) {
        
        File libDir = new File(sourceDir);
        String libDirPath = libDir.getAbsolutePath();
        File[] items = libDir.listFiles();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
            	if(items[i].isDirectory()){
            		getAllMatchingFiles(items[i].getPath(), fileNamePrefix, extension, fileList);
            	}else{
	                String item = items[i].getName();
	                if (fileNamePrefix != null && extension != null) {
	                    if (item.startsWith(fileNamePrefix) && item.endsWith(extension)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else if (fileNamePrefix == null && extension != null) {
	                    if (item.endsWith(extension)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else if (fileNamePrefix != null && extension == null) {
	                    if (item.startsWith(fileNamePrefix)) {
	                        fileList.add(new File(libDirPath + File.separator + item));
	                    }
	                } else {
	                    fileList.add(new File(libDirPath + File.separator + item));
	                }
            	}
            }
            return (File[]) fileList.toArray(new File[fileList.size()]);
        }
        return new File[0];
    }
    
    /**
     * Filter out files inside a <code>sourceDir</code> with matching <codefileNamePrefix></code>
     * and <code>extension</code>
     * @param sourceDir 		The directory to filter the files
     * @param fileNamePrefix	The filtering filename prefix 
     * @param extension			The filtering file extension
     */
    public static void filterOutRestrictedFiles(String sourceDir, String fileNamePrefix, String extension){
    	File[] resultedMatchingFiles = getMatchingFiles(sourceDir, fileNamePrefix, extension);
    	for (int i = 0; i < resultedMatchingFiles.length; i++) {
			File matchingFilePath = new File(resultedMatchingFiles[i].getAbsolutePath());
			matchingFilePath.delete();
		}
    }
    
    public static void createFile(File destinationFile, String content) throws IOException{
    	//Create parent folder if it doesn't exist 
    	if (!(destinationFile.getParentFile()==null || destinationFile.getParentFile().exists())){
    		destinationFile.getParentFile().mkdirs();
    	}

        InputStream dataStream=new ByteArrayInputStream(content.getBytes());
        
        createFile(destinationFile, dataStream);
    }

    public static void createFile(File destinationFile, InputStream dataStream)
            throws FileNotFoundException, IOException {
	    FileOutputStream out = new FileOutputStream(destinationFile);
        createFile(dataStream, out);
    }

    public static void createFile(String content, OutputStream out) throws IOException{

        InputStream dataStream=new ByteArrayInputStream(content.getBytes());
        
        createFile(dataStream, out);
    }
    
	public static void createFile(InputStream dataStream, OutputStream out)
			throws IOException {
		byte[] data=new byte[1024];
        int readLength;
        while((readLength=dataStream.read(data))>0){
        	out.write(data,0,readLength);
        }
        
        //Close input stream
        dataStream.close();
        
        //Close the output stream
        out.close();
	}
    
    
    public static void writeContent(File destinationFile, String content) throws IOException{
    	createFile(destinationFile, content);
    }
    
    public static ITemporaryFileTag createNewTempTag(){
    	ITemporaryFileTag tempFileTag = new TempFileTag();
    	getTemporaryTags().put(tempFileTag.getId(), tempFileTag);
    	return tempFileTag;
    }

	public static Map<String,ITemporaryFileTag> getTemporaryTags() {
		if (temporaryTags==null){
			temporaryTags=new HashMap<String, ITemporaryFileTag>();
		}
	    return temporaryTags;
    }
	
	private static void addFileToTempTags(File file){
		for(ITemporaryFileTag tag:getTemporaryTags().values()){
			if (!tag.isTagEnded()){
				tag.getLocations().add(file);
			}
		}
	}
	
	public static File createTempFile() throws IOException{
		File tempFile = File.createTempFile("cstudio", ".tmp");
		addFileToTempTags(tempFile);
		return tempFile;
	}
	
	public static File createTempDirectory() throws IOException{
		File tempDirectory = createTempFile();
		tempDirectory.delete();
		tempDirectory.mkdirs();
		return tempDirectory;
	}
	
	public static boolean isPathValid(String path){
		File file = new File(path);
		if(file.exists()){
			return true;
		}
		return false;
	}
	
	public static Map<File, ArrayList<String>> processJarList(File[] jarsList) throws IOException{
		Map<File, ArrayList<String>> jarInfoMap = new HashMap<File, ArrayList<String>>();
		for (File jarfile : jarsList) {
			File tempDir=extractJar(jarfile);
			List<File> classFiles=searchDir(tempDir);
			if(!jarInfoMap.containsKey(jarfile)){
				jarInfoMap.put(jarfile, exportedPackagesList(classFiles,tempDir));
			}
		}
		
		return jarInfoMap;
	}
	

	public static ArrayList<String> exportedPackagesList(List<File> classFiles, File tempDir){
		List<String> packagesTObeExported = new ArrayList<String>();
		String exportedpackage;
		if(classFiles != null && classFiles.size() != 0){
			for (File classFile : classFiles) {
				exportedpackage = classFile.getParent().substring(tempDir.getPath().length());
				String[] packagesInfo = exportedpackage.split(Pattern.quote(File.separator));
				if (packagesInfo.length>1){
    				StringBuffer sb=new StringBuffer();
    				sb.append(packagesInfo[1]);
    				for (int i =0 ; i< packagesInfo.length - 2;  i++) {
    					sb.append(".").append(packagesInfo[i+2]);
    				}
    				if(!packagesTObeExported.contains(sb.toString())){
    					packagesTObeExported.add(sb.toString());
    				}
				}
			}
		}
		return (ArrayList<String>) packagesTObeExported;
	}
	

	
	public static List<File> searchDir(File file){
		List<File> classFiles=new ArrayList<File>();
		if(file.isDirectory()){
			File[] listFiles = file.listFiles();
			if(listFiles != null){
				for (File file2 : listFiles) {
					if(file2.isDirectory()){
						classFiles.addAll(searchDir(file2));
					}else{
						if(file2.getName().endsWith(".class")){
							if(!classFiles.contains(file2)){
								classFiles.add(file2);
							}
						}
					}
				}
			}else{
				if(file.getName().endsWith(".class")){
					if(!classFiles.contains(file)){
						classFiles.add(file);
					}
					
				}
			}
		}
		return classFiles;
	}
	
	
	public static File extractJar(File jarfile) throws IOException{
		List<File> classFiles = new ArrayList<File>();
		ArchiveManipulator archiveManipulator = new ArchiveManipulator();
		File tempDir = FileUtils.createTempDirectory();
		archiveManipulator.extract(jarfile.getPath(), tempDir.getPath());
		return tempDir;
	}
	
	public static String getContentAsString(URL url) throws IOException {
		InputStream openStream = url.openStream();
	    String contentAsString = getContentAsString(openStream);
	    openStream.close();
		return contentAsString;
    }

	public static String getContentAsString(InputStream dataStream)
			throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		byte[] b=new byte[1024];
		int read = dataStream.read(b);
		while(read!=-1){
			stream.write(b,0,read);
			read = dataStream.read(b);
		}
		String string = stream.toString();
		stream.close();
		return string;
	}
	
	public static String getContentAsString(File file) throws IOException {
		return getContentAsString(file.toURI().toURL());
	}
	
	public static String getResourceFileNameExtension(String resourceName) {
		String[] split = resourceName.split("\\.");
		if (split==null || split.length<2){
			return null;
		}else{
			return split[split.length-1];
		}
	}
	
	public static String getResourceFileName(String resourceName){
		String extension = getResourceFileNameExtension(resourceName);
		if (extension==null){
			return resourceName;
		}else{
			return resourceName.substring(0,resourceName.length()-extension.length()-1);
		}
	}
	
	public static URL getParentURL(URL url) throws MalformedURLException{
		String path=url.toString();
	    if ((path == null) || path.equals("") || path.equals("/")){
	      return null;
	    }
	    if (path.endsWith("/")){
	    	path=path.substring(0,path.length()-1);
	    }
	    
	    int lastSlashPos = path.lastIndexOf('/');

	    if (lastSlashPos >= 0){
	      return new URL(path.substring(0, lastSlashPos));
	    }else{
	      return null;
	    }
	}
	
	public static String getFileName(URL url){
		return getFileName(url.getFile(), "/");
	}
	
	public static String getFileName(String path){
		return getFileName(path, File.pathSeparator);
	}

	public static String getFileName(String path, String pathSeparator){
		String[] split = path.split(pathSeparator);
		if (split==null || split.length==0){
			return path;
		}else{
			return split[split.length-1];
		}
	}
	
	private static List<String> getPathList(File f) {
		List<String> l = new ArrayList<String>();
		File r;
		try {
			r = f.getCanonicalFile();
			while(r != null) {
				l.add(r.getName());
				r = r.getParentFile();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			l = null;
		}
		return l;
	}

	private static String matchPathLists(List<String> r,List<String> f) {
		int i;
		int j;
		StringBuffer sb=new StringBuffer();
		i = r.size()-1;
		j = f.size()-1;

		while((i >= 0)&&(j >= 0)&&(r.get(i).equals(f.get(j)))) {
			i--;
			j--;
		}

		for(;i>=0;i--) {
			sb.append("..").append(File.separator);
		}

		for(;j>=1;j--) {
			sb.append(f.get(j)).append(File.separator);
		}

		sb.append(f.get(j));
		return sb.toString();
	}

	public static String getRelativePath(File home,File f){
		List<String> homelist;
		List<String> filelist;
		String s;

		homelist = getPathList(home);
		filelist = getPathList(f);
		s = matchPathLists(homelist,filelist);

		return s;
	}
    /**
     * Utility method to extract an archive
     * @param archive
     * @param extractDir
     * @throws IOException
     */
	public static void extract(String archive, String extractDir) throws IOException {
        FileInputStream inputStream = new FileInputStream(archive);
        extractFromStream(inputStream, extractDir);
    }
    /**
     * Utility method to extract an archive
     * @param archive
     * @param extractDir
     * @throws IOException
     */
	public static void extract(File archive, File extractDir) throws IOException {
    	extract(archive.toString(), extractDir.toString());
    }

    /**
     * Utility method to extract form a stream to a given directory
     * @param inputStream
     * @param extractDir
     * @throws IOException
     */
	public static void extractFromStream(InputStream inputStream, String extractDir) throws IOException {
        ZipInputStream zin = null;
        try {
            File unzipped = new File(extractDir);
            // Open the ZIP file
            zin = new ZipInputStream(inputStream);
            unzipped.mkdirs();
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String entryName = entry.getName();
                File f = new File(extractDir + File.separator + entryName);

                if (entryName.endsWith("/") && !f.exists()) { // this is a
                    // directory
                    f.mkdirs();
                    continue;
                }

                // This is a file. Carry out File processing
                int lastIndexOfSlash = entryName.lastIndexOf("/");
                String dirPath = "";
                if (lastIndexOfSlash != -1) {
                    dirPath = entryName.substring(0, lastIndexOfSlash);
                    File dir = new File(extractDir + File.separator + dirPath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                }

                if (!f.isDirectory()) {
                    OutputStream out = new FileOutputStream(f);
                    try {
                        byte[] buf = new byte[40960];

                        // Transfer bytes from the ZIP file to the output file
                        int len;
                        while ((len = zin.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } finally {
                        out.close();
                    }
                }
            }
        } catch (IOException e) {
            String msg = "Cannot unzip archive. It is probably corrupt";
            System.err.println(msg);
            throw e;
        } finally {
            try {
                if (zin != null) {
                    zin.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }
}
