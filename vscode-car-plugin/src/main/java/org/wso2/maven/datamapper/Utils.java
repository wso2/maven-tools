/*
 * Copyright (c) 2024, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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
package org.wso2.maven.datamapper;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    /**
     * Converts a byte array into a hexadecimal string.
     * Pads the result with leading zeros to ensure a length of 32 characters.
     *
     * @param messageDigest The byte array to convert.
     * @return The hexadecimal string representation.
     */
    public static String convertToHex(byte[] messageDigest) {
        BigInteger bigint = new BigInteger(1, messageDigest);
        String hexText = bigint.toString(16);
        while (hexText.length() < 32) {
            hexText = "0".concat(hexText);
        }
        return hexText;
    }

    /**
     * Calculates the MD5 checksum of the specified file.
     *
     * @param filePath The path to the file for which the checksum is to be calculated.
     * @return The MD5 checksum as a hexadecimal string.
     * @throws DataMapperException if an I/O error or algorithm error occurs during checksum calculation.
     */
    public static String getFileChecksum(Path filePath) throws DataMapperException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] digest = md.digest(fileBytes);
            return convertToHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DataMapperException("Failed to calculate checksum for file: " + filePath, e);
        }
    }

    /**
     * Generates an MD5 hash for the given input string.
     *
     * @param input The input string to hash.
     * @return The MD5 hash as a hexadecimal string, or null if the algorithm is not available.
     */
    public static String getHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            return convertToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
