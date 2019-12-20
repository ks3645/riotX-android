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
 *
 */

package im.vector.riotx.features.roomprofile

import android.content.Context
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import im.vector.riotx.R
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity

class RoomProfileActivity : VectorBaseActivity(), ToolbarConfigurable {

    companion object {

        private const val EXTRA_ROOM_PROFILE_ARGS = "EXTRA_ROOM_PROFILE_ARGS"

        fun newIntent(context: Context, roomId: String): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomProfileActivity::class.java).apply {
                putExtra(EXTRA_ROOM_PROFILE_ARGS, roomProfileArgs)
            }
        }
    }

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val roomProfileArgs: RoomProfileArgs = intent?.extras?.getParcelable(EXTRA_ROOM_PROFILE_ARGS)
                                                   ?: return
            addFragment(R.id.simpleFragmentContainer, RoomProfileFragment::class.java, roomProfileArgs)
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

}