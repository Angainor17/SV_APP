/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.dict

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import org.geometerplus.android.fbreader.FBReaderMainActivity
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.fbreader.fbreader.DurationEnum
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.util.Collections
import java.util.LinkedList

object DictionaryUtil {
    val translationToastDurationOption: ZLEnumOption<DurationEnum> =
        ZLEnumOption("Dictionary", "TranslationToastDuration", DurationEnum.duration40)
    val errorToastDurationOption: ZLEnumOption<DurationEnum> =
        ZLEnumOption("Dictionary", "ErrorToastDuration", DurationEnum.duration5)
    // TODO: use StringListOption instead
    @JvmField
    val targetLanguageOption = ZLStringOption("Dictionary", "TargetLanguage", Language.ANY_CODE)

    internal var FLAG_SHOW_AS_DICTIONARY = 2
    private var FLAG_INSTALLED_ONLY = 1
    private var FLAG_SHOW_AS_TRANSLATOR = 4
    private var ourSingleWordTranslatorOption: ZLStringOption? = null
    private var ourMultiWordTranslatorOption: ZLStringOption? = null
    // Map: dictionary info -> mode if package is not installed
    private val ourInfos = Collections.synchronizedMap(LinkedHashMap<PackageInfo, Int>())

    @JvmStatic
    fun init(activity: Activity, postAction: Runnable?) {
        if (ourInfos.isEmpty()) {
            val initThread = Thread(Initializer(activity, postAction))
            initThread.priority = Thread.MIN_PRIORITY
            initThread.start()
        } else {
            postAction?.run()
        }
    }

    @JvmStatic
    fun dictionaryInfos(context: Context, dictionaryNotTranslator: Boolean): List<PackageInfo> {
        val list = LinkedList<PackageInfo>()
        val installedPackages = HashSet<String>()
        val notInstalledPackages = HashSet<String>()
        synchronized(ourInfos) {
            for ((info, flags) in ourInfos) {
                if (dictionaryNotTranslator) {
                    if (flags and FLAG_SHOW_AS_DICTIONARY == 0) {
                        continue
                    }
                } else {
                    if (flags and FLAG_SHOW_AS_TRANSLATOR == 0) {
                        continue
                    }
                }

                val packageName = info["package"] ?: continue
                if (flags and FLAG_INSTALLED_ONLY == 0 || installedPackages.contains(packageName)) {
                    list.add(info)
                } else if (!notInstalledPackages.contains(packageName)) {
                    if (PackageUtil.canBeStarted(context, info.getActionIntent("test"), false)) {
                        list.add(info)
                        installedPackages.add(packageName)
                    } else {
                        notInstalledPackages.add(packageName)
                    }
                }
            }
        }
        return list
    }

    private fun firstInfo(): PackageInfo {
        synchronized(ourInfos) {
            for ((_, flags) in ourInfos) {
                if (flags and FLAG_INSTALLED_ONLY == 0) {
                    return ourInfos.keys.find { ourInfos[it] == flags }!!
                }
            }
        }
        throw RuntimeException("There are no available dictionary infos")
    }

    @JvmStatic
    fun singleWordTranslatorOption(): ZLStringOption {
        if (ourSingleWordTranslatorOption == null) {
            ourSingleWordTranslatorOption = ZLStringOption("Dictionary", "Id", firstInfo().id)
        }
        return ourSingleWordTranslatorOption!!
    }

    @JvmStatic
    fun multiWordTranslatorOption(): ZLStringOption {
        if (ourMultiWordTranslatorOption == null) {
            ourMultiWordTranslatorOption = ZLStringOption("Translator", "Id", firstInfo().id)
        }
        return ourMultiWordTranslatorOption!!
    }

    private fun getDictionaryInfo(id: String?): PackageInfo {
        if (id == null) {
            return firstInfo()
        }

        synchronized(ourInfos) {
            for (info in ourInfos.keys) {
                if (id == info.id) {
                    return info
                }
            }
        }
        return firstInfo()
    }

    @JvmStatic
    fun getCurrentDictionaryInfo(singleWord: Boolean): PackageInfo {
        val option = if (singleWord) singleWordTranslatorOption() else multiWordTranslatorOption()
        return getDictionaryInfo(option.value)
    }

    @JvmStatic
    fun openTextInDictionary(fbreader: Activity, text: String, singleWord: Boolean, selectionTop: Int, selectionBottom: Int, outliner: Runnable?) {
        val textToTranslate: String = if (singleWord) {
            var start = 0
            var end = text.length
            while (start < end && !Character.isLetterOrDigit(text[start])) {
                start++
            }
            while (start < end && !Character.isLetterOrDigit(text[end - 1])) {
                end--
            }
            if (start == end) {
                return
            }
            text.substring(start, end)
        } else {
            text
        }

        val metrics = DisplayMetrics()
        fbreader.windowManager.defaultDisplay.getMetrics(metrics)
        val frameMetrics = PopupFrameMetric(metrics, selectionTop, selectionBottom)

        val info = getCurrentDictionaryInfo(singleWord)
        fbreader.runOnUiThread {
            info.open(textToTranslate, outliner, fbreader, frameMetrics)
        }
    }

