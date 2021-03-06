/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.signout

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.util.Cancelable

/**
 * This interface defines a method to sign out, or to renew the token. It's implemented at the session level.
 */
interface SignOutService {

    /**
     * Ask the homeserver for a new access token.
     * The same deviceId will be used
     */
    fun signInAgain(password: String,
                    callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the session with credentials received after SSO
     */
    fun updateCredentials(credentials: Credentials,
                          callback: MatrixCallback<Unit>): Cancelable

    /**
     * Sign out, and release the session, clear all the session data, including crypto data
     * @param sigOutFromHomeserver true if the sign out request has to be done
     */
    fun signOut(sigOutFromHomeserver: Boolean,
                callback: MatrixCallback<Unit>): Cancelable
}
