/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

public class SQLSearcher implements DatabaseSearcher{
    private SQLiteDatabase sqlite;

    public SQLSearcher(SQLiteDatabase sqlite) {
        this.sqlite = sqlite;
    }

    @Override
    public ArrayList<String[]> search(Boolean noSearch, String query) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            ArrayList<String[]> searchResult = new ArrayList<>();

            Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);

            while (cursor.moveToNext()) {
                if (cursor.getInt(10) == 0) {
                    // If node and subnodes are not selected to be excluded from search
                    String uniqueID = String.valueOf(cursor.getInt(0));
                    String hasSubnode = this.hasSubnodes(uniqueID);
                    String isParent = "true"; // Main menu node will always be a parent
                    String isSubnode = "false"; // Main menu item will always be displayed as a parent
                    String[] result = findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                    if (hasSubnode.equals("true")) {
                        searchResult.addAll(searchNodesSkippingExcluded(uniqueID, query));
                    }
                } else if (cursor.getInt(10) == 1) {
                    // If only the node is selected to be excluded from search
                    String uniqueID = String.valueOf(cursor.getInt(0));
                    String hasSubnode = this.hasSubnodes(uniqueID);
                    if (hasSubnode.equals("true")) {
                        searchResult.addAll(searchNodesSkippingExcluded(uniqueID, query));
                    }
                } else if (cursor.getInt(10) == 2) {
                    // if only subnodes are selected to be excluded from search
                    String uniqueID = String.valueOf(cursor.getInt(0));
                    String hasSubnodes = this.hasSubnodes(uniqueID);
                    String isParent = "true"; // Main menu node will always be a parent
                    String isSubnode = "false"; // Main menu item will always be displayed as parent
                    String[] result = findInNode(cursor, query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
            }
            cursor.close();
            return searchResult;
        } else {
            Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);
            ArrayList<String[]> searchResult = new ArrayList<>();
            while (cursor.moveToNext()) {
                String uniqueID = String.valueOf(cursor.getInt(0));
                String hasSubnode = this.hasSubnodes(uniqueID);
                String isParent = "true"; // Main menu node will always be parent
                String isSubnode = "false"; // Main menu item will displayed as parent
                String[] result = this.findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
                if (hasSubnode.equals("true")) {
                    searchResult.addAll(this.searchAllNodes(uniqueID, query));
                }
            }
            cursor.close();
            return searchResult;
        }
    }

    public ArrayList<String[]> searchAllNodes(String parentUniqueID, String query) {
        // Searches thought all nodes without skipping marked to exclude
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()
        Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{parentUniqueID});
        ArrayList<String[]> searchResult = new ArrayList<>();
        while (cursor.moveToNext()) {
            String uniqueID = String.valueOf(cursor.getInt(0));
            String hasSubnode = this.hasSubnodes(uniqueID);
            String isParent = "false";
            String isSubnode = "true";
            if (hasSubnode.equals("true")) {
                isParent = "true";
                isSubnode = "false";
            }

            String[] result = this.findInNode(cursor, query, hasSubnode, isParent, isSubnode);
            if (result != null) {
                searchResult.add(result);
            }
            if (hasSubnode.equals("true")) {
                searchResult.addAll(this.searchAllNodes(uniqueID, query));
            }
        }

        cursor.close();
        return searchResult;
    }

    public ArrayList<String[]> searchNodesSkippingExcluded(String parentUniqueID, String query) {
        // If user marked that filter should omit nodes and/or node children from filter results
        ArrayList<String[]> searchResult = new ArrayList<>();

        Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{parentUniqueID});

        while (cursor.moveToNext()) {
            if (cursor.getInt(10) == 0) {
                // If node and subnodes are not selected to be excluded from search
                String uniqueID = String.valueOf(cursor.getInt(0));
                String hasSubnode = this.hasSubnodes(uniqueID);
                String isParent = "false";
                String isSubnode = "true";
                if (hasSubnode.equals("true")) {
                    isParent = "true";
                    isSubnode = "false";
                }
                String[] result = findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
                if (hasSubnode.equals("true")) {
                    searchResult.addAll(searchNodesSkippingExcluded(uniqueID, query));
                }
            } else if (cursor.getInt(10) == 1) {
                // If only the node is selected to be excluded from search
                String uniqueID = String.valueOf(cursor.getInt(0));
                String hasSubnode = hasSubnodes(uniqueID);
                if (hasSubnode.equals("true")) {
                    searchResult.addAll(searchNodesSkippingExcluded(uniqueID, query));
                }
            } else if (cursor.getInt(10) == 2) {
                // if only subnodes are selected to be excluded from search
                String uniqueID = String.valueOf(cursor.getInt(0));
                String hasSubnode = this.hasSubnodes(uniqueID);
                String isParent = "false";
                String isSubnode = "true";
                if (hasSubnode.equals("true")) {
                    isParent = "true";
                    isSubnode = "false";
                }
                String[] result = findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
            }
        }
        cursor.close();
        return searchResult;
    }

    public String[] findInNode(Cursor cursor, String query, String hasSubnodes, String isParent, String isSubnode) {
        // Searches thought node's content
        // This string builder will hold oll text content of the node
        StringBuilder nodeContent = new StringBuilder();
        // As in reader that all the text would be in order user sees it
        // filenames, table and codebox content hast to be inserted in correct location of the string
        // To help calculate that location totalCharOffset is used
        int totalCharOffset = 0;

        // ***Creating node content string
        String nodeSyntax = cursor.getString(3);

        if (nodeSyntax.equals("custom-colors")) {
            // This is formatting for Rich Text and Plain Text nodes
            NodeList nodeContentNodeList = getNodeFromString(cursor.getString(2), "node"); // Gets all the subnodes/childnodes of selected node
            for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                // Loops through nodes/tags of selected node
                nodeContent.append(nodeContentNodeList.item(x).getTextContent());
            }
            int hasCodebox = cursor.getInt(7);
            int hasTable = cursor.getInt(8);
            int hasImage = cursor.getInt(9);

            // If it is marked that node has codebox, table or image
            if (hasCodebox == 1 || hasTable == 1 || hasImage == 1) {
                //// Building string for SQLQuery
                // Because every element is in it own table
                // Only the ones that actually are in the node will be searched
                // hopefully
                // During the selection eleventh (index: 10) column will be added.
                // That will have 7 (codebox), 8 (table) or 9 (image) written to it. It should make separating which line comes from this table easier
                StringBuilder codeboxTableImageQueryString = new StringBuilder();

                // Depending on how many tables will be searched
                // instances of how many time uniqueID will have to be inserted will differ
                int queryCounter = 0; // This is the counter for that
                if (hasCodebox == 1) {
                    // Means that node has has codeboxes in it
                    codeboxTableImageQueryString.append("SELECT offset, txt, 1 FROM codebox WHERE node_id=? ");
                    queryCounter++;
                }
                if (hasTable == 1) {
                    // Means that node has tables in it
                    if (hasCodebox == 1) {
                        codeboxTableImageQueryString.append("UNION ");
                    }
                    codeboxTableImageQueryString.append("SELECT offset, txt, 2 FROM grid WHERE node_id=? ");
                    queryCounter++;
                }
                if (hasImage == 1) {
                    // Means that node has images (images, anchors or files) in it
                    if (hasCodebox == 1 || hasTable == 1) {
                        codeboxTableImageQueryString.append("UNION ");
                    }
                    codeboxTableImageQueryString.append("SELECT offset, filename, 3 FROM image WHERE node_id=? ");
                    queryCounter++;
                }
                codeboxTableImageQueryString.append("ORDER BY offset ASC");

                /// Creating the array that will be used to insert uniqueIDs
                String[] queryArguments = new String[queryCounter];
                Arrays.fill(queryArguments, cursor.getString(0));
                ///
                ////

                Cursor codeboxTableImageCursor = this.sqlite.rawQuery(codeboxTableImageQueryString.toString(), queryArguments);

                while (codeboxTableImageCursor.moveToNext()) {
                    int charOffset = codeboxTableImageCursor.getInt(0);
                    if (codeboxTableImageCursor.getInt(2) == 9) {
                        if (!codeboxTableImageCursor.getString(1).isEmpty()) {
                            // Text in column 5 means that this line is for file OR LaTeX formula box
                            if (!codeboxTableImageCursor.getString(1).equals("__ct_special.tex")) {
                                // If it is not LaTex file
                                String attachedFileFilename = " " + codeboxTableImageCursor.getString(1) + " ";
                                nodeContent.insert(charOffset + totalCharOffset, attachedFileFilename);
                                totalCharOffset += attachedFileFilename.length() - 1;
                                continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                            }
                        }
                    } else if (codeboxTableImageCursor.getInt(2) == 7) {
                        // codebox row
                        String codeboxText = codeboxTableImageCursor.getString(1);
                        nodeContent.insert(charOffset + totalCharOffset, codeboxText);
                        totalCharOffset += codeboxText.length() - 1;
                    } else if (codeboxTableImageCursor.getInt(2) == 8) {
                        StringBuilder tableContent = new StringBuilder();
                        // table row
                        NodeList tableRows = getNodeFromString(codeboxTableImageCursor.getString(1), "table");

                        // Adding all rows to arraylist
                        ArrayList<String> tableRowArray = new ArrayList<>();
                        for (int row = 0; row < tableRows.getLength(); row++) {
                            if (tableRows.item(row).getNodeName().equals("row")) {
                                // For table content from SQL database spaces around each cell needs to be added
                                // because there are any
                                // All cells from one row has to be connected to one string that represents a row
                                // Otherwise it might be not possible to put table header to the top of the table
                                StringBuilder rowStringBuilder = new StringBuilder();
                                NodeList cells = tableRows.item(row).getChildNodes();
                                for (int cell = 0; cell < cells.getLength(); cell++) {
                                    rowStringBuilder.append(" " + cells.item(cell).getTextContent() + " ");
                                }
                                tableRowArray.add(rowStringBuilder.toString());
                            }
                        }

                        // Adding the last row of the table to string builder as first because that's where header of the table is located
                        tableContent.append(tableRowArray.get(tableRowArray.size() - 1));
                        // Rest of the rows can be added in order
                        for (int x = 0; x < tableRowArray.size() - 1; x++) {
                            tableContent.append(tableRowArray.get(x));
                        }

                        // Adding table's content to nodes content string builder
                        nodeContent.insert(charOffset + totalCharOffset, tableContent);
                        // Changing total offset value with a value of the table content, because CherryTree uses different GUI toolkit
                        // And without doing this the first element with offset would mess node content order (or maybe that's by design)
                        totalCharOffset += tableContent.length() - 1;
                    }
                }
                codeboxTableImageCursor.close();
            }
        } else if (nodeSyntax.equals("plain-text")) {
            // Plain text node does not have any formatting and has not node embedded in to it
            nodeContent.append(cursor.getString(2));
        } else {
            // Node is Code Node. It's just a big CodeBox with no dimensions
            nodeContent.append(cursor.getString(2));
        }

        // ***Search
        int queryLength = query.length();
        int count = 0;
        int index = 0;
        StringBuilder samples = new StringBuilder(); // This will hold 3 samples to show to user

        // Removing all spaces and new line character from the node content string
        String preparedNodeContent = nodeContent.toString().toLowerCase().replaceAll("\n", " ").replaceAll(" +", " ");

        while (index != -1) {
            index = preparedNodeContent.indexOf(query, index);
            if (index != -1) {
                // if match to search query was found in the node's content
                if (count < 3 ) {
                    // Results display only first three found instances of search query
                    int startIndex = 0; // Start of sample substring that will be created
                    int endIndex = preparedNodeContent.length(); // End of sample substring that will be created
                    String sampleStart = "";
                    String sampleEnd = "";
                    if (index > 20) {
                        // if index is further than 20 symbols from the start of the node content
                        // ... are added to the start of the sample
                        // and only 20 preceding symbols before query match are showed
                        startIndex = index - 20;
                        sampleStart = "...";
                    }
                    if ((index + queryLength + 20) < endIndex) {
                        // if index is more than 20 symbols from the end of the node content
                        // ... are added to the end of the sample
                        // and only 20 proceeding symbols before query match are showed
                        endIndex = index + queryLength + 20;
                        sampleEnd = "...";
                    }

                    // Building a sample for search result from using previously formatted parts
                    StringBuilder sample = new StringBuilder();
                    sample.append(sampleStart);
                    sample.append(preparedNodeContent.substring(startIndex, endIndex).trim());
                    sample.append(sampleEnd);
                    sample.append("<br/>");
                    samples.append(sample);
                }

                count++;
                index += queryLength; // moving search start to the end of the last position that search query was found
            }
        }

        if (count > 0) {
            // if node count of matches is more than 0 that a match of q query was found
            return new String[]{cursor.getString(1), cursor.getString(0), query, String.valueOf(count), samples.toString(), hasSubnodes, isParent, isSubnode};
        } else {
            return null;
        }
    }

    public String hasSubnodes(String uniqueNodeID) {
        // Checks if node with provided unique_id has subnodes
        Cursor cursor = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{uniqueNodeID},null,null,null);

        if (cursor.getCount() > 0) {
            cursor.close();
            return "true";
        } else {
            cursor.close();
            return "false";
        }
    }

    public NodeList getNodeFromString(String nodeString, String type) {
        // SQL Database has a XML document inserted in to it
        // XML document is for node content formatting
        // So SQL document is just a XML document with extra steps
        // For that reason node with all the content has to be separated from other tags for processing
        // Variable type is just node type to select (node or table values)

        try {
            Document doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(nodeString.getBytes(StandardCharsets.UTF_8))
                    );
            // It seems that there is always just one tag (<node> or <table>), so returning just the first one in the NodeList
            return doc.getElementsByTagName(type).item(0).getChildNodes();
        } catch (Exception e) {
            // To not have to create SQLSearcher with context and handler no error message will be diplayed to user.
            // Up until now I did not have any problems in a reader
        }
        return null;
    }
}
