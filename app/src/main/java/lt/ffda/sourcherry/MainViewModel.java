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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;

/**
 * Stores data, that should be kept during screen orientation change
 */
public class MainViewModel extends ViewModel {
    private ScNode currentNode = null;
    // Stores results for FindInNode() int[textView index in nodeContent, start index of matching substring, end index of matching substring]
    private ArrayList<int[]> findInNodeResultStorage;
    private MutableLiveData<ScheduledFuture<?>> multiDatabaseSync;
    private MutableLiveData<ArrayList<ScNodeContent>> nodeContent;
    private ArrayList<ScNode> nodes;
    private ArrayList<ScNode> tempNodes;
    private ArrayList<ScNode> tempSearchNodes;

    /**
     * Adds result to FindInNodeResultStorage
     * @param result array that consists of three integers: index of view that holds this result, start and end of substring that has to be highlighted
     */
    public void addFindInNodeResult(int[] result) {
        this.findInNodeResultStorage.add(result);
    }

    /**
     * Deletes stored node content by setting it to empty ArrayList
     */
    public void deleteNodeContent() {
        this.nodeContent.postValue(new ArrayList<>());
    }

    /**
     * Initiates or sets to null findInNode and findInNodeResultStorage arrays
     * @param status true - initiates arrays, false - sets to null
     */
    public void findInNodeStorageToggle(Boolean status) {
        // Depending on boolean creates an array to store node content to search through
        // or sets it to null to clear it
        if (status) {
            this.findInNodeResultStorage = new ArrayList<>();
        } else {
            this.findInNodeResultStorage = null;
        }
    }

    /**
     * Get currently opened node's ScNode object
     * @return ScNode object of the currently opened node
     */
    public ScNode getCurrentNode() {
        return this.currentNode;
    }

    /**
     * Set currently opened node's ScNode object
     * @param currentNode ScNode object of the currently opened node
     */
    public void setCurrentNode(ScNode currentNode) {
        this.currentNode = currentNode;
    }

    /**
     * Returns the result of FindInNode search
     * @param resultIndex index of the result
     * @return FindInNode result
     */
    public int[] getFindInNodeResult(int resultIndex) {
        return this.findInNodeResultStorage.get(resultIndex);
    }

    /**
     * Returns the size of FindInNode result ListArray
     * @return count of FindInNode results
     */
    public int getFindInNodeResultCount() {
        return this.findInNodeResultStorage.size();
    }

    /**
     * Returns findInNodeResultStorage ArrayList
     * used to store result indexes for findInNode function
     * @return findInNodeResultStorage ArrayList
     */
    public ArrayList<int[]> getFindInNodeResultStorage() {
        return this.findInNodeResultStorage;
    }

    /**
     * Every text in the nodeContent is placed in a TextView. They can be counted in the order from
     * top to bottom. Every cell in the table is a separate TextView. This function returns text of
     * the TextView was placed in when creating the nodeContent. It is used by findInNode and related
     * functions.
     * @param index index of the TextView
     * @return content of the text view
     */
    public SpannableStringBuilder getTextViewContent(int index) {
        int counter = 0;
        for (ScNodeContent scNodeContent : this.getNodeContent().getValue()) {
            if (scNodeContent.getContentType() == 0) {
                // TextView
                if (counter == index) {
                    ScNodeContentText scNodeContentText = (ScNodeContentText) scNodeContent;
                    return scNodeContentText.getContent();
                }
                counter++;
            } else if (scNodeContent.getContentType() == 1) {
                // Table
                ScNodeContentTable scNodeContentTable = (ScNodeContentTable) scNodeContent;
                for (CharSequence[] row: scNodeContentTable.getContent()) {
                    for (CharSequence cell: row) {
                        if (counter == index) {
                            return new SpannableStringBuilder(cell);
                        }
                        counter++;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns LiveData object that holds ScheduledFuture of MultiFile database background scan.
     * It survives orientation changes and can be used to cancel the task.
     * @return LiveData object that holds ScheduledFuture of MultiFile database background scan
     */
    public MutableLiveData<ScheduledFuture<?>> getMultiDatabaseSync() {
        if (this.multiDatabaseSync == null) {
            this.multiDatabaseSync = new MutableLiveData<>();
        }
        return this.multiDatabaseSync;
    }

    /**
     * LiveData object that stores node content data. Use objects functions to get and set the data.
     * @return LiveData object with node content
     */
    public MutableLiveData<ArrayList<ScNodeContent>> getNodeContent() {
        if (this.nodeContent == null) {
            this.nodeContent = new MutableLiveData<>();
        }
        return this.nodeContent;
    }

    /**
     * Finds node's position in drawer menu
     * @param nodeUniqueID unique ID of the node which position has to found
     * @return position of the node in drawer menu
     */
    public int getNodePositionInMenu(String nodeUniqueID) {
        int position = -1;
        for (int i = 0; i < this.nodes.size(); i++) {
            if (this.getNodes().get(i).getUniqueId().equals(nodeUniqueID)) {
                position = i;
                break;
            }
        }
        return position;
    }

    /**
     * Returns current drawer menu items
     * @return drawer menu items
     */
    public ArrayList<ScNode> getNodes() {
        return this.nodes;
    }

    /**
     * Set nodes to a list that holds drawer menu items
     * @param nodes drawer menu items list
     */
    public void setNodes(ArrayList<ScNode> nodes) {
        if (this.nodes != null) {
            this.nodes.clear();
            this.nodes.addAll(nodes);
        } else {
            this.nodes = nodes;
        }
    }

    /**
     * Returns items saved using saveCurrentNodes
     * @return saved drawer menu items
     */
    public ArrayList<ScNode> getTempNodes() {
        return this.tempNodes;
    }

    /**
     * Returns array list that was previously saved with setTempSearchNodes
     * @return drawer menu node list
     */
    public ArrayList<ScNode> getTempSearchNodes() {
        return this.tempSearchNodes;
    }

    /**
     * Stores passed array list to another
     * Used to store all the nodes for node filter function
     * @param nodes nodes to save to a different array
     */
    public void setTempSearchNodes(ArrayList<ScNode> nodes) {
        this.tempSearchNodes.clear();
        this.tempSearchNodes.addAll(nodes);
    }

    /**
     * Clears FindInNode result ListArray
     * Used when user types new query
     */
    public void resetFindInNodeResultStorage() {
        if (this.findInNodeResultStorage != null) {
            this.findInNodeResultStorage = new ArrayList<>();
        }
    }

    /**
     * Sets temporary drawer menu node storage to null
     */
    public void resetTempNodes() {
        this.tempNodes = null;
    }

    /**
     * Restores saved menu drawer items to the drawer menu
     */
    public void restoreSavedCurrentNodes() {
        this.nodes.clear();
        this.nodes.addAll(this.tempNodes);
        this.tempNodes = null;
    }

    /**
     * Saves current menu items while using bookmarks and search
     */
    public void saveCurrentNodes() {
        this.tempNodes = new ArrayList<>();
        this.tempNodes.addAll(this.nodes);
    }

    /**
     * Saves or removes tempSearchNodes depending on passed boolean
     * @param status true - saves, false - removes
     */
    public void tempSearchNodesToggle(Boolean status) {
        if (status) {
            this.tempSearchNodes = new ArrayList<>();
            this.tempSearchNodes.addAll(this.nodes);
        } else {
            this.tempSearchNodes = null;
        }
    }

    /**
     * Sets updated nodes to tempNode variable
     */
    public void updateSavedCurrentNodes(ArrayList<ScNode> newNodes) {
        this.tempNodes = newNodes;
    }
}
