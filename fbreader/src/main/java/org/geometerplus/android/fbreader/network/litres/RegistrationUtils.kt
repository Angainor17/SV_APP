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

import android.content.Context
import android.net.wifi.WifiManager
import java.lang.reflect.InvocationTargetException
import java.math.BigInteger
import java.security.SecureRandom

class RegistrationUtils(private val myContext: Context) {
    private var myEMails: List<String>? = null

    fun getAutoLogin(email: String?): String? {
        return email?.let { "fbreader-auto-" + it.replace(".", "-").replace("@", "-at-") }
    }

    fun getAutoPassword(): String {
        try {
            val wifi = myContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mac = wifi.connectionInfo.macAddress
            if (mac.length > 3) {
                return "XXX$mac"
            }
        } catch (e: Exception) {
        }
        return BigInteger(50, SecureRandom()).toString(32)
    }

    fun eMails(): List<String> {
        collectEMails()
        return myEMails!!
    }

    fun firstEMail(): String? {
        collectEMails()
        return if (myEMails!!.isNotEmpty()) myEMails!![0] else null
    }

    private fun collectEMails() {
        if (myEMails != null) {
            return
        }
        try {
            val clsAccountManager = Class.forName("android.accounts.AccountManager")
            val clsAccount = Class.forName("android.accounts.Account")

            val methAccountManagerGet = clsAccountManager.getMethod("get", Context::class.java)
            val methAccountManagerGetAccountsByType = clsAccountManager.getMethod("getAccountsByType", String::class.java)
            val fldAccountName = clsAccount.getField("name")

            if (methAccountManagerGet.returnType == clsAccountManager
                && methAccountManagerGetAccountsByType.returnType.componentType == clsAccount
                && fldAccountName.type == String::class.java
            ) {
                val mgr = methAccountManagerGet.invoke(null, myContext)
                val accountsByType = methAccountManagerGetAccountsByType.invoke(mgr, "com.google") as Array<*>
                myEMails = ArrayList(accountsByType.size)
                for (a in accountsByType) {
                    val value = fldAccountName[a] as String
                    if (value.isNotEmpty()) {
                        (myEMails as ArrayList).add(value)
                    }
                }
                return
            }
        } catch (e: ClassNotFoundException) {
        } catch (e: NoSuchMethodException) {
        } catch (e: NoSuchFieldException) {
        } catch (e: IllegalAccessException) {
        } catch (e: IllegalArgumentException) {
        } catch (e: InvocationTargetException) {
        }
        myEMails = emptyList()
    }
}
