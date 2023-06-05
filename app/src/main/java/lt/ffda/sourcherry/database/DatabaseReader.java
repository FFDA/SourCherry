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

import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;

import org.w3c.dom.Node;

import java.util.ArrayList;

import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.model.ScNodeProperties;
import lt.ffda.sourcherry.model.ScSearchNode;

public interface DatabaseReader {
    /**
     * Returns all nodes from the database. Used for search/filter function in drawer menu.
     * @param noSearch true - skips all nodes that are marked not to be searched
     * @return ArrayList of all the nodes in the database.
     */
    ArrayList<ScNode> getAllNodes(boolean noSearch);

    /**
     * Returns main/first level (that do not have a parent) nodes in the database
     * @return ArrayList of main menu nodes in the database.
     */
    ArrayList<ScNode> getMainNodes();

    /**
     * Returns bookmarked nodes in the database
     * @return ArrayList of bookmarked nodes in the database.
     */
    ArrayList<ScNode> getBookmarkedNodes();

    /**
     * Returns first level subnodes of the node which nodeUniqueID is provided
     * @param nodeUniqueID unique ID of the node which subnodes to return
     * @return ArrayList of node's subnodes.
     */
    ArrayList<ScNode> getSubnodes(String nodeUniqueID);

//    ArrayList<String[]> returnSubnodeArrayList(NodeList nodeList, String isSubnode);
//    // This function scans provided NodeList and
//    // returns ArrayList with nested String Arrays that
//    // holds individual menu items

//    boolean hasSubnodes(Node node);
//    // Checks if provided node has nested "node" tag

//    String[] createParentNode(Node parentNode);
//    // Creates and returns the node that will be added to the node array as parent node

    /**
     * Checks if it is possible to go up in document's node tree from given node
     * Depending on result it will return ArrayList of subnodes of the parent node of given nodeUniqueID
     * or return main menu nodes
     * @param nodeUniqueID unique ID of the node which parent node with subnodes to return
     * @return ArrayList of node an it's subnodes.
     */
    ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID);

    /**
     * Returns single menu item with current information
     * @param nodeUniqueID unique ID of the node to find and return
     * @return Single drawer menu item.
     */
    ScNode getSingleMenuItem(String nodeUniqueID);

    /**
     * Nodes that does not have tables in them will have just one ArrayList of CharSequence[] and will start with "text" as first part of the sequence
     * Second part (1) can be added to SpannableStringBuilder that can be used in TextView without any other modifications
     * "text" CharSequence[] has images and every other elements of the node except for tables
     * If there are tables in the node it will be split into parts, each part with new table will indicated with text "table"
     * That part needs to be displayed in TableView that it could be displayed in the in in better format for the user
     * "table" CharSequence[] part has extra two fields (1 and 2) that has max and min column values extracted from table in CherryTree database
     * The last part of the CharSequence[] has headers of the table (as it is in CherryTree database)
     * @param nodeUniqueID unique ID of the node that content has to be retrieved
     * @return ArrayList of Arraylist of CharSequence[] that has to be used in combination with SpannableStringBuilder
     */
    ArrayList<ArrayList<CharSequence[]>> getNodeContent(String nodeUniqueID);

    /**
     * Rich text formatting of the node content.
     * It includes font color, strikethrough and similar formatting
     * Formatting is based on node's attributes
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that content has to be formatted to be displayed for the user
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    SpannableStringBuilder makeFormattedRichText(Node node);

//    SpannableStringBuilder makeFormattedCodebox(Node node);
//    // Returns SpannableStringBuilder that has spans marked for string formatting
//    // Formatting isn't based on nodes attributes, because all codeboxes will look the same

//    SpannableStringBuilder makeFormattedCodeNode(Node node);
//    // Returns SpannableStringBuilder that has spans marked for string formatting
//    // CodeNode is just a CodeBox that do not have height and width (dimensions)

//    SpannableStringBuilder makeImageSpan(Node node);
//    // Returns SpannableStringBuilder that has spans with images in them
//    // Images are decoded from Base64 string embedded in the tag

//    SpannableStringBuilder makeAttachedFileSpan(Node node);
//    // Returns SpannableStringBuilder that has spans with images and filename
//    // Files are decoded from Base64 string embedded in the tag

    /**
     * Returns an image of anchor in SpannableStringBuilder object.
     * Used to display anchors (links from other nodes) in the node
     * It does not respond to touches in any way
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @return SpannableStringBuilder that has an image in it
     */
    SpannableStringBuilder makeAnchorImageSpan();

    /**
     * Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
     * As in CherryTree it's foreground color #07841B
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeUniqueID unique ID of the node that the link has to load
     * @return ClickableSpan that touched by user will load the other node
     */
    ClickableSpan makeAnchorLinkSpan(String nodeUniqueID);

    /**
     * Creates and returns a span for a link to external file or folder
     * When user clicks on the link snackbar displays a path to the file that was saved in the original system
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param type "file" makes snackbar message indicate that external linked files is an file, anything else indicates that it is a folder
     * @param base64Filename filename of the file/folder to be displayed in the snackbar message
     * @return ClickableSpan that touched by user will display a snackbar message with a filename
     */
    ClickableSpan makeFileFolderLinkSpan(String type, String base64Filename);

    /**
     * Creates one row of the table
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param row Node object that contains one row of the table
     * @return CharSequence[] of the node's "cell" element text
     */
    CharSequence[] getTableRow(Node row);

