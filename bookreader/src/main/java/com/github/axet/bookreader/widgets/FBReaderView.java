package com.github.axet.bookreader.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.github.axet.androidlibrary.net.HttpClient;
import com.github.axet.androidlibrary.preferences.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.bookreader.R;
import com.github.axet.bookreader.app.ReaderPreferences;
import com.github.axet.bookreader.app.Plugin;
import com.github.axet.bookreader.app.Reflow;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.services.ImagesProvider;
import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.github.johnpersano.supertoasts.util.OnDismissWrapper;

import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.fbreader.options.ImageOptions;
import org.geometerplus.fbreader.fbreader.options.MiscOptions;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.options.StringPair;
import org.geometerplus.zlibrary.core.options.ZLOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextControlElement;
import org.geometerplus.zlibrary.text.view.ZLTextElement;
import org.geometerplus.zlibrary.text.view.ZLTextElementArea;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextHighlighting;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageElement;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import su.sv.managers.OnBookPagerManager;
import timber.log.Timber;

// SelectionView refactoring imports

public class FBReaderView extends RelativeLayout {
    public static final String ACTION_MENU = FBReaderView.class.getCanonicalName() + ".ACTION_MENU";

    public static final int PAGE_OVERLAP_PERCENTS = 5; // percents
    public static final int PAGE_PAPER_COLOR = 0x80ffffff;
    public FBReaderApp app;
    public ConfigShadow config;
    public ZLViewWidget widget;
    public int battery;
    public Storage.FBook book;
    public Plugin.View pluginview;
    public Listener listener;
    public TTSPopup tts;
    String title;
    Window w;
    FBFooterView footer;
    SelectionView selection;
    ZLTextPosition scrollDelayed;
    boolean scrollCentered; // центрировать позицию при прокрутке
    private boolean isFullscreenMode = false; // track fullscreen state
    DrawerLayout drawer;
    Plugin.View.Search search;
    int searchPagePending;
    private int searchCurrentIndex = 0; // Track current search result index

    public FBReaderView(Context context) { // create child view
        super(context);
        create();
    }

    public FBReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public FBReaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public FBReaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    public static void showControls(final ViewGroup p, final View areas) {
        p.removeCallbacks((Runnable) areas.getTag());
        p.addView(areas);
        Runnable hide = () -> hideControls(p, areas);
        areas.setTag(hide);
        p.postDelayed(hide, 3000);
    }

