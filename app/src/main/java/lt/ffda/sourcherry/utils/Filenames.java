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
 * Class to extract information from filenames
 */
public class Filenames {

    /**
     * Returns extension of the filename or null if it was impossible to find one
     * @param filename filename to get the file extension from
     * @return extension of the filename or null
     */
    public static String getFileExtension(String filename) {
        String[] splitFilename = filename.split("\\.");
        if (splitFilename.length > 1) {
            return splitFilename[splitFilename.length - 1];
        } else {
            return null;
        }
    }
}
