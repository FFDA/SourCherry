/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import lt.ffda.sourcherry.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLReader implements DatabaseReader{
    private Document doc;
    private Context context;
    private FragmentManager fragmentManager;

    public XMLReader(InputStream is, Context context, FragmentManager fragmentManager) {
        // Creates a document that can be used to read tags with provided InputStream
        this.context = context;
        this.fragmentManager = fragmentManager;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.doc = db.parse(new InputSource(is));
        } catch (Exception e) {
            Toast.makeText(this.context, "Failed to load database", Toast.LENGTH_SHORT).show();
            System.out.println(e.getMessage());
        }
    }

    @Override
    public ArrayList<String[]> getAllNodes() {
        // Returns all the node from the document
        // Used for the search/filter in the drawer menu
        NodeList nodeList = this.doc.getElementsByTagName("node");
        ArrayList<String[]> nodes = returnSubnodeArrayList(nodeList, "false");
        return nodes;
    }

    @Override
    public ArrayList<String[]> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts

        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("cherrytree"); // There is only one this type of tag in the database
        NodeList mainNodeList = nodeList.item(0).getChildNodes(); // So selecting all children of the first node is always safe

        nodes = returnSubnodeArrayList(mainNodeList, "false");

        return nodes;
    }

    @Override
    public ArrayList<String[]> getBookmarkedNodes() {
        // Returns bookmarked nodes from the document
        // Returns null if there aren't any
        ArrayList<String[]> nodes = new ArrayList<>();
        NodeList nodeBookmarkNode = this.doc.getElementsByTagName("bookmarks");
        if (nodeBookmarkNode == null) {
            return null;
        } else {
            List<String> uniqueIDArray = Arrays.asList(nodeBookmarkNode.item(0).getAttributes().getNamedItem("list").getNodeValue().split(","));
            NodeList nodeList = this.doc.getElementsByTagName("node");
            int counter = 0; // Counter to check if all bookmarked nodes were found
            for (int i=0; i < nodeList.getLength(); i++) {
                if (counter < nodeList.getLength()) {
                    Node node = nodeList.item(i);
                    if (uniqueIDArray.contains(node.getAttributes().getNamedItem("unique_id").getNodeValue())) {
                        String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                        String uniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                        String hasSubnode = String.valueOf(hasSubnodes(node));
                        String isParent = "false"; // There is only one parent Node and its added manually in getSubNodes()
                        String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, "false"};
                        nodes.add(currentNodeArray);
                    }
                } else {
                    break;
                }
            }
        }
        return nodes;
    }

    @Override
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
        // holds individual menu items

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

    public String[] createParentNode(Node parentNode) {
        // Creates and returns the node that will be added to the node array as parent node
        String parentNodeName = parentNode.getAttributes().getNamedItem("name").getNodeValue();
        String parentNodeUniqueID = parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        String parentNodeHasSubnode = String.valueOf(hasSubnodes(parentNode));
        String parentNodeIsParent = "true";
        String parentNodeIsSubnode = "false";
        String[] node = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};
        return node;
    }

    @Override
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

    @Override
    public String[] getSingleMenuItem(String uniqueNodeID) {
        // Returns single menu item to be used when opening anchor links
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueNodeID)) {
                if (node.getNodeName().equals("node")) {
                    // Node name and unique_id always the same for the node
                    String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                    String uniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                    if (hasSubnodes(node)) {
                        // if node has subnodes, then it has to be opened as a parent node and displayed as such
                        String hasSubnode = "true";
                        String isParent = "true";
                        String isSubnode = "false";
                        String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, isSubnode};
                        return currentNodeArray;
                    } else {
                        // If node doesn't have subnodes, then it has to be opened as subnode of some other node
                        String hasSubnode = "false";
                        String isParent = "false";
                        String isSubnode = "true";
                        String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, isSubnode};
                        return currentNodeArray;
                    }
                }
            }
        }
        return null; // null if no node was found
    }

    @Override
    public ArrayList<ArrayList<CharSequence[]>> getNodeContent(String uniqueID) {
        // Original XML document has newline characters marked
        // Returns ArrayList of SpannableStringBuilder elements

        ArrayList<ArrayList<CharSequence[]>> nodeContent = new ArrayList<>(); // The one that will be returned

        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary for text, codebox, image formatting
        ArrayList<ArrayList<CharSequence[]>> nodeTables = new ArrayList<>(); // Temporary for table storage

        NodeList nodeList = this.doc.getElementsByTagName("node");

        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        ////

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(uniqueID)) { // Finds node that user chose

                // prog_lang attribute is the same as syntax in SQL database
                // it is used to set formatting for the node and separate between node types
                // The same attribute is used for codeboxes
                String nodeProgLang = node.getAttributes().getNamedItem("prog_lang").getNodeValue();

                if (nodeProgLang.equals("custom-colors") || nodeProgLang.equals("plain-text")) {
                    // This is formatting for Rich Text and Plain Text nodes
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

                            if (currentNode.getAttributes().getNamedItem("filename") != null) {
                                SpannableStringBuilder attachedFileSpan = makeAttachedFileSpan(currentNode);
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                totalCharOffset += attachedFileSpan.length() - 1;
                            } else if (currentNode.getAttributes().getNamedItem("anchor") != null) {
                                // It doesn't need node to be passed to it,
                                // because there isn't any relevant information embedded into it
                                SpannableStringBuilder anchorImageSpan = makeAnchorImageSpan();
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                totalCharOffset += anchorImageSpan.length() - 1;
                            } else {
                                SpannableStringBuilder imageSpan = makeImageSpan(currentNode);
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                totalCharOffset += imageSpan.length() - 1;
                            }

                        } else if (currentNodeType.equals("table")) {
                            int charOffset = getCharOffset(currentNode) + totalCharOffset; // Place where SpannableStringBuilder will be split
                            CharSequence[] cellMaxMin = getTableMaxMin(currentNode);
                            nodeContentStringBuilder.insert(charOffset, " "); // Adding space for formatting reason
                            ArrayList<CharSequence[]> currentTable = new ArrayList<>(); // ArrayList with all the data from the table that will added to nodeTables
                            currentTable.add(new CharSequence[]{"table", String.valueOf(charOffset), cellMaxMin[0], cellMaxMin[1]}); // Values of the table. There aren't any table data in this line
                            NodeList tableRowsNodes = ((Element) currentNode).getElementsByTagName("row"); // All the rows of the table. There are empty text nodes that has to be filtered out (or only row nodes selected this way)
                            for (int row = 0; row < tableRowsNodes.getLength(); row++) {
                                currentTable.add(getTableRow(tableRowsNodes.item(row)));
                            }
                            nodeTables.add(currentTable);
                        }
                    }
                } else {
                    // Node is Code Node. It's just a big CodeBox with no dimensions
                    nodeContentStringBuilder.append(makeFormattedCodeNode(node));
                }
            }
        }

        int subStringStart = 0; // Holds start from where SpannableStringBuilder has to be split from

        if (nodeTables.size() > 0) {
            // If there are at least one table in the node
            // SpannableStringBuilder that holds are split in to parts
            // After each text array table array is added
            for (ArrayList<CharSequence[]> table: nodeTables) {
                // Getting table's char_offset that was embedded into CharArray
                // It will be used to split the text in appropriate parts
                int charOffset = Integer.valueOf((String) table.get(0)[1]);
                //

                // Creating text part of this iteration
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, charOffset);
                subStringStart = charOffset; // Next string will be cut starting from this offset (previous end)
                ArrayList<CharSequence[]> nodeContentText = new ArrayList<>();
                nodeContentText.add(new CharSequence[]{"text"});
                nodeContentText.add(new CharSequence[]{textPart});
                nodeContent.add(nodeContentText);
                //

                // Creating table part of this iteration
                ArrayList<CharSequence[]> nodeContentTable = new ArrayList<>();
                // Add string for separating text and table arrays and col_max, col_min to set table cells width
                nodeContentTable.add(new CharSequence[]{"table", table.get(0)[2], table.get(0)[3]});
                // Because first row had information about it start to read from the second line till the last one
                for (int row = 1; row < table.size(); row++) {
                    nodeContentTable.add(table.get(row));
                }
                nodeContent.add(nodeContentTable);
                //
            }
            // Last part of the SpannableStringBuilder (if there is one) is appended to nodeContent array
            if (subStringStart < nodeContentStringBuilder.length()) {
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, nodeContentStringBuilder.length());
                ArrayList<CharSequence[]> nodeContentText = new ArrayList<>();
                nodeContentText.add(new CharSequence[]{"text"});
                nodeContentText.add(new CharSequence[]{textPart});
                nodeContent.add(nodeContentText);
            }

        } else {
            // If there are no tables in the Node content
            // Only one text CharSequence array is created and added to the nodeContent
            ArrayList<CharSequence[]> nodeContentText = new ArrayList<>();
            nodeContentText.add(new CharSequence[]{"text"});
            nodeContentText.add(new CharSequence[]{nodeContentStringBuilder});
            nodeContent.add(nodeContentText);
        }

        return nodeContent;
    }

    @Override
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
                String foregroundColorOriginal = getValidColorCode(nodeAttributes.item(i).getTextContent()); // Extracting foreground color code from the tag
                ForegroundColorSpan fcs = new ForegroundColorSpan(Color.parseColor(foregroundColorOriginal));
                formattedNodeText.setSpan(fcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("background")) {
                String backgroundColorOriginal = getValidColorCode(nodeAttributes.item(i).getTextContent()); // Extracting background color code from the tag
                BackgroundColorSpan bcs = new BackgroundColorSpan(Color.parseColor(backgroundColorOriginal));
                formattedNodeText.setSpan(bcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("weight")) {
                StyleSpan boldStyleSpan = new StyleSpan(Typeface.BOLD);
                formattedNodeText.setSpan(boldStyleSpan, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("style")) {
                StyleSpan italicStyleSpan = new StyleSpan(Typeface.ITALIC);
                formattedNodeText.setSpan(italicStyleSpan, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("underline")) {
                UnderlineSpan us = new UnderlineSpan();
                formattedNodeText.setSpan(us, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("scale")) {
                String scaleValue = nodeAttributes.item(i).getTextContent();
                switch (scaleValue) {
                    case "h1": formattedNodeText.setSpan(new RelativeSizeSpan(1.75f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "h2": formattedNodeText.setSpan(new RelativeSizeSpan(1.50f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "h3": formattedNodeText.setSpan(new RelativeSizeSpan(1.25f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "small": formattedNodeText.setSpan(new RelativeSizeSpan(0.80f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "sup": formattedNodeText.setSpan(new RelativeSizeSpan(0.80f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                formattedNodeText.setSpan(new SuperscriptSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "sub": formattedNodeText.setSpan(new RelativeSizeSpan(0.80f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                formattedNodeText.setSpan(new SubscriptSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }
            } else if (attribute.equals("family")) {
                TypefaceSpan tf = new TypefaceSpan("monospace");
                formattedNodeText.setSpan(tf, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("link")) {
                String[] attributeValue = nodeAttributes.item(i).getNodeValue().split(" ");
                if (attributeValue[0].equals("webs")) {
                    // Making links to open websites
                    URLSpan us = new URLSpan(attributeValue[1]);
                    formattedNodeText.setSpan(us, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (attributeValue[0].equals("node")) {
                    // Making links to open other nodes (Anchors)
                    formattedNodeText.setSpan(makeAnchorLinkSpan(attributeValue[1]), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (attribute.equals("justification")) {
                String justification = nodeAttributes.item(i).getTextContent();
                switch (justification) {
                    case "right":   formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                    case "center":  formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                }
            }
        }
        return formattedNodeText;
    }

    public SpannableStringBuilder makeFormattedCodebox(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting depends on Codebox'es height and width
        // It is retrieved from the tag using getCodeBoxHeightWidth()
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(node.getTextContent());

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodebox.setSpan(tf, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int[] codeboxDimensions = this.getCodeBoxHeightWidth(node);

        // This part of codebox formatting depends on size of the codebox
        // Because if user made a small codebox it might have text in front or after it
        // For this reason some of the formatting can't be spanned over all the line
        if (codeboxDimensions[0] < 30 && codeboxDimensions [1] < 200) {
            BackgroundColorSpan bcs = new BackgroundColorSpan(this.context.getColor(R.color.codebox_background));
            formattedCodebox.setSpan(bcs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            // Adds vertical line in front the paragraph, to make it stand out as quote
            QuoteSpan qs = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                qs = new QuoteSpan(Color.parseColor("#AC1111"), 5, 30);
            } else {
                qs = new QuoteSpan(Color.RED);
            }
            formattedCodebox.setSpan(qs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Changes background color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
                formattedCodebox.setSpan(lbs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return formattedCodebox;
    }

    public SpannableStringBuilder makeFormattedCodeNode(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // CodeNode is just a CodeBox that do not have height and width (dimensions)

        SpannableStringBuilder formattedCodeNode = new SpannableStringBuilder();
        formattedCodeNode.append(node.getTextContent());

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodeNode.setSpan(tf, 0, formattedCodeNode.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Changes background color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
            formattedCodeNode.setSpan(lbs, 0, formattedCodeNode.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return formattedCodeNode;
    }

    public SpannableStringBuilder makeImageSpan(Node node) {
        // Returns SpannableStringBuilder that has spans with images in them
        // Images are decoded from Base64 string embedded in the tag

        SpannableStringBuilder formattedImage = new SpannableStringBuilder();

        formattedImage.append(" ");

        //// Adds image to the span
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
        ////

        //// Detects image touches/clicks
        ClickableSpan imageClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                TextView nodeContent = (TextView) widget; // This is all TextView that is being displayed for the user
                Spannable nodeContentSpan = (Spannable) nodeContent.getText(); // Getting all node content as a span
                int start = nodeContentSpan.getSpanStart(this); // Getting start position of the clicked span (this)
                int end = nodeContentSpan.getSpanEnd(this); // Getting end position of the clicked span (this)

                /// Setting up to send click span to fragment
                Bundle bundle = new Bundle();
                bundle.putCharSequence("image", nodeContentSpan.subSequence(start, end));
                ///

                XMLReader.this.fragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.pop_in, R.anim.fade_out, R.anim.fade_in, R.anim.pop_out)
                        .replace(R.id.main_view_fragment, NodeImageFragment.class, bundle)
                        .setReorderingAllowed(true)
                        .addToBackStack("image")
                        .commit();
            }
        };
        formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
        ////

        return formattedImage;
    }

    public SpannableStringBuilder makeAttachedFileSpan(Node node) {
        // Returns SpannableStringBuilder that has spans with images and filename
        // Files are decoded from Base64 string embedded in the tag

        String attachedFileFilename = node.getAttributes().getNamedItem("filename").getNodeValue();
        String time = node.getAttributes().getNamedItem("time").getNodeValue();

        SpannableStringBuilder formattedAttachedFile = new SpannableStringBuilder();

        formattedAttachedFile.append(" "); // Needed to insert image

        //// Inserting image
        Drawable drawableAttachedFileIcon = this.context.getDrawable(R.drawable.ic_outline_attachment_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpan attachedFileIcon = new ImageSpan(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        formattedAttachedFile.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ////

        formattedAttachedFile.append(attachedFileFilename); // Appending filename

        //// Detects touches on icon and filename
        ClickableSpan imageClickableSpan = new ClickableSpan() {

            @Override
            public void onClick(@NonNull View widget) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Setting up to send arguments to Dialog Fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("filename", attachedFileFilename);
                    bundle.putString("time", time);

                    SaveOpenDialogFragment saveOpenDialogFragment = new SaveOpenDialogFragment();
                    saveOpenDialogFragment.setArguments(bundle);
                    saveOpenDialogFragment.show(XMLReader.this.fragmentManager, "saveOpenDialog");
                } else {
                    Toast.makeText(XMLReader.this.context, R.string.toast_error_minimum_android_version_8, Toast.LENGTH_SHORT).show();
                }
            }
        };
        formattedAttachedFile.setSpan(imageClickableSpan, 0, attachedFileFilename.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
        ////

        return formattedAttachedFile;
    }

    @Override
    public SpannableStringBuilder makeAnchorImageSpan() {
        // Makes an image span that displays an anchor to mark position of it.
        // It does not respond to touches in any way
        SpannableStringBuilder anchorImageSpan = new SpannableStringBuilder();
        anchorImageSpan.append(" ");

        //// Inserting image
        Drawable drawableAttachedFileIcon = this.context.getDrawable(R.drawable.ic_outline_anchor_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpan attachedFileIcon = new ImageSpan(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        anchorImageSpan.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ////

        return anchorImageSpan;
    }

    @Override
    public ClickableSpan makeAnchorLinkSpan(String nodeUniqueID) {
        // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
        // As in CherryTree it's foreground color #07841B

        ClickableSpan AnchorLinkSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) XMLReader.this.context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                ds.setColor(context.getColor(R.color.anchor_link));
                ds.setUnderlineText(true);
            }
        };

        return AnchorLinkSpan;
    }

    @Override
    public CharSequence[] getTableRow(Node row) {
        // Returns CharSequence[] of the node's "cell" element text
        NodeList rowCellNodes = ((Element) row).getElementsByTagName("cell");
        CharSequence[] rowCells = new CharSequence[rowCellNodes.getLength()];
        for (int cell = 0; cell < rowCellNodes.getLength(); cell++) {
            rowCells[cell] = String.valueOf(rowCellNodes.item(cell).getTextContent());
        }
        return rowCells;
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

    public CharSequence[] getTableMaxMin(Node node) {
        // They will be used to set min and max width for table cell

        Element el = (Element) node;
        String colMax = el.getAttribute("col_max");
        String colMin = el.getAttribute("col_min");
        return new CharSequence[] {colMax, colMin};
    }

    public int[] getCodeBoxHeightWidth(Node node) {
        // This returns int[] with in codebox tag embedded box dimensions
        // They will be used to guess what type of formatting to use

        Element el = (Element) node;
        int frameHeight = Integer.valueOf(el.getAttribute("frame_height"));
        int frameWidth = Integer.valueOf(el.getAttribute("frame_width"));

        return new int[] {frameHeight, frameWidth};
    }

    @Override
    public byte[] getFileByteArray(String nodeUniqueID, String filename, String time) {
        // Returns byte array (stream) to be written to file or opened
        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) { // Finds node that user chose
                NodeList encodedpngNodeList = ((Element) node).getElementsByTagName("encoded_png"); // Gets all nodes with tag <encoded_png> (images and files)
                for (int x = 0; x < encodedpngNodeList.getLength(); x++) {
                    Node currentNode = encodedpngNodeList.item(x);
                    if (currentNode.getAttributes().getNamedItem("filename") != null) { // Checks if node has the attribute, otherwise it's an image
                        if (currentNode.getAttributes().getNamedItem("filename").getNodeValue().equals(filename)) { // If filename matches the one provided
                            if (currentNode.getAttributes().getNamedItem("time").getNodeValue().equals(time)) { // Checks if index of the file matches the counter
                                return Base64.decode(currentNode.getTextContent(), Base64.DEFAULT);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public String getValidColorCode(String originalColorCode) {
        // Sometimes, not always(!), CherryTree saves hexadecimal color values with doubled symbols
        // some colors can look like this #ffffffff0000 while other like this #ffff00 in the same file
        // To always get normal color hash code (made from 7 symbols) is the purpose of this function

        if (originalColorCode.length() == 7) {
            // If length of color code is 7 symbols it should be a valid one
            return originalColorCode;
        } else {
            // Creating a normal HEX color code, because XML tag has strange one with 13 symbols
            StringBuilder validColorCode = new StringBuilder();
            validColorCode.append("#");
            validColorCode.append(originalColorCode.substring(1,3));
            validColorCode.append(originalColorCode.substring(5,7));
            validColorCode.append(originalColorCode.substring(9,11));

            return validColorCode.toString();
        }
    }
}
