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

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource

internal class UserRegistrationActivity : RegistrationActivity() {

    private fun findTextView(resourceId: Int): TextView = findViewById(resourceId)

    private fun findButton(resourceId: Int): Button = findViewById(resourceId)

    private fun getViewText(resourceId: Int): String = findTextView(resourceId).text.toString().trim()

    private fun setViewText(resourceId: Int, text: String) {
        findTextView(resourceId).text = text
    }

    private fun setViewTextFromResource(resourceId: Int, fbResourceKey: String) {
        setViewText(resourceId, myResource!!.getResource(fbResourceKey).value)
    }

    private fun setErrorMessage(errorMessage: String) {
        val errorLabel = findTextView(R.id.lr_user_registration_error)
        errorLabel.visibility = View.VISIBLE
        errorLabel.text = errorMessage
    }

    private fun setErrorMessageFromResource(resourceKey: String) {
        setErrorMessage(ZLResource.resource("dialog").getResource("networkError").getResource(resourceKey).value)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        myResource = ZLResource.resource("dialog").getResource("litresUserRegistration")

        setContentView(R.layout.lr_user_registration)

        title = myResource!!.getResource("title").value
        setViewTextFromResource(R.id.lr_user_registration_login_text, "login")
        setViewTextFromResource(R.id.lr_user_registration_password_text, "password")
        setViewTextFromResource(R.id.lr_user_registration_confirm_password_text, "confirmPassword")
        setViewTextFromResource(R.id.lr_user_registration_email_text, "email")

        val errorLabel = findTextView(R.id.lr_user_registration_error)
        errorLabel.visibility = View.GONE
        errorLabel.text = ""

        val buttonResource = ZLResource.resource("dialog").getResource("button")
        val buttonsView = findViewById<View>(R.id.lr_user_registration_buttons)
        val okButton = buttonsView.findViewById<Button>(R.id.ok_button)
        val cancelButton = buttonsView.findViewById<Button>(R.id.cancel_button)
        val emailControl = findViewById<View>(R.id.lr_user_registration_email_control)
        val emailTextView = emailControl.findViewById<TextView>(R.id.lr_email_edit)

        okButton.text = buttonResource.getResource("ok").value
        okButton.setOnClickListener {
            val userName = getViewText(R.id.lr_user_registration_login)
            val password = getViewText(R.id.lr_user_registration_password)
            val confirmPassword = getViewText(R.id.lr_user_registration_confirm_password)
            val email = emailTextView.text.toString().trim()

            if (userName.isEmpty()) {
                setErrorMessageFromResource("usernameNotSpecified")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                setErrorMessageFromResource("passwordsDoNotMatch")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                setErrorMessageFromResource("passwordNotSpecified")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                setErrorMessageFromResource("emailNotSpecified")
                return@setOnClickListener
            }
            val atPos = email.indexOf("@")
            if (atPos == -1 || email.indexOf(".", atPos) == -1) {
                setErrorMessageFromResource("invalidEMail")
                return@setOnClickListener
            }

            val runnable = RegistrationNetworkRunnable(userName, password, email)
            runWithMessage("registerUser", runnable, object : PostRunnable {
                override fun run(exception: ZLNetworkException?) {
                    if (exception == null) {
                        reportSuccess(userName, password, runnable.xmlReader.sid)
                        finish()
                    } else {
                        setErrorMessage(exception.message ?: "Unknown error")
                    }
                }
            })
        }
        cancelButton.text = buttonResource.getResource("cancel").value
        cancelButton.setOnClickListener { finish() }

        setupEmailControl(emailControl, null)
    }
}
