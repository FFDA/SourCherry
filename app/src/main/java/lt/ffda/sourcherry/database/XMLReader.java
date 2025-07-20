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
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import lt.ffda.sourcherry.spans.StyleSpanBold;
import lt.ffda.sourcherry.spans.StyleSpanItalic;
import lt.ffda.sourcherry.spans.TypefaceSpanCodebox;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import lt.ffda.sourcherry.utils.DatabaseType;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class XMLReader extends DatabaseReader {
    private final Context context;
    private final String databaseUri;
    private final Document doc;
    private final Handler handler;
    private final MainViewModel mainViewModel;

    /**
     * Class that opens databases based on XML file. Provides all functions necessary to read and edit
     * the data in the database.
     * @param databaseUri uri of the XML database converted to String. This uri will be used to save changes made by the user
     * @param is InputStream of the database to open Document file
     * @param context application context to display toast messages, get resources, handle clicks
     * @param handler to run methods on main thread
     * @param mainViewModel ViewModel of MainView activity to store data
     * @throws ParserConfigurationException Indicates a serious configuration error.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     * @throws SAXException Encapsulate a general SAX error or warning.
     */
    public XMLReader(String databaseUri, InputStream is, Context context, Handler handler, MainViewModel mainViewModel) throws ParserConfigurationException, IOException, SAXException {
        // Creates a document that can be used to read tags with provided InputStream
        this.databaseUri = databaseUri;
        this.context = context;
        this.handler = handler;
        this.mainViewModel = mainViewModel;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        this.doc = db.parse(is);
    }

    @Override
    public void addNodeToBookmarks(String nodeUniqueID) {
        NodeList bookmarkTag = doc.getElementsByTagName("bookmarks");
        Node bookmarksNode = bookmarkTag.item(0);
        List<Integer> bmkrs;
        Node bookmarkList = bookmarksNode.getAttributes().getNamedItem("list");
        if (bookmarkList.getNodeValue().isEmpty()) {
            bmkrs = new ArrayList<>();
        } else {
            bmkrs = Arrays.stream(bookmarksNode.getAttributes().getNamedItem("list").getNodeValue().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        bmkrs.add(Integer.parseInt(nodeUniqueID));
        Collections.sort(bmkrs);
        bookmarksNode.getAttributes().getNamedItem("list").setTextContent(bmkrs.stream().map(String::valueOf).collect(Collectors.joining(",")));
        writeIntoDatabase();
    }

    /**
     * Checks if node is a subnode if another node
     * Not really sure if it does not return false positives
     * However all my tests worked
     * @param targetNodeUniqueID unique ID of the node that needs to be check if it's a parent node
     * @param destinationNodeUniqueID unique ID of the node that has to be check if it's a child
     * @return true - if target node is a parent of destination node
     */
    private boolean areNodesRelated(String targetNodeUniqueID, String destinationNodeUniqueID) {
        ArrayList<String> heredity = new ArrayList<>();

        NodeList nodeList = doc.getElementsByTagName("node");
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

    /**
     * Copies node content (but no children nodes) and all attributes from one CherryTree XML
     * document Node to another. Skips unique_id attribute and sets master_id attribute to 0.
     * @param source source node
     * @param destination destination node
     */
    private void cloneNodeAndAttributes(Node source, Node destination) {
        NamedNodeMap sourceAttributes = source.getAttributes();
        NamedNodeMap destAttributes = destination.getAttributes();
        for (int i = 0; i < sourceAttributes.getLength(); i++) {
            Node att = sourceAttributes.item(i);
            if (att.getNodeName().equals("unique_id")) {
                continue;
            } else if (att.getNodeName().equals("master_id")) {
                Node attribute = att.cloneNode(false);
                attribute.setNodeValue("0");
                destAttributes.setNamedItem(attribute);
            } else {
                destAttributes.setNamedItem(att.cloneNode(false));
            }
        }
        Node inserBefore = destination.getFirstChild();
        NodeList sourceNodeList = source.getChildNodes();
        for (int i = sourceNodeList.getLength() - 1; i >= 0; i--) {
            Node currentNode = sourceNodeList.item(i);
            String nodeName = currentNode.getNodeName();
            if (!nodeName.equals("node") && !nodeName.equals("#text")) {
                currentNode = currentNode.cloneNode(true);
                destination.insertBefore(currentNode, inserBefore);
                inserBefore = currentNode;
            }
        }
    }

    /**
     * Recursively scans through all the nodes in NodeList to collect the uniqueNodeIDs. Adds them
     * to provided String list
     * @param uniqueIdList String list to add the found nodeUniqueIDs
     * @param nodeList NodeList to scan recursively
     */
    private void collectIDs(List<String> uniqueIdList, NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals("node")) {
                NamedNodeMap attr = nodeList.item(i).getAttributes();
                uniqueIdList.add(attr.getNamedItem("unique_id").getNodeValue());
                collectIDs(uniqueIdList, nodeList.item(i).getChildNodes());
            }
        }
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
     * Converts node from XML node object to ScNode object
     * @param node node to converte
     * @return converted node that can be used in SourCherry menu
     */
    private ScNode convertNodeToScNode(Node node) {
        boolean hasSubnodes = hasSubnodes(node);
        String nodeUniqueId = node.getAttributes().getNamedItem("unique_id").getNodeValue();
        String masterId;
        Node masterIdNode = node.getAttributes().getNamedItem("master_id");
        if (masterIdNode == null) {
            masterId = "0";
        } else {
            masterId = masterIdNode.getNodeValue();
        }
        if (!"0".equals(masterId)) {
            node = findNode(masterId);
        }
        NamedNodeMap attr = node.getAttributes();
        String nameValue = attr.getNamedItem("name").getNodeValue();
        boolean isRichText = attr.getNamedItem("prog_lang").getNodeValue().equals("custom-colors");
        boolean isBold = attr.getNamedItem("is_bold").getNodeValue().equals("0");
        String foregroundColor = attr.getNamedItem("foreground").getNodeValue();
        int iconId = Integer.parseInt(attr.getNamedItem("custom_icon_id").getNodeValue());
        boolean isReadOnly = attr.getNamedItem("readonly").getNodeValue().equals("0");
        if (hasSubnodes) {
            // if node has subnodes, then it has to be opened as a parent node and displayed as such
            return new ScNode(nodeUniqueId, masterId, nameValue, true, hasSubnodes, false, isRichText, isBold, foregroundColor, iconId, isReadOnly);
        } else {
            // If node doesn't have subnodes, then it has to be opened as subnode of some other node
            return new ScNode(nodeUniqueId, masterId, nameValue, false, hasSubnodes, true, isRichText, isBold, foregroundColor, iconId, isReadOnly);
        }
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
                StringBuilder tableContent = convertTableNodeContentToPlainText(node);
                nodeContent.insert(charOffset, tableContent);
                totalCharOffset += tableContent.length() - 1;
            } else if (node.getNodeName().equals("encoded_png")) {
                if (node.getAttributes().getNamedItem("filename") != null) {
                    if (node.getAttributes().getNamedItem("filename").getNodeValue().equals("__ct_special.tex")) {
                        // For latex boxes
                        int charOffset = getCharOffset(node) + totalCharOffset;
                        StringBuilder latexContent = convertLatexToPlainText(node);
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
                StringBuilder codeboxContent = convertCodeboxToPlainText(node);
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
                    tableContent.append(convertTableRowToPlainText(node));
                } else {
                    tableContent.insert(0, convertTableRowToPlainText(node));
                }
                tableRowCount--;
            }
        }
        tableContent.insert(0, "\n");
        return tableContent;
    }

    @Override
    public ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh) {
        Node node;
        if (nodeUniqueID.equals("0")) {
            // User chose to create the node in main menu
            node = doc.getElementsByTagName("cherrytree").item(0);
        } else {
            node = findNode(nodeUniqueID);
        }
        String newNodeUniqueID = String.valueOf(getNodeMaxID() + 1);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // Creating new node with all necessary tags
        Element newNode = doc.createElement("node");
        newNode.setAttribute("name", name);
        newNode.setAttribute("unique_id", newNodeUniqueID);
        newNode.setAttribute("master_id", "0");
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

        boolean isSubnode = true;
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
                isSubnode = false;
            }
        } else {
            // As a subnode of selected node
            node.appendChild(newNode);
        }
        writeIntoDatabase();
        return new ScNode(newNodeUniqueID, "0", name,false, false, isSubnode, progLang.equals("custom-colors"), false, "", 0, false);
    }

    @Override
    public void deleteNode(String nodeUniqueID) {
        Node nodeToDelete = findNode(nodeUniqueID);
        // Collecting all nodeUniqueIDs that will be removed
        List<String> uniqueIdList = new ArrayList<>();
        uniqueIdList.add(nodeToDelete.getAttributes().getNamedItem("unique_id").getNodeValue());
        if (nodeToDelete.getAttributes().getNamedItem("master_id") != null
                && !nodeToDelete.getAttributes().getNamedItem("master_id").getNodeValue().equals("0")) {
        }
        NodeList deletedNodeChildren = nodeToDelete.getChildNodes();
        for (int i = 0; i < deletedNodeChildren.getLength(); i++) {
            if (deletedNodeChildren.item(i).getNodeName().equals("node")) {
                NamedNodeMap attr = deletedNodeChildren.item(i).getAttributes();
                uniqueIdList.add(attr.getNamedItem("unique_id").getNodeValue());
                collectIDs(uniqueIdList, deletedNodeChildren.item(i).getChildNodes());
            }
        }
        // Checking if master node will be deleted with childrend of deleted node
        for (String uniqueId: uniqueIdList) {
            List<String> sharedNodesIds = getSharedNodesIds(uniqueId);
            if (!sharedNodesIds.isEmpty()) {
                // masterNode will be deleted
                for (String sharedId: sharedNodesIds) {
                    // Looking for first sharedNode that will not be deleted with the rest of the nodes
                    if (!uniqueIdList.contains(sharedId)) {
                        // Copying content to the new masterNode
                        Node oldMasterNode = findNode(uniqueId);
                        Node newMasterNode = findNode(sharedId);
                        cloneNodeAndAttributes(oldMasterNode, newMasterNode);
                        for (int i = 1; i < sharedNodesIds.size(); i++) {
                            if (!uniqueIdList.contains(sharedNodesIds.get(i))) {
                                // Changes master_id to the to the newMasterNode id if node will
                                // not be deleted
                                Element node = (Element) findNode(sharedNodesIds.get(i));
                                node.setAttribute("master_id", sharedId);
                            }
                        }
                        break;
                    }
                }
            }
        }
        removeNodesFromBookmarks(uniqueIdList);
        nodeToDelete.getParentNode().removeChild(nodeToDelete);
        writeIntoDatabase();
    }

    /**
     * Removes all rich_text tags from the node
     * Used to prepare node for conversion from rich-text to plain-text
     * @param node node from which to delete all the content
     */
    private void deleteNodeContent(Node node) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (!currentNode.getNodeName().equals("node")) {
                node.removeChild(currentNode);
            }
        }
    }

    @Override
    public void displayToast(String message) {
        // Displays a toast on main thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
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
        String nodeMasterID = null;
        boolean isRichText = false;
        boolean isBold = false;
        String foregroundColor = "";
        int iconId = 0;
        boolean isReadOnly = false;
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
                    NamedNodeMap attr = node.getAttributes();
                    nodeName = attr.getNamedItem("name").getNodeValue();
                    nodeUniqueID = attr.getNamedItem("unique_id").getNodeValue();
                    nodeMasterID = attr.getNamedItem("master_id") != null ? attr.getNamedItem("master_id").getNodeValue() : "0";
                    isRichText = attr.getNamedItem("prog_lang").getNodeValue().equals("custom-colors");
                    isBold = attr.getNamedItem("is_bold").getNodeValue().equals("0");
                    foregroundColor = attr.getNamedItem("foreground").getNodeValue();
                    iconId = Integer.parseInt(attr.getNamedItem("custom_icon_id").getNodeValue());
                    isReadOnly = attr.getNamedItem("readonly").getNodeValue().equals("0");
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
            return new ScSearchNode(nodeUniqueID, nodeMasterID, nodeName, isParent, hasSubnodes, isSubnode, isRichText, isBold, foregroundColor, iconId, isReadOnly, query, resultCount, samples.toString());
        } else {
            return null;
        }
    }

    /**
     * Searches through database for the node with unique ID
     * @param nodeUniqueID node unique ID to search for
     * @return found Node object or null
     */
    private Node findNode(String nodeUniqueID) {
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public ArrayList<ScNode> getAllNodes(boolean noSearch) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            NodeList nodeList = doc.getFirstChild().getChildNodes();
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
            return returnSubnodeArrayList(doc.getElementsByTagName("node"), false);
        }
    }

    @Override
    public ArrayList<ScNode> getBookmarkedNodes() {
        ArrayList<ScNode> nodes = new ArrayList<>();
        NodeList nodeBookmarkNode = doc.getElementsByTagName("bookmarks");
        List<String> nodeUniqueIDArray = Arrays.asList(nodeBookmarkNode.item(0).getAttributes().getNamedItem("list").getNodeValue().split(","));
        NodeList nodeList = doc.getElementsByTagName("node");

        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (nodeUniqueIDArray.contains(node.getAttributes().getNamedItem("unique_id").getNodeValue())) {
                ScNode scNode = convertNodeToScNode(node);
                // None of them have to be indented
                scNode.setSubnode(false);
                nodes.add(scNode);
            }
        }
        if (nodes.isEmpty()) {
            return null;
        } else {
            Collections.sort(nodes);
            return nodes;
        }
    }

    /**
     * Returns character offset value that is used in codebox and encoded_png tags
     * It is needed to add text in the correct location
     * One needs to -1 from the value to make it work
     * I don't have any idea why
     * @param node Node object to extract it's character offset
     * @return offset of the node content
     */
    private int getCharOffset(Node node) {
        Element element = (Element) node;
        return Integer.parseInt(element.getAttribute("char_offset"));
    }

    @Override
    public int getChildrenNodeCount(String nodeUniqueID) {
        NodeList nodeList = findNode(nodeUniqueID).getChildNodes();
        int count = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals("node")) {
                count++;
            }
        }
        return count;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.XML;
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

    @Override
    public InputStream getFileInputStream(String nodeUniqueID, String filename, String time, String control) {
        // Returns byte array (stream) to be written to file or opened
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            return null;
        }
        NodeList encodedpngNodeList = ((Element) node).getElementsByTagName("encoded_png"); // Gets all nodes with tag <encoded_png> (images and files)
        for (int x = 0; x < encodedpngNodeList.getLength(); x++) {
            Node currentNode = encodedpngNodeList.item(x);
            if (currentNode.getAttributes().getNamedItem("filename") != null) { // Checks if node has the attribute, otherwise it's an image
                if (currentNode.getAttributes().getNamedItem("filename").getNodeValue().equals(filename)) { // If filename matches the one provided
                    if (currentNode.getAttributes().getNamedItem("time").getNodeValue().equals(time) && currentNode.getAttributes().getNamedItem("char_offset").getNodeValue().equals(control)) { // Checks if timestamp and offset matches the file
                        // Will crash the app if file is big enough (11MB on my phone). No way to catch it as exception.
                        return new ByteArrayInputStream(Base64.decode(currentNode.getTextContent(), Base64.DEFAULT));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public InputStream getImageInputStream(String nodeUniqueID, String control) {
        // Returns image byte array to be displayed in ImageViewFragment because some of the images are too big to pass in a bundle
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            return null;
        }
        NodeList encodedpngNodeList = ((Element) node).getElementsByTagName("encoded_png"); // Gets all nodes with tag <encoded_png> (images and files)
        for (int x = 0; x < encodedpngNodeList.getLength(); x++) {
            Node currentNode = encodedpngNodeList.item(x);
            if (currentNode.getAttributes().getNamedItem("filename") == null) { // Checks if node has the attribute "filename". If it does - it's a file
                if (currentNode.getAttributes().getNamedItem("char_offset").getNodeValue().equals(control)) { // If control matches the one provided
                    return new ByteArrayInputStream(Base64.decode(currentNode.getTextContent(), Base64.DEFAULT));
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<ScNode> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts
        NodeList nodeList = doc.getElementsByTagName("cherrytree"); // There is only one this type of tag in the database
        NodeList mainNodeList = nodeList.item(0).getChildNodes(); // So selecting all children of the first node is always safe
        return returnSubnodeArrayList(mainNodeList, false);
    }

    @Override
    public ArrayList<ScNode> getMenu(String nodeUniqueID) {
        ArrayList<ScNode> nodes;
        Node node = findNode(nodeUniqueID);
        NodeList childNodeList = node.getChildNodes();
        nodes = returnSubnodeArrayList(childNodeList, true);
        ScNode parentNode = convertNodeToScNode(node);
        nodes.add(0, parentNode);
        return nodes;
    }

    @Override
    public int getNodeMaxID() {
        int maxID = -1;
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            int foundNodeUniqueID = Integer.parseInt(node.getAttributes().getNamedItem("unique_id").getNodeValue());
            if (foundNodeUniqueID > maxID) {
                maxID = foundNodeUniqueID;
            }
        }
        return maxID;
    }

    @Override
    public ScNodeProperties getNodeProperties(String nodeUniqueID) {
        Node node = findNode(nodeUniqueID);
        String masterId = node.getAttributes().getNamedItem("master_id").getNodeValue();
        if (masterId != null && !"0".equals(masterId)) {
            node = findNode(masterId);
        }
        NamedNodeMap properties = node.getAttributes();
        String name = properties.getNamedItem("name").getNodeValue();
        String progLang = properties.getNamedItem("prog_lang").getNodeValue();
        byte noSearchMe = Byte.parseByte(properties.getNamedItem("nosearch_me").getNodeValue());
        byte noSearchCh = Byte.parseByte(properties.getNamedItem("nosearch_ch").getNodeValue());
        return new ScNodeProperties(nodeUniqueID, name, progLang, noSearchMe, noSearchCh, getSharedNodesGroup(nodeUniqueID));
    }

    @Override
    public String getParentNodeUniqueID(String nodeUniqueID) {
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            return null;
        }
        Node parentNode = node.getParentNode();
        if (parentNode == null || parentNode.getNodeName().equals("cherrytree")) {
            return null;
        } else {
            return parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        }
    }

    @Override
    public ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID) {
        ArrayList<ScNode> nodes = null;
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            return nodes;
        }
        Node parentNode = node.getParentNode();
        if (parentNode == null) {
            return nodes;
        } else if (parentNode.getNodeName().equals("cherrytree")) {
            nodes = getMainNodes();
        } else {
            nodes = returnSubnodeArrayList(parentNode.getChildNodes(), true);
            nodes.add(0, convertNodeToScNode(parentNode));
        }
        return nodes;
    }

    @Override
    public String getSharedNodesGroup(String nodeUniqueID) {
        Node node = findNode(nodeUniqueID);
        String masterId = node.getAttributes().getNamedItem("master_id").getNodeValue();
        List<String> sharedNodesGroup;
        if (masterId != null && !"0".equals(masterId)) {
            sharedNodesGroup = getSharedNodesIds(masterId);
            sharedNodesGroup.add(masterId);
        } else {
            sharedNodesGroup = getSharedNodesIds(nodeUniqueID);
            sharedNodesGroup.add(nodeUniqueID);
        }
        if (sharedNodesGroup.size() > 1 ) {
            return sharedNodesGroup.stream()
                    .mapToLong(Long::parseLong)
                    .sorted()
                    .mapToObj(Long::toString)
                    .collect(Collectors.joining(", "));
        } else {
            return null;
        }
    }

    @Override
    public List<String> getSharedNodesIds(String nodeUniqueID) {
        List<String> sharedNodes = new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            NamedNodeMap nodeAttributes = nodeList.item(i).getAttributes();
            if (nodeAttributes.getNamedItem("master_id") != null &&
                    nodeAttributes.getNamedItem("master_id").getNodeValue().equals(nodeUniqueID)) {
                sharedNodes.add(nodeAttributes.getNamedItem("unique_id").getNodeValue());
            }
        }
        return sharedNodes;
    }

    @Override
    public ScNode getSingleMenuItem(String nodeUniqueID) {
        if (nodeUniqueID == null) {
            return null;
        }
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            return null;
        }
        return convertNodeToScNode(node);
    }

    /**
     * Returns embedded table dimensions in the node
     * Used to set min and max width for the table cell
     * @param node table Node object
     * @return CharSequence[] {colMax, colMin} of table dimensions
     */
    private int[] getTableMinMax(Node node) {
        Element el = (Element) node;
        int colMin = Integer.parseInt(el.getAttribute("col_min"));
        int colMax = Integer.parseInt(el.getAttribute("col_max"));
        return new int[] {colMin, colMax};
    }

    /**
     * Checks if provided Node object has a subnode(s)
     * @param node Node object to check if it has subnodes
     * @return true if node is a subnode
     */
    private boolean hasSubnodes(Node node) {
        // Checks if provided node has nested "node" tag
        NodeList subNodes = node.getChildNodes();
        for (int i = 0; i < subNodes.getLength(); i++) {
            if (subNodes.item(i).getNodeName().equals("node")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNodeBookmarked(String nodeUniqueID) {
        NodeList bookmarkTag = doc.getElementsByTagName("bookmarks");
        if (bookmarkTag.getLength() < 1) {
            return false;
        }
        List<String> bookmarks = Arrays.asList(bookmarkTag.item(0).getAttributes().getNamedItem("list").getNodeValue().split(","));
        return bookmarks.contains(nodeUniqueID);
    }

    @Override
    public void loadNodeContent(String nodeUniqueID) {
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary for text, codebox, image formatting
        ArrayList<ScNodeContentTable> nodeTables = new ArrayList<>(); // Temporary for table storage
        ArrayList<Integer> nodeTableCharOffsets = new ArrayList<>();
        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        ////
        // prog_lang attribute is the same as syntax in SQL database
        // it is used to set formatting for the node and separate between node types
        // The same attribute is used for codeboxes
        Node node = findNode(nodeUniqueID);
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
                        SpannableStringBuilder codeboxText = makeFormattedCodeboxSpan(currentNode);
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
                        byte lightInterface = 0;
                        if (!((Element) currentNode).getAttribute("is_light").equals("")) {
                            lightInterface = Byte.parseByte(((Element) currentNode).getAttribute("is_light"));
                        }
                        NodeList tableRowsNodes = ((Element) currentNode).getElementsByTagName("row"); // All the rows of the table. There are empty text nodes that has to be filtered out (or only row nodes selected this way)
                        currentTableContent.add(getTableRow(tableRowsNodes.item(tableRowsNodes.getLength() - 1)));
                        for (int row = 0; row < tableRowsNodes.getLength() - 1; row++) {
                            currentTableContent.add(getTableRow(tableRowsNodes.item(row)));
                        }
                        ScNodeContentTable scNodeContentTable = new ScNodeContentTable((byte) 1, currentTableContent, cellMinMax[0], cellMinMax[1], lightInterface, ((Element) currentNode).getAttribute("justification"), ((Element) currentNode).getAttribute("col_widths"));
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
            nodeContentStringBuilder.append(makeFormattedCodeNodeSpan(node));
        }

        int subStringStart = 0; // Holds start from where SpannableStringBuilder has to be split from
        if (!nodeTables.isEmpty()) {
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
        mainViewModel.getNodeContent().postValue(nodeContent);
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
                ((MainView) context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
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

    /**
     * Creates a clickable span that initiates a context to open/save attached file
     * Files are decoded from Base64 string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that contains filename and datetime parameters
     * @param nodeUniqueID unique ID of the node to which file was attached to
     * @return SpannableStringBuilder that has spans with image and filename
     */
    private SpannableStringBuilder makeAttachedFileSpan(Node node, String nodeUniqueID) {
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
            ((MainView) context).saveOpenFile(nodeUniqueID, attachedFileFilename, time, offset);
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
    public ImageSpan makeBrokenImageSpan(int type) {
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
    public ClickableSpanLink makeFileFolderLinkSpan(String type, String base64Filename) {
        // Creates and returns a span for a link to external file or folder
        // When user clicks on the link snackbar displays a path to the file that was saved in the original system
        ClickableSpanLink clickableSpanLink = new ClickableSpanLink() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                ((MainView) context).fileFolderLinkFilepath(new String(Base64.decode(base64Filename, Base64.DEFAULT)));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
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

    /**
     * Creates SpannableStringBuilder with the content of the CodeNode
     * CodeNode is just a CodeBox that do not have height and width (dimensions)
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that contains content of the node
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    private SpannableStringBuilder makeFormattedCodeNodeSpan(Node node) {
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
            LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(context.getColor(R.color.codebox_background));
            formattedCodeNode.setSpan(lbs, 0, formattedCodeNode.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedCodeNode;
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
    private SpannableStringBuilder makeFormattedCodeboxSpan(Node node) {
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
                LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(context.getColor(R.color.codebox_background));
                formattedCodebox.setSpan(lbs, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } else {
            formattedCodebox.setSpan(typefaceSpanCodebox, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            BackgroundColorSpan bcs = new BackgroundColorSpan(context.getColor(R.color.codebox_background));
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
     * Creates a SpannableStringBuilder with image in it
     * Image is created from Base64 string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that has Image embedded in it
     * @param nodeUniqueID unique ID of the node that has image embedded in it
     * @param imageOffset image offset that can be extracted from the taf of the node
     * @return SpannableStringBuilder that has spans with image in them
     */
    private SpannableStringBuilder makeImageSpan(Node node, String nodeUniqueID, String imageOffset) {
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
            if (image.getIntrinsicWidth() > width - 10) {
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
            imageSpanImage = (ImageSpanImage) makeBrokenImageSpan(0);
            formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            displayToast(context.getString(R.string.toast_error_failed_to_load_image));
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
    private SpannableStringBuilder makeLatexImageSpan(Node node) {
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
                    ((MainView) context).openImageView(latexString);
                }
            };
            formattedLatexImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken latex image span to display in node content
            imageSpanLatex = (ImageSpanLatex) makeBrokenImageSpan(1);
            formattedLatexImage.setSpan(imageSpanLatex, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            displayToast(context.getString(R.string.toast_error_failed_to_compile_latex));
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

    @Override
    public boolean moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        if (areNodesRelated(targetNodeUniqueID, destinationNodeUniqueID)) {
            displayToast(context.getString(R.string.toast_error_new_parent_cant_be_one_of_its_children));
            return false;
        } else {
            Node targetNode = null;
            Node destinationNode = null;
            // User chose to move the node to main menu
            if (destinationNodeUniqueID.equals("0")) {
                NodeList nodeList = doc.getElementsByTagName("cherrytree");
                destinationNode = nodeList.item(0);
            }
            NodeList nodeList = doc.getElementsByTagName("node");
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
                displayToast(context.getString(R.string.toast_error_failed_to_move_node));
                return false;
            }
            Node parentNodeUniqueID = targetNode.getParentNode().getAttributes().getNamedItem("unique_id");
            if (parentNodeUniqueID != null && parentNodeUniqueID.getNodeValue().equals(destinationNodeUniqueID)) {
                displayToast(context.getString(R.string.toast_error_failed_to_move_node));
                return false;
            }
            destinationNode.appendChild(targetNode);
            writeIntoDatabase();
            return true;
        }
    }

    @Override
    public void removeNodeFromBookmarks(String nodeUniqueID) {
        NodeList bookmarkTag = doc.getElementsByTagName("bookmarks");
        Node bookmarksNode = bookmarkTag.item(0);
        ArrayList<String> bookmarks = new ArrayList<>(Arrays.asList(bookmarksNode.getAttributes().getNamedItem("list").getNodeValue().split(",")));
        bookmarks.remove(nodeUniqueID);
        bookmarksNode.getAttributes().getNamedItem("list").setTextContent(String.join(",", bookmarks));
        writeIntoDatabase();
    }

    /**
     * Removes all unique ID of the nodes in the list from the bookmarks
     * @param nodeUniqueIDs list of node unique IDs to remove from bookmarks
     */
    private void removeNodesFromBookmarks(List<String> nodeUniqueIDs) {
        NodeList bookmarkTag = doc.getElementsByTagName("bookmarks");
        Node bookmarksNode = bookmarkTag.item(0);
        ArrayList<String> bookmarks = new ArrayList<>(Arrays.asList(bookmarksNode.getAttributes().getNamedItem("list").getNodeValue().split(",")));
        for (String nodeUniqueID : nodeUniqueIDs) {
            bookmarks.remove(nodeUniqueID);
        }
        bookmarksNode.getAttributes().getNamedItem("list").setTextContent(String.join(",", bookmarks));
    }

    /**
     * Used during creation of the all the node in the document for drawer menu search/filter function
     * For that reason created node is never a parent node or a subnode
     * @param node node to collect information from
     * @return ScNode object for filter nodes function
     */
    private ScNode returnSearchMenuItem(Node node) {
        ScNode scNode = convertNodeToScNode(node);
        scNode.setParent(false);
        scNode.setSubnode(false);
        return scNode;
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
    private ArrayList<ScNode> returnSubnodeArrayList(NodeList nodeList, boolean isSubnode) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        for (int i=0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("node")) {
                ScNode scNode = convertNodeToScNode(node);
                // There is only one parent Node and its added manually in getSubNodes()
                scNode.setParent(false);
                scNode.setSubnode(isSubnode);
                nodes.add(scNode);
            }
        }
        return nodes;
    }

    /**
     * Creates an ArrayList of ScNode objects that can be used to display nodes during drawer menu search/filter function
     * @param nodeList NodeList object to collect information about node in it
     * @return ArrayList that contains all the nodes of the provided NodeList object
     */
    private ArrayList<ScNode> returnSubnodeSearchArrayListList(NodeList nodeList) {
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
     * Converts ImageSpanAnchor found in nodeContent to an Element object ready to be written to the database
     * @param imageSpanAnchor ImageSpanAnchor object from nodeContent
     * @param offset offset of the Anchor image
     * @param lastFoundJustification justification of the Anchor image
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveImageSpanAnchor(ImageSpanAnchor imageSpanAnchor, String offset, String lastFoundJustification) {
        Element element = doc.createElement("encoded_png");
        element.setAttribute("char_offset", offset);
        element.setAttribute("justification", lastFoundJustification);
        element.setAttribute("anchor", imageSpanAnchor.getAnchorName());
        return element;
    }

    /**
     * Converts ImageSpanFile found in nodeContent to an Element object ready to be written to the database
     * @param imageSpanFile ImageSpanFile object from nodeContent
     * @param offset offset of the file
     * @param lastFoundJustification justification of the file
     * @param node from the database that is being updated/saved
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveImageSpanFile(ImageSpanFile imageSpanFile, String offset, String lastFoundJustification, Node node) {
        Element element = doc.createElement("encoded_png");
        element.setAttribute("char_offset", offset);
        element.setAttribute("justification", lastFoundJustification);
        element.setAttribute("filename", imageSpanFile.getFilename());
        if (imageSpanFile.isFromDatabase()) {
            element.setAttribute("time", imageSpanFile.getTimestamp());
            element.setTextContent(getFileEncodedString(node, imageSpanFile.getOriginalOffset(), imageSpanFile.getFilename()));
        } else {
            Uri fileUri = Uri.parse(imageSpanFile.getFileUri());
            try (
                    InputStream fileInputSteam = context.getContentResolver().openInputStream(fileUri);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
            ) {
                byte[] buf = new byte[4 * 1024];
                int length;
                while ((length = fileInputSteam.read(buf)) != -1) {
                    byteArrayOutputStream.write(buf);
                }
                String base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                element.setAttribute("time", String.valueOf(System.currentTimeMillis() / 1000));
                element.setTextContent(base64String);
            } catch (IOException e) {
                displayToast(context.getString(R.string.toast_error_failed_to_save_database_changes));
            }
        }
        return element;
    }

    /**
     * Converts ImageSpan found in nodeContent to an Element object ready to be written to the database
     * @param imageSpanImage ImageSpan object from nodeContent
     * @param offset offset of the image
     * @param lastFoundJustification justification of the image
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveImageSpanImage(ImageSpanImage imageSpanImage, String offset, String lastFoundJustification) {
        Element element = doc.createElement("encoded_png");
        Drawable drawable = imageSpanImage.getDrawable();
        // Hopefully it's always a Bitmap drawable, because I get it from the same source
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        String baseString = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        element.setAttribute("char_offset", offset);
        element.setAttribute("justification", lastFoundJustification);
        element.setAttribute("link", "");
        element.setTextContent(baseString);
        return element;
    }

    /**
     * Converts ImageSpanLatex found in nodeContent to an Element object ready to be written to the database
     * @param imageSpanLatex ImageSpanLatex object from nodeContent
     * @param offset offset of the LaTeX image
     * @param lastFoundJustification justification of the LaTeX image
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveImageSpanLatex(ImageSpanLatex imageSpanLatex, String offset, String lastFoundJustification) {
        Element element = doc.createElement("encoded_png");
        element.setAttribute("char_offset", offset);
        element.setAttribute("justification", lastFoundJustification);
        element.setAttribute("filename", "__ct_special.tex");
        element.setTextContent(imageSpanLatex.getLatexCode());
        return element;
    }

    @Override
    public void saveNodeContent(String nodeUniqueID) {
        Node node = findNode(nodeUniqueID);
        if (node == null) {
            displayToast(context.getString(R.string.toast_error_while_saving_node_content_not_found));
            return;
        }
        if (mainViewModel.getCurrentNode().isRichText()) {
            int next; // The end of the current span and the start of the next one
            int totalContentLength = 0; // Needed to calculate offset for the tag
            int currentPartContentLength = 0; // Needed to calculate offset for the tag
            ArrayList<Element> normalNodes = new ArrayList<>(); // Store all normal tags in order
            ArrayList<Element> offsetNodes = new ArrayList<>(); // Store all tags with char_offset attribute
            // Can't get justification for all items that have offset (except tables), so the best next
            // thing I can do is save last detected justification value and used it when creating those nodes
            String lastFoundJustification = "left";
            for (ScNodeContent scNodeContent : mainViewModel.getNodeContent().getValue()) {
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
                        Element element = doc.createElement("rich_text");
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
                                offsetNodes.add(saveImageSpanFile(
                                        (ImageSpanFile) span,
                                        String.valueOf(currentPartContentLength + totalContentLength),
                                        lastFoundJustification,
                                        node
                                ));
                            } else if (span instanceof ClickableSpanFile) {
                                // Attached File text part
                                addContent = false;
                            } else if (span instanceof ForegroundColorSpan) {
                                ForegroundColorSpan foregroundColorSpan = (ForegroundColorSpan) span;
                                String backgroundColor = String.format("#%1$s", Integer.toHexString(foregroundColorSpan.getForegroundColor()).substring(2));
                                element.setAttribute("foreground", backgroundColor);
                            } else if (span instanceof ImageSpanAnchor) {
                                addContent = false;
                                offsetNodes.add(saveImageSpanAnchor(
                                        (ImageSpanAnchor) span,
                                        String.valueOf(currentPartContentLength + totalContentLength),
                                        lastFoundJustification
                                ));
                            } else if (span instanceof ImageSpanImage) {
                                addContent = false;
                                offsetNodes.add(saveImageSpanImage(
                                        (ImageSpanImage) span,
                                        String.valueOf(currentPartContentLength + totalContentLength),
                                        lastFoundJustification
                                ));
                            } else if (span instanceof ImageSpanLatex) {
                                addContent = false;
                                offsetNodes.add(saveImageSpanLatex(
                                        (ImageSpanLatex) span,
                                        String.valueOf(currentPartContentLength + totalContentLength),
                                        lastFoundJustification
                                ));
                            } else if (span instanceof LeadingMarginSpan.Standard) {
                                LeadingMarginSpan.Standard leadingMarginSpan = (LeadingMarginSpan.Standard) span;
                                int indent = leadingMarginSpan.getLeadingMargin(true) / 40;
                                element.setAttribute("indent", String.valueOf(indent));
                            } else if (span instanceof TypefaceSpanCodebox) {
                                addContent = false;
                                offsetNodes.add(saveTypefaceSpanCodebox(
                                        (TypefaceSpanCodebox) span,
                                        String.valueOf(currentPartContentLength + totalContentLength),
                                        lastFoundJustification,
                                        nodeContent.subSequence(i, next).toString()
                                ));
                            } else if (span instanceof RelativeSizeSpan) {
                                element.setAttribute("scale", saveRelativeSizeSpan((RelativeSizeSpan) span));
                            } else if (span instanceof StrikethroughSpan) {
                                element.setAttribute("strikethrough", "true");
                            } else if (span instanceof StyleSpanBold) {
                                element.setAttribute("weight", "heavy");
                            } else if (span instanceof StyleSpanItalic) {
                                element.setAttribute("style", "italic");
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
                    offsetNodes.add(saveScNodeContentTable(
                            (ScNodeContentTable) scNodeContent,
                            String.valueOf(currentPartContentLength + totalContentLength)
                    ));
                }
            }
            deleteNodeContent(node);
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
            ScNodeContentText scNodeContentText = (ScNodeContentText) mainViewModel.getNodeContent().getValue().get(0);
            SpannableStringBuilder nodeContent = scNodeContentText.getContent();
            Element element = doc.createElement("rich_text");
            deleteNodeContent(node);
            element.setTextContent(nodeContent.toString());
            node.appendChild(element);
        }
        node.getAttributes().getNamedItem("ts_lastsave").setTextContent(String.valueOf(System.currentTimeMillis() / 1000));
        if (node.getAttributes().getNamedItem("master_id") == null) {
            Element element = (Element) node;
            element.setAttribute("master_id", "0");
        }
        writeIntoDatabase();
    }

    /**
     * Converts ScNodeContentTable found in nodeContent to an Element object ready to be written to the database
     * @param scNodeContentTable ScNodeContentTable from the nodeContent
     * @param offset offset of the table
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveScNodeContentTable(ScNodeContentTable scNodeContentTable, String offset) {
        Element tableElement = doc.createElement("table");
        tableElement.setAttribute("char_offset", offset);
        tableElement.setAttribute("justification", scNodeContentTable.getJustification());
        tableElement.setAttribute("col_min", String.valueOf(scNodeContentTable.getColMin()));
        tableElement.setAttribute("col_max", String.valueOf(scNodeContentTable.getColMax()));
        tableElement.setAttribute("col_widths", scNodeContentTable.getColWidths());
        tableElement.setAttribute("is_light", String.valueOf(scNodeContentTable.getLightInterface()));
        // Adding table content
        for (int i = 1; i < scNodeContentTable.getContent().size(); i++) {
            Element rowElement = doc.createElement("row");
            for (CharSequence cell: scNodeContentTable.getContent().get(i)) {
                Element cellElement = doc.createElement("cell");
                cellElement.setTextContent(cell.toString());
                rowElement.appendChild(cellElement);
            }
            tableElement.appendChild(rowElement);
        }
        // Adding header at the end of the table tag
        Element headerRowElement = doc.createElement("row");
        for (CharSequence cell : scNodeContentTable.getContent().get(0)) {
            Element cellElement = doc.createElement("cell");
            cellElement.setTextContent(cell.toString());
            headerRowElement.appendChild(cellElement);
        }
        tableElement.appendChild(headerRowElement);
        return tableElement;
    }

    /**
     * Converts TypefaceSpanCodebox found in nodeContent to an Element object ready to be written to the database
     * @param typefaceSpanCodebox TypefaceSpanCodebox object from nodeContent
     * @param offset offset of the codebox
     * @param lastFoundJustification justification of the codebox
     * @param codeboxContent text of the codebox
     * @return Element that can be added to Node and writen to the database
     */
    private Element saveTypefaceSpanCodebox(TypefaceSpanCodebox typefaceSpanCodebox, String offset, String lastFoundJustification, String codeboxContent) {
        Element element = doc.createElement("codebox");
        element.setAttribute("char_offset", offset);
        element.setAttribute("justification", lastFoundJustification);
        element.setAttribute("frame_width", String.valueOf(typefaceSpanCodebox.getFrameWidth()));
        element.setAttribute("frame_height", String.valueOf(typefaceSpanCodebox.getFrameHeight()));
        element.setAttribute("width_in_pixels", typefaceSpanCodebox.isWidthInPixel() ? "1" : "0");
        element.setAttribute("syntax_highlighting", typefaceSpanCodebox.getSyntaxHighlighting());
        element.setAttribute("highlight_brackets", typefaceSpanCodebox.isHighlightBrackets() ? "1" : "0");
        element.setAttribute("show_line_numbers", typefaceSpanCodebox.isShowLineNumbers() ? "1" : "0");
        element.setTextContent(codeboxContent);
        return element;
    }

    @Override
    public ArrayList<ScSearchNode> search(Boolean noSearch, String query) {
        NodeList nodeList = doc.getFirstChild().getChildNodes();
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        if (noSearch) {
            // If user marked that search should skip search "excluded" nodes
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getNodeName().equals("node")) {
                    // If node is a "node" and not some other tag
                    boolean hasSubnodes = hasSubnodes(nodeList.item(i));
                    Node masterIdAttr = nodeList.item(i).getAttributes().getNamedItem("master_id");
                    if (masterIdAttr != null && !"0".equals(masterIdAttr.getNodeValue()) && hasSubnodes) {
                        Node masterNode = findNode(masterIdAttr.getNodeValue());
                        if (masterNode.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                            searchResult.addAll(searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                        }
                        continue;
                    }
                    if (nodeList.item(i).getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                        // if user haven't marked to skip current node - searches through its content
                        boolean isParent = false;
                        boolean isSubnode = true;
                        if (hasSubnodes) {
                            isParent = true;
                            isSubnode = false;
                        }
                        ScSearchNode result = findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                        if (result != null) {
                            searchResult.add(result);
                        }
                    }
                    if (hasSubnodes && nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                        // if user haven't selected not to search subnodes of current node
                        searchResult.addAll(searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                    }
                }
            }
            return searchResult;
        } else {
            for (int i = 0; i < nodeList.getLength(); i++) {
                boolean hasSubnodes = hasSubnodes(nodeList.item(i));
                if (nodeList.item(i).getNodeName().equals("node")) {
                    Node masterIdAttr = nodeList.item(i).getAttributes().getNamedItem("master_id");
                    if (masterIdAttr != null && !"0".equals(masterIdAttr.getNodeValue())) {
                        searchResult.addAll(searchAllNodes(query, nodeList.item(i).getChildNodes()));
                        continue;
                    }
                    boolean isParent  = false;
                    boolean isSubnode  = true;
                    if (hasSubnodes) {
                        isParent = true;
                        isSubnode = false;
                    }
                    ScSearchNode result = findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes) {
                    // If node has subnodes
                    searchResult.addAll(searchAllNodes(query, nodeList.item(i).getChildNodes()));
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
                Node masterIdAttr = nodeList.item(i).getAttributes().getNamedItem("master_id");
                if (masterIdAttr != null && !"0".equals(masterIdAttr.getNodeValue())) {
                    searchResult.addAll(searchAllNodes(query, nodeList.item(i).getChildNodes()));
                    continue;
                }
                boolean isParent;
                boolean isSubnode;
                if (hasSubnodes) {
                    isParent = true;
                    isSubnode = false;
                } else {
                    isParent = false;
                    isSubnode = true;
                }
                ScSearchNode result = findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
            }
            if (hasSubnodes) {
                // If node has subnodes
                searchResult.addAll(searchAllNodes(query, nodeList.item(i).getChildNodes()));
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
                boolean hasSubnodes = hasSubnodes(nodeList.item(i));
                String noSearchCh = nodeList.item(i).getAttributes().getNamedItem("nosearch_ch").getNodeValue();
                Node masterIdAttr = nodeList.item(i).getAttributes().getNamedItem("master_id");
                if (masterIdAttr != null && !"0".equals(masterIdAttr.getNodeValue()) && hasSubnodes && noSearchCh.equals("0")) {
                    Node masterNode = findNode(masterIdAttr.getNodeValue());
                    if (masterNode.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                        searchResult.addAll(searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                    }
                    continue;
                }
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
                    ScSearchNode result = findInNode(nodeList.item(i), query, hasSubnodes, isParent, isSubnode);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
                if (hasSubnodes && noSearchCh.equals("0")) {
                    // If node has subnodes and user haven't selected not to search subnodes of current node
                    searchResult.addAll(searchNodesSkippingExcluded(query, nodeList.item(i).getChildNodes()));
                }
            }
        }
        return searchResult;
    }

    @Override
    public void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh) {
        Node node = findNode(nodeUniqueID);
        String masterId = node.getAttributes().getNamedItem("master_id").getNodeValue();
        if (masterId != null && !"0".equals(masterId)) {
            node = findNode(masterId);
        }
        NamedNodeMap properties = node.getAttributes();
        properties.getNamedItem("name").setNodeValue(name);
        if (properties.getNamedItem("prog_lang").getNodeValue().equals("custom-colors") && !progLang.equals("custom-colors")) {
            StringBuilder nodeContent = convertRichTextNodeContentToPlainText(node);
            deleteNodeContent(node);
            Element newContentNode = doc.createElement("rich_text");
            newContentNode.setTextContent(nodeContent.toString());
            node.appendChild(newContentNode);
        }
        properties.getNamedItem("prog_lang").setNodeValue(progLang);
        properties.getNamedItem("nosearch_me").setNodeValue(noSearchMe);
        properties.getNamedItem("nosearch_ch").setNodeValue(noSearchCh);
        properties.getNamedItem("ts_lastsave").setNodeValue(String.valueOf(System.currentTimeMillis() / 1000));
        writeIntoDatabase();
    }

    /**
     * Write opened database to the file
     * App has to have the the permissions to write to said file
     */
    private void writeIntoDatabase(){
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(doc);
            OutputStream fileOutputStream;
            if (databaseUri.startsWith("content://")) {
                fileOutputStream = context.getContentResolver().openOutputStream(Uri.parse(databaseUri), "wt");
            } else {
                fileOutputStream = new FileOutputStream(databaseUri);
            }
            StreamResult result = new StreamResult(fileOutputStream);  // To save it in the Internal Storage
            transformer.transform(dSource, result);
        } catch (TransformerException e) {
            displayToast(context.getString(R.string.toast_error_failed_to_save_database_changes));
        } catch (FileNotFoundException e) {
            displayToast(context.getString(R.string.toast_error_database_does_not_exists));
        }
    }
}
