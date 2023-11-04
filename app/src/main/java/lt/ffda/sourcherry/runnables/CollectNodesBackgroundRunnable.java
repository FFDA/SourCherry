/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lt.ffda.sourcherry.R;

public class CollectNodesBackgroundRunnable implements Runnable {
    private final Uri mainFolderUri;
    private final Context context;
    private final NodesCollectedCallback callbackFinished;
    private final File file;
    private final DocumentBuilder documentBuilder;

    /**
     * Runnable that scans through directory tree and creates drawer_menu.xml file in app's app-specific
     * storage's root. drawer_menu.xml file contains all information about the node except it's content.
     * This information includes unique_id, name, has_subnodes, prog_lang, nosearch_me, nosearch_ch,
     * is_rich_text, is_bold, foreground_color, icon_id, readonly and saf_id.
     * @param mainFolderURi uri of the Multifile database root
     * @param context context of the app
     * @param callbackFinished callbackFinished that has to be executed after collection processes is finished
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    public CollectNodesBackgroundRunnable(Uri mainFolderURi, Context context, NodesCollectedCallback callbackFinished) throws ParserConfigurationException {
        this.mainFolderUri = mainFolderURi;
        this.context = context;
        this.file = new File(context.getFilesDir(), "drawer_menu.xml");
        this.callbackFinished = callbackFinished;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        this.documentBuilder = dbf.newDocumentBuilder();
    }

    /**
     * Initiates the recursive scan of all the folders under the Multifile databases root directory
     * @return Document object with all the nodes of the database in tree structure
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    public Document getDrawerMenuTree() throws IOException, SAXException {
        Document doc = this.documentBuilder.newDocument();
        Element sourCherry = doc.createElement("sourcherry");
        sourCherry.setAttribute("saf_id", DocumentsContract.getTreeDocumentId(this.mainFolderUri));
        List<String> subnodes = null;
        try (Cursor cursor = this.getMainNodesCursor()) {
            while (cursor.moveToNext()) {
                if (cursor.getString(2).equals("subnodes.lst")) {
                    subnodes = this.getSubnodesList(DocumentsContract.buildChildDocumentsUriUsingTree(this.mainFolderUri, cursor.getString(0)));
                }
            }
            if (subnodes == null) {
                throw new FileNotFoundException(context.getString(R.string.toast_error_failed_to_delete_multi_database_file, "subnodes.lst"));
            }
            while (subnodes.size() > 0) {
                cursor.moveToPosition(-1);
                String nextNodeUniqueID = subnodes.remove(0);
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals(nextNodeUniqueID)) {
                        sourCherry.appendChild(scanNodeSubtree(doc, cursor.getString(0)));
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        doc.appendChild(sourCherry);
        return doc;
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
     * Creates cursor with children of the document using SAF documentId. DocumentId has to be of a
     * children of this.mainFolderUri.
     * @param documentId documentId of the document to create children of
     * @return cursor with children documents. Has to be closed after use.
     */
    private Cursor getNodeChildrenCursor(String documentId) {
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
     * Returns list of nodeUniqueIDs from subnodes.lst file that uri points to. This file is used to
     * display nodes in the order that user sorted them
     * @param uri uri of the subnodes.lst file
     * @return list that contains nodeUniqueIDs
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private List<String> getSubnodesList(Uri uri) throws IOException {
        List<String> subnodes = null;
        try (InputStream is = this.context.getContentResolver().openInputStream(uri)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            if (line != null) {
                subnodes = new LinkedList<>(Arrays.asList(line.split(",")));
            }
        }
        return subnodes;
    }

    @Override
    public void run() {
        try {
            Document doc = this.getDrawerMenuTree();
            if (doc != null) {
                this.saveDrawerMenuToStorage(doc);
                this.callbackFinished.onNodesCollected(0);
            } else {
                this.callbackFinished.onNodesCollected(2);
            }
        } catch (IOException | TransformerException | SAXException e) {
            e.printStackTrace();
            this.callbackFinished.onNodesCollected(1);
        }
    }

    /**
     * Writes Document object global variable to file.
     * @param doc Document object to save to file
     * @throws TransformerException This class specifies an exceptional condition that occurred during the transformation process.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void saveDrawerMenuToStorage(Document doc) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        transformer = transformerFactory.newTransformer();
        DOMSource dSource = new DOMSource(doc);
        OutputStream fileOutputStream = new FileOutputStream(this.file);
        StreamResult result = new StreamResult(fileOutputStream);  // To save it in the Internal Storage
        transformer.transform(dSource, result);
        fileOutputStream.close();
    }

    /**
     * Gets all the children under the documentId and creates Node object with the data of the node.
     * If there is a subfolder(s) in folder under the documentId it recursively searches and creates
     * nodes from them too
     * @param doc Document object to create new XML nodes with
     * @param documentId FAS document id to create cursor with all the children under it
     * @return Node object with node under documentId and all it's children
     * @throws IOException Signals that an I/O exception of some sort has occurred
     * @throws SAXException Encapsulate a general SAX error or warning
     */
    private Node scanNodeSubtree(Document doc, String documentId) throws IOException, SAXException {
        Element node = doc.createElement("node");
        String nodeUniqueID = null;
        String name = null;
        boolean hasSubnodes = false;
        String progLang = null;
        boolean noSearchMe = false;
        boolean noSearchCh = false;
        boolean isRichText = false;
        boolean isBold = false;
        String foregroundColor = null;
        String iconId = null;
        boolean isReadOnly = false;
        try (Cursor cursor = this.getNodeChildrenCursor(documentId)) {
            List<String> subnodes = null;
            while (cursor.moveToNext()) {
                if (cursor.getString(1).equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    hasSubnodes = true;
                } else if (cursor.getString(2).equals("node.xml")) {
                    try (InputStream is = this.context.getContentResolver().openInputStream(
                            DocumentsContract.buildDocumentUriUsingTree(this.mainFolderUri, cursor.getString(0))
                    )) {
                        Node element = this.documentBuilder.parse(is).getElementsByTagName("node").item(0);
                        nodeUniqueID = element.getAttributes().getNamedItem("unique_id").getNodeValue();
                        name = element.getAttributes().getNamedItem("name").getNodeValue();
                        progLang = element.getAttributes().getNamedItem("prog_lang").getNodeValue();
                        noSearchMe = element.getAttributes().getNamedItem("nosearch_me").getNodeValue().equals("1");
                        noSearchCh = element.getAttributes().getNamedItem("nosearch_ch").getNodeValue().equals("1");
                        isRichText = element.getAttributes().getNamedItem("prog_lang").getNodeValue().equals("custom-colors");
                        isBold = element.getAttributes().getNamedItem("is_bold").getNodeValue().equals("1");
                        foregroundColor = element.getAttributes().getNamedItem("foreground").getNodeValue();
                        iconId = element.getAttributes().getNamedItem("custom_icon_id").getNodeValue();
                        isReadOnly = element.getAttributes().getNamedItem("readonly").getNodeValue().equals("1");
                    }
                } else if (cursor.getString(2).equals("subnodes.lst")) {
                    subnodes = this.getSubnodesList(DocumentsContract.buildChildDocumentsUriUsingTree(this.mainFolderUri, cursor.getString(0)));
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
        node.setAttribute("name", name);
        node.setAttribute("has_subnodes", hasSubnodes ? "1" : "0");
        node.setAttribute("prog_lang", progLang);
        node.setAttribute("nosearch_me", noSearchMe ? "1" : "0");
        node.setAttribute("nosearch_ch", noSearchCh ? "1" : "0");
        node.setAttribute("is_rich_text", isRichText ? "1" : "0");
        node.setAttribute("is_bold", isBold ? "1" : "0");
        node.setAttribute("foreground_color", foregroundColor);
        node.setAttribute("icon_id", iconId);
        node.setAttribute("readonly", isReadOnly ? "1" : "0");
        node.setAttribute("saf_id", documentId);
        if (Thread.interrupted()) {
            throw new RuntimeException("Scan canceled");
        }
        return node;
    }
}
