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
 * Enum of checkbox states
 */
public enum CheckBoxSwitch {
    EMPTY(9744),
    CHECKED(9745),
    CROSSED(9746);

    private final int code;

    CheckBoxSwitch(int code) {
        this.code = code;
    }

    /**
     * Returns unicode decimal code of the checkbox
     * @return unicode decimal code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns CharSequence value of CheckBox that can be used in replace method
     * @return CharSequence value of CheckBox
     */
    public CharSequence getCharSequence() {
        return String.valueOf((char) code);
    }
}
