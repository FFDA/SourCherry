/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

public class SQLReader implements DatabaseReader {
    private SQLiteDatabase sqlite;
    private Context context;
    private FragmentManager fragmentManager;
    private Handler handler;

    public SQLReader(SQLiteDatabase sqlite, Context context, FragmentManager fragmentManager, Handler handler) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.sqlite = sqlite;
        this.handler = handler;
    }

    @Override
    public ArrayList<String[]> getAllNodes(boolean noSearch) {
        // Returns all the node from the document
        // Used for the search/filter in the drawer menu
        Cursor cursor = this.sqlite.query("node", new String[]{"name", "node_id"}, null, null, null, null, null);

        ArrayList<String[]> nodes = returnSubnodeArrayList(cursor, "false");

        cursor.close();
        return nodes;
    }

    @Override
    public ArrayList<String[]> getMainNodes() {
        // Returns main nodes from the database
        // Used to display menu when app starts
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);
        ArrayList<String[]> nodes = returnSubnodeArrayList(cursor, "false");

        cursor.close();
        return nodes;
    }

    @Override
    public ArrayList<String[]> getBookmarkedNodes() {
        // Returns bookmarked nodes from the document
        // Returns null if there aren't any
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id FROM node INNER JOIN bookmark ON node.node_id=bookmark.node_id ORDER BY bookmark.sequence ASC", null);
        if(cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        ArrayList<String[]> nodes = returnSubnodeArrayList(cursor, "false");

        cursor.close();
        return nodes;
    }

    @Override
    public ArrayList<String[]> getSubnodes(String uniqueID) {
        // Returns Subnodes of the node which uniqueID is provided
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{uniqueID});
        ArrayList<String[]> nodes = returnSubnodeArrayList(cursor, "true");

        nodes.add(0, createParentNode(uniqueID));

        cursor.close();
        return nodes;
    }

    public ArrayList<String[]> returnSubnodeArrayList(Cursor cursor, String isSubnode) {
        // This function scans provided NodeList and
        // returns ArrayList with nested String Arrays that
        // holds individual menu items
        ArrayList<String[]> nodes = new ArrayList<>();

        while (cursor.moveToNext()) {
            String nameValue = cursor.getString(0);
            String uniqueID = String.valueOf(cursor.getInt(1));
            String hasSubnode = String.valueOf(hasSubnodes(uniqueID));
            String isParent = "false"; // There is only one parent Node and its added manually in getSubNodes()
            String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, isSubnode};
            nodes.add(currentNodeArray);
        }

        return nodes;
    }

    public boolean hasSubnodes(String uniqueNodeID) {
        // Checks if node with provided unique_id has subnodes
        Cursor cursor = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{uniqueNodeID},null,null,null);

        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public String[] createParentNode(String uniqueNodeID) {
        // Creates and returns the node that will be added to the node array as parent node
        Cursor cursor = this.sqlite.query("node", new String[]{"name"}, "node_id=?", new String[]{uniqueNodeID}, null, null,null);

        String parentNodeName = "";
        if (cursor.move(1)) { // Cursor items start at 1 not 0!!!
            parentNodeName = cursor.getString(0);
        } else {
            return null;
        }
        String parentNodeUniqueID = uniqueNodeID;
        String parentNodeHasSubnode = String.valueOf(hasSubnodes(parentNodeUniqueID));
        String parentNodeIsParent = "true";
        String parentNodeIsSubnode = "false";
        String[] node = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};

        cursor.close();

        return node;
    }

    @Override
    public ArrayList<String[]> getParentWithSubnodes(String uniqueID) {
        // Checks if it is possible to go up in document's node tree from given node's uniqueID
        // Returns array with appropriate nodes
        ArrayList<String[]> nodes = null;

        String nodeParentID = "-1";
        Cursor cursor = this.sqlite.query("children", new String[]{"father_id"}, "node_id=?", new String[]{uniqueID}, null, null, null);
        if (cursor.move(1)) { // Cursor items start at 1 not 0!!!
            nodeParentID = cursor.getString(0);
            cursor.close();
            if (nodeParentID.equals("0")) {
                nodes = getMainNodes();
            } else {
                cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{nodeParentID});
                nodes = returnSubnodeArrayList(cursor, "true");
                nodes.add(0, createParentNode(nodeParentID));
            }
        }

        cursor.close();
        return nodes;
    }

    @Override
    public String[] getSingleMenuItem(String uniqueNodeID) {
        // Returns single menu item to be used when opening anchor links
        String[] currentNodeArray = null;
        Cursor cursor = this.sqlite.query("node", new String[]{"name"}, "node_id=?", new String[]{uniqueNodeID}, null, null,null);
        if (cursor.move(1)) { // Cursor items starts at 1 not 0!!!
            // Node name and unique_id always the same for the node
            String nameValue = cursor.getString(0);
            if (hasSubnodes(uniqueNodeID)) {
                // if node has subnodes, then it has to be opened as a parent node and displayed as such
                String hasSubnode = "true";
                String isParent = "true";
                String isSubnode = "false";
                currentNodeArray = new String[]{nameValue, uniqueNodeID, hasSubnode, isParent, isSubnode};
            } else {
                // If node doesn't have subnodes, then it has to be opened as subnode of some other node
                String hasSubnode = "false";
                String isParent = "false";
                String isSubnode = "true";
                currentNodeArray = new String[]{nameValue, uniqueNodeID, hasSubnode, isParent, isSubnode};
            }
        }
        cursor.close();
        return currentNodeArray;
    }

    @Override
    public ArrayList<ArrayList<CharSequence[]>> getNodeContent(String uniqueID) {
        // Original XML document has newline characters marked (hopefully it's the same with SQL database)
        // Returns ArrayList of SpannableStringBuilder elements

        ArrayList<ArrayList<CharSequence[]>> nodeContent = new ArrayList<>(); // The one that will be returned

        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary for text, codebox, image formatting
        ArrayList<ArrayList<CharSequence[]>> nodeTables = new ArrayList<>(); // Temporary for table storage

        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        ////

        Cursor cursor = this.sqlite.query("node", null, "node_id=?", new String[]{uniqueID}, null, null, null); // Get node table entry with uniqueID
        if (cursor.move(1)) { // Cursor items starts at 1 not 0!!!
            // syntax is the same as prog_lang attribute in XML database
            // It is used to set formatting for the node and separate between node types (Code Node)
            // The same attribute is used for codeboxes
            String nodeSyntax = cursor.getString(3);

            if (nodeSyntax.equals("custom-colors")) {
                // This is formatting for Rich Text and Plain Text nodes
                NodeList nodeContentNodeList = getNodeFromString(cursor.getString(2), "node"); // Gets all the subnodes/childnodes of selected node
                for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                    // Loops through nodes of selected node
                    Node currentNode = nodeContentNodeList.item(x);
                    if (currentNode.hasAttributes()) {
                        nodeContentStringBuilder.append(makeFormattedRichText(currentNode));
                    } else {
                        nodeContentStringBuilder.append(currentNode.getTextContent());
                    }
                }

                int hasCodebox = cursor.getInt(7);
                int hasTable = cursor.getInt(8);
                int hasImage = cursor.getInt(9);
                
                // If any it is marked that node has codebox, table or image
                if (hasCodebox == 1 || hasTable == 1 || hasImage == 1) {
                    //// Building string for SQLQuery
                    // Because every element is in it own table
                    // Only the ones that actually are in the table will be searched
                    // hopefully
                    // During the selection eleventh (index: 10) column will be added.
                    // That will have 7 (codebox), 8 (table) or 9 (image) written to it. It should make separating which line come from this table easier
                    StringBuilder codeboxTableImageQueryString = new StringBuilder();

                    // Depending on how many tables will be searched
                    // instances of how many time uniqueID will have to be inserted will differ
                    int queryCounter = 0; // This is the counter for that
                    if (hasCodebox == 1) {
                        // Means that node has has codeboxes in it
                        codeboxTableImageQueryString.append("SELECT *, 7 FROM codebox WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasTable == 1) {
                        // Means that node has tables in it
                        if (hasCodebox == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT *, null, null, null, null, 8 FROM grid WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasImage == 1) {
                        // Means that node has has images (images, anchors or files) in it
                        if (hasCodebox == 1 || hasTable == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT *, null, null, 9 FROM image WHERE node_id=? ");
                        queryCounter++;
                    }
                    codeboxTableImageQueryString.append("ORDER BY offset ASC");

                    /// Creating the array that will be used to insert uniqueIDs
                    String[] queryArguments = new String[queryCounter];
                    Arrays.fill(queryArguments, uniqueID);
                    ///
                    ////
                    
                    Cursor codeboxTableImageCursor = this.sqlite.rawQuery(codeboxTableImageQueryString.toString(), queryArguments);
                    
                    while (codeboxTableImageCursor.moveToNext()) {
                        int charOffset = codeboxTableImageCursor.getInt(1);
                        if (codeboxTableImageCursor.getInt(10) == 9) {
                            // If 8th or 9th columns are null then this row is from image table;
                            if (!codeboxTableImageCursor.getString(3).isEmpty()) {
                                // Text in column 3 means that this line is for anchor
                                SpannableStringBuilder anchorImageSpan = makeAnchorImageSpan();
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                totalCharOffset += anchorImageSpan.length() - 1;
                                continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                            }
                            if (!codeboxTableImageCursor.getString(5).isEmpty()) {
                                // Text in column 5 means that this line is for file OR LaTeX formula box
                                if (!codeboxTableImageCursor.getString(5).equals("__ct_special.tex")) {
                                    // If it is not LaTex file
                                    SpannableStringBuilder attachedFileSpan = makeAttachedFileSpan(codeboxTableImageCursor.getString(5), String.valueOf(codeboxTableImageCursor.getDouble(7)));
                                    nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                    totalCharOffset += attachedFileSpan.length() - 1;
                                    continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                }
                            } else {
                                // Any other line should be an image
                                SpannableStringBuilder imageSpan = makeImageSpan(codeboxTableImageCursor.getBlob(4)); // Blob is the image in byte[] form
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                totalCharOffset += imageSpan.length() - 1;
                                continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                            }
                        } else if (codeboxTableImageCursor.getInt(10) == 7) {
                            // codebox row
                            SpannableStringBuilder codeboxText = makeFormattedCodebox(codeboxTableImageCursor.getString(3), codeboxTableImageCursor.getInt(5), codeboxTableImageCursor.getInt(6));
                            nodeContentStringBuilder.insert(charOffset + totalCharOffset, codeboxText);
                            totalCharOffset += codeboxText.length() - 1;
                        } else if (codeboxTableImageCursor.getInt(10) == 8) {
                            // table row
                            int tableCharOffset = charOffset + totalCharOffset; // Place where SpannableStringBuilder will be split
                            String cellMax = codeboxTableImageCursor.getString(5);
                            String cellMin = codeboxTableImageCursor.getString(4);
                            nodeContentStringBuilder.insert(tableCharOffset, " "); // Adding space for formatting reason
                            ArrayList<CharSequence[]> currentTable = new ArrayList<>(); // ArrayList with all the data from the table that will added to nodeTables
                            currentTable.add(new CharSequence[]{"table", String.valueOf(tableCharOffset), cellMax, cellMin}); // Values of the table. There aren't any table data in this line
                            NodeList tableRowsNodes = getNodeFromString(codeboxTableImageCursor.getString(3), "table"); // All the rows of the table. Not like in XML database, there are not any empty text nodes to be filtered out
                            for (int row = 0; row < tableRowsNodes.getLength(); row++) {
                                currentTable.add(getTableRow(tableRowsNodes.item(row)));
                            }
                            nodeTables.add(currentTable);
                        }
                    }
                    codeboxTableImageCursor.close();
                }
            } else if (nodeSyntax.equals("plain-text")) {
                // Plain text node does not have any formatting and has not node embedded in to it
                nodeContentStringBuilder.append(cursor.getString(2));
            } else {
                // Node is Code Node. It's just a big CodeBox with no dimensions
                nodeContentStringBuilder.append(makeFormattedCodeNode(cursor.getString(2)));
            }
        }

        cursor.close();

        int subStringStart = 0; // Holds start from where SpannableStringBuilder has to be split from

        if (nodeTables.size() > 0) {
            // If there are at least one table in the node
            // SpannableStringBuilder that holds are split in to parts
            // After each text array table array is added
            for (ArrayList<CharSequence[]> table: nodeTables) {
                // Getting table's char_offset that was embedded into CharArray
                // It will be used to split the text in appropriate parts
                int charOffset = Integer.parseInt((String) table.get(0)[1]);
                //

                // Creating text part of this iteration
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, charOffset);
                subStringStart = charOffset; // Next stirng will be cut starting from this offset (previous end)
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

            switch (attribute) {
                case "strikethrough":
                    StrikethroughSpan sts = new StrikethroughSpan();
                    formattedNodeText.setSpan(sts,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "foreground":
                    String foregroundColorOriginal = getValidColorCode(nodeAttributes.item(i).getTextContent()); // Extracting foreground color code from the tag
                    ForegroundColorSpan fcs = new ForegroundColorSpan(Color.parseColor(foregroundColorOriginal));
                    formattedNodeText.setSpan(fcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "background":
                    String backgroundColorOriginal = getValidColorCode(nodeAttributes.item(i).getTextContent()); // Extracting background color code from the tag
                    BackgroundColorSpan bcs = new BackgroundColorSpan(Color.parseColor(backgroundColorOriginal));
                    formattedNodeText.setSpan(bcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "weight":
                    StyleSpan boldStyleSpan = new StyleSpan(Typeface.BOLD);
                    formattedNodeText.setSpan(boldStyleSpan, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "style":
                    StyleSpan italicStyleSpan = new StyleSpan(Typeface.ITALIC);
                    formattedNodeText.setSpan(italicStyleSpan, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "underline":
                    formattedNodeText.setSpan(new UnderlineSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "scale":
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
                    break;
                case "family":
                    TypefaceSpan tf = new TypefaceSpan("monospace");
                    formattedNodeText.setSpan(tf, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "link":
                    String[] attributeValue = nodeAttributes.item(i).getNodeValue().split(" ");
                    if (attributeValue[0].equals("webs")) {
                        // Making links to open websites
                        URLSpan us = new URLSpan(attributeValue[1]);
                        formattedNodeText.setSpan(us, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (attributeValue[0].equals("node")) {
                        // Making links to open other nodes (Anchors)
                        formattedNodeText.setSpan(makeAnchorLinkSpan(attributeValue[1]), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (attributeValue[0].equals("file") || attributeValue[0].equals("fold")) {
                        // Making links to the file or folder
                        // It will not try to open the file, but just mark it, and display path to it on original system
                        formattedNodeText.setSpan(this.makeFileFolderLinkSpan(attributeValue[0], attributeValue[1]), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
                case "justification":
                    String justification = nodeAttributes.item(i).getTextContent();
                    switch (justification) {
                        case "right":   formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "center":  formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                    }
                    break;
                case "indent":
                    int indent = Integer.parseInt(nodeAttributes.item(i).getTextContent()) * 40;
                    LeadingMarginSpan.Standard lmss = new LeadingMarginSpan.Standard(indent);
                    formattedNodeText.setSpan(lmss, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
            }
        }
        return formattedNodeText;
    }

    public SpannableStringBuilder makeFormattedCodebox(String nodeContent, int frameWidth, int frameHeight ) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting depends on Codebox'es height and width
        // They are passed as arguments
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(nodeContent);

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodebox.setSpan(tf, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // This part of codebox formatting depends on size of the codebox
        // Because if user made a small codebox it might have text in front or after it
        // For this reason some of the formatting can't be spanned over all the line
        if (frameWidth < 200 && frameHeight < 30) {
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

    public SpannableStringBuilder makeFormattedCodeNode(String nodeContent) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // CodeNode is just a CodeBox that do not have height and width (dimensions)

        SpannableStringBuilder formattedCodeNode = new SpannableStringBuilder();
        formattedCodeNode.append(nodeContent);

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

    public SpannableStringBuilder makeImageSpan(byte[] imageBlob) {
        // Returns SpannableStringBuilder that has spans with images in them
        // Images are decoded from byte array that was passed to the function

        SpannableStringBuilder formattedImage = new SpannableStringBuilder();

        formattedImage.append(" ");

        //// Adds image to the span
        try {
            Bitmap decodedByte = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            Drawable image = new BitmapDrawable(context.getResources(),decodedByte);
            image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            ImageSpan is = new ImageSpan(image);
            formattedImage.setSpan(is, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //// Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting activity to view enlarged  zoomable image
                    Intent displayImage = new Intent(context, ImageViewActivity.class);
                    displayImage.putExtra("imageByteArray", imageBlob);
                    context.startActivity(displayImage);
                }
            };
            formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            ////

        } catch (Exception e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SQLReader.this.context, R.string.toast_error_failed_to_load_image, Toast.LENGTH_SHORT).show();
                }
            });
        }
        ////

        return formattedImage;
    }

    public SpannableStringBuilder makeAttachedFileSpan(String attachedFileFilename, String time) {
        // Returns SpannableStringBuilder that has spans with images and filename
        // Files are decoded from Base64 string embedded in the tag

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

                // Setting up to send arguments to Dialog Fragment
                Bundle bundle = new Bundle();
                bundle.putString("filename", attachedFileFilename);
                bundle.putString("time", time);

                SaveOpenDialogFragment saveOpenDialogFragment = new SaveOpenDialogFragment();
                saveOpenDialogFragment.setArguments(bundle);
                saveOpenDialogFragment.show(SQLReader.this.fragmentManager, "saveOpenDialog");
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

        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) SQLReader.this.context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                ds.setColor(context.getColor(R.color.link_anchor));
                ds.setUnderlineText(true);
            }
        };
    }

    @Override
    public ClickableSpan makeFileFolderLinkSpan(String type, String base64Filename) {
        // Creates and returns a span for a link to external file or folder
        // When user clicks on the link snackbar displays a path to the file that was saved in the original system

        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                Snackbar.make(((MainView) SQLReader.this.context).findViewById(R.id.content_fragment_linearlayout), new String(Base64.decode(base64Filename, Base64.DEFAULT)), Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_dismiss_action, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        })
                        .show();
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
    }

    @Override
    public CharSequence[] getTableRow(Node row) {
        // Returns CharSequence[] of the node's "cell" element text
        NodeList rowCellNodes = row.getChildNodes();
        CharSequence[] rowCells = new CharSequence[rowCellNodes.getLength()];
        for (int cell = 0; cell < rowCellNodes.getLength(); cell++) {
                rowCells[cell] = String.valueOf(rowCellNodes.item(cell).getTextContent());
        }
        return rowCells;
    }

    @Override
    public byte[] getFileByteArray(String uniqueID, String filename, String time) {
        // Returns byte array (stream) to be written to file or opened

        Cursor cursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND filename=? AND time=?", new String[]{uniqueID, filename, time}, null, null, null);
        try {
            // Try needed to close the cursor. Otherwise ofter return statement it won't be closed;
            cursor.move(1);
            return cursor.getBlob(0);
        } finally {
            cursor.close();
        }
    }

    public NodeList getNodeFromString(String nodeString, String type) {
        // SQL Database has a XML document inserted in to it
        // XML document is for node content formatting
        // So SQL document is just a XML document with extra steps
        // For that reason node with all the content has to be separated from other tags for processing
        // Variable type is just node type to select (node or table values)

        try {
            Document doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(nodeString.getBytes(StandardCharsets.UTF_8))
                    );
            // It seems that there is always just one tag (<node> or <table>), so returning just the first one in the NodeList
            return doc.getElementsByTagName(type).item(0).getChildNodes();
        } catch (Exception e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SQLReader.this.context, R.string.toast_error_failed_to_convert_string_to_nodelist, Toast.LENGTH_SHORT).show();
                }
            });
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
