package com.ffda.sourcherry;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    private Context context;

    public XMLReader(InputStream is, Context context) {
        // Creates a document that can be used to read tags with provided InputStream
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.doc = db.parse(new InputSource(is));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        this.context = context;
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

    public SpannableStringBuilder getNodeContent(String uniqueID) {
        // Original XML document has newline characters marked
        // Returns ArrayList of SpannableStringBuilder elements

        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder();
        NodeList nodeList = this.doc.getElementsByTagName("node");

        int totalCharOffset = 0;

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueID)) { // Finds node that user chose

                NodeList nodeContentNodeList = node.getChildNodes(); // Gets all the subnodes/childnodes of selected node

                for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                    // Loops through nodes of selected node
                    Node currentNode = nodeContentNodeList.item(x);
                    String currentNodeType = currentNode.getNodeName();
                    if (currentNodeType.equals("rich_text")) {
                        if (currentNode.hasAttributes()) {
                            nodeContentStringBuilder.append(makeFormattedRichText(currentNode));
                        } else {
                            nodeContentStringBuilder.append(currentNode.getTextContent());
                        }
                    } else if (currentNodeType.equals("codebox")) {
                        int charOffset = getCharOffset(currentNode);

                        SpannableStringBuilder codeboxText = makeFormattedCodebox(currentNode);
                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, codeboxText);
                        totalCharOffset += codeboxText.length() - 1;
                    } else if (currentNodeType.equals("encoded_png")) {
                        int charOffset = getCharOffset(currentNode);

                        nodeContentStringBuilder.insert(charOffset, getImageSpan(currentNode));
                    }
                }
            }
        }

        return nodeContentStringBuilder;
    }

    public SpannableStringBuilder makeFormattedRichText(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting made based on nodes attribute
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
            } else if (attribute.equals("link")) {
                String linkURL = nodeAttributes.item(i).getTextContent();
                URLSpan us = new URLSpan(linkURL);
                formattedNodeText.setSpan(us,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return formattedNodeText;
    }

    public SpannableStringBuilder makeFormattedCodebox(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting isn't based on nodes attributes, because all codebox'es will look the same
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(node.getTextContent());

        // Adds vertical line in front the paragraph, to make it stand out as quote
        QuoteSpan qs = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            qs = new QuoteSpan(Color.parseColor("#AC1111"), 5, 30);
        } else {
            qs = new QuoteSpan(Color.RED);
        }
        formattedCodebox.setSpan(qs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodebox.setSpan(tf, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Changes background color. Doesn't work as it should to / look decent.
//        BackgroundColorSpan bcs = new BackgroundColorSpan(Color.BLUE);
//        formattedNodeText.setSpan(bcs, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return formattedCodebox;
    }

    public SpannableStringBuilder getImageSpan(Node node) {
        // Returns SpannableStringBuilder that has spans with images in them
        // Images are decoded from Base64 string embedded in the tag

        SpannableStringBuilder formattedImage = new SpannableStringBuilder();

        formattedImage.append(" ");

        try {
            byte[] decodedString = Base64.decode(node.getTextContent().trim(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Drawable image = new BitmapDrawable(context.getResources(),decodedByte);
            image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            ImageSpan is = new ImageSpan(image);
            formattedImage.setSpan(is, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (Exception e) {
            Toast.makeText(this.context, "Failed to load image", Toast.LENGTH_SHORT).show();
        }

        return formattedImage;
    }


    public int getCharOffset(Node node) {
        // Returns character offset value that is used in codebox and encoded_png tags
        // It is needed to add text in the correct location
        // One needs to -1 from the value to make it work
        // I don't have and idea why

        Element el = (Element) node;
        int charOffset = Integer.valueOf(el.getAttribute("char_offset"));
        return charOffset;
    }
}
