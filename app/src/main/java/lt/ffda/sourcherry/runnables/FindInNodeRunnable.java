/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.runnables;

import android.text.SpannableStringBuilder;

import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;

public class FindInNodeRunnable implements Runnable {
    private final MainViewModel mainViewModel;
    private final String query;
    private final FindInNodeRunnableCallback callback;

    /**
     * Runnable that searches through content of currently opened node. Saves result in MainViewModel.
     * @param mainViewModel mainViewModel to get the node content from and store the results of the search
     * @param query text to search for
     * @param callback implemented interface of FindInNodeRunnableCallback
     */
    public FindInNodeRunnable(MainViewModel mainViewModel, String query, FindInNodeRunnableCallback callback) {
        this.mainViewModel = mainViewModel;
        this.query = query;
        this.callback = callback;
    }

    @Override
    public void run() {
        callback.searchStarted();
        // To keep track of which TextView representing SpannableStringBuilder is being search for
        // currently. It DOES NOT count HorizontalScrollView.
        int counter = 0;
        for (ScNodeContent nodeContent: mainViewModel.getNodeContent().getValue()) {
            if (nodeContent.getContentType() == 0) {
                // if textview
                ScNodeContentText scNodeContentText = (ScNodeContentText) nodeContent;
                int searchLength = query.length();
                SpannableStringBuilder spannedSearchQuery = scNodeContentText.getContent();
                int index = 0;
                while ((index != -1)) {
                    index = spannedSearchQuery.toString().toLowerCase().indexOf(query.toLowerCase(), index); // searches in case insensitive mode
                    if (index != -1) {
                        // If there was a match
                        int startIndex = index;
                        int endIndex = index + searchLength; // End of the substring that has to be marked
                        mainViewModel.addFindInNodeResult(new int[] {counter, startIndex, endIndex});
                        index += searchLength; // moves search to the end of the last found string
                    }
                }
                counter++;
            } else if (nodeContent.getContentType() == 1) {
                // if it is a table
                // Has to go to the cell level to reach text
                // to be able to mark it at appropriate place
                int searchLength = query.length();
                ScNodeContentTable scNodeContentTable = (ScNodeContentTable) nodeContent;
                for (CharSequence[] row: scNodeContentTable.getContent()) {
                    for (CharSequence cell: row) {
                        SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder(cell);
                        int index = 0;
                        while ((index != -1)) {
                            index = spannedSearchQuery.toString().toLowerCase().indexOf(query.toLowerCase(), index); // searches in case insensitive mode
                            if (index != -1) {
                                int startIndex = index;
                                int endIndex = index + searchLength; // End of the substring that has to be marked
                                mainViewModel.addFindInNodeResult(new int[]{counter, startIndex, endIndex});
                                index += searchLength; // moves search to the end of the last found string
                            }
                        }
                        counter++;
                    }
                }
            }
        }
        callback.searchFinished();
    }
}
