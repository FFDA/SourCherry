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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLReader {
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

    public ArrayList<String[]> getAllNodes() {
        // Returns all the node from the document
        // Used for the search/filter in the drawer menu
        NodeList nodeList = this.doc.getElementsByTagName("node");
        ArrayList<String[]> nodes = returnSubnodeArrayList(nodeList, "false");
        return nodes;
    }

    public ArrayList<String[]> getMainNodes() {
        // Returns main nodes from the document
        // Used to display menu when app starts

        ArrayList<String[]> nodes = new ArrayList<>();

        NodeList nodeList = this.doc.getElementsByTagName("cherrytree");
        NodeList mainNodeList = nodeList.item(0).getChildNodes();

        nodes = returnSubnodeArrayList(mainNodeList, "false");

        return nodes;
    }

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
        // holds individual menu items.

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

    private String[] createParentNode(Node parentNode) {
        // Creates and returns the node that will be added to the node array as parent node
        String parentNodeName = parentNode.getAttributes().getNamedItem("name").getNodeValue();
        String parentNodeUniqueID = parentNode.getAttributes().getNamedItem("unique_id").getNodeValue();
        String parentNodeHasSubnode = String.valueOf(hasSubnodes(parentNode));
        String parentNodeIsParent = "true";
        String parentNodeIsSubnode = "false";
        String[] node = {parentNodeName, parentNodeUniqueID, parentNodeHasSubnode, parentNodeIsParent, parentNodeIsSubnode};
        return node;
    }

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

                        nodeContentStringBuilder.insert(charOffset, getImageSpan(currentNode));
                    } else if (currentNodeType.equals("table")) {
                        int charOffset = getCharOffset(currentNode) + totalCharOffset; // Place where SpannableStringBuilder will be split
                        CharSequence[] cellMaxMin = getTableMaxMin(currentNode);
                        nodeContentStringBuilder.insert(charOffset," "); // Adding space for formatting reason
                        ArrayList<CharSequence[]> currentTable = new ArrayList<>(); // ArrayList with all the data from the table that will added to nodeTables
                        currentTable.add(new CharSequence[]{"table", String.valueOf(charOffset), cellMaxMin[0], cellMaxMin[1]}); // Values of the table. There aren't any table data in this line
                        NodeList tableRowsNodes = currentNode.getChildNodes(); // All the rows of the table. There are empty text nodes that has to be filered out
                        for (int row = 0; row < tableRowsNodes.getLength(); row++) {
                            if (tableRowsNodes.item(row).getNodeType() == 1) {
                                currentTable.add(getTableRow(tableRowsNodes.item(row)));
                            }
                        }
                        nodeTables.add(currentTable);
                    }
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
                String foregroundColorOriginal = nodeAttributes.item(i).getTextContent();
                // Creating a normal HEX color code, because XML document has strange one with 12 symbols
                StringBuilder colorCode = new StringBuilder();
                colorCode.append("#");
                colorCode.append(foregroundColorOriginal.substring(1,3));
                colorCode.append(foregroundColorOriginal.substring(5,7));
                colorCode.append(foregroundColorOriginal.substring(9,11));
                //
                ForegroundColorSpan fcs = new ForegroundColorSpan(Color.parseColor(colorCode.toString()));
                formattedNodeText.setSpan(fcs,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (attribute.equals("link")) {
                URLSpan us = new URLSpan(node.getTextContent());
                formattedNodeText.setSpan(us,0, formattedNodeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return formattedNodeText;
    }

    public SpannableStringBuilder makeFormattedCodebox(Node node) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        // Formatting isn't based on nodes attributes, because all codebox'es will look the same
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

    public SpannableStringBuilder getImageSpan(Node node) {
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

    public CharSequence[] getTableRow(Node row) {
        // Returns CharSequence[] of the node's "cell" element text
        NodeList rowCellNodes = row.getChildNodes();
        CharSequence[] rowCells = new CharSequence[(rowCellNodes.getLength() - 1) / 2];
        int cellCounter = 0;
        for (int cell = 0; cell < rowCellNodes.getLength(); cell++) {
            if (rowCellNodes.item(cell).getNodeType() == 1) {
                rowCells[cellCounter] = String.valueOf(rowCellNodes.item(cell).getTextContent());
                cellCounter++;
            }
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
        // They will be used to set min and max width for table ceel

        Element el = (Element) node;
        String colMax = el.getAttribute("col_max");
        String colMin = el.getAttribute("col_min");
        return new CharSequence[] {colMax, colMin};
    }

    public int[] getCodeBoxHeightWidth(Node node) {
        // This returns int[] with in codebox tag embedded box dimensios
        // They will be used to guess what type of formatting to use

        Element el = (Element) node;
        int frameHeight = Integer.valueOf(el.getAttribute("frame_height"));
        int frameWidth = Integer.valueOf(el.getAttribute("frame_width"));

        return new int[] {frameHeight, frameWidth};
    }
}
