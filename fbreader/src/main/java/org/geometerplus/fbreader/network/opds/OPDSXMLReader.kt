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

import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.atom.ATOMDateConstruct
import org.geometerplus.fbreader.network.atom.ATOMFeedHandler
import org.geometerplus.fbreader.network.atom.ATOMXMLReader
import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.money.MoneyException
import org.geometerplus.zlibrary.core.xml.ZLStringMap

open class OPDSXMLReader(
    library: NetworkLibrary,
    handler: ATOMFeedHandler<OPDSFeedMetadata, OPDSEntry>,
    readEntryNotFeed: Boolean
) : ATOMXMLReader<OPDSFeedMetadata, OPDSEntry>(library, handler, readEntryNotFeed) {

    private var dcIssued: DCDate? = null
    private var priceCurrency: String? = null

    internal val opdsFeed: OPDSFeedMetadata?
        get() = getATOMFeed()

    internal val opdsEntry: OPDSEntry?
        get() = getATOMEntry()

    internal val opdsLink: OPDSLink
        get() = getATOMLink() as OPDSLink

    override fun startElementHandler(ns: String?, tag: String, attributes: ZLStringMap, bufferContent: String?): Boolean {
        when (state) {
            FEED -> {
                if (ns == XMLNamespaces.OpenSearch) {
                    when (tag) {
                        OPENSEARCH_TAG_TOTALRESULTS -> state = OPENSEARCH_TOTALRESULTS
                        OPENSEARCH_TAG_ITEMSPERPAGE -> state = OPENSEARCH_ITEMSPERPAGE
                        OPENSEARCH_TAG_STARTINDEX -> state = OPENSEARCH_STARTINDEX
                    }
                    return false
                } else if (ns == XMLNamespaces.FBReaderCatalogMetadata) {
                    if (tag == FBREADER_TAG_VIEW) {
                        state = FBREADER_VIEW
                    }
                    return false
                } else {
                    return super.startElementHandler(ns, tag, attributes, bufferContent)
                }
            }
            F_ENTRY -> {
                if (ns == XMLNamespaces.DublinCoreTerms) {
                    when (tag) {
                        DC_TAG_LANGUAGE -> state = FE_DC_LANGUAGE
                        DC_TAG_ISSUED -> {
                            dcIssued = DCDate(attributes)
                            state = FE_DC_ISSUED
                        }
                        DC_TAG_PUBLISHER -> state = FE_DC_PUBLISHER
                        DC_TAG_IDENTIFIER -> state = FE_DC_IDENTIFIER
                    }
                    return false
                } else if (ns == XMLNamespaces.CalibreMetadata) {
                    when (tag) {
                        CALIBRE_TAG_SERIES -> state = FE_CALIBRE_SERIES
                        CALIBRE_TAG_SERIES_INDEX -> state = FE_CALIBRE_SERIES_INDEX
                    }
                    return false
                } else {
                    return super.startElementHandler(ns, tag, attributes, bufferContent)
                }
            }
            FE_LINK -> {
                if (ns == XMLNamespaces.Opds && tag == TAG_PRICE) {
                    priceCurrency = attributes.getValue("currencycode")
                    state = FEL_PRICE
                    return false
                } else if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_FORMAT) {
                    state = FEL_FORMAT
                    return false
                } else {
                    return super.startElementHandler(ns, tag, attributes, bufferContent)
                }
            }
            FE_CONTENT -> {
                super.startElementHandler(ns, tag, attributes, bufferContent)
                // FIXME: HACK: html handling must be implemeted neatly
                if (tag == TAG_HACK_SPAN || attributes.getValue("class") == "price") {
                    state = FEC_HACK_SPAN
                }
                return false
            }
            else -> return super.startElementHandler(ns, tag, attributes, bufferContent)
        }
    }

    override fun endElementHandler(ns: String?, tag: String, bufferContent: String?): Boolean {
        when (state) {
            FEL_PRICE -> {
                if (ns == XMLNamespaces.Opds && tag == TAG_PRICE) {
                    if (bufferContent != null && priceCurrency != null) {
                        try {
                            val price = Money(bufferContent, priceCurrency)
                            opdsLink.prices.add(price)
                        } catch (e: MoneyException) {
                            e.printStackTrace()
                        }
                        priceCurrency = null
                    }
                    state = FE_LINK
                }
                return false
            }
            FEL_FORMAT -> {
                if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_FORMAT) {
                    if (bufferContent != null) {
                        opdsLink.formats.add(bufferContent.intern())
                    }
                    state = FE_LINK
                }
                return false
            }
            FEC_HACK_SPAN -> {
                // FIXME: HACK
                formattedBuffer.appendText(bufferContent)
                formattedBuffer.appendEndTag(tag)
                formattedBuffer.appendText("<br/>")
                if (bufferContent != null) {
                    opdsEntry?.addAttribute(KEY_PRICE, bufferContent.intern())
                }
                state = FE_CONTENT
                return false
            }
            FE_DC_LANGUAGE -> {
                if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_LANGUAGE) {
                    // FIXME:language can be lost:buffer will be truncated, if there are extension tags inside the <dc:language> tag
                    opdsEntry?.dcLanguage = bufferContent
                    state = F_ENTRY
                }
                return false
            }
            FE_DC_ISSUED -> {
                if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_ISSUED) {
                    // FIXME:issued can be lost:buffer will be truncated, if there are extension tags inside the <dc:issued> tag
                    if (bufferContent != null && dcIssued != null) {
                        if (ATOMDateConstruct.parse(bufferContent, dcIssued!!)) {
                            opdsEntry?.dcIssued = dcIssued
                        }
                    }
                    dcIssued = null
                    state = F_ENTRY
                }
                return false
            }
            FE_DC_PUBLISHER -> {
                if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_PUBLISHER) {
                    // FIXME:publisher can be lost:buffer will be truncated, if there are extension tags inside the <dc:publisher> tag
                    opdsEntry?.dcPublisher = bufferContent
                    state = F_ENTRY
                }
                return false
            }
            FE_DC_IDENTIFIER -> {
                if (ns == XMLNamespaces.DublinCoreTerms && tag == DC_TAG_IDENTIFIER) {
                    // FIXME:identifier can be lost:buffer will be truncated, if there are extension tags inside the <dc:publisher> tag
                    if (bufferContent != null) {
                        opdsEntry?.dcIdentifiers?.add(bufferContent)
                    }
                    state = F_ENTRY
                }
                return false
            }
            FE_CALIBRE_SERIES -> {
                if (ns == XMLNamespaces.CalibreMetadata && tag == CALIBRE_TAG_SERIES) {
                    opdsEntry?.seriesTitle = bufferContent
                    state = F_ENTRY
                }
                return false
            }
            FE_CALIBRE_SERIES_INDEX -> {
                if (ns == XMLNamespaces.CalibreMetadata && tag == CALIBRE_TAG_SERIES_INDEX) {
                    if (bufferContent != null) {
                        try {
                            opdsEntry?.seriesIndex = bufferContent.toFloat()
                        } catch (ex: NumberFormatException) {
                        }
                    }
                    state = F_ENTRY
                }
                return false
            }
            OPENSEARCH_TOTALRESULTS -> {
                if (ns == XMLNamespaces.OpenSearch && tag == OPENSEARCH_TAG_TOTALRESULTS) {
                    if (opdsFeed != null && bufferContent != null) {
                        try {
                            opdsFeed!!.opensearchTotalResults = bufferContent.toInt()
                        } catch (ex: NumberFormatException) {
                        }
                    }
                    state = FEED
                }
                return false
            }
            OPENSEARCH_ITEMSPERPAGE -> {
                if (ns == XMLNamespaces.OpenSearch && tag == OPENSEARCH_TAG_ITEMSPERPAGE) {
                    if (opdsFeed != null && bufferContent != null) {
                        try {
                            opdsFeed!!.opensearchItemsPerPage = bufferContent.toInt()
                        } catch (ex: NumberFormatException) {
                        }
                    }
                    state = FEED
                }
                return false
            }
            OPENSEARCH_STARTINDEX -> {
                if (ns == XMLNamespaces.OpenSearch && tag == OPENSEARCH_TAG_STARTINDEX) {
                    if (opdsFeed != null && bufferContent != null) {
                        try {
                            opdsFeed!!.opensearchStartIndex = bufferContent.toInt()
                        } catch (ex: NumberFormatException) {
                        }
                    }
                    state = FEED
                }
                return false
            }
            FBREADER_VIEW -> {
                if (ns == XMLNamespaces.FBReaderCatalogMetadata && tag == FBREADER_TAG_VIEW) {
                    opdsFeed?.viewType = bufferContent
                    state = FEED
                }
                return false
            }
            else -> return super.endElementHandler(ns, tag, bufferContent)
        }
    }

    companion object {
        const val KEY_PRICE = "price"

        private const val FE_DC_LANGUAGE = ATOM_STATE_FIRST_UNUSED
        private const val FE_DC_ISSUED = ATOM_STATE_FIRST_UNUSED + 1
        private const val FE_DC_PUBLISHER = ATOM_STATE_FIRST_UNUSED + 2
        private const val FE_DC_IDENTIFIER = ATOM_STATE_FIRST_UNUSED + 3
        private const val FE_CALIBRE_SERIES = ATOM_STATE_FIRST_UNUSED + 4
        private const val FE_CALIBRE_SERIES_INDEX = ATOM_STATE_FIRST_UNUSED + 5
        private const val FEL_PRICE = ATOM_STATE_FIRST_UNUSED + 6
        private const val FEL_FORMAT = ATOM_STATE_FIRST_UNUSED + 7
        private const val OPENSEARCH_TOTALRESULTS = ATOM_STATE_FIRST_UNUSED + 8
        private const val OPENSEARCH_ITEMSPERPAGE = ATOM_STATE_FIRST_UNUSED + 9
        private const val OPENSEARCH_STARTINDEX = ATOM_STATE_FIRST_UNUSED + 10
        private const val FEC_HACK_SPAN = ATOM_STATE_FIRST_UNUSED + 11
        private const val FBREADER_VIEW = ATOM_STATE_FIRST_UNUSED + 12

        private const val TAG_PRICE = "price"
        private const val TAG_HACK_SPAN = "span"
        private const val DC_TAG_LANGUAGE = "language"
        private const val DC_TAG_ISSUED = "issued"
        private const val DC_TAG_PUBLISHER = "publisher"
        private const val DC_TAG_FORMAT = "format"
        private const val DC_TAG_IDENTIFIER = "identifier"
        private const val CALIBRE_TAG_SERIES = "series"
        private const val CALIBRE_TAG_SERIES_INDEX = "series_index"
        private const val OPENSEARCH_TAG_TOTALRESULTS = "totalResults"
        private const val OPENSEARCH_TAG_ITEMSPERPAGE = "itemsPerPage"
        private const val OPENSEARCH_TAG_STARTINDEX = "startIndex"
        private const val FBREADER_TAG_VIEW = "view"
    }
}
