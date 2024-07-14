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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import lt.ffda.sourcherry.AppContainer;
import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ScApplication;
import lt.ffda.sourcherry.customUiElements.CustomTextEdit;
import lt.ffda.sourcherry.customUiElements.ScTableLayout;
import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;
import lt.ffda.sourcherry.spans.BackgroundColorSpanCustom;
import lt.ffda.sourcherry.spans.ClickableSpanLink;
import lt.ffda.sourcherry.spans.ClickableSpanNode;
import lt.ffda.sourcherry.spans.StyleSpanBold;
import lt.ffda.sourcherry.spans.StyleSpanItalic;
import lt.ffda.sourcherry.spans.TypefaceSpanCodebox;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import lt.ffda.sourcherry.utils.ColorPickerPresets;

public class NodeContentEditorFragment extends Fragment implements NodeContentEditorMainMenuActions, NodeContentEditorInsertMenuActions, NodeContentEditorTableMenuActions {
    private View.OnFocusChangeListener onCustomTextEditFocusChangeListener;
    private boolean changesSaved = false;
    private int color;
    private Handler handler;
    private MainViewModel mainViewModel;
    private LinearLayout nodeEditorFragmentLinearLayout;
    /**
     * Promts user to select an image they want to attach to the node. If user selects an image
     * inserts an image representing string at the possition of the cursor
     */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            EditText editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
            editText.getText().insert(editText.getSelectionStart(), DatabaseReader.createImageSpan(getContext(), uri));
        }
    });
    /**
     * Promts user to select a file they want to attach to the node. If user selects a file inserts
     * file representing string at the possition of the cursor
     */
    private final ActivityResultLauncher<String[]> attachFile = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        if (result != null) {
            DocumentFile file = DocumentFile.fromSingleUri(getContext(), result);
            EditText editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
            editText.getText().insert(editText.getSelectionStart(), DatabaseReader.createAttachFileSpan(getContext(), file.getName(), this.mainViewModel.getCurrentNode().getUniqueId(), result.toString()));
        }
    });
    private SharedPreferences sharedPreferences;
    private boolean textChanged = false;
    private TextWatcher textWatcher;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (NodeContentEditorFragment.this.textChanged) {
                String unsavedChangesDefaultPreference = NodeContentEditorFragment.this.sharedPreferences.getString("preferences_unsaved_changes", null);
                if (unsavedChangesDefaultPreference == null || unsavedChangesDefaultPreference.equals("ask")) {
                    NodeContentEditorFragment.this.createUnsavedChangesAlertDialog();
                } else if (unsavedChangesDefaultPreference.equals("save")) {
                    NodeContentEditorFragment.this.saveNodeContent();
                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    remove(); // Otherwise there will be onBackPressed infinite loop
                    ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
                } else {
                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    remove(); // Otherwise there will be onBackPressed infinite loop
                    ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
                }
            } else {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                remove(); // Otherwise there will be onBackPressed infinite loop
                ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
            }
        }
    };

    /**
     * Adds textChangedListeners for all EditText views
     * Does it on the main thread
     */
    private void addTextChangedListeners() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
                    View view = nodeEditorFragmentLinearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((EditText) view).addTextChangedListener(textWatcher);
                    } else if (view instanceof HorizontalScrollView){
                        ScTableLayout scTableLayout = (ScTableLayout) ((HorizontalScrollView) view).getChildAt(0);
                        for (int j = 0; j < scTableLayout.getChildCount(); j++) {
                            TableRow tableRow = (TableRow) scTableLayout.getChildAt(j);
                            for (int k = 0; k < tableRow.getChildCount(); k++) {
                                ((EditText) tableRow.getChildAt(k)).addTextChangedListener(textWatcher);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void attachFile() {
        View view = NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.getFocusedChild();
        if (view == null) {
            Toast.makeText(getContext(), R.string.toast_message_attach_file_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        if (view instanceof HorizontalScrollView) {
            Toast.makeText(getContext(), R.string.toast_message_attach_file_insert_into_table, Toast.LENGTH_SHORT).show();
            return;
        }
        NodeContentEditorFragment.this.attachFile.launch(new String[]{"*/*"});
    }

    public void changeBackgroundColor() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            BackgroundColorSpanCustom[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, BackgroundColorSpanCustom.class);
            for (BackgroundColorSpanCustom span : spans) {
                int startOfSpan = editText.getText().getSpanStart(span);
                int endOfSpan = editText.getText().getSpanEnd(span);
                editText.getText().removeSpan(span);
                int backgroundColor = span.getBackgroundColor();
                this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new BackgroundColorSpanCustom(backgroundColor), new BackgroundColorSpanCustom(backgroundColor));
            }
            BackgroundColorSpanCustom bcs = new BackgroundColorSpanCustom(this.color);
            editText.getText().setSpan(bcs, startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.textChanged = true;
        }
    }

    public void changeForegroundColor() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            ForegroundColorSpan[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                int startOfSpan = editText.getText().getSpanStart(span);
                int endOfSpan = editText.getText().getSpanEnd(span);
                editText.getText().removeSpan(span);
                int foregroundColor = span.getForegroundColor();
                this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new ForegroundColorSpan(foregroundColor), new ForegroundColorSpan(foregroundColor));
            }
            ForegroundColorSpan fcs = new ForegroundColorSpan(this.color);
            editText.getText().setSpan(fcs, startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.textChanged = true;
        }
    }

    /**
     * Checks selected text for codeboxes
     * @return true - if at least one codebox was found, false - otherwise
     */
    private boolean checkSelectionForCodebox() {
        boolean codeboxExists = false;
        EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
        Object[] spans = editText.getText().getSpans(editText.getSelectionStart(), editText.getSelectionEnd(), Object.class);
        for (Object span: spans) {
            if (span instanceof TypefaceSpanCodebox) {
                codeboxExists = true;
                break;
            }
        }
        return codeboxExists;
    }

    public void clearFormatting() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            Object[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, Object.class);
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be cleared using clear formatting
                // It only should be deleted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            for (Object span: spans) {
                if (span instanceof ForegroundColorSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    int foregroundColor = ((ForegroundColorSpan) span).getForegroundColor();
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new ForegroundColorSpan(foregroundColor), new ForegroundColorSpan(foregroundColor));
                    this.textChanged = true;
                } else if (span instanceof BackgroundColorSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    int backgroundColor = ((BackgroundColorSpan) span).getBackgroundColor();
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new BackgroundColorSpanCustom(backgroundColor), new BackgroundColorSpanCustom(backgroundColor));
                    this.textChanged = true;
                } else if (span instanceof StrikethroughSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StrikethroughSpan(), new StrikethroughSpan());
                    this.textChanged = true;
                } else if (span instanceof StyleSpanBold) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanBold(), new StyleSpanBold());
                    this.textChanged = true;
                } else if (span instanceof StyleSpanItalic) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanItalic(), new StyleSpanItalic());
                    this.textChanged = true;
                } else if (span instanceof RelativeSizeSpan) {
                    RelativeSizeSpan relativeSizeSpan = (RelativeSizeSpan) span;
                    float size = relativeSizeSpan.getSizeChange();
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new RelativeSizeSpan(size), new RelativeSizeSpan(size));
                    this.textChanged = true;
                } else if (span instanceof SubscriptSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new SubscriptSpan(), new SubscriptSpan());
                    this.textChanged = true;
                } else if (span instanceof SuperscriptSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new SuperscriptSpan(), new SuperscriptSpan());
                    this.textChanged = true;
                } else if (span instanceof TypefaceSpanFamily) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new TypefaceSpanFamily("monospace"), new TypefaceSpanFamily("monospace"));
                    this.textChanged = true;
                } else if (span instanceof UnderlineSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new UnderlineSpan(), new UnderlineSpan());
                    this.textChanged = true;
                } else if (span instanceof URLSpanWebs) {
                    URLSpanWebs urlSpanWebs = (URLSpanWebs) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new URLSpanWebs(urlSpanWebs.getURL()), new URLSpanWebs(urlSpanWebs.getURL()));
                    this.textChanged = true;
                } else if (span instanceof ClickableSpanNode) {
                    ClickableSpanNode clickableSpanNode = (ClickableSpanNode) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, this.createNodeLink(clickableSpanNode), this.createNodeLink(clickableSpanNode));
                    this.textChanged = true;
                } else if (span instanceof ClickableSpanLink) {
                    ClickableSpanLink clickableSpanLink = (ClickableSpanLink) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, this.createFileFolderLink(clickableSpanLink), this.createFileFolderLink(clickableSpanLink));
                    this.textChanged = true;
                } else if (span instanceof LeadingMarginSpan.Standard) {
                    editText.getText().removeSpan(span);
                    this.textChanged = true;
                }
            }
        }
    }

    /**
     * Edit text that can be inserted into fragments view
     * @param typeface font be set on the text in the EditText. Null will use default android font
     * @param textSize textSize to be set on the text in the EditText
     * @param content content of the EditText
     * @return EditText ready to be inserted in to view/layout
     */
    private EditText createEditText(Typeface typeface, int textSize, CharSequence content) {
        CustomTextEdit editText = (CustomTextEdit) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
        editText.setText(content, TextView.BufferType.EDITABLE);
        editText.addTextChangedListener(textWatcher);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        editText.setTypeface(typeface);
        editText.setOnFocusChangeListener(onCustomTextEditFocusChangeListener);
        return editText;
    }

    /**
     * Creates new ClickableSpanLink span object based on passed ClickableSpanLink object as an argument
     * @param clickableSpanLink ClickableSpanLink object to base new ClickableSpanLink span on
     * @return new ClickableSpanLink object
     */
    private ClickableSpanLink createFileFolderLink(ClickableSpanLink clickableSpanLink) {
        // Needed to save context to memory. Otherwise will cause a crash
        Context context = getContext();
        ClickableSpanLink newClickableSpanLink = new ClickableSpanLink() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                ((MainView) context).fileFolderLinkFilepath(new String(Base64.decode(clickableSpanLink.getBase64Link(), Base64.DEFAULT)));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                // Formatting of span text
                if (clickableSpanLink.getLinkType().equals("file")) {
                    ds.setColor(context.getColor(R.color.link_file));
                } else {
                    ds.setColor(context.getColor(R.color.link_folder));
                }
                ds.setUnderlineText(true);
            }
        };
        newClickableSpanLink.setLinkType(clickableSpanLink.getLinkType());
        newClickableSpanLink.setBase64Link(clickableSpanLink.getBase64Link());
        return newClickableSpanLink;
    }

    /**
     * Creates new ClickableSpanNode span object based on passed ClickableSpanNode object as an argument
     * @param clickableSpanNode ClickableSpanNode object to base new ClickableSpanNode span on
     * @return new ClickableSpanNode object
     */
    private ClickableSpanNode createNodeLink(ClickableSpanNode clickableSpanNode) {
        // Needed to save context to memory. Otherwise will cause a crash
        Context context = getContext();
        ClickableSpanNode newClickableSpanNode = new ClickableSpanNode() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) context).openAnchorLink(DatabaseReaderFactory.getReader().getSingleMenuItem(clickableSpanNode.getNodeUniqueID()));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                ds.setColor(context.getColor(R.color.link_anchor));
                ds.setUnderlineText(true);
            }
        };
        newClickableSpanNode.setLinkAnchorName(clickableSpanNode.getLinkAnchorName());
        return newClickableSpanNode;
    }

    /**
     * Creates focus change listiner for CustomEditText. It changes editor menu depending on where
     * the user puts cursor. When cursor in table it should have different editor menu items.
     * @return OnFocusChangeListener for CustomeEditText
     */
    private View.OnFocusChangeListener createOnCustomTextEditFocusChangeListener() {
        if (this.mainViewModel.getCurrentNode().isRichText()) {
            return new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        FragmentManager fragmentManager = getChildFragmentManager();
                        Fragment tableMenuFragment = fragmentManager.findFragmentByTag("tableMenuFragment");
                        CustomTextEdit customTextEdit = (CustomTextEdit) view;
                        if (customTextEdit.isTableCell() && tableMenuFragment == null) {
                            tableMenuFragment = new NodeContentEditorMenuTableFragment();
                            ((NodeContentEditorMenuTableFragment) tableMenuFragment).setNodeContentEditorTableMenuActions(NodeContentEditorFragment.this);
                            fragmentManager.beginTransaction()
                                    .add(R.id.edit_node_fragment_button_row_fragment, tableMenuFragment, "tableMenuFragment")
                                    .setReorderingAllowed(true)
                                    .addToBackStack(null)
                                    .commit();
                        } else if (!customTextEdit.isTableCell() && tableMenuFragment != null) {
                            if (!fragmentManager.isStateSaved()) {
                                fragmentManager.popBackStack();
                            }
                        }
                    }
                }
            };
        }
        return null;
    }

    /**
     * Creates EditText that is ready to be inserted into table as a cell. Only not header cell will
     * have maxWdith applied to them. So header cell will expand with the text typed into them while
     * normal cell will be only as wide as header cell of the column.
     * @param header true - cell will be used in the header, false - normal table cell
     * @param params parameters of the EditText (width, height) otherwise it will net be displayed
     * @param typeface font be set on the text in the cell. Null will use default android font
     * @param textSize textSize to be set on the text in the cell
     * @param colWidth width of the cell. Will not be appliead to header cell.
     * @param content content of the cell.
     * @return EditText ready to be inserted in the table as a cell
     */
    private EditText createTableCell(boolean header, ViewGroup.LayoutParams params, Typeface typeface, int textSize, int colWidth, CharSequence content) {
        CustomTextEdit cell = (CustomTextEdit) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
        cell.setTableCell(true);
        if (header) {
            cell.setBackground(getActivity().getDrawable(R.drawable.table_header_cell));
            cell.setTypeface(typeface, Typeface.BOLD);
        } else {
            cell.setBackground(getActivity().getDrawable(R.drawable.table_data_cell));
            cell.setMaxWidth(colWidth);
            cell.setTypeface(typeface);
        }
        cell.setPadding(10,10,10,10);
        cell.setLayoutParams(params);
        cell.setText(content);
        cell.setMinWidth(100);
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        cell.addTextChangedListener(textWatcher);
        cell.setOnFocusChangeListener(onCustomTextEditFocusChangeListener);
        return cell;
    }

    /**
     * Creates new empty table ready to be inserted in to fragments view
     * @param typeface font be set on the text in the table. Null will use default android font.
     * @param textSize textSize to be set on the text in the table.
     * @return HorizontalScrollView that holds TableView
     */
    private HorizontalScrollView createTableView(Typeface typeface, int textSize) {
        HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
        LinearLayout.LayoutParams tableScrollViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        tableScrollView.setLayoutParams(tableScrollViewParams);
        ScTableLayout table = new ScTableLayout(getActivity());
        table.setLightInterface((byte) 0);
        table.setColWidths("50,50");
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        ViewGroup.LayoutParams cellParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        TableRow tableHeaderRow = new TableRow(getActivity());
        tableHeaderRow.setLayoutParams(rowParams);
        tableHeaderRow.addView(createTableCell(true, cellParams, typeface, textSize, 100, null));
        tableHeaderRow.addView(createTableCell(true, cellParams, typeface, textSize, 100, null));
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(rowParams);
        tableRow.addView(createTableCell(false, cellParams, typeface, textSize, 100, null));
        tableRow.addView(createTableCell(false, cellParams, typeface, textSize, 100, null));
        table.addView(tableHeaderRow);
        table.addView(tableRow);
        tableScrollView.addView(table);
        return tableScrollView;
    }

    /**
     * Creates dialog to ask user if changes in the editor has to be saved
     * User can check the checkbox to save the choice to the database
     */
    private void createUnsavedChangesAlertDialog() {
        View checkboxView = View.inflate(getContext(), R.layout.alert_dialog_unsaved_changes, null);
        CheckBox checkBox = checkboxView.findViewById(R.id.alert_dialog_unsaved_changes_remember_choice_checkBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(checkboxView);
        builder.setTitle(R.string.alert_dialog_unsaved_changes_title);
        builder.setMessage(R.string.alert_dialog_unsaved_changes_message);
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                NodeContentEditorFragment.this.textChanged = false;
                if (checkBox.isChecked()) {
                    NodeContentEditorFragment.this.saveUnsavedChangesDialogChoice("exit");
                }
                NodeContentEditorFragment.this.getActivity().onBackPressed();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    NodeContentEditorFragment.this.saveUnsavedChangesDialogChoice("save");
                }
                NodeContentEditorFragment.this.saveNodeContent();
                NodeContentEditorFragment.this.getActivity().onBackPressed();
            }
        });
        builder.show();
    }

    /**
     * Get typeface that user set to be used in preferences
     * @return Typeface that can be used to change Views font
     */
    private Typeface getTypeface() {
        Typeface typeface = null;
        switch (this.sharedPreferences.getString("preference_font_type", "Default")) {
            case "Comfortaa":
                typeface = ResourcesCompat.getFont(getContext(), R.font.comfortaa_regular);
                break;
            case "Merriweather":
                typeface = ResourcesCompat.getFont(getContext(), R.font.merriweather_regular);
                break;
            case "Caladea":
                typeface = ResourcesCompat.getFont(getContext(), R.font.caladea_regular);
        }
        return typeface;
    }

    @Override
    public void insertColumn() {
        HorizontalScrollView tableScrollView = (HorizontalScrollView) this.nodeEditorFragmentLinearLayout.getFocusedChild();
        ScTableLayout table = (ScTableLayout) tableScrollView.getFocusedChild();
        TableRow focusedTableRow = (TableRow) table.getFocusedChild();
        int selectedColumnIndex = -1;
        for (int i = 0; i < focusedTableRow.getChildCount(); i++) {
            View cell = focusedTableRow.getChildAt(i);
            if (cell.hasFocus()) {
                selectedColumnIndex = i + 1;
                break;
            }
        }
        ViewGroup.LayoutParams cellParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Typeface typeface = getTypeface();
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow tableRow = (TableRow) table.getChildAt(i);
            if (i == 0) {
                tableRow.addView(createTableCell(true, cellParams, typeface, textSize, 100, null), selectedColumnIndex);
            } else {
                tableRow.addView(createTableCell(false, cellParams, typeface, textSize, 100, null), selectedColumnIndex);
            }
        }
        textChanged = true;
    }

    @Override
    public void insertImage() {
        View view = this.nodeEditorFragmentLinearLayout.getFocusedChild();
        if (view == null) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        if (view instanceof HorizontalScrollView) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_insert_into_table, Toast.LENGTH_SHORT).show();
            return;
        }
        this.pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    @Override
    public void insertTable() {
        View view = this.nodeEditorFragmentLinearLayout.getFocusedChild();
        if (view == null) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        // To insert a table EditText has to be split in two at the possition on the cursor
        int indexOfChild = this.nodeEditorFragmentLinearLayout.indexOfChild(view);
        EditText editText = ((EditText) view);
        int startOfSelection = editText.getSelectionStart();
        Typeface typeface = getTypeface();
        int textSize = this.sharedPreferences.getInt("preferences_text_size", 15);
        EditText firstPart = createEditText(typeface, textSize, editText.getText().subSequence(0, startOfSelection));
        EditText secondPart = createEditText(typeface, textSize, editText.getText().subSequence(startOfSelection, editText.getText().length()));
        this.nodeEditorFragmentLinearLayout.removeViewAt(indexOfChild);
        this.nodeEditorFragmentLinearLayout.addView(firstPart, indexOfChild);
        this.nodeEditorFragmentLinearLayout.addView(createTableView(typeface, textSize), ++indexOfChild);
        this.nodeEditorFragmentLinearLayout.addView(secondPart, ++indexOfChild);
        this.textChanged = true;
    }

    /**
     * Loads node content to UI
     */
    public void loadContent() {
        // Clears layout just in case. Most of the time it is needed
        if (this.nodeEditorFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.removeAllViews();
                }
            });
        }
        int textSize = this.sharedPreferences.getInt("preferences_text_size", 15);
        Typeface typeface = getTypeface();
        for (ScNodeContent part : this.mainViewModel.getNodeContent().getValue()) {
            if (part.getContentType() == 0) {
                // This adds not only text, but images, codeboxes
                ScNodeContentText scNodeContentText = (ScNodeContentText) part;
                SpannableStringBuilder nodeContentSSB = scNodeContentText.getContent();
                EditText editText = createEditText(typeface, textSize, nodeContentSSB);
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(editText);
                    }
                });
            } else {
                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
                ScTableLayout table = new ScTableLayout(getActivity());
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
                table.setLightInterface(scNodeContentTable.getLightInterface());
                table.setColWidths(scNodeContentTable.getColWidths());
                // Multiplying by arbitrary number to make table cells look better.
                // For some reason table that looks good in PC version looks worse on android
                int colMin = (int) (scNodeContentTable.getColMin() * 1.3);
                // Wraps content in cell correctly
                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);

                //// Creates and formats header for the table
                CharSequence[] tableHeaderCells = scNodeContentTable.getContent().get(0);
                TableRow tableHeaderRow = new TableRow(getActivity());
                for (CharSequence cell: tableHeaderCells) {
                    EditText headerTextView = createTableCell(true, params, typeface, textSize, colMin, cell);
                    headerTextView.setMinWidth(colMin);
                    tableHeaderRow.addView(headerTextView);
                }
                table.addView(tableHeaderRow);
                ////

                //// Creates and formats data for the table
                for (int row = 1; row < scNodeContentTable.getContent().size(); row++) {
                    TableRow tableRow = new TableRow(getActivity());
                    CharSequence[] tableRowCells = scNodeContentTable.getContent().get(row);
                    for (CharSequence cell: tableRowCells) {
                        EditText cellTextView = createTableCell(false, params, typeface, textSize, colMin, cell);
                        cellTextView.setMinWidth(colMin);
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
                        NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(tableScrollView);
                    }
                });
            }
        }
        // If last NodeContent elements is a table it won't be possible to add text after it
        // Adding an extra EditText to allow user to continue typing after last table element
        if (this.mainViewModel.getNodeContent().getValue().get(this.mainViewModel.getNodeContent().getValue().size() - 1).getContentType() == 1) {
            EditText editText = createEditText(typeface, textSize, "");
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(editText);
                }
            });
        }
        // Shows keyboard if opened node for editing has less than 2 characters in the EditText
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                if (mainViewModel.getNodeContent().getValue().size() == 1) {
                    EditText editText = (EditText) NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.getChildAt(0);
                    if (editText.getText().length() <= 1) {
                        editText.requestFocus();
                        WindowCompat.getInsetsController(getActivity().getWindow(), editText).show(WindowInsetsCompat.Type.ime());
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_node_editor, container, false);
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.onCustomTextEditFocusChangeListener = createOnCustomTextEditFocusChangeListener();
        this.nodeEditorFragmentLinearLayout = view.findViewById(R.id.node_edit_fragment_linearlayout);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        this.handler = appContainer.handler;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.color = this.sharedPreferences.getInt("colorPickerColor", ColorPickerPresets.BLACK.getColor());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Top and bottom paddings are always the same: 14px (5dp)
        this.nodeEditorFragmentLinearLayout.setPadding(this.sharedPreferences.getInt("paddingStart", 14), 14, this.sharedPreferences.getInt("paddingEnd", 14), 14);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ScrollView scrollView = getView().findViewById(R.id.edit_node_fragment_scrollview);
        if (scrollView != null) {
            outState.putInt("scrollY", scrollView.getScrollY());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                NodeContentEditorFragment.this.textChanged = true;
                NodeContentEditorFragment.this.removeTextChangedListeners();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };

        // Loading new menu for the fragment
        // that only have a save button in it
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.options_menu_node_editor_fragment, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    getActivity().onBackPressed();
                    return true;
                } else if (menuItem.getItemId() == R.id.toolbar_button_save_node) {
                    NodeContentEditorFragment.this.saveNodeContent();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        this.requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
        ScrollView scrollView = view.findViewById(R.id.edit_node_fragment_scrollview);
        if (savedInstanceState == null) {
            // Tries to scroll screen to the same location where it was when user chose to open editor
                this.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.setScrollY(getArguments().getInt("scrollY"));
                    }
                }, 150);
        } else {
            // Tries to scroll screen to the same location where it was when user rotated device
            if (savedInstanceState.getInt("scrollY") != 0) {
                this.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        double aspectRatio = (double) scrollView.getHeight() / scrollView.getWidth();
                        // Dividing or multiplying aspectRatio by random numbers to get more precise scroll position
                        if (aspectRatio < 1) {
                            aspectRatio /= 0.52;
                        } else {
                            aspectRatio *= 0.87;
                        }
                        scrollView.setScrollY((int) (savedInstanceState.getInt("scrollY") * aspectRatio));
                    }
                }, 150);
            }
            ((MainView) getContext()).disableDrawerMenu();
        }
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment tableMenuFragment = fragmentManager.findFragmentByTag("tableMenuFragment");
        if (tableMenuFragment != null) {
            fragmentManager.popBackStack();
        }
        if (this.mainViewModel.getCurrentNode().isRichText()) {
            NodeContentEditorMenuMainFragment fragment = new NodeContentEditorMenuMainFragment();
            fragment.setNodeContentEditorMenuActions(this);
            fragmentManager.beginTransaction()
                    .add(R.id.edit_node_fragment_button_row_fragment, fragment, null)
                    .commit();
        } else {
            FrameLayout buttonRowFragmentContainer = getView().findViewById(R.id.edit_node_fragment_button_row_fragment);
            buttonRowFragmentContainer.setVisibility(View.GONE);
        }
        this.loadContent();
    }

    /**
     * Applies provided span to the text to the portion of the span that is outside of the selected text
     * It needs to have two spans because any span object can be used only once
     * @param startOfSelection index of the text at the start of selection
     * @param endOfSelection index of the text at the end of selection
     * @param startOfSpan index of the start of the span
     * @param endOfSpan index of the end of the span
     * @param span1 span to reapply
     * @param span2 span to reapply
     */
    private void reapplySpanOutsideSelection(int startOfSelection, int endOfSelection, int startOfSpan, int endOfSpan, Object span1, Object span2) {
        EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
        if (startOfSelection >= startOfSpan && endOfSelection <= endOfSpan) {
            // If selection is inside the span
            editText.getText().setSpan(span1, startOfSpan, startOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editText.getText().setSpan(span2, endOfSelection, endOfSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (startOfSelection < startOfSpan && endOfSelection <= endOfSpan) {
            // If start of selection is outside the span, but the end is inside
            editText.getText().setSpan(span1, endOfSelection, endOfSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (startOfSelection >= startOfSpan) {
            // If start if selection is inside of the span, but the end is outside
            editText.getText().setSpan(span1, startOfSpan, startOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Removes all textChangedListeners from EditText views
     * Does it on the main thread
     */
    private void removeTextChangedListeners() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
                    View view = nodeEditorFragmentLinearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((EditText) view).removeTextChangedListener(textWatcher);
                    } else if (view instanceof HorizontalScrollView){
                        ScTableLayout scTableLayout = (ScTableLayout) ((HorizontalScrollView) view).getChildAt(0);
                        for (int j = 0; j < scTableLayout.getChildCount(); j++) {
                            TableRow tableRow = (TableRow) scTableLayout.getChildAt(j);
                            for (int k = 0; k < tableRow.getChildCount(); k++) {
                                ((EditText) tableRow.getChildAt(k)).removeTextChangedListener(textWatcher);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Saves current content of the that is being displayed to
     * the database
     */
    private void saveNodeContent() {
        this.changesSaved = true;
        this.textChanged = false;
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
            View view = this.nodeEditorFragmentLinearLayout.getChildAt(i);
            // if it a table
            if (view instanceof HorizontalScrollView) {
                ScTableLayout tableLayout = (ScTableLayout) ((HorizontalScrollView) view).getChildAt(0);
                String justification;
                // Getting justification of the table
                LinearLayout.LayoutParams tableScrollView = (LinearLayout.LayoutParams) view.getLayoutParams();
                switch (tableScrollView.gravity) {
                    case Gravity.RIGHT:
                        justification = "right";
                        break;
                    case Gravity.CENTER:
                        justification = "center";
                        break;
                    case Gravity.FILL:
                        justification = "fill";
                        break;
                    default:
                        justification = "left";
                        break;
                }
                ArrayList<CharSequence[]> tableContent = new ArrayList<>();
                // Getting table header
                int colMin = 0;
                TableRow tableHeader = (TableRow) tableLayout.getChildAt(0);
                CharSequence[] headerCells = new CharSequence[tableHeader.getChildCount()];
                for (int cell = 0; cell < tableHeader.getChildCount(); cell++) {
                    EditText currentCell = (EditText) tableHeader.getChildAt(cell);
                    currentCell.clearComposingText();
                    headerCells[cell] = currentCell.getText();
                    if (cell == 0) {
                        // Getting colMin. Dividing by the same arbitrary
                        // value it was multiplied when the table was created
                        colMin = (int) (currentCell.getMinWidth() / 1.3);
                    }
                }
                tableContent.add(headerCells);
                // Getting table content
                for (int row = 1; row < tableLayout.getChildCount(); row++) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                    CharSequence[] rowCells = new CharSequence[tableRow.getChildCount()];
                    for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                        EditText currentCell = (EditText) tableRow.getChildAt(cell);
                        currentCell.clearComposingText();
                        rowCells[cell] = currentCell.getText();
                    }
                    tableContent.add(rowCells);
                }
                // colMax and colMin is always the same?
                nodeContent.add(new ScNodeContentTable((byte) 1, tableContent, colMin, colMin, tableLayout.getLightInterface(), justification, tableLayout.getColWidths()));
            } else {
                EditText editText = (EditText) view;
                editText.clearComposingText();
                nodeContent.add(new ScNodeContentText((byte) 0, (SpannableStringBuilder) editText.getText()));
            }
        }
        // Setting new node content
        this.mainViewModel.getNodeContent().setValue(nodeContent);
        DatabaseReaderFactory.getReader().saveNodeContent(getArguments().getString("nodeUniqueID"));
        this.addTextChangedListeners();
    }

    /**
     * Saves user choice to the preference key preferences_unsaved_changes
     * @param choice user choice (ask, exit, save)
     */
    private void saveUnsavedChangesDialogChoice(String choice) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString("preferences_unsaved_changes", choice);
        editor.apply();
    }

    @Override
    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void showInsertRow() {
        NodeContentEditorMenuInsertFragment fragment = new NodeContentEditorMenuInsertFragment();
        fragment.setNodeContentEditorMenuActions(this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.edit_node_fragment_button_row_fragment, fragment, null)
                .addToBackStack("inserMenu")
                .commit();
    }

    @Override
    public void toggleFontBold() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            StyleSpanBold[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, StyleSpanBold.class);
            if (spans.length > 0) {
                for (StyleSpanBold span: spans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanBold(), new StyleSpanBold());
                }
            } else {
                editText.getText().setSpan(new StyleSpanBold(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            this.textChanged = true;
        }
    }

    @Override
    public void toggleFontItalic() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            StyleSpanItalic[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, StyleSpanItalic.class);
            if (spans.length > 0) {
                for (StyleSpan span: spans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanItalic(), new StyleSpanItalic());
                }
            } else {
                editText.getText().setSpan(new StyleSpanItalic(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            this.textChanged = true;
        }
    }

    @Override
    public void toggleFontStrikethrough() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            StrikethroughSpan[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, StrikethroughSpan.class);
            if (spans.length > 0) {
                for (StrikethroughSpan span: spans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StrikethroughSpan(), new StrikethroughSpan());
                }
            } else {
                editText.getText().setSpan(new StrikethroughSpan(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            this.textChanged = true;
        }
    }

    @Override
    public void toggleFontUnderline() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (this.checkSelectionForCodebox()) {
                // As in CherryTree codebox can't be formatted
                Toast.makeText(getContext(), R.string.toast_message_codebox_cant_be_formatted, Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
            int startOfSelection = editText.getSelectionStart();
            int endOfSelection = editText.getSelectionEnd();
            if (endOfSelection - startOfSelection == 0) {
                // No text selected
                return;
            }
            UnderlineSpan[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, UnderlineSpan.class);
            if (spans.length > 0) {
                for (UnderlineSpan span: spans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    this.reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new UnderlineSpan(), new UnderlineSpan());
                }
            } else {
                editText.getText().setSpan(new UnderlineSpan(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            this.textChanged = true;
        }
    }
}
