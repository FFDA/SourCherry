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

import android.content.res.Resources;

public class Calculations {
    /**
     * Converts provided PX value to DP and returns it
     * @param paddingInPX padding value in PX to be converted
     * @return padding value in DP
     */
    public static int pxToDp(int paddingInPX) {
        return (int) (paddingInPX / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Converts provided DP value to PX and returns it
     * @param paddingInDP padding value in DP to be converted
     * @return padding value in PX
     */
    public static int dpToPx(int paddingInDP) {
        return (int) (paddingInDP * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Calculates position to scroll horizontally to display FindInNode result to the user
     * @param scrollToX primary horizontal position
     * @return posittion to scroll horizontally
     */
    public static int adjustScrollToX(float scrollToX) {
        if (Resources.getSystem().getDisplayMetrics().density < scrollToX) {
            scrollToX -= 40; // Arbitrary position adjustment
        }
        return (int) scrollToX;
    }
}
