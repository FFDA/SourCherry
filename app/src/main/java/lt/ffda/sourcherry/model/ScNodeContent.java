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

public interface ScNodeContent {
    /**
     * Get ScNodeContent type
     * @return 0 - text to set into TextView, 1 - table content
     */
    byte getContentType();

    /**
     * Set node content type
     * @param type 0 - text to set into TextView, 1 - table content
     */
    void setContentType(byte type);
}
