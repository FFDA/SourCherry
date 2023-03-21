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

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class MainViewModel extends ViewModel {
    // Stores data, that should be kept during screen orientation change
    private ArrayList<ArrayList<CharSequence[]>> nodeContent;
    private ArrayList<String[]> nodes;
    private ArrayList<String[]> tempNodes;
    private ArrayList<String[]> tempSearchNodes;
    // String value of every TextView in node
    private ArrayList<SpannableStringBuilder> findInNodeStorage;
    // Stores results for FindInNode() int[textView index in findInNodeStorage, start index of matching substring, end index of matching substring]
    private ArrayList<int[]> findInNodeResultStorage;

    /**
     * Set currently opened node content
     * that will be loaded to display for user
     * @param nodeContent node content retrieved from database
     */
    public void setNodeContent(ArrayList<ArrayList<CharSequence[]>> nodeContent) {
        this.nodeContent = nodeContent;
    }

    /**
     * Get current drawer menu items
     * @return drawer menu items
     */
    public ArrayList<ArrayList<CharSequence[]>> getNodeContent() {
        return this.nodeContent;
    }

    /**
     * Set nodes to a list that holds drawer menu items
     * @param nodes drawer menu items list
     */
    public void setNodes(ArrayList<String[]> nodes) {
        if (this.nodes != null) {
            this.nodes.clear();
            this.nodes.addAll(nodes);
        } else {
            this.nodes = nodes;
        }
    }

    /**
     * Returns current drawer menu items
     * @return drawer menu items
     */
    public ArrayList<String[]> getNodes() {
        return this.nodes;
    }

    /**
     * Sets temporary drawer menu node storage to null
     */
    public void resetTempNodes() {
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
     * Restores saved menu drawer items to the drawer menu
     */
    public void restoreSavedCurrentNodes() {
        this.nodes.clear();
        this.nodes.addAll(this.tempNodes);
        this.tempNodes = null;
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
     * Returns array list that was previously saved with setTempSearchNodes
     * @return drawer menu node list
     */
    public ArrayList<String[]> getTempSearchNodes() {
        return this.tempSearchNodes;
    }

    /**
     * Stores passed array list to another
     * Used to store all the nodes for node filter function
     * @param nodes nodes to save to a different array
     */
    public void setTempSearchNodes(ArrayList<String[]> nodes) {
        this.tempSearchNodes.clear();
        this.tempSearchNodes.addAll(nodes);
    }

    /**
     * Initiates or sets to null findInNode and findInNodeResultStorage arrays
     * @param status true - initiates arrays, false - sets to null
     */
    public void findInNodeStorageToggle(Boolean status) {
        // Depending on boolean creates an array to store node content to search through
        // or sets it to null to clear it
        if (status) {
            this.findInNodeStorage = new ArrayList<>();
            this.findInNodeResultStorage = new ArrayList<>();
        } else {
            this.findInNodeStorage = null;
            this.findInNodeResultStorage = null;
        }
    }

    /**
     * Returns part of the content to search though it
     * @param index index of the findInNodeStorage array element to return
     * @return part of node content
     */
    public SpannableStringBuilder getFindInNodeStorageItem(int index) {
        return this.findInNodeStorage.get(index);
    }

    /**
     * Adds node content part to content storage for findInNode function
     * @param contentPart node content part to add to findInNodeStorage ArrayList
     */
    public void addFindInNodeStorage(SpannableStringBuilder contentPart) {
        this.findInNodeStorage.add(contentPart);
    }

    /**
     * Adds result to FindInNodeResultStorage
     * @param result array that consists of three integers: index of view that holds this result, start and end of substring that has to be highlighted
     */
    public void addFindInNodeResult(int[] result) {
        this.findInNodeResultStorage.add(result);
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
     * Clears FindInNode result ListArray
     * Used when user types new query
     */
    public void resetFindInNodeResultStorage() {
        if (this.findInNodeResultStorage != null) {
            this.findInNodeResultStorage = new ArrayList<>();
        }
    }

    /**
     * Deletes node content
     */
    public void deleteNodeContent() {
        this.nodeContent.clear();
    }
}