    fun onActivityResult(fbreader: FBReaderMainActivity, resultCode: Int, data: Intent?) {
        (getDictionaryInfo("dictan") as? Dictan)?.onActivityResult(fbreader, resultCode, data)
    }

    abstract class PackageInfo(
        id: String,
        title: String?,
        val supportsTargetLanguageSetting: Boolean = false
    ) : HashMap<String, String>() {

        constructor(id: String, title: String?) : this(id, title, false)

        init {
            put("id", id)
            put("title", title ?: id)
        }

        val id: String
            get() = get("id") ?: ""

        val title: String
            get() = get("title") ?: ""

        internal fun getActionIntent(text: String): Intent {
            val intent = Intent(get("action"))

            val packageName = get("package")
            if (packageName != null) {
                val className = get("class")
                if (className != null) {
                    intent.component = ComponentName(
                        packageName,
                        if (className.startsWith(".")) packageName + className else className
                    )
                }
            }

            val category = get("category")
            if (category != null) {
                intent.addCategory(category)
            }

            val key = get("dataKey")
            return if (key != null) {
                intent.putExtra(key, text)
            } else {
                intent.data = Uri.parse(text)
                intent
            }
        }

        open fun onActivityResult(fbreader: FBReaderMainActivity, resultCode: Int, data: Intent?) {
            // does nothing; implement in subclasses
        }

        internal abstract fun open(text: String, outliner: Runnable?, fbreader: Activity, frameMetrics: PopupFrameMetric)
    }

    private class PlainPackageInfo(id: String, title: String?) : PackageInfo(id, title) {
        override fun open(text: String, outliner: Runnable?, fbreader: Activity, frameMetrics: PopupFrameMetric) {
            val intent = getActionIntent(text)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            InternalUtil.startDictionaryActivity(fbreader, intent, this)
        }
    }

    private class InfoReader : DefaultHandler() {
        @Throws(SAXException::class)
        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName != "dictionary") {
                return
            }

            val id = attributes.getValue("id") ?: return
            val title = attributes.getValue("title")
            val role = attributes.getValue("role")
            val flags: Int = when (role) {
                "dictionary" -> FLAG_SHOW_AS_DICTIONARY
                "translator" -> FLAG_SHOW_AS_TRANSLATOR
                else -> FLAG_SHOW_AS_DICTIONARY or FLAG_SHOW_AS_TRANSLATOR
            }
            val finalFlags = if (attributes.getValue("list") != "always") {
                flags or FLAG_INSTALLED_ONLY
            } else {
                flags
            }
            val info: PackageInfo = when (id) {
                "dictan" -> Dictan(id, title)
                "ColorDict" -> ColorDict(id, title)
                else -> PlainPackageInfo(id, title)
            }
            for (i in attributes.length - 1 downTo 0) {
                info[attributes.getLocalName(i)] = attributes.getValue(i)
            }
            ourInfos[info] = finalFlags
        }
    }

    private class BitKnightsInfoReader(private val myContext: Context) : DefaultHandler() {
        private var myCounter = 0

        @Throws(SAXException::class)
        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName != "dictionary") {
                return
            }

            val info = PlainPackageInfo(
                "BK${myCounter++}",
                attributes.getValue("title")
            )
            for (i in attributes.length - 1 downTo 0) {
                info[attributes.getLocalName(i)] = attributes.getValue(i)
            }
            info["class"] = "com.bitknights.dict.ShareTranslateActivity"
            info["action"] = Intent.ACTION_VIEW
            // TODO: other attributes
            if (PackageUtil.canBeStarted(myContext, info.getActionIntent("test"), false)) {
                ourInfos[info] = FLAG_SHOW_AS_DICTIONARY or FLAG_INSTALLED_ONLY
            }
        }
    }

    private class Initializer(
        private val myActivity: Activity,
        private val myPostAction: Runnable?
    ) : Runnable {

        override fun run() {
            synchronized(ourInfos) {
                if (ourInfos.isNotEmpty()) {
                    myPostAction?.run()
                    return
                }
                XmlUtil.parseQuietly(
                    ZLFile.createFileByPath("dictionaries/main.xml"),
                    InfoReader()
                )
                XmlUtil.parseQuietly(
                    ZLFile.createFileByPath("dictionaries/bitknights.xml"),
                    BitKnightsInfoReader(myActivity)
                )
                myActivity.runOnUiThread {
                    myPostAction?.run()
                }
            }
        }
    }

    class PopupFrameMetric(metrics: DisplayMetrics, selectionTop: Int, selectionBottom: Int) {
        val height: Int
        val gravity: Int

        init {
            val screenHeight = metrics.heightPixels
            val topSpace = selectionTop
            val bottomSpace = metrics.heightPixels - selectionBottom
            val showAtBottom = bottomSpace >= topSpace
            val space = (if (showAtBottom) bottomSpace else topSpace) - metrics.densityDpi / 12
            val maxHeight = Math.min(metrics.densityDpi * 20 / 12, screenHeight * 2 / 3)
            val minHeight = Math.min(metrics.densityDpi * 10 / 12, screenHeight * 2 / 3)

            height = Math.max(minHeight, Math.min(maxHeight, space))
            gravity = if (showAtBottom) android.view.Gravity.BOTTOM else android.view.Gravity.TOP
        }
    }
}
