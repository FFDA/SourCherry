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
}
