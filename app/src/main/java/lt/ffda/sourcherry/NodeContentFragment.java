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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import lt.ffda.sourcherry.R;

import java.util.ArrayList;

public class NodeContentFragment extends Fragment {
    private LinearLayout contentFragmentLinearLayout;
    private MainViewModel mainViewModel;
    private Handler handler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.node_content_fragment, container, false);

        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.contentFragmentLinearLayout = rootView.findViewById(R.id.content_fragment_linearlayout);
        this.handler = ((MainView) getActivity()).getHandler();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Top and bottom paddings are always the same: 14px (5dp)
        this.contentFragmentLinearLayout.setPadding(sharedPreferences.getInt("paddingStart", 14), 14, sharedPreferences.getInt("paddingEnd", 14), 14);
    }

    public void loadContent() {

        // Clears layout just in case. Most of the time it is needed
        if (this.contentFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeContentFragment.this.contentFragmentLinearLayout.removeAllViews();
                }
            });
        }

        for (ArrayList<CharSequence[]> part: mainViewModel.getNodeContent()) {
            CharSequence[] type = part.get(0);
            if (type[0].equals("text")) {
                // This adds not only text, but images, codeboxes
                CharSequence[] textContent = part.get(1);
                SpannableStringBuilder nodeContentSSB = (SpannableStringBuilder) textContent[0];
                TextView tv = new TextView(getActivity());
                tv.setTextSize(16);
                tv.setTextIsSelectable(true);
                tv.setMovementMethod(CustomMovementMethod.getInstance()); // Needed to detect click/open links
                tv.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeContentFragment.this.contentFragmentLinearLayout.addView(tv);
                    }
                });
            }
            if (type[0].equals("table")) {
                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
                TableLayout table = new TableLayout(getActivity());

                //// Getting max and min column values from table
                // Multiplying by arbitrary number to make it look better.
                // For some reason table that looks good in PC version looks worse on android
                int colMax = (int) (Integer.parseInt((String) type[1]) * 1.3);
                int colMin = (int) (Integer.parseInt((String) type[2]) * 1.3);
                ////

                // Wraps content in cell correctly
                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

                //// Creates and formats header for the table
                CharSequence[] tableHeaderCells = part.get(part.size() - 1);
                TableRow tableHeaderRow = new TableRow(getActivity());

                for (CharSequence cell: tableHeaderCells) {
                    TextView headerTextView = new TextView(getActivity());
                    headerTextView.setBackground(getActivity().getDrawable(R.drawable.table_header_cell));
                    headerTextView.setMinWidth(colMin);
                    headerTextView.setMaxWidth(colMax);
                    headerTextView.setPadding(10,10,10,10);
                    headerTextView.setLayoutParams(params);
                    headerTextView.setText(cell);
                    tableHeaderRow.addView(headerTextView);
                }
                table.addView(tableHeaderRow);
                ////

                //// Creates and formats data for the table
                for (int row = 1; row < part.size() - 1; row++) {
                    TableRow tableRow = new TableRow(getActivity());
                    CharSequence[] tableRowCells = part.get(row);
                    for (CharSequence cell: tableRowCells) {
                        TextView cellTextView = new TextView(getActivity());
                        cellTextView.setBackground(getActivity().getDrawable(R.drawable.table_data_cell));
                        cellTextView.setMinWidth(colMin);
                        cellTextView.setMaxWidth(colMax);
                        cellTextView.setPadding(10,10,10,10);
                        cellTextView.setLayoutParams(params);
                        cellTextView.setText(cell);
                        tableRow.addView(cellTextView);
                    }
                    table.addView(tableRow);
                }
                ////

                table.setBackground(getActivity().getDrawable(R.drawable.table_border));
                tableScrollView.addView(table);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeContentFragment.this.contentFragmentLinearLayout.addView(tableScrollView);
                    }
                });
            }
        }
    }
}