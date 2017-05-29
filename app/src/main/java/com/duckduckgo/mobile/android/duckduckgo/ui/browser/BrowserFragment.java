package com.duckduckgo.mobile.android.duckduckgo.ui.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.duckduckgo.mobile.android.duckduckgo.Injector;
import com.duckduckgo.mobile.android.duckduckgo.R;
import com.duckduckgo.mobile.android.duckduckgo.ui.browser.web.DDGWebChromeClient;
import com.duckduckgo.mobile.android.duckduckgo.ui.browser.web.DDGWebView;
import com.duckduckgo.mobile.android.duckduckgo.ui.browser.web.DDGWebViewClient;
import com.duckduckgo.mobile.android.duckduckgo.ui.navigator.Navigator;
import com.duckduckgo.mobile.android.duckduckgo.ui.tabswitcher.TabSwitcherActivity;
import com.duckduckgo.mobile.android.duckduckgo.util.KeyboardUtils;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by fgei on 5/15/17.
 */

public class BrowserFragment extends Fragment implements BrowserView, OmnibarView {

    public static final String TAG = BrowserFragment.class.getSimpleName();

    public static BrowserFragment newInstance() {
        return new BrowserFragment();
    }

    private static final int REQUEST_PICK_TAB = 199;

    @BindView(R.id.browser_web_view)
    DDGWebView webView;

    @BindView(R.id.appbar_toolbar)
    Toolbar toolbar;

    @BindView(R.id.appbar_edit_text)
    EditText searchEditText;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private Unbinder unbinder;
    private BrowserPresenter browserPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        browserPresenter = Injector.getBrowserPresenter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browser, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        browserPresenter.attachBrowserView(this);
        browserPresenter.attachOmnibarView(this);
        browserPresenter.attachTabView(webView);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initUI();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.resumeTimers();
        webView.onResume();
    }

    @Override
    public void onPause() {
        webView.pauseTimers();
        webView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        browserPresenter.detachViews();
        unbinder.unbind();
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_TAB) {
            handleTabSwitcherResult(resultCode, data);
        }
    }

    @Override
    public void navigateToTabSwitcher() {
        Navigator.navigateToTabSwitcher(getContext(), this, REQUEST_PICK_TAB);
    }

    @Override
    public void displayText(@NonNull String text) {
        searchEditText.setText(text);
    }

    @Override
    public void clearText() {
        searchEditText.setText("");
    }

    @Override
    public void clearFocus() {
        searchEditText.clearFocus();
    }

    @Override
    public void requestFocus() {
        searchEditText.requestFocus();
    }

    @Override
    public void setBackEnabled(boolean enabled) {
        MenuItem backMenuItem = toolbar.getMenu().findItem(R.id.action_go_back);
        backMenuItem.setEnabled(enabled);
    }

    @Override
    public void setForwardEnabled(boolean enabled) {
        MenuItem forwardMenuItem = toolbar.getMenu().findItem(R.id.action_go_forward);
        forwardMenuItem.setEnabled(enabled);
    }

    @Override
    public void setRefreshEnabled(boolean enabled) {
        MenuItem reloadMenuItem = toolbar.getMenu().findItem(R.id.action_refresh);
        reloadMenuItem.setEnabled(enabled);
    }

    @Override
    public void showProgressBar() {
        if (progressBar.getVisibility() == View.GONE) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.animate().alpha(1);
        }
    }

    @Override
    public void hideProgressBar() {
        final WeakReference<ProgressBar> progressBarWeakReference = new WeakReference<>(progressBar);
        progressBar.animate().setStartDelay(400).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ProgressBar progressBar = progressBarWeakReference.get();
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onProgressChanged(int newProgress) {
        progressBar.setProgress(newProgress);
    }

    private void initUI() {
        initWebView(webView);
        initToolbar(toolbar);
        initSearchEditText(searchEditText);
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new DDGWebViewClient(browserPresenter));
        webView.setWebChromeClient(new DDGWebChromeClient(browserPresenter));
    }

    private void initToolbar(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_go_back:
                        browserPresenter.navigateHistoryBackward();
                        return true;
                    case R.id.action_go_forward:
                        browserPresenter.navigateHistoryForward();
                        return true;
                    case R.id.action_refresh:
                        browserPresenter.refreshCurrentPage();
                        return true;
                    case R.id.action_tab_switcher:
                        browserPresenter.openTabSwitcher();
                        return true;
                }
                return false;
            }
        });
    }

    private void initSearchEditText(EditText searchEditText) {
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || wasEnterPressed(event)) {
                    String text = v.getText().toString().trim();
                    browserPresenter.requestSearch(text);
                    KeyboardUtils.hideKeyboard(v);
                    v.clearFocus();
                    return true;
                }
                return false;
            }
        });
    }

    private boolean wasEnterPressed(KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
    }

    private void handleTabSwitcherResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (TabSwitcherActivity.getResultExtra(data)) {
                case TabSwitcherActivity.RESULT_CREATE_NEW_TAB:
                    browserPresenter.createNewTab();
                    break;
                case TabSwitcherActivity.RESULT_TAB_SELECTED:
                    int tabPosition = TabSwitcherActivity.getResultTabSelected(data);
                    browserPresenter.openTab(tabPosition);
                    break;
            }
        }
    }
}
