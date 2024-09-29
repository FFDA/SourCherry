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

import android.text.style.BackgroundColorSpan;

/**
 * Span to mark background color of the monospace font spans. Needed to separate between CodeBox,
 * Background and Monospace font background spans.
 */
public class MonospaceBackgroundColorSpan extends BackgroundColorSpan {
    public MonospaceBackgroundColorSpan(int color) {
        super(color);
    }
}
