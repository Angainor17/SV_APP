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

import org.fbreader.util.ComparisonUtil
import org.geometerplus.fbreader.network.ICustomNetworkLink
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkException
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.util.MimeType
import java.io.InputStream

class OPDSCustomNetworkLink(
    library: NetworkLibrary,
    id: Int,
    override val type: INetworkLink.Type = INetworkLink.Type.Predefined,
    title: String,
    summary: String?,
    language: String?,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : OPDSNetworkLink(library, id, title, summary, language, infos), ICustomNetworkLink {

    private var hasChanges = false
    private var customTitle: String = title
    private var customSummary: String? = summary

    override fun hasChanges(): Boolean = hasChanges

    override fun resetChanges() {
        hasChanges = false
    }

    override val title: String
        get() = customTitle

    override val summary: String?
        get() = customSummary

    override fun setSummary(summary: String?) {
        hasChanges = hasChanges || !ComparisonUtil.equal(customSummary, summary)
        customSummary = summary
    }

    override fun setTitle(title: String) {
        hasChanges = hasChanges || !ComparisonUtil.equal(customTitle, title)
        customTitle = title
    }

    override fun setUrl(type: UrlInfo.Type, url: String?, mime: MimeType?) {
        infosInternal.removeAllInfos(type)
        if (url != null) {
            infosInternal.addInfo(UrlInfoWithDate(type, url, mime ?: MimeType.NULL))
        }
        hasChanges = true
    }

    override fun removeUrl(type: UrlInfo.Type) {
        hasChanges = hasChanges || infosInternal.getInfo(type) != null
        infosInternal.removeAllInfos(type)
    }

    override fun isObsolete(milliSeconds: Long): Boolean {
        val old = System.currentTimeMillis() - milliSeconds

        var updateDate = getUrlInfo(UrlInfo.Type.Search).updated
        if (updateDate == null || updateDate.time < old) {
            return true
        }

        updateDate = getUrlInfo(UrlInfo.Type.Image).updated
        if (updateDate == null || updateDate.time < old) {
            return true
        }

        return false
    }

    @Throws(ZLNetworkException::class)
    override fun reloadInfo(nc: ZLNetworkContext, urlsOnly: Boolean, quietly: Boolean) {
        val opensearchDescriptionURLs = mutableListOf<String>()
        val descriptions = mutableListOf<OpenSearchDescription>()

        var error: ZLNetworkException? = null
        try {
            nc.perform(object : ZLNetworkRequest.Get(getUrl(UrlInfo.Type.Catalog), quietly) {
                override fun handleStream(inputStream: InputStream, length: Int) {
                    val info = OPDSCatalogInfoHandler(
                        url,
                        this@OPDSCustomNetworkLink,
                        opensearchDescriptionURLs
                    )
                    OPDSXMLReader(library, info, false).read(inputStream)

                    if (!info.feedStarted) {
                        throw ZLNetworkException.forCode(NetworkException.ERROR_NOT_AN_OPDS)
                    }
                    if (info.title == null) {
                        throw ZLNetworkException.forCode(NetworkException.ERROR_NO_REQUIRED_INFORMATION)
                    }
                    setUrl(UrlInfo.Type.Image, info.icon, MimeType.IMAGE_AUTO)
                    if (info.directOpenSearchDescription != null) {
                        descriptions.add(info.directOpenSearchDescription!!)
                    }
                    if (!urlsOnly) {
                        customTitle = info.title.toString()
                        customSummary = info.summary?.toString()
                    }
                }
            })
        } catch (e: ZLNetworkException) {
            error = e
        }

        if (opensearchDescriptionURLs.isNotEmpty()) {
            val requests = mutableListOf<ZLNetworkRequest>()
            for (url in opensearchDescriptionURLs) {
                requests.add(object : ZLNetworkRequest.Get(url, quietly) {
                    override fun handleStream(inputStream: InputStream, length: Int) {
                        OpenSearchXMLReader(url, descriptions).read(inputStream)
                    }
                })
            }
            try {
                nc.perform(requests)
            } catch (e: ZLNetworkException) {
                // we do ignore errors in opensearch description loading/parsing
                e.printStackTrace()
            }
        }

        if (descriptions.isNotEmpty()) {
            // TODO: May be do not use '%s'??? Use Description instead??? (this needs to rewrite SEARCH engine logic a little)
            val d = descriptions[0]
            setUrl(UrlInfo.Type.Search, d.makeQuery("%s"), d.mime)
        } else {
            setUrl(UrlInfo.Type.Search, null, null)
        }
        error?.let { throw it }
    }
}


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
//
//package org.geometerplus.fbreader.network.opds;
//
//import org.fbreader.util.ComparisonUtil;
//import org.geometerplus.fbreader.network.ICustomNetworkLink;
//import org.geometerplus.fbreader.network.NetworkException;
//import org.geometerplus.fbreader.network.NetworkLibrary;
//import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
//import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection;
//import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate;
//import org.geometerplus.zlibrary.core.network.ZLNetworkContext;
//import org.geometerplus.zlibrary.core.network.ZLNetworkException;
//import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;
//import org.geometerplus.zlibrary.core.util.MimeType;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Collections;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//
//public class OPDSCustomNetworkLink extends OPDSNetworkLink implements ICustomNetworkLink {
//    private final Type myType;
//    private boolean myHasChanges;
//
//    public OPDSCustomNetworkLink(NetworkLibrary library, int id, Type type, String title, String summary, String language, UrlInfoCollection<UrlInfoWithDate> infos) {
//        super(library, id, title, summary, language, infos);
//        myType = type;
//    }
//
//    public Type getType() {
//        return myType;
//    }
//
//    public boolean hasChanges() {
//        return myHasChanges;
//    }
//
//    public void resetChanges() {
//        myHasChanges = false;
//    }
//
//    public final void setSummary(String summary) {
//        myHasChanges = myHasChanges || !ComparisonUtil.equal(mySummary, summary);
//        mySummary = summary;
//    }
//
//    public final void setTitle(String title) {
//        myHasChanges = myHasChanges || !ComparisonUtil.equal(myTitle, title);
//        myTitle = title;
//    }
//
//    public final void setUrl(UrlInfo.Type type, String url, MimeType mime) {
//        myInfos.removeAllInfos(type);
//        myInfos.addInfo(new UrlInfoWithDate(type, url, mime));
//        myHasChanges = true;
//    }
//
//    public final void removeUrl(UrlInfo.Type type) {
//        myHasChanges = myHasChanges || myInfos.getInfo(type) != null;
//        myInfos.removeAllInfos(type);
//    }
//
//    public boolean isObsolete(long milliSeconds) {
//        final long old = System.currentTimeMillis() - milliSeconds;
//
//        Date updateDate = getUrlInfo(UrlInfo.Type.Search).Updated;
//        if (updateDate == null || updateDate.getTime() < old) {
//            return true;
//        }
//
//        updateDate = getUrlInfo(UrlInfo.Type.Image).Updated;
//        if (updateDate == null || updateDate.getTime() < old) {
//            return true;
//        }
//
//        return false;
//    }
//
//    public void reloadInfo(ZLNetworkContext nc, final boolean urlsOnly, boolean quietly) throws ZLNetworkException {
//        final LinkedList<String> opensearchDescriptionURLs = new LinkedList<String>();
//        final List<OpenSearchDescription> descriptions = Collections.synchronizedList(new LinkedList<OpenSearchDescription>());
//
//        ZLNetworkException error = null;
//        try {
//            nc.perform(new ZLNetworkRequest.Get(getUrl(UrlInfo.Type.Catalog), quietly) {
//                @Override
//                public void handleStream(InputStream inputStream, int length) throws IOException, ZLNetworkException {
//                final OPDSCatalogInfoHandler info = new OPDSCatalogInfoHandler(getURL(), OPDSCustomNetworkLink.this, opensearchDescriptionURLs);
//                new OPDSXMLReader(myLibrary, info, false).read(inputStream);
//
//                if (!info.FeedStarted) {
//                    throw ZLNetworkException.forCode(NetworkException.ERROR_NOT_AN_OPDS);
//                }
//                if (info.Title == null) {
//                    throw ZLNetworkException.forCode(NetworkException.ERROR_NO_REQUIRED_INFORMATION);
//                }
//                setUrl(UrlInfo.Type.Image, info.Icon, MimeType.IMAGE_AUTO);
//                if (info.DirectOpenSearchDescription != null) {
//                    descriptions.add(info.DirectOpenSearchDescription);
//                }
//                if (!urlsOnly) {
//                    myTitle = info.Title.toString();
//                    mySummary = info.Summary != null ? info.Summary.toString() : null;
//                }
//            }
//            });
//        } catch (ZLNetworkException e) {
//            error = e;
//        }
//
//        if (!opensearchDescriptionURLs.isEmpty()) {
//            LinkedList<ZLNetworkRequest> requests = new LinkedList<ZLNetworkRequest>();
//            for (String url : opensearchDescriptionURLs) {
//                requests.add(new ZLNetworkRequest.Get(url, quietly) {
//                    @Override
//                    public void handleStream(InputStream inputStream, int length) throws IOException, ZLNetworkException {
//                    new OpenSearchXMLReader(getURL(), descriptions).read(inputStream);
//                }
//                });
//            }
//            try {
//                nc.perform(requests);
//            } catch (ZLNetworkException e) {
//                // we do ignore errors in opensearch description loading/parsing
//                e.printStackTrace();
//            }
//        }
//
//        if (!descriptions.isEmpty()) {
//            // TODO: May be do not use '%s'??? Use Description instead??? (this needs to rewrite SEARCH engine logic a little)
//            final OpenSearchDescription d = descriptions.get(0);
//            setUrl(UrlInfo.Type.Search, d.makeQuery("%s"), d.Mime);
//        } else {
//            setUrl(UrlInfo.Type.Search, null, null);
//        }
//        if (error != null) {
//            throw error;
//        }
//    }
//}
