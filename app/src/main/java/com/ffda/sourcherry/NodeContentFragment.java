package com.ffda.sourcherry;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class NodeContentFragment extends Fragment {
    private LinearLayout contentFragmentLinearLayout;
    private ArrayList<ArrayList<CharSequence[]>> nodeContent;

    @Override
    public void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.nodeContent != null) {
            this.loadContent();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_content_fragment, container, false);

        this.contentFragmentLinearLayout = (LinearLayout) rootView.findViewById(R.id.content_fragment_linearlayout);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (this.nodeContent != null) {
            this.loadContent();
        }
    }

    public void loadContent() {

        // Clears layout just in case. Most of the time it is needed
        this.contentFragmentLinearLayout.removeAllViews();

        for (ArrayList part: this.nodeContent) {
            CharSequence[] type = (CharSequence[]) part.get(0);
            if (type[0].equals("text")) {
                // This adds not only text, but images, codeboxes
                CharSequence[] textContent = (CharSequence[]) part.get(1);
                SpannableStringBuilder nodeContentSSB = (SpannableStringBuilder) textContent[0];
                TextView tv = new TextView(getActivity());
                tv.setTextSize(16);
                tv.setTextIsSelectable(true);
                tv.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                this.contentFragmentLinearLayout.addView(tv);
            }
            if (type[0].equals("table")) {
                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
                TableLayout table = new TableLayout(getActivity());

                //// Getting max and min column values from table
                // Multiplying by arbitrary number to make it look better.
                // For some reason table that looks good in PC version looks worse on android
                int colMax = (int) (Integer.valueOf((String) type[1]) * 1.3);
                int colMin = (int) (Integer.valueOf((String) type[2]) * 1.3);
                ////

                // Wraps content in cell correctly
                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

                //// Creates and formats header for the table
                CharSequence[] tableHeaderCells = (CharSequence[]) part.get(part.size() - 1);
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
                    CharSequence[] tableRowCells = (CharSequence[]) part.get(row);
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
                this.contentFragmentLinearLayout.addView(tableScrollView);
            }
        }
    }

    public void setNodeContent(ArrayList<ArrayList<CharSequence[]>> nodeContent) {
        this.nodeContent = nodeContent;
    }
}