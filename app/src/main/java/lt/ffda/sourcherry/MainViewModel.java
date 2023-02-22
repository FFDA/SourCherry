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

    public void setNodeContent(ArrayList<ArrayList<CharSequence[]>> nodeContent) {
        this.nodeContent = nodeContent;
    }
    public ArrayList<ArrayList<CharSequence[]>> getNodeContent() {
        return this.nodeContent;
    }

    public void setNodes(ArrayList<String[]> nodes) {
        if (this.nodes != null) {
            this.nodes.clear();
            this.nodes.addAll(nodes);
        } else {
            this.nodes = nodes;
        }
    }
    public ArrayList<String[]> getNodes() {
        return this.nodes;
    }

    public void resetTempNodes() {
        this.tempNodes = null;
    }

    public void saveCurrentNodes() {
        // Saves current menu items while using bookmarks and search
        this.tempNodes = new ArrayList<>();
        this.tempNodes.addAll(this.nodes);
    }

    public void restoreSavedCurrentNodes() {
        // Restores saved menu items and destroys saved
        this.nodes.clear();
        this.nodes.addAll(this.tempNodes);
        this.tempNodes = null;
    }

    public void tempSearchNodesToggle(Boolean status) {
        // Saves or removes tempSearchNodes depending on passed boolean
        // true - saves, false - removes
        if (status) {
            this.tempSearchNodes = new ArrayList<>();
            this.tempSearchNodes.addAll(this.nodes);
        } else {
            this.tempSearchNodes = null;
        }
    }

    public ArrayList<String[]> getTempSearchNodes() {
        return this.tempSearchNodes;
    }

    public void setTempSearchNodes(ArrayList<String[]> nodes) {
        this.tempSearchNodes.clear();
        this.tempSearchNodes.addAll(nodes);
    }

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

    public SpannableStringBuilder getFindInNodeStorageItem(int index) {
        // returns part of the content to search though it
        return this.findInNodeStorage.get(index);
    }

    public void addFindInNodeStorage(SpannableStringBuilder contentPart) {
        // Adds node content part to content storage for findInNode function
        // Every TextView if the node is a separate SpannableStringBuilder
        this.findInNodeStorage.add(contentPart);
    }

    public void addFindInNodeResult(int[] result) {
        // Adds result to FindInNodeResultStorage
        // Results is int array that consists of three int
        // index of view that holds this result, start and end of substring that has to be highlighted
        this.findInNodeResultStorage.add(result);
    }

    public int[] getFindInNodeResult(int resultIndex) {
        // Returns result of the index in FindInNodeResultStorage
        return this.findInNodeResultStorage.get(resultIndex);
    }

    public int getFindInNodeResultCount() {
        // returns the size of FindInNode result ListArray
        return this.findInNodeResultStorage.size();
    }

    public void resetFindInNodeResultStorage() {
        // Clears FindInNode result ListArray
        // Used when user types new query
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
