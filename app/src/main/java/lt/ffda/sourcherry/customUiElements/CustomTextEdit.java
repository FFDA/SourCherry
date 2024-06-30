/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

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
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * Used with custom_edittext.xml to display editable text in nodeContentEditor fragment.
 * Has a boolean to mark it as a table cell. Defaults to reporting that it is not a
 * table cell.
 */
public class CustomTextEdit extends AppCompatEditText {
    private boolean tableCell = false;

    public CustomTextEdit(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Check if CustomTextEdit is marked as a table cell
     * @return true - table cell, false - otherwise
     */
    public boolean isTableCell() {
        return tableCell;
    }

    /**
     * Mark CustomTextEdit as a table cell. Defaults as false when creating a new CustomTextEdit.
     * @param tableCell true - is a table cell, false - otherwise
     */
    public void setTableCell(boolean tableCell) {
        this.tableCell = tableCell;
    }
}
