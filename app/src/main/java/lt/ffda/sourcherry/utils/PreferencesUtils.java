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

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Class holds commands to disable specific preferences at one
 */
public class PreferencesUtils {

    /**
     * Disables MirrorDatabase preferences
     * @param sharedPreferences apps SharedPreferences instance
     */
    public static void disableMirrorDatabase(@NonNull SharedPreferences sharedPreferences) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("mirror_database_switch", false);
        sharedPreferencesEditor.putBoolean("mirror_database_auto_export_switch", false);
        sharedPreferencesEditor.remove("mirrorDatabaseFilename");
        sharedPreferencesEditor.remove("mirrorDatabaseLastModified");
        sharedPreferencesEditor.commit();
    }
}
