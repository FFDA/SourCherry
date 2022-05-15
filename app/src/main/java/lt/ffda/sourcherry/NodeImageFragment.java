/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import lt.ffda.sourcherry.R;

public class NodeImageFragment extends Fragment {
    public NodeImageFragment() {
        super(R.layout.node_image_fragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_image_fragment, container, false);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.image_fragment_imageview);
        // Inflate the layout for this fragment
        Spannable imageSpan = (Spannable) requireArguments().getCharSequence("image"); // Retrieving Span from bundle
        ImageSpan[] img = imageSpan.getSpans(0, imageSpan.length(), ImageSpan.class); // Converting Span to ImageSpan
        Drawable image = img[0].getDrawable(); // Getting Drawable from ImageSpan

        imageView.setImageDrawable(image);

        //// Rotating and scaling image up if image would be better to see in landscape mode
        if (image.getIntrinsicHeight() < image.getIntrinsicWidth()) {
            // Getting screen dimensions for calculations
            int widthScreen = Resources.getSystem().getDisplayMetrics().widthPixels;
            int heightScreen = Resources.getSystem().getDisplayMetrics().heightPixels;
            int heightImage = image.getIntrinsicHeight();
            int widthImage = image.getIntrinsicWidth();

            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Calculating two scale type and using the smaller one
            float scale1 = (float) (widthScreen/heightImage - 0.2);
            float scale2 = (float) (heightScreen/widthImage - 0.2);
            if (scale1 < scale2) {
                imageView.setScaleX(scale1);
                imageView.setScaleY(scale1);
            } else {
                imageView.setScaleX(scale2);
                imageView.setScaleY(scale2);
            }
            // Rotating ImageView
            imageView.setRotation(90);
        }
        ////

        return rootView;
    }
}
