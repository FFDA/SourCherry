package com.ffda.sourcherry;

import android.content.res.XmlResourceParser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLReader {
    private String databaseUriString;

    public XMLReader(String databaseUriString) {
        this.databaseUriString = databaseUriString;
    }

    public ArrayList<String> getNodes() {
        ArrayList<String> nodes = new ArrayList<>();

        nodes.add("Vienas");
        nodes.add("penki");
        nodes.add("trys");

        File database = new File(this.databaseUriString);
        nodes.add(this.databaseUriString); // trinti
        try {
            InputStream is = new FileInputStream(database.getPath());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(is));
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("node");

            nodes.add(String.valueOf(nodeList.getLength()));

//            for (int i=0; i < nodeList.getLength(); i++) {
//                Node node = nodeList.item(i);
////                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
//                String nameValue = node.getNodeValue();
//                nodes.add(nameValue);
//                nodes.add(String.valueOf(i));
//            }

        } catch (Exception e) {
            nodes.add(e.getMessage());
        }

        return nodes;
    }


}
