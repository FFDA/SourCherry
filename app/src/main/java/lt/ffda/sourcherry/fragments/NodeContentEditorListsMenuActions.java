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

public interface NodeContentEditorListsMenuActions {

    /**
     * Starts checklist at the current line. Insert empty checkbox ant the beginning of the current
     * line. If there is a checkbox at the start of the line - removes it.
     */
    void startChecklist();

    /**
     * Starts unordered list at the current line. If the line is already an unordered list line -
     * makes it a normal line.
     */
    void startUnordered();

    /**
     * Starts ordered list at the current line. If the line is already an ordered list line -
     * makes it a normal line.
     */
    void startOrdered();

    /**
     * Decreases list item indentation. Changes list items symbol if necessary.
     */
    void decreaseListItemIndentation();

    /**
     * Increases list item indentation. Changes list items symbol if necessary.
     */
    void increaseListItemIndentation();
}
