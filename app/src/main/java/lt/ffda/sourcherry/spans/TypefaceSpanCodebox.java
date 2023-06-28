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

import androidx.annotation.Nullable;

/**
 * Span used to display codeboxes.
 * Holds additional values.
 */
public class TypefaceSpanCodebox extends TypefaceSpan {
    private int frameWidth;
    private int frameHeight;
    private boolean widthInPixel;
    private String syntaxHighlighting;
    private boolean highlightBrackets;
    private boolean showLineNumbers;

    public TypefaceSpanCodebox(@Nullable String family) {
        super(family);
    }

    /**
     * Get the width of the codebox
     * Has no effect in SourCherry
     * @return width of the codebox
     */
    public int getFrameWidth() {
        return frameWidth;
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
        return frameHeight;
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
        return widthInPixel;
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
        return syntaxHighlighting;
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
        return highlightBrackets;
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
        return showLineNumbers;
    }

    /**
     * Set codebox property: Show Line Numbers
     * Has no effect in SourCherry
     * @return true - line numbers should be shown
     */
    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
    }
}
