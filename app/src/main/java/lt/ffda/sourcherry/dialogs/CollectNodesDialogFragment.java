/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;

import lt.ffda.sourcherry.MainActivity;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.runnables.CollectNodesRunnable;
import lt.ffda.sourcherry.runnables.NodesCollectedCallback;

public class CollectNodesDialogFragment extends DialogFragment {
    private TextView textView; // Message to the user where the count of scanned node will be displayed
    private ExecutorService executor;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_collect_nodes, null);
        builder.setTitle(R.string.dialog_fragment_collect_nodes_title);
        builder.setView(view);
        setCancelable(false); // Not allowing user to cancel the the dialog fragment
        this.executor = Executors.newSingleThreadExecutor();
        this.textView = view.findViewById(R.id.dialog_fragment_collect_nodes_message);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Handler handler = new Handler(Looper.getMainLooper());
        try {
            this.executor.submit(new CollectNodesRunnable(Uri.parse(getArguments().getString("uri")), getContext(), handler, this.textView, new NodesCollectedCallback() {
                @Override
                public void onNodesCollected(boolean success) {
                    if (success) {
                        executor.shutdown();
                        ((MainActivity) getActivity()).startMainViewActivity();
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), R.string.toast_error_failed_to_collect_drawer_menu_xml, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                }
            }));
        } catch (ParserConfigurationException e) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_collect_drawer_menu_xml, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.executor.shutdown();
    }
}
