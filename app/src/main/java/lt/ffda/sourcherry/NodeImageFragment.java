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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lt.ffda.sourcherry.R;

public class NodeImageFragment extends Fragment {
    public NodeImageFragment() {
        super(R.layout.node_image_fragment);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callbackExitImageView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_image_fragment, container, false);

        ZoomableImageView imageView = (ZoomableImageView) rootView.findViewById(R.id.image_fragment_imageview);
        // Inflate the layout for this fragment
        Spannable imageSpan = (Spannable) requireArguments().getCharSequence("image"); // Retrieving Span from bundle
        ImageSpan[] img = imageSpan.getSpans(0, imageSpan.length(), ImageSpan.class); // Converting Span to ImageSpan
        Drawable image = img[0].getDrawable(); // Getting Drawable from ImageSpan

        imageView.setImageDrawable(image);

        return rootView;
    }

    OnBackPressedCallback callbackExitImageView = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {

        getParentFragmentManager().popBackStack();
        }
    };
}
