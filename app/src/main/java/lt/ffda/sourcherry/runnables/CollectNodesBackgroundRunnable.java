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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lt.ffda.sourcherry.R;

public class CollectNodesBackgroundRunnable extends CollectNodesAbstractRunnable implements Runnable {
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

    @Override
    public Document getDrawerMenuTree() throws IOException, SAXException {
        Document doc = documentBuilder.newDocument();
        Element sourCherry = doc.createElement("sourcherry");
        sourCherry.setAttribute("saf_id", DocumentsContract.getTreeDocumentId(mainFolderUri));
        List<String> subnodes = null;
        try (Cursor cursor = getMainNodesCursor(mainFolderUri, context)) {
            while (cursor.moveToNext()) {
                if (cursor.getString(2).equals("subnodes.lst")) {
                    subnodes = getSubnodesList(DocumentsContract.buildChildDocumentsUriUsingTree(mainFolderUri, cursor.getString(0)));
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
            Document doc = getDrawerMenuTree();
            if (doc != null) {
                saveDrawerMenuToStorage(doc, file);
                callbackFinished.onNodesCollected(0);
            } else {
                callbackFinished.onNodesCollected(2);
            }
        } catch (IOException | TransformerException | SAXException e) {
            callbackFinished.onNodesCollected(1);
        }
    }

    @Override
    protected Node scanNodeSubtree(Document doc, String documentId) throws IOException, SAXException {
        return scanNode(doc, documentId, context, mainFolderUri, documentBuilder);
    }
}
