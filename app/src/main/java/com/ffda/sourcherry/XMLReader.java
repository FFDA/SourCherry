package com.ffda.sourcherry;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    public ArrayList<String> getAllNodes() {
        // Returns all the node from the document
        // Used for the search/filter in the drawer menu
        ArrayList<String> nodes = new ArrayList<>();

//        this.doc.getDocumentElement().normalize(); // Tikriausiai tikrinti
        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
            nodes.add(nameValue);
        }

        return nodes;
    }

    public ArrayList<String> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts

        ArrayList<String> nodes = new ArrayList<>();

//        this.doc.getDocumentElement().normalize(); // Tikriausiai tikrinti
        NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
        NodeList mainNodeList = nodeList.item(0).getChildNodes();

        for (int i=0; i < mainNodeList.getLength(); i++) {
            Node node = mainNodeList.item(i);
            if (node.getNodeType() != Node.TEXT_NODE) {
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                nodes.add(nameValue);
            }
        }
        return nodes;
    }

    public ArrayList<String> getSubNodes(String nodeName) {
        // Returns Subnodes of the node that's name is provided
        ArrayList<String> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("name").getNodeValue().equals(nodeName)) {
                NodeList childNodeList = node.getChildNodes();
                for (int x = 0; x < childNodeList.getLength(); x++) {
                    Node currentNode = childNodeList.item(x);
                    if (currentNode.getNodeName().equals("node")) {
                        nodes.add(currentNode.getAttributes().getNamedItem("name").getNodeValue());
                    }
                }
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
