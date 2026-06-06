/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.core.constants

interface XMLNamespaces {
    companion object {
        const val DublinCore = "http://purl.org/dc/elements/1.1/"
        const val DublinCoreLegacy = "http://purl.org/metadata/dublin_core"
        const val XLink = "http://www.w3.org/1999/xlink"
        const val OpenPackagingFormat = "http://www.idpf.org/2007/opf"

        const val Atom = "http://www.w3.org/2005/Atom"
        const val Opds = "http://opds-spec.org/2010/catalog"
        const val DublinCoreTerms = "http://purl.org/dc/terms/"
        const val DublinCoreSyndication = "http://purl.org/syndication/thread/1.0"
        const val OpenSearch = "http://a9.com/-/spec/opensearch/1.1/"
        const val CalibreMetadata = "http://calibre.kovidgoyal.net/2009/metadata"

        const val FBReaderCatalogMetadata = "http://data.fbreader.org/catalog/metadata/"

        const val MarlinEpub = "http://marlin-drm.com/epub"
        const val XMLEncryption = "http://www.w3.org/2001/04/xmlenc#"
        const val XMLDigitalSignature = "http://www.w3.org/2000/09/xmldsig#"
        const val EpubContainer = "urn:oasis:names:tc:opendocument:xmlns:container"
    }
}
