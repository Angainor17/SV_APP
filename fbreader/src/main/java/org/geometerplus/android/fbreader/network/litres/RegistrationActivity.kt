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

package org.geometerplus.android.fbreader.network.litres

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.android.fbreader.network.UserRegistrationConstants
import org.geometerplus.android.fbreader.network.Util
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext
import org.geometerplus.fbreader.network.authentication.litres.LitResLoginXMLReader
import org.geometerplus.fbreader.network.authentication.litres.LitResNetworkRequest
import org.geometerplus.fbreader.network.authentication.litres.LitResRegisterUserXMLReader
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource

internal abstract class RegistrationActivity : Activity(), UserRegistrationConstants {

    protected val myNetworkContext = ActivityNetworkContext(this)

    protected var myResource: ZLResource? = null
    protected var myCatalogURL: String? = null
    protected var myRecoverPasswordURL: String? = null
    private var mySignInURL: String? = null
    private var mySignUpURL: String? = null

    protected fun reportSuccess(username: String, password: String, sid: String?) {
        val data = Intent(Util.SIGNIN_ACTION)
        data.putExtra(UserRegistrationConstants.USER_REGISTRATION_USERNAME, username)
        data.putExtra(UserRegistrationConstants.USER_REGISTRATION_PASSWORD, password)
        data.putExtra(UserRegistrationConstants.USER_REGISTRATION_LITRES_SID, sid)
        data.putExtra(UserRegistrationConstants.CATALOG_URL, myCatalogURL)
        sendBroadcast(data)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val intent = intent
        myCatalogURL = intent.getStringExtra(UserRegistrationConstants.CATALOG_URL)
        mySignInURL = intent.getStringExtra(UserRegistrationConstants.SIGNIN_URL)
        mySignUpURL = intent.getStringExtra(UserRegistrationConstants.SIGNUP_URL)
        myRecoverPasswordURL = intent.getStringExtra(UserRegistrationConstants.RECOVER_PASSWORD_URL)
    }

    @Synchronized
    protected fun runWithMessage(key: String, action: NetworkRunnable, postAction: PostRunnable) {
        val message = ZLResource.resource("dialog").getResource("waitMessage").getResource(key).value
        val progress = ProgressDialog.show(this, null, message, true, false)

        Thread {
            var error: ZLNetworkException? = null
            try {
                action.run()
            } catch (e: ZLNetworkException) {
                error = e
            }
            runOnUiThread {
                progress.dismiss()
                postAction.run(error)
            }
        }.start()
    }

    protected fun setupEmailControl(emailControl: View, eMailToSkip: String?) {
        val emailListButton = emailControl.findViewById<Button>(R.id.lr_email_button)
        val emailTextView = emailControl.findViewById<TextView>(R.id.lr_email_edit)

        val emails = RegistrationUtils(applicationContext).eMails()

        emailListButton.visibility = if (emails.size > 1) View.VISIBLE else View.GONE
        if (emails.isNotEmpty()) {
            emailTextView.text = emails[0]
            for (e in emails) {
                if (e != eMailToSkip) {
                    emailTextView.text = e
                    break
                }
            }

            val listener = DialogInterface.OnClickListener { dialog, which ->
                if (which >= 0 && which < emails.size) {
                    emailTextView.text = emails[which]
                }
                dialog.dismiss()
            }

            emailListButton.setOnClickListener {
                val selectedEmail = emailTextView.text.toString().trim()
                val selected = emails.indexOf(selectedEmail)
                val buttonResource = ZLResource.resource("dialog").getResource("button")
                val dialog = AlertDialog.Builder(this@RegistrationActivity)
                    .setSingleChoiceItems(emails.toTypedArray(), selected, listener)
                    .setTitle(myResource!!.getResource("email").value)
                    .setNegativeButton(buttonResource.getResource("cancel").value, null)
                    .create()

                dialog.show()
            }
        }
    }

    protected interface NetworkRunnable {
        @Throws(ZLNetworkException::class)
        fun run()
    }

    protected interface PostRunnable {
        fun run(exception: ZLNetworkException?)
    }

    protected inner class RegistrationNetworkRunnable(
        private val username: String,
        private val password: String,
        private val email: String
    ) : NetworkRunnable {

        val xmlReader = LitResRegisterUserXMLReader()

        @Throws(ZLNetworkException::class)
        override fun run() {
            val request = LitResNetworkRequest(mySignUpURL!!, xmlReader)
            request.addPostParameter("new_login", username)
            request.addPostParameter("new_pwd1", password)
            request.addPostParameter("mail", email)
            myNetworkContext.perform(request)
        }
    }

    protected inner class SignInNetworkRunnable(
        private val username: String,
        private val password: String
    ) : NetworkRunnable {

        val xmlReader = LitResLoginXMLReader()

        @Throws(ZLNetworkException::class)
        override fun run() {
            val request = LitResNetworkRequest(mySignInURL!!, xmlReader)
            request.addPostParameter("login", username)
            request.addPostParameter("pwd", password)
            myNetworkContext.perform(request)
        }
    }
}
