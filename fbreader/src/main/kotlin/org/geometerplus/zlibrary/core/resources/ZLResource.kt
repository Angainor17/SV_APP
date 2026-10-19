package org.geometerplus.zlibrary.core.resources

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.options.ZLStringOption
import java.util.Collections
import java.util.LinkedList
import java.util.Locale

abstract class ZLResource protected constructor(name: String) {
    @JvmField
    val Name: String = name

    abstract fun hasValue(): Boolean

    abstract fun getValue(): String

    abstract fun getValue(condition: Int): String

    abstract fun getResource(key: String): ZLResource

    companion object {
        private val ourLanguageCodes = LinkedList<String>()
        private val ourLanguageOption =
            ZLStringOption("LookNFeel", "Language", Language.SYSTEM_CODE)

        @JvmStatic
        fun languageCodes(): List<String> {
            synchronized(ourLanguageCodes) {
                if (ourLanguageCodes.isEmpty()) {
                    val dir = ZLResourceFile.createResourceFile("resources/application")
                    val children = dir.children()
                    for (file in children) {
                        val name = file.getShortName()
                        val postfix = ".xml"
                        if (name.endsWith(postfix) && name != "neutral.xml") {
                            ourLanguageCodes.add(name.substring(0, name.length - postfix.length))
                        }
                    }
                }
            }
            return Collections.unmodifiableList(ourLanguageCodes)
        }

        @JvmStatic
        fun interfaceLanguages(): List<Language> {
            val allLanguages = LinkedList<Language>()
            val resource = ZLResource.resource("language-self")
            for (c in languageCodes()) {
                allLanguages.add(Language(c, resource))
            }
            Collections.sort(allLanguages)
            allLanguages.add(0, Language(Language.SYSTEM_CODE))
            return allLanguages
        }

        @JvmStatic
        fun getLanguageOption(): ZLStringOption = ourLanguageOption

        @JvmStatic
        fun getLanguage(): String {
            val lang = getLanguageOption().getValue()
            return if (Language.SYSTEM_CODE == lang) Locale.getDefault().getLanguage() else lang
        }

        @JvmStatic
        fun resource(key: String): ZLResource {
            ZLTreeResource.buildTree()
            val root = ZLTreeResource.ourRoot
            if (root == null) {
                return ZLMissingResource.Instance
            }
            return root.getResource(key)
        }
    }
}
