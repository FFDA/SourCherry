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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;

public class NodeContentFragment extends Fragment {
    private LinearLayout contentFragmentLinearLayout;
    private MainViewModel mainViewModel;
    private Handler handler;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_node_content, container, false);

        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.contentFragmentLinearLayout = rootView.findViewById(R.id.content_fragment_linearlayout);
        this.handler = ((MainView) getActivity()).getHandler();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.options_menu_node_content_fragment, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner() , Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Top and bottom paddings are always the same: 14px (5dp)
        this.contentFragmentLinearLayout.setPadding(this.sharedPreferences.getInt("paddingStart", 14), 14, this.sharedPreferences.getInt("paddingEnd", 14), 14);
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
                tv.setTextIsSelectable(true);
                tv.setMovementMethod(LinkMovementMethod.getInstance()); // Needed to detect click/open links
                tv.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
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
                    headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
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
                        cellTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
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

    public void switchFindInNodeHighlight(int previouslyHighlightedViewIndex, int newResultIndex) {
        // Removes highlighting from TextView which findInNodeStorage index is provided with previouslyHighlightedViewIndex
        // And highlights findInNodeResultStorage item that is provided with newResultIndex

        LinearLayout contentFragmentLinearLayout = getView().findViewById(R.id.content_fragment_linearlayout);
        ScrollView contentFragmentScrollView = getView().findViewById(R.id.content_fragment_scrollview);
        int lineCounter = 0; // Needed to calculate position where view will have to be scrolled to
        int counter = 0; // Counts iteration over node layout. Counts every TextView

        int viewCounter = this.mainViewModel.getFindInNodeResult(newResultIndex)[0]; // Saved findInNodeStorage view index
        int startIndex = this.mainViewModel.getFindInNodeResult(newResultIndex)[1]; // Search result substring start index
        int endIndex = this.mainViewModel.getFindInNodeResult(newResultIndex)[2]; // Search result substring end index

        for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
            View view = contentFragmentLinearLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView currentTextView = (TextView) view;
                if (previouslyHighlightedViewIndex != viewCounter) {
                    // If substring that has to be marked IS NOT IN the same view as previously marked substring
                    if (previouslyHighlightedViewIndex == counter) {
                        // If encountered the view that was previously marked view
                        // It is restored to original state
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                        spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                        currentTextView.setText(spannedSearchQuery);
                    }
                    if (viewCounter == counter) {
                        // If encountered the view that has to be marked now
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                        spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                        spannedSearchQuery.setSpan(new BackgroundColorSpan(getContext().getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        currentTextView.setText(spannedSearchQuery);
                        int line = currentTextView.getLayout().getLineForOffset(startIndex);
                        int scrollTo = currentTextView.getLayout().getLineTop(line) + lineCounter;
                        if (scrollTo < (contentFragmentLinearLayout.getHeight() - contentFragmentScrollView.getHeight())) {
                            scrollTo -= 100;
                        }
                        contentFragmentScrollView.scrollTo(0, scrollTo);
                    }
                } else {
                    // If substring that has to be marked IS IN the same view as previously marked substring
                    // Previous "highlight" will be removed while marking the current one
                    if (viewCounter == counter) {
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                        spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                        spannedSearchQuery.setSpan(new BackgroundColorSpan(getContext().getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        currentTextView.setText(spannedSearchQuery);
                        int line = currentTextView.getLayout().getLineForOffset(startIndex);
                        int scrollTo = currentTextView.getLayout().getLineTop(line) + lineCounter;
                        if (scrollTo < (contentFragmentLinearLayout.getHeight() - contentFragmentScrollView.getHeight())) {
                           scrollTo -= 100;
                        }
                        contentFragmentScrollView.scrollTo(0, scrollTo);
                    }
                }
                lineCounter += currentTextView.getHeight();
                counter++;
            } else if (view instanceof HorizontalScrollView) {
                // If encountered a table
                TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                if (previouslyHighlightedViewIndex != viewCounter) {
                    // If substring that has to be marked IS NOT IN the same view as previously marked substring
                    for (int row = 0; row < tableLayout.getChildCount(); row++) {
                        TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                        for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                            if (previouslyHighlightedViewIndex == counter) {
                                // If encountered view that was previously highlighted
                                SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                                spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                                ((TextView) tableRow.getChildAt(cell)).setText(spannedSearchQuery);
                            }
                            if (viewCounter == counter) {
                                // If encountered a view that has to be highlighted
                                SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                                spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                                spannedSearchQuery.setSpan(new BackgroundColorSpan(getContext().getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                ((TextView) tableRow.getChildAt(cell)).setText(spannedSearchQuery);
                                contentFragmentScrollView.scrollTo(0, lineCounter - 100);
                            }
                            counter++;
                        }
                        lineCounter += tableRow.getHeight();
                    }
                } else {
                    // If substring that has to be marked IS IN the same view as previously marked substring
                    for (int row = 0; row < tableLayout.getChildCount(); row++) {
                        TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                        for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                            if (viewCounter == counter) {
                                // If encountered a view that has to be marked
                                SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                                spannedSearchQuery.append(this.mainViewModel.getFindInNodeStorageItem(counter));
                                spannedSearchQuery.setSpan(new BackgroundColorSpan(getContext().getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                ((TextView) tableRow.getChildAt(cell)).setText(spannedSearchQuery);
                                contentFragmentScrollView.scrollTo(0, lineCounter - 100);
                            }
                            counter++;
                        }
                        lineCounter += tableRow.getHeight();
                    }
                }
            }
        }
    }

    /**
     * Removes node content from view, mainViewModel
     */
    public void removeLoadedNodeContent() {
        if (this.contentFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeContentFragment.this.contentFragmentLinearLayout.removeAllViews();
                }
            });
        }
        this.mainViewModel.deleteNodeContent();
    }
}