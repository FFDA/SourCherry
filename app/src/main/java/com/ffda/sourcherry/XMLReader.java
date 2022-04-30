package com.ffda.sourcherry;


import org.w3c.dom.Document;
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

    public ArrayList<String[]> getSubnodes(String nodeName) {
        // Returns Subnodes of the node which name is provided
        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("name").getNodeValue().equals(nodeName)) {
                // When it finds a match - creates a NodeList and uses other function to get the MenuItems
                NodeList childNodeList = node.getChildNodes();
                nodes = returnSubnodeArrayList(childNodeList, "true");

                // Creating parent node and adding it to the ArrayList
                String parentNodeName = node.getAttributes().getNamedItem("name").getNodeValue();
                String parentNodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                String parentNodeHasSubnode = String.valueOf(hasSubnodes(node));
                String parentNodeIsParent = "true";
                String parentNodeIsSubnode = "false";
                String[] parentNode = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};
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
        // Creates and returns the node that will be added to the node array
        String parentNodeName = parentNode.getAttributes().getNamedItem("name").getNodeValue();
        String parentNodeUniqueID = parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        String parentNodeHasSubnode = String.valueOf(hasSubnodes(parentNode));
        String parentNodeIsParent = "true";
        String parentNodeIsSubnode = "false";
        String[] node = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};
        return node;
    }

    public ArrayList<String[]> getParentWithSubnodes(String nodeName) {
        // Checks if it is possible to go up in document's node tree from given node's name
        // Returns array with appropriate nodes
        ArrayList<String[]> nodes = null;

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("name").getNodeValue().equals(nodeName)) {
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

}
