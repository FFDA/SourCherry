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
        ArrayList<String[]> nodes = returnSubnodeArrayList(nodeList);
        return nodes;
    }

    public ArrayList<String[]> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts

        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
        NodeList mainNodeList = nodeList.item(0).getChildNodes();

        for (int i=0; i < mainNodeList.getLength(); i++) {
            Node node = mainNodeList.item(i);
            if (node.getNodeType() != Node.TEXT_NODE) {
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                String[] currentNodeArray = {nameValue};
                nodes.add(currentNodeArray);
            }
        }
        return nodes;
    }

    public ArrayList<String[]> getSubNodes(String nodeName) {
        // Returns Subnodes of the node that's name is provided
        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("name").getNodeValue().equals(nodeName)) {
                NodeList childNodeList = node.getChildNodes();
                nodes = returnSubnodeArrayList(childNodeList);
                return nodes;
            }

        }

        return nodes;
    }
    
    public ArrayList<String[]> returnSubnodeArrayList(NodeList nodeList) {
        
        ArrayList<String[]> nodes = new ArrayList<>();

        for (int i=0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);
            if (node.getNodeName().equals("node")) {
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                String uniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                String hasSubnode = String.valueOf(hasSubnodes(node));
                String[] currentNodeArray = {nameValue, uniqueID, hasSubnode};
                nodes.add(currentNodeArray);
            }
        }
        
        return nodes;
    }

    public boolean hasSubnodes(Node node) {
        NodeList subNodes = node.getChildNodes();

        for (int i = 0; i < subNodes.getLength(); i++) {
            if (subNodes.item(i).getNodeName().equals("node")) {
                return true;
            }
        }

        return false;
    }
}
