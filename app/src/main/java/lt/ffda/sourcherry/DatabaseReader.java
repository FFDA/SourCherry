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
    ArrayList<String[]> getAllNodes();
    // Returns all the node from the document
    // Used for the search/filter in the drawer menu

    ArrayList<String[]> getMainNodes();
    // Returns main nodes from the database
    // Used to display menu when app starts

    ArrayList<String[]> getBookmarkedNodes();
    // Returns bookmarked nodes from the document
    // Returns null if there aren't any

    ArrayList<String[]> getSubnodes(String uniqueID);
    // Returns Subnodes of the node which uniqueID is provided

//    ArrayList<String[]> returnSubnodeArrayList(NodeList nodeList, String isSubnode);
//    // This function scans provided NodeList and
//    // returns ArrayList with nested String Arrays that
//    // holds individual menu items

//    boolean hasSubnodes(Node node);
//    // Checks if provided node has nested "node" tag

//    String[] createParentNode(Node parentNode);
//    // Creates and returns the node that will be added to the node array as parent node

    ArrayList<String[]> getParentWithSubnodes(String uniqueID);
    // Checks if it is possible to go up in document's node tree from given node's uniqueID
    // Returns array with appropriate nodes

    String[] getSingleMenuItem(String uniqueNodeID);
    // Returns single menu item to be used when opening anchor links

    ArrayList<ArrayList<CharSequence[]>> getNodeContent(String uniqueID);
    // Original XML document has newline characters marked (hopefully it's the same with SQL database)
    // Returns ArrayList of SpannableStringBuilder elements

    SpannableStringBuilder makeFormattedRichText(Node node);
    // Returns SpannableStringBuilder that has spans marked for string formatting
    // Formatting made based on nodes attribute

//    SpannableStringBuilder makeFormattedCodebox(Node node);
//    // Returns SpannableStringBuilder that has spans marked for string formatting
//    // Formatting isn't based on nodes attributes, because all codebox'es will look the same

//    SpannableStringBuilder makeFormattedCodeNode(Node node);
//    // Returns SpannableStringBuilder that has spans marked for string formatting
//    // CodeNode is just a CodeBox that do not have height and width (dimensions)

//    SpannableStringBuilder makeImageSpan(Node node);
//    // Returns SpannableStringBuilder that has spans with images in them
//    // Images are decoded from Base64 string embedded in the tag

//    SpannableStringBuilder makeAttachedFileSpan(Node node);
//    // Returns SpannableStringBuilder that has spans with images and filename
//    // Files are decoded from Base64 string embedded in the tag

    SpannableStringBuilder makeAnchorImageSpan();
    // Makes an image span that displays an anchor to mark position of it.
    // It does not respond to touches in any way

    ClickableSpan makeAnchorLinkSpan(String nodeUniqueID);
    // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
    // As in CherryTree it's foreground color #07841B

    ClickableSpan makeFileFolderLinkSpan(String type, String base64Filename);
    // Creates and returns a span for a link to external file or folder
    // When user clicks on the link snackbar displays a path to the file that was saved in the original system

    CharSequence[] getTableRow(Node row);
    // Returns CharSequence[] of the node's "cell" element text

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

    byte[] getFileByteArray(String nodeUniqueID, String filename, String time);
    // Returns byte array (stream) to be written to file or opened

    String getValidColorCode(String originalColorCode);
    // Sometimes, not always(!), CherryTree saves hexadecimal color values with doubled symbols
    // some colors can look like this #ffffffff0000 while other like this #ffff00 in the same file
    // To always get normal color hash code (made from 7 symbols) is the purpose of this function
}
