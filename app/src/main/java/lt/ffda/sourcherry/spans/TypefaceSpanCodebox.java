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

import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Span used to display codeboxes.
 * Holds additional values.
 */
public class TypefaceSpanCodebox extends TypefaceSpan implements Cloneable {
    private int frameWidth;
    private int frameHeight;
    private boolean widthInPixel;
    private String syntaxHighlighting;
    private boolean highlightBrackets;
    private boolean showLineNumbers;
    private String spanContent;
    private int newOffset;
    private String justification;

    public TypefaceSpanCodebox(@Nullable String family) {
        super(family);
    }

    /**
     * Get the width of the codebox
     * Has no effect in SourCherry
     * @return width of the codebox
     */
    public int getFrameWidth() {
        return this.frameWidth;
    }

    /**
     * Set the width of the codebox
     * Has no effect in SourCherry
     * @param frameWidth width of the codebox
     */
    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    /**
     * Get the height if the codebox
     * Has no effect in SourCherry
     * @return height of the codebox
     */
    public int getFrameHeight() {
        return this.frameHeight;
    }

    /**
     * Set the frame of the codebox
     * Has no effect in SourCherry
     * @param frameHeight height of the codebox
     */
    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    /**
     * Get codebox property: width in pixels or percents
     * Has no effect in SourCherry
     * @return true - width is calculated int pixels, false - width is calculated in percentages
     */
    public boolean isWidthInPixel() {
        return this.widthInPixel;
    }

    /**
     * Set codebox property: width in pixel
     * Has no effect in SourCherry
     * @param widthInPixel true - width is calculated int pixels, false - width is calculated in percentages
     */
    public void setWidthInPixel(boolean widthInPixel) {
        this.widthInPixel = widthInPixel;
    }

    /**
     * Get codebox syntax highlighting value
     * Has no effect in SourCherry. All highlighting looks the same
     * @return syntax highlighting value
     */
    public String getSyntaxHighlighting() {
        return this.syntaxHighlighting;
    }

    /**
     * Set codebox syntax highlighting value
     * Has no effect in SourCherry. All highlighting looks the same
     * @param syntaxHighlighting syntax highlighting value
     */
    public void setSyntaxHighlighting(String syntaxHighlighting) {
        this.syntaxHighlighting = syntaxHighlighting;
    }

    /**
     * Get codebox property: Highlight Matching Brackets
     * Has no effect in SourCherry
     * @return true - matching brackets is being highlighted
     */
    public boolean isHighlightBrackets() {
        return this.highlightBrackets;
    }

    /**
     * Set codebox property: Highlight Matching Brackets
     * Has no effect in SourCherry
     * @param highlightBrackets  true - matching brackets will be highlighter
     */
    public void setHighlightBrackets(boolean highlightBrackets) {
        this.highlightBrackets = highlightBrackets;
    }

    /**
     * Get codebox property: Show Line Numbers
     * Has no effect in SourCherry
     * @return true - line numbers are being shown
     */
    public boolean isShowLineNumbers() {
        return this.showLineNumbers;
    }

    /**
     * Set codebox property: Show Line Numbers
     * Has no effect in SourCherry
     * @param showLineNumbers true - line numbers should be shown
     */
    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
    }

    /**
     * Get span content. Used only in SQLReader to pass span content for sorting
     * @return span content
     */
    public String getSpanContent() {
        return this.spanContent;
    }

    /**
     * Set span content. Used only in SQLReader to pass span content for sorting
     * @param spanContent content that span marks
     */
    public void setSpanContent(String spanContent) {
        this.spanContent = spanContent;
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

    @NonNull
    @Override
    public TypefaceSpanCodebox clone() {
        try {
            TypefaceSpanCodebox codeboxClone = (TypefaceSpanCodebox) super.clone();
            codeboxClone.syntaxHighlighting = ((TypefaceSpanCodebox) super.clone()).getSyntaxHighlighting();
            codeboxClone.spanContent = ((TypefaceSpanCodebox) super.clone()).spanContent;
            codeboxClone.justification = ((TypefaceSpanCodebox) super.clone()).justification;
            return codeboxClone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
