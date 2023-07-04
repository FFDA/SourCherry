/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.database;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;
import lt.ffda.sourcherry.model.ScNodeProperties;
import lt.ffda.sourcherry.model.ScSearchNode;
import lt.ffda.sourcherry.spans.BackgroundColorSpanCustom;
import lt.ffda.sourcherry.spans.ClickableSpanFile;
import lt.ffda.sourcherry.spans.ClickableSpanLink;
import lt.ffda.sourcherry.spans.ClickableSpanNode;
import lt.ffda.sourcherry.spans.ImageSpanAnchor;
import lt.ffda.sourcherry.spans.ImageSpanFile;
import lt.ffda.sourcherry.spans.ImageSpanImage;
import lt.ffda.sourcherry.spans.ImageSpanLatex;
import lt.ffda.sourcherry.spans.TypefaceSpanCodebox;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class XMLReader extends DatabaseReader {
    private Document doc;
    private final Context context;
    private final Handler handler;
    private final String databaseUri;
    private final MainViewModel mainViewModel;

    public XMLReader(String databaseUri, InputStream is, Context context, Handler handler, MainViewModel mainViewModel) {
        // Creates a document that can be used to read tags with provided InputStream
        this.databaseUri = databaseUri;
        this.context = context;
        this.handler = handler;
        this.mainViewModel = mainViewModel;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.doc = db.parse(is);
        } catch (Exception e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_read_database));
        }
    }

    @Override
    public ArrayList<ScNode> getAllNodes(boolean noSearch) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            NodeList nodeList = this.doc.getFirstChild().getChildNodes();
            ArrayList<ScNode> nodes = new ArrayList<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getNodeName().equals("node")) {
                    // If node is a "node" and not some other tag
                    if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                        // if user haven't marked to skip current node - creates a menu item
                        nodes.add(returnSearchMenuItem(nodeList.item(i)));
                    }
                    if (nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                        // if user haven't selected not to search subnodes of current node
                        // node list of current node is passed to another function that returns ArrayList with all menu items from that list
                        nodes.addAll(returnSubnodeSearchArrayListList(nodeList.item(i).getChildNodes()));
                    }
                }
            }
            return nodes;
        } else {
            NodeList nodeList = this.doc.getElementsByTagName("node");
            return returnSubnodeArrayList(nodeList, false);
        }
    }

    @Override
    public ArrayList<ScNode> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts
        NodeList nodeList = this.doc.getElementsByTagName("cherrytree"); // There is only one this type of tag in the database
        NodeList mainNodeList = nodeList.item(0).getChildNodes(); // So selecting all children of the first node is always safe
        return returnSubnodeArrayList(mainNodeList, false);
    }

    @Override
    public ArrayList<ScNode> getBookmarkedNodes() {
        // Returns bookmarked nodes from the document
        // Returns null if there aren't any
        ArrayList<ScNode> nodes = new ArrayList<>();
        NodeList nodeBookmarkNode = this.doc.getElementsByTagName("bookmarks");
        List<String> nodeUniqueIDArray = Arrays.asList(nodeBookmarkNode.item(0).getAttributes().getNamedItem("list").getNodeValue().split(","));
        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (nodeUniqueIDArray.contains(node.getAttributes().getNamedItem("unique_id").getNodeValue())) {
                String nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                boolean hasSubnode = hasSubnodes(node);
                // There is only one parent Node and its added manually in getSubNodes()
                nodes.add(new ScNode(nodeUniqueID, nameValue, false, hasSubnode, false));
            }
        }

        Collections.sort(nodes);

        if (nodes.size() == 0) {
            return null;
        } else {
            return nodes;
        }
    }

    @Override
    public ArrayList<ScNode> getSubnodes(String nodeUniqueID) {
        // Returns Subnodes of the node which nodeUniqueID is provided
        ArrayList<ScNode> nodes = new ArrayList<>();
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                // When it finds a match - creates a NodeList and uses other function to get the MenuItems
                NodeList childNodeList = node.getChildNodes();
                nodes = returnSubnodeArrayList(childNodeList, true);
                ScNode parentNode = createParentNode(node);
                nodes.add(0, parentNode);
                return nodes;
            }
        }
        return nodes;
    }

    /**
     * This function scans provided NodeList to collect all the nodes from it to be displayed as subnodes in drawer menu
     * Most of the time it is used to collect information about subnodes of the node that is being opened
     * However, it can be used to create information Main menu items
     * In that case isSubnode should passed as false
     * If true this value will make node look indented
     * @param nodeList NodeList object to collect information about nodes from
     * @param isSubnode true - means that node is not a subnode and should not be displayed indented in the drawer menu. false - apposite of that
     * @return ArrayList of node's subnodes
     */
    public ArrayList<ScNode> returnSubnodeArrayList(NodeList nodeList, boolean isSubnode) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("node")) {
                String nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                boolean hasSubnode = hasSubnodes(node);
                // There is only one parent Node and its added manually in getSubNodes()
                nodes.add(new ScNode(nodeUniqueID, nameValue, false, hasSubnode, isSubnode));
            }
        }
        return nodes;
    }

    /**
     * Creates an ArrayList of ScNode objects that can be used to display nodes during drawer menu search/filter function
     * @param nodeList NodeList object to collect information about node in it
     * @return ArrayList that contains all the nodes of the provided NodeList object
     */
    public ArrayList<ScNode> returnSubnodeSearchArrayListList(NodeList nodeList) {
        // This function scans provided NodeList and
        // returns ArrayList that holds individual menu items
        // It skips all the nodes, that are marked as to be excluded from search
        ArrayList<ScNode> nodes = new ArrayList<>();
        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("node")) {
                if (node.getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                    nodes.add(returnSearchMenuItem(node));
                }
                if (node.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                    nodes.addAll(returnSubnodeSearchArrayListList(node.getChildNodes()));
                }
            }
        }
        return nodes;
    }

    /**
     * Checks if provided Node object has a subnode(s)
     * @param node Node object to check if it has subnodes
     * @return true if node is a subnode
     */
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

    /**
     * Used during creation of the all the node in the document for drawer menu search/filter function
     * For that reason created node is never a parent node or a subnode
     * @param node node to collect information from
     * @return ScNode object for filter nodes function
     */
    private ScNode returnSearchMenuItem(Node node) {
        String nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
        String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
        boolean hasSubnode = hasSubnodes(node);
        return new ScNode(nodeUniqueID, nameValue, false, hasSubnode, false);
    }

    /**
     * Parent node (top) in the drawer menu
     * Used when creating a drawer menu
     * @param parentNode Node object of the parent node from which parent (top) node information has to be collected
     * @return ScNode object with properties of a parent node
     */
    public ScNode createParentNode(Node parentNode) {
        String parentNodeUniqueID = parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        String parentNodeName = parentNode.getAttributes().getNamedItem("name").getNodeValue();
        boolean parentNodeHasSubnode = hasSubnodes(parentNode);
        return new ScNode(parentNodeUniqueID, parentNodeName, true, parentNodeHasSubnode, false);
    }

    @Override
    public ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID) {
        // Checks if it is possible to go up in document's node tree from given nodeUniqueID
        // Returns array with appropriate nodes
        ArrayList<ScNode> nodes = null;
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                Node parentNode = node.getParentNode();
                if (parentNode == null) {
                    return nodes;
                } else if (parentNode.getNodeName().equals("cherrytree")) {
                    nodes = this.getMainNodes();
                } else {
                    NodeList parentSubnodes = parentNode.getChildNodes();
                    nodes = returnSubnodeArrayList(parentSubnodes, true);
                    nodes.add(0, createParentNode(parentNode));
                }
            }
        }
        return nodes;
    }

    @Override
    public ScNode getSingleMenuItem(String nodeUniqueID) {
        // Returns single menu item to be used when opening anchor links
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                if (node.getNodeName().equals("node")) {
                    // Node name and unique_id always the same for the node
                    String menuItemsNodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                    String nameValue = node.getAttributes().getNamedItem("name").getNodeValue();
                    if (hasSubnodes(node)) {
                        // if node has subnodes, then it has to be opened as a parent node and displayed as such
                        return new ScNode(menuItemsNodeUniqueID, nameValue, true, true, false);
                    } else {
                        // If node doesn't have subnodes, then it has to be opened as subnode of some other node
                        return new ScNode(menuItemsNodeUniqueID, nameValue, false, false, true);
                    }
                }
            }
        }
        return null; // null if no node was found
    }

    @Override
    public ArrayList<ScNodeContent> getNodeContent(String nodeUniqueID) {
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>(); // The one that will be returned
        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary for text, codebox, image formatting
        ArrayList<ScNodeContentTable> nodeTables = new ArrayList<>(); // Temporary for table storage
        ArrayList<Integer> nodeTableCharOffsets = new ArrayList<>();
        NodeList nodeList = this.doc.getElementsByTagName("node");

        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        ////
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) { // Finds node that user chose
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
                        switch (currentNodeType) {
                            case "rich_text":
                                if (currentNode.hasAttributes()) {
                                    nodeContentStringBuilder.append(makeFormattedRichText(currentNode));
                                } else {
                                    nodeContentStringBuilder.append(currentNode.getTextContent());
                                }
                                break;
                            case "codebox": {
                                int charOffset = getCharOffset(currentNode);
                                SpannableStringBuilder codeboxText = makeFormattedCodebox(currentNode);
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, codeboxText);
                                totalCharOffset += codeboxText.length() - 1;
                                break;
                            }
                            case "encoded_png": {
                                // "encoded_png" might actually be image, attached files or anchors (just images that mark the position)
                                int charOffset = getCharOffset(currentNode);
                                if (currentNode.getAttributes().getNamedItem("filename") != null) {
                                    if (currentNode.getAttributes().getNamedItem("filename").getNodeValue().equals("__ct_special.tex")) {
                                        // For latex boxes
                                        SpannableStringBuilder latexImageSpan = makeLatexImageSpan(currentNode);
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, latexImageSpan);
                                    } else {
                                        // For actual attached files
                                        SpannableStringBuilder attachedFileSpan = makeAttachedFileSpan(currentNode, nodeUniqueID);
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                        totalCharOffset += attachedFileSpan.length() - 1;
                                    }
                                } else if (currentNode.getAttributes().getNamedItem("anchor") != null) {
                                    SpannableStringBuilder anchorImageSpan = makeAnchorImageSpan(currentNode.getAttributes().getNamedItem("anchor").getNodeValue());
                                    nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                } else {
                                    // Images
                                    SpannableStringBuilder imageSpan = makeImageSpan(currentNode, nodeUniqueID, String.valueOf(charOffset));
                                    nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                }
                                break;
                            }
                            case "table": {
                                int charOffset = getCharOffset(currentNode) + totalCharOffset; // Place where SpannableStringBuilder will be split

                                nodeTableCharOffsets.add(charOffset);
                                int[] cellMinMax = getTableMinMax(currentNode);
                                ArrayList<CharSequence[]> currentTableContent = new ArrayList<>(); // ArrayList with all the content of the table

                                NodeList tableRowsNodes = ((Element) currentNode).getElementsByTagName("row"); // All the rows of the table. There are empty text nodes that has to be filtered out (or only row nodes selected this way)

                                for (int row = 0; row < tableRowsNodes.getLength(); row++) {
                                    currentTableContent.add(getTableRow(tableRowsNodes.item(row)));
                                }
                                ScNodeContentTable scNodeContentTable = new ScNodeContentTable((byte) 1, currentTableContent, cellMinMax[0], cellMinMax[1], ((Element) currentNode).getAttribute("justification"));
                                nodeTables.add(scNodeContentTable);
                                // Instead of adding space for formatting reason
                                // it might be better to take one of totalCharOffset
                                totalCharOffset -= 1;
                                break;
                            }
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
            for (int i = 0; i < nodeTables.size(); i++) {
                // Adding text part of this iteration
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, nodeTableCharOffsets.get(i));
                subStringStart = nodeTableCharOffsets.get(i); // Next string will be cut starting from this offset (previous end)
                ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, textPart);
                nodeContent.add(nodeContentText);
                // Adding table part of this iteration
                nodeContent.add(nodeTables.get(i));
            }
            // Last part of the SpannableStringBuilder (if there is one)
            if (subStringStart < nodeContentStringBuilder.length()) {
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, nodeContentStringBuilder.length());
                ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, textPart);
                nodeContent.add(nodeContentText);
            }
        } else {
            // If there are no tables in the Node content
            ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, nodeContentStringBuilder);
            nodeContent.add(nodeContentText);
        }
        return nodeContent;
    }

    /**
     * Creates a codebox span from the provided Node object
     * Formatting depends on new line characters in Node object
     * if codebox content has new line character it means that it has to span multiple lines
     * if not it's a single line box
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that has codebox content
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    public SpannableStringBuilder makeFormattedCodebox(Node node) {
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(node.getTextContent());
        // Changes font
        TypefaceSpanCodebox typefaceSpanCodebox = new TypefaceSpanCodebox("monospace");
        // Saving codebox attribute to the span
        Element element = (Element) node;
        String justificationAttribute = element.getAttribute("justification");
        typefaceSpanCodebox.setFrameWidth(Integer.parseInt(element.getAttribute("frame_width")));
        typefaceSpanCodebox.setFrameHeight(Integer.parseInt(element.getAttribute("frame_height")));
        typefaceSpanCodebox.setWidthInPixel(element.getAttribute("width_in_pixels").equals("1"));
        typefaceSpanCodebox.setSyntaxHighlighting(element.getAttribute("syntax_highlighting"));
        typefaceSpanCodebox.setHighlightBrackets(element.getAttribute("highlight_brackets").equals("1"));
        typefaceSpanCodebox.setShowLineNumbers(element.getAttribute("show_line_numbers").equals("1"));
        if (node.getTextContent().contains("\n")) {
            formattedCodebox.setSpan(typefaceSpanCodebox, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            // Adds vertical line in front the paragraph, to make it stand out as quote
            QuoteSpan qs;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                qs = new QuoteSpan(Color.parseColor("#AC1111"), 5, 30);
            } else {
                qs = new QuoteSpan(Color.RED);
            }
            formattedCodebox.setSpan(qs, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            // Changes background color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
                formattedCodebox.setSpan(lbs, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } else {
            formattedCodebox.setSpan(typefaceSpanCodebox, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            BackgroundColorSpan bcs = new BackgroundColorSpan(this.context.getColor(R.color.codebox_background));
            formattedCodebox.setSpan(bcs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (justificationAttribute.equals("right")) {
            formattedCodebox.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justificationAttribute.equals("center")) {
            formattedCodebox.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedCodebox;
    }

    /**
     * Creates SpannableStringBuilder with the content of the CodeNode
     * CodeNode is just a CodeBox that do not have height and width (dimensions)
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that contains content of the node
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    public SpannableStringBuilder makeFormattedCodeNode(Node node) {
        SpannableStringBuilder formattedCodeNode = new SpannableStringBuilder();
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeName().equals("rich_text")) {
                formattedCodeNode.append(currentNode.getTextContent());
                break;
            }
        }

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

    /**
     * Creates a SpannableStringBuilder with image in it
     * Image is created from Base64 string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that has Image embedded in it
     * @param nodeUniqueID unique ID of the node that has image embedded in it
     * @param imageOffset image offset that can be extracted from the taf of the node
     * @return SpannableStringBuilder that has spans with image in them
     */
    public SpannableStringBuilder makeImageSpan(Node node, String nodeUniqueID, String imageOffset) {
        SpannableStringBuilder formattedImage = new SpannableStringBuilder();
        ImageSpanImage imageSpanImage;
        //* Adds image to the span
        try {
            formattedImage.append(" ");
            byte[] decodedString = Base64.decode(node.getTextContent(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Drawable image = new BitmapDrawable(context.getResources(), decodedByte);
            image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            if (image.getIntrinsicWidth() > width) {
                // If image is wider than screen it is scaled down to fit the screen
                // otherwise it will not load/be display
                float scale = ((float) width / image.getIntrinsicWidth()) - (float) 0.1;
                int newWidth = (int) (image.getIntrinsicWidth() * scale);
                int newHeight = (int) (image.getIntrinsicHeight() * scale);
                image.setBounds(0, 0, newWidth, newHeight);
            }
            imageSpanImage = new ImageSpanImage(image);
            formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting fragment to view enlarged zoomable image
                    ((MainView) context).openImageView(nodeUniqueID, imageOffset);
                }
            };
            formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken image span to display in node content
            imageSpanImage = (ImageSpanImage) this.getBrokenImageSpan(0);
            formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_load_image));
        }
        //*
        String justificationAttribute = node.getAttributes().getNamedItem("justification").getNodeValue();
        if (justificationAttribute.equals("right")) {
            formattedImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justificationAttribute.equals("center")) {
            formattedImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedImage;
    }

    /**
     * Creates a SpannableStringBuilder with image with drawn Latex formula
     * Image is created from latex code string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that has a Latex code embedded in it
     * @return SpannableStringBuilder that has span with Latex image in them
     */
    public SpannableStringBuilder makeLatexImageSpan(Node node) {
        SpannableStringBuilder formattedLatexImage = new SpannableStringBuilder();
        ImageSpanLatex imageSpanLatex;
        //* Creates and adds image to the span
        try {
            // Embedded latex code has tags/code that is not recognized by jlatexmath-android
            // It has to be removed
            formattedLatexImage.append(" ");
            String latexString = node.getTextContent()
                .replace("\\documentclass{article}\n" +
                "\\pagestyle{empty}\n" +
                "\\usepackage{amsmath}\n" +
                "\\begin{document}\n" +
                "\\begin{align*}", "")
                .replace("\\end{align*}\n\\end{document}", "")
                .replaceAll("&=", "="); // Removing '&' sing, otherwise latex image fails to compile

            final JLatexMathDrawable latexDrawable = JLatexMathDrawable.builder(latexString)
                .textSize(40)
                .padding(8)
                .background(0xFFffffff)
                .align(JLatexMathDrawable.ALIGN_RIGHT)
                .build();

            latexDrawable.setBounds(0, 0, latexDrawable.getIntrinsicWidth(), latexDrawable.getIntrinsicHeight());

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            if (latexDrawable.getIntrinsicWidth() > width - 50) {
                // If image is wider than screen-50 px it is scaled down to fit the screen
                // otherwise it will not load/be display
                float scale = ((float) width / latexDrawable.getIntrinsicWidth()) - (float) 0.2;
                int newWidth = (int) (latexDrawable.getIntrinsicWidth() * scale);
                int newHeight = (int) (latexDrawable.getIntrinsicHeight() * scale);
                latexDrawable.setBounds(0, 0, newWidth, newHeight);
            }
            imageSpanLatex = new ImageSpanLatex(latexDrawable);
            formattedLatexImage.setSpan(imageSpanLatex, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting fragment to view enlarged zoomable image
                    ((MainView) XMLReader.this.context).openImageView(latexString);
                }
            };
            formattedLatexImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken latex image span to display in node content
            imageSpanLatex = (ImageSpanLatex) this.getBrokenImageSpan(1);
            formattedLatexImage.setSpan(imageSpanLatex, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_compile_latex));
        }
        //*
        String justificationAttribute = node.getAttributes().getNamedItem("justification").getNodeValue();
        if (justificationAttribute.equals("right")) {
            formattedLatexImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedLatexImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justificationAttribute.equals("center")) {
            formattedLatexImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedLatexImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        imageSpanLatex.setLatexCode(node.getTextContent());
        return formattedLatexImage;
    }

    /**
     * Creates a clickable span that initiates a context to open/save attached file
     * Files are decoded from Base64 string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that contains filename and datetime parameters
     * @param nodeUniqueID unique ID of the node to which file was attached to
     * @return SpannableStringBuilder that has spans with image and filename
     */
    public SpannableStringBuilder makeAttachedFileSpan(Node node, String nodeUniqueID) {
        String attachedFileFilename = node.getAttributes().getNamedItem("filename").getNodeValue();
        String time = node.getAttributes().getNamedItem("time").getNodeValue();
        String offset = node.getAttributes().getNamedItem("char_offset").getNodeValue();
        SpannableStringBuilder formattedAttachedFile = new SpannableStringBuilder();
        formattedAttachedFile.append(" "); // Needed to insert an image
        // Inserting image
        Drawable drawableAttachedFileIcon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_attachment_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpanFile attachedFileIcon = new ImageSpanFile(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        attachedFileIcon.setFromDatabase(true);
        attachedFileIcon.setNodeUniqueId(nodeUniqueID);
        attachedFileIcon.setFilename(attachedFileFilename);
        attachedFileIcon.setTimestamp(time);
        attachedFileIcon.setOriginalOffset(offset);
        formattedAttachedFile.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        formattedAttachedFile.append(attachedFileFilename); // Appending filename
        // Detects touches on icon and filename
        ClickableSpanFile imageClickableSpan = new ClickableSpanFile() {
            @Override
            public void onClick(@NonNull View widget) {
            // Launches function in MainView that checks if there is a default action in for attached files
            ((MainView) XMLReader.this.context).saveOpenFile(nodeUniqueID, attachedFileFilename, time, offset);
            }
        };
        formattedAttachedFile.setSpan(imageClickableSpan, 0, attachedFileFilename.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
        String justificationAttribute = node.getAttributes().getNamedItem("justification").getNodeValue();
        if (justificationAttribute.equals("right")) {
            formattedAttachedFile.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedAttachedFile.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justificationAttribute.equals("center")) {
            formattedAttachedFile.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedAttachedFile.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedAttachedFile;
    }

    @Override
    public SpannableStringBuilder makeAnchorImageSpan(String anchorValue) {
        // Makes an image span that displays an anchor to mark position of it.
        // It does not respond to touches in any way
        SpannableStringBuilder anchorImageSpan = new SpannableStringBuilder();
        anchorImageSpan.append(" ");
        // Inserting image
        Drawable drawableAttachedFileIcon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_anchor_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpanAnchor attachedFileIcon = new ImageSpanAnchor(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, anchorValue);
        anchorImageSpan.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return anchorImageSpan;
    }

    @Override
    public ClickableSpanNode makeAnchorLinkSpan(String nodeUniqueID, String linkAnchorName) {
        // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
        // As in CherryTree it's foreground color #07841B
        ClickableSpanNode clickableSpanNode = new ClickableSpanNode() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) XMLReader.this.context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                ds.setColor(context.getColor(R.color.link_anchor));
                ds.setUnderlineText(true);
            }
        };
        clickableSpanNode.setNodeUniqueID(nodeUniqueID);
        if (linkAnchorName.length() > 0) {
            clickableSpanNode.setLinkAnchorName(linkAnchorName);
        }
        return clickableSpanNode;
    }

    @Override
    public ClickableSpanLink makeFileFolderLinkSpan(String type, String base64Filename) {
        // Creates and returns a span for a link to external file or folder
        // When user clicks on the link snackbar displays a path to the file that was saved in the original system
        ClickableSpanLink clickableSpanLink = new ClickableSpanLink() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                ((MainView) XMLReader.this.context).fileFolderLinkFilepath(new String(Base64.decode(base64Filename, Base64.DEFAULT)));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                if (type.equals("file")) {
                    ds.setColor(context.getColor(R.color.link_file));
                } else {
                    ds.setColor(context.getColor(R.color.link_folder));
                }
                ds.setUnderlineText(true);
            }
        };
        clickableSpanLink.setLinkType(type);
        clickableSpanLink.setBase64Link(base64Filename);
        return clickableSpanLink;
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

    /**
     * Returns character offset value that is used in codebox and encoded_png tags
     * It is needed to add text in the correct location
     * One needs to -1 from the value to make it work
     * I don't have any idea why
     * @param node Node object to extract it's character offset
     * @return offset of the node content
     */
    public int getCharOffset(Node node) {
        Element element = (Element) node;
        return Integer.parseInt(element.getAttribute("char_offset"));
    }

    /**
     * Returns embedded table dimensions in the node
     * Used to set min and max width for the table cell
     * @param node table Node object
     * @return CharSequence[] {colMax, colMin} of table dimensions
     */
    public int[] getTableMinMax(Node node) {
        Element el = (Element) node;
        int colMin = Integer.parseInt(el.getAttribute("col_min"));
        int colMax = Integer.parseInt(el.getAttribute("col_max"));

        return new int[] {colMin, colMax};
    }

    /**
     * No longer in use
     * While function works it no longer used in creating codeboxes
     * @param node codebox node from which extract codebox dimensions that are embedded into tags
     * @return int[] {frameHeight, frameWidth} with codebox dimensions
     */
    public int[] getCodeBoxHeightWidth(Node node) {
        // This returns int[] with in codebox tag embedded box dimensions
        // They will be used to guess what type of formatting to use
        Element el = (Element) node;
        int frameHeight = Integer.parseInt(el.getAttribute("frame_height"));
        int frameWidth = Integer.parseInt(el.getAttribute("frame_width"));
        return new int[] {frameHeight, frameWidth};
    }

    @Override
    public byte[] getFileByteArray(String nodeUniqueID, String filename, String time, String offset) {
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
                            if (currentNode.getAttributes().getNamedItem("time").getNodeValue().equals(time) && currentNode.getAttributes().getNamedItem("char_offset").getNodeValue().equals(offset)) { // Checks if timestamp and offset matches the file
                                // Will crash the app if file is big enough (11MB on my phone). No way to catch it as exception.
                                return Base64.decode(currentNode.getTextContent(), Base64.DEFAULT);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public byte[] getImageByteArray(String nodeUniqueID, String offset) {
        // Returns image byte array to be displayed in ImageViewFragment because some of the images are too big to pass in a bundle
        NodeList nodeList = this.doc.getElementsByTagName("node");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) { // Finds node that user chose
                NodeList encodedpngNodeList = ((Element) node).getElementsByTagName("encoded_png"); // Gets all nodes with tag <encoded_png> (images and files)
                for (int x = 0; x < encodedpngNodeList.getLength(); x++) {
                    Node currentNode = encodedpngNodeList.item(x);
                    if (currentNode.getAttributes().getNamedItem("filename") == null) { // Checks if node has the attribute "filename". If it does - it's a file
                        if (currentNode.getAttributes().getNamedItem("char_offset").getNodeValue().equals(offset)) { // If offset matches the one provided
                            return Base64.decode(currentNode.getTextContent(), Base64.DEFAULT);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void displayToast(String message) {
        // Displays a toast on main thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(XMLReader.this.context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public ImageSpan getBrokenImageSpan(int type) {
        // Returns an image span that is used to display as placeholder image
        // used when cursor window is to small to get an image blob
        // pass 0 to get broken image span, pass 1 to get broken latex span
        Drawable drawableBrokenImage;
        ImageSpan brokenImage;
        if (type == 0) {
            drawableBrokenImage = AppCompatResources.getDrawable(context, R.drawable.ic_outline_broken_image_48);
            brokenImage = new ImageSpanImage(drawableBrokenImage);
        } else {
            drawableBrokenImage = AppCompatResources.getDrawable(context, R.drawable.ic_outline_broken_latex_48);
            brokenImage = new ImageSpanLatex(drawableBrokenImage);
        }
        // Inserting image
        drawableBrokenImage.setBounds(0,0, drawableBrokenImage.getIntrinsicWidth(), drawableBrokenImage.getIntrinsicHeight());
        return brokenImage;
    }

    @Override
    public boolean doesNodeExist(String nodeUniqueID) {
        if (nodeUniqueID == null) {
            return false;
        }
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getNodeMaxID() {
        int nodeUniqueID = -1;
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            int foundNodeUniqueID = Integer.parseInt(node.getAttributes().getNamedItem("unique_id").getNodeValue());
            if (foundNodeUniqueID > nodeUniqueID) {
                nodeUniqueID = foundNodeUniqueID;
            }
        }
        return nodeUniqueID;
    }

    /**
     * Write opened database to the file
     * App has to have the the permissions to write to said file
     */
    public void writeIntoDatabase(){
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(this.doc);
            OutputStream fileOutputStream;
            if (this.databaseUri.startsWith("content://")) {
                fileOutputStream = context.getContentResolver().openOutputStream(Uri.parse(this.databaseUri), "wt");
            } else {
                fileOutputStream = new FileOutputStream(this.databaseUri);
            }
            StreamResult result = new StreamResult(fileOutputStream);  // To save it in the Internal Storage
            transformer.transform(dSource, result);
        } catch (TransformerException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_database_changes));
        } catch (FileNotFoundException e) {
            this.displayToast(this.context.getString(R.string.toast_error_database_does_not_exists));
        }
    }

    @Override
    public ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh) {
        Node node = null;
        if (nodeUniqueID.equals("0")) {
            // User chose to create the node in main menu
            NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
            node = nodeList.item(0);
        } else {
            NodeList nodeList = this.doc.getElementsByTagName("node");
            for (int i = 0; i < nodeList.getLength(); i++) {
                node = nodeList.item(i);
                if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                    break;
                }
            }
        }
        String newNodeUniqueID = String.valueOf(getNodeMaxID() + 1);
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Creating new node with all necessary tags
        Element newNode = this.doc.createElement("node");
        newNode.setAttribute("name", name);
        newNode.setAttribute("unique_id", newNodeUniqueID);
        newNode.setAttribute("prog_lang", progLang);
        newNode.setAttribute("tags", "");
        newNode.setAttribute("readonly", "0");
        newNode.setAttribute("nosearch_me", noSearchMe);
        newNode.setAttribute("nosearch_ch", noSearchCh);
        newNode.setAttribute("custom_icon_id", "0");
        newNode.setAttribute("is_bold", "0");
        newNode.setAttribute("foreground", "");
        newNode.setAttribute("ts_creation", timestamp);
        newNode.setAttribute("ts_lastsave", timestamp);

        boolean isSubNode = true;
        // Adding node to document
        if (relation == 0) {
            // As a sibling to selected node
            if (node.getNextSibling() == null) {
                // Selected node was the last of the parents children
                // Selecting at the end of parent children node list
                node.getParentNode().appendChild(newNode);
            } else {
                // Inserting after selected node
                node.getParentNode().insertBefore(newNode, node.getNextSibling());
            }
            // Checking if node is being created as MainMenu node
            // Needed to set correct indentation for the node in the menu
            if (node.getParentNode().getNodeName().equals("cherrytree")) {
                isSubNode = false;
            }
        } else {
            // As a subnode of selected node
            node.appendChild(newNode);
        }
        this.writeIntoDatabase();
        return new ScNode(newNodeUniqueID, name,false, false, isSubNode);
    }

    @Override
    public boolean isNodeBookmarked(String nodeUniqueID) {
        NodeList bookmarkTag = this.doc.getElementsByTagName("bookmarks");
        List<String> bookmarks = Arrays.asList(bookmarkTag.item(0).getAttributes().getNamedItem("list").getNodeValue().split(","));
        return bookmarks.contains(nodeUniqueID);
    }

    @Override
    public void addNodeToBookmarks(String nodeUniqueID) {
        NodeList bookmarkTag = this.doc.getElementsByTagName("bookmarks");
        Node bookmarksNode = bookmarkTag.item(0);
        String bookmarks = bookmarksNode.getAttributes().getNamedItem("list").getNodeValue(); // This is a string with all bookmark IDs separated by comma (,)

        if (bookmarks.length() == 0) {
            bookmarks = nodeUniqueID;
        } else {
            bookmarks += "," + nodeUniqueID;
        }

        bookmarksNode.getAttributes().getNamedItem("list").setTextContent(bookmarks);
        writeIntoDatabase();
    }

    @Override
    public void removeNodeFromBookmarks(String nodeUniqueID) {
        NodeList bookmarkTag = this.doc.getElementsByTagName("bookmarks");
        Node bookmarksNode = bookmarkTag.item(0);
        ArrayList<String> bookmarks = new ArrayList<>(Arrays.asList(bookmarkTag.item(0).getAttributes().getNamedItem("list").getNodeValue().split(",")));
        bookmarks.remove(nodeUniqueID);
        bookmarksNode.getAttributes().getNamedItem("list").setTextContent(String.join(",", bookmarks));
        writeIntoDatabase();
    }

    @Override
    public boolean areNodesRelated(String targetNodeUniqueID, String destinationNodeUniqueID) {
        ArrayList<String> heredity = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            // Finds destination node
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(destinationNodeUniqueID)) {
                heredity.add(node.getAttributes().getNamedItem("unique_id").getNodeValue());
                // Goes up the document tree and adds every nodes unique ID to heredity list
                // until reaches cherrytree tag
                while (true) {
                    Node parentNode = node.getParentNode();
                    if (parentNode.getNodeName().equals("cherrytree")) {
                        break;
                    } else {
                        heredity.add(parentNode.getAttributes().getNamedItem("unique_id").getNodeValue());
                        node = parentNode;
                    }
                }
                break;
            }
        }
        // Returns true if heredity contains unique ID of the target node
        return heredity.contains(targetNodeUniqueID);
    }

    @Override
    public void moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        if (areNodesRelated(targetNodeUniqueID, destinationNodeUniqueID)) {
            displayToast(context.getString(R.string.toast_error_new_parent_cant_be_one_of_its_children));
        } else {
            Node targetNode = null;
            Node destinationNode = null;

            // User chose to move the node to main menu
            if (destinationNodeUniqueID.equals("0")) {
                NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
                destinationNode = nodeList.item(0);
            }

            NodeList nodeList = this.doc.getElementsByTagName("node");
            for (int i = 0; i < nodeList.getLength(); i++) {
                // Goes through the nodes until target and destination nodes are found
                if (targetNode == null || destinationNode == null) {
                    Node node = nodeList.item(i);
                    if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(targetNodeUniqueID)) {
                        targetNode = nodeList.item(i);
                    }
                    if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(destinationNodeUniqueID)) {
                        destinationNode = nodeList.item(i);
                    }
                } else {
                    break;
                }
            }

            // Checks for when user wants to move node to the same parent node
            // In XML databases that causes crash and it is not necessary write operation
            if (destinationNode.getNodeName().equals("cherrytree") && targetNode.getParentNode().getNodeName().equals("cherrytree")) {
                return;
            }
            Node parentNodeUniqueID = targetNode.getParentNode().getAttributes().getNamedItem("unique_id");
            if (parentNodeUniqueID != null && parentNodeUniqueID.getNodeValue().equals(destinationNodeUniqueID)) {
                return;
            }

            destinationNode.appendChild(targetNode);
            this.writeIntoDatabase();
        }
    }

    @Override
    public void deleteNode(String nodeUniqueID) {
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                node.getParentNode().removeChild(node);
                this.writeIntoDatabase();
                break;
            }
        }
    }

    @Override
    public ScNodeProperties getNodeProperties(String nodeUniqueID) {
        ScNodeProperties nodeProperties = null;
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                NamedNodeMap properties = node.getAttributes();
                String name = properties.getNamedItem("name").getNodeValue();
                String progLang = properties.getNamedItem("prog_lang").getNodeValue();
                byte noSearchMe = Byte.parseByte(properties.getNamedItem("nosearch_me").getNodeValue());
                byte noSearchCh = Byte.parseByte(properties.getNamedItem("nosearch_ch").getNodeValue());
                nodeProperties = new ScNodeProperties(nodeUniqueID, name, progLang, noSearchMe, noSearchCh);
                break;
            }
        }
        return nodeProperties;
    }

    @Override
    public void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh) {
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                NamedNodeMap properties = node.getAttributes();
                properties.getNamedItem("name").setNodeValue(name);
                if (properties.getNamedItem("prog_lang").getNodeValue().equals("custom-colors") && !progLang.equals("custom-colors")) {
                    StringBuilder nodeContent = this.convertRichTextNodeContentToPlainText(node);
                    this.deleteNodeContent(node);
                    Element newContentNode = this.doc.createElement("rich_text");
                    newContentNode.setTextContent(nodeContent.toString());
                    node.appendChild(newContentNode);
                }
                properties.getNamedItem("prog_lang").setNodeValue(progLang);
                properties.getNamedItem("nosearch_me").setNodeValue(noSearchMe);
                properties.getNamedItem("nosearch_ch").setNodeValue(noSearchCh);
                properties.getNamedItem("ts_lastsave").setNodeValue(String.valueOf(System.currentTimeMillis()));
                break;
            }
        }
        this.writeIntoDatabase();
    }

    /**
     * Coverts content of provided node from rich-text to plain-text or automatic-syntax-highlighting
     * Conversion adds all the content from the node's rich-text tags to StringBuilder
     * that can be added to the new rich-text (single) tag later
     * @param contentNode node that needs to be converted
     * @return StringBuilder with all the node content without addition tags
     */
    private StringBuilder convertRichTextNodeContentToPlainText(Node contentNode) {
        StringBuilder nodeContent = new StringBuilder();
        int totalCharOffset = 0;
        NodeList nodeList = contentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("rich_text")) {
                nodeContent.append(node.getTextContent());
            } else if (node.getNodeName().equals("table")) {
                int charOffset = getCharOffset(node) + totalCharOffset;
                StringBuilder tableContent = this.convertTableNodeContentToPlainText(node);
                nodeContent.insert(charOffset, tableContent);
                totalCharOffset += tableContent.length() - 1;
            } else if (node.getNodeName().equals("encoded_png")) {
                if (node.getAttributes().getNamedItem("filename") != null) {
                    if (node.getAttributes().getNamedItem("filename").getNodeValue().equals("__ct_special.tex")) {
                        // For latex boxes
                        int charOffset = getCharOffset(node) + totalCharOffset;
                        StringBuilder latexContent = this.convertLatexToPlainText(node);
                        nodeContent.insert(charOffset, latexContent);
                        totalCharOffset += latexContent.length() - 1;
                        continue;
                    } else {
                        totalCharOffset -= 1;
                    }
                // For every element, even ones that will not be added
                // 1 has to be deducted from totalCharOffset
                // to make node's data be displayed in order
                } else if (node.getAttributes().getNamedItem("anchor") != null) {
                    totalCharOffset -= 1;
                } else {
                    totalCharOffset -= 1;
                }
            } else if (node.getNodeName().equals("codebox")) {
                int charOffset = getCharOffset(node) + totalCharOffset;
                StringBuilder codeboxContent = this.convertCodeboxToPlainText(node);
                nodeContent.insert(charOffset, codeboxContent);
                totalCharOffset += codeboxContent.length() - 1;
            }
        }
        return nodeContent;
    }

    /**
     * Coverts content of the table Node (part of content node) to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param tableNode table node that needs to be converted
     * @return StringBuilder that can be added to the content node StringBuilder at the proper offset
     */
    private StringBuilder convertTableNodeContentToPlainText(Node tableNode) {
        StringBuilder tableContent = new StringBuilder();
        NodeList nodeList = tableNode.getChildNodes();
        int tableRowCount = nodeList.getLength() / 2;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("row")) {
                // Header row for the table is kept at the end of the table
                // When converting to string it has to be added to the beginning
                // of the string fro the information to make sense
                if (tableRowCount > 1) {
                    tableContent.append(this.convertTableRowToPlainText(node));
                } else {
                    tableContent.insert(0, this.convertTableRowToPlainText(node));
                }
                tableRowCount--;
            }
        }
        tableContent.insert(0, "\n");
        return tableContent;
    }

    /**
     * Converts latex node content to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param node latex node that needs to be converted
     * @return StringBuilder that can be added to the content node StringBuilder at the proper offset
     */
    private StringBuilder convertLatexToPlainText(Node node) {
        StringBuilder latexContent = new StringBuilder();
        latexContent.append(node.getTextContent());
        latexContent.delete(0, 79);
        latexContent.delete(latexContent.length()-14, latexContent.length());
        latexContent.insert(0,getSeparator());
        latexContent.insert(0, "\n");
        latexContent.append(getSeparator());
        latexContent.append("\n");
        return latexContent;
    }

    /**
     * Coverts codebox node content to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param node codebox node that needs to be converted
     * @return StringBuilder that can be added to the node StringBuilder at the proper offset
     */
    private StringBuilder convertCodeboxToPlainText(Node node) {
        StringBuilder codeboxContent = new StringBuilder();
        codeboxContent.append("\n");
        codeboxContent.append(getSeparator());
        codeboxContent.append("\n");
        codeboxContent.append(node.getTextContent());
        codeboxContent.append("\n");
        codeboxContent.append(getSeparator());
        codeboxContent.append("\n");
        return codeboxContent;
    }

    /**
     * Removes all rich_text tags from the node
     * Used to prepare node for conversion from rich-text to plain-text
     * @param node node from which to delete all the content
     */
    public void deleteNodeContent(Node node) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (!currentNode.getNodeName().equals("node")) {
                node.removeChild(currentNode);
            }
        }
    }

    @Override
    public boolean isNodeRichText(String nodeUniqueID) {
        boolean result = false;
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                if (node.getAttributes().getNamedItem("prog_lang").getNodeValue().equals("custom-colors")) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void saveNodeContent(String nodeUniqueID) {
        Node node = null;
        NodeList nodeList = this.doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                break;
            }
        }
        if (node == null) {
            this.displayToast(this.context.getString(R.string.toast_error_while_saving_node_content_not_found));
            return;
        }
        if (this.isNodeRichText(nodeUniqueID)) {
            int next; // The end of the current span and the start of the next one
            int totalContentLength = 0; // Needed to calculate offset for the tag
            int currentPartContentLength = 0; // Needed to calculate offset for the tag
            ArrayList<Element> normalNodes = new ArrayList<>(); // Store all normal tags in order
            ArrayList<Element> offsetNodes = new ArrayList<>(); // Store all tags with char_offset attribute
            // Can't get justification for all items that have offset (except tables), so the best next
            // thing I can do is save last detected justification value and used it when creating those nodes
            String lastFoundJustification = "left";
            for (ScNodeContent scNodeContent : this.mainViewModel.getNodeContent()) {
                if (scNodeContent.getContentType() == 0) {
                    // To not add content of the the span that is being processed
                    // set addContent to false. Needed because not all elements of the node
                    // have content that is displayed for user in text form.
                    boolean addContent;
                    ScNodeContentText scNodeContentText = (ScNodeContentText) scNodeContent;
                    SpannableStringBuilder nodeContent = scNodeContentText.getContent();
                    // Iterating through the spans of the nodeContentText
                    // and adding information depending on Span class
                    // for more information on span open appropriate span class. Furthermore,
                    // other elements have different tag name than "rich_text". For those elements
                    // new (appropriate) tags will be created and added to appropriate list.
                    for (int i = 0; i < nodeContent.length(); i = next) {
                        addContent = true;
                        next = nodeContent.nextSpanTransition(i, nodeContent.length(), Object.class);
                        Object[] spans = nodeContent.getSpans(i, next, Object.class);
                        Element element = this.doc.createElement("rich_text");
                        for (Object span : spans) {
                            if (span instanceof AlignmentSpan) {
                                AlignmentSpan alignmentSpan = (AlignmentSpan) span;
                                if (alignmentSpan.getAlignment() == Layout.Alignment.ALIGN_CENTER) {
                                    element.setAttribute("justification", "center");
                                    lastFoundJustification = "center";
                                } else if (alignmentSpan.getAlignment() == Layout.Alignment.ALIGN_OPPOSITE) {
                                    element.setAttribute("justification", "right");
                                    lastFoundJustification = "right";
                                } else {
                                    element.setAttribute("justification", "left");
                                    lastFoundJustification = "left";
                                }
                            } else if (span instanceof BackgroundColorSpanCustom) {
                                BackgroundColorSpanCustom backgroundColorSpan = (BackgroundColorSpanCustom) span;
                                String backgroundColor = String.format("#%1$s", Integer.toHexString(backgroundColorSpan.getBackgroundColor()).substring(2));
                                element.setAttribute("background", backgroundColor);
                            } else if (span instanceof ClickableSpanNode) {
                                ClickableSpanNode clickableSpanNode = (ClickableSpanNode) span;
                                String attributeValue = String.format("node %1$s", clickableSpanNode.getNodeUniqueID());
                                if (clickableSpanNode.getLinkAnchorName() != null) {
                                    attributeValue = String.format("%1$s %2$s", attributeValue, clickableSpanNode.getLinkAnchorName());
                                }
                                element.setAttribute("link", attributeValue);
                            } else if (span instanceof ClickableSpanLink) {
                                ClickableSpanLink clickableSpanLink = (ClickableSpanLink) span;
                                element.setAttribute("link", String.format("%1$s %2$s", clickableSpanLink.getLinkType(), clickableSpanLink.getBase64Link()));
                            } else if (span instanceof ImageSpanFile) {
                                // Attached file
                                addContent = false;
                                ImageSpanFile imageSpanFile = (ImageSpanFile) span;
                                element = this.doc.createElement("encoded_png");
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("filename", imageSpanFile.getFilename());
                                element.setAttribute("time", String.valueOf(System.currentTimeMillis()));
                                if (imageSpanFile.isFromDatabase()) {
                                    element.setTextContent(this.getFileEncodedString(node, imageSpanFile.getOriginalOffset(), imageSpanFile.getFilename()));
                                }
                                offsetNodes.add(element);
                            } else if (span instanceof ClickableSpanFile) {
                                // Attached File text part
                                addContent = false;
                            } else if (span instanceof ForegroundColorSpan) {
                                ForegroundColorSpan foregroundColorSpan = (ForegroundColorSpan) span;
                                String backgroundColor = String.format("#%1$s", Integer.toHexString(foregroundColorSpan.getForegroundColor()).substring(2));
                                element.setAttribute("foreground", backgroundColor);
                            } else if (span instanceof ImageSpanAnchor) {
                                addContent = false;
                                element = this.doc.createElement("encoded_png");
                                ImageSpanAnchor imageSpanAnchor = (ImageSpanAnchor) span;
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("anchor", imageSpanAnchor.getAnchorName());
                                offsetNodes.add(element);
                            } else if (span instanceof ImageSpanImage) {
                                addContent = false;
                                element = this.doc.createElement("encoded_png");
                                ImageSpanImage imageSpanImage = (ImageSpanImage) span;
                                Drawable drawable = imageSpanImage.getDrawable();
                                // Hopefully it's always a Bitmap drawable, because I get it from the same source
                                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                String baseString = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setTextContent(baseString);
                                offsetNodes.add(element);
                            } else if (span instanceof ImageSpanLatex) {
                                addContent = false;
                                ImageSpanLatex imageSpanLatex = (ImageSpanLatex) span;
                                element = this.doc.createElement("encoded_png");
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("filename", "__ct_special.tex");
                                element.setTextContent(imageSpanLatex.getLatexCode());
                                offsetNodes.add(element);
                            } else if (span instanceof LeadingMarginSpan.Standard) {
                                LeadingMarginSpan.Standard leadingMarginSpan = (LeadingMarginSpan.Standard) span;
                                int indent = leadingMarginSpan.getLeadingMargin(true) / 40;
                                element.setAttribute("indent", String.valueOf(indent));
                            } else if (span instanceof TypefaceSpanCodebox) {
                                addContent = false;
                                element = this.doc.createElement("codebox");
                                TypefaceSpanCodebox typefaceSpanCodebox = (TypefaceSpanCodebox) span;
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("frame_width", String.valueOf(typefaceSpanCodebox.getFrameWidth()));
                                element.setAttribute("frame_height", String.valueOf(typefaceSpanCodebox.getFrameHeight()));
                                element.setAttribute("width_in_pixels", typefaceSpanCodebox.isWidthInPixel() ? "1" : "0");
                                element.setAttribute("syntax_highlighting", typefaceSpanCodebox.getSyntaxHighlighting());
                                element.setAttribute("highlight_brackets", typefaceSpanCodebox.isHighlightBrackets() ? "1" : "0");
                                element.setAttribute("show_line_numbers", typefaceSpanCodebox.isShowLineNumbers() ? "1" : "0");
                                element.setTextContent(nodeContent.subSequence(i, next).toString());
                                offsetNodes.add(element);
                            } else if (span instanceof RelativeSizeSpan) {
                                RelativeSizeSpan relativeSizeSpan = (RelativeSizeSpan) span;
                                float size = relativeSizeSpan.getSizeChange();
                                if (size == 1.75f) {
                                    element.setAttribute("scale", "h1");
                                } else if (size == 1.5f) {
                                    element.setAttribute("scale", "h2");
                                } else if (size == 1.25f) {
                                    element.setAttribute("scale", "h3");
                                } else if (size == 0.75f) {
                                    element.setAttribute("scale", "small");
                                }
                            } else if (span instanceof StrikethroughSpan) {
                                element.setAttribute("strikethrough", "true");
                            } else if (span instanceof StyleSpan) {
                                StyleSpan styleSpan = (StyleSpan) span;
                                if (styleSpan.getStyle() == Typeface.BOLD) {
                                    element.setAttribute("weight", "heavy");
                                } else if (styleSpan.getStyle() == Typeface.ITALIC) {
                                    element.setAttribute("style", "italic");
                                }
                            } else if (span instanceof SubscriptSpan) {
                                element.setAttribute("scale", "sub");
                            } else if (span instanceof SuperscriptSpan) {
                                element.setAttribute("scale", "sup");
                            } else if (span instanceof TypefaceSpanFamily) {
                                element.setAttribute("family", "monospace");
                            } else if (span instanceof URLSpanWebs) {
                                URLSpanWebs urlSpanWebs = (URLSpanWebs) span;
                                element.setAttribute("link", String.format("webs %1$s", urlSpanWebs.getURL()));
                            } else if (span instanceof UnderlineSpan) {
                                element.setAttribute("underline", "single");
                            }
                        }
                        if (addContent) {
                            element.setTextContent(nodeContent.subSequence(i, next).toString());
                            // If span content is being added to the element it's length
                            // has to be added to the length of the node
                            currentPartContentLength += (next - i);
                            normalNodes.add(element);
                        }
                    }
                    totalContentLength += currentPartContentLength;
                    currentPartContentLength = 0;
                } else {
                    ScNodeContentTable scNodeContentTable = (ScNodeContentTable) scNodeContent;
                    Element tableElement = this.doc.createElement("table");
                    tableElement.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                    tableElement.setAttribute("justification", scNodeContentTable.getJustification());
                    tableElement.setAttribute("col_min", String.valueOf(scNodeContentTable.getColMin()));
                    tableElement.setAttribute("col_max", String.valueOf(scNodeContentTable.getColMax()));
                    for (CharSequence[] row : scNodeContentTable.getContent()) {
                        Element rowElement = this.doc.createElement("row");
                        for (CharSequence cell : row) {
                            Element cellElement = this.doc.createElement("cell");
                            cellElement.setTextContent(cell.toString());
                            rowElement.appendChild(cellElement);
                        }
                        tableElement.appendChild(rowElement);
                    }
                    offsetNodes.add(tableElement);
                }
            }
            this.deleteNodeContent(node);
            for (Element element : normalNodes) {
                node.appendChild(element);
            }
            // Extra char offset. Because when I created nodeContent I had to take -1 off the totalCharOffset
            // I have to add it when saving content to tags. Otherwise content in the CherryTree (and SourCherry)
            // will look differently every time. In the end it will cause a crash.
            int extraCharOffset = 0;
            Element collectedCodebox = null;
            for (int i = 0; i < offsetNodes.size(); i++) {
                if (offsetNodes.get(i).getNodeName().equals("codebox")) {
                    // When inserting text (user typing) inside QuoteSpan EditText for some reason will create
                    // multiple spans following one another instead of one long span. That will create multiple
                    // Codebox spans. Recreating codebox from multiple spans can produce unexpected results.
                    // To fix it all spans that have the same offset have to be merged into one.
                    if (collectedCodebox == null) {
                        // First time encountering codebox after writing another element
                        collectedCodebox = offsetNodes.get(i);
                    } else {
                        // Multiple consecutive codeboxes
                        Element element = offsetNodes.get(i);
                        if (Integer.parseInt(collectedCodebox.getAttribute("char_offset")) == Integer.parseInt(element.getAttribute("char_offset"))) {
                            // If offset is the same as in the previous codebox - merge content
                            collectedCodebox.setTextContent(collectedCodebox.getTextContent() + element.getTextContent());
                        } else {
                            // Two consecutive codebox elements however belonging to separate codeboxes
                            collectedCodebox.setAttribute("char_offset", String.valueOf(Integer.parseInt(collectedCodebox.getAttribute("char_offset")) + extraCharOffset));
                            extraCharOffset++;
                            node.appendChild(collectedCodebox);
                            collectedCodebox = element;
                        }
                    }
                } else {
                    // Other type offset element
                    if (collectedCodebox != null) {
                        // Previous element was a codebox element
                        collectedCodebox.setAttribute("char_offset", String.valueOf(Integer.parseInt(collectedCodebox.getAttribute("char_offset")) + extraCharOffset));
                        node.appendChild(collectedCodebox);
                        extraCharOffset++;
                        collectedCodebox = null;
                    }
                    Element element = offsetNodes.get(i);
                    element.setAttribute("char_offset", String.valueOf(Integer.parseInt(element.getAttribute("char_offset")) + extraCharOffset));
                    node.appendChild(element);
                    extraCharOffset++;
                }
            }
            if (collectedCodebox != null) {
                // Could be that codebox was the last element in offsetNodes list
                collectedCodebox.setAttribute("char_offset", String.valueOf(Integer.parseInt(collectedCodebox.getAttribute("char_offset")) + extraCharOffset));
                node.appendChild(collectedCodebox);
                collectedCodebox = null;
            }
        } else {
            ScNodeContentText scNodeContentText = (ScNodeContentText) this.mainViewModel.getNodeContent().get(0);
            SpannableStringBuilder nodeContent = scNodeContentText.getContent();
            Element element = this.doc.createElement("rich_text");
            this.deleteNodeContent(node);
            element.setTextContent(nodeContent.toString());
            node.appendChild(element);
        }
        node.getAttributes().getNamedItem("ts_lastsave").setTextContent(String.valueOf(System.currentTimeMillis()));
        this.writeIntoDatabase();
    }

    @Override
    public ArrayList<ScSearchNode> search(Boolean noSearch, String query) {
        NodeList nodeList = this.doc.getFirstChild().getChildNodes();
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        if (noSearch) {
            // If user marked that search should skip search "excluded" nodes
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getNodeName().equals("node")) {
                    // If node is a "node" and not some other tag
                    boolean hasSubnodes = this.hasSubnodes(nodeList.item(i));
                    if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                        // if user haven't marked to skip current node - searches through its content
                        boolean isParent = false;
                        boolean isSubnode = true;
                        if (hasSubnodes) {
                            isParent = true;
                            isSubnode = false;
                        }
                        ScSearchNode result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                        if (result != null) {
                            searchResult.add(result);
                        }
                    }
                    if (hasSubnodes && nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                        // if user haven't selected not to search subnodes of current node
                        searchResult.addAll(this.searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                    }
                }
            }
            return searchResult;
        } else {
            for (int i = 0; i < nodeList.getLength(); i++) {
                boolean hasSubnodes = hasSubnodes(nodeList.item(i));
                if (nodeList.item(i).getNodeName().equals("node")) {
                    boolean isParent  = false;
                    boolean isSubnode  = true;
                    if (hasSubnodes) {
                        isParent = true;
                        isSubnode = false;
                    }
                    ScSearchNode result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes) {
                    // If node has subnodes
                    searchResult.addAll(this.searchAllNodes(query, nodeList.item(i).getChildNodes()));
                }
            }
        }
        return searchResult;
    }

    /**
     * Searches through all nodes without skipping marked to exclude nodes
     * @param query string to search for
     * @param nodeList list of nodes to search through
     * @return ArrayList of search result objects
     */
    private ArrayList<ScSearchNode> searchAllNodes(String query, NodeList nodeList) {
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            boolean hasSubnodes = hasSubnodes(nodeList.item(i));
            if (nodeList.item(i).getNodeName().equals("node")) {
                // If node is a "node" and not some other tag
                boolean isParent;
                boolean isSubnode;
                if (hasSubnodes) {
                    isParent = true;
                    isSubnode = false;
                } else {
                    isParent = false;
                    isSubnode = true;
                }
                ScSearchNode result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
            }
            if (hasSubnodes) {
                // If node has subnodes
                searchResult.addAll(this.searchAllNodes(query, nodeList.item(i).getChildNodes()));
            }
        }
        return searchResult;
    }

    /**
     * Searches through nodes skipping marked to exclude
     * @param query string to search for
     * @param nodeList list of nodes to search through
     * @return ArrayList of search result objects
     */
    private ArrayList<ScSearchNode> searchNodesSkippingExcluded(String query, NodeList nodeList) {
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals("node")) {
                // If node is a "node" and not some other tag
                boolean hasSubnodes = this.hasSubnodes(nodeList.item(i));

                if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                    // If user haven't marked to skip current node - searches through its content
                    boolean isParent;
                    boolean isSubnode;
                    if (hasSubnodes) {
                        isParent = true;
                        isSubnode = false;
                    } else {
                        isParent = false;
                        isSubnode = true;
                    }
                    ScSearchNode result = this.findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes && nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                    // If node has subnodes and user haven't selected not to search subnodes of current node
                    searchResult.addAll(this.searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                }
            }
        }
        return searchResult;
    }

    /**
     * Searches through node's content
     * @param node node to search in
     * @param query string to search for
     * @param hasSubnodes true if node has subnodes, else - false
     * @param isParent true if node is a parent node, else - false
     * @param isSubnode true if node is a subnode, else - false
     * @return search result object or null if nothing was found
     */
    private ScSearchNode findInNode(Node node, String query, boolean hasSubnodes, boolean isParent, boolean isSubnode) {
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
        int resultCount = 0;
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
                if (resultCount < 1) {
                    // If it's first match
                    // Settings node name and unique_id values that they could be returned with result
                    nodeName = node.getAttributes().getNamedItem("name").getNodeValue();
                    nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                }

                if (resultCount < 3 ) {
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

                resultCount++;
                index += queryLength; // moving search start to the end of the last position that search query was found
            }
        }

        if (nodeName != null) {
            // if node name isn't null that means match for a query was found
            return new ScSearchNode(nodeUniqueID, nodeName, isParent, hasSubnodes, isSubnode, query, resultCount, samples.toString());
        } else {
            return null;
        }
    }

    /**
     * Finds and returns Base64 encoded String of the file in the provided node
     * @param node node to search file for
     * @param offset file's offset
     * @param filename file's filename
     * @return file data in Base64 String
     */
    private String getFileEncodedString(Node node, String offset, String filename) {
        NodeList nodeList = node.getChildNodes();
        Node fileNode;
        for (int i = 0; i < nodeList.getLength(); i++) {
            fileNode = nodeList.item(i);
            if (fileNode.getNodeName().equals("encoded_png")) {
                if (fileNode.getAttributes().getNamedItem("char_offset").getTextContent().equals(offset) && fileNode.getAttributes().getNamedItem("filename").getTextContent().equals(filename)) {
                    return fileNode.getTextContent();
                }
            }
        }
        return "";
    }
}
