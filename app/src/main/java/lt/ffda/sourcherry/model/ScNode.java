/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ScNode implements Comparable<ScNode>, Parcelable {
    // Keeping it as String because I need it it this way most of the time. Otherwise I would have to convert ot String quite often
    private String uniqueId;
    private String name;
    // Depending on this value a node will be made look like it's a parent node (will not be indented and have an arrow pointing down)
    private boolean isParent;
    // Depending on this value node name will have an arrow in front of it to indicate that it has subnodes
    private boolean hasSubnodes;
    // Depending in this value node will look indented or not
    private boolean isSubnode;
    private boolean isRichText;

    /**
     * Constructor for SourCherry node object. Changes previous method of keeping node's data in String[]{name, unique_id, has_subnodes, is_parent, is_subnode}
     * @param uniqueId unique id of the node
     * @param name node name
     * @param isParent is node a parent node. Parent node has a arrow indicating that it has subnodes
     * @param hasSubnodes does node has any subnodes. Node with subnodes has an arrow indicating it
     * @param isSubnode is node a subnode. Subnodes are indented in drawer menu
     * @param isRichText is node's type is rich text
     */
    public ScNode(String uniqueId, String name, boolean isParent, boolean hasSubnodes, boolean isSubnode, boolean isRichText) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.isParent = isParent;
        this.hasSubnodes = hasSubnodes;
        this.isSubnode = isSubnode;
        this.isRichText = isRichText;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isParent() {
        return this.isParent;
    }

    public void setIsParent(boolean isParent) {
        this.isParent = isParent;
    }

    public boolean hasSubnodes() {
        return this.hasSubnodes;
    }

    public void setHasSubnodes(boolean hasSubnodes) {
        this.hasSubnodes = hasSubnodes;
    }

    public boolean isSubnode() {
        return this.isSubnode;
    }

    public void setSubnode(boolean isSubnode) {
        this.isSubnode = isSubnode;
    }

    public boolean isRichText() {
        return this.isRichText;
    }

    public void setRichText(boolean richText) {
        this.isRichText = richText;
    }

    @Override
    public int compareTo(ScNode o) {
        return Integer.parseInt(this.uniqueId) - Integer.parseInt(o.getUniqueId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.uniqueId);
        dest.writeString(this.name);
        dest.writeInt(this.isParent ? 1 : 0);
        dest.writeInt(this.hasSubnodes ? 1 : 0);
        dest.writeInt(this.isSubnode ? 1 : 0);
        dest.writeInt(this.isRichText ? 1 : 0);
    }

    public static final Parcelable.Creator<ScNode> CREATOR = new Parcelable.Creator<ScNode>() {
        @Override
        public ScNode createFromParcel(Parcel source) {
            String uniqueId = source.readString();
            String name = source.readString();
            boolean isParent = source.readInt() == 1;
            boolean hasSubnodes = source.readInt() == 1;
            boolean isSubnode = source.readInt() == 1;
            boolean isRichText = source.readInt() == 1;
            return new ScNode(uniqueId, name, isParent, hasSubnodes, isSubnode, isRichText);
        }

        @Override
        public ScNode[] newArray(int size) {
            return new ScNode[0];
        }
    };
}
