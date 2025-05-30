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
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.model.ScNodeProperties;
import lt.ffda.sourcherry.model.ScSearchNode;
import lt.ffda.sourcherry.spans.BackgroundColorSpanCustom;
import lt.ffda.sourcherry.spans.ClickableSpanFile;
import lt.ffda.sourcherry.spans.ImageSpanFile;
import lt.ffda.sourcherry.spans.ImageSpanImage;
import lt.ffda.sourcherry.spans.MonospaceBackgroundColorSpan;
import lt.ffda.sourcherry.spans.StyleSpanBold;
import lt.ffda.sourcherry.spans.StyleSpanItalic;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import lt.ffda.sourcherry.utils.DatabaseType;

/**
 * Class that reads data from the database and provides it to MainView and its model to
 * display it to the user. Moreover, it provides methods that allows user to manipulated/edit/add
 * the data stored in the database. Most of the methods in the class has to be implemented by the
 * class that extends this one.
 */
public abstract class DatabaseReader {

    /**
     * Creates String with spans that can be inserted into node content and it will have formatting
     * for attached file. This span when saving will make the reader to save attached file in to the
     * database file.
     * @param context context of the app to get resources
     * @param filename filename of the file user chose to attach to the node
     * @param nodeUniqueID unique ID of the node to which the file has to be attached
     * @param fileUri file Uri that user wants to attach to the node
     * @return formatted String to look like attached file in the node content
     */
    public static SpannableStringBuilder createAttachFileSpan(Context context, String filename, String nodeUniqueID, String fileUri) {
        SpannableStringBuilder attachedFile = new SpannableStringBuilder();
        attachedFile.append(" "); // Needed to insert an image
        Drawable drawableAttachedFileIcon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_attachment_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpanFile attachedFileIcon = new ImageSpanFile(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        attachedFileIcon.setNodeUniqueId(nodeUniqueID);
        attachedFileIcon.setFilename(filename);
        attachedFileIcon.setFileUri(fileUri);
        attachedFile.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        attachedFile.append(filename);
        ClickableSpanFile imageClickableSpan = new ClickableSpanFile() {
            @Override
            public void onClick(@NonNull View widget) {
                // There is no need to implelent it here, because this function is called only from
                // nodeContentEditor and clicks are not detected in that fragment. It is only needed
                // for text formatting.
            }
        };
        attachedFile.setSpan(imageClickableSpan, 0, attachedFile.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return attachedFile;
    }

    /**
     * Creates a String with image span. This String can be inserted into the node to display the image.
     * @param context context of the app to get resources
     * @param uri uri of the image file on the filesystem
     * @return formatted String with image span
     */
    public static SpannableStringBuilder createImageSpan(Context context, Uri uri) throws FileNotFoundException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        SpannableStringBuilder formattedImage = new SpannableStringBuilder(" ");
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        Drawable image = new BitmapDrawable(context.getResources(), bitmap);
        image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        ImageSpanImage imageSpanImage = new ImageSpanImage(image);
        formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        if (image.getIntrinsicWidth() > width - 10) {
            // If image is wider than screen it is scaled down to fit the screen
            float scale = ((float) width / image.getIntrinsicWidth()) - (float) 0.1;
            int newWidth = (int) (image.getIntrinsicWidth() * scale);
            int newHeight = (int) (image.getIntrinsicHeight() * scale);
            image.setBounds(0, 0, newWidth, newHeight);
        }
        imageSpanImage.setSha256sum(uri.toString());
        return formattedImage;
    }

    /**
     * Adds node to the bookmarks
     * @param nodeUniqueID node unique ID that has to be added to the bookmarks
     */
    public abstract void addNodeToBookmarks(String nodeUniqueID);

    /**
     * Coverts content of the table row Node (part of table node) to a StringBuilder
     * used as pat of convertTableNodeContentToPlainText function
     * Data of every cell is surrounded with "|" (following CherryTree example)
     * @param tableRow table row that need to be converted
     * @return StringBuilder that can be added to the table content StringBuilder
     */
    public StringBuilder convertTableRowToPlainText(Node tableRow) {
        StringBuilder rowContent = new StringBuilder();
        rowContent.append("|");
        NodeList nodeList = tableRow.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("cell")) {
                rowContent.append(" ");
                rowContent.append(node.getTextContent());
                rowContent.append(" |");
            }
        }
        rowContent.append("\n");
        return rowContent;
    }

    /**
     * Creates new node and writes changes to the database
     * @param nodeUniqueID unique ID of the node that new node will be created in relation with. Pass 0 to create node in main menu
     * @param relation relation to the node. 0 - sibling, 1 - subnode
     * @param name node name
     * @param progLang prog_lang value if the node. "custom-colors" - means rich text node, "plain-text" - plain text node and "sh" - for the rest
     * @param noSearchMe 0 - marks that node should be searched, 1 - marks that node should be excluded from the search
     * @param noSearchCh 0 - marks that subnodes of the node should be searched, 1 - marks that subnodes should be excluded from the search
     * @return Created node
     */
    public abstract ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh);

    /**
     * Deletes node and it subnodes from database
     * @param nodeUniqueID unique ID of the node to delete
     */
    public abstract void deleteNode(String nodeUniqueID);

    /**
     * Displays toast message on the main thread
     * @param message message to display in the toast
     */
    public abstract void displayToast(String message);

    /**
     * Returns all nodes from the database. Used for search/filter function in drawer menu.
     * @param noSearch true - skips all nodes that are marked not to be searched
     * @return ArrayList of all the nodes in the database.
     */
    public abstract ArrayList<ScNode> getAllNodes(boolean noSearch);

    /**
     * Returns bookmarked nodes in the database
     * @return ArrayList of bookmarked nodes in the database or null if there are no bookmarks
     */
    public abstract ArrayList<ScNode> getBookmarkedNodes();

    /**
     * Returns count of how many direct subnodes a node have
     * @param node nodeto count the direct children nodes
     * @return count of direct children node
     */
    public abstract int getChildrenNodeCount(String nodeUniqueID);

    /**
     * Returns enum representig database type
     * @return database type (SQL, XML, MULTI)
     */
    public abstract DatabaseType getDatabaseType();

    /**
     * Returns byte array (stream) of the embedded file in the database to be written to file or opened
     * @param nodeUniqueID unique ID of the node to which file was attached to
     * @param filename filename of the file attached to the node
     * @param time datetime of when the file was attached (saved by CherryTree)
     * @param control control value of the file to get byte array of the right file. For XML/SQL readers it's offset and sha256sum sum of the file for Multifile database reader
     * @return input steam of the file
     */
    public abstract InputStream getFileInputStream(String nodeUniqueID, String filename, String time, String control);

    /**
     * Finds and extracts image from the database
     * Used in ImageViewFragment because some images can be too large to pass in the bundle
     * @param nodeUniqueID unique ID of the node in which image was embedded into
     * @param control control value of the image to get byte array of the right file. For XML/SQL readers it's offset and sha256sum sum of the file for Multifile database reader
     * @return input stream of the image
     */
    public abstract InputStream getImageInputStream(String nodeUniqueID, String control);

    /**
     * Returns main/first level (that do not have a parent) nodes in the database
     * @return ArrayList of main menu nodes in the database.
     */
    public abstract ArrayList<ScNode> getMainNodes();

    /**
     * Returns ScNode list that can be displayed as a DrawerMenu. Node with nodeUniqueID provided as
     * an argument will be made as the Parent node.
     * @param nodeUniqueID unique ID of the node which subnodes to return
     * @return ArrayList of node's subnodes.
     */
    public abstract ArrayList<ScNode> getMenu(String nodeUniqueID);

    /**
     * Returns biggest node unique ID of the database
     * Used when creating a new node
     * @return biggest node unique ID of the database
     */
    public abstract int getNodeMaxID();

    /**
     * Retrieves relevant (the ones that app displays) node properties
     * @param nodeUniqueID unique ID of the node for which properties has to be retrieved
     * @return Node properties object
     */
    public abstract ScNodeProperties getNodeProperties(String nodeUniqueID);

    /**
     * Checks if it is possible to go up in document's node tree from given node
     * Depending on result it will return nodeUniqueID of the parent node of given nodeUniqueID
     * or null if node is part of main menu.
     * @param nodeUniqueID unique ID of the node for wich parent node should be searched for
     * @return nodeUniqueID of the parent node or null if node is part of main menu
     */
    public abstract String getParentNodeUniqueID(String nodeUniqueID);

    /**
     * Checks if it is possible to go up in document's node tree from given node
     * Depending on result it will return ArrayList of subnodes of the parent node of given nodeUniqueID
     * or return main menu nodes
     * @param nodeUniqueID unique ID of the node which parent node with subnodes to return
     * @return ArrayList of node an it's subnodes.
     */
    public abstract ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID);

    /**
     * Separator that is used for formatting of codeboxes, latex
     * it's made of 33 tilde (~) characters
     * in plain-text form
     * @return a separator
     */
    public String getSeparator() {
        return "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";
    }

    /**
     * Returns string with all the node ID's that belonging to the node's shared group. Node have to
     * have atleast one shared node or have a master node for shared groupt to exist. If shared node
     * group does not exist returns null.
     * @param nodeUniqueID unique ID of the node to get shared group for
     * @return shared groud as a string with all the IDs seperated by comma or null
     */
    public abstract String getSharedNodesGroup(String nodeUniqueID);

    /**
     * Collects uniqueIds of the master node's shared nodes
     * @param nodeUniqueID uniqueId of the master node
     * @return ordered list of uniqueIds
     */
    public abstract List<String> getSharedNodesIds(String nodeUniqueID);

    /**
     * Returns single menu item with current information
     * @param nodeUniqueID unique ID of the node to find and return
     * @return Single drawer menu item, or null if not found
     */
    public abstract ScNode getSingleMenuItem(String nodeUniqueID);

    /**
     * Creates one row of the table
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param row Node object that contains one row of the table
     * @return CharSequence[] of the node's "cell" element text
     */
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
     * Sometimes, not always(!), CherryTree saves hexadecimal color values with doubled symbols
     * The same color can look like this #ffffffff0000 or like this #ffff00 in the same file
     * This function is used to constantly get the correct color hash code (made from 7 symbols)
     * that can be used to set color for the node content
     * @param originalColorCode color code extracted from the database
     * @return color hash code
     */
    public String getValidColorCode(String originalColorCode) {
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

    /**
     * Checks if node with provided unique ID is bookmarked
     * @param nodeUniqueID node unique ID that need to be checked
     * @return true - node is bookmarked, false - if node is not bookmarked
     */
    public abstract boolean isNodeBookmarked(String nodeUniqueID);

    /**
     * Saves node content data to MainViewModel. MainViewModel has an observer that will load the
     * data on change.
     * @param nodeUniqueID unique ID of the node that content has to be retrieved
     */
    public abstract void loadNodeContent(String nodeUniqueID);

    /**
     * Returns an image of anchor in SpannableStringBuilder object.
     * Used to display anchors (links from other nodes) in the node
     * It does not respond to touches in any way
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param anchorValue value of the anchor attribute of the encoded_png tag
     * @return SpannableStringBuilder that has an image in it
     */
    public abstract SpannableStringBuilder makeAnchorImageSpan(String anchorValue);

    /**
     * Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
     * As in CherryTree it's foreground color #07841B
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeUniqueID unique ID of the node that the link has to load
     * @param linkAnchorName name of the anchor that opened node has to show. Has no effect in SourCherry
     * @return ClickableSpan that touched by user will load the other node
     */
    public abstract ClickableSpan makeAnchorLinkSpan(String nodeUniqueID, String linkAnchorName);

    /**
     * Returns an image span that is used to display a placeholder image
     * Used when cursor window is to small to get an image blob
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * It will be used during the creation of the node content as needed
     * @param type pass 0 to get broken image span, pass 1 to get a broken latex span
     * @return ImageSpan with broken image image
     */
    public abstract ImageSpan makeBrokenImageSpan(int type);

    /**
     * Creates and returns a span for a link to external file or folder
     * When user clicks on the link snackbar displays a path to the file that was saved in the original system
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param type "file" makes snackbar message indicate that external linked files is an file, anything else indicates that it is a folder
     * @param base64Filename filename of the file/folder to be displayed in the snackbar message
     * @return ClickableSpan that touched by user will display a snackbar message with a filename
     */
    public abstract ClickableSpan makeFileFolderLinkSpan(String type, String base64Filename);

    /**
     * Rich text formatting of the node content.
     * It includes font color, strikethrough and similar formatting
     * Formatting is based on node's attributes
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param node Node object that content has to be formatted to be displayed for the user
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
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
                    BackgroundColorSpanCustom bcs = new BackgroundColorSpanCustom(Color.parseColor(backgroundColorOriginal));
                    formattedNodeText.setSpan(bcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "weight":
                    formattedNodeText.setSpan(new StyleSpanBold(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "style":
                    formattedNodeText.setSpan(new StyleSpanItalic(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "underline":
                    formattedNodeText.setSpan(new UnderlineSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "scale":
                    String scaleValue = nodeAttributes.item(i).getTextContent();
                    switch (scaleValue) {
                        case "h1":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.75f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "h2":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.50f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "h3":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.25f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "h4":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.20f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "h5":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.15f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "h6":
                            formattedNodeText.setSpan(new RelativeSizeSpan(1.10f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "small":
                            formattedNodeText.setSpan(new RelativeSizeSpan(0.75f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "sup":
                            formattedNodeText.setSpan(new RelativeSizeSpan(0.80f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedNodeText.setSpan(new SuperscriptSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "sub":
                            formattedNodeText.setSpan(new RelativeSizeSpan(0.80f), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formattedNodeText.setSpan(new SubscriptSpan(), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                    }
                    break;
                case "family":
                    formattedNodeText.setSpan(new TypefaceSpanFamily("monospace"), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    formattedNodeText.setSpan(new MonospaceBackgroundColorSpan(R.color.monospace_background), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case "link":
                    String[] attributeValue = nodeAttributes.item(i).getNodeValue().split(" ");
                    if (attributeValue[0].equals("webs")) {
                        // Making links to open websites
                        URLSpanWebs us = new URLSpanWebs(attributeValue[1]);
                        formattedNodeText.setSpan(us, 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (attributeValue[0].equals("node")) {
                        // Making links to open other nodes (Anchors)
                        String linkAnchorName = String.join(" ", Arrays.copyOfRange(attributeValue, 2, attributeValue.length));
                        formattedNodeText.setSpan(makeAnchorLinkSpan(attributeValue[1], linkAnchorName), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (attributeValue[0].equals("file") || attributeValue[0].equals("fold")) {
                        // Making links to the file or folder
                        // It will not try to open the file, but just mark it, and display path to it on original system
                        formattedNodeText.setSpan(makeFileFolderLinkSpan(attributeValue[0], attributeValue[1]), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
                case "justification":
                    String justification = nodeAttributes.item(i).getTextContent();
                    switch (justification) {
                        case "right": formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case "center": formattedNodeText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
     * Moves node to different location of the document tree
     * @param targetNodeUniqueID unique ID of the node that user chose to move
     * @param destinationNodeUniqueID unique ID of the node that has to be a parent of the target node
     */
    public abstract boolean moveNode(String targetNodeUniqueID, String destinationNodeUniqueID);

    /**
     * Removes node from the bookmarks
     * @param nodeUniqueID node unique ID that has to be removed from the bookmarks
     */
    public abstract void removeNodeFromBookmarks(String nodeUniqueID);

    /**
     * Writes node content to database
     * Supports only plain-text, automatic-syntax-highlighting nodes
     * @param nodeUniqueID unique ID of the node
     */
    public abstract void saveNodeContent(String nodeUniqueID);

    /**
     * Returns scale attribute value for the rich-text tag depending on the size change of the span
     * @param relativeSizeSpan RelativeSizeSpan from nodeContent
     * @return scale attribute value
     */
    public String saveRelativeSizeSpan(RelativeSizeSpan relativeSizeSpan) {
        float size = relativeSizeSpan.getSizeChange();
        if (size == 1.75f) {
            return "h1";
        } else if (size == 1.5f) {
            return "2";
        } else if (size == 1.25f) {
            return "h3";
        } else if (size == 1.20f) {
            return "h4";
        } else if (size == 1.15f) {
            return "h5";
        } else if (size == 1.10f) {
            return "h6";
        } else {
            // size == 0.75f
            return "small";
        }
    }

    /**
     * Search for string in the database
     * @param noSearch true - skip nodes marked excluded
     * @param query string to search in database
     * @return search ArrayList of search result objects
     */
    public abstract ArrayList<ScSearchNode> search(Boolean noSearch, String query);

    /**
     * Updates properties if the node
     * @param nodeUniqueID unique ID of the node for which properties has to be updated
     * @param name new name of the node
     * @param progLang new node type
     * @param noSearchMe 1 - to exclude node from searches, 0 - keep node searches
     * @param noSearchCh 1 - to exclude subnodes of the node from searches, 0 - keep subnodes of the node in searches
     */
    public abstract void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh);
}