    public static void hideControls(final ViewGroup p, final View areas) {
        p.removeCallbacks((Runnable) areas.getTag());
        areas.setTag(null);
        ValueAnimator v = ValueAnimator.ofFloat(1f, 0f);
        v.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewCompat.setAlpha(areas, (float) animation.getAnimatedValue());
            }
        });
        v.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                p.removeView(areas);
            }
        });
        v.setDuration(500);
        v.start();
    }

    public static Rect findUnion(List<ZLTextElementArea> areas, Storage.Bookmark bm) {
        Rect union = null;
        for (ZLTextElementArea a : areas) {
            if (bm.start.compareTo(a) <= 0 && bm.end.compareTo(a) >= 0) {
                if (union == null)
                    union = new Rect(a.XStart, a.YStart, a.XEnd, a.YEnd);
                else
                    union.union(a.XStart, a.YStart, a.XEnd, a.YEnd);
            }
        }
        return union;
    }

    public void create() {
        config = new ConfigShadow();
        app = new FBReaderApp(getContext());

        app.setWindow(new FBApplicationWindow());
        app.initWindow();

        config();

        app.BookTextView = new CustomView(app);
        app.setView(app.BookTextView);

        footer = new FBFooterView(getContext(), this);

        setWidget(Widgets.PAGING);
    }

    public void configColorProfile() {
        config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.DAY);
        ColorProfile p = ColorProfile.get(ColorProfile.DAY);
        config.setValue(p.BackgroundOption, 0xF5E5CC);
        config.setValue(p.WallpaperOption, "");
    }

    public void configWidget(SharedPreferences shared) {
        String mode = shared.getString(ReaderPreferences.PREFERENCE_VIEW_MODE, "");
        setWidget(mode.equals(FBReaderView.Widgets.CONTINUOUS.toString()) ? FBReaderView.Widgets.CONTINUOUS : FBReaderView.Widgets.PAGING);
    }

    public void config() {
        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        configColorProfile();

        int d = shared.getInt(ReaderPreferences.PREFERENCE_FONTSIZE_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue());
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, d);

        String f = shared.getString(ReaderPreferences.PREFERENCE_FONTFAMILY_FBREADER, app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption.getValue());
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption, f);

        boolean ignoreCSSFonts = shared.getBoolean(ReaderPreferences.PREFERENCE_IGNORE_EMBEDDED_FONTS, false);
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().UseCSSFontFamilyOption, !ignoreCSSFonts);

        config.setValue(app.MiscOptions.AllowScreenBrightnessAdjustment, false);
        config.setValue(app.ViewOptions.ScrollbarType, 0); // FBView.SCROLLBAR_SHOW_AS_FOOTER
        config.setValue(app.ViewOptions.getFooterOptions().ShowProgress, FooterOptions.ProgressDisplayType.asPages);

        config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.openImageView);
        config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.covers);

        config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.startSelecting);
    }

    public void setWidget(Widgets w) {
        switch (w) {
            case CONTINUOUS:
                setWidget(new ScrollWidget(this));
                break;
            case PAGING:
                setWidget(new PagerWidget(this));
                break;
        }
    }

    /**
     * Возвращает текущий тип widget.
     * Используется для проверки перед переключением режима.
     */
    public Widgets getWidgetType() {
        if (widget instanceof ScrollWidget) {
            return Widgets.CONTINUOUS;
        } else if (widget instanceof PagerWidget) {
            return Widgets.PAGING;
        }
        return Widgets.PAGING; // default
    }

    public void setWidget(ZLViewWidget v) {
        // При смене режима (paging ↔ scroll) закрываем выделение полностью
        // Координаты выделения в разных режимах не совместимы
        if (selection != null) {
            Timber.tag("voronin").d("FBReaderView setWidget: closing selection before mode switch");
            selectionClose();
        }
        overlaysClose();
        ZLTextPosition pos = null;
        if (widget != null) {
            pos = getPosition();
            // Сбрасываем zoom перед удалением widget
            // Zoom в разных режимах не совместим (paging vs scroll имеют разные coordinate systems)
            Timber.tag("voronin").d("FBReaderView setWidget: resetting zoom before mode switch");
            setScaleX(1.0f);
            setScaleY(1.0f);
            setTranslationX(0f);
            setTranslationY(0f);
            removeView((View) widget);
        }
        widget = v;
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, footer.getId());
        addView((View) v, 0, lp);
        if (pos != null)
            gotoPosition(pos);
        if (footer != null)
            removeView(footer);
        lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        addView(footer, lp);
    }

    public void loadBook(Storage.FBook fbook) {
        try {
            this.book = fbook;
            if (book.info == null)
                book.info = new Storage.RecentInfo();
            FormatPlugin plugin = Storage.getPlugin((Storage.Info) app.SystemInfo, fbook);
            if (plugin instanceof Plugin) {
                pluginview = ((Plugin) plugin).create(fbook);
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    gotoPluginPosition(book.info.position);
            } else {
                BookModel Model = BookModel.createModel(fbook.book, plugin);
                ZLTextHyphenator.Instance().load(fbook.book.getLanguage());
                app.BookTextView.setModel(Model.getTextModel());
                app.Model = Model;
                if (book.info.position != null)
                    app.BookTextView.gotoPosition(book.info.position);
                if (book.info.scale != null)
                    config.setValue(app.ImageOptions.FitToScreen, book.info.scale);
                if (book.info.fontsize != null)
                    config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, book.info.fontsize);
                bookmarksUpdate();
            }
            widget.repaint();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeBook() {
        if (pluginview != null) {
            pluginview.close();
            pluginview = null;
        }
        app.BookTextView.setModel(null);
        app.Model = null;
        book = null;
        if (tts != null) {
            tts.close();
            tts = null;
        }
    }

    public ZLTextPosition getPosition() {
        if (pluginview != null) {
            if (widget instanceof ScrollWidget) {
                int first = ((ScrollWidget) widget).findFirstPage();
                if (first != -1) {
                    RecyclerView.ViewHolder h = ((ScrollWidget) widget).findViewHolderForAdapterPosition(first);
                    ScrollWidget.ScrollAdapter.PageView p = (ScrollWidget.ScrollAdapter.PageView) h.itemView;
                    ScrollWidget.ScrollAdapter.PageCursor c = ((ScrollWidget) widget).adapter.pages.get(first);
                    Plugin.Page info = pluginview.getPageInfo(p.getWidth(), p.getHeight(), c);
                    if (p.info != null) { // reflow can be true but reflower == null
                        ArrayList<Rect> rr = new ArrayList<>(p.info.dst.keySet());
                        Collections.sort(rr, new SelectionView.UL());
                        int top = -p.getTop();
                        for (Rect r : rr) {
                            if (r.top > top || (r.top < top && r.bottom > top)) {
                                int screen = r.top - top; // offset from top screen to top element
                                double ratio = p.info.bm.width() / (float) p.getWidth();
                                int offset = (int) (p.info.dst.get(r).top / ratio - screen); // recommended page offset (element - current screen offset)
                                offset *= info.ratio;
                                return new ZLTextFixedPosition(pluginview.current.pageNumber, offset, 0);
                            }
                        }
                    } else {
                        int top = -p.getTop();
                        if (top < 0)
                            top = 0;
                        int offset = (int) (top * info.ratio);
                        return new ZLTextFixedPosition(pluginview.current.pageNumber, offset, 0);
                    }
                }
            }
            return pluginview.getPosition();
        } else {
            if (widget instanceof ScrollWidget) {
                int first = ((ScrollWidget) widget).findFirstPage();
                if (first != -1) {
                    ScrollWidget.ScrollAdapter.PageCursor c = ((ScrollWidget) widget).adapter.pages.get(first);
                    RecyclerView.ViewHolder h = ((ScrollWidget) widget).findViewHolderForAdapterPosition(first);
                    ScrollWidget.ScrollAdapter.PageView p = (ScrollWidget.ScrollAdapter.PageView) h.itemView;
                    if (p.text != null) { // happens when view invalidate / recycled before calling getPosition()
                        int top = -p.getTop();
                        for (ZLTextElementArea a : p.text.areas()) {
                            if (a.YStart > top || (a.YStart < top && a.YEnd > top)) {
                                ZLTextParagraphCursor paragraphCursor = new ZLTextParagraphCursor(app.Model.getTextModel(), a.getParagraphIndex());
                                ZLTextWordCursor wordCursor = new ZLTextWordCursor(paragraphCursor);
                                wordCursor.moveTo(a);
                                ZLTextFixedPosition last;
                                ZLTextElement e;
                                do {
                                    last = new ZLTextFixedPosition(wordCursor);
                                    wordCursor.previousWord();
                                    e = wordCursor.getElement();
                                } while (e instanceof ZLTextControlElement && wordCursor.compareTo(c.start) >= 0);
                                return last;
                            }
                        }
                    }
                }
            }
            return new ZLTextFixedPosition(app.BookTextView.getStartCursor());
        }
    }

    public void setWindow(Window w) {
        this.w = w;
    }

    public void setActivity(final Activity a, OnBookPagerManager onBookPagerManager) {

        app.addAction(ActionCode.DISPLAY_BOOK_POPUP, new FBAction(app) { //  new DisplayBookPopupAction(this, myFBReaderApp))
            @Override
            protected void run(Object... params) {
            }
        });
        app.addAction(ActionCode.PROCESS_HYPERLINK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (pluginview != null) {
                    BookModel.Label l = (BookModel.Label) params[0];
                    showHyperlink(l);
                    return;
                }
                final ZLTextRegion region = app.BookTextView.getOutlinedRegion();
                if (region == null) {
                    return;
                }
                final ZLTextRegion.Soul soul = region.getSoul();
                if (soul instanceof ZLTextHyperlinkRegionSoul) {
                    app.BookTextView.hideOutline();
                    final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul) soul).Hyperlink;
                    switch (hyperlink.Type) {
                        case FBHyperlinkType.EXTERNAL:
                            AboutPreferenceCompat.openUrlDialog(getContext(), hyperlink.Id);
                            break;
                        case FBHyperlinkType.INTERNAL:
                        case FBHyperlinkType.FOOTNOTE: {
                            final AutoTextSnippet snippet = app.getFootnoteData(hyperlink.Id);
                            if (snippet == null)
                                break;
                            app.Collection.markHyperlinkAsVisited(app.getCurrentBook(), hyperlink.Id);
                            final boolean showToast;
                            switch (app.MiscOptions.ShowFootnoteToast.getValue()) {
                                default:
                                case never:
                                    showToast = false;
                                    break;
                                case footnotesOnly:
                                    showToast = hyperlink.Type == FBHyperlinkType.FOOTNOTE;
                                    break;
                                case footnotesAndSuperscripts:
                                    showToast =
                                            hyperlink.Type == FBHyperlinkType.FOOTNOTE ||
                                                    region.isVerticallyAligned();
                                    break;
                                case allInternalLinks:
                                    showToast = true;
                                    break;
                            }
                            if (showToast) {
                                final SuperActivityToast toast;
                                if (snippet.IsEndOfText) {
                                    toast = new SuperActivityToast(a, SuperToast.Type.STANDARD);
                                } else {
                                    toast = new SuperActivityToast(a, SuperToast.Type.BUTTON);
                                    toast.setButtonIcon(
                                            android.R.drawable.ic_menu_more,
                                            ZLResource.resource("toast").getResource("more").getValue()
                                    );
                                    toast.setOnClickWrapper(new OnClickWrapper("ftnt", new SuperToast.OnClickListener() {
                                        @Override
                                        public void onClick(View view, Parcelable token) {
                                            showHyperlink(hyperlink);
                                        }
                                    }));
                                }
                                toast.setText(snippet.getText());
                                toast.setDuration(app.MiscOptions.FootnoteToastDuration.getValue().Value);
                                toast.setOnDismissWrapper(new OnDismissWrapper("ftnt", new SuperToast.OnDismissListener() {
                                    @Override
                                    public void onDismiss(View view) {
                                        app.BookTextView.hideOutline();
                                    }
                                }));
                                app.BookTextView.outlineRegion(region);
                                showToast(toast);
                            } else {
                                book.info.position = getPosition();
                                showHyperlink(hyperlink);
                            }
                            break;
                        }
                    }
                } else if (soul instanceof ZLTextImageRegionSoul) {
                    final ZLTextImageRegionSoul image = ((ZLTextImageRegionSoul) soul);
                    final View anchor = new View(getContext());
                    LayoutParams lp = new LayoutParams(region.getRight() - region.getLeft(), region.getBottom() - region.getTop());
                    lp.leftMargin = region.getLeft();
                    lp.topMargin = region.getTop();
                    if (widget instanceof ScrollWidget) {
                        ScrollWidget.ScrollAdapter.PageView p = ((ScrollWidget) widget).findRegionView(soul);
                        lp.leftMargin += p.getLeft();
                        lp.topMargin += p.getTop();
                    }
                    FBReaderView.this.addView(anchor, lp);
                    final PopupMenu menu = new PopupMenu(getContext(), anchor, Gravity.BOTTOM);
                    menu.inflate(R.menu.image_menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            if (id == R.id.action_open) {
                                String name = image.ImageElement.Id;
                                Uri uri = Uri.parse(image.ImageElement.URL);
                                Intent intent = ImagesProvider.getProvider().openIntent(uri, name);
                                getContext().startActivity(intent);
                            } else if (id == R.id.action_share) {
                                String name = image.ImageElement.Id;
                                String type = Storage.getTypeByExt(ImagesProvider.EXT);
                                Uri uri = Uri.parse(image.ImageElement.URL);
                                Intent intent = ImagesProvider.getProvider().shareIntent(uri, name, type, Storage.getTitle(book.info) + " (" + name + ")");
                                getContext().startActivity(intent);
                            } else if (id == R.id.action_original) {
                                ((CustomView) app.BookTextView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.OriginalSize);
                                resetCaches();
                            } else if (id == R.id.action_zoom) {
                                ((CustomView) app.BookTextView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.FitMaximum);
                                resetCaches();
                            } else if (id == R.id.action_original_all) {
                                book.info.scales.clear();
                                book.info.scale = FBView.ImageFitting.covers;
                                config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.covers);
                                resetCaches();
                            } else if (id == R.id.action_zoom_all) {
                                book.info.scales.clear();
                                book.info.scale = FBView.ImageFitting.all;
                                config.setValue(app.ImageOptions.FitToScreen, FBView.ImageFitting.all);
                                resetCaches();
                            }
                            return true;
                        }
                    });
                    menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            app.BookTextView.hideOutline();
                            widget.repaint();
                            FBReaderView.this.removeView(anchor);
                        }
                    });
                    // allow anchor view to be placed
                    FBReaderView.this.post(() -> menu.show());
                } else if (soul instanceof ZLTextWordRegionSoul) {
                    DictionaryUtil.openTextInDictionary(
                            a,
                            ((ZLTextWordRegionSoul) soul).Word.getString(),
                            true,
                            region.getTop(),
                            region.getBottom(),
                            () -> {
                                // a.outlineRegion(soul);
                            }
                    );
                }
            }
        });
        app.addAction(ActionCode.SHOW_MENU, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                // Toggle fullscreen mode - listener notification is inside toggleFullscreen()
                toggleFullscreen();
            }

            private boolean toggleFullscreen() {
                if (w != null) {
                    // Use modern WindowInsetsController API (Android 11+)
                    // This avoids flickering during fullscreen transition
                    WindowInsetsControllerCompat controller =
                        new WindowInsetsControllerCompat(w, w.getDecorView());

                    // Set window background to match book background to avoid flickering
                    // Book bgColor may have alpha=0, use wallpaper/paper color instead
                    int bgColor = app.BookTextView.getBackgroundColor().intValue();
                    // If alpha is 0, use paper color (light cream)
                    int bgColorWithAlpha = (bgColor & 0xFF000000) == 0
                        ? (0xFF << 24) | bgColor  // Force opaque if transparent
                        : bgColor;  // Keep original if already has alpha
                    w.getDecorView().setBackgroundColor(bgColorWithAlpha);

                    // Notify listener BEFORE native toggle to sync with Compose
                    // This ensures Scaffold updates first, then bars hide
                    Timber.tag("voronin").d("=== Fullscreen Toggle ===");
                    Timber.tag("voronin").d("Current mode: isFullscreenMode=%s", isFullscreenMode);
                    Timber.tag("voronin").d("Widget type: %s", widget.getClass().getSimpleName());
                    Timber.tag("voronin").d("Book bgColor: 0x%08X", bgColor);
                    Timber.tag("voronin").d("Window bgColor set: 0x%08X", bgColorWithAlpha);
                    Timber.tag("voronin").d("DecorView size: %dx%d", w.getDecorView().getWidth(), w.getDecorView().getHeight());
                    Timber.tag("voronin").d("FBReaderView size: %dx%d", getWidth(), getHeight());
                    if (widget instanceof ScrollWidget) {
                        ScrollWidget sw = (ScrollWidget) widget;
                        Timber.tag("voronin").d("ScrollWidget mainAreaHeight: %d", sw.getMainAreaHeight());
                        Timber.tag("voronin").d("ScrollWidget height: %d", sw.getHeight());
                    }

                    // Use tracked state instead of unreliable insets check
                    if (isFullscreenMode) {
                        // Exit fullscreen - notify first, then show bars
                        Timber.tag("voronin").d("Action: EXIT fullscreen");
                        isFullscreenMode = false;
                        if (listener != null) {
                            listener.onFullscreenToggle(false);
                        }
                        // Delay native toggle slightly to let Compose update first
                        post(() -> {
                            controller.show(WindowInsetsCompat.Type.systemBars());
                            // Update selection coordinates after fullscreen change
                            updateSelectionAfterFullscreenChange();
                        });
                        return false; // now not fullscreen
                    } else {
                        // Enter fullscreen - notify first, then hide bars
                        Timber.tag("voronin").d("Action: ENTER fullscreen");
                        isFullscreenMode = true;
                        if (listener != null) {
                            listener.onFullscreenToggle(true);
                        }
                        // Delay native toggle slightly to let Compose update first
                        post(() -> {
                            controller.hide(WindowInsetsCompat.Type.systemBars());
                            controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            );
                            // Update selection coordinates after fullscreen change
                            updateSelectionAfterFullscreenChange();
                        });
                        return true; // now fullscreen
                    }
                }
                return false;
            }
        });
        app.addAction(ActionCode.SHOW_NAVIGATION, new FBAction(app) {
            @Override
            public boolean isVisible() {
                if (pluginview != null)
                    return true;
                final ZLTextModel textModel = app.BookTextView.getModel();
                return textModel != null && textModel.getParagraphsNumber() != 0;
            }

            @Override
            protected void run(Object... params) {
                if (listener != null) {
                    listener.onNavigationRequest();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_SHOW_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                final ZLTextView view = app.getTextView();
                if (listener != null) {
                    listener.onSelectionShow(view.getSelectionStartY(), view.getSelectionEndY());
                }
            }
        });
        app.addAction(ActionCode.SELECTION_HIDE_PANEL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                // Уведомляем listener о скрытии панели
                if (listener != null) {
                    listener.onSelectionHide();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                String text;

                if (selection != null) {
                    text = selection.selection.getText();
                } else {
                    TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                    if (snippet == null)
                        return;
                    text = snippet.getText();
                }

                app.BookTextView.clearSelection();
                selectionClose();

                final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Application.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                UIMessageUtil.showMessageText(a, clipboard.getText().toString());

                if (widget instanceof ScrollWidget) {
                    ((ScrollWidget) widget).adapter.processInvalidate();
                    ((ScrollWidget) widget).adapter.processClear();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_SHARE, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                String text;

                if (selection != null) {
                    text = selection.selection.getText();
                } else {
                    TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                    if (snippet == null)
                        return;
                    text = snippet.getText();
                }

                app.BookTextView.clearSelection();
                selectionClose();

                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType(HttpClient.CONTENTTYPE_TEXT);
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, Storage.getTitle(book.info));
                intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                a.startActivity(Intent.createChooser(intent, null));

                if (widget instanceof ScrollWidget) {
                    // do not clear immidiallty. let onPause to be called before p.text = null
                    FBReaderView.this.post(() -> {
                        ((ScrollWidget) widget).adapter.processInvalidate();
                        ((ScrollWidget) widget).adapter.processClear();
                    });
                }
            }
        });
        app.addAction(ActionCode.SELECTION_BOOKMARK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (params.length != 0) {
                    Storage.Bookmark bm = (Storage.Bookmark) params[0];
                    // Открываем BottomSheet через listener
                    if (listener != null) {
                        listener.onEditBookmark(bm);
                    }
                } else {
                    if (book.info.bookmarks == null)
                        book.info.bookmarks = new Storage.Bookmarks();
                    Storage.Bookmark bm;
                    if (selection != null) {
                        bm = new Storage.Bookmark(selection.selection.getText(), selection.selection.getStart(), selection.selection.getEnd());
                    } else {
                        TextSnippet snippet = app.BookTextView.getSelectedSnippet();
                        bm = new Storage.Bookmark(snippet.getText(), snippet.getStart(), snippet.getEnd());
                    }
                    // Сохраняем coverUrl книги в закладке на момент создания
                    bm.coverUrl = book.info.coverUrl;
                    book.info.bookmarks.add(bm);
                    bookmarksUpdate();
                    if (listener != null)
                        listener.onBookmarksUpdate();
                    app.BookTextView.clearSelection();
                    selectionClose();
                }
            }
        });
        app.addAction(ActionCode.SELECTION_CLEAR, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                app.BookTextView.clearSelection();
                selectionClose();
                if (widget instanceof ScrollWidget) {
                    ((ScrollWidget) widget).adapter.processInvalidate();
                    ((ScrollWidget) widget).adapter.processClear();
                }
            }
        });

        app.addAction(ActionCode.FIND_PREVIOUS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (search != null) {
                    Runnable run = () -> {
                        final int page = search.prev();
                        if (page == -1)
                            return;
                        FBReaderView.this.post(() -> {
                            if (widget instanceof ScrollWidget) {
                                ((ScrollWidget) widget).searchPage(page);
                                return;
                            }
                            if (widget instanceof PagerWidget) {
                                ((PagerWidget) widget).searchPage(page);
                            }
                        });
                    };
                    UIUtil.wait("search", run, getContext());
                    return;
                } else {
                    app.BookTextView.findPrevious();
                }
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.FIND_NEXT, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (search != null) {
                    Runnable run = () -> {
                        final int page = search.next();
                        if (page == -1)
                            return;
                        FBReaderView.this.post(() -> {
                            if (widget instanceof ScrollWidget) {
                                ((ScrollWidget) widget).searchPage(page);
                                return;
                            }
                            if (widget instanceof PagerWidget) {
                                ((PagerWidget) widget).searchPage(page);
                            }
                        });
                    };
                    UIUtil.wait("search", run, getContext());
                    return;
                } else {
                    app.BookTextView.findNext();
                }
                resetNewPosition();
            }
        });
        app.addAction(ActionCode.CLEAR_FIND_RESULTS, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                if (listener != null)
                    listener.onSearchClose();
                searchClose();
                app.BookTextView.clearFindResults();
                resetNewPosition();
            }
        });

        app.addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                scrollNextPage();
            }
        });
        app.addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                scrollPrevPage();
            }
        });
        app.addAction(ActionCode.ASK_QUESTION, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                selectionSVAction(onBookPagerManager::askQuestion);
            }
        });
        app.addAction(ActionCode.TEL_ABOUT_MISSPELL, new FBAction(app) {
            @Override
            protected void run(Object... params) {
                selectionSVAction(onBookPagerManager::tellAboutMisspell);
            }
        });
    }

    private void selectionSVAction(CustomAction action) {
        TextSnippet snippet = app.BookTextView.getSelectedSnippet();

        final String text;
        final int pageIndex;
        if (selection != null) {
            text = selection.selection.getText();
            pageIndex = selection.selection.getStart().getParagraphIndex();
        } else {
            if (snippet == null)
                return;
            text = snippet.getText();
            pageIndex = snippet.getStart().getParagraphIndex();
        }

        app.BookTextView.clearSelection();
        selectionClose();

        final String title = book.info != null ? book.info.title : "";
        final String authors = book.info != null ? book.info.authors : "";

        action.action(
                this.getContext(),
                text,
                title != null ? title : "",
                authors != null ? authors : "",
                pageIndex
        );
    }

    private interface CustomAction {
        void action(
                Context context,
                String selectionText,
                String title,
                String author,
                int page
        );
    }

    public void scrollNextPage() {
        final PageTurningOptions preferences = app.PageTurningOptions;
        widget.startAnimatedScrolling(
                FBView.PageIndex.next,
                preferences.horizontal.getValue()
                        ? FBView.Direction.rightToLeft : FBView.Direction.up,
                preferences.animationSpeed.getValue()
        );
    }

    public void scrollPrevPage() {
        final PageTurningOptions preferences = app.PageTurningOptions;
        widget.startAnimatedScrolling(
                FBView.PageIndex.previous,
                preferences.horizontal.getValue()
                        ? FBView.Direction.rightToLeft : FBView.Direction.up,
                preferences.animationSpeed.getValue()
        );
    }

    public void setDrawer(DrawerLayout drawer) {
        this.drawer = drawer;
    }

    void showHyperlink(final ZLTextHyperlink hyperlink) {
        final BookModel.Label label = app.Model.getLabel(hyperlink.Id);
        showHyperlink(label);
    }

    void showHyperlink(final BookModel.Label label) {
        Context context = getContext();

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        WallpaperLayout f = new WallpaperLayout(context);
        ImageButton c = new ImageButton(context);
        c.setImageResource(com.github.axet.androidlibrary.R.drawable.ic_close_black_24dp);
        c.setColorFilter(ThemeUtils.getThemeColor(context, com.github.axet.androidlibrary.R.attr.colorAccent));
        f.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP));

        final FBReaderView r = new FBReaderView(context) {
            @Override
            public void config() {
                super.config();
                config.setValue(app.ViewOptions.ScrollbarType, 0);
                config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.doNothing);
                config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.doNothing);
            }
        };

        r.app.addAction(ActionCode.PROCESS_HYPERLINK, new FBAction(r.app) {
            @Override
            protected void run(Object... params) {
                if (r.pluginview != null) {
                    BookModel.Label l = (BookModel.Label) params[0];
                    r.app.BookTextView.gotoPosition(l.ParagraphIndex, 0, 0);
                    r.resetNewPosition();
                    return;
                }
                final ZLTextRegion region = r.app.BookTextView.getOutlinedRegion();
                if (region == null) {
                    return;
                }
                final ZLTextRegion.Soul soul = region.getSoul();
                if (soul instanceof ZLTextHyperlinkRegionSoul) {
                    r.app.BookTextView.hideOutline();
                    r.widget.repaint();
                    final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul) soul).Hyperlink;
                    switch (hyperlink.Type) {
                        case FBHyperlinkType.EXTERNAL:
                            AboutPreferenceCompat.openUrlDialog(getContext(), hyperlink.Id);
                            break;
                        case FBHyperlinkType.INTERNAL:
                        case FBHyperlinkType.FOOTNOTE: {
                            final BookModel.Label label = r.app.Model.getLabel(hyperlink.Id);
                            r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                            r.resetNewPosition();
                        }
                    }
                }
            }
        });

        SharedPreferences shared = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        r.configWidget(shared);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ll.addView(f, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.addView(r, rlp);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(ll);
        builder.setOnDismissListener(dialog -> listener.onDismissDialog());
        if (label.ModelId == null) {
            builder.setNeutralButton(R.string.sv_keep_reading_position, (dialog, which) -> gotoPosition(r.getPosition()));
        }
        builder.setPositiveButton(com.github.axet.androidlibrary.R.string.close, (dialog, which) -> {
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            w.setLayout(getWidth(), getHeight()); // fixed size after creation
            r.loadBook(book);
            if (label.ModelId == null) {
                r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                r.app.setView(r.app.BookTextView);
            } else {
                final ZLTextModel model = r.app.Model.getFootnoteModel(label.ModelId);
                r.app.BookTextView.setModel(model);
                r.app.setView(r.app.BookTextView);
                r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
            }
        });
        c.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    void showToast(SuperActivityToast toast) {
        toast.show();
    }

    void gotoPluginPosition(ZLTextPosition p) {
        if (p == null)
            return;
        if (widget instanceof ScrollWidget) {
            // Для центрирования или при наличии offset - используем scrollDelayed
            if (p.getElementIndex() != 0 || scrollCentered) {
                scrollDelayed = p;
                p = new ZLTextFixedPosition(p.getParagraphIndex(), 0, 0);
            }
        }
        pluginview.gotoPosition(p);
    }

    public void gotoPosition(TOCTree.Reference p) {
        if (p.Model != null)
            app.BookTextView.setModel(p.Model);
        gotoPosition(new ZLTextFixedPosition(p.ParagraphIndex, 0, 0));
    }

    public void gotoPosition(ZLTextPosition p) {
        scrollCentered = false;
        if (pluginview != null)
            gotoPluginPosition(p);
        else
            app.BookTextView.gotoPosition(p);
        resetNewPosition();
    }

    /**
     * Переход к позиции с центрированием на экране (для непрерывного режима)
     */
    public void gotoPositionCentered(ZLTextPosition p) {
        scrollCentered = true;
        if (pluginview != null)
            gotoPluginPosition(p);
        else
            app.BookTextView.gotoPosition(p);
        resetNewPosition();
    }

    public void resetNewPosition() { // get position from new loaded page, then reset
        if (widget instanceof ScrollWidget) {
            ((ScrollWidget) widget).adapter.reset();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void reset() { // keep current position, then reset
        if (widget instanceof ScrollWidget) {
            ((ScrollWidget) widget).updatePosition();
            ((ScrollWidget) widget).adapter.reset();
            if (pluginview != null)
                ((ScrollWidget) widget).updateOverlays();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void updateTheme() {
        if (pluginview != null)
            pluginview.updateTheme();
        if (widget instanceof ScrollWidget) {
            ((ScrollWidget) widget).requestLayout(); // repaint views
            widget.reset();
        } else {
            widget.reset();
            widget.repaint();
        }
    }

    public void resetCaches() {
        app.clearTextCaches();
        reset();
    }

    public void invalidateFooter() {
        if (footer == null) {
            if (widget instanceof ScrollWidget)
                ((ScrollWidget) widget).invalidate();
            else
                widget.repaint();
        } else {
            footer.invalidate();
        }
    }

    public void clearReflowPage() {
        pluginview.current.pageOffset = 0;
        if (pluginview.reflower != null)
            pluginview.reflower.index = 0;
    }

    public void selectionOpen(Plugin.View.Selection s) {
        // Закрываем предыдущее выделение без уведомления listener
        // (мы сразу покажем новое, поэтому SELECTION_HIDE_PANEL не нужен)
        if (selection != null) {
            selectionCloseInternal();
        }

        // Создаем callbacks для управления drawer и panel
        SelectionCallbacks callbacks = new SelectionCallbacks() {
            @Override
            public void onDragStart(HandleType handle) {
                if (drawer != null)
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                app.runAction(ActionCode.SELECTION_HIDE_PANEL);
            }

            @Override
            public void onDragEnd(HandleType handle) {
                if (drawer != null)
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                app.runAction(ActionCode.SELECTION_SHOW_PANEL);
            }

            @Override
            public void onBoundsChanged(Rect startBounds, Rect endBounds) {
                // Bounds changes handled automatically through update()
            }
        };

        selection = new SelectionView(getContext(), (CustomView) app.BookTextView, s, callbacks);
        addView(selection);
        if (widget instanceof ScrollWidget) {
            ((ScrollWidget) widget).updateOverlays();
            selection.setClipHeight(((ScrollWidget) widget).getMainAreaHeight());
        } else {
            selection.setClipHeight(((ZLAndroidWidget) widget).getMainAreaHeight());
        }
        app.runAction(ActionCode.SELECTION_SHOW_PANEL);
    }

    /**
     * Закрывает selection без уведомления listener (без SELECTION_HIDE_PANEL).
     * Используется внутри selectionOpen() чтобы очистить старый selection перед созданием нового.
     */
    private void selectionCloseInternal() {
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).selectionClose();
        if (selection != null) {
            selection.close();
            removeView(selection);
            selection = null;
        }
    }

    public void selectionClose() {
        selectionCloseInternal();
        app.runAction(ActionCode.SELECTION_HIDE_PANEL);
    }

    /**
     * Обновляет координаты выделения после смены fullscreen режима.
     * При fullscreen mainAreaHeight меняется, нужно пересчитать координаты SelectionView.
     */
    private void updateSelectionAfterFullscreenChange() {
        if (selection == null || widget == null) {
            Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: no selection or widget, skipping");
            return;
        }

        Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: updating selection coordinates for fullscreen change");

        // Обновляем clip height для SelectionView
        if (widget instanceof ScrollWidget) {
            selection.setClipHeight(((ScrollWidget) widget).getMainAreaHeight());
        } else if (widget instanceof PagerWidget) {
            selection.setClipHeight(((PagerWidget) widget).getMainAreaHeight());
        }

        // Запускаем обновление координат в PagerWidget
        if (widget instanceof PagerWidget) {
            ((PagerWidget) widget).updateOverlays();
        }

        // Обновляем SelectionView
        if (selection.getChildCount() > 0) {
            try {
                selection.update();
                Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: selection updated successfully");
            } catch (Exception e) {
                Timber.tag("voronin").e(e, "updateSelectionAfterFullscreenChange: error updating selection");
            }
        }
    }

    public void linksClose() {
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).linksClose();
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).linksClose();
    }

    public void bookmarksClose() {
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).bookmarksClose();
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).bookmarksClose();
    }

    public void bookmarksUpdate() {
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLBookmark.class);
            ArrayList<ZLTextHighlighting> hi = new ArrayList<>();
            if (book.info.bookmarks != null) {
                for (int i = 0; i < book.info.bookmarks.size(); i++) {
                    final Storage.Bookmark b = book.info.bookmarks.get(i);
                    ZLBookmark h = new ZLBookmark(app.BookTextView, b);
                    hi.add(h);
                }
            }
            app.BookTextView.addHighlightings(hi);
        }
        if (widget instanceof ScrollWidget) {
            if (pluginview == null) {
                for (ScrollWidget.ScrollAdapter.PageHolder h : ((ScrollWidget) widget).adapter.holders) {
                    h.page.recycle();
                    h.page.invalidate();
                }
            } else {
                ((ScrollWidget) widget).bookmarksUpdate();
            }
        }
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).updateOverlaysReset();
    }

    public void ttsClose() {
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).ttsClose();
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).ttsClose();
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLTTSMark.class);
            if (widget instanceof ScrollWidget) {
                for (ScrollWidget.ScrollAdapter.PageHolder h : ((ScrollWidget) widget).adapter.holders) {
                    h.page.recycle();
                    h.page.invalidate();
                }
            }
        }
    }

    public void ttsUpdate() {
        if (tts == null)
            return; // already closed, run from handler
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLTTSMark.class);
            if (tts != null) {
                ArrayList<ZLTextHighlighting> hi = new ArrayList<>();
                for (Storage.Bookmark m : tts.marks) {
                    ZLTTSMark h = new ZLTTSMark(app.BookTextView, m);
                    hi.add(h);
                }
                app.BookTextView.addHighlightings(hi);
            }
        }
        if (widget instanceof ScrollWidget) {
            if (pluginview == null) {
                for (ScrollWidget.ScrollAdapter.PageHolder h : ((ScrollWidget) widget).adapter.holders) {
                    h.page.recycle();
                    h.page.invalidate();
                }
            } else {
                ((ScrollWidget) widget).ttsUpdate();
            }
        }
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).updateOverlaysReset();
        tts.view.bringToFront();
    }

    public void ttsOpen() {
        tts = new TTSPopup(this);
        tts.show();
        ttsUpdate();
    }

    public void searchClose() {
        app.hideActivePopup();
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).searchClose();
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).searchClose();
        if (search != null) {
            search.close();
            search = null;
        }
        searchPagePending = -1;
        searchCurrentIndex = 0;
    }

    /**
     * Perform search without ProgressDialog, results returned via callback.
     * @param pattern search query
     * @param callback callback with (count, currentIndex)
     */
    public void performSearch(String pattern, SearchCallback callback) {
        if (pattern == null || pattern.isEmpty()) {
            callback.onResult(0, 0);
            return;
        }

        app.hideActivePopup();
        config.setValue(app.MiscOptions.TextSearchPattern, pattern);

        if (pluginview != null) {
            searchClose();
            search = pluginview.search(pattern);
            search.setPage(getPosition().getParagraphIndex());
            int count = search.getCount();
            searchCurrentIndex = 0;
            Timber.tag("voronin").d("performSearch: pattern=%s, count=%d, currentPage=%d", pattern, count, getPosition().getParagraphIndex());
            if (count > 0) {
                if (widget instanceof ScrollWidget)
                    ((ScrollWidget) widget).updateOverlays();
                if (widget instanceof PagerWidget)
                    ((PagerWidget) widget).updateOverlaysReset();
            }
            callback.onResult(count, searchCurrentIndex);
        } else {
            int count = app.getTextView().search(pattern, true, false, false, false);
            searchCurrentIndex = 0;
            if (count > 0 && widget instanceof ScrollWidget) {
                post(() -> reset());
            }
            callback.onResult(count, searchCurrentIndex);
        }
    }

    /**
     * Navigate to next search result, results returned via callback.
     */
    public void performSearchNext(SearchCallback callback) {
        hideKeyboard();
        if (search != null) {
            int count = search.getCount();
            int page = search.next();
            if (page != -1 && searchCurrentIndex < count - 1) {
                searchCurrentIndex++;
                post(() -> {
                    if (widget instanceof ScrollWidget) {
                        ((ScrollWidget) widget).searchPage(page);
                    } else if (widget instanceof PagerWidget) {
                        ((PagerWidget) widget).searchPage(page);
                        ((PagerWidget) widget).updateOverlaysReset(); // Refresh highlights
                    }
                });
            }
            callback.onResult(count, searchCurrentIndex);
        } else {
            app.BookTextView.findNext();
            callback.onResult(0, 0);
        }
    }

    /**
     * Navigate to previous search result, results returned via callback.
     */
    public void performSearchPrevious(SearchCallback callback) {
        hideKeyboard();
        if (search != null) {
            int count = search.getCount();
            int page = search.prev();
            if (page != -1 && searchCurrentIndex > 0) {
                searchCurrentIndex--;
                post(() -> {
                    if (widget instanceof ScrollWidget) {
                        ((ScrollWidget) widget).searchPage(page);
                    } else if (widget instanceof PagerWidget) {
                        ((PagerWidget) widget).searchPage(page);
                        ((PagerWidget) widget).updateOverlaysReset(); // Refresh highlights
                    }
                });
            }
            callback.onResult(count, searchCurrentIndex);
        } else {
            app.BookTextView.findPrevious();
            callback.onResult(0, 0);
        }
    }

    private void hideKeyboard() {
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    /**
     * Callback interface for search results.
     */
    public interface SearchCallback {
        void onResult(int count, int currentIndex);
    }

    public void overlaysClose() {
        selectionClose();
        linksClose();
        bookmarksClose();
        searchClose();
    }

    /**
     * Check if currently in zoom mode
     */
    public boolean isZoom() {
        if (widget instanceof ScrollWidget)
            return ((ScrollWidget) widget).gesturesListener.getZoomHandler().isInZoom();
        if (widget instanceof PagerWidget)
            return ((PagerWidget) widget).getZoomHandler().isInZoom();
        return false;
    }

    /**
     * Reset zoom to normal (1.0)
     */
    public void resetZoom() {
        if (widget instanceof ScrollWidget)
            ((ScrollWidget) widget).gesturesListener.getZoomHandler().resetZoom();
        if (widget instanceof PagerWidget)
            ((PagerWidget) widget).getZoomHandler().resetZoom();
    }

    /**
     * Get current zoom scale
     */
    public float getZoomScale() {
        if (widget instanceof ScrollWidget)
            return ((ScrollWidget) widget).gesturesListener.getZoomHandler().getCurrentZoom();
        if (widget instanceof PagerWidget)
            return ((PagerWidget) widget).getZoomHandler().getCurrentZoom();
        return 1.0f;
    }

    /**
     * Get zoom touch adapter for coordinate adaptation
     */
    public ZoomTouchAdapter getZoomTouchAdapter() {
        return new ZoomTouchAdapter(this);
    }

    public void showControls() {
        ActiveAreasView areas = new ActiveAreasView(getContext());
        int w = getWidth();
        if (w == 0)
            return; // activity closed
        areas.create(app, w);
        showControls(this, areas);
    }

    /**
     * Exit fullscreen mode - restore normal system UI visibility
     * Uses modern WindowInsetsController API to avoid flickering.
     */
    public void exitFullscreen() {
        if (w != null && isFullscreenMode) {
            WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(w, w.getDecorView());
            controller.show(WindowInsetsCompat.Type.systemBars());
            isFullscreenMode = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
    }

    public boolean isReflow() {
        return pluginview.reflow;
    }

    public void setReflow(boolean b) {
        pluginview.reflow = b;
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
    }

    /**
     * Проверяет возможность смены шрифта.
     * Шрифт можно менять для:
     * - FB2/EPUB/MOBI (pluginview == null)
     * - PDF/DJVU в режиме reflow (pluginview != null && pluginview.reflow = true)
     */
    public boolean canChangeFont() {
        if (pluginview == null) {
            // Текстовые форматы (FB2/EPUB/MOBI) - шрифт можно менять всегда
            return true;
        }
        // PDF/DJVU - шрифт можно менять только в режиме reflow
        return pluginview.reflow;
    }

    public int getFontsizeFB() {
        // Возвращаем значение в исходных единицах (без DPI) для UI
        int dpiValue;
        if (book.info.fontsize != null)
            dpiValue = book.info.fontsize;
        else
            dpiValue = app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue();
        // Делим на DPI чтобы получить исходное значение
        return dpiValue * 160 / ZLibrary.Instance().getDisplayDPI();
    }

    public void setFontsizeFB(int p) {
        // Умножаем на DPI чтобы значение было в тех же единицах как FontSizeOption
        // FontSizeOption создаётся с fontSize * DPI / 160 в ZLTextBaseStyle
        int dpiScaled = p * ZLibrary.Instance().getDisplayDPI() / 160;
        book.info.fontsize = dpiScaled;
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption, dpiScaled);
        // Сохраняем позицию перед reset, как в setFontsizeReflow
        ZLTextPosition scrollDelayed = getPosition();
        resetCaches();
        gotoPosition(scrollDelayed);
    }

    public boolean getIgnoreCssFonts() {
        return !app.ViewOptions.getTextStyleCollection().getBaseStyle().UseCSSFontFamilyOption.getValue();
    }

    public void setIgnoreCssFonts(boolean b) {
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().UseCSSFontFamilyOption, !b);
        // Сохраняем позицию перед reset
        ZLTextPosition scrollDelayed = getPosition();
        resetCaches();
        gotoPosition(scrollDelayed);
    }

    public void setFontFB(String f) {
        config.setValue(app.ViewOptions.getTextStyleCollection().getBaseStyle().FontFamilyOption, f);
        // Сохраняем позицию перед reset
        ZLTextPosition scrollDelayed = getPosition();
        resetCaches();
        gotoPosition(scrollDelayed);
    }

    public Float getFontsizeReflow() {
        if (book.info.fontsize != null)
            return book.info.fontsize / 100f;
        return null;
    }

    public void setFontsizeReflow(float p) {
        book.info.fontsize = (int) (p * 100f);
        if (pluginview.reflower != null && pluginview.reflower.k2 != null)
            pluginview.reflower.k2.setFontSize(p);
        ZLTextPosition scrollDelayed = getPosition();
        reset();
        gotoPosition(scrollDelayed);
    }

    public void onScrollingFinished(ZLViewEnums.PageIndex pageIndex) {
        if (listener != null)
            listener.onScrollingFinished(pageIndex);
        if (tts != null)
            tts.onScrollingFinished(pageIndex);
    }

    public enum Widgets {PAGING, CONTINUOUS}

    public interface Listener {
        void onScrollingFinished(ZLViewEnums.PageIndex index);

        void onSearchClose();

        void onBookmarksUpdate();

        void onDismissDialog();

        void ttsStatus(boolean speaking);

        void onEditBookmark(Storage.Bookmark bookmark);

        void onFullscreenToggle(boolean isFullscreen);

        void onNavigationRequest();

        void onSelectionShow(int startY, int endY);

        void onSelectionHide();

        /**
         * Called when zoom scale changes.
         * @param scale The new zoom scale (1.0 = normal, >1.0 = zoomed in)
         * @param pivotX The X pivot point for zoom
         * @param pivotY The Y pivot point for zoom
         */
        void onZoomChange(float scale, float pivotX, float pivotY);

        /**
         * Called when zoom mode ends (scale returns to 1.0)
         */
        void onZoomEnd();
    }

    public static class ZLTextIndexPosition extends com.github.axet.bookreader.widgets.ZLTextIndexPosition {
        public static final Parcelable.Creator<ZLTextIndexPosition> CREATOR = new Parcelable.Creator<ZLTextIndexPosition>() {
            public ZLTextIndexPosition createFromParcel(Parcel in) {
                return new ZLTextIndexPosition(in);
            }

            public ZLTextIndexPosition[] newArray(int size) {
                return new ZLTextIndexPosition[size];
            }
        };

        public ZLTextIndexPosition(ZLTextPosition p, ZLTextPosition e) {
            super(p, e);
        }

        public ZLTextIndexPosition(Parcel in) {
            super(in);
        }
    }

    public static class ZLBookmark extends com.github.axet.bookreader.widgets.ZLBookmark {
        public ZLBookmark(FBView view, Storage.Bookmark b) {
            super(view, b);
        }
    }

    public static class ZLTTSMark extends com.github.axet.bookreader.widgets.ZLTTSMark {
        public ZLTTSMark(FBView view, Storage.Bookmark m) {
            super(view, m);
        }
    }

    public static class ConfigShadow extends Config { // disable config changes across this app view instancies and fbreader
        public Map<String, String> map = new TreeMap<>();

        public ConfigShadow() {
        }

        public void setValue(ZLOption opt, int i) {
            apply(opt);
            setValue(opt.myId, String.valueOf(i));
        }

        public void setValue(ZLOption opt, boolean b) {
            apply(opt);
            setValue(opt.myId, String.valueOf(b));
        }

        public void setValue(ZLOption opt, Enum v) {
            apply(opt);
            setValue(opt.myId, String.valueOf(v));
        }

        public void setValue(ZLOption opt, String v) {
            apply(opt);
            setValue(opt.myId, v);
        }

        public void apply(ZLOption opt) {
            opt.Config = new ZLOption.ConfigInstance() {
                public Config Instance() {
                    return ConfigShadow.this;
                }
            };
        }

        @Override
        public String getValue(StringPair id, String defaultValue) {
            String v = map.get(id.Group + ":" + id.Name);
            if (v != null)
                return v;
            return super.getValue(id, defaultValue);
        }

        @Override
        public void setValue(StringPair id, String value) {
            map.put(id.Group + ":" + id.Name, value);
        }

        @Override
        public void unsetValue(StringPair id) {
            map.remove(id.Group + ":" + id.Name);
        }

        @Override
        protected void setValueInternal(String group, String name, String value) {
        }

        @Override
        protected void unsetValueInternal(String group, String name) {
        }

        @Override
        protected Map<String, String> requestAllValuesForGroupInternal(String group) throws NotAvailableException {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void runOnConnect(Runnable runnable) {
        }

        @Override
        public List<String> listGroups() {
            return null;
        }

        @Override
        public List<String> listNames(String group) {
            return null;
        }

        @Override
        public void removeGroup(String name) {
        }

        @Override
        public boolean getSpecialBooleanValue(String name, boolean defaultValue) {
            return false;
        }

        @Override
        public void setSpecialBooleanValue(String name, boolean value) {
        }

        @Override
        public String getSpecialStringValue(String name, String defaultValue) {
            return null;
        }

        @Override
        public void setSpecialStringValue(String name, String value) {
        }

        @Override
        protected String getValueInternal(String group, String name) throws NotAvailableException {
            throw new NotAvailableException("default");
        }
    }

    public static class BrightnessGesture extends com.github.axet.bookreader.widgets.BrightnessGesture {
        public BrightnessGesture(FBReaderView view) {
            super(view);
        }
    }

    public static class LinksView {
        public ArrayList<View> links = new ArrayList<>();
        FBReaderView fb;

        public LinksView(final FBReaderView view, Plugin.View.Link[] ll, Reflow.Info info) {
            this.fb = view;
            if (ll == null)
                return;
            for (int i = 0; i < ll.length; i++) {
                final Plugin.View.Link l = ll[i];
                Rect[] rr;
                if (fb.pluginview.reflow) {
                    rr = fb.pluginview.boundsUpdate(new Rect[]{l.rect}, info);
                    if (rr.length == 0)
                        continue;
                } else {
                    rr = new Rect[]{l.rect};
                }
                for (Rect r : rr) {
                    MarginLayoutParams lp = new MarginLayoutParams(r.width(), r.height());
                    View v = new View(fb.getContext());
                    v.setLayoutParams(lp);
                    v.setTag(r);
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (l.index != -1)
                                fb.app.runAction(ActionCode.PROCESS_HYPERLINK, new BookModel.Label(null, l.index));
                            else
                                AboutPreferenceCompat.openUrlDialog(fb.getContext(), l.url);
                        }
                    });
                    links.add(v);
                    fb.addView(v);
                }
            }
        }

        public void update(int x, int y) {
            for (View v : links) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left;
                lp.topMargin = y + l.top;
                lp.width = l.width();
                lp.height = l.height();
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : links)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : links)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(links);
            // can be called during RelativeLayout onLayout
            fb.post(() -> {
                for (View v : old)
                    fb.removeView(v);
            });
            links.clear();
        }
    }

    public static class BookmarksView {
        public ArrayList<View> bookmarks = new ArrayList<>();
        FBReaderView fb;
        int clip;

        public BookmarksView(final FBReaderView view, Plugin.View.Selection.Page page, Storage.Bookmarks bms, Reflow.Info info) {
            this.fb = view;
            if (fb.widget instanceof ScrollWidget)
                clip = ((ScrollWidget) fb.widget).getMainAreaHeight();
            else
                clip = ((ZLAndroidWidget) fb.widget).getMainAreaHeight();
            if (bms == null)
                return;
            ArrayList<Storage.Bookmark> ll = bms.getBookmarks(page);
            if (ll == null)
                return;
            for (int i = 0; i < ll.size(); i++) {
                final ArrayList<View> bmv = new ArrayList<>();
                final Storage.Bookmark l = ll.get(i);
                Plugin.View.Selection s = fb.pluginview.select(l.start, l.end);
                if (s == null)
                    return;
                Plugin.View.Selection.Bounds bb = s.getBounds(page);
                s.close();
                Rect[] rr;
                if (fb.pluginview.reflow)
                    rr = fb.pluginview.boundsUpdate(bb.rr, info);
                else
                    rr = bb.rr;
                List<Rect> kk;
                kk = SelectionView.lines(rr);
                for (Rect r : kk) {
                    MarginLayoutParams lp = new MarginLayoutParams(r.width(), r.height());
                    WordView v = new WordView(fb.getContext());
                    v.setLayoutParams(lp);
                    v.setTag(r);
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Открываем BottomSheet через listener
                            if (fb.listener != null) {
                                fb.listener.onEditBookmark(l);
                            }
                        }
                    });
                    int color = l.color == 0 ? fb.app.BookTextView.getHighlightingBackgroundColor().intValue() : l.color;
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | (color & 0xffffff));
                    bmv.add(v);
                    addView(v);
                }
            }
        }

        public void addView(View v) {
            bookmarks.add(v);
            fb.addView(v);
        }

        public void update(int x, int y) {
            for (View v : bookmarks) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left;
                lp.topMargin = y + l.top;
                lp.width = l.width();
                lp.height = l.height();
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : bookmarks)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : bookmarks)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(bookmarks); // can be called during RelativeLayout onLayout
            fb.post(() -> {
                for (View v : old)
                    fb.removeView(v);
            });
            bookmarks.clear();
        }

        public class WordView extends View {
            public WordView(Context context) {
                super(context);
            }

            @Override
            public void draw(Canvas canvas) {
                Rect c = canvas.getClipBounds();
                c.bottom = clip - getTop();
                canvas.clipRect(c);
                super.draw(canvas);
            }
        }
    }

    public static class TTSView extends BookmarksView {
        public TTSView(final FBReaderView view, Plugin.View.Selection.Page page, Reflow.Info info) {
            super(view, page, view.tts.marks, info);
        }

        @Override
        public void addView(View v) {
            super.addView(v);
            v.setOnClickListener(null);
        }
    }

    public static class SearchView {
        public ArrayList<View> words = new ArrayList<>();
        FBReaderView fb;
        int padding;
        int clip;

        @SuppressWarnings("unchecked")
        public SearchView(FBReaderView view, Plugin.View.Search.Bounds bb, Reflow.Info info) {
            this.fb = view;
            if (fb.widget instanceof ScrollWidget)
                clip = ((ScrollWidget) fb.widget).getMainAreaHeight();
            else
                clip = ((ZLAndroidWidget) fb.widget).getMainAreaHeight();
            padding = ThemeUtils.dp2px(fb.getContext(), SelectionView.SELECTION_PADDING);
            if (bb == null || bb.rr == null)
                return;
            if (fb.pluginview.reflow)
                bb.rr = fb.pluginview.boundsUpdate(bb.rr, info);
            HashSet hh = null;
            if (bb.highlight != null) {
                if (fb.pluginview.reflow)
                    hh = new HashSet(Arrays.asList(fb.pluginview.boundsUpdate(bb.highlight, info)));
                else
                    hh = new HashSet(Arrays.asList(bb.highlight));
            }
            for (int i = 0; i < bb.rr.length; i++) {
                final Rect l = bb.rr[i];
                MarginLayoutParams lp = new MarginLayoutParams(l.width(), l.height());
                WordView v = new WordView(fb.getContext());
                v.setLayoutParams(lp);
                v.setTag(l);
                if (hh != null && hh.contains(l))
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | 0x00AA00); // Green for current match
                else
                    v.setBackgroundColor(SelectionView.SELECTION_ALPHA << 24 | fb.app.BookTextView.getHighlightingBackgroundColor().intValue());
                words.add(v);
                fb.addView(v);
            }
        }

        public void update(int x, int y) {
            for (View v : words) {
                Rect l = (Rect) v.getTag();
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                lp.leftMargin = x + l.left - padding;
                lp.topMargin = y + l.top - padding;
                lp.width = l.width() + 2 * padding;
                lp.height = l.height() + 2 * padding;
                v.requestLayout();
            }
        }

        public void hide() {
            for (View v : words)
                v.setVisibility(GONE);
        }

        public void show() {
            for (View v : words)
                v.setVisibility(VISIBLE);
        }

        public void close() {
            final ArrayList<View> old = new ArrayList<>(words);
            // can be called during RelativeLayout onLayout
            fb.post(() -> {
                for (View v : old)
                    fb.removeView(v);
            });
            words.clear();
        }

        public class WordView extends View {
            public WordView(Context context) {
                super(context);
            }

            @Override
            public void draw(Canvas canvas) {
                Rect c = canvas.getClipBounds();
                c.bottom = clip - getTop();
                canvas.clipRect(c);
                super.draw(canvas);
            }
        }
    }

    public class CustomView extends FBView {

        public CustomView(FBReaderApp reader) {
            super(reader);
        }

        public ZLAndroidPaintContext createContext(Canvas c) {
            return new ZLAndroidPaintContext(
                    app.SystemInfo,
                    c,
                    new ZLAndroidPaintContext.Geometry(
                            getWidth(),
                            getHeight(),
                            getWidth(),
                            getHeight(),
                            0,
                            0
                    ),
                    getVerticalScrollbarWidth()
            );
        }

        public Footer getFooter() {
            int type = SCROLLBAR_SHOW_AS_FOOTER; // app.ViewOptions.ScrollbarType.getValue();
            if (type == SCROLLBAR_SHOW_AS_FOOTER)
                return new FooterNew();
            if (type == SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE)
                return new FooterOld();
            return null;
        }

        public ZLAndroidPaintContext setContext() {
            ZLAndroidPaintContext context = createContext(new Canvas());
            setContext(context);
            return context;
        }

        @Override
        public Animation getAnimationType() {
            PowerManager pm = (PowerManager) FBReaderView.this.getContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isPowerSaveMode())
                return Animation.none;
            else
                return super.getAnimationType();
        }

        public void setScalingType(ZLTextImageElement imageElement, ZLPaintContext.ScalingType s) {
            book.info.scales.put(imageElement.Id, s);
        }

        @Override
        protected ZLPaintContext.ScalingType getScalingType(ZLTextImageElement imageElement) {
            ZLPaintContext.ScalingType s = book.info.scales.get(imageElement.Id);
            if (s != null)
                return s;
            return super.getScalingType(imageElement);
        }

        @Override
        public void hideOutline() {
            super.hideOutline();
            if (widget instanceof ScrollWidget) {
                ((ScrollWidget) widget).adapter.processInvalidate();
                ((ScrollWidget) widget).adapter.processClear();
            }
        }

        @Override
        protected ZLTextRegion findRegion(int x, int y, int maxDistance, ZLTextRegion.Filter filter) {
            return super.findRegion(x, y, maxDistance, filter);
        }

        @Override
        public void onFingerSingleTap(int x, int y) {
            final ZLTextHighlighting highlighting = findHighlighting(x, y, maxSelectionDistance());
            if (highlighting instanceof ZLBookmark) {
                app.runAction(ActionCode.SELECTION_BOOKMARK, ((ZLBookmark) highlighting).getB());
                return;
            }
            super.onFingerSingleTap(x, y);
        }

        @Override
        public void onFingerSingleTapLastResort(int x, int y) {
            if (widget instanceof ScrollWidget)
                onFingerSingleTapLastResort(((ScrollWidget) widget).gesturesListener.e);
            else
                super.onFingerSingleTapLastResort(x, y);
        }

        public void onFingerSingleTapLastResort(MotionEvent e) {
            setContext();
            super.onFingerSingleTapLastResort((int) e.getX(), (int) e.getY());
        }

        @Override
        public boolean twoColumnView() {
            if (widget instanceof ScrollWidget)
                return false;
            return super.twoColumnView();
        }

        @Override
        public boolean canScroll(PageIndex index) {
            if (pluginview != null)
                return pluginview.canScroll(index);
            else
                return super.canScroll(index);
        }

        @Override
        public synchronized void onScrollingFinished(PageIndex pageIndex) {
            if (pluginview != null) {
                if (pluginview.onScrollingFinished(pageIndex))
                    widget.reset();
            } else {
                super.onScrollingFinished(pageIndex);
            }
            FBReaderView.this.onScrollingFinished(pageIndex);
            if (widget instanceof ZLAndroidWidget)
                ((PagerWidget) widget).updateOverlays();
        }

        @Override
        public synchronized PagePosition pagePosition() { // Footer draw
            if (pluginview != null)
                return pluginview.pagePosition();
            else
                return super.pagePosition();
        }

        @Override
        public void gotoHome() {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(0, 0, 0));
            else
                super.gotoHome();
            resetNewPosition();
        }

        @Override
        public synchronized void gotoPage(int page) {
            if (pluginview != null)
                pluginview.gotoPosition(new ZLTextFixedPosition(page - 1, 0, 0));
            else
                super.gotoPage(page);
            resetNewPosition();
        }

        @Override
        public synchronized void paint(ZLPaintContext context, PageIndex pageIndex) {
            super.paint(context, pageIndex);
        }

        @Override
        public int getSelectionStartY() {
            if (selection != null)
                return selection.getSelectionStartY();
            return super.getSelectionStartY();
        }

        @Override
        public int getSelectionEndY() {
            if (selection != null)
                return selection.getSelectionEndY();
            return super.getSelectionEndY();
        }

        public class FooterNew extends FooterNewStyle {
            @Override
            protected String buildInfoString(PagePosition pagePosition, String separator) {
                return "";
            }
        }

        public class FooterOld extends FooterOldStyle {
            @Override
            protected String buildInfoString(PagePosition pagePosition, String separator) {
                return "";
            }
        }
    }

    public class FBApplicationWindow implements ZLApplicationWindow {
        @Override
        public void setWindowTitle(String title) {
            FBReaderView.this.title = title;
        }

        @Override
        public void showErrorMessage(String resourceKey) {
        }

        @Override
        public void showErrorMessage(String resourceKey, String parameter) {
        }

        @Override
        public ZLApplication.SynchronousExecutor createExecutor(String key) {
            return null;
        }

        @Override
        public void processException(Exception e) {
        }

        @Override
        public void refresh() {
            if (widget instanceof PagerWidget)
                ((PagerWidget) widget).updateOverlays();
        }

        @Override
        public ZLViewWidget getViewWidget() {
            return widget;
        }

        @Override
        public void close() {
        }

        @Override
        public int getBatteryLevel() {
            return battery;
        }
    }

    public class FBReaderApp extends org.geometerplus.fbreader.fbreader.FBReaderApp {
        public FBReaderApp(Context context) {
            super(new Storage.Info(context), new BookCollectionShadow());
        }

        @Override
        public TOCTree getCurrentTOCElement() {
            if (Model == null)
                return null;
            if (pluginview != null)
                return pluginview.getCurrentTOCElement(Model.TOCTree);
            else
                return super.getCurrentTOCElement();
        }
    }
}
