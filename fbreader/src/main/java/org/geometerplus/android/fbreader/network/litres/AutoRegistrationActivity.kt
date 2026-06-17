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
import org.geometerplus.fbreader.network.authentication.litres.LitResNetworkRequest
import org.geometerplus.fbreader.network.authentication.litres.LitResPasswordRecoveryXMLReader
import org.geometerplus.fbreader.network.authentication.litres.LitResRegisterUserXMLReader
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource

/*
 * Algorithm:
 *    step 0. Select first e-mail from the list
 *       a. Success => 1
 *       b. E-mails list is empty => 4
 *    step 1. try to login with auto-generated username/password
 *       a. Success => 'login successful' dialog => finish
 *       b. Authentication failure => 2
 *       c. Any other problem => show error message => finish
 *    step 2. try to register with given e-mail + auto-generated username/password
 *       a. Success => 'registration successful' dialog => finish
 *       b. Username already in used => 3
 *       c. E-address already in use => 3
 *       d. Any other problem => show error message => finish
 *    step 3. 'e-address already in use' dialog, choices:
 *       a. Sign in => standard sign in dialog
 *       b. Send password => 5
 *       c. Select other email => 4
 *    step 4. 'email selection' dialog
 *       a. Ok => 1
 *       b. Cancel => finish
 *    step 5. send password
 *       a. Success => 'password sent dialog' => finish
 *       b. Failure => show error message => finish
 */

internal class AutoRegistrationActivity : RegistrationActivity() {

    private val myUtil = RegistrationUtils(this)
    private val myFinishListener = View.OnClickListener { finish() }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val dialogResource = ZLResource.resource("dialog")
        val buttonResource = dialogResource.getResource("button")

        myResource = dialogResource.getResource("litresAutoSignIn")
        setContentView(R.layout.lr_auto_registration)
        title = myResource!!.getResource("title").value

        okButton.text = buttonResource.getResource("ok").value
        cancelButton.text = buttonResource.getResource("cancel").value
        textArea.visibility = View.GONE
        actionSignIn.visibility = View.GONE
        actionAnotherEmail.visibility = View.GONE
        actionRecover.visibility = View.GONE
        emailControl.visibility = View.GONE
        buttons.visibility = View.GONE

