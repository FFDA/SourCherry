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

package lt.ffda.sourcherry.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReturnSelectedFileUriForSaving extends ActivityResultContract<String[], Intent> {
    // Almost standard Intent.ACTION_CREATE_DOCUMENT, just adds some extra data that is needed to find file in database
    private String nodeUniqueID;
    private String filename;
    private String time;

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, String[] strings) {
        // String[] has to be [FileMimeType, nodeUniqueID, attachedFileFilename, time]
        this.nodeUniqueID = strings[1];
        this.filename = strings[2];
        this.time = strings[3];
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(strings[0]);
        intent.putExtra(Intent.EXTRA_TITLE, this.filename);
        return intent;
    }

    @Override
    public Intent parseResult(int resultCode, @Nullable Intent result) {
        // Returns selected file Uri with attached data needed to retrieve data from database
        if (resultCode != Activity.RESULT_OK || result == null) {
            return null;
        }
        result.putExtra("nodeUniqueID", this.nodeUniqueID);
        result.putExtra("filename", this.filename);
        result.putExtra("time", this.time);
        return result;
    }
}
