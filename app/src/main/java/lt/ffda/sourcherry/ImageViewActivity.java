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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

public class ImageViewActivity extends AppCompatActivity {

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


        ZoomableImageView imageView = findViewById(R.id.image_activity_imageview);

        // Closes ImageViewActivity on tap
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Sets image to ImageView
        byte[] imageByteArray = getIntent().getExtras().getByteArray("imageByteArray");
        Bitmap decodedByte = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        Drawable image = new BitmapDrawable(this.getResources(),decodedByte);
        imageView.setImageDrawable(image);
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