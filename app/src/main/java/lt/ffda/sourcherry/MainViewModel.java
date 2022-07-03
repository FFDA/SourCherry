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

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class MainViewModel extends ViewModel {
    // Stores data, that should be kept during screen orientation change
    private ArrayList<ArrayList<CharSequence[]>> nodeContent;
    private ArrayList<String[]> nodes;
    private ArrayList<String[]> tempNodes;

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
}
