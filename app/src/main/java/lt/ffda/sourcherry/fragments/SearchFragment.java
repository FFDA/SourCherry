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

import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import lt.ffda.sourcherry.AppContainer;
import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ScApplication;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.model.ScSearchNode;

public class SearchFragment extends Fragment {
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            remove(); // Otherwise there will be onBackPressed infinite loop
            ((MainView) getActivity()).returnFromFragmentWithHomeButtonAndRestoreTitle();
        }
    };
    private ScheduledThreadPoolExecutor executor;
    private Handler handler;
    private TextView resultCount;
    private ProgressBar searchProgressBar;
    private LinearLayout searchResultLinearLayout;

    /**
     * Changes background of the parts of the string
     * Used to mark search query string in search result samples
     * @param searchResult text to mark
     * @param query text to match for marking
     * @return text with marked matching parts
     */
    private SpannableStringBuilder markSearchQuery(String searchResult, String query) {
        int index = 0; // index of start of the found substring
        int searchLength = query.length();

        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
        spannedSearchQuery.append(searchResult);
        while (index != -1) {
            index = searchResult.indexOf(query, index);
            if (index != -1) {
                int startIndex = index;
                int endIndex = index + searchLength;
                spannedSearchQuery.setSpan(new BackgroundColorSpan(getContext().getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += searchLength; // moves search to the end of the last found string
            }
        }
        return spannedSearchQuery;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        this.searchProgressBar = view.findViewById(R.id.search_fragment_progressBar);
        this.searchResultLinearLayout = view.findViewById(R.id.search_fragment_results_linear_layout);
        this.resultCount = view.findViewById(R.id.search_fragment_result_count);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        this.handler = appContainer.handler;
        this.executor = appContainer.executor;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);

        // Checkmark for user to choose if searcher should skip "excluded" nodes
        CheckBox checkBoxExclude = view.findViewById(R.id.search_fragment_checkbox_exclude);

        // Search field and related functions
        SearchView searchView = view.findViewById(R.id.search_fragment_search_view);
        searchView.setSubmitButtonEnabled(true);
        searchView.requestFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // submits a search request to searcher only when user presses the button
                // gets current value of checkbox as boolean to pass it to searcher
                // boolean tells if search should skip "excluded" nodes or not
                SearchFragment.this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        SearchFragment.this.search(checkBoxExclude.isChecked(), query.toLowerCase());
                    }
                });
                return true;
            }
        });
    }

    /**
     * Executes search and adds results to UI
     * @param noSearch true - exclude nodes marked to exclude from search, false - search all nodes
     * @param query string to search for
     */
    private void search(Boolean noSearch, String query) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchFragment.this.searchProgressBar.setVisibility(View.VISIBLE);
            }
        });

        LayoutInflater layoutInflater = this.getLayoutInflater();

        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchFragment.this.searchResultLinearLayout.removeAllViews();
            }
        });

        ArrayList<ScSearchNode> searchResult = DatabaseReaderFactory.getReader().search(noSearch, query);

        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchFragment.this.resultCount.setText(getString(R.string.options_menu_search_result_count, searchResult.size()));
                SearchFragment.this.resultCount.setVisibility(View.VISIBLE);
            }
        });

        if (searchResult != null) {
            for (ScSearchNode result: searchResult) {
                LinearLayout searchResultItem = (LinearLayout) layoutInflater.inflate(R.layout.item_search_activity_result, null);

                // Title of a search result
                // Because of how strings resources formatting works
                // String has to be created from resource, then all tags html tags have to be converted to normal string elements (<br/> -> \n, etc)
                TextView resultTitle = searchResultItem.findViewById(R.id.search_activity_results_item_title);
                String itemTitle = getString(R.string.options_menu_search_item_title, result.getResultCount(), result.getQuery(), result.getName());
                Spanned styledItemTitle = Html.fromHtml(itemTitle, Html.FROM_HTML_MODE_LEGACY);
                resultTitle.setText(styledItemTitle);

                // if there are more than 3 instances of the query in node
                // adds a string to the bottom of the 3 instances that tells user how many are left
                TextView resultSearchSamples = searchResultItem.findViewById(R.id.search_activity_results_item_search_samples);
                int instanceCount = result.getResultCount();
                if (instanceCount > 3) {
                    String resultText = getString(R.string.options_menu_search_query_instances_node,result.getResultSamples(), instanceCount - 3);
                    Spanned styledResultText = Html.fromHtml(resultText, Html.FROM_HTML_MODE_LEGACY);
                    // Because of how Html.fromHtml (removes spanned formatting) works it is not possible to mark string queries in searcher
                    // it has to be done on this end
                    resultSearchSamples.setText(markSearchQuery(styledResultText.toString(), result.getQuery()));
                } else {
                    Spanned styledResultText = Html.fromHtml(result.getResultSamples(), Html.FROM_HTML_MODE_LEGACY);
                    resultSearchSamples.setText(markSearchQuery(styledResultText.toString(), result.getQuery()));
                }

                // Detects click on search result
                // Returns intent to MainView to load selected node
                searchResultItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainView) getActivity()).openSearchResult(result);
                    }
                });
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        SearchFragment.this.searchResultLinearLayout.addView(searchResultItem);
                    }
                });
            }
        }
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchFragment.this.searchProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }
}
