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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLSearcher implements DatabaseSearcher{
    private Document doc;

    public XMLSearcher(InputStream is) throws Exception {
        // Creates a document that can be used to read tags with provided InputStream
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        this.doc = db.parse(is);
    }

    @Override
    public ArrayList<String[]> search(Boolean noSearch, String query) {
        NodeList nodeList = this.doc.getFirstChild().getChildNodes();
        ArrayList<String[]> searchResult = new ArrayList<>();
        if (noSearch) {
            // If user marked that search should skip search "excluded" nodes

            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getNodeName().equals("node")) {
                    // If node is a "node" and not some other tag
                    String hasSubnodes = String.valueOf(this.hasSubnodes(nodeList.item(i)));

                    if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                        // if user haven't marked to skip current node - searches through its content
                        // Because this is a start of the search all nodes never are "subnodes"
                        String isParent = "true";
                        String isSubnode = "false";
                        if (hasSubnodes.equals("true")) {
                            isParent = "true";
                            isSubnode = "false";
                        }

                        String[] result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                        if (result != null) {
                            searchResult.add(result);
                        }
                    }
                    if (hasSubnodes.equals("true") && nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                        // if user haven't selected not to search subnodes of current node
                        searchResult.addAll(this.searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                    }
                }
            }
            return searchResult;
        } else {
            for (int i = 0; i < nodeList.getLength(); i++) {
                String hasSubnodes = String.valueOf(hasSubnodes(nodeList.item(i)));
                if (nodeList.item(i).getNodeName().equals("node")) {

                    String isParent  = "true";
                    String isSubnode  = "false";
                    if (hasSubnodes.equals("true")) {
                        isParent = "true";
                        isSubnode = "false";
                    }

                    String[] result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes.equals("true")) {
                    // If node has subnodes
                    searchResult.addAll(this.searchAllNodes(query, nodeList.item(i).getChildNodes()));
                }
            }
        }
        return searchResult;
    }

    private ArrayList<String[]> searchAllNodes(String query, NodeList nodeList) {
        // Searches thought all nodes without skipping marked to exclude
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()

        ArrayList<String[]> searchResult = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            String hasSubnodes = String.valueOf(hasSubnodes(nodeList.item(i)));

            if (nodeList.item(i).getNodeName().equals("node")) {
                // If node is a "node" and not some other tag
                String isParent;
                String isSubnode;
                if (hasSubnodes.equals("true")) {
                    isParent = "true";
                    isSubnode = "false";
                } else {
                    isParent = "false";
                    isSubnode = "true";
                }
                String[] result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
            }
            if (hasSubnodes.equals("true")) {
                // If node has subnodes
                searchResult.addAll(this.searchAllNodes(query, nodeList.item(i).getChildNodes()));
            }
        }
        return searchResult;
    }

    private ArrayList<String[]> searchNodesSkippingExcluded(String query, NodeList nodeList) {
        // Searches thought nodes skipping marked to exclude
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()

        ArrayList<String[]> searchResult = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals("node")) {
                // If node is a "node" and not some other tag
                String hasSubnodes = String.valueOf(this.hasSubnodes(nodeList.item(i)));

                if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                    // If user haven't marked to skip current node - searches through its content
                    String isParent;
                    String isSubnode;
                    if (hasSubnodes.equals("true")) {
                        isParent = "true";
                        isSubnode = "false";
                    } else {
                        isParent = "false";
                        isSubnode = "true";
                    }
                    String[] result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes.equals("true") && nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                    // If node has subnodes and user haven't selected not to search subnodes of current node
                    searchResult.addAll(this.searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                }
            }
        }
        return searchResult;
    }

    private String[] findInNode(Node node, String query, String hasSubnodes, String isParent, String isSubnode) {
        // Searches thought node's content

        // This string builder will hold oll text content of the node
        StringBuilder nodeContent = new StringBuilder();

        // As in reader that all the text would be in order user sees it
        // filenames, table and codebox content hast to be inserted in correct location of the string
        // To help calculate that location totalCharOffset is used
        int totalCharOffset = 0;

        // Gets all child nodes
        NodeList nodeContentNodeList = node.getChildNodes();

        for (int i = 0; i < nodeContentNodeList.getLength(); i++) {
            // Going through all the tags of the node
            // Skipping other "node" tags (subnodes).
            // To decide if these nodes have to be search are for a job for
            // searchNodesSkippingExcluded() or searchInNodeList()
            switch (nodeContentNodeList.item(i).getNodeName()) {
                case "rich_text":
                    // All the text of the node
                    nodeContent.append(nodeContentNodeList.item(i).getTextContent());
                    break;
                case "table":
                    // Table of the node
                    // offset where table's content has to be inserted
                    int tableContentCharOffset = Integer.parseInt(nodeContentNodeList.item(i).getAttributes().getNamedItem("char_offset").getNodeValue());
                    StringBuilder tableContent = new StringBuilder();
                    // Getting all the rows of the table
                    // It would be possible to add all the table content directly from the nodeContentNodeList.item(i)
                    // However, "header" of the table is places in the last row, so it would be showed at the end of tables content
                    NodeList tableRows = nodeContentNodeList.item(i).getChildNodes();

                    // Adding all rows to arraylist
                    ArrayList<String> tableRowArray = new ArrayList<>();
                    for (int row = 0; row < tableRows.getLength(); row++) {
                        if (tableRows.item(row).getNodeName().equals("row")) {
                            tableRowArray.add(tableRows.item(row).getTextContent());
                        }
                    }

                    // Adding the last row of the table to string builder as first because that's where header of the table is located
                    tableContent.append(tableRowArray.get(tableRowArray.size() - 1));
                    // Rest of the rows can be added in order
                    for (int x = 0; x < tableRowArray.size() - 1; x++) {
                        tableContent.append(tableRowArray.get(x));
                    }

                    // Adding table's content to nodes content string builder
                    nodeContent.insert(tableContentCharOffset + totalCharOffset, tableContent);
                    // Changing total offset value with a value of the table content, because CherryTree uses different GUI toolkit
                    // And without doing this the first element with offset would mess node content order (or maybe that's by design)
                    totalCharOffset += tableContent.length() - 1;
                    break;
                case "codebox":
                    int codeboxContentCharOffset = Integer.parseInt(nodeContentNodeList.item(i).getAttributes().getNamedItem("char_offset").getNodeValue());

                    StringBuilder codeboxContent = new StringBuilder();
                    codeboxContent.append(nodeContentNodeList.item(i).getTextContent());
                    nodeContent.insert(codeboxContentCharOffset + totalCharOffset, codeboxContent);
                    totalCharOffset += codeboxContent.length() - 1;
                    break;
                case "encoded_png":
                    // Getting just image's filename and inserting it in the nodeContent string that it would be possible to search for them
                    Node filename = nodeContentNodeList.item(i).getAttributes().getNamedItem("filename");
                    if (filename != null) {
                        if (!filename.getNodeValue().equals("__ct_special.tex")) {
                            // Ignoring LaTeX formulas
                            int encodedPngContentCharOffset = Integer.parseInt(nodeContentNodeList.item(i).getAttributes().getNamedItem("char_offset").getNodeValue());
                            StringBuilder encodedPngContent = new StringBuilder();
                            encodedPngContent.append(filename.getNodeValue());
                            encodedPngContent.append(" ");
                            nodeContent.insert(encodedPngContentCharOffset + totalCharOffset, encodedPngContent);
                            totalCharOffset += encodedPngContent.length() - 1;
                        }
                    }
                    break;
            }
        }

        // Search
        int queryLength = query.length();
        int count = 0;
        int index = 0;
        String nodeName = null; // To display in results
        String nodeUniqueID = null; // That it could be returned to MainView to load selected node
        StringBuilder samples = new StringBuilder(); // This will hold 3 samples to show to user

        // Removing all spaces and new line character from the node content string
        String preparedNodeContent = nodeContent.toString().toLowerCase().replaceAll("\n", " ").replaceAll(" +", " ");

        while (index != -1) {
            index = preparedNodeContent.indexOf(query, index);
            if (index != -1) {
                // if match to search query was found in the node's content
                if (count < 1) {
                    // If it's first match
                    // Settings node name and unique_id values that they could be returned with result
                    nodeName = node.getAttributes().getNamedItem("name").getNodeValue();
                    nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                }

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

        if (nodeName != null) {
            // if node name isn't null that means match for a query was found
            return new String[]{nodeName, nodeUniqueID, query, String.valueOf(count), samples.toString(), hasSubnodes, isParent, isSubnode};
        } else {
            return null;
        }
    }

    private boolean hasSubnodes(Node node) {
        // Checks if provided node has nested "node" tag
        NodeList subNodes = node.getChildNodes();

        for (int i = 0; i < subNodes.getLength(); i++) {
            if (subNodes.item(i).getNodeName().equals("node")) {
                return true;
            }
        }
        return false;
    }
}
