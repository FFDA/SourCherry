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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.InputStream;
import java.security.MessageDigest;

import lt.ffda.sourcherry.model.FileInfo;

/**
 * Class to extract information from filenames
 */
public class Files {

    /**
     * Returns file's sha256sum
     * @param inputStream input stream of the file
     * @return sha256sum of the file
     * @throws Exception when fails to read the file or open MessageDigest instance
     */
    public static String calculateFileSha256Sum(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int length;
        byte[] buf = new byte[4 * 1024];
        while ((length = inputStream.read(buf, 0, buf.length)) != -1) {
            digest.update(buf, 0, length);
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Returns file's sha256sum
     * @param context context to open the file
     * @param uri uri of the file to calculate sha256sum
     * @return sha256sum of the file
     * @throws Exception when fails to read the file or open MessageDigest instance
     */
    public static String calculateFileSha256Sum(Context context, Uri uri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            return calculateFileSha256Sum(inputStream);
        }
    }

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

    /**
     * Returns name or if file has more than one dot - first part of the name. If filename does not
     * have any dots in it - returns all of it.
     * @param filename filename to get the file filename from
     * @return name of the file
     */
    public static String getFileName(String filename) {
        String[] splitFilename = filename.split("\\.");
        if (splitFilename.length > 1) {
            return splitFilename[0];
        } else {
            return filename;
        }
    }

    /**
     * Returns timestamp of the file
     * @param contentResolver content resolver to query for file information
     * @param uri Uri of the file to get timestamp for
     * @return timestamp or -1 if file was not found
     */
    public static long getFileTimestamp(ContentResolver contentResolver, Uri uri) {
        try (Cursor cursor = contentResolver.query(uri, new String[]{"last_modified"}, null, null, null)) {
            if (cursor != null && cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }
        return -1;
    }

    /**
     * Collects data about file in folder of provided Uri
     * @param contentResolver content resolver to retrieve the database file
     * @param folderUri Uri string of the root folder. Uri has to have permission to manage files in the folder
     * @param filename filename of the file to collect data about
     * @return FileInfo DTO with filled data about the file if it was found
     */
    public static FileInfo getFileUriAndModDate(ContentResolver contentResolver, String folderUri, String filename) {
        FileInfo data = new FileInfo();
        Uri mirrorDatabaseFolderUri = Uri.parse(folderUri);
        Uri mirrorDatabaseFolderChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDatabaseFolderUri,
                DocumentsContract.getTreeDocumentId(mirrorDatabaseFolderUri));
        try (Cursor cursor = contentResolver.query(mirrorDatabaseFolderChildrenUri,
                new String[]{"document_id", "_display_name", "last_modified"}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                if (cursor.getString(1).equals(filename)) {
                    data.setUri(DocumentsContract.buildDocumentUriUsingTree(mirrorDatabaseFolderUri, cursor.getString(0)));
                    data.setModified(cursor.getLong(2));
                    break;
                }
            }
        }

        return data;
    }
}
