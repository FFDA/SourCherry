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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import lt.ffda.sourcherry.AppContainer;
import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ScApplication;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.database.MultiReader;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;

public class NodeContentFragment extends Fragment {
    OnBackPressedCallback callbackDisplayToastBeforeExit = createCallbackDisplayToastBeforeExit();
    private boolean backToExit;
    private LinearLayout contentFragmentLinearLayout;
    private Handler handler;
    private MainViewModel mainViewModel;
    private SharedPreferences sharedPreferences;

    /**
     * Create OnBackPressedCallback used to deal with clicks in NodeContentFragment fragemnt.
     * If there are any fragment in the BackStack - removes one. Handles back to exit to make user
     * double press back button to exit.
     * @return OnBackPressedCallback for use in the fragment
     */
    private OnBackPressedCallback createCallbackDisplayToastBeforeExit() {
        return new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (backToExit) { // If button back was already pressed once
                    getActivity().finish();
                    return;
                }

                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                    ((MainView) getActivity()).enableDrawer();
                    return;
                }

                backToExit = true; // Marks that back button was pressed once
                Toast.makeText(getContext(), R.string.toast_confirm_mainview_exit, Toast.LENGTH_SHORT).show();

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    // Reverts boolean that marks if user pressed back button once after 2 seconds
                    @Override
                    public void run() {
                        backToExit = false;
                    }
                }, 2000);
            }
        };
    }

    /**
     * Creates TextView that is ready to be inserted into table as a cell.
     * @param header true - cell will be used in the header, false - normal table cell
     * @param params parameters of the EditText (width, height) otherwise it will net be displayed
     * @param typeface font be set on the text in the cell. Null will use default android font
     * @param textSize textSize to be set on the text in the cell
     * @param colWidth width of the cell. Will not be appliead to header cell.
     * @param content content of the cell.
     * @return TextView ready to be inserted in the table as a cell
     */
    private TextView createTableCell(boolean header, ViewGroup.LayoutParams params, Typeface typeface, int textSize, int colWidth, CharSequence content) {
        TextView cell = new TextView(getActivity());
        if (header) {
            cell.setBackground(getActivity().getDrawable(R.drawable.table_header_cell));
            cell.setTypeface(typeface, Typeface.BOLD);
        } else {
            cell.setBackground(getActivity().getDrawable(R.drawable.table_data_cell));
            cell.setMaxWidth(colWidth);
            cell.setTypeface(typeface);
        }
        cell.setTextIsSelectable(true);
        cell.setPadding(10,10,10,10);
        cell.setLayoutParams(params);
        cell.setText(content);
        cell.setMinWidth(100);
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        return cell;
    }

    public void loadContent() {
        if (mainViewModel.getNodeContent().getValue() == null) {
            return;
        }
        // Clears layout just in case. Most of the time it is needed
        if (contentFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    contentFragmentLinearLayout.removeAllViews();
                }
            });
        }
        Typeface typeface = null;
        switch (sharedPreferences.getString("preference_font_type", "Default")) {
            case "Comfortaa":
                typeface = ResourcesCompat.getFont(getContext(), R.font.comfortaa_regular);
                break;
            case "Merriweather":
                typeface = ResourcesCompat.getFont(getContext(), R.font.merriweather_regular);
                break;
            case "Caladea":
                typeface = ResourcesCompat.getFont(getContext(), R.font.caladea_regular);
                break;
            case "Monospace":
                typeface = Typeface.MONOSPACE;
                break;
        }
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        for (ScNodeContent part: mainViewModel.getNodeContent().getValue()) {
            if (part.getContentType() == 0) {
                ScNodeContentText scNodeContentText = (ScNodeContentText) part;
                SpannableStringBuilder nodeContentSSB = scNodeContentText.getContent();
                TextView tv = new TextView(getActivity());
                tv.setTextIsSelectable(true);
                tv.setMovementMethod(LinkMovementMethod.getInstance()); // Needed to detect click/open links
                tv.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                tv.setTextSize(textSize);
                if (typeface != null) {
                    tv.setTypeface(typeface);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        contentFragmentLinearLayout.addView(tv);
                    }
                });
            } else {
                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
                TableLayout table = new TableLayout(getActivity());
                ScNodeContentTable scNodeContentTable = (ScNodeContentTable) part;
                // Setting gravity for the table
                LinearLayout.LayoutParams tableScrollViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                switch (scNodeContentTable.getJustification()) {
                    case "right":
                        tableScrollViewParams.gravity = Gravity.RIGHT;
                        break;
                    case "center":
                        tableScrollViewParams.gravity = Gravity.CENTER;
                        break;
                    case "fill":
                        tableScrollViewParams.gravity = Gravity.FILL;
                        break;
                }
                tableScrollView.setLayoutParams(tableScrollViewParams);
                // Multiplying by arbitrary number to make table cells look better.
                // For some reason table that looks good in PC version looks worse on android
                int colMin = (int) (scNodeContentTable.getColMin() * 1.3);
                // Wraps content in cell correctly
                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
                //// Creates and formats header for the table
                CharSequence[] tableHeaderCells = scNodeContentTable.getContent().get(0);
                TableRow tableHeaderRow = new TableRow(getActivity());
                for (CharSequence cell: tableHeaderCells) {
                    tableHeaderRow.addView(createTableCell(true, params, typeface, textSize, colMin, cell));
                }
                table.addView(tableHeaderRow);
                ////

                //// Creates and formats data for the table
                for (int row = 1; row < scNodeContentTable.getContent().size(); row++) {
                    TableRow tableRow = new TableRow(getActivity());
                    CharSequence[] tableRowCells = scNodeContentTable.getContent().get(row);
                    for (CharSequence cell: tableRowCells) {
                        tableRow.addView(createTableCell(false, params, typeface, textSize, colMin, cell));
                    }
                    table.addView(tableRow);
                }
                ////

                table.setBackground(getActivity().getDrawable(R.drawable.table_border));
                tableScrollView.addView(table);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        contentFragmentLinearLayout.addView(tableScrollView);
                    }
                });
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_node_content, container, false);
        contentFragmentLinearLayout = rootView.findViewById(R.id.content_fragment_linearlayout);
        ViewCompat.setOnApplyWindowInsetsListener(contentFragmentLinearLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), ((MainView) getActivity()).getFindInNodeToggle() ? 0 : insets.bottom);
            return windowInsets;
        });

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        handler = appContainer.handler;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        backToExit = false;
        final Observer<ArrayList<ScNodeContent>> contentObserver = new Observer<ArrayList<ScNodeContent>>() {
            @Override
            public void onChanged(ArrayList<ScNodeContent> scNodeContents) {
                loadContent();
            }
        };
        mainViewModel.getNodeContent().observe(getViewLifecycleOwner(), contentObserver);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Top and bottom paddings are always the same: 14px (5dp)
        contentFragmentLinearLayout.setPadding(sharedPreferences.getInt("paddingStart", 14), 14, sharedPreferences.getInt("paddingEnd", 14), 14);
        // Otherwise when content is recreated insets won't be set and some of it will be under navigation bar
        ViewCompat.requestApplyInsets(contentFragmentLinearLayout);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ScrollView scrollView = getView().findViewById(R.id.content_fragment_scrollview);
        outState.putInt("scrollY", scrollView.getScrollY());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.options_menu_node_content_fragment, menu);
                if (DatabaseReaderFactory.getReader() instanceof MultiReader) {
                    menu.findItem(R.id.options_menu_rescan_database).setVisible(true);
                    menu.removeItem(R.id.options_menu_export_database);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner() , Lifecycle.State.RESUMED);
        // Registers listener for back button clicks
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackDisplayToastBeforeExit);

        if (savedInstanceState != null) {
            // Tries to scroll screen to the same location where it was when screen orientation happened
            ScrollView scrollView = view.findViewById(R.id.content_fragment_scrollview);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    double aspectRatio = (double) scrollView.getHeight() / scrollView.getWidth();
                    // Dividing or multiplying aspectRatio by random numbers to get more precise scroll position
                    if (aspectRatio < 1) {
                        aspectRatio /= 0.6;
                    } else {
                        aspectRatio *= 0.82;
                    }
                    scrollView.setScrollY((int) (savedInstanceState.getInt("scrollY") * aspectRatio));
                }
            }, 150);
        }
    }

    /**
     * Removes highlighting from TextView which findInNodeStorage index is provided with previouslyHighlightedViewIndex
     * And highlights findInNodeResultStorage item that is provided with newResultIndex
     * @param previouslyHighlightedViewIndex index of the TextView to remove the highlight. If -1 - no TextView to restore
     * @param newResultIndex index of the TextView to add highlight to
     */
    public void switchFindInNodeHighlight(int previouslyHighlightedViewIndex, int newResultIndex) {
        LinearLayout contentFragmentLinearLayout = getView().findViewById(R.id.content_fragment_linearlayout);
        ScrollView contentFragmentScrollView = getView().findViewById(R.id.content_fragment_scrollview);
        int lineCounter = 0; // Needed to calculate position where view will have to be scrolled to
        int counter = 0; // Counts iteration over node layout. Counts every TextView

        int viewCounter = mainViewModel.getFindInNodeResult(newResultIndex)[0]; // Saved findInNodeStorage view index
        int startIndex = mainViewModel.getFindInNodeResult(newResultIndex)[1]; // Search result substring start index
        int endIndex = mainViewModel.getFindInNodeResult(newResultIndex)[2]; // Search result substring end index

        for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
            View view = contentFragmentLinearLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView currentTextView = (TextView) view;
                if (previouslyHighlightedViewIndex != -1 && previouslyHighlightedViewIndex != viewCounter) {
                    // If substring that has to be marked IS NOT IN the same view as previously marked substring
                    if (previouslyHighlightedViewIndex == counter) {
                        // If encountered the view that was previously marked view
                        // It is restored to original state
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                        spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
                        currentTextView.setText(spannedSearchQuery);
                    }
                    if (viewCounter == counter) {
                        // If encountered the view that has to be marked now
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                        spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
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
                        spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
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
                                spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
                                ((TextView) tableRow.getChildAt(cell)).setText(spannedSearchQuery);
                            }
                            if (viewCounter == counter) {
                                // If encountered a view that has to be highlighted
                                SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                                spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
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
                                spannedSearchQuery.append(mainViewModel.getfindInNodeStorageContent(counter));
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
}