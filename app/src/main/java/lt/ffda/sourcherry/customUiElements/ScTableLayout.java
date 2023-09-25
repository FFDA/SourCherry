/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.customUiElements;

import android.content.Context;
import android.widget.TableLayout;

public class ScTableLayout extends TableLayout {
    private String colWidths;
    private byte lightInterface;
    public ScTableLayout(Context context) {
        super(context);
    }

    /**
     * Getting what column widths should be used in CherryTree
     * @return comma separated values for each column. 0 - default value, otherwise - user set value
     */
    public String getColWidths() {
        return colWidths;
    }

    /**
     * Setting what column widths should be used in CherryTree. This value has no effect in SourCherry
     * @param colWidths comma separated values for each column. 0 - default value, otherwise - user set value
     */
    public void setColWidths(String colWidths) {
        this.colWidths = colWidths;
    }

    /**
     * Is table table using lightweight interface in CherryTree
     * @return 1 - if table using lightweight interface, 0 - otherwise
     */
    public byte getLightInterface() {
        return this.lightInterface;
    }

    /**
     * Setting what interface should be used in CherryTree
     * @param light 1 - if table using lightweight interface, 0 - otherwise
     */
    public void setLightInterface(byte light) {
        this.lightInterface = light;
    }
}
