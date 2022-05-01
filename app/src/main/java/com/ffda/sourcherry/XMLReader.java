package com.ffda.sourcherry;


import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLReader {
    private Document doc;

    public XMLReader(InputStream is) {
        // Creates a document that can be used to read tags with provided InputStream
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.doc = db.parse(new InputSource(is));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<String[]> getAllNodes() {
        // Returns all the node from the document
        // Used for the search/filter in the drawer menu
        NodeList nodeList = this.doc.getElementsByTagName("node");
        ArrayList<String[]> nodes = returnSubnodeArrayList(nodeList, "false");
        return nodes;
    }

    public ArrayList<String[]> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts

        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
        NodeList mainNodeList = nodeList.item(0).getChildNodes();

        nodes = returnSubnodeArrayList(mainNodeList, "false");

        return nodes;
    }

    public ArrayList<String[]> getSubnodes(String uniqueID) {
        // Returns Subnodes of the node which uniqueID is provided
        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueID)) {
                // When it finds a match - creates a NodeList and uses other function to get the MenuItems
                NodeList childNodeList = node.getChildNodes();
                nodes = returnSubnodeArrayList(childNodeList, "true");

                String[] parentNode = createParentNode(node);
                nodes.add(0, parentNode);
                //

                return nodes;
            }
        }
        return nodes;
    }

    public ArrayList<String[]> returnSubnodeArrayList(NodeList nodeList, String isSubnode) {
        // This function scans provided NodeList and
        // returns ArrayList with nested String Arrays that
        // holds individual menu items.

        ArrayList<String[]> nodes = new ArrayList<>();

        for (int i=0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);
            if (node.getNodeName().equals("node")) {
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                String uniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                String hasSubnode = String.valueOf(hasSubnodes(node));
                String isParent = "false"; // There is only one parent Node and its added manually in getSubNodes()
                String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, isSubnode};
                nodes.add(currentNodeArray);
            }
        }
        return nodes;
    }

    public boolean hasSubnodes(Node node) {
        // Checks if provided node has nested "node" tag
        NodeList subNodes = node.getChildNodes();

        for (int i = 0; i < subNodes.getLength(); i++) {
            if (subNodes.item(i).getNodeName().equals("node")) {
                return true;
            }
        }
        return false;
    }

    private String[] createParentNode(Node parentNode) {
        // Creates and returns the node that will be added to the node array as parent node
        String parentNodeName = parentNode.getAttributes().getNamedItem("name").getNodeValue();
        String parentNodeUniqueID = parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        String parentNodeHasSubnode = String.valueOf(hasSubnodes(parentNode));
        String parentNodeIsParent = "true";
        String parentNodeIsSubnode = "false";
        String[] node = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};
        return node;
    }

    public ArrayList<String[]> getParentWithSubnodes(String uniqueID) {
        // Checks if it is possible to go up in document's node tree from given node's uniqueID
        // Returns array with appropriate nodes
        ArrayList<String[]> nodes = null;

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueID)) {
                Node parentNode = node.getParentNode();
                if (parentNode == null) {
                    return nodes;
                } else if (parentNode.getNodeName().equals("cherrytree")) {
                    nodes = this.getMainNodes();
                } else {
                    NodeList parentSubnodes = parentNode.getChildNodes();
                    nodes = returnSubnodeArrayList(parentSubnodes, "true");
                    nodes.add(0, createParentNode(parentNode));
                }

            }
        }
        return nodes;
    }

    public ArrayList<SpannableStringBuilder> getNodeContent(String uniqueID) {
        // Original XML document has newline characters marked
        // Returns ArrayList of SpannableStringBuilder elements

        ArrayList<SpannableStringBuilder> nodeContent = new ArrayList<>();
        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueID)) { // Finds element that user chose
                SpannableStringBuilder newNode = new SpannableStringBuilder();
                NodeList nodeContentNodeList = node.getChildNodes();
                for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                    // Loops through nodes of selected node
                    Node currentNode = nodeContentNodeList.item(x);
                    if (currentNode.getNodeName().equals("rich_text")) {
                        // Node is a text_rich node
                        if (currentNode.hasAttributes()) {
                            newNode.append(makeFormattedText(currentNode));
                        } else {
                            newNode.append(currentNode.getTextContent());
                        }
                    }
                }

                nodeContent.add(newNode);
            }
        }

        return nodeContent;
    }

    public SpannableStringBuilder makeFormattedText(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting made out of nodes attribute
        SpannableStringBuilder formattedNodeText = new SpannableStringBuilder();
        formattedNodeText.append(node.getTextContent());

        NamedNodeMap nodeAttributes = node.getAttributes(); // Gets all the passed node attributes as NodeList
        for (int i = 0; i < nodeAttributes.getLength(); i++) {
            String attribute = nodeAttributes.item(i).getNodeName();

            if (attribute.equals("strikethrough")) {
                    StrikethroughSpan sts = new StrikethroughSpan();
                    formattedNodeText.setSpan(sts,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("foreground")) {
                String foregroundColorOriginal = nodeAttributes.item(i).getTextContent();
                // Creating a normal HEX color code, because XML document has strange one with 12 symbols
                StringBuilder colorCode = new StringBuilder();
                colorCode.append("#");
                colorCode.append(foregroundColorOriginal.substring(1,3));
                colorCode.append(foregroundColorOriginal.substring(5,7));
                colorCode.append(foregroundColorOriginal.substring(9,11));
                //
                ForegroundColorSpan fcs = new ForegroundColorSpan(Color.parseColor(colorCode.toString()));
                formattedNodeText.setSpan(fcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

        return formattedNodeText;
    }

}
