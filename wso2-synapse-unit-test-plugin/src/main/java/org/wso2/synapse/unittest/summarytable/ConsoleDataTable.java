/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.synapse.unittest.summarytable;

/**
 * Class is responsible for print the test summary table in the console.
 */
public final class ConsoleDataTable {

    private static final String EMPTY = "(empty)";
    private static final String ANSI_COLORS = "\u001B\\[[;\\d]*m";
    private static final String TABLE_EDGES = "+-+-+";

    private final String[] headers;
    private final String[][] data;
    private final int columns;
    private final int[] columnWidths;
    private final int emptyWidth;

    /**
     * Constructor method of the class.
     *
     * @param headers list of headers of the table
     * @param data list of row data of the table
     */
    private ConsoleDataTable(String[] headers, String[][] data) {
        this.headers = headers;
        this.data = data;

        columns = headers.length;
        columnWidths = new int[columns];
        for (int row = -1; row < data.length; row++) {
            // Hack to parse headers too.
            String[] rowData = (row == -1) ? headers : data[row];
            if (rowData.length != columns) {
                throw new IllegalArgumentException(
                        String.format("Row %s's %s columns != %s columns", row + 1, rowData.length, columns));
            }
            for (int column = 0; column < columns; column++) {
                for (String rowDataLine : rowData[column].split("\\n")) {
                    String rowDataWithoutColor = rowDataLine.replaceAll(ANSI_COLORS, "");
                    columnWidths[column] = Math.max(columnWidths[column], rowDataWithoutColor.length());
                }
            }
        }

        // Account for column dividers and their spacing.
        int emptyWidth = 3 * (columns - 1);
        for (int columnWidth : columnWidths) {
            emptyWidth += columnWidth;
        }
        this.emptyWidth = emptyWidth;

        // Make sure we're wide enough for the empty text.
        if (emptyWidth < EMPTY.length()) {
            columnWidths[columns - 1] += EMPTY.length() - emptyWidth;
        }
    }

    /**
     * Create a new table with the specified headers and row data.
     *
     * @param headers list of headers of the table
     * @param data list of row data of the table
     * @return full table as a string
     */
    public static String of(String[] headers, String[][] data) {
        if (headers == null) throw new NullPointerException("headers == null");
        if (headers.length == 0) throw new IllegalArgumentException("Headers must not be empty.");
        if (data == null) throw new NullPointerException("data == null");
        return new ConsoleDataTable(headers, data).toString();
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        printDivider(builder, TABLE_EDGES);
        printData(builder, headers);
        if (data.length == 0) {
            printDivider(builder, TABLE_EDGES);
            builder.append('|').append(pad(emptyWidth, EMPTY)).append("|\n");
            printDivider(builder, "+---+");
        } else {
            for (int row = 0; row < data.length; row++) {
                printDivider(builder, row == 0 ? "+=+=+" : TABLE_EDGES);
                printData(builder, data[row]);
            }
            printDivider(builder, TABLE_EDGES);
        }
        return builder.toString();
    }

    /**
     * Method of print divider of the table.
     *
     * @param out string builder
     * @param format format type
     */
    private void printDivider(StringBuilder out, String format) {
        for (int column = 0; column < columns; column++) {
            out.append(column == 0 ? format.charAt(0) : format.charAt(2));
            out.append(pad(columnWidths[column], "").replace(' ', format.charAt(1)));
        }
        out.append(format.charAt(4)).append('\n');
    }

    /**
     * Method of print data of the table.
     *
     * @param out string builder
     * @param data list of row data of the table
     */
    private void printData(StringBuilder out, String[] data) {
        for (int line = 0, lines = 1; line < lines; line++) {
            for (int column = 0; column < columns; column++) {
                out.append(column == 0 ? '|' : '|');
                String[] cellLines = data[column].split("\\n");
                lines = Math.max(lines, cellLines.length);
                String cellLine = line < cellLines.length ? cellLines[line] : "";
                out.append(pad(columnWidths[column], cellLine));
            }
            out.append("|\n");
        }
    }

    /**
     * Method of padding for the table.
     *
     * @param width padding width
     * @param data data of the cell
     */
    private static String pad(int width, String data) {
        return String.format(" %1$-" + width + "s ", data);
    }
}
