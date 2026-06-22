package com.github.axet.bookreader.widgets

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.androidlibrary.widgets.OpenChoicer
import com.github.axet.androidlibrary.widgets.OpenFileDialog
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.PermissionHelper
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.app.TTFManager
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil
import java.io.File

/**
 * Popup для выбора шрифта и размера текста.
 */
open class FontsPopup(context: Context, private val ttf: TTFManager) : PopupWindow(context) {

    companion object {
        private val TAG = FontsPopup::class.java.simpleName
    }

    val fonts: FontAdapter = FontAdapter(context)
    val fontsFrame: View
    val fontsList: RecyclerView
    val fontsizePopup: View = LayoutInflater.from(context).inflate(R.layout.font_popup, FrameLayout(context), false)
    val fontsizepopupText: TextView = fontsizePopup.findViewById(R.id.fontsize_text)
    val fontsizepopupSeek: SeekBar = fontsizePopup.findViewById(R.id.fontsize_seek)
    val fontsizepopupMinus: View = fontsizePopup.findViewById(R.id.fontsize_minus)
    val fontsizepopupPlus: View = fontsizePopup.findViewById(R.id.fontsize_plus)
    val ignoreEmbeddedFonts: CheckBox = fontsizePopup.findViewById(R.id.ignore_embedded_fonts)

    var choicer: OpenChoicer? = null
    var fragment: Fragment? = null
    var code: Int = 0

    private val fontsText: TextView = fontsizePopup.findViewById(R.id.fonts_text)
    private val fontsBrowse: View = fontsizePopup.findViewById(R.id.fonts_browse)

    init {
        fonts.clickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            fonts.select(position)
            setFont(fonts.ff[position].name)
        }
        fontsFrame = fontsizePopup.findViewById(R.id.fonts_frame)
        fontsText.text = context.getString(R.string.sv_add_more_fonts_to, TTFManager.USER_FONTS.toString())
        val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (Build.VERSION.SDK_INT >= 30) {
            fontsBrowse.visibility = View.VISIBLE
            fontsBrowse.setOnClickListener {
                choicer = object : OpenChoicer(OpenFileDialog.DIALOG_TYPE.FOLDER_DIALOG, true) {
                    override fun onResult(uri: Uri) {
                        shared.edit().putString(BookApplication.PREFERENCE_FONTS_FOLDER, uri.toString()).commit()
                        updatePath()
                        ttf.setFolder(uri)
                        ttf.preloadFonts()
                        loadFonts()
                    }
                }
                choicer!!.setStorageAccessFramework(fragment, code)
                choicer!!.show(null)
            }
            updatePath()
        } else {
            if (!PermissionHelper.hasStoragePermissions(context, false))
                fontsText.text = context.getString(R.string.sv_add_more_fonts_to, ttf.appFonts.toString())
        }
        fontsList = fontsizePopup.findViewById(R.id.fonts_list)
        fontsList.layoutManager = LinearLayoutManager(context)

        ignoreEmbeddedFonts.setOnCheckedChangeListener { _, isChecked ->
            setIgnoreEmbeddedFonts(isChecked)
        }
        setContentView(fontsizePopup)
    }

    fun updatePath() {
        val context = ttf.context
        val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var uri = shared.getString(BookApplication.PREFERENCE_FONTS_FOLDER, "")
        if (uri.isNullOrEmpty())
            uri = ttf.appFonts?.toString() ?: ""
        else
            uri = Storage.getDisplayName(context, uri.toUri())
        fontsText.text = context.getString(R.string.sv_add_more_fonts_to, uri)
    }

    open fun setFont(str: String) {}
    open fun setFontsize(f: Int) {}
    open fun setIgnoreEmbeddedFonts(f: Boolean) {}
    open fun updateFontsize(f: Int) {}

    fun loadFonts() {
        fonts.clear()
        fontsFrame.visibility = View.VISIBLE
        fontsList.adapter = fonts
        fonts.addBasics()
        for (name in AndroidFontUtil.ourFontFileMap.keys) {
            val ff = AndroidFontUtil.ourFontFileMap[name]
            for (f in ff!!) {
                if (f != null) {
                    try {
                        fonts.ff.add(FontView(name, ttf.load(f)))
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                    }
                    break // regular first, ignore rest
                }
            }
        }
        fonts.sort()
    }

    fun updateFontsize(start: Int, end: Int, f: Int) {
        fontsizepopupSeek.max = end - start
        fontsizepopupSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFontsize(progress + start)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val p = fontsizepopupSeek.progress
                setFontsize(start + p)
            }
        })
        fontsizepopupSeek.progress = f - start
        fontsizepopupMinus.setOnClickListener {
            var p = fontsizepopupSeek.progress
            p--
            if (p < 0) p = 0
            fontsizepopupSeek.progress = p
            setFontsize(start + p)
        }
        fontsizepopupPlus.setOnClickListener {
            var p = fontsizepopupSeek.progress
            p++
            if (p >= end - start) p = end - start
            fontsizepopupSeek.progress = p
            setFontsize(start + p)
        }
    }

    /**
     * Класс для представления шрифта.
     */
    class FontView(
        val name: String,
        val font: Typeface = Typeface.create(name, Typeface.NORMAL),
        val file: File = File(""),
        val index: Int = 0
    )

    /**
     * ViewHolder для элемента списка шрифтов.
     */
    class FontHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: CheckedTextView = itemView.findViewById(android.R.id.text1)
    }

    /**
     * Адаптер для списка шрифтов.
     */
    class FontAdapter(private val context: Context) : RecyclerView.Adapter<FontHolder>() {

        companion object {
            val DEFAULT = ArrayList(listOf("sans-serif", "serif", "monospace"))
        }

        val ff = ArrayList<FontView>()
        var selected: Int = 0
        var clickListener: AdapterView.OnItemClickListener? = null

        fun clear() {
            ff.clear()
        }

        fun addBasics() {
            for (s in DEFAULT)
                add(s)
        }

        fun sort() {
            ff.sortWith { o1, o2 ->
                val i1 = DEFAULT.indexOf(o1.name)
                val i2 = DEFAULT.indexOf(o2.name)
                if (i1 != -1 && i2 != -1)
                    return@sortWith i1.compareTo(i2)
                if (i1 != -1) return@sortWith -1
                if (i2 != -1) return@sortWith 1
                var r = o1.name.compareTo(o2.name)
                if (r != 0) return@sortWith r
                r = o1.file.compareTo(o2.file)
                if (r != 0) return@sortWith r
                o1.index.compareTo(o2.index)
            }
            notifyDataSetChanged()
        }

        fun select(f: String) {
            for (i in ff.indices) {
                if (ff[i].name == f)
                    selected = i
            }
            notifyDataSetChanged()
        }

        fun select(i: Int) {
            selected = i
            notifyDataSetChanged()
        }

        fun add(f: String) {
            ff.add(FontView(f))
        }

        override fun getItemCount(): Int = ff.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontHolder {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(android.R.layout.select_dialog_singlechoice, parent, false)
            return FontHolder(view)
        }

        override fun onBindViewHolder(holder: FontHolder, position: Int) {
            holder.itemView.setOnClickListener {
                clickListener?.onItemClick(null, null, holder.adapterPosition, -1)
            }
            holder.tv.isChecked = selected == position
            holder.tv.typeface = ff[position].font
            holder.tv.text = ff[position].name
        }
    }
}
