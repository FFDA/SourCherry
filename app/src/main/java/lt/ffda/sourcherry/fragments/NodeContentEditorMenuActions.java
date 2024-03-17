/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.fragments;

public interface NodeContentEditorMenuActions {

    /**
     * Attaches file to the node
     */
    void attachFile();

    /**
     * Changes selected text background color
     */
    void changeBackgroundColor();

    /**
     * Changes selected text foreground color
     */
    void changeForegroundColor();

    /**
     * Clears some formatting of selected text
     */
    void clearFormatting();

    /**
     * Inserts image into the node
     */
    void inserImage();

    /**
     * Set color that will be used to set background/foreground color of a text
     */
    void setColor(int color);

    /**
     * Makes selected font bold if there isn't any bold text in selection.
     * Otherwise it will remove bold text  property of the in selected part of the text.
     */
    void toggleFontBold();

    /**
     * Makes selected font italic if there isn't any italic text in selection.
     * Otherwise it will remove italic property of the text in selected part of the text.
     */
    void toggleFontItalic();

    /**
     * Makes selected text strikethrough if there isn't any struckthrough text in selection.
     * Otherwise it will remove strikethrough property of the text in selected part of the text.
     */
    void toggleFontStrikethrough();

    /**
     * Makes selected text underlined if there isn't any underlined text in selection.
     * Otherwise it will remove underlined property of the text in selected part of the text.
     */
    void toggleFontUnderline();
}
