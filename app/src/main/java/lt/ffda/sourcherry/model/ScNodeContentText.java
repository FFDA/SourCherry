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

import android.text.SpannableStringBuilder;

public class ScNodeContentText implements ScNodeContent {
    private byte type;
    private SpannableStringBuilder content;

    /**
     * Constructor for ScNodeContent object that holds node content (or a part of it)
     * in StringBuilder object. It holds everything except tables of the node.
     * @param type 0 - text to set into TextView, 1 - table content
     * @param content node text content
     */
    public ScNodeContentText(byte type, SpannableStringBuilder content) {
        this.type = type;
        this.content = content;
    }

    @Override
    public void setContentType(byte type) {
        this.type = type;
    }

    @Override
    public byte getContentType() {
        return this.type;
    }

    /**
     * Get node text content
     * @return node text content
     */
    public SpannableStringBuilder getContent() {
        return this.content;
    }

    /**
     * Set node text content
     * @param content node text content
     */
    public void setContent(SpannableStringBuilder content) {
        this.content = content;
    }
}
