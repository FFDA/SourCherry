/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.model;

import android.net.Uri;

/**
 * Holds data for the MirrorDatabase file found in MirrorDatabase folder set in settings
 */
public class FileInfo {
    private long modified = 0;
    private Uri uri;

    /**
     * Get last modified date if the file
     * @return last modified of the database file, 0 - if database file was not found
     */
    public long getModified() {
        return modified;
    }

    /**
     * Get Uri of found mirror database
     * @return Uri of file
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Set last modified date of the file
     * @param modified last modified date
     */
    public void setModified(long modified) {
        this.modified = modified;
    }

    /**
     * Set Uri of the mirror database
     * @param uri Uri of file
     */
    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