//    int getCharOffset(Node node);
//    // Returns character offset value that is used in codebox and encoded_png tags
//    // It is needed to add text in the correct location
//    // One needs to -1 from the value to make it work
//    // I don't have and idea why

//    CharSequence[] getTableMaxMin(Node node);
//    // They will be used to set min and max width for table cell

//    int[] getCodeBoxHeightWidth(Node node);
//    // This returns int[] with in codebox tag embedded box dimensions
//    // They will be used to guess what type of formatting to use

    /**
     * Returns byte array (stream) of the embedded file in the database to be written to file or opened
     * @param nodeUniqueID unique ID of the node to which file was attached to
     * @param filename filename of the file attached to the node
     * @param time datetime of when the file was attached (saved by CherryTree)
     * @return byte[] that contains a file
     */
    byte[] getFileByteArray(String nodeUniqueID, String filename, String time);

    /**
     * Finds and extracts image from the database
     * Used in ImageViewFragment because some images can be too large to pass in the bundle
     * @param nodeUniqueID unique ID of the node in which image was embedded into
     * @param offset offset of the image in the node
     * @return byte[] that contains an image
     */
    byte[] getImageByteArray(String nodeUniqueID, String offset);

    /**
     * Sometimes, not always(!), CherryTree saves hexadecimal color values with doubled symbols
     * The same color can look like this #ffffffff0000 or like this #ffff00 in the same file
     * This function is used to constantly get the correct color hash code (made from 7 symbols)
     * that can be used to set color for the node content
     * @param originalColorCode color code extracted from the database
     * @return color hash code
     */
    String getValidColorCode(String originalColorCode);

    /**
     * Displays toast message on the main thread
     * @param message message to display in the toast
     */
    void displayToast(String message);

    /**
     * Returns an image span that is used to display a placeholder image
     * Used when cursor window is to small to get an image blob
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * It will be used during the creation of the node content as needed
     * @param type pass 0 to get broken image span, pass 1 to get a broken latex span
     * @return ImageSpan with broken image image
     */
    SpannableStringBuilder getBrokenImageSpan(int type);

    /**
     * Checks database if the node exists in it
     * @param nodeUniqueID unique ID of the node to check existence of
     * @return true - if node exists, false - if it doesn't
     */
    boolean doesNodeExist(String nodeUniqueID);

    /**
     * Returns biggest node unique ID of the database
     * Used when creating a new node
     * @return biggest node unique ID of the database
     */
    int getNodeMaxID();

    /**
     * Creates new node and writes changes to the database
     * @param nodeUniqueID unique ID of the node that new node will be created in relation with
     * @param relation relation to the node. 0 - sibling, 1 - subnode
     * @param name node name
     * @param progLang prog_lang value if the node. "custom-colors" - means rich text node, "plain-text" - plain text node and "sh" - for the rest
     * @param noSearchMe 0 - marks that node should be searched, 1 - marks that node should be excluded from the search
     * @param noSearchCh 0 - marks that subnodes of the node should be searched, 1 - marks that subnodes should be excluded from the search
     * @return Created node
     */
    ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh);

    /**
     * Checks if node with provided unique ID is bookmarked
     * @param nodeUniqueID node unique ID that need to be checked
     * @return true - node is bookmarked, false - if node is not bookmarked
     */
    boolean isNodeBookmarked(String nodeUniqueID);

    /**
     * Adds node to the bookmarks
     * @param nodeUniqueID node unique ID that has to be added to the bookmarks
     */
    void addNodeToBookmarks(String nodeUniqueID);

    /**
     * Removes node from the bookmarks
     * @param nodeUniqueID node unique ID that has to be removed from the bookmarks
     */
    void removeNodeFromBookmarks(String nodeUniqueID);

    /**
     * Checks if node is a subnode if another node
     * Not really sure if it does not return false positives
     * However all my tests worked
     * @param targetNodeUniqueID unique ID of the node that needs to be check if it's a parent node
     * @param destinationNodeUniqueID unique ID of the node that has to be check if it's a child
     * @return true - if target node is a parent of destination node
     */
    boolean areNodesRelated(String targetNodeUniqueID, String destinationNodeUniqueID);

    /**
     * Moves node to different location of the document tree
     * @param targetNodeUniqueID unique ID of the node that user chose to move
     * @param destinationNodeUniqueID unique ID of the node that has to be a parent of the target node
     */
    void moveNode(String targetNodeUniqueID, String destinationNodeUniqueID);

    /**
     * Deletes node and it subnodes from database
     * @param nodeUniqueID unique ID of the node to delete
     */
    void deleteNode(String nodeUniqueID);

    /**
     * Retrieves relevant (the ones that app displays) node properties
     * @param nodeUniqueID unique ID of the node for which properties has to be retrieved
     * @return Node properties onject
     */
    ScNodeProperties getNodeProperties(String nodeUniqueID);

    /**
     * Updates properties if the node
     * @param nodeUniqueID unique ID of the node for which properties has to be updated
     * @param name new name of the node
     * @param progLang new node type
     * @param noSearchMe 1 - to exclude node from searches, 0 - keep node searches
     * @param noSearchCh 1 - to exclude subnodes of the node from searches, 0 - keep subnodes of the node in searches
     */
    void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh);

    /**
     * Coverts content of the table row Node (part of table node) to a StringBuilder
     * used as pat of convertTableNodeContentToPlainText function
     * Data of every cell is surrounded with "|" (following CherryTree example)
     * @param tableRow table row that need to be converted
     * @return StringBuilder that can be added to the table content StringBuilder
     */
    StringBuilder convertTableRowToPlainText(Node tableRow);

    /**
     * Separator that is used for formatting of codeboxes, latex
     * it's made of 33 tilde (~) characters
     * in plain-text form
     * @return a separator
     */
    String getSeparator();

    /**
     * Checks if node's type is rich-text
     * Used while rich-text node editing is not supported
     * @param nodeUniqueID unique ID of the node to check
     * @return true - node's type rich-text, false - other types
     */
    boolean isNodeRichText(String nodeUniqueID);

    /**
     * Writes node content to database
     * Supports only plain-text, automatic-syntax-highlighting nodes
     * @param nodeUniqueID unique ID of the node
     * @param nodeContent new node content
     */
    void saveNodeContent(String nodeUniqueID, String nodeContent);

    /**
     * Search for string in the database
     * @param noSearch true - skip nodes marked excluded
     * @param search string to search in database
     * @return search ArrayList of search result objects
     */
    ArrayList<ScSearchNode> search(Boolean noSearch, String search);
}
