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
import android.os.Handler;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lt.ffda.sourcherry.R;

public class CollectNodesDialogRunnable extends CollectNodesAbstractRunnable implements Runnable {
    private final Uri mainFolderUri;
    private final Context context;
    private final NodesCollectedCallback callback;
    private final TextView textView;
    private final File file;
    private final Handler handler;
    private final DocumentBuilder documentBuilder;
    private int counter;

    /**
     * Runnable that scans through directory tree and creates drawer_menu.xml file in app's app-specific
     * storage's root. drawer_menu.xml file contains all information about the node except it's content.
     * This information includes unique_id, name, has_subnodes, prog_lang, nosearch_me, nosearch_ch,
     * is_rich_text, is_bold, foreground_color, icon_id, readonly and saf_id.
     * @param mainFolderURi uri of the Multifile database root
     * @param context context of the app
     * @param handler handler to execute code on main thread
     * @param textView textview object that will display text to user about how many nodes where processed
     * @param callback callback that has to be executed after collection processes is finished
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    public CollectNodesDialogRunnable(Uri mainFolderURi, Context context, Handler handler, TextView textView, NodesCollectedCallback callback) throws ParserConfigurationException {
        this.mainFolderUri = mainFolderURi;
        this.context = context;
        this.handler = handler;
        this.textView = textView;
        this.file = new File(context.getFilesDir(), "drawer_menu.xml");
        this.callback = callback;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        this.documentBuilder = dbf.newDocumentBuilder();
        this.counter = 0;
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
                    subnodes = getSubnodesList(DocumentsContract.buildChildDocumentsUriUsingTree(mainFolderUri, cursor.getString(0)), context);
                }
            }
            if (subnodes == null) {
                throw new FileNotFoundException("Could not find subnodes.lst file");
            }
            while (!subnodes.isEmpty()) {
                cursor.moveToPosition(-1);
                String nextNodeUniqueID = subnodes.remove(0);
                while (cursor.moveToNext()) {
                    if (cursor.getString(2).equals(nextNodeUniqueID)) {
                        sourCherry.appendChild(scanNodeSubtree(doc, cursor.getString(0)));
                        break;
                    }
                }
            }
        }
        doc.appendChild(sourCherry);
        return doc;
    }

    @Override
    public void run() {
        try {
            Document doc = getDrawerMenuTree();
            saveDrawerMenuToStorage(doc, file);
            callback.onNodesCollected(0);
        } catch (IOException | TransformerException | SAXException e) {
            callback.onNodesCollected(1);
        }
    }

    @Override
    protected Node scanNodeSubtree(Document doc, String documentId) throws IOException, SAXException {
        Node node = scanNode(doc, documentId, context, mainFolderUri, documentBuilder);
        if (node == null) {
            return null;
        }
        counter++;
        handler.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(context.getString(R.string.dialog_fragment_collect_nodes_message, counter));
            }
        });
        return node;
    }
}
