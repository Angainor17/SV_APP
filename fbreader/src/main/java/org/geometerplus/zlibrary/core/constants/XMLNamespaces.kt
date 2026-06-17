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
