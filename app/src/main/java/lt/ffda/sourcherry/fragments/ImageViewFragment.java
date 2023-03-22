/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ZoomableImageView;
import lt.ffda.sourcherry.database.DatabaseReader;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class ImageViewFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_image_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ZoomableImageView imageView = view.findViewById(R.id.image_fragment_imageview);
        // Closes fragment on image tap
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        // Loading new menu for the fragment
        // that only have a save button in it
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    getActivity().onBackPressed();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
        this.loadImage();
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            remove(); // Otherwise there will be onBackPressed infinite loop
            ((MainView) getActivity()).returnFromFragmentWithHomeButton();
        }
    };

    private void loadImage() {
        ZoomableImageView imageView = getView().findViewById(R.id.image_fragment_imageview);
        if (getArguments().getString("type").equals("image")) {
            DatabaseReader reader = ((MainView) getActivity()).getReader();
            // Sets image to ImageView
            byte[] imageByteArray = reader.getImageByteArray(getArguments().getString("nodeUniqueID"), getArguments().getString("imageOffset"));
            if (imageByteArray != null) {
                Bitmap decodedByte = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
                Drawable image = new BitmapDrawable(this.getResources(),decodedByte);
                imageView.setImageDrawable(image);
            } else {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_load_image, Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                final JLatexMathDrawable latexDrawable = JLatexMathDrawable.builder(getArguments().getString("latexString"))
                        .textSize(40)
                        .padding(8)
                        .background(0xFFffffff)
                        .align(JLatexMathDrawable.ALIGN_RIGHT)
                        .build();
                imageView.setImageDrawable(latexDrawable);
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_compile_latex, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
