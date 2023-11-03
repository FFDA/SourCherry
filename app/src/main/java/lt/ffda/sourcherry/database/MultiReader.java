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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.DocumentsContract;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import lt.ffda.sourcherry.utils.Filenames;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class MultiReader extends DatabaseReader {
    private Document drawerMenu;
    private final Context context;
    private final DocumentBuilder documentBuilder;
    private final Handler handler;
    private final Uri mainFolderUri;
    private final MainViewModel mainViewModel;

    /**
     * Class that opens databases based on file system and categories in it. Every node has it's own
     * catalog, XML file in it with text data of the file. All files, images are saved in the node's
     * folder with the hash256 as it filename. Provides all function necessary to read and edit the
     * data in the database.
     * @param mainFolderUri Uri of the root folder of the database
     * @param context application context to display toast messages, get resources, handle clicks
     * @param handler to run methods on main thread
     * @param mainViewModel ViewModel of MainView activity to store data
     * @throws ParserConfigurationException Indicates a serious configuration error.
     */
    public MultiReader(Uri mainFolderUri, Context context, Handler handler, MainViewModel mainViewModel) throws ParserConfigurationException {
        this.mainFolderUri = mainFolderUri;
        this.context = context;
        this.handler = handler;
        this.mainViewModel = mainViewModel;
        this.documentBuilder = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder();
    }

    @Override
    public void addNodeToBookmarks(String nodeUniqueID) {
        try {
            String lstFileDocumentId = null;
            try (Cursor cursor = this.getMainNodesCursor()) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals("bookmarks.lst")) {
                        lstFileDocumentId = cursor.getString(0);
                        break;
                    }
                }
            }
            if (lstFileDocumentId == null) {
                lstFileDocumentId = DocumentsContract.getDocumentId(DocumentsContract.createDocument(
                        this.context.getContentResolver(),
                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, DocumentsContract.getTreeDocumentId(this.mainFolderUri)),
                        "application/octet-stream",
                        "bookmarks.lst"
                ));
            }
            List<Integer> list = null;
            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId))) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                if (line != null) {
                    list = Arrays.stream(line.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                }
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(Integer.parseInt(nodeUniqueID));
            }
            list.sort(Comparator.comparingInt(b -> b));
            try (
                    OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId), "wt");
                    PrintWriter pw = new PrintWriter(os)) {
                pw.println(list.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
            }
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, "bookmarks.lst"));
        }
    }

    @Override
    public ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh) {
        ScNode scNode = null;
        try {
            String newNodeUniqueID = String.valueOf(this.getNodeMaxID() + 1);
            Node node;
            Uri parentUri;
            boolean isSubnode = true;
            if (nodeUniqueID.equals("0")) {
                // User chose to create node in MainMenu
                node = this.drawerMenu.getElementsByTagName("sourcherry").item(0);
                parentUri = DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, node.getAttributes().getNamedItem("saf_id").getNodeValue());
            } else {
                node = this.findSingleNode(nodeUniqueID);
                if (relation == 0) {
                    // Sibling. New folder has to be created for a parent of the selected node and
                    // parent's subnodes.lst has to be sorted in a way that new folder would be after
                    // the newly added folder/node
                    if (node.getParentNode().getNodeName().equals("sourcherry")) {
                        // If node actually is in main menu
                        isSubnode = false;
                    }
                    parentUri = DocumentsContract.buildDocumentUriUsingTree(
                            this.mainFolderUri,
                            node.getParentNode().getAttributes().getNamedItem("saf_id").getNodeValue()
                    );
                } else {
                    parentUri = DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, node.getAttributes().getNamedItem("saf_id").getNodeValue());
                }
            }
            if (parentUri == null) {
                this.displayToast(this.context.getString(R.string.toast_error_failed_to_create_node));
                return null;
            }
            Uri newNodeFolderUri = DocumentsContract.createDocument(
                    this.context.getContentResolver(),
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    newNodeUniqueID
            );
            if (newNodeFolderUri == null) {
                this.displayToast(this.context.getString(R.string.toast_error_failed_to_create_node));
                return null;
            }
            Uri newNodeNodeXmlUri = DocumentsContract.createDocument(
                    this.context.getContentResolver(),
                    newNodeFolderUri,
                    "text/xml",
                    "node.xml"
            );
            if (newNodeNodeXmlUri == null) {
                this.displayToast(this.context.getString(R.string.toast_error_failed_to_create_node));
                return null;
            }
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            // Creating new node with all necessary tags
            Document document = this.documentBuilder.newDocument();
            Element newNode = document.createElement("node");
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
            // Creating the main tag for the node.xml
            Node newCherryTreeNode = document.createElement("cherrytree");
            newCherryTreeNode.appendChild(newNode);
            try (OutputStream outputStream = this.context.getContentResolver().openOutputStream(newNodeNodeXmlUri)) {
                this.saveChanges(newCherryTreeNode, outputStream);
            }
            // Creating new drawerMenu.xml item
            Element newDrawerMenuItem = this.drawerMenu.createElement("node");
            newDrawerMenuItem.setAttribute("unique_id", newNodeUniqueID);
            newDrawerMenuItem.setAttribute("name", name);
            newDrawerMenuItem.setAttribute("has_subnodes", "0");
            newDrawerMenuItem.setAttribute("prog_lang", progLang);
            newDrawerMenuItem.setAttribute("nosearch_me", noSearchMe);
            newDrawerMenuItem.setAttribute("nosearch_ch", noSearchCh);
            newDrawerMenuItem.setAttribute("is_rich_text", progLang.equals("custom-colors") ? "1" : "0");
            newDrawerMenuItem.setAttribute("is_bold", "0");
            newDrawerMenuItem.setAttribute("foreground_color", "");
            newDrawerMenuItem.setAttribute("icon_id", "0");
            newDrawerMenuItem.setAttribute("readonly", "0");
            newDrawerMenuItem.setAttribute("saf_id", DocumentsContract.getDocumentId(newNodeFolderUri));
            if (relation == 0) {
                this.addNodeToLst(DocumentsContract.getDocumentId(parentUri), nodeUniqueID, newNodeUniqueID);
                if (!node.getParentNode().getNodeName().equals("sourcherry")) {
                    ((Element) node.getParentNode()).setAttribute("has_subnodes", "1");
                }
                if (node.getNextSibling() == null) {
                    node.getParentNode().appendChild(newDrawerMenuItem);
                } else {
                    node.getParentNode().insertBefore(newDrawerMenuItem, node.getNextSibling());
                }
            } else {
                this.addNodeToLst(DocumentsContract.getDocumentId(parentUri), newNodeUniqueID);
                if (!node.getNodeName().equals("sourcherry")) {
                    ((Element) node).setAttribute("has_subnodes", "1");
                }
                node.appendChild(newDrawerMenuItem);
            }
            this.saveDrawerMenuToStorage();
            scNode = new ScNode(newNodeUniqueID, name,false, false, isSubnode, progLang.equals("custom-colors"), false, "", 0, false);
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_create_node));
        }
        return scNode;
    }

    @Override
    public void deleteNode(String nodeUniqueID) {
        try {
            Node node = this.findSingleNode(nodeUniqueID);
            Node parentNode = node.getParentNode();
            Uri nodeParentUri = this.getNodeUri(parentNode);
            Uri nodeUri = this.getNodeUri(node);
            List<String> uniqueIDList = new ArrayList<>(); // Collecting all nodeUniqueIDs to remove them from the bookmarks
            if (nodeUri == null || nodeParentUri == null) {
                this.displayToast(this.context.getString(R.string.toast_error_failed_to_delete_node));
                return;
            } else {
                DocumentsContract.deleteDocument(
                        this.context.getContentResolver(),
                        nodeUri
                );
                this.removeNodeFromLst(DocumentsContract.getDocumentId(nodeParentUri), nodeUniqueID, "subnodes.lst");
            }
            uniqueIDList.add(node.getAttributes().getNamedItem("unique_id").getNodeValue());
            NodeList deletedNodeChildren = node.getChildNodes();
            for (int i = 0; i < deletedNodeChildren.getLength(); i++) {
                if (deletedNodeChildren.item(i).getNodeName().equals("node")) {
                    uniqueIDList.add(deletedNodeChildren.item(i).getAttributes().getNamedItem("unique_id").getNodeValue());
                    this.collectUniqueID(uniqueIDList, deletedNodeChildren.item(i).getChildNodes());
                }
            }
            parentNode.removeChild(node);
            this.removeNodesFromBookmarks(uniqueIDList);
            if (parentNode.getChildNodes().getLength() < 1) {
                ((Element) parentNode).setAttribute("has_subnodes", "0");
            }
            this.saveDrawerMenuToStorage();
        } catch (FileNotFoundException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_delete_node));
        }
    }

    @Override
    public void displayToast(String message) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MultiReader.this.context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean doesNodeExist(String nodeUniqueID) {
        if (nodeUniqueID == null) {
            return false;
        }
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<ScNode> getAllNodes(boolean noSearch) {
        if (noSearch) {
            return this.returnSubnodeSearchArrayList(this.drawerMenu.getElementsByTagName("sourcherry").item(0).getChildNodes());
        } else {
            return this.returnSubnodeArrayList(this.drawerMenu.getElementsByTagName("node"), false);
        }
    }

    @Override
    public ArrayList<ScNode> getBookmarkedNodes() {
        ArrayList<ScNode> nodes;
        List<String> bookmarksIds = null;
        try (Cursor cursor = this.getMainNodesCursor()) {
            while (cursor != null && cursor.moveToNext()) {
                if (cursor.getString(2).equals("bookmarks.lst")) {
                    try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)))) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = br.readLine();
                        if (line != null) {
                            bookmarksIds = Arrays.stream(line.split(","))
                                    .collect(Collectors.toList());
                        }
                    } catch (IOException e) {
                        this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, cursor.getString(2)));
                    }
                    break;
                }
            }
            if (bookmarksIds == null || bookmarksIds.size() == 0) {
                // Bookmark list is empty
                return null;
            } else {
                nodes = new ArrayList<>();
            }
        }
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (bookmarksIds.contains(item.getAttributes().getNamedItem("unique_id").getNodeValue())) {
                nodes.add(this.createSingleMenuItem(item, false, false));
            }
            if (nodes.size() >= bookmarksIds.size()) {
                break;
            }
        }
        return nodes;
    }

    @Override
    public ImageSpan makeBrokenImageSpan(int type) {
        // Returns an image span that is used to display as placeholder image
        // used when cursor window is to small to get an image blob
        // pass 0 to get broken image span, pass 1 to get broken latex span
        SpannableStringBuilder brokenSpan = new SpannableStringBuilder();
        brokenSpan.append(" ");
        Drawable drawableBrokenImage;
        ImageSpan brokenImage;
        if (type == 0) {
            drawableBrokenImage = AppCompatResources.getDrawable(this.context, R.drawable.ic_outline_broken_image_48);
            brokenImage = new ImageSpanImage(drawableBrokenImage);
        } else {
            drawableBrokenImage =  AppCompatResources.getDrawable(this.context, R.drawable.ic_outline_broken_latex_48);
            brokenImage = new ImageSpanLatex(drawableBrokenImage);
        }
        //// Inserting image
        drawableBrokenImage.setBounds(0,0, drawableBrokenImage.getIntrinsicWidth(), drawableBrokenImage.getIntrinsicHeight());
        return brokenImage;
    }

    @Override
    public InputStream getFileInputStream(String nodeUniqueID, String filename, String time, String control) {
        try (Cursor nodeContentCursor = this.getNodeChildrenCursor(nodeUniqueID)) {
            while (nodeContentCursor.moveToNext()) {
                if (!nodeContentCursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR) && nodeContentCursor.getString(2).substring(0, nodeContentCursor.getString(2).lastIndexOf(".")).equals(control)) {
                    try {
                        return this.context.getContentResolver().openInputStream(
                                DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, nodeContentCursor.getString(0)));
                    } catch (FileNotFoundException e) {
                        this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, filename));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public InputStream getImageInputStream(String nodeUniqueID, String control) {
        try (Cursor nodeContentCursor = this.getNodeChildrenCursor(nodeUniqueID)) {
            while (nodeContentCursor.moveToNext()) {
                if (!nodeContentCursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR) && nodeContentCursor.getString(2).substring(0, nodeContentCursor.getString(2).lastIndexOf(".")).equals(control)) {
                    try {
                        return this.context.getContentResolver().openInputStream(
                                DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, nodeContentCursor.getString(0)));
                    } catch (FileNotFoundException e) {
                        this.displayToast(this.context.getString(R.string.toast_error_failed_to_load_image));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<ScNode> getMainNodes() {
        NodeList nodelist = drawerMenu.getElementsByTagName("sourcherry").item(0).getChildNodes();
        return this.returnSubnodeArrayList(nodelist, false);
    }

    @Override
    public ArrayList<ScNode> getMenu(String nodeUniqueID) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (item.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                nodes.add(this.createSingleMenuItem(item, true, false));
                nodes.addAll(this.returnSubnodeArrayList(item.getChildNodes(), true));
            }
        }
        return nodes;
    }

    @Override
    public void loadNodeContent(String nodeUniqueID) {
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary storage for text, codebox, image formatting
        ArrayList<ScNodeContentTable> nodeTables = new ArrayList<>(); // Temporary storage for tables
        ArrayList<Integer> nodeTableCharOffsets = new ArrayList<>();
        Cursor nodeContentCursor = this.getNodeChildrenCursor(nodeUniqueID);
        if (nodeContentCursor == null) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_find_node_content));
        }
        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        while (nodeContentCursor != null && nodeContentCursor.moveToNext()) {
            if (nodeContentCursor.getString(2).equals("node.xml")) {
                try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, nodeContentCursor.getString(0)))) {
                    Node node = this.documentBuilder.parse(is).getElementsByTagName("node").item(0);
                    String nodeProgLang = node.getAttributes().getNamedItem("prog_lang").getNodeValue();
                    if (nodeProgLang.equals("custom-colors") || nodeProgLang.equals("plain-text")) {
                        // This is formatting for Rich Text and Plain Text nodes
                        NodeList nodeContentNodeList = node.getChildNodes(); // Gets all the subnodes/childnodes of selected node
                        for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                            // Loops through nodes of selected node
                            Node currentNode = nodeContentNodeList.item(x);
                            switch (currentNode.getNodeName()) {
                                case "rich_text":
                                    if (currentNode.hasAttributes()) {
                                        nodeContentStringBuilder.append(makeFormattedRichText(currentNode));
                                    } else {
                                        nodeContentStringBuilder.append(currentNode.getTextContent());
                                    }
                                    break;
                                case "codebox": {
                                    int charOffset = this.getCharOffset(currentNode);
                                    SpannableStringBuilder codeboxText = this.makeFormattedCodeboxSpan(currentNode);
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
                                            SpannableStringBuilder latexImageSpan = this.makeLatexImageSpan(currentNode);
                                            nodeContentStringBuilder.insert(charOffset + totalCharOffset, latexImageSpan);
                                        } else {
                                            // For actual attached files
                                            SpannableStringBuilder attachedFileSpan = this.makeAttachedFileSpan(currentNode, nodeUniqueID);
                                            nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                            totalCharOffset += attachedFileSpan.length() - 1;
                                        }
                                    } else if (currentNode.getAttributes().getNamedItem("anchor") != null) {
                                        SpannableStringBuilder anchorImageSpan = this.makeAnchorImageSpan(currentNode.getAttributes().getNamedItem("anchor").getNodeValue());
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                    } else {
                                        // Images
                                        int pos = nodeContentCursor.getPosition(); // Save position of the cursor before resetting
                                        nodeContentCursor.moveToPosition(-1);
                                        SpannableStringBuilder imageSpan = this.makeImageSpan(nodeContentCursor, currentNode.getAttributes().getNamedItem("sha256sum").getNodeValue(), nodeUniqueID);
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                        nodeContentCursor.moveToPosition(pos); // Restore cursor position, otherwise - infinite loop
                                    }
                                    break;
                                }
                                case "table": {
                                    int charOffset = getCharOffset(currentNode) + totalCharOffset; // Place where SpannableStringBuilder will be split
                                    nodeTableCharOffsets.add(charOffset);
                                    int[] cellMinMax = this.getTableMinMax(currentNode);
                                    ArrayList<CharSequence[]> currentTableContent = new ArrayList<>(); // ArrayList with all the content of the table
                                    byte lightInterface = 0;
                                    if (!((Element) currentNode).getAttribute("is_light").equals("")) {
                                        lightInterface = Byte.parseByte(((Element) currentNode).getAttribute("is_light"));
                                    }
                                    NodeList tableRowsNodes = ((Element) currentNode).getElementsByTagName("row"); // All the rows of the table. There are empty text nodes that has to be filtered out (or only row nodes selected this way)
                                    currentTableContent.add(this.getTableRow(tableRowsNodes.item(tableRowsNodes.getLength() - 1)));
                                    for (int row = 0; row < tableRowsNodes.getLength() - 1; row++) {
                                        currentTableContent.add(this.getTableRow(tableRowsNodes.item(row)));
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
                        nodeContentStringBuilder.append(this.makeFormattedCodeNodeSpan(node));
                    }
                } catch (IOException | SAXException e) {
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_load_node_content));
                }
            }
        }
        nodeContentCursor.close();
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
        this.mainViewModel.getNodeContent().postValue(nodeContent);
    }

    @Override
    public int getNodeMaxID() {
        int maxID = -1;
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
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
        Node node = this.findSingleNode(nodeUniqueID);
        String name = node.getAttributes().getNamedItem("name").getNodeValue();
        String progLang = node.getAttributes().getNamedItem("prog_lang").getNodeValue();
        byte noSearchMe = Byte.parseByte(node.getAttributes().getNamedItem("nosearch_me").getNodeValue());
        byte noSearchCh = Byte.parseByte(node.getAttributes().getNamedItem("nosearch_ch").getNodeValue());
        if (name == null) {
            return null;
        } else {
            return new ScNodeProperties(nodeUniqueID, name, progLang, noSearchMe, noSearchCh);
        }
    }

    @Override
    public ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID) {
        ArrayList<ScNode> nodes = null;
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                Node parentNode = node.getParentNode();
                if (parentNode == null) {
                    return nodes;
                } else if (parentNode.getNodeName().equals("sourcherry")) {
                    nodes = this.getMainNodes();
                } else {
                    nodes = this.returnSubnodeArrayList(parentNode.getChildNodes(), true);
                    nodes.add(0, this.createParentNode(parentNode));
                }
            }
        }
        return nodes;
    }

    @Override
    public ScNode getSingleMenuItem(String nodeUniqueID) {
        ScNode node = null;
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (item.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                node = this.createSingleMenuItem(item);
            }
        }
        return node;
    }

    @Override
    public boolean isNodeBookmarked(String nodeUniqueID) {
        boolean nodeBookmarked = false;
        try (Cursor cursor = this.getMainNodesCursor()) {
            while (cursor.moveToNext()) {
                if (cursor.getString(2).equals("bookmarks.lst")) {
                    try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)))) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = br.readLine();
                        if (line != null) {
                            nodeBookmarked = Arrays.asList(line.split(",")).contains(nodeUniqueID);
                        }
                    } catch (IOException e) {
                        this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, cursor.getString(2)));
                    }
                    break;
                }
            }
        }
        return nodeBookmarked;
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
    public ClickableSpan makeAnchorLinkSpan(String nodeUniqueID, String linkAnchorName) {
        // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
        // As in CherryTree it's foreground color #07841B
        ClickableSpanNode clickableSpanNode = new ClickableSpanNode() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) MultiReader.this.context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
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
    public ClickableSpan makeFileFolderLinkSpan(String type, String base64Filename) {
        // Creates and returns a span for a link to external file or folder
        // When user clicks on the link snackbar displays a path to the file that was saved in the original system
        ClickableSpanLink clickableSpanLink = new ClickableSpanLink() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                ((MainView) MultiReader.this.context).fileFolderLinkFilepath(new String(Base64.decode(base64Filename, Base64.DEFAULT)));
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

    @Override
    public boolean moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        Node targetNode = null;
        Node destinationNode = null;
        // User chose to move the node to main menu
        if (destinationNodeUniqueID.equals("0")) {
            NodeList nodeList = this.drawerMenu.getElementsByTagName("sourcherry");
            destinationNode = nodeList.item(0);
        }
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
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
        // In XML file it causes a crash
        if (destinationNode.getNodeName().equals("sourcherry") && targetNode.getParentNode().getNodeName().equals("sourcherry")) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_move_node));
            return false;
        }
        Node parentNodeUniqueID = targetNode.getParentNode().getAttributes().getNamedItem("unique_id");
        if (parentNodeUniqueID != null && parentNodeUniqueID.getNodeValue().equals(destinationNodeUniqueID)) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_move_node));
            return false;
        }
        // Proceeding with the move
        Uri sourceDocumentUri = this.getNodeUri(targetNode);
        Uri sourchParentDocumentUri = this.getNodeUri(targetNode.getParentNode());
        Uri targetPerentDocumentUri = this.getNodeUri(destinationNode);
        Uri result;
        try {
            result = DocumentsContract.moveDocument(
                    this.context.getContentResolver(),
                    sourceDocumentUri,
                    sourchParentDocumentUri,
                    targetPerentDocumentUri
            );
        } catch (FileNotFoundException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_move_node));
            return false;
        }
        if (result == null) {
            this.displayToast(context.getString(R.string.toast_error_new_parent_cant_be_one_of_its_children));
            return false;
        }
        // Removing unique node ID from subnodes.lst in source folder
        this.removeNodeFromLst(DocumentsContract.getDocumentId(sourchParentDocumentUri), targetNodeUniqueID, "subnodes.lst");
        // Adding unique node ID to subnodes.lst in destination folder
        this.addNodeToLst(DocumentsContract.getDocumentId(targetPerentDocumentUri), targetNodeUniqueID);
        // Updating drawer_menu cache
        if (!targetNode.getParentNode().getNodeName().equals("sourcherry")) {
            if (targetNode.getParentNode().getChildNodes().getLength() < 2) {
                ((Element) targetNode.getParentNode()).setAttribute("has_subnodes", "0");
            }
        }
        ((Element) targetNode).setAttribute("saf_id", DocumentsContract.getTreeDocumentId(result));
        destinationNode.appendChild(targetNode);
        if (!destinationNode.getNodeName().equals("sourcherry")) {
            ((Element) destinationNode).setAttribute("has_subnodes", "1");
        }
        this.saveDrawerMenuToStorage();
        return true;
    }

    @Override
    public void removeNodeFromBookmarks(String nodeUniqueID) {
        this.removeNodeFromLst(DocumentsContract.getTreeDocumentId(this.mainFolderUri), nodeUniqueID, "bookmarks.lst");
    }

    @Override
    public void saveNodeContent(String nodeUniqueID) {
        Cursor cursor = this.getNodeChildrenCursor(this.findSingleNode(nodeUniqueID));
        if (cursor == null) {
            this.displayToast(this.context.getString(R.string.toast_error_error_while_saving_node_content_aborting));
            return;
        }
        String documentId = null;
        while (cursor.moveToNext()) {
            if (cursor.getString(2).equals("node.xml")) {
                documentId = cursor.getString(0);
            }
        }
        if (documentId == null) {
            this.displayToast(this.context.getString(R.string.toast_error_error_while_saving_node_content_aborting));
            return;
        }
        Document doc = null;
        Node node = null;
        try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, documentId))) {
            doc = this.documentBuilder.parse(is);
            node = doc.getElementsByTagName("node").item(0);
        } catch (IOException | SAXException e) {
            this.displayToast(this.context.getString(R.string.toast_error_while_searching));
        }
        if (doc == null || node == null) {
            this.displayToast(this.context.getString(R.string.toast_error_error_while_saving_node_content_aborting));
            return;
        }
        if (this.mainViewModel.getCurrentNode().isRichText()) {
            int next; // The end of the current span and the start of the next one
            int totalContentLength = 0; // Needed to calculate offset for the tag
            int currentPartContentLength = 0; // Needed to calculate offset for the tag
            ArrayList<Element> normalNodes = new ArrayList<>(); // Store all normal tags in order
            ArrayList<Element> offsetNodes = new ArrayList<>(); // Store all tags with char_offset attribute
            // Can't get justification for all items that have offset (except tables), so the best next
            // thing I can do is save last detected justification value and used it when creating those nodes
            String lastFoundJustification = "left";
            // Collecting all sha256 sums that were saved in to database. Rest will have to be deleted from internal storage
            List<String> fileImageSha256Sums = new ArrayList<>();
            for (ScNodeContent scNodeContent : this.mainViewModel.getNodeContent().getValue()) {
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
                                ImageSpanFile imageSpanFile = (ImageSpanFile) span;
                                element = doc.createElement("encoded_png");
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("filename", imageSpanFile.getFilename());
                                if (imageSpanFile.isFromDatabase()) {
                                    element.setAttribute("time", imageSpanFile.getTimestamp());
                                    element.setAttribute("sha256sum", imageSpanFile.getSha256sum());
                                    fileImageSha256Sums.add(imageSpanFile.getSha256sum());
                                } else {
                                    try {
                                        Uri userAttachedFileUri = Uri.parse(imageSpanFile.getFileUri());
                                        InputStream inputStream = this.context.getContentResolver().openInputStream(userAttachedFileUri);
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        byte[] buf = new byte[4 * 1024];
                                        int length;
                                        while ((length = inputStream.read(buf)) != -1) {
                                            byteArrayOutputStream.write(buf);
                                        }
                                        byte[] hash = MessageDigest.getInstance("SHA-256").digest(byteArrayOutputStream.toByteArray());
                                        String sha256sum = new BigInteger(1, hash).toString(16);
                                        byteArrayOutputStream.close();
                                        inputStream.close();
                                        String extension = Filenames.getFileExtension(imageSpanFile.getFilename());
                                        Uri multiFileStorageFileUri = DocumentsContract.createDocument(
                                                this.context.getContentResolver(),
                                                this.getNodeUri(this.findSingleNode(this.mainViewModel.getCurrentNode().getUniqueId())),
                                                "*/*",
                                                extension != null ? sha256sum + "." + extension : sha256sum
                                        );
                                        inputStream = this.context.getContentResolver().openInputStream(userAttachedFileUri);
                                        OutputStream outputStream = this.context.getContentResolver().openOutputStream(multiFileStorageFileUri);
                                        while ((length = inputStream.read(buf)) != -1) {
                                            outputStream.write(buf);
                                        }
                                        inputStream.close();
                                        outputStream.close();
                                        element.setAttribute("time", String.valueOf(System.currentTimeMillis() / 1000));
                                        element.setAttribute("sha256sum", sha256sum);
                                        fileImageSha256Sums.add(sha256sum);
                                    } catch (IOException | NoSuchAlgorithmException e) {
                                        this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_database_changes));
                                    }
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
                                element = doc.createElement("encoded_png");
                                ImageSpanAnchor imageSpanAnchor = (ImageSpanAnchor) span;
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("anchor", imageSpanAnchor.getAnchorName());
                                offsetNodes.add(element);
                            } else if (span instanceof ImageSpanImage) {
                                addContent = false;
                                element = doc.createElement("encoded_png");
                                ImageSpanImage imageSpanImage = (ImageSpanImage) span;
                                Drawable drawable = imageSpanImage.getDrawable();
                                // Hopefully it's always a Bitmap drawable, because I get it from the same source
                                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                element.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
                                element.setAttribute("justification", lastFoundJustification);
                                element.setAttribute("link", "");
                                if (imageSpanImage.getSha256sum() != null) {
                                    // If this value isn't null that means that it was loaded from database
                                    element.setAttribute("sha256sum", imageSpanImage.getSha256sum());
                                    fileImageSha256Sums.add(imageSpanImage.getSha256sum());
                                }
                                offsetNodes.add(element);
                            } else if (span instanceof ImageSpanLatex) {
                                addContent = false;
                                ImageSpanLatex imageSpanLatex = (ImageSpanLatex) span;
                                element = doc.createElement("encoded_png");
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
                                element = doc.createElement("codebox");
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
                                } else if (size == 1.20f) {
                                    element.setAttribute("scale", "h4");
                                } else if (size == 1.15f) {
                                    element.setAttribute("scale", "h5");
                                } else if (size == 1.10f) {
                                    element.setAttribute("scale", "h6");
                                } else if (size == 0.75f) {
                                    element.setAttribute("scale", "small");
                                }
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
                    ScNodeContentTable scNodeContentTable = (ScNodeContentTable) scNodeContent;
                    Element tableElement = doc.createElement("table");
                    tableElement.setAttribute("char_offset", String.valueOf(currentPartContentLength + totalContentLength));
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
                    for (CharSequence cell: scNodeContentTable.getContent().get(0)) {
                        Element cellElement = doc.createElement("cell");
                        cellElement.setTextContent(cell.toString());
                        headerRowElement.appendChild(cellElement);
                    }
                    tableElement.appendChild(headerRowElement);
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
            // Cleaning up files if user deleted any
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                if (!cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    String filename = cursor.getString(2);
                    if (!filename.equals("subnodes.lst") && !filename.equals("node.xml")) {
                        filename = filename.substring(0, filename.lastIndexOf("."));
                        if (!fileImageSha256Sums.contains(filename)) {
                            try {
                                DocumentsContract.deleteDocument(
                                        this.context.getContentResolver(),
                                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0))
                                );
                            } catch (FileNotFoundException e) {
                                this.displayToast(this.context.getString(R.string.toast_error_error_while_saving_node_content_failed_to_delete));
                            }
                        }
                    }
                }

            }
            cursor.close();
        } else {
            ScNodeContentText scNodeContentText = (ScNodeContentText) this.mainViewModel.getNodeContent().getValue().get(0);
            SpannableStringBuilder nodeContent = scNodeContentText.getContent();
            Element element = doc.createElement("rich_text");
            this.deleteNodeContent(node);
            element.setTextContent(nodeContent.toString());
            node.appendChild(element);
        }
        Element cherrytreeElement = doc.createElement("cherrytree");
        cherrytreeElement.appendChild(node);
        try (OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, documentId))) {
            this.saveChanges(cherrytreeElement, os);
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_error_while_saving_node_content_aborting));
        }
    }

    @Override
    public ArrayList<ScSearchNode> search(Boolean noSearch, String query) {
        ArrayList<ScSearchNode> searchResult;
        if (noSearch) {
            searchResult = this.searchNodesSkippingExcluded(this.drawerMenu.getElementsByTagName("sourcherry").item(0).getChildNodes(), query);
        } else {
            searchResult = this.searchAllNodes(query);
        }
        return searchResult;
    }

    @Override
    public void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh) {
        Node drawerMenuItem = this.findSingleNode(nodeUniqueID);
        try (Cursor cursor = this.getNodeChildrenCursor(drawerMenuItem)) {
            while (cursor.moveToNext()) {
                if (cursor.getString(2).equals("node.xml")) {
                    InputStream inputStream = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)));
                    Document document = this.documentBuilder.parse(inputStream);
                    Node node = document.getElementsByTagName("node").item(0);
                    if (node.getAttributes().getNamedItem("prog_lang").getNodeValue().equals("custom-colors") && !progLang.equals("custom-colors")) {
                        StringBuilder nodeContent = this.convertRichTextNodeContentToPlainText(node);
                        this.deleteNodeContent(node);
                        Element newContentNode = document.createElement("rich_text");
                        newContentNode.setTextContent(nodeContent.toString());
                        node.appendChild(newContentNode);
                    }
                    node.getAttributes().getNamedItem("name").setNodeValue(name);
                    node.getAttributes().getNamedItem("prog_lang").setNodeValue(progLang);
                    node.getAttributes().getNamedItem("nosearch_me").setNodeValue(noSearchMe);
                    node.getAttributes().getNamedItem("nosearch_ch").setNodeValue(noSearchCh);
                    node.getAttributes().getNamedItem("ts_lastsave").setNodeValue(String.valueOf(System.currentTimeMillis() / 1000));
                    OutputStream outputStream = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)), "wt");
                    this.saveChanges(document, outputStream);
                    inputStream.close();
                    outputStream.close();
                    break;
                }
            }
        } catch (IOException | SAXException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_get_node_properties));
        }
        Element element = (Element) drawerMenuItem;
        element.setAttribute("name", name);
        element.setAttribute("prog_lang", progLang);
        element.setAttribute("nosearch_me", noSearchMe);
        element.setAttribute("nosearch_ch", noSearchCh);
        this.saveDrawerMenuToStorage();
    }

    /**
     * There are *.lst files in Multifile database, that holds list of NodeUniqueID. This method appends
     * NodeUniqueID to that list. If file does not exists it will be created with the file name provided
     * @param documentId   SAF documentID of the folder under witch *.lst file has to be updated
     * @param nodeUniqueID unique ID of the node to add to the file
     */
    private void addNodeToLst(String documentId, String nodeUniqueID) {
        try {
            String lstFileDocumentId = null;
            try (Cursor cursor = this.getNodeChildrenCursorSaf(documentId)) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals("subnodes.lst")) {
                        lstFileDocumentId = cursor.getString(0);
                        break;
                    }
                }
            }
            if (lstFileDocumentId == null) {
                lstFileDocumentId = DocumentsContract.getDocumentId(DocumentsContract.createDocument(
                        this.context.getContentResolver(),
                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, documentId),
                        "application/octet-stream",
                        "subnodes.lst"
                ));
            }
            List<Integer> list = null;
            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId))) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                if (line != null) {
                    list = Arrays.stream(line.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                }
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(Integer.parseInt(nodeUniqueID));
            }
            try (
                    OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId), "wt");
                    PrintWriter pw = new PrintWriter(os)) {
                pw.println(list.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
            }
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, "subnodes.lst"));
        }
    }

    /**
     * There are *.lst files in Multifile database, that holds list of NodeUniqueID. This method adds
     * NodeUniqueID to that list after unique ID of the node indicated by siblingNodeUniqueID. If file does not exists
     * it will be created with the file name provided
     * @param documentId          SAF documentID of the folder under witch *.lst file has to be
     * @param siblingNodeUniqueID unique ID of the node after which new node should appear in DrawerMenu
     * @param nodeUniqueID        unique ID of the node to add to the file
     */
    private void addNodeToLst(String documentId, String siblingNodeUniqueID, String nodeUniqueID) {
        try {
            String lstFileDocumentId = null;
            try (Cursor cursor = this.getNodeChildrenCursorSaf(documentId)) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals("subnodes.lst")) {
                        lstFileDocumentId = cursor.getString(0);
                        break;
                    }
                }
            }
            if (lstFileDocumentId == null) {
                lstFileDocumentId = DocumentsContract.getDocumentId(DocumentsContract.createDocument(
                        this.context.getContentResolver(),
                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, documentId),
                        "application/octet-stream",
                        "subnodes.lst"
                ));
            }
            List<Integer> list = null;
            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId))) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                if (line != null) {
                    list = Arrays.stream(line.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                }
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(list.indexOf(Integer.valueOf(siblingNodeUniqueID)) + 1, Integer.parseInt(nodeUniqueID));
            }
            try (
                    OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId), "wt");
                    PrintWriter pw = new PrintWriter(os)) {
                pw.println(list.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
            }
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, "subnodes.lst"));
        }
    }

    /**
     * Recursively scans through all the nodes in NodeList to collect the uniqueNodeIDs. Adds them
     * to provided String list
     * @param uniqueIDList String list to add the found nodeUniqueIDs
     * @param nodeList NodeList to scan recursively
     */
    private void collectUniqueID(List<String> uniqueIDList, NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals("node")) {
                uniqueIDList.add(nodeList.item(i).getAttributes().getNamedItem("unique_id").getNodeValue());
                collectUniqueID(uniqueIDList, nodeList.item(i).getChildNodes());
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
     * Convenience method to create single node with parent node properties
     * @param node node object to create ScNode object from. This object has to be from drawer_menu.xml file
     * @return ScNode object ready to be displayed in DrawerMenu as a parent node
     */
    private ScNode createParentNode(Node node) {
        return this.createSingleMenuItem(node, true, false);
    }

    /**
     * Creates single ScNode object from provided Node object.
     * @param node node object to create ScNode object from. This object has to be from drawer_menu.xml file
     * @param isParent true - mark node to be displayed as a parent node (will not have indentation and will have arrow pointing down), false - opposite of that
     * @param isSubnode true - mark node to be displayed as a subnode (will have indentation), false - opposite of that
     * @return ScNode object ready to be displayed in DrawerMenu
     */
    private ScNode createSingleMenuItem(Node node, boolean isParent, boolean isSubnode) {
        NamedNodeMap nodeAttribute = node.getAttributes();
        return new ScNode(
                nodeAttribute.getNamedItem("unique_id").getNodeValue(),
                nodeAttribute.getNamedItem("name").getNodeValue(),
                isParent,
                nodeAttribute.getNamedItem("has_subnodes").getNodeValue().equals("1"),
                isSubnode,
                nodeAttribute.getNamedItem("is_rich_text").getNodeValue().equals("1"),
                nodeAttribute.getNamedItem("is_bold").getNodeValue().equals("1"),
                nodeAttribute.getNamedItem("foreground_color").getNodeValue(),
                Integer.parseInt(nodeAttribute.getNamedItem("icon_id").getNodeValue()),
                nodeAttribute.getNamedItem("readonly").getNodeValue().equals("1")
        );
    }

    /**
     * Creates single ScNode object from provided Node object.
     * @param node node object to create ScNode object from. This object has to be from drawer_menu.xml file
     * @return ScNode object ready to be displayed in DrawerMenu
     */
    private ScNode createSingleMenuItem(Node node) {
        NamedNodeMap nodeAttribute = node.getAttributes();
        return new ScNode(
                nodeAttribute.getNamedItem("unique_id").getNodeValue(),
                nodeAttribute.getNamedItem("name").getNodeValue(),
                nodeAttribute.getNamedItem("has_subnodes").getNodeValue().equals("1"), // If node, has subnodes - it means it can be marked as parent
                nodeAttribute.getNamedItem("has_subnodes").getNodeValue().equals("1"),
                !nodeAttribute.getNamedItem("has_subnodes").getNodeValue().equals("1"), // If node, has subnodes - it means it can't be marked as subnode,
                nodeAttribute.getNamedItem("is_rich_text").getNodeValue().equals("1"),
                nodeAttribute.getNamedItem("is_bold").getNodeValue().equals("1"),
                nodeAttribute.getNamedItem("foreground_color").getNodeValue(),
                Integer.parseInt(nodeAttribute.getNamedItem("icon_id").getNodeValue()),
                nodeAttribute.getNamedItem("readonly").getNodeValue().equals("1")
        );
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
                    if (nodeContent.length() < codeboxContentCharOffset + totalCharOffset) {
                        // This check most likely needed in Searcher, but not in Reader
                        // Because in search some objects (like images) are being skipped, however their offset is still being counted
                        nodeContent.append(codeboxContent);
                    } else {
                        nodeContent.insert(codeboxContentCharOffset + totalCharOffset, codeboxContent);
                    }
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
                    nodeName = node.getAttributes().getNamedItem("name").getNodeValue();
                    nodeUniqueID = node.getAttributes().getNamedItem("unique_id").getNodeValue();
                    isRichText = node.getAttributes().getNamedItem("prog_lang").getNodeValue().equals("custom-colors");
                    isBold = node.getAttributes().getNamedItem("is_bold").getNodeValue().equals("0");
                    foregroundColor = node.getAttributes().getNamedItem("foreground").getNodeValue();
                    iconId = Integer.parseInt(node.getAttributes().getNamedItem("custom_icon_id").getNodeValue());
                    isReadOnly = node.getAttributes().getNamedItem("readonly").getNodeValue().equals("0");
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
            return new ScSearchNode(nodeUniqueID, nodeName, isParent, hasSubnodes, isSubnode, isRichText, isBold, foregroundColor, iconId, isReadOnly, query, resultCount, samples.toString());
        } else {
            return null;
        }
    }

    /**
     * Search through DrawerMenu item and returns one with specific node unique ID
     * @param nodeUniqueID unique ID of the node to search for
     * @return found Node or null
     */
    private Node findSingleNode(String nodeUniqueID) {
        Node node = null;
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                node = nodeList.item(i);
                break;
            }
        }
        return node;
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

    /**
     * Returns cursor with children of the root folder of the Multifile database
     * @return SAF cursor. Cursor should be closed after use manually. Cursor has 3 fields: 0 - document_id, 1 - mime_type, 2 - _display_name
     */
    private Cursor getMainNodesCursor() {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(this.mainFolderUri, DocumentsContract.getTreeDocumentId(this.mainFolderUri));
        return this.context.getContentResolver().query(
                childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        );
    }

    /**
     * Creates node's children cursor
     * @param nodeUniqueID unique ID of the node to get the children cursor for
     * @return SAF cursor object with all the children of the folder. Cursor has three columns: document_id, mime_type and _display_name. Cursor has to be closed after user.
     */
    private Cursor getNodeChildrenCursor(String nodeUniqueID) {
        Cursor cursor = null;
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (item.getAttributes().getNamedItem("unique_id").getNodeValue().equals(nodeUniqueID)) {
                Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        this.mainFolderUri,
                        item.getAttributes().getNamedItem("saf_id").getNodeValue());
                cursor = this.context.getContentResolver().query(
                        uri,
                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        null,
                        null,
                        null
                );
            }
        }
        return cursor;
    }

    /**
     * Creates node's children cursor
     * @param node node object to get the children cursor for
     * @return SAF cursor object with all the children of the folder. Cursor has three columns: document_id, mime_type and _display_name. Cursor has to be closed after user.
     */
    private Cursor getNodeChildrenCursor(Node node) {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        this.mainFolderUri,
                        node.getAttributes().getNamedItem("saf_id").getNodeValue()
        );
        return this.context.getContentResolver().query(
                        uri,
                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        null,
                        null,
                        null
                );
    }

    /**
     * Creates cursor with children of the document using SAF documentId. DocumentId has to be of a
     * children of this.mainFolderUri.
     * @param documentId documentId of the document to create children of
     * @return cursor with children documents. Has to be closed after use.
     */
    private Cursor getNodeChildrenCursorSaf(String documentId) {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(this.mainFolderUri, documentId);
        return this.context.getContentResolver().query(
                uri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        );
    }

    /**
     * Creates document Uri of the node
     * @param node node for with folder to create Uri for. It has to be child of the drawerMenu document.
     * @return Uri of node's folder
     */
    private Uri getNodeUri(Node node) {
        return DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, node.getAttributes().getNamedItem("saf_id").getNodeValue());
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
        String sha256sum = node.getAttributes().getNamedItem("sha256sum").getNodeValue();
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
        attachedFileIcon.setSha256sum(sha256sum);
        formattedAttachedFile.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        formattedAttachedFile.append(attachedFileFilename); // Appending filename
        // Detects touches on icon and filename
        ClickableSpanFile imageClickableSpan = new ClickableSpanFile() {
            @Override
            public void onClick(@NonNull View widget) {
                // Launches function in MainView that checks if there is a default action in for attached files
                ((MainView) MultiReader.this.context).saveOpenFile(nodeUniqueID, attachedFileFilename, time, sha256sum);
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
            LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
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
     * Creates a SpannableStringBuilder with image in it
     * Image is created from Base64 string embedded in the tag
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param cursor SAF cursor that holds all node data
     * @param sha256sum image file sha256sum that doubles as the filename for the image
     * @param nodeUniqueID unique ID of the node that has image embedded in it
     * @return SpannableStringBuilder that has spans with image in them
     */
    private SpannableStringBuilder makeImageSpan(Cursor cursor, String sha256sum, String nodeUniqueID) {
        SpannableStringBuilder formattedImage = new SpannableStringBuilder();
        ImageSpanImage imageSpanImage;
        while (cursor.moveToNext()) {
            if (cursor.getString(1).equals("image/png") && cursor.getString(2).substring(0, cursor.getString(2).lastIndexOf(".")).equals(sha256sum)) {
                try {
                    formattedImage.append(" ");
                    InputStream inputStream = this.context.getContentResolver().openInputStream(
                            DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)));
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    Drawable image = new BitmapDrawable(context.getResources(), bitmap);
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
                            ((MainView) context).openImageView(nodeUniqueID, sha256sum);
                        }
                    };
                    formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
                    //**
                    imageSpanImage.setSha256sum(sha256sum);
                } catch (FileNotFoundException e) {
                    // Displays a toast message and appends broken image span to display in node content
                    imageSpanImage = (ImageSpanImage) this.makeBrokenImageSpan(0);
                    formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_load_image));
                }
            }
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
                    ((MainView) MultiReader.this.context).openImageView(latexString);
                }
            };
            formattedLatexImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken latex image span to display in node content
            imageSpanLatex = (ImageSpanLatex) this.makeBrokenImageSpan(1);
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
     * There are *.lst files in Multifile database, that holds list of NodeUniqueID. This method
     * removes NodeUniqueID from the file or if it's the only NodeUniqueID in the *.lst file -
     * deletes the file
     * @param documentId SAF documentID of the folder under witch *.lst file has to be updated
     * @param nodeUniqueID unique ID of the node to remove from the file
     * @param filename filename from which to remove the file. It is used for error message
     */
    private void removeNodeFromLst(String documentId, String nodeUniqueID, String filename) {
        try {
            String lstFileDocumentId = null;
            try (Cursor cursor = this.getNodeChildrenCursorSaf(documentId)) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals(filename)) {
                        lstFileDocumentId = cursor.getString(0);
                        break;
                    }
                }
            }
            // Check if *.lst file was found. Displays message only for subnodes.lst file because
            // bookmarks.lst can be missing if there were no bookmarks
            if (lstFileDocumentId == null) {
                if (filename.equals("subnodes.lst")) {
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, filename));
                }
                return;
            }
            String[] subnodes = null;
            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId))) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                if (line != null) {
                    subnodes = line.split(",");
                }
            }
            subnodes = Arrays.stream(subnodes)
                    .filter(s -> !s.equals(nodeUniqueID))
                    .toArray(String[]::new);
            if (subnodes.length == 0) {
                DocumentsContract.deleteDocument(
                        this.context.getContentResolver(),
                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId)
                );
            } else {
                try (
                        OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId), "wt");
                        PrintWriter pw = new PrintWriter(os)) {
                    pw.println(String.join(",", subnodes));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_open_multi_database_file, filename));
        }
    }

    /**
     * Removes all unique ID of the nodes in the list from the bookmarks
     * @param nodeUniqueIDs list of node unique IDs to remove from bookmarks
     */
    private void removeNodesFromBookmarks(List<String> nodeUniqueIDs) {
        try {
            String lstFileDocumentId = null;
            try (Cursor cursor = this.getNodeChildrenCursorSaf(DocumentsContract.getTreeDocumentId(this.mainFolderUri))) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals("bookmarks.lst")) {
                        lstFileDocumentId = cursor.getString(0);
                        break;
                    }
                }
            }
            // Check if *.lst file was found. The bookmarks.lst file might be missing if there were no bookmarks
            if (lstFileDocumentId == null) {
                return;
            }
            String[] subnodes = null;
            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId))) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                if (line != null) {
                    subnodes = line.split(",");
                }
            }
            if (subnodes == null) {
                this.displayToast(this.context.getString(R.string.toast_error_failed_to_remove_nodes_from_bookmarks));
                return;
            }
            subnodes = Arrays.stream(subnodes)
                    .filter(s -> !nodeUniqueIDs.contains(s))
                    .toArray(String[]::new);
            if (subnodes.length == 0) {
                DocumentsContract.deleteDocument(
                        this.context.getContentResolver(),
                        DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId)
                );
            } else {
                try (
                        OutputStream os = this.context.getContentResolver().openOutputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, lstFileDocumentId), "wt");
                        PrintWriter pw = new PrintWriter(os)) {
                    pw.println(String.join(",", subnodes));
                }
            }
        } catch (IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_remove_nodes_from_bookmarks));
        }
    }

    /**
     * Creates ArrayList of ScNode objects from all the nodes in provided NodeList
     * @param nodelist nodes to make ScNode object list from
     * @param isSubnode true - nodes have to be marked as subnodes, false - opposite of that
     * @return ScNode object list ready to be used in DrawerMenu
     */
    private ArrayList<ScNode> returnSubnodeArrayList(NodeList nodelist, boolean isSubnode) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodelist.getLength(); i++) {
            nodes.add(this.createSingleMenuItem(nodelist.item(i), false, isSubnode));
        }
        return nodes;
    }

    /**
     * Creates ArrayList of ScNode objects from all the nodes in provided NodeList filtering out
     * nodes that are marked to be skipped in search
     * @param nodelist nodes to make ScNode object list from
     * @return ScNode object list ready to be used in DrawerMenu
     */
    private ArrayList<ScNode> returnSubnodeSearchArrayList(NodeList nodelist) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node item = nodelist.item(i);
            if (item.getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("0")) {
                nodes.add(this.createSingleMenuItem(item, false, false));
            }
            if (item.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("0")) {
                nodes.addAll(this.returnSubnodeSearchArrayList(item.getChildNodes()));
            }
        }
        return nodes;
    }

    /**
     * Writes XML document to output stream. Used to save created or updated node data to permanent storage
     * @param node document to write
     * @param outputStream output stream to write the file into
     */
    private void saveChanges(Node node, OutputStream outputStream) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(node);
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(dSource, result);
        } catch (TransformerException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_database_changes));
        }
    }

    /**
     * Writes drawerMenu global variable to drawer_menu.xml file
     */
    private void saveDrawerMenuToStorage() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(this.drawerMenu);
            OutputStream fileOutputStream = new FileOutputStream(new File(this.context.getFilesDir(), "drawer_menu.xml"));
            StreamResult result = new StreamResult(fileOutputStream);  // To save it in the Internal Storage
            transformer.transform(dSource, result);
            fileOutputStream.close();
        } catch (TransformerException | IOException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_database_changes));
        }
    }

    /**
     * Searches through content of all the node that are currently in drawer_menu.xml
     * @param query string to search for
     * @return all the matches to the search in ScSearchNode objects
     */
    private ArrayList<ScSearchNode> searchAllNodes(String query) {
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        NodeList nodeList = this.drawerMenu.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            try (Cursor cursor = this.getNodeChildrenCursor(nodeList.item(i))) {
                while (cursor.moveToNext()) {
                    if (!cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR) && cursor.getString(2).equals("node.xml")) {
                        try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)))) {
                            Node node = this.documentBuilder.parse(is).getElementsByTagName("node").item(0);
                            boolean hasSubnodes = nodeList.item(i).getAttributes().getNamedItem("has_subnodes").getNodeValue().equals("1");
                            ScSearchNode scSearchNode = this.findInNode(node, query, hasSubnodes, hasSubnodes, !hasSubnodes);
                            if (scSearchNode != null) {
                                searchResult.add(scSearchNode);
                            }
                        } catch (IOException | SAXException e) {
                            this.displayToast(this.context.getString(R.string.toast_error_while_searching));
                        }
                        break;
                    }
                }
            }
        }
        return searchResult;
    }

    /**
     * Searches through content of all the node that are currently in drawer_menu.xml and not
     * marked to be skipped in searches
     * @param nodeList node list to filter the nodes that needs to be searched and search them
     * @param query string to search for
     * @return all the matches to the search in ScSearchNode objects
     */
    private ArrayList<ScSearchNode> searchNodesSkippingExcluded(NodeList nodeList, String query) {
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node drawerMenuItem = nodeList.item(i);
            boolean hasSubnodes = drawerMenuItem.getAttributes().getNamedItem("has_subnodes").getNodeValue().equals("1");
            boolean noSearchMe = drawerMenuItem.getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("1");
            boolean noSearchCh = drawerMenuItem.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("1");
            if (!noSearchMe) {
                try (Cursor cursor = this.getNodeChildrenCursor(nodeList.item(i))) {
                    while (cursor.moveToNext()) {
                        if (!cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR) && cursor.getString(2).equals("node.xml")) {
                            try (InputStream is = this.context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0)))) {
                                Node node = this.documentBuilder.parse(is).getElementsByTagName("node").item(0);
                                ScSearchNode scSearchNode = this.findInNode(node, query, hasSubnodes, hasSubnodes, !hasSubnodes);
                                if (scSearchNode != null) {
                                    searchResult.add(scSearchNode);
                                }
                            } catch (IOException | SAXException e) {
                                this.displayToast(this.context.getString(R.string.toast_error_while_searching));
                            }
                            break;
                        }
                    }
                }
            }
            if (!noSearchCh) {
                searchResult.addAll(this.searchNodesSkippingExcluded(drawerMenuItem.getChildNodes(), query));
            }
        }
        return searchResult;
    }

    /**
     * Loads data from drawer_menu.xml file into drawerMenu global variable
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    public void setDrawerMenu() throws IOException, SAXException {
        try (InputStream is = new FileInputStream(new File(this.context.getFilesDir(), "drawer_menu.xml"))) {
            this.drawerMenu = documentBuilder.parse(is);
        }
    }
}