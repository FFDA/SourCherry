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
