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

package org.geometerplus.android.fbreader.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkDatabase
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.options.Config

object Util : UserRegistrationConstants {
    const val SIGNIN_ACTION = "android.fbreader.action.network.SIGNIN"
    const val ADD_CATALOG_ACTION = "android.fbreader.action.ADD_OPDS_CATALOG"
    const val ADD_CATALOG_URL_ACTION = "android.fbreader.action.ADD_OPDS_CATALOG_URL"
    const val EDIT_CATALOG_ACTION = "android.fbreader.action.EDIT_OPDS_CATALOG"
    internal const val AUTHORISATION_ACTION = "android.fbreader.action.network.AUTHORISATION"
    internal const val TOPUP_ACTION = "android.fbreader.action.network.TOPUP"
    internal const val EXTRA_CATALOG_ACTION = "android.fbreader.action.network.EXTRA_CATALOG"

    @JvmStatic
    fun intentByLink(intent: Intent, link: INetworkLink?): Intent {
        if (link != null) {
            intent.data = Uri.parse(link.getUrl(UrlInfo.Type.Catalog))
        }
        return intent
    }

    @JvmStatic
    fun networkLibrary(context: Context): NetworkLibrary {
        return NetworkLibrary.Instance(Paths.systemInfo(context))
    }

    @JvmStatic
    fun initLibrary(activity: Activity, nc: ZLNetworkContext, action: Runnable?) {
        Config.Instance()?.runOnConnect {
            UIUtil.wait("loadingNetworkLibrary", {
                val library = networkLibrary(activity)
                if (NetworkDatabase.Instance() == null) {
                    SQLiteNetworkDatabase(activity.application, library)
                }

                if (!library.isInitialized()) {
                    try {
                        library.initialize(nc)
                    } catch (e: ZLNetworkException) {
                    }
                }
                action?.run()
            }, activity)
        }
    }

    @JvmStatic
    fun authorisationIntent(link: INetworkLink, activity: Activity, cls: Class<out Activity>): Intent {
        val intent = Intent(activity, cls)
        intent.putExtra(UserRegistrationConstants.CATALOG_URL, link.getUrl(UrlInfo.Type.Catalog))
        intent.putExtra(UserRegistrationConstants.SIGNIN_URL, link.getUrl(UrlInfo.Type.SignIn))
        intent.putExtra(UserRegistrationConstants.SIGNUP_URL, link.getUrl(UrlInfo.Type.SignUp))
        intent.putExtra(UserRegistrationConstants.RECOVER_PASSWORD_URL, link.getUrl(UrlInfo.Type.RecoverPassword))
        return intent
    }

    @JvmStatic
    fun runAuthenticationDialog(activity: Activity, link: INetworkLink, onSuccess: Runnable?) {
        val mgr = link.authenticationManager()

        val intent = intentByLink(Intent(activity, AuthenticationActivity::class.java), link)
        AuthenticationActivity.registerRunnable(intent, onSuccess)
        intent.putExtra(AuthenticationActivity.USERNAME_KEY, mgr?.getUserName())
        intent.putExtra(AuthenticationActivity.SCHEME_KEY, "https")
        intent.putExtra(AuthenticationActivity.CUSTOM_AUTH_KEY, true)
        activity.startActivity(intent)
    }

    @JvmStatic
    fun isOurLink(url: String): Boolean {
        return try {
            url.toUri().host?.endsWith(".fbreader.org") ?: false
        } catch (t: Throwable) {
            false
        }
    }

    @JvmStatic
    fun openInBrowser(activity: Activity, url: String?) {
        if (url != null) {
            val rewrittenUrl = networkLibrary(activity).rewriteUrl(url, true)
            activity.startActivity(Intent(Intent.ACTION_VIEW, rewrittenUrl.toUri()))
        }
    }

    @JvmStatic
    fun doDownloadBook(activity: Activity, book: NetworkBookItem, demo: Boolean) {
        val resolvedType = if (demo) UrlInfo.Type.BookDemo else UrlInfo.Type.Book
        val ref = book.reference(resolvedType)
        if (ref != null) {
            activity.startService(
                Intent(Intent.ACTION_VIEW, Uri.parse(ref.url), activity.applicationContext, BookDownloaderService::class.java)
                    .putExtra(BookDownloaderService.Key.BOOK_MIME, ref.mime.toString())
                    .putExtra(BookDownloaderService.Key.BOOK_KIND, resolvedType)
                    .putExtra(BookDownloaderService.Key.CLEAN_URL, ref.cleanUrl())
                    .putExtra(BookDownloaderService.Key.BOOK_TITLE, book.title)
            )
        }
    }

    @JvmStatic
    fun rewriteUri(uri: Uri?): Uri? {
        if (uri == null) {
            return null
        }

        if ("http" == uri.scheme &&
            "www.litres.ru" == uri.host &&
            "/pages/biblio_book/" == uri.path
        ) {
            val bookId = uri.getQueryParameter("art")
            if (!bookId.isNullOrEmpty()) {
                return Uri.parse("litres-book://data.fbreader.org/catalogs/litres2/full.php5?id=$bookId")
            }
        }
        return uri
    }
}
