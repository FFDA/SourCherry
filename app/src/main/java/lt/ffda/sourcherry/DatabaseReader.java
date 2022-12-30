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

import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;

import org.w3c.dom.Node;

import java.util.ArrayList;

public interface DatabaseReader {
    /**
     * Returns all nodes from the database. Used for search/filter function in drawer menu.
     * @param noSearch true - skips all nodes that are marked not to be searched
     * @return ArrayList of all the nodes in the database. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    ArrayList<String[]> getAllNodes(boolean noSearch);

    /**
     * Returns main/first level (that do not have a parent) nodes in the database
     * @return ArrayList of main nodes in the database. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    ArrayList<String[]> getMainNodes();

    /**
     * Returns bookmarked nodes in the database
     * @return ArrayList of bookmarked nodes in the database. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    ArrayList<String[]> getBookmarkedNodes();

    /**
     * Returns first level subnodes of the node which uniqueID is provided
     * @param uniqueID uniqueID of the node which subnodes to return
     * @return ArrayList of node's subnodes. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    ArrayList<String[]> getSubnodes(String uniqueID);

//    ArrayList<String[]> returnSubnodeArrayList(NodeList nodeList, String isSubnode);
//    // This function scans provided NodeList and
//    // returns ArrayList with nested String Arrays that
//    // holds individual menu items

//    boolean hasSubnodes(Node node);
//    // Checks if provided node has nested "node" tag

//    String[] createParentNode(Node parentNode);
//    // Creates and returns the node that will be added to the node array as parent node

    /**
     * Checks if it is possible to go up in document's node tree from given node's uniqueID
     * @param uniqueID uniqueID of the node which parent node with subnodes to return
     * @return ArrayList subnodes. They are represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    ArrayList<String[]> getParentWithSubnodes(String uniqueID);

    /**
     * Returns single menu item to be used when opening anchor links
     * @param uniqueID uniqueID of the node to find and return
     * @return Singlge drawer menu item. It is represented by String[] {name, unique_id, has_subnodes, is_parent, is_subnode}
     */
    String[] getSingleMenuItem(String uniqueID);

    /**
     * Nodes that does not have tables in them will have just one ArrayList of CharSequence[] and will start with "text" as first part of the sequence
     * Second part (1) can be added to SpannableStringBuilder that can be used in TextView without any other modifications
     * "text" CharSequence[] has images and every other elements of the node except for tables
     * If there are tables in the node it will be split into parts, each part with new table will indicated with text "table"
     * That part needs to be displayed in TableView that it could be displayed in the in in better format for the user
     * "table" CharSequence[] part has extra two fields (1 and 2) that has max and min column values extracted from table in CherryTree database
     * The last part of the CharSequence[] has headers of the table (as it is in CherryTree database)
     * @param uniqueID uniqueID of the node that content has to be retrieved
     * @return ArrayList of Arraylist of CharSequence[] that has to be used in combination with SpannableStringBuilder
     */
    ArrayList<ArrayList<CharSequence[]>> getNodeContent(String uniqueID);

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
     * Creates and returns clickable span that when touched loads another node which uniqueID was passed as an argument
     * As in CherryTree it's foreground color #07841B
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param uniqueID uniqueID of the node that the link has to load
     * @return ClickableSpan that touched by user will load the other node
     */
    ClickableSpan makeAnchorLinkSpan(String uniqueID);

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
     * @param nodeUniqueID UniqueID of the node to which file was attached to
     * @param filename filename of the file attached to the node
     * @param time datetime of when the file was attached (saved by CherryTree)
     * @return byte[] that contains a file
     */
    byte[] getFileByteArray(String nodeUniqueID, String filename, String time);

    /**
     * Finds and extracts image from the database
     * Used in ImageViewActivity because some images can be bigger that can be passed in bundle
     * @param uniqueID uniqueID of the node in which image was embedded into
     * @param offset offset of the image in the node
     * @return byte[] that contains an image
     */
    byte[] getImageByteArray(String uniqueID, String offset);

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
}
