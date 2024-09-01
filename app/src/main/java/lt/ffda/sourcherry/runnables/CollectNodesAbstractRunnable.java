/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.runnables;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public abstract class CollectNodesAbstractRunnable {

    /**
     * Initiates the recursive scan of all the folders under the Multifile databases root directory
     * @return Document object with all the nodes of the database in tree structure
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    public abstract Document getDrawerMenuTree() throws IOException, SAXException;

    /**
     * Return SAF cursor with main nodes of the MultiFile database nodes
     * @param mainFolderUri Uri of the main folder
     * @param context application context to display toast messages, get resources, handle clicks
     * @return SAF cursor with main nodes
     */
    protected Cursor getMainNodesCursor(Uri mainFolderUri, Context context) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mainFolderUri, DocumentsContract.getTreeDocumentId(mainFolderUri));
        return context.getContentResolver().query(
                childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        );
    }

    /**
     * Creates cursor with children of the document using SAF documentId. DocumentId has to be of a
     * children of mainFolderUri.
     * @param mainFolderUri SAF Uri of the main folder
     * @param context application context to display toast messages, get resources, handle clicks
     * @param documentId documentId of the document to create children of
     * @return cursor with children documents. Has to be closed after use.
     */
    private Cursor getNodeChildrenCursor(Uri mainFolderUri, Context context, String documentId) {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(mainFolderUri, documentId);
        return context.getContentResolver().query(
                uri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        );
    }

    /**
     * Returns list of nodeUniqueIDs from subnodes.lst file that uri points to. This file is used to
     * display nodes in the order that user sorted them
     * @param uri uri of the subnodes.lst file
     * @param context application context to display toast messages, get resources, handle clicks
     * @return list that contains nodeUniqueIDs
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    protected List<String> getSubnodesList(Uri uri, Context context) throws IOException {
        List<String> subnodes = null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            if (line != null) {
                subnodes = new LinkedList<>(Arrays.asList(line.split(",")));
            }
        }
        return subnodes;
    }

    /**
     * Writes Document object global variable to file.
     * @param doc Document object to save to file
     * @param file file that drawerMenu has to base saved in
     * @throws TransformerException This class specifies an exceptional condition that occurred during the transformation process.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    protected void saveDrawerMenuToStorage(Document doc, File file) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource dSource = new DOMSource(doc);
        try (OutputStream fileOutputStream = new FileOutputStream(file)) {
            StreamResult result = new StreamResult(fileOutputStream);  // To save it in the Internal Storage
            transformer.transform(dSource, result);
        }
    }

    /**
     * Scan nodes and it's subnodes for drawerMenu items. Return object does not contain all
     * information necessary to open it as drawerMenu, because sharedNodes do not have all the
     * data about them. See fixSharedNodes method for more information about it.
     * @param doc document object that stores all drawerMenu data before it is writen to the file
     * @param documentId SAF document ID that has to be scanned
     * @param context application context to display toast messages, get resources, handle clicks
     * @param mainFolderUri SAF Uri of the main folder
     * @param documentBuilder biulder to create new XML nodes for the document
     * @return Node with all the subnodes for drawerMenu xml file
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    protected Node scanNode(Document doc, String documentId, Context context, Uri mainFolderUri, DocumentBuilder documentBuilder) throws IOException, SAXException{
        Element node = doc.createElement("node");
        String nodeUniqueID = null;
        String nodeMasterID = null;
        String name = null;
        boolean hasSubnodes = false;
        String progLang = null;
        String noSearchMe = null;
        String noSearchCh = null;
        String isRichText = null;
        String isBold = null;
        String foregroundColor = null;
        String iconId = null;
        String isReadOnly = null;
        try (Cursor cursor = getNodeChildrenCursor(mainFolderUri, context, documentId)) {
            List<String> subnodes = null;
            while (cursor.moveToNext()) {
                if (cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    hasSubnodes = true;
                } else if (cursor.getString(2).equals("node.xml")) {
                    try (InputStream is = context.getContentResolver().openInputStream(
                            DocumentsContract.buildDocumentUriUsingTree(mainFolderUri, cursor.getString(0))
                    )) {
                        Node element = documentBuilder.parse(is).getElementsByTagName("node").item(0);
                        nodeUniqueID = element.getAttributes().getNamedItem("unique_id").getNodeValue();
                        Node masterNodeIdAttr = element.getAttributes().getNamedItem("master_id");
                        nodeMasterID = masterNodeIdAttr != null ? masterNodeIdAttr.getNodeValue() : "0";
                        if ("0".equals(nodeMasterID)) {
                            name = element.getAttributes().getNamedItem("name").getNodeValue();
                            progLang = element.getAttributes().getNamedItem("prog_lang").getNodeValue();
                            noSearchMe = element.getAttributes().getNamedItem("nosearch_me").getNodeValue();
                            noSearchCh = element.getAttributes().getNamedItem("nosearch_ch").getNodeValue();
                            isRichText = element.getAttributes().getNamedItem("prog_lang").getNodeValue();
                            isBold = element.getAttributes().getNamedItem("is_bold").getNodeValue();
                            foregroundColor = element.getAttributes().getNamedItem("foreground").getNodeValue();
                            iconId = element.getAttributes().getNamedItem("custom_icon_id").getNodeValue();
                            isReadOnly = element.getAttributes().getNamedItem("readonly").getNodeValue();
                        }
                    }
                } else if (cursor.getString(2).equals("subnodes.lst")) {
                    subnodes = getSubnodesList(DocumentsContract.buildChildDocumentsUriUsingTree(mainFolderUri, cursor.getString(0)), context);
                }
            }
            if (hasSubnodes && subnodes != null) {
                cursor.moveToPosition(-1);
                while (subnodes.size() > 0) {
                    String nextNodeUniqueID = subnodes.remove(0);
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR) && cursor.getString(2).equals(nextNodeUniqueID)) {
                            node.appendChild(scanNodeSubtree(doc, cursor.getString(0)));
                            break;
                        }
                    }
                    cursor.moveToPosition(-1);
                }
            }
        }
        if (nodeUniqueID == null) {
            return null;
        }
        node.setAttribute("unique_id", nodeUniqueID);
        node.setAttribute("master_id", nodeMasterID);
        if ("0".equals(nodeMasterID)) {
            node.setAttribute("name", name);
            node.setAttribute("prog_lang", progLang);
            node.setAttribute("nosearch_me", noSearchMe);
            node.setAttribute("nosearch_ch", noSearchCh);
            node.setAttribute("is_rich_text", isRichText);
            node.setAttribute("is_bold", isBold);
            node.setAttribute("foreground_color", foregroundColor);
            node.setAttribute("icon_id", iconId);
            node.setAttribute("readonly", isReadOnly);
        }
        node.setAttribute("saf_id", documentId);
        return node;
    }

    /**
     * Abstraction to allow different logic for scans from MainActivity when database is opened for
     * the first time and when user triggers rescan from MainView
     * @param doc XML document that will store drawerMenu data
     * @param documentId SAF document ID that has to be recursivaly scanned for node
     * @return Node with all subnodes that can be added to XML document to be writen drawer_menu.xml
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    protected abstract Node scanNodeSubtree(Document doc, String documentId) throws IOException, SAXException;
}
