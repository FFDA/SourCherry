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

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

/**
 * Span used to show anchor image placeholder.
 * Holds anchor attribute value, that does not have
 * any affect in SourCherry
 */
public class ImageSpanAnchor extends ImageSpan {
    // Anchor attribute value of the encoded_png tag
    // It's anchor name that CherryTree used to scroll view to
    private final String anchorName;
    private int newOffset;
    private String justification;

    /**
     * Constructor to create anchor image to be inserted into node content
     * @param drawable drawable of the anchor
     * @param verticalAlignment alignment of the image in the line
     * @param anchorName Anchor attribute value of the encoded_png tag
     */
    public ImageSpanAnchor(@NonNull Drawable drawable, int verticalAlignment, String anchorName) {
        super(drawable, verticalAlignment);
        this.anchorName = anchorName;
    }

    /**
     * Get anchor attribute value of the encoded_png tag
     * @return anchor attribute value
     */
    public String getAnchorName() {
        return this.anchorName;
    }

    /**
     * Get justification of the element
     * @return justification of the elements
     */
    public String getJustification() {
        return this.justification;
    }

    /**
     * Set justification of the element
     * @param justification element's justification ("left", "right", "center", "fill")
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    /**
     * Get new offset of the element. It shows location where the element has to be inserted back
     * into the node content when it is being recreated.
     * @return element's offset
     */
    public int getNewOffset() {
        return this.newOffset;
    }

    /**
     * Set new offset of the element. It has to be calculated using the location of the span in the
     * node content.
     * @param newOffset element's offset
     */
    public void setNewOffset(int newOffset) {
        this.newOffset = newOffset;
    }
}
