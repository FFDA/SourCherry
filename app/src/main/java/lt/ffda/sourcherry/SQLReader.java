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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

import ru.noties.jlatexmath.JLatexMathDrawable;

public class SQLReader implements DatabaseReader {
    private final SQLiteDatabase sqlite;
    private final Context context;
    private final Handler handler;
    private final String databaseUri;

    public SQLReader(String databaseUri, SQLiteDatabase sqlite, Context context, Handler handler) {
        this.databaseUri = databaseUri;
        this.context = context;
        this.sqlite = sqlite;
        this.handler = handler;
    }

    @Override
    public ArrayList<String[]> getAllNodes(boolean noSearch) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.level FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);
            ArrayList<String[]> nodes = new ArrayList<>(returnSubnodeSearchArrayList(cursor));
            cursor.close();

            return nodes;
        } else {
            Cursor cursor = this.sqlite.query("node", new String[]{"name", "node_id"}, null, null, null, null, null);

            ArrayList<String[]> nodes = returnSubnodeArrayList(cursor, "false");

            cursor.close();
            return nodes;
        }
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

    /**
     * This function scans provided Cursor to collect all the nodes from it to be displayed as subnodes in drawer menu
     * Most of the time it is used to collect information about subnodes of the node that is being opened
     * However, it can be used to create information Main menu items
     * In that case isSubnode should passed as "false"
     * If true this value will make node look indented
     * @param cursor SQL Cursor object that contains nodes from which to make a node list
     * @param isSubnode "true" - means that node is not a subnode and should not be displayed indented in the drawer menu. "false" - apposite of that
     * @return ArrayList of node's subnodes. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    public ArrayList<String[]> returnSubnodeArrayList(Cursor cursor, String isSubnode) {
        ArrayList<String[]> nodes = new ArrayList<>();

        while (cursor.moveToNext()) {
            String nameValue = cursor.getString(0);
            String uniqueID = String.valueOf(cursor.getInt(1));
            String hasSubnode = hasSubnodes(uniqueID);
            String isParent = "false"; // There is only one parent Node and its added manually in getSubNodes()
            String[] currentNodeArray = {nameValue, uniqueID, hasSubnode, isParent, isSubnode};
            nodes.add(currentNodeArray);
        }

        return nodes;
    }

    /**
     * Creates an ArrayList of String[] that can be used to display nodes during drawer menu search/filter function
     * ArrayList is creaded based on node.level value (to exclude node/subnodes from search)
     * @param cursor SQL Cursor object that contains nodes from which to make a node list
     * @return ArrayList<String[]> that contains all the nodes of the provided cursor object
     */
    public ArrayList<String[]> returnSubnodeSearchArrayList(Cursor cursor) {
        ArrayList<String[]> nodes = new ArrayList<>();

        while (cursor.moveToNext()) {
            if (cursor.getInt(2) == 0) {
                // If node and subnodes are not selected to be excluded from search
                String nameValue = cursor.getString(0);
                String uniqueID = String.valueOf(cursor.getInt(1));
                String hasSubnode = hasSubnodes(uniqueID);
                String isParent = "false"; // There are no "parent" nodes in search. All nodes displayed without indentation
                nodes.add(new String[]{nameValue, uniqueID, hasSubnode, isParent, "false"});
                if (hasSubnode.equals("true")) {
                    Cursor subCursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.level FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{uniqueID});
                    nodes.addAll(returnSubnodeSearchArrayList(subCursor));
                    subCursor.close();
                }
            } else if (cursor.getInt(2) == 1) {
                // If only node is selected to be excluded from search
                String uniqueID = String.valueOf(cursor.getInt(1));
                String hasSubnode = hasSubnodes(uniqueID);
                if (hasSubnode.equals("true")) {
                    Cursor subCursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.level FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{uniqueID});
                    nodes.addAll(returnSubnodeSearchArrayList(subCursor));
                    subCursor.close();
                }
            } else if (cursor.getInt(2) == 2) {
                // if only subnodes are selected to be excluded from search
                String nameValue = cursor.getString(0);
                String uniqueID = String.valueOf(cursor.getInt(1));
                String hasSubnode = hasSubnodes(uniqueID);
                String isParent = "false"; // There is only one parent Node and its added manually in getSubNodes()
                nodes.add(new String[]{nameValue, uniqueID, hasSubnode, isParent, "false"});
            }
        }

        return nodes;
    }

    /**
     * Checks if provided Node object has a subnode(s)
     * @param uniqueID uniqueID of the node that is being checked for subnodes
     * @return "true" (string) if node has a subnode, "false" - if not
     */
    public String hasSubnodes(String uniqueID) {
        // Checks if node with provided unique_id has subnodes
        Cursor cursor = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{uniqueID},null,null,null);

        if (cursor.getCount() > 0) {
            cursor.close();
            return "true";
        } else {
            cursor.close();
            return "false";
        }
    }

    /**
     * Parent node (top) in the drawer menu
     * Used when creating a drawer menu
     * @param uniqueID uniqueID of the node that is parent node
     * @return String[] with information about provided node. Information is as fallows: {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    public String[] createParentNode(String uniqueID) {
        // Creates and returns the node that will be added to the node array as parent node
        Cursor cursor = this.sqlite.query("node", new String[]{"name"}, "node_id=?", new String[]{uniqueID}, null, null,null);

        String parentNodeName;
        if (cursor.move(1)) { // Cursor items start at 1 not 0!!!
            parentNodeName = cursor.getString(0);
        } else {
            return null;
        }
        String parentNodeUniqueID = uniqueID;
        String parentNodeHasSubnode = hasSubnodes(parentNodeUniqueID);
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

        String nodeParentID;
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
    public String[] getSingleMenuItem(String uniqueID) {
        // Returns single menu item to be used when opening anchor links
        String[] currentNodeArray = null;
        Cursor cursor = this.sqlite.query("node", new String[]{"name"}, "node_id=?", new String[]{uniqueID}, null, null,null);
        if (cursor.move(1)) { // Cursor items starts at 1 not 0!!!
            // Node name and unique_id always the same for the node
            String nameValue = cursor.getString(0);
            if (hasSubnodes(uniqueID).equals("true")) {
                // if node has subnodes, then it has to be opened as a parent node and displayed as such
                String hasSubnode = "true";
                String isParent = "true";
                String isSubnode = "false";
                currentNodeArray = new String[]{nameValue, uniqueID, hasSubnode, isParent, isSubnode};
            } else {
                // If node doesn't have subnodes, then it has to be opened as subnode of some other node
                String hasSubnode = "false";
                String isParent = "false";
                String isSubnode = "true";
                currentNodeArray = new String[]{nameValue, uniqueID, hasSubnode, isParent, isSubnode};
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
                
                // If it is marked that node has codebox, table or image
                if (hasCodebox == 1 || hasTable == 1 || hasImage == 1) {
                    //// Building string for SQLQuery
                    // Because every element is in it own table
                    // Only the tables that are marked that node have assets will be search
                    // Only offsets will be selected and second column will be created
                    // That will have 7 (codebox), 8 (table) or 9 (image) written in it
                    StringBuilder codeboxTableImageQueryString = new StringBuilder();

                    // Depending on how many tables will be searched
                    // instances of how many time uniqueID will have to be inserted will differ
                    int queryCounter = 0; // This is the counter for that
                    if (hasCodebox == 1) {
                        // Means that node has has codeboxes in it
                        codeboxTableImageQueryString.append("SELECT offset, 7 FROM codebox WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasTable == 1) {
                        // Means that node has tables in it
                        if (hasCodebox == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT offset, 8 FROM grid WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasImage == 1) {
                        // Means that node has has images (images, anchors or files) in it
                        if (hasCodebox == 1 || hasTable == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT offset, 9 FROM image WHERE node_id=? ");
                        queryCounter++;
                    }
                    codeboxTableImageQueryString.append("ORDER BY offset ASC");

                    /// Creating the array that will be used to insert uniqueIDs
                    String[] queryArguments = new String[queryCounter];
                    Arrays.fill(queryArguments, uniqueID);
                    ///
                    ////
                    // Getting user choice how big the cursor window should be
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
                    Cursor codeboxTableImageCursor = this.sqlite.rawQuery(codeboxTableImageQueryString.toString(), queryArguments);

                    while (codeboxTableImageCursor.moveToNext()) {
                        int charOffset = codeboxTableImageCursor.getInt(0);
                        if (codeboxTableImageCursor.getInt(1) == 9) {
                            // Get image entry for current node_id and charOffset
                            Cursor imageCursor = this.sqlite.query("image", new String[]{"anchor", "filename", "time"}, "node_id=? AND offset=?", new String[]{uniqueID, String.valueOf(charOffset)}, null, null, null);
                            if (imageCursor.moveToFirst()) {
                                if (!imageCursor.getString(0).isEmpty()) {
                                    // Text in column "anchor" (0) means that this line is for anchor
                                    imageCursor.close();
                                    SpannableStringBuilder anchorImageSpan = makeAnchorImageSpan();
                                    nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                    totalCharOffset += anchorImageSpan.length() - 1;
                                    continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                }
                                if (!imageCursor.getString(1).isEmpty()) {
                                    // Text in column "filename" (1) means that this line is for file OR LaTeX formula box
                                    if (!imageCursor.getString(1).equals("__ct_special.tex")) {
                                        // If it is not LaTex file
                                        SpannableStringBuilder attachedFileSpan = makeAttachedFileSpan(uniqueID, imageCursor.getString(1), String.valueOf(imageCursor.getDouble(2)));
                                        imageCursor.close();
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                        totalCharOffset += attachedFileSpan.length() - 1;
                                        continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                    } else {
                                        // For latex boxes
                                        imageCursor.close();
                                        Cursor latexBlobCursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{uniqueID, String.valueOf(charOffset)}, null, null, null);
                                        latexBlobCursor.moveToFirst();
                                        SpannableStringBuilder latexImageSpan = makeLatexImageSpan(latexBlobCursor.getBlob(0));
                                        latexBlobCursor.close();
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, latexImageSpan);
                                        totalCharOffset += latexImageSpan.length() - 1;
                                        continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                    }
                                }
                                else {
                                    // Any other line should be an image
                                    imageCursor.close();
                                    Cursor imageBlobCursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{uniqueID, String.valueOf(charOffset)}, null, null, null);
                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        // Expands cursor window for API 28 (Android 9) and greater
                                        // This allows to display bigger images and open/save bigger files
                                        // Right now limit is 15mb
                                        ((SQLiteCursor) imageBlobCursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
                                    }  else {
                                        // Setting cursorWindow as to 2 (default android value)
                                        // Android 8 and lower versions do not have this function
                                        // It's only that error toast would show a correct size
                                        cursorWindow = 2;
                                    }
                                    try {
                                        // Tries to move to get image blob from DB. Might me too big.
                                        imageBlobCursor.moveToFirst();
                                        SpannableStringBuilder imageSpan = makeImageSpan(imageBlobCursor.getBlob(0), uniqueID, String.valueOf(charOffset)); // Blob is the image in byte[] form
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                        totalCharOffset += imageSpan.length() - 1;
                                    } catch (Exception SQLiteBlobTooBigException) {
                                        // If image blob was to big for SQL Toast error message will be displayed
                                        // And placeholder image is placed
                                        SpannableStringBuilder brokenImageSpan = getBrokenImageSpan(0);
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, brokenImageSpan);
                                        totalCharOffset += brokenImageSpan.length() - 1;
                                        this.displayToast(context.getString(R.string.toast_error_failed_to_load_image_large, cursorWindow));
                                    }
                                    imageBlobCursor.close();
                                    continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                }
                            }
                        } else if (codeboxTableImageCursor.getInt(1) == 7) {
                            // codebox row
                            // Get codebox entry for current node_id and charOffset
                            Cursor codeboxCursor = this.sqlite.query("codebox", new String[]{"txt"}, "node_id=? AND offset=?", new String[]{uniqueID, String.valueOf(charOffset)}, null, null, null);
                            if (codeboxCursor.moveToFirst()) {
                                SpannableStringBuilder codeboxText = makeFormattedCodebox(codeboxCursor.getString(0));
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, codeboxText);
                                codeboxCursor.close();
                                totalCharOffset += codeboxText.length() - 1;
                            }
                        } else if (codeboxTableImageCursor.getInt(1) == 8) {
                            // table row
                            // Get table row entry for current node_id and charOffset
                            Cursor tableCursor = this.sqlite.query("grid", new String[]{"txt", "col_min", "col_max"}, "node_id=? AND offset=?", new String[]{uniqueID, String.valueOf(charOffset)}, null, null, null);
                            if (tableCursor.moveToFirst()) {
                                int tableCharOffset = charOffset + totalCharOffset; // Place where SpannableStringBuilder will be split
                                String cellMax = tableCursor.getString(2);
                                String cellMin = tableCursor.getString(1);
                                nodeContentStringBuilder.insert(tableCharOffset, " "); // Adding space for formatting reason
                                ArrayList<CharSequence[]> currentTable = new ArrayList<>(); // ArrayList with all the data from the table that will added to nodeTables
                                currentTable.add(new CharSequence[]{"table", String.valueOf(tableCharOffset), cellMax, cellMin}); // Values of the table. There aren't any table data in this line
                                NodeList tableRowsNodes = getNodeFromString(tableCursor.getString(0), "table"); // All the rows of the table. Not like in XML database, there are not any empty text nodes to be filtered out
                                tableCursor.close();
                                for (int row = 0; row < tableRowsNodes.getLength(); row++) {
                                    currentTable.add(getTableRow(tableRowsNodes.item(row)));
                                }
                                nodeTables.add(currentTable);
                            }
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

    /**
     * Creates a codebox span from the provided nodeContent string
     * Formatting depends on new line characters nodeContent string
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeContent content of the codebox
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    public SpannableStringBuilder makeFormattedCodebox(String nodeContent) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(nodeContent);

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodebox.setSpan(tf, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (nodeContent.contains("\n")) {
            // Adds vertical line in front the paragraph, to make it stand out as quote
            QuoteSpan qs;
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
        } else {
            BackgroundColorSpan bcs = new BackgroundColorSpan(this.context.getColor(R.color.codebox_background));
            formattedCodebox.setSpan(bcs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return formattedCodebox;
    }

    /**
     * Creates SpannableStringBuilder with the content of the CodeNode
     * CodeNode is just a CodeBox that do not have height and width (dimensions)
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeContent content of the code node
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    public SpannableStringBuilder makeFormattedCodeNode(String nodeContent) {
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

    /**
     * Creates a SpannableStringBuilder with image in it
     * Image is created from byte[]
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param imageBlob byte[] that has data for the image
     * @param uniqueID uniqueID of the node that has the image embedded
     * @param imageOffset offset of the image in the node
     * @return SpannableStringBuilder that has spans with image in them
     */
    public SpannableStringBuilder makeImageSpan(byte[] imageBlob, String uniqueID, String imageOffset) {
        // Returns SpannableStringBuilder that has spans with images in them
        // Images are decoded from byte array that was passed to the function

        SpannableStringBuilder formattedImage = new SpannableStringBuilder();

        //* Adds image to the span
        try {
            formattedImage.append(" ");
            Bitmap decodedByte = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            Drawable image = new BitmapDrawable(context.getResources(),decodedByte);
            image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            ImageSpan is = new ImageSpan(image);
            formattedImage.setSpan(is, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            if (image.getIntrinsicWidth() > width) {
                // If image is wider than screen it is scaled down to fit the screen
                // otherwise it will not load/be displayed
                float scale = ((float) width / image.getIntrinsicWidth()) - (float) 0.1;
                int newWidth = (int) (image.getIntrinsicWidth() * scale);
                int newHeight = (int) (image.getIntrinsicHeight() * scale);
                image.setBounds(0, 0, newWidth, newHeight);
            }

            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting activity to view enlarged zoomable image
                    Intent displayImage = new Intent(context, ImageViewActivity.class);
                    displayImage.putExtra("type", "image");
                    displayImage.putExtra("imageNodeUniqueID", uniqueID);
                    displayImage.putExtra("imageOffset", imageOffset);
                    context.startActivity(displayImage);
                }
            };
            formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**

        } catch (Exception e) {
            // Displays a toast message and appends broken image span to display in node content
            formattedImage.append(this.getBrokenImageSpan(0));
            this.displayToast(context.getString(R.string.toast_error_failed_to_load_image));
        }
        //*

        return formattedImage;
    }

    /**
     * Creates a SpannableStringBuilder with image with drawn Latex formula in it
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param imageBlob byte[] that is actually a String that contains LaTex formula
     * @return SpannableStringBuilder that has span with Latex image in them
     */
    public SpannableStringBuilder makeLatexImageSpan(byte[] imageBlob) {
        // Image is created from byte[] that is passed as an arguments
        SpannableStringBuilder formattedLatexImage = new SpannableStringBuilder();

        //* Creates and adds image to the span
        try {
            formattedLatexImage.append(" ");
            String latexString = new String(imageBlob)
                .replace("\\documentclass{article}\n" +
                        "\\pagestyle{empty}\n" +
                        "\\usepackage{amsmath}\n" +
                        "\\begin{document}\n" +
                        "\\begin{align*}", "")
                .replace("\\end{align*}\n\\end{document}", "")
                .replaceAll("&=", "="); // Removing & sign, otherwise latex image fails to compile

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

            ImageSpan is = new ImageSpan(latexDrawable);
            formattedLatexImage.setSpan(is, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting activity to view enlarged zoomable image
                    Intent displayImage = new Intent(context, ImageViewActivity.class);
                    displayImage.putExtra("type", "latex");
                    displayImage.putExtra("latexString", latexString);
                    context.startActivity(displayImage);
                }
            };
            formattedLatexImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken latex image span to display in node content
            formattedLatexImage.append(this.getBrokenImageSpan(1));
            this.displayToast(context.getString(R.string.toast_error_failed_to_compile_latex));
        }
        //*

        return formattedLatexImage;
    }

    /**
     * Creates a clickable span that initiates a context to open/save attached file
     * Arguments that a passed to this function has to be retrieved from the appropriate tables in the database
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param uniqueID uniqueID of the node that has file attached in it
     * @param attachedFileFilename filename of the attached file
     * @param time datetime of when file was attached to the node
     * @return Clickable spannableStringBuilder that has spans with image and filename
     */
    public SpannableStringBuilder makeAttachedFileSpan(String uniqueID, String attachedFileFilename, String time) {
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
            // Launches function in MainView that checks if there is a default action in for attached files
            ((MainView) SQLReader.this.context).saveOpenFile(uniqueID, attachedFileFilename, time);
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
    public ClickableSpan makeAnchorLinkSpan(String uniqueID) {
        // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
        // As in CherryTree it's foreground color #07841B

        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) SQLReader.this.context).openAnchorLink(getSingleMenuItem(uniqueID));
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
                ((MainView) SQLReader.this.context).fileFolderLinkFilepath(new String(Base64.decode(base64Filename, Base64.DEFAULT)));
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
        // Getting user choice how big the cursor window should be
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
        try {
            // Try needed to close the cursor. Otherwise ofter return statement it won't be closed;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Expands cursor window for API 28 (Android 9) and greater
                // This allows to save/open bigger files
                ((SQLiteCursor) cursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
            } else {
                // Setting cursorWindow as to 2 (default android value)
                // Android 8 and lower versions do not have this function
                // It's only that error toast would show a correct size
                cursorWindow = 2;
            }
            cursor.move(1);
            return cursor.getBlob(0);
        } catch (Exception SQLiteBlobTooBigException) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_open_file_large, cursorWindow));
            return null;
        }finally {
            cursor.close();
        }
    }

    @Override
    public byte[] getImageByteArray(String nodeUniqueID, String offset) {
        // Returns image byte array to be displayed in ImageViewActivity because some of the images are too big to pass in a bundle
        Cursor cursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, offset}, null, null, null);
        // Getting user choice how big the cursor window should be
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
        try {
            // Try is needed to close the cursor. Otherwise after return statement it won't be closed;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Expands cursor window for API 28 (Android 9) and greater
                // This allows to display bigger images
                ((SQLiteCursor) cursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
            } else {
                // Setting cursorWindow as to 2 (default android value)
                // Android 8 and lower versions do not have this function
                // It's only that error toast would show a correct size
                cursorWindow = 2;
            }
            cursor.move(1);
            return cursor.getBlob(0);
        } catch (Exception SQLiteBlobTooBigException) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_load_image_large, cursorWindow));
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * SQL Database has a XML document inserted in to it in a form of the String
     * With all the tags an attributes the same way as in XML document
     * So SQL document is just a XML document with extra steps
     * @param nodeString String object with all the information of the node or it's table
     * @param type node type to select ("node" or "table")
     * @return NodeList object with content of the node
     */
    public NodeList getNodeFromString(String nodeString, String type) {
        try {
            Document doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(nodeString.getBytes(StandardCharsets.UTF_8))
                    );
            // It seems that there is always just one tag (<node> or <table>), so returning just the first one in the NodeList
            return doc.getElementsByTagName(type).item(0).getChildNodes();
        } catch (Exception e) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_convert_string_to_nodelist));
        }

        return null;
    }

    @Override
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

    @Override
    public void displayToast(String message) {
        // Displays a toast on main thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SQLReader.this.context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public SpannableStringBuilder getBrokenImageSpan(int type) {
        // Returns an image span that is used to display as placeholder image
        // used when cursor window is to small to get an image blob
        // pass 0 to get broken image span, pass 1 to get broken latex span
        SpannableStringBuilder brokenSpan = new SpannableStringBuilder();
        brokenSpan.append(" ");
        Drawable drawableBrokenImage;
        if (type == 0) {
            drawableBrokenImage = this.context.getDrawable(R.drawable.ic_outline_broken_image_48);
        } else {
            drawableBrokenImage = this.context.getDrawable(R.drawable.ic_outline_broken_latex_48);
        }
        //// Inserting image

        drawableBrokenImage.setBounds(0,0, drawableBrokenImage.getIntrinsicWidth(), drawableBrokenImage.getIntrinsicHeight());
        ImageSpan brokenImage = new ImageSpan(drawableBrokenImage, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        brokenSpan.setSpan(brokenImage,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ////

        return brokenSpan;
    }

    @Override
    public boolean doesNodeExist(String uniqueID) {
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name FROM node WHERE node.node_id=?", new String[]{uniqueID});
        if (cursor.getCount() == 1) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    @Override
    public String[] createNewNode(String uniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh){
        //Place holder
        return new String[]{};
    }

    @Override
    public boolean isNodeBookmarked(String nodeUniqueID) {
        //Placeholder;
        return false;
    }

    @Override
    public void addNodeToBookmarks(String nodeUniqueID){
        //Placeholder
    }

    @Override
    public void removeNodeFromBookmarks(String nodeUniqueID) {
    }
}
