/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.InputStream;

import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.database.SQLReader;
import lt.ffda.sourcherry.database.XMLReader;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class ImageViewActivity extends AppCompatActivity {
    private DatabaseReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // Displays toolbar
        Toolbar imageViewActivityToolbar = findViewById(R.id.image_activity_toolbar);
        setSupportActionBar(imageViewActivityToolbar);
        ActionBar toolbar = getSupportActionBar();
        toolbar.setDisplayHomeAsUpEnabled(true); // Enables home (arrow back button)
        toolbar.setDisplayShowTitleEnabled(false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ZoomableImageView imageView = findViewById(R.id.image_activity_imageview);
        // Closes ImageViewActivity on tap
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (getIntent().getExtras().getString("type").equals("image")) {
            String databaseString = sharedPreferences.getString("databaseUri", null);
            Handler handler = new Handler(Looper.getMainLooper());

            try {
                if (sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
                    // If file is in external storage
                    if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                        // If file is xml
                        InputStream is = getContentResolver().openInputStream(Uri.parse(databaseString));
                        this.reader = new XMLReader(databaseString, is, this, handler);
                        is.close();
                    }
                } else {
                    // If file is in internal app storage
                    if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                        // If file is xml
                        InputStream is = new FileInputStream(sharedPreferences.getString("databaseUri", null));
                        this.reader = new XMLReader(databaseString, is, this, handler);
                        is.close();
                    } else {
                        // If file is sql (password protected or not)
                        SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READONLY);
                        this.reader = new SQLReader(databaseString, sqlite, this, handler);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Sets image to ImageView
            byte[] imageByteArray = this.reader.getImageByteArray(getIntent().getExtras().getString("imageNodeUniqueID"), getIntent().getExtras().getString("imageOffset"));
            if (imageByteArray != null) {
                Bitmap decodedByte = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
                Drawable image = new BitmapDrawable(this.getResources(),decodedByte);
                imageView.setImageDrawable(image);
            } else {
                Toast.makeText(this, R.string.toast_error_failed_to_load_image, Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                final JLatexMathDrawable latexDrawable = JLatexMathDrawable.builder(getIntent().getExtras().getString("latexString"))
                        .textSize(40)
                        .padding(8)
                        .background(0xFFffffff)
                        .align(JLatexMathDrawable.ALIGN_RIGHT)
                        .build();
                imageView.setImageDrawable(latexDrawable);
            } catch (Exception e) {
                Toast.makeText(this, R.string.toast_error_failed_to_compile_latex, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Overrides default action for back button that reloaded MainView activity to blank screen
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}