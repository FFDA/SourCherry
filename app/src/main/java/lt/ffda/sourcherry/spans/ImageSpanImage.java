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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

/**
 * Span used to display images in node
 */
public class ImageSpanImage extends ImageSpan {
    private int newOffset;
    private String justification;
    private String sha256sum;

    public ImageSpanImage(@NonNull Drawable drawable) {
        super(drawable);
    }

    public ImageSpanImage(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
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

    /**
     * Get sha256sum value of the file. It is only used in Multifile databases as a filename in filesystem
     * @return sha256sum value of the file
     */
    public String getSha256sum() {
        return sha256sum;
    }

    /**
     * Set sha256sum value of the file. It is only used in Multifile databases as a filename in filesystem
     * @param sha256sum sha256sum value of the file
     */
    public void setSha256sum(String sha256sum) {
        this.sha256sum = sha256sum;
    }
}
