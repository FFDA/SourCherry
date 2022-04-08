package com.ffda.sourcherry;

import android.content.res.XmlResourceParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLReader {
    private String databaseUriString;

    public XMLReader(String databaseUriString) {
        this.databaseUriString = databaseUriString;
    }

    public String getSomething() {
        return "Domas";
    }

    public ArrayList<String> getNodes() {
        ArrayList<String> nodes = new ArrayList<>();

        File database = new File(this.databaseUriString);
        nodes.add(this.databaseUriString);
        try {
            InputStream is = new FileInputStream(database.getPath());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(is));
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("node");

            nodes.add("viduje");

            for (int i=0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                nodes.add(nameValue);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

//        nodes.add("vienas");
//        nodes.add("du");
//        nodes.add("trys");
        return nodes;
    }


}
