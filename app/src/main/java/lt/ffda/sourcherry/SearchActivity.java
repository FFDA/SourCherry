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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {
    private DatabaseSearcher searcher;
    private Handler handler;
    private ExecutorService executor;
    private LinearLayout searchResultLinearLayout;
    private TextView resultCount;
    private ProgressBar searchProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        // Displays toolbar
        Toolbar imageViewActivityToolbar = findViewById(R.id.search_activity_toolbar);
        setSupportActionBar(imageViewActivityToolbar);
        ActionBar toolbar = getSupportActionBar();
        toolbar.setDisplayHomeAsUpEnabled(true); // Enables home (arrow back button)
        toolbar.setTitle(R.string.options_menu_item_search);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String databaseString = sharedPreferences.getString("databaseUri", null);
        this.handler = new Handler(Looper.getMainLooper());

        // Creates a searcher
        try {
            if (sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
                // If file is in external storage
                if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = getContentResolver().openInputStream(Uri.parse(databaseString));
                    this.searcher = new XMLSearcher(is);
                    is.close();
                }
            } else {
                // If file is in internal app storage
                if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = new FileInputStream(sharedPreferences.getString("databaseUri", null));
                    this.searcher = new XMLSearcher(is);
                    is.close();
                } else {
                    // If file is sql (password protected or not)
                    SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READONLY);
                    this.searcher = new SQLSearcher(sqlite);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.toast_error_failed_to_read_database, Toast.LENGTH_SHORT).show();
        }

        // Checkmark for user to choose if searcher should skip "excluded" nodes
        CheckBox checkBoxExclude = findViewById(R.id.search_activity_checkbox_exclude);

        // Search field and related functions
        SearchView searchView = findViewById(R.id.search_activity_search_view);
        searchView.setSubmitButtonEnabled(true);
        searchView.requestFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // submits a search request to searcher only when user presses the button
                // gets current value of checkbox as boolean to pass it to searcher
                // boolean tells if search should skip "excluded" nodes or not
                SearchActivity.this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        SearchActivity.this.search(checkBoxExclude.isChecked(), query.toLowerCase());
                    }
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        this.executor = Executors.newSingleThreadExecutor();

        this.searchProgressBar = findViewById(R.id.search_activity_progressBar);
        this.searchResultLinearLayout = findViewById(R.id.search_activity_results_linear_layout);
        this.resultCount = findViewById(R.id.search_activity_result_count);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void search(Boolean noSearch, String query) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchActivity.this.searchProgressBar.setVisibility(View.VISIBLE);
            }
        });

        LayoutInflater layoutInflater = this.getLayoutInflater();

        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchActivity.this.searchResultLinearLayout.removeAllViews();
            }
        });

        ArrayList<String[]> searchResult = this.searcher.search(noSearch, query);

        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchActivity.this.resultCount.setText(getString(R.string.options_menu_search_result_count, searchResult.size()));
                SearchActivity.this.resultCount.setVisibility(View.VISIBLE);
            }
        });


        if (searchResult != null) {
            for (String[] result: searchResult) {
                LinearLayout searchResultItem = (LinearLayout) layoutInflater.inflate(R.layout.search_activity_result_item, null);

                // Title of a search result
                // Because of how strings resources formatting works
                // String has to be created from resource, then all tags html tags have to be converted to normal string elements (<br/> -> \n, etc)
                TextView resultTitle = searchResultItem.findViewById(R.id.search_activity_results_item_title);
                String itemTitle = getString(R.string.options_menu_search_item_title, result[3], result[2], result[0]);
                Spanned styledItemTitle = Html.fromHtml(itemTitle, Html.FROM_HTML_MODE_LEGACY);
                resultTitle.setText(styledItemTitle);

                // if there are more than 3 instances of the query in node
                // adds a string to the bottom of the 3 instances that tells user how many are left
                TextView resultSearchSamples = searchResultItem.findViewById(R.id.search_activity_results_item_search_samples);
                int instanceCount = Integer.parseInt(result[3]);
                if (instanceCount > 3) {
                    String resultText = getString(R.string.options_menu_search_query_instances_node,result[4], instanceCount - 3);
                    Spanned styledResultText = Html.fromHtml(resultText, Html.FROM_HTML_MODE_LEGACY);
                    // Because of how Html.fromHtml (removes spanned formattings) works it is not possible to mark string queries in searcher
                    // it has to be done on this end
                    resultSearchSamples.setText(markSearchQuery(styledResultText.toString(), result[2]));
                } else {
                    Spanned styledResultText = Html.fromHtml(result[4], Html.FROM_HTML_MODE_LEGACY);
                    resultSearchSamples.setText(markSearchQuery(styledResultText.toString(), result[2]));
                }

                // Detects click on search result
                // Returns intent to MainView to load selected node
                searchResultItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent selectedNode = new Intent();
                        selectedNode.putExtra("selectedNode", new String[]{result[0], result[1], result[5], result[6], result[7]});
                        setResult(RESULT_OK, selectedNode);
                        finish();
                    }
                });
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        SearchActivity.this.searchResultLinearLayout.addView(searchResultItem);
                    }
                });
            }
        }
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                SearchActivity.this.searchProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private SpannableStringBuilder markSearchQuery(String searchResult, String query) {
        // Changes background of the parts of the string (searchResult) that matches second string (searchQuery)
        // Used to mark search query string in in search result samples

        int index = 0; // index of start of the found substring
        int searchLength = query.length();

        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
        spannedSearchQuery.append(searchResult);
        while (index != -1) {
            index = searchResult.indexOf(query, index);
            if (index != -1) {
                int startIndex = index;
                int endIndex = index + searchLength;
                spannedSearchQuery.setSpan(new BackgroundColorSpan(getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += searchLength; // moves search to the end of the last found string
            }
        }
        return spannedSearchQuery;
    }
}