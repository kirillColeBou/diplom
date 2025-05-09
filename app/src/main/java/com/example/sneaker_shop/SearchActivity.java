package com.example.sneaker_shop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.apmem.tools.layouts.FlowLayout;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    private static final int VISIBLE_HISTORY_LIMIT = 20;
    private static final int FULL_HISTORY_LIMIT = 40;
    private FlowLayout searchHistoryContainer;
    private ImageView editHistoryButton;
    private EditText searchEditText;
    private boolean isEditMode = false;
    private Set<String> searchHistory = new HashSet<>();
    private long currentUserId;
    private boolean showFullHistory = false;
    private View moreButtonView;
    private boolean isHistoryBeingProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        searchEditText = findViewById(R.id.search);
        if (getIntent().getBooleanExtra("clear_text", false)) {
            searchEditText.setText("");
        }
        searchHistoryContainer = findViewById(R.id.searchHistoryContainer);
        editHistoryButton = findViewById(R.id.editHistoryButton);
        currentUserId = AuthUtils.getCurrentUserId(this);
        loadSearchHistory();
        updateSearchHistoryUI();
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                filterHistory(s.toString().trim());
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void filterHistory(String query) {
        searchHistoryContainer.removeAllViews();
        if (query.isEmpty()) {
            updateHistoryUI(new ArrayList<>(searchHistory));
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String item : searchHistory) {
            if (item.toLowerCase().startsWith(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        updateHistoryUI(filtered);
    }

    private void updateHistoryUI(List<String> itemsToShow) {
        int limit = showFullHistory ? Integer.MAX_VALUE : VISIBLE_HISTORY_LIMIT;
        int count = Math.min(itemsToShow.size(), limit);
        for (int i = 0; i < count; i++) {
            addSearchHistoryItem(itemsToShow.get(i));
        }
        if (!showFullHistory && itemsToShow.size() > VISIBLE_HISTORY_LIMIT) {
            addMoreButton();
        }
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        if (!query.isEmpty() && !isFinishing()) {
            if (!isHistoryBeingProcessed) {
                isHistoryBeingProcessed = true;
                addToSearchHistory(query);
                isHistoryBeingProcessed = false;
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            Intent intent = new Intent(this, SearchResultActivity.class);
            intent.putExtra("search_query", query);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private void performSearchWithQuery(String query) {
        if (!query.isEmpty() && !isFinishing()) {
            if (!isHistoryBeingProcessed) {
                isHistoryBeingProcessed = true;
                addToSearchHistory(query);
                isHistoryBeingProcessed = false;
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            Intent intent = new Intent(this, SearchResultActivity.class);
            intent.putExtra("search_query", query);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    private void addToSearchHistory(String query) {
        searchHistory.remove(query);
        searchHistory.add(query);
        if (searchHistory.size() > FULL_HISTORY_LIMIT) {
            List<String> tempList = new ArrayList<>(searchHistory);
            searchHistory.remove(tempList.get(tempList.size() - 1));
        }
        updateSearchHistoryUI();
        saveSearchHistory();
        saveSearchToSupabase(query);
    }

    private void saveSearchToSupabase(String query) {
        SearchContext.saveSearchQuery(currentUserId, query);
    }

    private void loadSearchHistory() {
        Set<String> defaultSet = new HashSet<>();
        Set<String> savedHistory = getSharedPreferences("SearchHistory", MODE_PRIVATE)
                .getStringSet(String.valueOf(currentUserId), defaultSet);
        if (savedHistory != null) {
            searchHistory = new HashSet<>(savedHistory);
        }
    }

    private void saveSearchHistory() {
        getSharedPreferences("SearchHistory", MODE_PRIVATE)
                .edit()
                .putStringSet(String.valueOf(currentUserId), searchHistory)
                .apply();
    }

    private void addSearchHistoryItem(String query) {
        View historyItemView = LayoutInflater.from(this)
                .inflate(R.layout.search_history_item, searchHistoryContainer, false);
        TextView textSearchItem = historyItemView.findViewById(R.id.textSearchItem);
        LinearLayout deleteSearchItem = historyItemView.findViewById(R.id.deleteSearchItem);
        textSearchItem.setText(query);
        historyItemView.setOnClickListener(v -> {
            searchEditText.setText(query);
            searchEditText.setSelection(query.length());
            performSearchWithQuery(query);
        });
        historyItemView.setOnLongClickListener(v -> {
            searchEditText.setText(query);
            searchEditText.setSelection(query.length());
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            return true;
        });
        deleteSearchItem.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        historyItemView.setTag(query);
        searchHistoryContainer.addView(historyItemView);
    }

    public void onBack(View view) {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    public void onRedact(View view) {
        isEditMode = !isEditMode;
        editHistoryButton.setImageResource(isEditMode ?
                R.drawable.check_mark : R.drawable.redac_search);
        for (int i = 0; i < searchHistoryContainer.getChildCount(); i++) {
            View child = searchHistoryContainer.getChildAt(i);
            LinearLayout deleteButton = child.findViewById(R.id.deleteSearchItem);
            deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
    }

    public void onDeleteSearchItem(View view) {
        View historyItem = (View) view.getParent();
        String query = (String) historyItem.getTag();
        searchHistory.remove(query);
        searchHistoryContainer.removeView(historyItem);
        saveSearchHistory();
    }

    private void updateSearchHistoryUI() {
        searchHistoryContainer.removeAllViews();
        int itemsToShow = showFullHistory
                ? Math.min(searchHistory.size(), FULL_HISTORY_LIMIT)
                : Math.min(searchHistory.size(), VISIBLE_HISTORY_LIMIT);
        List<String> historyList = new ArrayList<>(searchHistory);
        for (int i = 0; i < itemsToShow; i++) {
            addSearchHistoryItem(historyList.get(i));
        }
        if (searchHistory.size() > VISIBLE_HISTORY_LIMIT && !showFullHistory) {
            addMoreButton();
        }
    }

    private void addMoreButton() {
        moreButtonView = LayoutInflater.from(this)
                .inflate(R.layout.search_history_item, searchHistoryContainer, false);
        TextView textSearchItem = moreButtonView.findViewById(R.id.textSearchItem);
        LinearLayout deleteSearchItem = moreButtonView.findViewById(R.id.deleteSearchItem);
        textSearchItem.setText("Еще...");
        deleteSearchItem.setVisibility(View.GONE);
        moreButtonView.setOnClickListener(v -> {
            showFullHistory = true;
            updateSearchHistoryUI();
        });
        searchHistoryContainer.addView(moreButtonView);
    }

    public void onDeleteSearchText(View view) {
        searchEditText.setText("");
        updateSearchHistoryUI();
        searchEditText.requestFocus();
    }
}