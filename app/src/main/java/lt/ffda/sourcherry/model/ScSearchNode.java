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

public class ScSearchNode extends ScNode {
    private String query;
    private int resultCount;
    private String resultSamples;

    /**
     * Constructor for SourCherry search result node object. Changes the previous method of transferring
     * data of search results in String[]{nodeName, nodeUniqueID, query, countOfResults, samplesOfResult, hasSubnodes, isParent, isSubnode}
     * @param uniqueId unique id of the node
     * @param name node name
     * @param isParent is node a parent node. Parent node has a arrow indicating that it has subnodes
     * @param hasSubnodes does node has any subnodes. Node with subnodes has an arrow indicating it
     * @param isSubnode is node a subnode. Subnodes are indented in drawer menu
     * @param isRichText is node's type is rich text
     * @param isBold is node name text should be in bold
     * @param foregroundColor foreground color of the node name text
     * @param iconId node icon id
     * @param isReadOnly is node read only
     * @param query query that was used to find a node
     * @param resultCount count of results in the node
     * @param resultSamples sample lines in the node with the query
     */
    public ScSearchNode(String uniqueId, String name, boolean isParent, boolean hasSubnodes, boolean isSubnode, boolean isRichText, boolean isBold, String foregroundColor, int iconId, boolean isReadOnly, String query, int resultCount, String resultSamples) {
        super(uniqueId, name, isParent, hasSubnodes, isSubnode, isRichText, isBold, foregroundColor, iconId, isReadOnly);
        this.query = query;
        this.resultCount = resultCount;
        this.resultSamples = resultSamples;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getResultSamples() {
        return resultSamples;
    }

    public void setResultSamples(String resultSamples) {
        this.resultSamples = resultSamples;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.getUniqueId());
        dest.writeString(this.getName());
        dest.writeInt(this.isParent() ? 1 : 0);
        dest.writeInt(this.hasSubnodes() ? 1 : 0);
        dest.writeInt(this.isSubnode() ? 1 : 0);
        dest.writeInt(this.isRichText() ? 1 : 0);
        dest.writeString(this.query);
        dest.writeInt(this.resultCount);
        dest.writeString(this.resultSamples);
    }

    public static final Parcelable.Creator<ScSearchNode> CREATOR = new Parcelable.Creator<ScSearchNode>() {
        @Override
        public ScSearchNode createFromParcel(Parcel source) {
            String uniqueId = source.readString();
            String name = source.readString();
            boolean isParent = source.readInt() == 1;
            boolean hasSubnodes = source.readInt() == 1;
            boolean isSubnode = source.readInt() == 1;
            boolean isRichText = source.readInt() == 1;
            boolean isBold = source.readInt() == 1;
            String foregoundColor = source.readString();
            int iconId = source.readInt();
            boolean isReadOnly = source.readInt() == 1;
            String query = source.readString();
            int resultCount = source.readInt();
            String resultSamples = source.readString();
            return new ScSearchNode(uniqueId, name, isParent, hasSubnodes, isSubnode, isRichText, isBold, foregoundColor, iconId, isReadOnly, query, resultCount, resultSamples);
        }

        @Override
        public ScSearchNode[] newArray(int size) {
            return new ScSearchNode[0];
        }
    };
}
