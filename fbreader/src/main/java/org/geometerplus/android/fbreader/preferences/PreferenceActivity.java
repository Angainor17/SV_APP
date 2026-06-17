/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.preferences;

import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext;
import org.geometerplus.android.fbreader.preferences.background.BackgroundPreference;
import org.geometerplus.android.fbreader.preferences.fileChooser.FileChooserCollection;
import org.geometerplus.android.fbreader.sync.SyncOperations;
import org.geometerplus.android.util.DeviceType;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.fbreader.fbreader.options.EInkOptions;
import org.geometerplus.fbreader.fbreader.options.FooterOptions;
import org.geometerplus.fbreader.fbreader.options.ImageOptions;
import org.geometerplus.fbreader.fbreader.options.MiscOptions;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.fbreader.fbreader.options.SyncOptions;
import org.geometerplus.fbreader.fbreader.options.ViewOptions;
import org.geometerplus.fbreader.network.sync.SyncData;
import org.geometerplus.fbreader.network.sync.SyncUtil;
import org.geometerplus.fbreader.tips.TipsManager;
import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.language.Language;
import org.geometerplus.zlibrary.core.network.JsonRequest;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.text.view.style.ZLTextBaseStyle;
import org.geometerplus.zlibrary.text.view.style.ZLTextNGStyleDescription;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PreferenceActivity extends ZLPreferenceActivity {
    private static final int BACKGROUND_REQUEST_CODE = 3000;
    private final ActivityNetworkContext myNetworkContext = new ActivityNetworkContext(this);
    private final FileChooserCollection myChooserCollection = new FileChooserCollection(this, 2000);
    private BackgroundPreference myBackgroundPreference;

    public PreferenceActivity() {
        super("Preferences");
    }

    @Override
    protected void onResume() {
        super.onResume();
        myNetworkContext.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (myNetworkContext.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (BACKGROUND_REQUEST_CODE == requestCode) {
            if (myBackgroundPreference != null) {
                myBackgroundPreference.update(data);
            }
            return;
        }

        myChooserCollection.update(requestCode, data);
    }

    @Override
    protected void init(Intent intent) {
        final Config config = Config.Instance();
        config.requestAllValuesForGroup("Style");
        config.requestAllValuesForGroup("Options");
        config.requestAllValuesForGroup("LookNFeel");
        config.requestAllValuesForGroup("Fonts");
        config.requestAllValuesForGroup("Files");
        config.requestAllValuesForGroup("Scrolling");
        config.requestAllValuesForGroup("Colors");
        config.requestAllValuesForGroup("Sync");
        setResult(FBReader.RESULT_REPAINT);

        final ViewOptions viewOptions = new ViewOptions();
        final MiscOptions miscOptions = new MiscOptions();
        final FooterOptions footerOptions = viewOptions.getFooterOptions();
        final PageTurningOptions pageTurningOptions = new PageTurningOptions();
        final ImageOptions imageOptions = new ImageOptions();
        final SyncOptions syncOptions = new SyncOptions();
        final ColorProfile profile = viewOptions.getColorProfile();
        final ZLTextStyleCollection collection = viewOptions.getTextStyleCollection();
        final ZLKeyBindings keyBindings = new ZLKeyBindings();

        final ZLAndroidLibrary androidLibrary = (ZLAndroidLibrary) ZLAndroidLibrary.Instance();
        // TODO: use user-defined locale, not the default one,
        // or set user-defined locale as default
        final String decimalSeparator =
                String.valueOf(new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator());

        final Screen directoriesScreen = createPreferenceScreen("directories");
        final Runnable libraryUpdater = new Runnable() {
            public void run() {
                final BookCollectionShadow bookCollection = new BookCollectionShadow();
                bookCollection.bindToService(PreferenceActivity.this, new Runnable() {
                    public void run() {
                        bookCollection.reset(false);
                        bookCollection.unbind();
                    }
                });
            }
        };
        directoriesScreen.addPreference(myChooserCollection.createPreference(
                directoriesScreen.Resource, "bookPath", Paths.bookPathOption, libraryUpdater
        ));
        directoriesScreen.addPreference(myChooserCollection.createPreference(
                directoriesScreen.Resource, "downloadDir", Paths.downloadsDirectoryOption, libraryUpdater
        ));
        final PreferenceSet fontReloader = new PreferenceSet.Reloader();
        directoriesScreen.addPreference(myChooserCollection.createPreference(
                directoriesScreen.Resource, "fontPath", Paths.fontPathOption, fontReloader
        ));
        directoriesScreen.addPreference(myChooserCollection.createPreference(
                directoriesScreen.Resource, "tempDir", Paths.TempDirectoryOption(this), null
        ));

        final Screen syncScreen = createPreferenceScreen("sync");
        final PreferenceSet syncPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                return syncOptions.getEnabled().getValue();
            }
        };
        syncScreen.addPreference(new UrlPreference(this, syncScreen.Resource, "site"));
        syncScreen.addPreference(new ZLCheckBoxPreference(
                this, syncScreen.Resource.getResource("enable")
        ) {
            {
                if (syncOptions.getEnabled().getValue()) {
                    setChecked(true);
                    setOnSummary(SyncUtil.getAccountName(myNetworkContext));
                } else {
                    setChecked(false);
                }
            }

            private void enableSynchronisation() {
                SyncOperations.enableSync(PreferenceActivity.this, syncOptions);
            }

            @Override
            protected void onClick() {
                super.onClick();
                syncPreferences.run();

                if (!isChecked()) {
                    SyncUtil.logout(myNetworkContext);
                    syncOptions.getEnabled().setValue(false);
                    enableSynchronisation();
                    syncPreferences.run();
                    new SyncData().reset();
                    return;
                }

                UIUtil.createExecutor(PreferenceActivity.this, "tryConnect").execute(new Runnable() {
                    public void run() {
                        try {
                            myNetworkContext.perform(
                                    new JsonRequest(SyncOptions.BASE_URL + "login/test") {
                                        @Override
                                        public void processResponse(Object response) {
                                            final String account = (String) ((Map) response).get("user");
                                            syncOptions.getEnabled().setValue(account != null);
                                            enableSynchronisation();
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    setOnSummary(account);
                                                    syncPreferences.run();
                                                }
                                            });
                                        }
                                    }
                            );
                        } catch (ZLNetworkException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    setChecked(false);
                                }
                            });
                        }
                    }
                }, null);
            }

            private void setOnSummary(String account) {
                final String summary = account != null
                        ? Resource.getResource("summaryOnWithAccount").getValue().replace("%s", account)
                        : Resource.getResource("summaryOn").getValue();
                runOnUiThread(new Runnable() {
                    public void run() {
                        setSummaryOn(summary);
                    }
                });
            }
        });
        syncPreferences.add(syncScreen.addOption(syncOptions.getUploadAllBooks(), "uploadAllBooks", "values"));
        syncPreferences.add(syncScreen.addOption(syncOptions.getPositions(), "positions", "values"));
        syncPreferences.add(syncScreen.addOption(syncOptions.getChangeCurrentBook(), "changeCurrentBook"));
        //syncPreferences.add(syncScreen.addOption(syncOptions.Metainfo, "metainfo", "values"));
        syncPreferences.add(syncScreen.addOption(syncOptions.getBookmarks(), "bookmarks", "values"));
        syncPreferences.run();

        final Screen appearanceScreen = createPreferenceScreen("appearance");
        appearanceScreen.addPreference(new LanguagePreference(
                this, appearanceScreen.Resource.getResource("language"), ZLResource.interfaceLanguages()
        ) {
            @Override
            protected void init() {
                setInitialValue(ZLResource.getLanguageOption().getValue());
            }

            @Override
            protected void setLanguage(String code) {
                final ZLStringOption languageOption = ZLResource.getLanguageOption();
                if (!code.equals(languageOption.getValue())) {
                    languageOption.setValue(code);
                    finish();
                    startActivity(new Intent(
                            Intent.ACTION_VIEW, Uri.parse("fbreader-action:preferences#appearance")
                    ));
                }
            }
        });
        appearanceScreen.addPreference(new ZLStringChoicePreference(
                this, appearanceScreen.Resource.getResource("screenOrientation"),
                androidLibrary.getOrientationOption(), androidLibrary.allOrientations()
        ));
        appearanceScreen.addPreference(new ZLBooleanPreference(
                this,
                viewOptions.twoColumnView,
                appearanceScreen.Resource.getResource("twoColumnView")
        ));
        appearanceScreen.addPreference(new ZLBooleanPreference(
                this,
                miscOptions.allowScreenBrightnessAdjustment,
                appearanceScreen.Resource.getResource("allowScreenBrightnessAdjustment")
        ) {
            private final int myLevel = androidLibrary.ScreenBrightnessLevelOption.getValue();

            @Override
            protected void onClick() {
                super.onClick();
                androidLibrary.ScreenBrightnessLevelOption.setValue(isChecked() ? myLevel : 0);
            }
        });
        appearanceScreen.addPreference(new BatteryLevelToTurnScreenOffPreference(
                this,
                androidLibrary.BatteryLevelToTurnScreenOffOption,
                appearanceScreen.Resource.getResource("dontTurnScreenOff")
        ));
		/*
		appearanceScreen.addPreference(new ZLBooleanPreference(
			this,
			androidLibrary.DontTurnScreenOffDuringChargingOption,
			appearanceScreen.Resource.getResource("dontTurnScreenOffDuringCharging")
		));
		 */
        appearanceScreen.addOption(androidLibrary.ShowStatusBarOption, "showStatusBar");
        appearanceScreen.addOption(androidLibrary.DisableButtonLightsOption, "disableButtonLights");

        if (DeviceType.Instance().isEInk()) {
            final EInkOptions einkOptions = new EInkOptions();
            final Screen einkScreen = createPreferenceScreen("eink");
            final PreferenceSet einkPreferences = new PreferenceSet.Enabler() {
                @Override
                protected Boolean detectState() {
                    return einkOptions.EnableFastRefresh.getValue();
                }
            };

            einkScreen.addPreference(new ZLBooleanPreference(
                    this, einkOptions.EnableFastRefresh,
                    einkScreen.Resource.getResource("enableFastRefresh")
            ) {
                @Override
                protected void onClick() {
                    super.onClick();
                    einkPreferences.run();
                }
            });

            final ZLIntegerRangePreference updateIntervalPreference = new ZLIntegerRangePreference(
                    this, einkScreen.Resource.getResource("interval"), einkOptions.UpdateInterval
            );
            einkScreen.addPreference(updateIntervalPreference);

            einkPreferences.add(updateIntervalPreference);
            einkPreferences.run();
        }

        final Screen textScreen = createPreferenceScreen("text");

        final Screen fontPropertiesScreen = textScreen.createPreferenceScreen("fontProperties");
        fontPropertiesScreen.addOption(ZLAndroidPaintContext.AntiAliasOption, "antiAlias");
        fontPropertiesScreen.addOption(ZLAndroidPaintContext.DeviceKerningOption, "deviceKerning");
        fontPropertiesScreen.addOption(ZLAndroidPaintContext.DitheringOption, "dithering");
        fontPropertiesScreen.addOption(ZLAndroidPaintContext.SubpixelOption, "subpixel");

        final ZLTextBaseStyle baseStyle = collection.getBaseStyle();

        fontReloader.add(textScreen.addPreference(new FontPreference(
                this, textScreen.Resource.getResource("font"),
                baseStyle.FontFamilyOption, false
        )));
        textScreen.addPreference(new ZLIntegerRangePreference(
                this, textScreen.Resource.getResource("fontSize"),
                baseStyle.FontSizeOption
        ));
        textScreen.addPreference(new FontStylePreference(
                this, textScreen.Resource.getResource("fontStyle"),
                baseStyle.BoldOption, baseStyle.ItalicOption
        ));
        final ZLIntegerRangeOption spaceOption = baseStyle.LineSpaceOption;
        final String[] spacings = new String[spaceOption.MaxValue - spaceOption.MinValue + 1];
        for (int i = 0; i < spacings.length; ++i) {
            final int val = spaceOption.MinValue + i;
            spacings[i] = (char) (val / 10 + '0') + decimalSeparator + (char) (val % 10 + '0');
        }
        textScreen.addPreference(new ZLChoicePreference(
                this, textScreen.Resource.getResource("lineSpacing"),
                spaceOption, spacings
        ));
        final String[] alignments = {"left", "right", "center", "justify"};
        textScreen.addPreference(new ZLChoicePreference(
                this, textScreen.Resource.getResource("alignment"),
                baseStyle.AlignmentOption, alignments
        ));
        textScreen.addOption(baseStyle.AutoHyphenationOption, "autoHyphenations");

        final Screen moreStylesScreen = textScreen.createPreferenceScreen("more");
        for (ZLTextNGStyleDescription description : collection.getDescriptionList()) {
            final Screen ngScreen = moreStylesScreen.createPreferenceScreen(description.Name);
            ngScreen.addPreference(new FontPreference(
                    this, textScreen.Resource.getResource("font"),
                    description.FontFamilyOption, true
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.FontSizeOption,
                    StringPreference.Constraint.POSITIVE_LENGTH,
                    textScreen.Resource, "fontSize"
            ));
            ngScreen.addPreference(new ZLStringChoicePreference(
                    this, textScreen.Resource.getResource("bold"),
                    description.FontWeightOption,
                    new String[]{"inherit", "normal", "bold"}
            ));
            ngScreen.addPreference(new ZLStringChoicePreference(
                    this, textScreen.Resource.getResource("italic"),
                    description.FontStyleOption,
                    new String[]{"inherit", "normal", "italic"}
            ));
            ngScreen.addPreference(new ZLStringChoicePreference(
                    this, textScreen.Resource.getResource("textDecoration"),
                    description.TextDecorationOption,
                    new String[]{"inherit", "none", "underline", "line-through"}
            ));
            ngScreen.addPreference(new ZLStringChoicePreference(
                    this, textScreen.Resource.getResource("allowHyphenations"),
                    description.HyphenationOption,
                    new String[]{"inherit", "none", "auto"}
            ));
            ngScreen.addPreference(new ZLStringChoicePreference(
                    this, textScreen.Resource.getResource("alignment"),
                    description.AlignmentOption,
                    new String[]{"inherit", "left", "right", "center", "justify"}
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.LineHeightOption,
                    StringPreference.Constraint.PERCENT,
                    textScreen.Resource, "lineSpacing"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.MarginTopOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "spaceBefore"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.MarginBottomOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "spaceAfter"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.MarginLeftOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "leftIndent"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.MarginRightOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "rightIndent"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.TextIndentOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "firstLineIndent"
            ));
            ngScreen.addPreference(new StringPreference(
                    this, description.VerticalAlignOption,
                    StringPreference.Constraint.LENGTH,
                    textScreen.Resource, "verticalAlignment"
            ));
        }

        final Screen toastsScreen = createPreferenceScreen("toast");
        toastsScreen.addOption(miscOptions.toastFontSizePercent, "fontSizePercent");
        toastsScreen.addOption(miscOptions.showFootnoteToast, "showFootnoteToast");
        toastsScreen.addPreference(new ZLEnumPreference(
                this,
                miscOptions.footnoteToastDuration,
                toastsScreen.Resource.getResource("footnoteToastDuration"),
                ZLResource.resource("duration")
        ));

        final Screen cssScreen = createPreferenceScreen("css");
        cssScreen.addOption(baseStyle.UseCSSFontFamilyOption, "fontFamily");
        cssScreen.addOption(baseStyle.UseCSSFontSizeOption, "fontSize");
        cssScreen.addOption(baseStyle.UseCSSTextAlignmentOption, "textAlignment");
        cssScreen.addOption(baseStyle.UseCSSMarginsOption, "margins");

        final Screen colorsScreen = createPreferenceScreen("colors");

        final PreferenceSet backgroundSet = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                return profile.wallpaperOption.getValue().startsWith("/");
            }
        };
        myBackgroundPreference = new BackgroundPreference(
                this,
                profile,
                colorsScreen.Resource.getResource("background"),
                BACKGROUND_REQUEST_CODE
        ) {
            @Override
            public void update(Intent data) {
                super.update(data);
                backgroundSet.run();
            }
        };
        colorsScreen.addPreference(myBackgroundPreference);
        backgroundSet.add(colorsScreen.addOption(profile.fillModeOption, "fillMode"));
        backgroundSet.run();

        colorsScreen.addOption(profile.regularTextOption, "text");
        colorsScreen.addOption(profile.hyperlinkTextOption, "hyperlink");
        colorsScreen.addOption(profile.visitedHyperlinkTextOption, "hyperlinkVisited");
        colorsScreen.addOption(profile.footerFillOption, "footerOldStyle");
        colorsScreen.addOption(profile.footerNGBackgroundOption, "footerBackground");
        colorsScreen.addOption(profile.footerNGForegroundOption, "footerForeground");
        colorsScreen.addOption(profile.footerNGForegroundUnreadOption, "footerForegroundUnread");
        colorsScreen.addOption(profile.selectionBackgroundOption, "selectionBackground");
        colorsScreen.addOption(profile.selectionForegroundOption, "selectionForeground");
        colorsScreen.addOption(profile.highlightingForegroundOption, "highlightingForeground");
        colorsScreen.addOption(profile.highlightingBackgroundOption, "highlightingBackground");

        final Screen marginsScreen = createPreferenceScreen("margins");
        marginsScreen.addOption(viewOptions.leftMargin, "left");
        marginsScreen.addOption(viewOptions.rightMargin, "right");
        marginsScreen.addOption(viewOptions.topMargin, "top");
        marginsScreen.addOption(viewOptions.bottomMargin, "bottom");
        marginsScreen.addOption(viewOptions.spaceBetweenColumns, "spaceBetweenColumns");

        final Screen statusLineScreen = createPreferenceScreen("scrollBar");

        final PreferenceSet footerPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                switch (viewOptions.scrollbarType.getValue()) {
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER:
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE:
                        return true;
                    default:
                        return false;
                }
            }
        };
        final PreferenceSet tocPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                switch (viewOptions.scrollbarType.getValue()) {
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER:
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE:
                        return footerOptions.showTOCMarks.getValue();
                    default:
                        return false;
                }
            }
        };
        final PreferenceSet oldStyleFooterPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                switch (viewOptions.scrollbarType.getValue()) {
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE:
                        return true;
                    default:
                        return false;
                }
            }
        };
        final PreferenceSet newStyleFooterPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                switch (viewOptions.scrollbarType.getValue()) {
                    case FBView.SCROLLBAR_SHOW_AS_FOOTER:
                        return true;
                    default:
                        return false;
                }
            }
        };


        final String[] scrollBarTypes = {"hide", "show", "showAsProgress", "showAsFooter", "showAsFooterOldStyle"};
        statusLineScreen.addPreference(new ZLChoicePreference(
                this, statusLineScreen.Resource.getResource("scrollbarType"),
                viewOptions.scrollbarType, scrollBarTypes
        ) {
            @Override
            protected void onDialogClosed(boolean result) {
                super.onDialogClosed(result);
                footerPreferences.run();
                tocPreferences.run();
                oldStyleFooterPreferences.run();
                newStyleFooterPreferences.run();
            }
        });

        footerPreferences.add(statusLineScreen.addPreference(new ZLIntegerRangePreference(
                this, statusLineScreen.Resource.getResource("footerHeight"),
                viewOptions.footerHeight
        )));
        oldStyleFooterPreferences.add(statusLineScreen.addOption(profile.footerFillOption, "footerOldStyleColor"));
        newStyleFooterPreferences.add(statusLineScreen.addOption(profile.footerNGBackgroundOption, "footerBackgroundColor"));
        newStyleFooterPreferences.add(statusLineScreen.addOption(profile.footerNGForegroundOption, "footerForegroundColor"));
        newStyleFooterPreferences.add(statusLineScreen.addOption(profile.footerNGForegroundUnreadOption, "footerForegroundUnreadColor"));
        footerPreferences.add(statusLineScreen.addPreference(new ZLBooleanPreference(
                PreferenceActivity.this,
                footerOptions.showTOCMarks,
                statusLineScreen.Resource.getResource("tocMarks")
        ) {
            @Override
            protected void onClick() {
                super.onClick();
                tocPreferences.run();
            }
        }));
        tocPreferences.add(statusLineScreen.addOption(footerOptions.maxTOCMarks, "tocMarksMaxNumber"));
        footerPreferences.add(statusLineScreen.addOption(footerOptions.showProgress, "showProgress"));
        footerPreferences.add(statusLineScreen.addOption(footerOptions.showClock, "showClock"));
        footerPreferences.add(statusLineScreen.addOption(footerOptions.showBattery, "showBattery"));
        footerPreferences.add(statusLineScreen.addPreference(new FontPreference(
                this, statusLineScreen.Resource.getResource("font"),
                footerOptions.font, false
        )));
        footerPreferences.run();
        tocPreferences.run();
        oldStyleFooterPreferences.run();
        newStyleFooterPreferences.run();

		/*
		final Screen colorProfileScreen = createPreferenceScreen("colorProfile");
		final ZLResource resource = colorProfileScreen.Resource;
		colorProfileScreen.setSummary(ColorProfilePreference.createTitle(resource, fbreader.getColorProfileName()));
		for (String key : ColorProfile.names()) {
			colorProfileScreen.addPreference(new ColorProfilePreference(
				this, fbreader, colorProfileScreen, key, ColorProfilePreference.createTitle(resource, key)
			));
		}
		 */

        final Screen scrollingScreen = createPreferenceScreen("scrolling");
        scrollingScreen.addOption(pageTurningOptions.fingerScrolling, "fingerScrolling");
        scrollingScreen.addOption(miscOptions.enableDoubleTap, "enableDoubleTapDetection");

        final PreferenceSet volumeKeysPreferences = new PreferenceSet.Enabler() {
            @Override
            protected Boolean detectState() {
                return keyBindings.hasBinding(KeyEvent.KEYCODE_VOLUME_UP, false);
            }
        };
        scrollingScreen.addPreference(new ZLCheckBoxPreference(
                this, scrollingScreen.Resource.getResource("volumeKeys")
        ) {
            {
                setChecked(keyBindings.hasBinding(KeyEvent.KEYCODE_VOLUME_UP, false));
            }

            @Override
            protected void onClick() {
                super.onClick();
                if (isChecked()) {
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
                } else {
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, FBReaderApp.NoAction);
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, FBReaderApp.NoAction);
                }
                volumeKeysPreferences.run();
            }
        });
        volumeKeysPreferences.add(scrollingScreen.addPreference(new ZLCheckBoxPreference(
                this, scrollingScreen.Resource.getResource("invertVolumeKeys")
        ) {
            {
                setChecked(ActionCode.VOLUME_KEY_SCROLL_FORWARD.equals(
                        keyBindings.getBinding(KeyEvent.KEYCODE_VOLUME_UP, false)
                ));
            }

            @Override
            protected void onClick() {
                super.onClick();
                if (isChecked()) {
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
                } else {
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
                    keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
                }
            }
        }));
        volumeKeysPreferences.run();

        scrollingScreen.addOption(pageTurningOptions.animation, "animation");
        scrollingScreen.addPreference(new AnimationSpeedPreference(
                this,
                scrollingScreen.Resource,
                "animationSpeed",
                pageTurningOptions.animationSpeed
        ));
        scrollingScreen.addOption(pageTurningOptions.horizontal, "horizontal");

        final Screen dictionaryScreen = createPreferenceScreen("dictionary");

        final List<String> langCodes = ZLResource.languageCodes();
        final ArrayList<Language> languages = new ArrayList<Language>(langCodes.size() + 1);
        for (String code : langCodes) {
            languages.add(new Language(code));
        }
        Collections.sort(languages);
        languages.add(0, new Language(
                Language.ANY_CODE, dictionaryScreen.Resource.getResource("targetLanguage")
        ));
        final LanguagePreference targetLanguagePreference = new LanguagePreference(
                this, dictionaryScreen.Resource.getResource("targetLanguage"), languages
        ) {
            @Override
            protected void init() {
                setInitialValue(DictionaryUtil.targetLanguageOption.getValue());
            }

            @Override
            protected void setLanguage(String code) {
                DictionaryUtil.targetLanguageOption.setValue(code);
            }
        };

        DictionaryUtil.init(this, new Runnable() {
            public void run() {
                dictionaryScreen.addPreference(new DictionaryPreference(
                        PreferenceActivity.this,
                        dictionaryScreen.Resource.getResource("dictionary"),
                        DictionaryUtil.singleWordTranslatorOption(),
                        DictionaryUtil.dictionaryInfos(PreferenceActivity.this, true)
                ) {
                    @Override
                    protected void onDialogClosed(boolean result) {
                        super.onDialogClosed(result);
                        targetLanguagePreference.setEnabled(
                                DictionaryUtil.getCurrentDictionaryInfo(true).getSupportsTargetLanguageSetting()
                        );
                    }
                });
                dictionaryScreen.addPreference(new DictionaryPreference(
                        PreferenceActivity.this,
                        dictionaryScreen.Resource.getResource("translator"),
                        DictionaryUtil.multiWordTranslatorOption(),
                        DictionaryUtil.dictionaryInfos(PreferenceActivity.this, false)
                ));
                dictionaryScreen.addPreference(new ZLBooleanPreference(
                        PreferenceActivity.this,
                        miscOptions.navigateAllWords,
                        dictionaryScreen.Resource.getResource("navigateOverAllWords")
                ));
                dictionaryScreen.addOption(miscOptions.wordTappingAction, "longTapAction");
                dictionaryScreen.addPreference(targetLanguagePreference);
                targetLanguagePreference.setEnabled(
                        DictionaryUtil.getCurrentDictionaryInfo(true).getSupportsTargetLanguageSetting()
                );
            }
        });

        final Screen imagesScreen = createPreferenceScreen("images");
        imagesScreen.addOption(imageOptions.tapAction, "longTapAction");
        imagesScreen.addOption(imageOptions.fitToScreen, "fitImagesToScreen");
        imagesScreen.addOption(imageOptions.imageViewBackground, "backgroundColor");
        imagesScreen.addOption(imageOptions.matchBackground, "matchBackground");

        final CancelMenuHelper cancelMenuHelper = new CancelMenuHelper();
        final Screen cancelMenuScreen = createPreferenceScreen("cancelMenu");
        cancelMenuScreen.addOption(cancelMenuHelper.getShowLibraryItemOption(), "library");
        cancelMenuScreen.addOption(cancelMenuHelper.getShowNetworkLibraryItemOption(), "networkLibrary");
        cancelMenuScreen.addOption(cancelMenuHelper.getShowPreviousBookItemOption(), "previousBook");
        cancelMenuScreen.addOption(cancelMenuHelper.getShowPositionItemsOption(), "positions");
        final String[] backKeyActions =
                {ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU};
        cancelMenuScreen.addPreference(new ZLStringChoicePreference(
                this, cancelMenuScreen.Resource.getResource("backKeyAction"),
                keyBindings.getOption(KeyEvent.KEYCODE_BACK, false), backKeyActions
        ));
        final String[] backKeyLongPressActions =
                {ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU, FBReaderApp.NoAction};
        cancelMenuScreen.addPreference(new ZLStringChoicePreference(
                this, cancelMenuScreen.Resource.getResource("backKeyLongPressAction"),
                keyBindings.getOption(KeyEvent.KEYCODE_BACK, true), backKeyLongPressActions
        ));

        final Screen tipsScreen = createPreferenceScreen("tips");
        tipsScreen.addOption(TipsManager.showTipsOption, "showTips");

        final Screen aboutScreen = createPreferenceScreen("about");
        aboutScreen.addPreference(new InfoPreference(
                this,
                aboutScreen.Resource.getResource("version").getValue(),
                androidLibrary.getFullVersionName()
        ));
        aboutScreen.addPreference(new UrlPreference(this, aboutScreen.Resource, "site"));
        aboutScreen.addPreference(new UrlPreference(this, aboutScreen.Resource, "email"));
        aboutScreen.addPreference(new UrlPreference(this, aboutScreen.Resource, "googleplus"));
        aboutScreen.addPreference(new UrlPreference(this, aboutScreen.Resource, "twitter"));
        aboutScreen.addPreference(new UrlPreference(this, aboutScreen.Resource, "facebook"));
        aboutScreen.addPreference(new ThirdPartyLibrariesPreference(this, aboutScreen.Resource, "thirdParty"));
    }
}
