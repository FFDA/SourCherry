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
    private InputStream is;

    public XMLReader(InputStream is) {
        this.is = is;
    }

    public ArrayList<String> getNodes() {
        ArrayList<String> nodes = new ArrayList<>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(this.is));
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("node");

            for (int i=0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                nodes.add(nameValue);
            }

        } catch (Exception e) {
            nodes.add(e.getMessage());
        }

        return nodes;
    }


}
