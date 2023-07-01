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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import lt.ffda.sourcherry.MainViewModel;

/**
 * Defines a factory API that enables applications to obtain a database reader that
 * allows to read and write to CherryTree databases
 */
public class DatabaseReaderFactory {

    public DatabaseReaderFactory(){}

    /**
     * Creates an object that implements DatabaseReader interface depending on the database type
     * @param context application context
     * @param handler used by database reader to execute task not on the main thread
     * @param sharedPreferences application preferences
     * @return object implementing DatabaseReader interface
     * @throws IOException exceptions while opening XML type databases
     */
    public DatabaseReader getReader(Context context, Handler handler, SharedPreferences sharedPreferences, MainViewModel mainViewModel) throws IOException, ParserConfigurationException {
        DatabaseReader databaseReader = null;
        String databaseString = sharedPreferences.getString("databaseUri", null);
        if (sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
            // If file is in external storage
            if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                // If file is xml
                InputStream is = context.getContentResolver().openInputStream(Uri.parse(databaseString));
                databaseReader = new XMLReader(databaseString, is, context, handler, mainViewModel);
                is.close();
            }
        } else {
            // If file is in internal app storage
            if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                // If file is xml
                InputStream is = new FileInputStream(sharedPreferences.getString("databaseUri", null));
                databaseReader = new XMLReader(databaseString, is, context, handler, mainViewModel);
                is.close();
            } else {
                // If file is sql (password protected or not)
                SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READWRITE);
                databaseReader = new SQLReader(sqlite, context, handler, mainViewModel);
            }
        }
        return databaseReader;
    }
}
