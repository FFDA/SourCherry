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

/**
 * Enum for unordered/bulleted list items
 */
public enum UnorderedSwitch {
    BULLTET(8226, "•"), // First level bullet point U+2022
    DIAMOND(9671, "◇"), // Second level bullet point U+25C7
    BLACK_SMALL_SQUERE(9642, "▪"), // Third level bullet point U+25AA
    HYPHEN_MINUS(45, "-"), // Forth level bullet point U+002D
    RIGHTWARDS_ARROW(8594, "→"), // Fifth level bullter point U+2192
    RIGHTWARDS_DOUBLE_ARROW(8658, "⇒"); // Sixth level bullter point U+21D2

    private final int code;
    private final String string;

    UnorderedSwitch(int code, String string) {
        this.code = code;
        this.string = string;
    }

    /**
     * Returns unicode decimal code of the unordered list item
     * @return unicode decimal code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns CharSequence value of unordered list item that can be used in replace method
     * @return CharSequence value of nordered list item
     */
    public String getString() {
        return string;
    }

    /**
     * Returns unordered list item string value for a specific level. Unordered lists have 6 levels
     * @param level item's level
     * @return unordered list sting item
     */
    public static String getItemForLevel(int level) {
        switch (level) {
            case 1:
                return DIAMOND.string;
            case 2:
                return BLACK_SMALL_SQUERE.string;
            case 3:
                return HYPHEN_MINUS.string;
            case 4:
                return RIGHTWARDS_ARROW.string;
            case 5:
                return RIGHTWARDS_DOUBLE_ARROW.string;
            default:
                return BULLTET.string;
        }
    }
}
