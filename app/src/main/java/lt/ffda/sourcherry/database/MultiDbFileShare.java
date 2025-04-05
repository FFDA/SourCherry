/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.database;

import android.net.Uri;
public interface MultiDbFileShare {

    /**
     * Returns Uri of the file inside multi file database
     * @param nodeUniqueID unique id of the node
     * @param filename filename of the file attached to the node
     * @param control sha256sum sum of the file when "Use embedded file name on disk" is not turned on
     * @return uri of the file
     */
    public Uri getAttachedFileUri(String nodeUniqueID, String filename, String control);
}
