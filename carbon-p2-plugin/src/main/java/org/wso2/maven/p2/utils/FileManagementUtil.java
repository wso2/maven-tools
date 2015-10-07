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

        try (InputStream inputStream = new FileInputStream(configIniFile)){
            prop.load(inputStream);
            prop.setProperty(propKey, value);
            try(OutputStream outputStream = new FileOutputStream(configIniFile)) {
                prop.store(outputStream, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Zip a give folder to a give output zip file.
     *
     * @param srcFolder   source folder
     * @param destZipFile path to the output zip file
     */
    public static void zipFolder(String srcFolder, String destZipFile) {
        try(FileOutputStream fileWriter = new FileOutputStream(destZipFile);
            ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFolderContentsToZip(srcFolder, zip);
        } catch (IOException e) {
            e.printStackTrace();
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
            try (FileInputStream inputStream = new FileInputStream(srcFile)){

                if (path.trim().equals("")) {
                    zip.putNextEntry(new ZipEntry(folder.getName()));
                } else {
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                }
                while ((len = inputStream.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
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
            } catch (IOException e) {
                e.printStackTrace();
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
            } catch (IOException e) {
                e.printStackTrace();
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

        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Unzip a given archive to a given destination
     *
     * @param archiveFile archive file to be unzipped
     * @param destination location to put the unzipped file
     * @throws IOException
     */
    public static void unzip(File archiveFile, File destination) throws IOException {

        try(FileInputStream fis = new FileInputStream(archiveFile);
            ZipInputStream zis = new
                    ZipInputStream(new BufferedInputStream(fis))) {
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
                try(FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER)) {
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                }
            }
        }
    }
}
