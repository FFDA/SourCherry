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
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;

/**
 * Defines a factory API that enables applications to obtain a database reader that
 * allows to read and write to CherryTree databases
 */
public class DatabaseReaderFactory {
    private static DatabaseReader databaseReader = null;
    private DatabaseReaderFactory(){}

    /**
     * Creates an object that implements DatabaseReader abstract class depending on the database type
     * @param context application context
     * @param handler used by database reader to execute task on the main thread
     * @param sharedPreferences application preferences
     * @param mainViewModel view model that stores all the node content and drawer menu data
     * @return object implementing DatabaseReader interface
     * @throws IOException exceptions while opening XML type databases
     */
    public static DatabaseReader getReader(Context context, Handler handler, SharedPreferences sharedPreferences, MainViewModel mainViewModel) throws IOException, ParserConfigurationException, TransformerConfigurationException, SAXException, InterruptedException, ExecutionException {
        String databaseString = sharedPreferences.getString("databaseUri", "");
        if (sharedPreferences.getString("databaseStorageType", "").equals("shared")) {
            // If file is in external storage
            if (sharedPreferences.getString("databaseFileExtension", "").equals("ctd")) {
                // If file is xml
                InputStream is = context.getContentResolver().openInputStream(Uri.parse(databaseString));
                databaseReader = new XMLReader(databaseString, is, context, handler, mainViewModel);
                if (is != null) {
                    is.close();
                }
            } else if (sharedPreferences.getString("databaseFileExtension", "").equals("multi")) {
                // Multi-file storage
                databaseReader = new MultiReader(Uri.parse(databaseString), context, handler, mainViewModel);
                File drawerMenuFile = new File(context.getFilesDir(), "drawer_menu.xml");
                if (drawerMenuFile.exists()) {
                    ((MultiReader) databaseReader).setDrawerMenu();
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_error_failed_to_open_multi_database_file, "drawer_menu.xml"), Toast.LENGTH_SHORT).show();
                    ((MainView) context).finish();
                    return null;
                }
            }
        } else {
            // If file is in internal app storage
            if (sharedPreferences.getString("databaseFileExtension", "").equals("ctd")) {
                // If file is xml
                InputStream is = new FileInputStream(sharedPreferences.getString("databaseUri", null));
                databaseReader = new XMLReader(databaseString, is, context, handler, mainViewModel);
                is.close();
            } else {
                // If file is sql (password protected or not)
                SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(databaseString, null, SQLiteDatabase.OPEN_READWRITE);
                databaseReader = new SQLReader(sqlite, context, handler, mainViewModel);
            }
        }
        return databaseReader;
    }

    /**
     * Get last created reader by the factory. This function should be called only from MainView
     * and it's fragments and Activities that were initiated by it
     * @return database reader for the currently opened database
     */
    public static DatabaseReader getReader() {
        return databaseReader;
    }
}