        startAutoRegistration()
    }

    // step 0
    private fun startAutoRegistration() {
        val email = myUtil.firstEMail()
        if (email != null) {
            runAutoLogin(email)
        } else {
            runEmailSelectionDialog(null)
        }
    }

    // step 1
    private fun runAutoLogin(email: String) {
        val username = myUtil.getAutoLogin(email)!!
        val password = myUtil.getAutoPassword()

        val runnable = SignInNetworkRunnable(username, password)
        runWithMessage("autoSignIn", runnable, object : PostRunnable {
            override fun run(e: ZLNetworkException?) {
                if (e == null) {
                    reportSuccess(username, password, runnable.xmlReader.sid)
                    showFinalMessage(
                        myResource!!.getResource("signedIn").value.replace("%s", email)
                    )
                } else if (e is ZLNetworkAuthenticationException) {
                    runAutoRegistraion(email)
                } else {
                    showErrorMessage(e)
                }
            }
        })
    }

    // step 2
    private fun runAutoRegistraion(email: String) {
        val username = myUtil.getAutoLogin(email)!!
        val password = myUtil.getAutoPassword()

        val runnable = RegistrationNetworkRunnable(username, password, email)
        runWithMessage("autoSignIn", runnable, object : PostRunnable {
            override fun run(e: ZLNetworkException?) {
                if (e == null) {
                    reportSuccess(username, password, runnable.xmlReader.sid)
                    showFinalMessage(
                        myResource!!.getResource("registrationSuccessful").value.replace("%s", email)
                    )
                } else if (e is LitResRegisterUserXMLReader.AlreadyInUseException) {
                    runEmailAlreadyInUseDialog(email)
                } else {
                    showErrorMessage(e)
                }
            }
        })
    }

    // step 3
    private fun runEmailAlreadyInUseDialog(email: String) {
        val actionResource = myResource!!.getResource("actions")
        textArea.visibility = View.VISIBLE
        textArea.text = actionResource.getResource("title").value.replace("%s", email)
        val rbListener = View.OnClickListener { view ->
            val rb = view as android.widget.RadioButton
            actionSignIn.isChecked = false
            actionAnotherEmail.isChecked = false
            actionRecover.isChecked = false
            rb.isChecked = true
            okButton.isEnabled = true
        }
        actionSignIn.visibility = View.GONE

        actionAnotherEmail.visibility = View.VISIBLE
        actionAnotherEmail.text = actionResource.getResource("anotherEmail").value
        actionAnotherEmail.setOnClickListener(rbListener)
        actionRecover.visibility = View.VISIBLE
        actionRecover.text = actionResource.getResource("recover").value
        actionRecover.setOnClickListener(rbListener)
        emailControl.visibility = View.GONE
        buttons.visibility = View.VISIBLE
        okButton.visibility = View.VISIBLE
        okButton.isEnabled = false
        okButton.setOnClickListener {
            when {
                actionSignIn.isChecked -> {
                    // TODO: implement
                }
                actionAnotherEmail.isChecked -> runEmailSelectionDialog(email)
                actionRecover.isChecked -> recoverAccountInformation(email)
            }
        }
        cancelButton.visibility = View.VISIBLE
        cancelButton.setOnClickListener(myFinishListener)
    }

    // step 4
    private fun runEmailSelectionDialog(email: String?) {
        textArea.visibility = View.VISIBLE
        textArea.text = myResource!!.getResource("email").value
        actionSignIn.visibility = View.GONE
        actionAnotherEmail.visibility = View.GONE
        actionRecover.visibility = View.GONE
        emailControl.visibility = View.VISIBLE
        setupEmailControl(emailControl, email)
        buttons.visibility = View.VISIBLE
        okButton.visibility = View.VISIBLE
        okButton.setOnClickListener {
            runAutoLogin(emailTextView.text.toString().trim())
        }
        cancelButton.visibility = View.VISIBLE
        cancelButton.setOnClickListener(myFinishListener)
    }

    // step 5
    private fun recoverAccountInformation(email: String) {
        System.err.println("recoverAccountInformation 0")
        val xmlReader = LitResPasswordRecoveryXMLReader()

        val runnable = object : NetworkRunnable {
            override fun run() {
                System.err.println("recoverAccountInformation 1")
                val request = LitResNetworkRequest(myRecoverPasswordURL!!, xmlReader)
                request.addPostParameter("mail", email)
                myNetworkContext.perform(request)
            }
        }
        runWithMessage("recoverPassword", runnable, object : PostRunnable {
            override fun run(e: ZLNetworkException?) {
                System.err.println("recoverAccountInformation 2")
                if (e == null) {
                    System.err.println("recoverAccountInformation 3")
                    showFinalMessage(
                        myResource!!.getResource("passwordSent").value.replace("%s", email)
                    )
                } else {
                    System.err.println("recoverAccountInformation 4")
                    showErrorMessage(e)
                }
            }
        })
        System.err.println("recoverAccountInformation 5")
        System.err.println("recoverAccountInformation 6")
    }

    private val textArea: TextView
        get() = findViewById(R.id.lr_auto_registration_text)

    private val actionSignIn: android.widget.RadioButton
        get() = findViewById(R.id.lr_auto_registration_action_signin)

    private val actionAnotherEmail: android.widget.RadioButton
        get() = findViewById(R.id.lr_auto_registration_action_change_email)

    private val actionRecover: android.widget.RadioButton
        get() = findViewById(R.id.lr_auto_registration_action_recover)

    private val emailControl: View
        get() = findViewById(R.id.lr_auto_registration_email_control)

    private val emailTextView: TextView
        get() = emailControl.findViewById(R.id.lr_email_edit)

    private val buttons: View
        get() = findViewById(R.id.lr_auto_registration_buttons)

    private val okButton: Button
        get() = buttons.findViewById(R.id.ok_button)

    private val cancelButton: Button
        get() = buttons.findViewById(R.id.cancel_button)

    private fun showFinalMessage(message: String) {
        textArea.visibility = View.VISIBLE
        textArea.text = message
        actionSignIn.visibility = View.GONE
        actionAnotherEmail.visibility = View.GONE
        actionRecover.visibility = View.GONE
        emailControl.visibility = View.GONE
        buttons.visibility = View.VISIBLE
        okButton.visibility = View.VISIBLE
        okButton.setOnClickListener(myFinishListener)
        cancelButton.visibility = View.GONE
    }

    private fun showErrorMessage(exception: ZLNetworkException) {
        exception.printStackTrace()
        showFinalMessage(exception.message ?: "Unknown error")
    }

    private companion object {
        private val myRecoverPasswordURL: String? = null
    }
}
