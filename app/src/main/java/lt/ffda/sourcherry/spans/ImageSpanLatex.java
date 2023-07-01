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
 * Image span used to display compiled latex image
 * from latex code. Contains code that was used to compile
 * latex image saved in it.
 */
public class ImageSpanLatex extends ImageSpan {
    private String latexCode;
    private int newOffset;
    private String justification;
    public ImageSpanLatex(@NonNull Drawable drawable) {
        super(drawable);
    }

    /**
     * Get latex code that was used to compile latex image
     * @return latex code
     */
    public String getLatexCode() {
        return this.latexCode;
    }

    /**
     * Set latex code that was used to compile latex image
     * @param latexCode latex code
     */
    public void setLatexCode(String latexCode) {
        this.latexCode = latexCode;
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
}
