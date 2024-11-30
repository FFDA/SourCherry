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
 * Enum for ordered/numbered list items
 */
public enum OrderedSwitch {
    FULL_STOP(46, "."), // First level ordered list item's mark
    RIGHT_PARENTHESIS(29, ")"), // Second level ordered list item's mark
    HYPHEN_MINUS(45, "-"), // Third level ordered list item's mark
    GREATER_THAN_SIGN(62, ">"); // Fourth level ordered list item's mark

    private final int code;
    private final String string;

    OrderedSwitch(int code, String string) {
        this.code = code;
        this.string = string;
    }

    /**
     * Returns unicode decimal code of the ordered list item
     * @return unicode decimal code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns CharSequence value of ordered list item that can be used in replace method
     * @return CharSequence value of ordered list item
     */
    public String getString() {
        return string;
    }

    /**
     * Returns ordered list item string value for a specific level. Ordered lists have 4 levels
     * @param level item's level
     * @return ordered list string item
     */
    public static String getItemForLevel(int level) {
        switch (level) {
            case 1:
                return RIGHT_PARENTHESIS.string;
            case 2:
                return HYPHEN_MINUS.string;
            case 3:
                return GREATER_THAN_SIGN.string;
            default:
                return FULL_STOP.string;
        }
    }
}
