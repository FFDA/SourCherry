/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.utils;

public enum ColorPickerPresets {
    BLUE(-10313494),
    GREEN(-11017335),
    YELLOW(-465828),
    ORANGE(-23736),
    RED(-630447),
    VIOLET(-2323747),
    BROWN(-3298417),
    WHITE(-1),
    BLACK(-8948101);

    private final int color;
    ColorPickerPresets(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
