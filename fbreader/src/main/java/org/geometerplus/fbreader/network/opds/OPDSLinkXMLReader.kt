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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.authentication.litres.LitResAuthenticationManager
import org.geometerplus.fbreader.network.rss.RSSNetworkLink
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.xml.ZLStringMap

internal class OPDSLinkXMLReader(library: NetworkLibrary) : OPDSXMLReader(library, FeedHandler(library), false) {

    fun links(): List<INetworkLink> = feedHandler.links

    private val feedHandler: FeedHandler
        get() = getATOMFeedHandler() as FeedHandler

    override fun startElementHandler(ns: String?, tag: String, attributes: ZLStringMap, bufferContent: String?): Boolean {
        when (state) {
            FEED -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_ENTRY) {
                    feedHandler.clear()
                }
            }
            F_ENTRY -> {
                if (ns == XMLNamespaces.FBReaderCatalogMetadata) {
                    when (tag) {
                        FBREADER_ADVANCED_SEARCH -> return false
                        FBREADER_AUTHENTICATION -> {
                            val type = attributes.getValue("type")
                            feedHandler.setAuthenticationType(type)
                            return false
                        }
                        FBREADER_RELATION_ALIAS -> {
                            val name = attributes.getValue("name")
                            val type = attributes.getValue("type")
                            var alias = attributes.getValue("alias")
                            if (alias != null && name != null) {
                                if (alias.isEmpty()) {
                                    alias = null
                                }
                                feedHandler.addRelationAlias(RelationAlias(alias, type), name)
                            }
                            return false
                        }
                        FBREADER_REWRITING_RULE -> {
                            feedHandler.addUrlRewritingRule(URLRewritingRule(attributes))
                            return false
                        }
                        FBREADER_EXTRA -> {
                            val name = attributes.getValue("name")
                            val value = attributes.getValue("value")
                            if (name != null && value != null) {
                                feedHandler.putExtraData(name, value)
                            }
                        }
                    }
                }
            }
        }
        return super.startElementHandler(ns, tag, attributes, bufferContent)
    }

    private class FeedHandler(private val library: NetworkLibrary) : AbstractOPDSFeedHandler() {
        val links = mutableListOf<INetworkLink>()
        private val urlRewritingRules = mutableListOf<URLRewritingRule>()
        private val relationAliases = mutableMapOf<RelationAlias, String>()
        private val extraData = linkedMapOf<String, String>()
        private var authenticationType: String? = null

        fun setAuthenticationType(type: String?) {
            authenticationType = type
        }

        fun addUrlRewritingRule(rule: URLRewritingRule) {
            urlRewritingRules.add(rule)
        }

        fun addRelationAlias(alias: RelationAlias, relation: String) {
            relationAliases[alias] = relation
        }

        fun putExtraData(name: String, value: String) {
            extraData[name] = value
        }

        fun clear() {
            authenticationType = null
            urlRewritingRules.clear()
            relationAliases.clear()
            extraData.clear()
        }

        override fun processFeedEntry(entry: OPDSEntry): Boolean {
            val id = entry.id?.uri
            if (id == null) {
                return false
            }
            val title = entry.title
            val summary = entry.content
            val language = entry.dcLanguage

            val infos = UrlInfoCollection<UrlInfoWithDate>()
            for (link in entry.links) {
                val href = link.href ?: continue
                val mime = MimeType.get(link.type)
                val rel = link.rel
                when {
                    rel == OPDSConstants.REL_IMAGE_THUMBNAIL || rel == OPDSConstants.REL_THUMBNAIL -> {
                        if (MimeType.IMAGE_PNG == mime || MimeType.IMAGE_JPEG == mime) {
                            infos.addInfo(UrlInfoWithDate(UrlInfo.Type.Thumbnail, href, mime))
                        }
                    }
                    (rel != null && rel.startsWith(OPDSConstants.REL_IMAGE_PREFIX)) || rel == OPDSConstants.REL_COVER -> {
                        if (MimeType.IMAGE_PNG == mime || MimeType.IMAGE_JPEG == mime) {
                            infos.addInfo(UrlInfoWithDate(UrlInfo.Type.Image, href, mime))
                        }
                    }
                    rel == null -> {
                        if (MimeType.APP_ATOM_XML.weakEquals(mime) || MimeType.APP_RSS_XML.weakEquals(mime)) {
                            infos.addInfo(UrlInfoWithDate(UrlInfo.Type.Catalog, href, mime))
                        }
                    }
                    rel == "search" -> {
                        if (MimeType.APP_ATOM_XML.weakEquals(mime) || MimeType.TEXT_HTML.weakEquals(mime)) {
                            val descr = OpenSearchDescription.createDefault(href, mime)
                            if (descr.isValid()) {
                                infos.addInfo(UrlInfoWithDate(UrlInfo.Type.Search, descr.makeQuery("%s") ?: "", mime))
                            }
                        }
                    }
                    rel == "listbooks" -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.ListBooks, href, mime))
                    }
                    rel == OPDSConstants.REL_LINK_SIGN_IN -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.SignIn, href, mime))
                    }
                    rel == OPDSConstants.REL_LINK_SIGN_OUT -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.SignOut, href, mime))
                    }
                    rel == OPDSConstants.REL_LINK_SIGN_UP -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.SignUp, href, mime))
                    }
                    rel == OPDSConstants.REL_LINK_TOPUP -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.TopUp, href, mime))
                    }
                    rel == OPDSConstants.REL_LINK_RECOVER_PASSWORD -> {
                        infos.addInfo(UrlInfoWithDate(UrlInfo.Type.RecoverPassword, href, mime))
                    }
                }
            }

            if (title != null && infos.getInfo(UrlInfo.Type.Catalog) != null) {
                val l = createLink(id, title, summary, language, infos)
                if (l != null) {
                    links.add(l)
                }
            }
            return false
        }

        private fun createLink(
            id: String,
            title: CharSequence,
            summary: CharSequence?,
            language: String?,
            infos: UrlInfoCollection<UrlInfoWithDate>
        ): INetworkLink? {
            val titleString = title.toString()
            val summaryString = summary?.toString()

            val catalogInfo = infos.getInfo(UrlInfo.Type.Catalog) ?: return null

            return if (MimeType.APP_ATOM_XML.weakEquals(catalogInfo.mime)) {
                OPDSPredefinedNetworkLink(
                    library,
                    OPDSNetworkLink.INVALID_ID,
                    id,
                    titleString,
                    summaryString,
                    language,
                    infos
                ).apply {
                    setRelationAliases(relationAliases)
                    setUrlRewritingRules(urlRewritingRules)
                    setExtraData(extraData)

                    if (authenticationType == "litres") {
                        setAuthenticationManager(
                            NetworkAuthenticationManager.createManager(
                                library, this, LitResAuthenticationManager::class.java
                            )
                        )
                    }
                }
            } else if (MimeType.APP_RSS_XML.weakEquals(catalogInfo.mime)) {
                RSSNetworkLink(
                    OPDSNetworkLink.INVALID_ID,
                    id,
                    titleString,
                    summaryString,
                    language,
                    infos
                )
            } else {
                null
            }
        }

        override fun processFeedMetadata(feed: OPDSFeedMetadata, beforeEntries: Boolean): Boolean = false

        override fun processFeedStart() {}

        override fun processFeedEnd() {}
    }

    companion object {
        private const val FBREADER_ADVANCED_SEARCH = "advancedSearch"
        private const val FBREADER_AUTHENTICATION = "authentication"
        private const val FBREADER_REWRITING_RULE = "urlRewritingRule"
        private const val FBREADER_RELATION_ALIAS = "relationAlias"
        private const val FBREADER_EXTRA = "extra"
    }
}
