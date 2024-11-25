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

import static lt.ffda.sourcherry.utils.RegexPatterns.allCheckbox;
import static lt.ffda.sourcherry.utils.RegexPatterns.allListStarts;
import static lt.ffda.sourcherry.utils.RegexPatterns.checkedCheckbox;
import static lt.ffda.sourcherry.utils.RegexPatterns.lastNewline;
import static lt.ffda.sourcherry.utils.RegexPatterns.orderdList;

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
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.regex.Matcher;

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
import lt.ffda.sourcherry.spans.ClickableSpanFile;
import lt.ffda.sourcherry.spans.ClickableSpanLink;
import lt.ffda.sourcherry.spans.ClickableSpanNode;
import lt.ffda.sourcherry.spans.ImageSpanFile;
import lt.ffda.sourcherry.spans.MonospaceBackgroundColorSpan;
import lt.ffda.sourcherry.spans.StyleSpanBold;
import lt.ffda.sourcherry.spans.StyleSpanItalic;
import lt.ffda.sourcherry.spans.TypefaceSpanCodebox;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import lt.ffda.sourcherry.utils.CheckBoxSwitch;
import lt.ffda.sourcherry.utils.ColorPickerPresets;

public class NodeContentEditorFragment extends Fragment implements NodeContentEditorMainMenuActions,
        NodeContentEditorInsertMenuActions, NodeContentEditorTableMenuActions,
        NodeContentEditorMenuBackAction, NodeContentEditorListsMenuActions {
    private View.OnFocusChangeListener onCustomTextEditFocusChangeListener;
    private boolean changesSaved = false;
    private int color;
    private Handler handler;
    private MainViewModel mainViewModel;
    private LinearLayout nodeEditorFragmentLinearLayout;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerPickImage();
    private final ActivityResultLauncher<String[]> attachFile = registerAttachFile();
    private SharedPreferences sharedPreferences;
    private boolean unsavedChanges = false;
    private TextWatcher textWatcher;
    private final OnBackPressedCallback onBackPressedCallback = createOnBackPressedCallback();
    private View.OnClickListener clickListener;

    /**
     * Adds textChangedListeners for all EditText views
     * Does it on the main thread
     */
    private void addTextChangedListeners() {
        handler.post(new Runnable() {
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
        if (!isCursorPlaced()) {
            Toast.makeText(getContext(), R.string.toast_message_attach_file_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isCursorInTable()) {
            Toast.makeText(getContext(), R.string.toast_message_attach_file_insert_into_table, Toast.LENGTH_SHORT).show();
            return;
        }
        attachFile.launch(new String[]{"*/*"});
    }

    @Override
    public void back() {
        getChildFragmentManager().popBackStack();
    }

    public void changeBackgroundColor() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new BackgroundColorSpanCustom(backgroundColor), new BackgroundColorSpanCustom(backgroundColor));
            }
            BackgroundColorSpanCustom bcs = new BackgroundColorSpanCustom(color);
            editText.getText().setSpan(bcs, startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            unsavedChanges = true;
        }
    }

    public void changeForegroundColor() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new ForegroundColorSpan(foregroundColor), new ForegroundColorSpan(foregroundColor));
            }
            ForegroundColorSpan fcs = new ForegroundColorSpan(color);
            editText.getText().setSpan(fcs, startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            unsavedChanges = true;
        }
    }

    /**
     * Checks if clicked char in the EditText is a checkbox. If so - toggles it's state in this
     * order: Empty -> Checked -> Crossed
     * @param editText clicked EditText
     */
    private void checkBoxToggle(EditText editText) {
        int selection = editText.getSelectionStart();
        int length = editText.getText().length();
        if (selection >= length) {
            selection = length - 1;
        }
        int clickedChar = editText.getText().charAt(selection);
        if (clickedChar == CheckBoxSwitch.EMPTY.getCode()) {
            editText.getText().replace(editText.getSelectionStart(), editText.getSelectionEnd() + 1, CheckBoxSwitch.CHECKED.getString());
        } else if (clickedChar == CheckBoxSwitch.CHECKED.getCode()) {
            editText.getText().replace(editText.getSelectionStart(), editText.getSelectionEnd() + 1, CheckBoxSwitch.CROSSED.getString());
        } else if (clickedChar == CheckBoxSwitch.CROSSED.getCode()) {
            editText.getText().replace(editText.getSelectionStart(), editText.getSelectionEnd() + 1, CheckBoxSwitch.EMPTY.getString());
        }
    }

    /**
     * Checks selected text for codeboxes
     * @return true - if at least one codebox was found, false - otherwise
     */
    private boolean checkSelectionForCodebox() {
        EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
        Object[] spans = editText.getText().getSpans(editText.getSelectionStart(), editText.getSelectionEnd(), Object.class);
        for (Object span: spans) {
            if (span instanceof TypefaceSpanCodebox) {
                return true;
            }
        }
        return false;
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
            if (checkSelectionForCodebox()) {
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
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new ForegroundColorSpan(foregroundColor), new ForegroundColorSpan(foregroundColor));
                    unsavedChanges = true;
                } else if (span instanceof BackgroundColorSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    int backgroundColor = ((BackgroundColorSpan) span).getBackgroundColor();
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new BackgroundColorSpanCustom(backgroundColor), new BackgroundColorSpanCustom(backgroundColor));
                    unsavedChanges = true;
                } else if (span instanceof StrikethroughSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StrikethroughSpan(), new StrikethroughSpan());
                    unsavedChanges = true;
                } else if (span instanceof StyleSpanBold) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanBold(), new StyleSpanBold());
                    unsavedChanges = true;
                } else if (span instanceof StyleSpanItalic) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanItalic(), new StyleSpanItalic());
                    unsavedChanges = true;
                } else if (span instanceof RelativeSizeSpan) {
                    RelativeSizeSpan relativeSizeSpan = (RelativeSizeSpan) span;
                    float size = relativeSizeSpan.getSizeChange();
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new RelativeSizeSpan(size), new RelativeSizeSpan(size));
                    unsavedChanges = true;
                } else if (span instanceof SubscriptSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new SubscriptSpan(), new SubscriptSpan());
                    unsavedChanges = true;
                } else if (span instanceof SuperscriptSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new SuperscriptSpan(), new SuperscriptSpan());
                    unsavedChanges = true;
                } else if (span instanceof TypefaceSpanFamily) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new TypefaceSpanFamily("monospace"), new TypefaceSpanFamily("monospace"));
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new MonospaceBackgroundColorSpan(getContext().getColor(R.color.monospace_background)), new MonospaceBackgroundColorSpan(getContext().getColor(R.color.monospace_background)));
                    unsavedChanges = true;
                } else if (span instanceof UnderlineSpan) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new UnderlineSpan(), new UnderlineSpan());
                    unsavedChanges = true;
                } else if (span instanceof URLSpanWebs) {
                    URLSpanWebs urlSpanWebs = (URLSpanWebs) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new URLSpanWebs(urlSpanWebs.getURL()), new URLSpanWebs(urlSpanWebs.getURL()));
                    unsavedChanges = true;
                } else if (span instanceof ClickableSpanNode) {
                    ClickableSpanNode clickableSpanNode = (ClickableSpanNode) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, createNodeLink(clickableSpanNode), createNodeLink(clickableSpanNode));
                    unsavedChanges = true;
                } else if (span instanceof ClickableSpanLink) {
                    ClickableSpanLink clickableSpanLink = (ClickableSpanLink) span;
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, createFileFolderLink(clickableSpanLink), createFileFolderLink(clickableSpanLink));
                    unsavedChanges = true;
                } else if (span instanceof LeadingMarginSpan.Standard) {
                    editText.getText().removeSpan(span);
                    unsavedChanges = true;
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
        CustomTextEdit editText = (CustomTextEdit) getLayoutInflater().inflate(R.layout.custom_edittext, nodeEditorFragmentLinearLayout, false);
        editText.setText(content, TextView.BufferType.EDITABLE);
        editText.addTextChangedListener(textWatcher);
        editText.setOnClickListener(clickListener);
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
     * Creates callback to manage back button clicks in this fragment
     * @return OnBackPressedCallback for the fragment
     */
    private OnBackPressedCallback createOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (unsavedChanges) {
                    String unsavedChangesDefaultPreference = sharedPreferences.getString("preferences_unsaved_changes", null);
                    if (unsavedChangesDefaultPreference == null || unsavedChangesDefaultPreference.equals("ask")) {
                        createUnsavedChangesAlertDialog();
                    } else if (unsavedChangesDefaultPreference.equals("save")) {
                        saveNodeContent();
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
    }

    /**
     * Creates focus change listiner for CustomEditText. It changes editor menu depending on where
     * the user puts cursor. When cursor in table it should have different editor menu items.
     * @return OnFocusChangeListener for CustomEditText
     */
    private View.OnFocusChangeListener createOnCustomTextEditFocusChangeListener() {
        if (mainViewModel.getCurrentNode().isRichText()) {
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
        CustomTextEdit cell = (CustomTextEdit) getLayoutInflater().inflate(R.layout.custom_edittext, nodeEditorFragmentLinearLayout, false);
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
     * Creates row of the table with provided cell count and properties
     * @param header if row should be created with cell with header properties
     * @param cols count of the cells in the row
     * @param rowParams view parametres of the row
     * @param cellParams view paramteres of the cell
     * @param typeface typeface font be set on the text in the table. Null will use default android font.
     * @param textSize textSize textSize to be set on the text in the table.
     * @param colWidth default width of the cell. The cell will not expand more then set width. Does not apply to header cell.
     * @return view with the row of the table ready to be added to one
     */
    private TableRow createTableRow(boolean header, int cols, TableRow.LayoutParams rowParams, ViewGroup.LayoutParams cellParams, Typeface typeface, int textSize, int colWidth) {
        TableRow tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(rowParams);
        for (int i = 0; i < cols; i++) {
            tableRow.addView(createTableCell(header, cellParams, typeface, textSize, colWidth, null));
        }
        return tableRow;
    }

    /**
     * Creates new empty table ready to be inserted in to fragments view
     * @param rows count of rows in the table
     * @param cols count of columns in the table
     * @param defaultWidth default width of the cell. The cell will not expand more then set width. Does not apply to header cell.
     * @param lightInterface set tables interface to be lightweight. 0 - normal interface, 1 - lightweight. Has no effect in mobile version
     * @param typeface typeface font be set on the text in the table. Null will use default android font.
     * @param textSize textSize textSize to be set on the text in the table.
     * @return HorizontalScrollView that holds TableView
     */
    private HorizontalScrollView createTableView(int rows, int cols, int defaultWidth, byte lightInterface, Typeface typeface, int textSize) {
        HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
        LinearLayout.LayoutParams tableScrollViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        tableScrollView.setLayoutParams(tableScrollViewParams);
        ScTableLayout table = new ScTableLayout(getActivity());
        table.setLightInterface(lightInterface);
        table.setColWidths("50,50");
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        ViewGroup.LayoutParams cellParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        table.addView(createTableRow(true, cols, rowParams, cellParams, typeface, textSize, defaultWidth));
        for (int i = 0; i < rows - 1; i++) {
            table.addView(createTableRow(false, cols, rowParams, cellParams, typeface, textSize, defaultWidth));
        }
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
        builder.setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                unsavedChanges = false;
                if (checkBox.isChecked()) {
                    saveUnsavedChangesDialogChoice("exit");
                }
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        builder.setPositiveButton(R.string.button_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    saveUnsavedChangesDialogChoice("save");
                }
                saveNodeContent();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        builder.show();
    }

    @Override
    public void deleteColumn() {
        HorizontalScrollView tableScrollView = (HorizontalScrollView) nodeEditorFragmentLinearLayout.getFocusedChild();
        ScTableLayout table = (ScTableLayout) tableScrollView.getFocusedChild();
        TableRow focusedTableRow = (TableRow) table.getFocusedChild();
        int focusedColumnIndex = -1;
        for (int i = 0; i < focusedTableRow.getChildCount(); i++) {
            View cell = focusedTableRow.getChildAt(i);
            if (cell.hasFocus()) {
                focusedColumnIndex = i;
                break;
            }
        }
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow tableRow = (TableRow) table.getChildAt(i);
            tableRow.removeViewAt(focusedColumnIndex);
        }
        unsavedChanges = true;
    }

    @Override
    public void deleteRow() {
        HorizontalScrollView tableScrollView = (HorizontalScrollView) nodeEditorFragmentLinearLayout.getFocusedChild();
        ScTableLayout table = (ScTableLayout) tableScrollView.getFocusedChild();
        int focusedRowIndex = -1;
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            if (row.hasFocus()) {
                focusedRowIndex = i;
                break;
            }
        }
        table.removeViewAt(focusedRowIndex);
        unsavedChanges = true;
    }

    @Override
    public void deleteTable() {
        View focusedTable = nodeEditorFragmentLinearLayout.getFocusedChild();
        int indexOfTable = nodeEditorFragmentLinearLayout.indexOfChild(focusedTable);
        if (indexOfTable > 0) {
            // If table surrounded by by normal content they have to be merged again
            if (nodeEditorFragmentLinearLayout.getChildAt(indexOfTable - 1) instanceof EditText && nodeEditorFragmentLinearLayout.getChildAt(indexOfTable + 1) instanceof EditText) {
                ((EditText) nodeEditorFragmentLinearLayout.getChildAt(indexOfTable - 1)).getText().append(((EditText) nodeEditorFragmentLinearLayout.getChildAt(indexOfTable + 1)).getText());
                nodeEditorFragmentLinearLayout.removeViewAt(indexOfTable);
                nodeEditorFragmentLinearLayout.removeViewAt(indexOfTable);
            } else {
                nodeEditorFragmentLinearLayout.removeViewAt(indexOfTable);
            }
        } else {
            nodeEditorFragmentLinearLayout.removeViewAt(indexOfTable);
        }
        unsavedChanges = true;
    }

    /**
     * Return index of last newLine char in the given editText. Looks from the start of the editText
     * to the provided end index. Does no do any checks. End index should be valid.
     * @param editText edit text to search for new line chars
     * @param end index up to which search for new line
     * @return index of found last new line or -1 if not found
     */
    private int getLastIndexOfNewLine(EditText editText, int end) {
        Matcher lastLine = lastNewline.matcher(editText.getText());
        lastLine.region(0, end);
        int indexOfLastNewline = -1;
        while (lastLine.find()) {
            indexOfLastNewline = lastLine.end();
        }
        return indexOfLastNewline;
    }

    /**
     * Returns start and end of the paragraph in which a cursor is currently placed
     * @param editText currectly focused editText
     * @return start and end index of paragraphs in array [start,end]
     */
    private int[] getParagraphStartEnd(EditText editText) {
        Matcher paragraph = lastNewline.matcher(editText.getText());
        int cursorLocation = editText.getSelectionStart();
        paragraph.region(0, cursorLocation);
        int start = 0; // Defaults to the start of the editText
        while (paragraph.find()) {
            start = paragraph.end();
        }
        int end = editText.getText().length(); // defaults to the end of the edtiText
        paragraph.region(cursorLocation, end);
        if (paragraph.find()) {
            end = paragraph.start();
        }
        return new int[] {start, end};
    }

    /**
     * Get typeface that user set to be used in preferences
     * @return Typeface that can be used to change Views font
     */
    private Typeface getTypeface() {
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
        return typeface;
    }

    @Override
    public void insertColumn() {
        HorizontalScrollView tableScrollView = (HorizontalScrollView) nodeEditorFragmentLinearLayout.getFocusedChild();
        ScTableLayout table = (ScTableLayout) tableScrollView.getFocusedChild();
        TableRow focusedTableRow = (TableRow) table.getFocusedChild();
        int newColumnIndex = -1;
        for (int i = 0; i < focusedTableRow.getChildCount(); i++) {
            View cell = focusedTableRow.getChildAt(i);
            if (cell.hasFocus()) {
                newColumnIndex = i + 1;
                break;
            }
        }
        ViewGroup.LayoutParams cellParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Typeface typeface = getTypeface();
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow tableRow = (TableRow) table.getChildAt(i);
            if (i == 0) {
                tableRow.addView(createTableCell(true, cellParams, typeface, textSize, 100, null), newColumnIndex);
            } else {
                tableRow.addView(createTableCell(false, cellParams, typeface, textSize, 100, null), newColumnIndex);
            }
        }
        unsavedChanges = true;
    }

    @Override
    public void insertImage() {
        if (!isCursorPlaced()) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isCursorInTable()) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_insert_into_table, Toast.LENGTH_SHORT).show();
            return;
        }
        pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    @Override
    public void insertRow() {
        HorizontalScrollView tableScrollView = (HorizontalScrollView) nodeEditorFragmentLinearLayout.getFocusedChild();
        ScTableLayout table = (ScTableLayout) tableScrollView.getFocusedChild();
        TableRow focusedTableRow = (TableRow) table.getFocusedChild();
        int newRowIndex = -1;
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            if (row.hasFocus()) {
                newRowIndex = i + 1;
                break;
            }
        }
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow tableRow = new TableRow(getContext());
        tableRow.setLayoutParams(rowParams);
        ViewGroup.LayoutParams cellParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Typeface typeface = getTypeface();
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        for (int i = 0; i < focusedTableRow.getChildCount(); i++) {
            tableRow.addView(createTableCell(false, cellParams, typeface, textSize, 100, null));
        }
        table.addView(tableRow, newRowIndex);
        unsavedChanges = true;
    }

    /**
     * Insert the table at the position of the cursor with provided properties
     * @param indexOfChild index of focused edit text
     * @param startOfSelection position where to insert the table in the focused edit text
     * @param rows count of rows in the table
     * @param cols count of columns in the table
     * @param defaultWidth default width of the cell. The cell will not expand more then set width. Does not apply to header cell.
     * @param lightInterface set tables interface to be lightweight. 0 - normal interface, 1 - lightweight. Has no effect in mobile version
     */
    private void insertTable(int indexOfChild, int startOfSelection, int rows, int cols, int defaultWidth, byte lightInterface) {
        EditText editText = (EditText) nodeEditorFragmentLinearLayout.getChildAt(indexOfChild);
        Typeface typeface = getTypeface();
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        if (startOfSelection == 0 && nodeEditorFragmentLinearLayout.getChildAt(indexOfChild - 1) instanceof HorizontalScrollView) {
            // Table will be inserted before another table
            nodeEditorFragmentLinearLayout.addView(createTableView(rows, cols, defaultWidth, lightInterface, typeface, textSize), indexOfChild);
        } else {
            EditText firstPart = createEditText(typeface, textSize, editText.getText().subSequence(0, startOfSelection));
            EditText secondPart = createEditText(typeface, textSize, editText.getText().subSequence(startOfSelection, editText.getText().length()));
            nodeEditorFragmentLinearLayout.removeViewAt(indexOfChild);
            nodeEditorFragmentLinearLayout.addView(firstPart, indexOfChild);
            nodeEditorFragmentLinearLayout.addView(createTableView(rows, cols, defaultWidth, lightInterface, typeface, textSize), ++indexOfChild);
            nodeEditorFragmentLinearLayout.addView(secondPart, ++indexOfChild);
        }
        unsavedChanges = true;
        getParentFragmentManager().clearFragmentResultListener("tablePropertiesListener");
    }

    /**
     * Checks if cursor is placed in table or not
     * @return true if cursor is in table, false - otherwise
     */
    private boolean isCursorInTable() {
        return nodeEditorFragmentLinearLayout.getFocusedChild() instanceof HorizontalScrollView;
    }

    /**
     * Checks if cursor is placed
     * @return true if cursor is placed, false - otherwise
     */
    private boolean isCursorPlaced() {
        return nodeEditorFragmentLinearLayout.getFocusedChild() != null;
    }

    /**
     * Checks if table can be inserted at the cursor location
     * @return true - if table can be inserted at the cursor location, false - otherwise
     */
    private boolean isTableInsertionAllowed() {
        boolean allowed = true;
        if (!isCursorPlaced()) {
            Toast.makeText(getContext(), R.string.toast_message_insert_table_place_cursor, Toast.LENGTH_SHORT).show();
            return false;
        }
        EditText editText = ((EditText) nodeEditorFragmentLinearLayout.getFocusedChild());
        Object[] spans = editText.getText().getSpans(editText.getSelectionStart(), editText.getSelectionEnd(), Object.class);
        for (Object span: spans) {
            if (span instanceof TypefaceSpanCodebox) {
                allowed = false;
                break;
            }
            if (span instanceof ClickableSpanFile) {
                allowed = false;
                break;
            }
            if (span instanceof ImageSpanFile) {
                allowed = false;
                break;
            }
            if (span instanceof URLSpanWebs) {
                allowed = false;
                break;
            }
        }
        if (!allowed) {
            Toast.makeText(getContext(), R.string.toast_message_table_cant_be_inserted_here, Toast.LENGTH_SHORT).show();
        }
        return allowed;
    }

    /**
     * Loads node content to UI
     */
    public void loadContent() {
        // Clears layout just in case. Most of the time it is needed
        if (nodeEditorFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    nodeEditorFragmentLinearLayout.removeAllViews();
                }
            });
        }
        int textSize = sharedPreferences.getInt("preferences_text_size", 15);
        Typeface typeface = getTypeface();
        for (ScNodeContent part : mainViewModel.getNodeContent().getValue()) {
            if (part.getContentType() == 0) {
                // This adds not only text, but images, codeboxes
                ScNodeContentText scNodeContentText = (ScNodeContentText) part;
                SpannableStringBuilder nodeContentSSB = scNodeContentText.getContent();
                EditText editText = createEditText(typeface, textSize, nodeContentSSB);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        nodeEditorFragmentLinearLayout.addView(editText);
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
                        nodeEditorFragmentLinearLayout.addView(tableScrollView);
                    }
                });
            }
        }
        // If last NodeContent elements is a table it won't be possible to add text after it
        // Adding an extra EditText to allow user to continue typing after last table element
        if (mainViewModel.getNodeContent().getValue().get(mainViewModel.getNodeContent().getValue().size() - 1).getContentType() == 1) {
            EditText editText = createEditText(typeface, textSize, "");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    nodeEditorFragmentLinearLayout.addView(editText);
                }
            });
        }
        // Shows keyboard if opened node for editing has less than 2 characters in the EditText
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mainViewModel.getNodeContent().getValue().size() == 1) {
                    EditText editText = (EditText) nodeEditorFragmentLinearLayout.getChildAt(0);
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
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        onCustomTextEditFocusChangeListener = createOnCustomTextEditFocusChangeListener();
        nodeEditorFragmentLinearLayout = view.findViewById(R.id.node_edit_fragment_linearlayout);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        handler = appContainer.handler;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        color = sharedPreferences.getInt("colorPickerColor", ColorPickerPresets.BLACK.getColor());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Top and bottom paddings are always the same: 14px (5dp)
        nodeEditorFragmentLinearLayout.setPadding(sharedPreferences.getInt("paddingStart", 14), 14, sharedPreferences.getInt("paddingEnd", 14), 14);
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
        textWatcher = new TextWatcher() {
            int lineCount;
            EditText editText;
            boolean changedInput = false;
            @Override
            public void afterTextChanged(Editable s) {
                unsavedChanges = true;
                if (changedInput) {
                    changedInput = false;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!changedInput) {
                    editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
                    lineCount = editText.getLineCount();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!changedInput) {
                    editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
                    if (lineCount < editText.getLineCount()) {
                        int indexOfLastNewline = getLastIndexOfNewLine(editText, start);
                        Matcher allListMatcher = allListStarts.matcher(editText.getText());
                        allListMatcher.region(indexOfLastNewline, editText.getText().length());
                        if (allListMatcher.lookingAt()) {
                            // If newline follows list item line
                            changedInput = true;
                            SpannableStringBuilder newListItem = new SpannableStringBuilder();
                            // Substrings start of the previous list line with the list symbol and spaces
                            newListItem.append(editText.getText().subSequence(indexOfLastNewline, allListMatcher.end()));
                            Matcher checkboxMatcher = checkedCheckbox.matcher(newListItem);
                            Matcher orderedMatcher = orderdList.matcher(newListItem);
                            if (checkboxMatcher.find()) {
                                // Checks if new line chas a checkbox that is in checked or crossed state
                                // If so - replces it with empty checkbox
                                newListItem.replace(checkboxMatcher.start(), checkboxMatcher.end(), CheckBoxSwitch.EMPTY.getString());
                            } else if (orderedMatcher.find()) {
                                // If new line has a number in front of it - replaces the number with currect number + 1
                                int position = Integer.parseInt(orderedMatcher.group(1));
                                newListItem.replace(orderedMatcher.start(1), orderedMatcher.end(1), String.valueOf(position + 1) );
                            }
                            CustomTextEdit customTextEdit = (CustomTextEdit) editText;
                            customTextEdit.getText().insert(start + count, newListItem);
                        }
                    }
                }
            }
        };

        clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBoxToggle((EditText) view);
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
                if (getParentFragmentManager().findFragmentByTag("insertTable") != null) {
                    return false;
                }
                if (menuItem.getItemId() == android.R.id.home) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return true;
                } else if (menuItem.getItemId() == R.id.toolbar_button_save_node) {
                    saveNodeContent();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        ScrollView scrollView = view.findViewById(R.id.edit_node_fragment_scrollview);
        if (savedInstanceState == null) {
            // Tries to scroll screen to the same location where it was when user chose to open editor
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.setScrollY(getArguments().getInt("scrollY"));
                    }
                }, 150);
        } else {
            // Tries to scroll screen to the same location where it was when user rotated device
            if (savedInstanceState.getInt("scrollY") != 0) {
                handler.postDelayed(new Runnable() {
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
        if (mainViewModel.getCurrentNode().isRichText()) {
            NodeContentEditorMenuMainFragment fragment = new NodeContentEditorMenuMainFragment();
            fragment.setNodeContentEditorMenuActions(this);
            fragmentManager.beginTransaction()
                    .add(R.id.edit_node_fragment_button_row_fragment, fragment, null)
                    .commit();
        } else {
            FrameLayout buttonRowFragmentContainer = getView().findViewById(R.id.edit_node_fragment_button_row_fragment);
            buttonRowFragmentContainer.setVisibility(View.GONE);
        }
        loadContent();
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
     * Registered ActivityResultLauncher launches a file picker that allows user to select a file
     * for inserting into node. After user selects the file it is embded into node content.
     * @return ActivityResultLauncher to select a file
     */
    private ActivityResultLauncher<String[]> registerAttachFile() {
        return registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
            if (result != null) {
                DocumentFile file = DocumentFile.fromSingleUri(getContext(), result);
                EditText editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
                editText.getText().insert(editText.getSelectionStart(), DatabaseReader.createAttachFileSpan(getContext(), file.getName(), mainViewModel.getCurrentNode().getUniqueId(), result.toString()));
            }
        });
    }

    /**
     * Registered ActivityResultLauncher launches an image picker that allows user to select an
     * image for inserting into node. After user selects an image it is embded into node content.
     * @return ActivityResultLauncher to select an image
     */
    private ActivityResultLauncher<PickVisualMediaRequest> registerPickImage() {
        return registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            try {
                if (uri != null) {
                    SpannableStringBuilder imageSpan = DatabaseReader.createImageSpan(getContext(), uri);
                    EditText editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
                    editText.getText().insert(editText.getSelectionStart(), imageSpan);
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_insert_image, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Saves current content of the that is being displayed to
     * the database
     */
    private void saveNodeContent() {
        changesSaved = true;
        unsavedChanges = false;
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
            View view = nodeEditorFragmentLinearLayout.getChildAt(i);
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
        mainViewModel.getNodeContent().setValue(nodeContent);
        if (mainViewModel.getCurrentNode().getMasterId().equals("0")) {
            DatabaseReaderFactory.getReader().saveNodeContent(getArguments().getString("nodeUniqueID"));
        } else {
            DatabaseReaderFactory.getReader().saveNodeContent(mainViewModel.getCurrentNode().getMasterId());
        }
        addTextChangedListeners();
    }

    /**
     * Saves user choice to the preference key preferences_unsaved_changes
     * @param choice user choice (ask, exit, save)
     */
    private void saveUnsavedChangesDialogChoice(String choice) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
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
        fragment.setNodeContentEditorMenuActions(this, this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.edit_node_fragment_button_row_fragment, fragment, null)
                .addToBackStack("inserMenu")
                .commit();
    }

    @Override
    public void showListsRow() {
        NodeContentEditorMenuListsFragment fragment = new NodeContentEditorMenuListsFragment();
        fragment.setNodeContentEditorInsertMenuActions(this, this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.edit_node_fragment_button_row_fragment, fragment, null)
                .addToBackStack("listsMenu")
                .commit();
    }

    @Override
    public void startChecklist() {
        if (!isCursorPlaced()) {
            Toast.makeText(getContext(), R.string.toast_message_start_list_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isCursorInTable()) {
            Toast.makeText(getContext(), R.string.toast_message_start_list_insert_into_table, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText editText = (EditText) nodeEditorFragmentLinearLayout.getFocusedChild();
        int[] paraStartEnd = getParagraphStartEnd(editText);
        Matcher allListMatcher = allListStarts.matcher(editText.getText());
        allListMatcher.region(paraStartEnd[0], paraStartEnd[1]);
        // Checking if there is already a list at this line
        if (allListMatcher.lookingAt()) {
            Matcher checkboxMatcher = allCheckbox.matcher(editText.getText());
            checkboxMatcher.region(paraStartEnd[0], paraStartEnd[1]);
            if (checkboxMatcher.find()) {
                // If it's a checkbox list line - deleting it
                editText.getText().replace(checkboxMatcher.start(), checkboxMatcher.end() + 1, "");
            } else {
                // If it's any other list line
                editText.getText().replace(allListMatcher.start(1), allListMatcher.end(1), CheckBoxSwitch.EMPTY.getString());
            }
        } else {
            editText.getText().insert(paraStartEnd[0], new StringBuilder(CheckBoxSwitch.EMPTY.getString()).append(" "));
        }
    }

    @Override
    public void startInsertTable() {
        if (!isTableInsertionAllowed()) {
            return;
        }
        View view = nodeEditorFragmentLinearLayout.getFocusedChild();
        if (view == null) {
            Toast.makeText(getContext(), R.string.toast_message_insert_image_place_cursor, Toast.LENGTH_SHORT).show();
            return;
        }
        // To insert a table EditText has to be split in two at the possition on the cursor
        int indexOfChild = nodeEditorFragmentLinearLayout.indexOfChild(view);
        EditText editText = ((EditText) view);
        int startOfSelection = editText.getSelectionStart();
        FragmentManager fm = getParentFragmentManager();
        fm.setFragmentResultListener("tablePropertiesListener", this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        int rows = result.getInt("rows");
                        int cols = result.getInt("cols");
                        int defaultWidth = result.getInt("defaultWidth");
                        byte lightInterface = result.getByte("lightInterface");
                        insertTable(indexOfChild, startOfSelection, rows, cols, defaultWidth, lightInterface);
                    }
                });
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, CreateTableOptionsFragment.class, null, "insertTable")
                .addToBackStack("insertTable")
                .commit();
    }

    @Override
    public void toggleFontBold() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanBold(), new StyleSpanBold());
                }
            } else {
                editText.getText().setSpan(new StyleSpanBold(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            unsavedChanges = true;
        }
    }

    @Override
    public void toggleFontItalic() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StyleSpanItalic(), new StyleSpanItalic());
                }
            } else {
                editText.getText().setSpan(new StyleSpanItalic(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            unsavedChanges = true;
        }
    }

    @Override
    public void toggleFontMonospace() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
            TypefaceSpanFamily[] spans = editText.getText().getSpans(startOfSelection, endOfSelection, TypefaceSpanFamily.class);
            MonospaceBackgroundColorSpan[] backgroundSpans = editText.getText().getSpans(startOfSelection, endOfSelection, MonospaceBackgroundColorSpan.class);
            if (spans.length > 0) {
                for (TypefaceSpanFamily span: spans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new TypefaceSpanFamily("monospace"), new TypefaceSpanFamily("monospace"));
                }
                for (MonospaceBackgroundColorSpan span: backgroundSpans) {
                    int startOfSpan = editText.getText().getSpanStart(span);
                    int endOfSpan = editText.getText().getSpanEnd(span);
                    editText.getText().removeSpan(span);
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new MonospaceBackgroundColorSpan(R.color.monospace_background), new MonospaceBackgroundColorSpan(R.color.monospace_background));
                }
            } else {
                editText.getText().setSpan(new TypefaceSpanFamily("monospace"), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editText.getText().setSpan(new MonospaceBackgroundColorSpan(R.color.monospace_background), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            unsavedChanges = true;
        }
    }

    @Override
    public void toggleFontStrikethrough() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new StrikethroughSpan(), new StrikethroughSpan());
                }
            } else {
                editText.getText().setSpan(new StrikethroughSpan(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            unsavedChanges = true;
        }
    }

    @Override
    public void toggleFontUnderline() {
        if (nodeEditorFragmentLinearLayout.getFocusedChild() instanceof EditText) {
            if (checkSelectionForCodebox()) {
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
                    reapplySpanOutsideSelection(startOfSelection, endOfSelection, startOfSpan, endOfSpan, new UnderlineSpan(), new UnderlineSpan());
                }
            } else {
                editText.getText().setSpan(new UnderlineSpan(), startOfSelection, endOfSelection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            unsavedChanges = true;
        }
    }
}
