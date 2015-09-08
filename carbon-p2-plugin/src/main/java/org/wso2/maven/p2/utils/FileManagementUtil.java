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
package org.wso2.maven.p2.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileManagementUtil {
    private static final int BUFFER = 2048;


    /**
     * @param destination path pointing the [carbon product]/repository/components folder
     * @param profile     name of the profile
     * @return the config.ini file for the Profile
     */
    public static File getProfileConfigIniFile(String destination, String profile) {
        return new File(destination + File.separator + profile + File.separator + "configuration" +
                File.separator + "config.ini");
    }

    /**
     * Updated the given config.ini file with a given key value pair.
     *
     * @param configIniFile File object representing the config.ini
     * @param propKey       property key
     * @param value         property value
     */
    public static void changeConfigIniProperty(File configIniFile, String propKey, String value) {
        Properties prop = new Properties();

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(configIniFile);
            outputStream = new FileOutputStream(configIniFile);
            prop.load(inputStream);
            prop.setProperty(propKey, value);
            prop.store(outputStream, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Zip a give folder to a give output zip file.
     *
     * @param srcFolder   source folder
     * @param destZipFile path to the output zip file
     */
    public static void zipFolder(String srcFolder, String destZipFile) {
        ZipOutputStream zip = null;
        try {
            FileOutputStream fileWriter = new FileOutputStream(destZipFile);
            zip = new ZipOutputStream(fileWriter);
            addFolderContentsToZip(srcFolder, zip);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(zip != null) {
                    zip.flush();
                    zip.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void addToZip(String path, String srcFile, ZipOutputStream zip) {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(srcFile);
                if (path.trim().equals("")) {
                    zip.putNextEntry(new ZipEntry(folder.getName()));
                } else {
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                }
                while ((len = inputStream.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                inputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void addFolderContentsToZip(String srcFolder, ZipOutputStream zip) {
        File folder = new File(srcFolder);
        String fileList[] = folder.list();
        if (fileList != null) {
            try {
                int i = 0;
                while (true) {
                    if (fileList.length == i) {
                        break;
                    }
                    if (new File(folder, fileList[i]).isDirectory()) {
                        zip.putNextEntry(new ZipEntry(fileList[i] + "/"));
                        zip.closeEntry();
                    }
                    addToZip("", srcFolder + "/" + fileList[i], zip);
                    i++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) {
        File folder = new File(srcFolder);
        String fileList[] = folder.list();
        if (fileList != null) {
            try {
                int i = 0;
                while (true) {
                    if (fileList.length == i) {
                        break;
                    }
                    String newPath = folder.getName();
                    if (!path.equalsIgnoreCase("")) {
                        newPath = path + "/" + newPath;
                    }
                    if (new File(folder, fileList[i]).isDirectory()) {
                        zip.putNextEntry(new ZipEntry(newPath + "/" + fileList[i] + "/"));
                        //					zip.closeEntry();
                    }
                    addToZip(newPath, srcFolder + "/" + fileList[i], zip);
                    i++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Delete a given directory.
     *
     * @param dir directory to be deleted
     * @throws IOException
     */
    public static void deleteDirectories(File dir) throws IOException {
        File[] children = dir.listFiles();

        if (children != null) {
            for (File child : children) {
                if (child != null) {
                    String[] childrenOfChild = child.list();
                    if(childrenOfChild != null) {
                        if(childrenOfChild.length > 0) {
                            deleteDirectories(child);
                        } else {
                            if (!child.delete()) {
                                throw new IOException("Failed to delete " + child.getAbsolutePath());
                            }
                        }
                    } else {
                        if (!child.delete()) {
                            throw new IOException("Failed to delete " + child.getAbsolutePath());
                        }
                    }
                }
            }
            if (!dir.delete()) {
                throw new IOException("Failed to delete " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Copies all files under srcDir to dstDir. If dstDir does not exist, it will be created.
     *
     * @param srcDir source directory
     * @param dstDir destination directory
     * @throws IOException
     */
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                if (!dstDir.mkdirs()) {
                    throw new IOException("Failed to delete " + dstDir.getAbsolutePath());
                }
            }

            String[] children = srcDir.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(srcDir, child),
                            new File(dstDir, child));
                }
            }
        } else {
            copy(srcDir, dstDir);
        }
    }

    /**
     * Copies src file to dst file. If the dst file does not exist, it is created.
     *
     * @param src source file
     * @param dst destination file
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        if (dst.getParentFile() != null && !dst.getParentFile().exists()) {
            if (!dst.getParentFile().mkdirs()) {
                throw new IOException("Failed to create " + dst.getAbsolutePath());
            }
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Unzip a given archive to a given destination
     *
     * @param archiveFile archive file to be unzipped
     * @param destination location to put the unzipped file
     * @throws IOException
     */
    public static void unzip(File archiveFile, File destination) throws IOException {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        BufferedOutputStream dest = null;

        try {
            fis = new FileInputStream(archiveFile);
            zis = new
                    ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                File file = new File(destination, entry.getName());
                if (entry.getName().endsWith("/")) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new IOException("Failed to create directories at " + file.getAbsolutePath());
                    }
                    continue;
                }
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create directories at " + file.getAbsolutePath());
                    }
                }
                FileOutputStream fos = new FileOutputStream(file);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
            if (fis != null) {
                fis.close();
            }
            if (dest != null) {
                dest.close();
            }
        }
    }

    //    public static void copyDirectory(File srcPath, File dstPath, List filesToBeCopied) throws IOException {
//        if (srcPath.isDirectory()) {
//            if (!dstPath.exists()) {
//                dstPath.mkdir();
//            }
//            String files[] = srcPath.list();
//            for (int i = 0; i < files.length; i++) {
//                copyDirectory(new File(srcPath, files[i]),
//                        new File(dstPath, files[i]), filesToBeCopied);
//            }
//        } else {
//            if (!filesToBeCopied.contains(srcPath.getAbsolutePath()))
//                return;
//            if (!srcPath.exists()) {
//                return;
//            } else {
//                FileManagementUtil.copy(srcPath, dstPath);
//            }
//        }
//    }
//
//    public static List getAllFilesPresentInFolder(File srcPath) {
//        List fileList = new ArrayList();
//        if (srcPath.isDirectory()) {
//            String files[] = srcPath.list();
//            for (int i = 0; i < files.length; i++) {
//                fileList.addAll(getAllFilesPresentInFolder(new File(srcPath, files[i])));
//            }
//        } else {
//            fileList.add(srcPath.getAbsolutePath());
//        }
//        return fileList;
//    }
//
//    public static void removeEmptyDirectories(File srcPath) {
//        if (srcPath.isDirectory()) {
//            String files[] = srcPath.list();
//            for (int i = 0; i < files.length; i++) {
//                removeEmptyDirectories(new File(srcPath, files[i]));
//            }
//            if (srcPath.list().length == 0) {
//                srcPath.delete();
//            }
//        }
//    }

//    public static void copyFile(String src, String dest) {
//        InputStream is = null;
//        FileOutputStream fos = null;
//
//        try {
//            is = new FileInputStream(src);
//            fos = new FileOutputStream(dest);
//            int c = 0;
//            byte[] array = new byte[1024];
//            while ((c = is.read(array)) >= 0) {
//                fos.write(array, 0, c);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                fos.close();
//                is.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static File createFileAndParentDirectories(String fileName) throws Exception {
//        File file = new File(fileName);
//        File parent = file.getParentFile();
//        if (!parent.exists()) {
//            parent.mkdirs();
//        }
//        file.createNewFile();
//        return file;
//    }
//
//    public static boolean deleteDir(File dir) {
//        if (dir.isDirectory()) {
//            String[] children = dir.list();
//            for (int i = 0; i < children.length; i++) {
//                boolean success = deleteDir(new File(dir, children[i]));
//                if (!success) {
//                    return false;
//                }
//            }
//        }
//        return dir.delete();
//    }

//    public static void createTargetFile(String sourceFileName, String targetFileName,
//                                        boolean overwrite) throws Exception {
//        File idealResultFile = new File(targetFileName);
//        if (overwrite || !idealResultFile.exists()) {
//            FileManagementUtil.createFileAndParentDirectories(targetFileName);
//            FileManagementUtil.copyFile(sourceFileName, targetFileName);
//        }
//    }

//    public static void deleteDirectories(String dir) {
//        File directory = new File(dir);
//        deleteDirectories(directory);
//    }
//
//    public static void createTargetFile(String sourceFileName, String targetFileName)
//            throws Exception {
//        createTargetFile(sourceFileName, targetFileName, false);
//    }

//    public static boolean createDirectory(String directory) {
//        // Create a directory; all ancestor directories must exist
//        boolean success = (new File(directory)).mkdir();
//        if (!success) {
//            // Directory creation failed
//        }
//        return success;
//    }
//
//    public static boolean createDirectorys(String directory) {
//        // Create a directory; all ancestor directories must exist
//        boolean success = (new File(directory)).mkdirs();
//        if (!success) {
//            // Directory creation failed
//        }
//        return success;
//    }

    //    public static String addAnotherNodeToPath(String currentPath, String newNode) {
//        return currentPath + File.separator + newNode;
//    }
//
//    public static String addNodesToPath(String currentPath, String[] newNode) {
//        String returnPath = currentPath;
//        for (int i = 0; i < newNode.length; i++) {
//            returnPath = returnPath + File.separator + newNode[i];
//        }
//        return returnPath;
//    }
//
//    public static String addNodesToPath(StringBuffer currentPath, String[] pathNodes) {
//        for (int i = 0; i < pathNodes.length; i++) {
//            currentPath.append(File.separator);
//            currentPath.append(pathNodes[i]);
//        }
//        return currentPath.toString();
//    }
//
//    public static String addNodesToURL(String currentPath, String[] newNode) {
//        String returnPath = currentPath;
//        for (int i = 0; i < newNode.length; i++) {
//            returnPath = returnPath + "/" + newNode[i];
//        }
//        return returnPath;
//    }

//    /**
//     * Get the list of file with a prefix of <code>fileNamePrefix</code> &amp; an extension of
//     * <code>extension</code>
//     *
//     * @param sourceDir      The directory in which to search the files
//     * @param fileNamePrefix The prefix to look for
//     * @param extension      The extension to look for
//     * @return The list of file with a prefix of <code>fileNamePrefix</code> &amp; an extension of
//     * <code>extension</code>
//     */
//    public static File[] getMatchingFiles(String sourceDir, String fileNamePrefix, String extension) {
//        List fileList = new ArrayList();
//        File libDir = new File(sourceDir);
//        String libDirPath = libDir.getAbsolutePath();
//        String[] items = libDir.list();
//        if (items != null) {
//            for (int i = 0; i < items.length; i++) {
//                String item = items[i];
//                if (fileNamePrefix != null && extension != null) {
//                    if (item.startsWith(fileNamePrefix) && item.endsWith(extension)) {
//                        fileList.add(new File(libDirPath + File.separator + item));
//                    }
//                } else if (fileNamePrefix == null && extension != null) {
//                    if (item.endsWith(extension)) {
//                        fileList.add(new File(libDirPath + File.separator + item));
//                    }
//                } else if (fileNamePrefix != null && extension == null) {
//                    if (item.startsWith(fileNamePrefix)) {
//                        fileList.add(new File(libDirPath + File.separator + item));
//                    }
//                } else {
//                    fileList.add(new File(libDirPath + File.separator + item));
//                }
//            }
//            return (File[]) fileList.toArray(new File[fileList.size()]);
//        }
//        return new File[0];
//    }

    //    /**
//     * Filter out files inside a <code>sourceDir</code> with matching <codefileNamePrefix></code>
//     * and <code>extension</code>
//     *
//     * @param sourceDir      The directory to filter the files
//     * @param fileNamePrefix The filtering filename prefix
//     * @param extension      The filtering file extension
//     */
//    public static void filterOutRestrictedFiles(String sourceDir, String fileNamePrefix, String extension) {
//        File[] resultedMatchingFiles = getMatchingFiles(sourceDir, fileNamePrefix, extension);
//        for (int i = 0; i < resultedMatchingFiles.length; i++) {
//            File matchingFilePath = new File(resultedMatchingFiles[i].getAbsolutePath());
//            matchingFilePath.delete();
//        }
//    }

}
