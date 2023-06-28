/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.spans;

import android.text.style.ClickableSpan;

/**
 * Span used to show link to another node
 * Holds additional values
 */
public abstract class ClickableSpanNode extends ClickableSpan {
    // Unique ID of the node to open when link is tapped
    private String nodeUniqueID;
    // Anchor name that view scrolls to. This value has no affect in SourCherry
    private String linkAnchorName;

    /**
     * Get unique ID of the node the link leads to
     * @return unique ID of the node
     */
    public String getNodeUniqueID() {
        return nodeUniqueID;
    }

    /**
     * Set unique ID of the node the link leads to
     * @param nodeUniqueID unique ID of the node
     */
    public void setNodeUniqueID(String nodeUniqueID) {
        this.nodeUniqueID = nodeUniqueID;
    }

    /**
     * Get anchor name that view has to scroll to when
     * linked node is opened
     * @return anchor name
     */
    public String getLinkAnchorName() {
        return linkAnchorName;
    }

    /**
     * Set anchor name that view has to scroll to when
     * linked node is opened
     * @param linkAnchorName anchor name
     */
    public void setLinkAnchorName(String linkAnchorName) {
        this.linkAnchorName = linkAnchorName;
    }
}
