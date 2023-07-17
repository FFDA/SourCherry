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

public class ColorConverter {
    public static String rgb24intToRgb24str(int rgb24int) {
        String red = Integer.toHexString((rgb24int >> 16) & 0xff);
        String green = Integer.toHexString((rgb24int >> 8) & 0xff);
        String blue = Integer.toHexString(rgb24int & 0xff);
        return String.format("#%1$s%2$s%3$s", red, green, blue);
    }
}
