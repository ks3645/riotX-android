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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.DividerItem_
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.profiles.profileItemAction
import im.vector.riotx.core.epoxy.profiles.profileItemSection
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class RoomProfileController @Inject constructor(private val stringProvider: StringProvider)
    : TypedEpoxyController<RoomProfileViewState>() {

    var callback: Callback? = null

    interface Callback {
        fun onLearnMoreClicked()
        fun onMemberListClicked()
        fun onNotificationsClicked()
        fun onUploadsClicked()
        fun onSettingsClicked()
        fun onLeaveRoomClicked()
    }

    override fun buildModels(data: RoomProfileViewState?) {
        if (data == null) {
            return
        }

        val roomSummary = data.roomSummary()

        // Security
        buildSection(stringProvider.getString(R.string.room_profile_section_security))
        val learnMoreSubtitle = if (data.isEncrypted) {
            R.string.room_profile_encrypted_subtitle
        } else {
            R.string.room_profile_not_encrypted_subtitle
        }
        buildAction(
                id = "learn_more",
                title = stringProvider.getString(R.string.room_profile_section_security_learn_more),
                subtitle = stringProvider.getString(learnMoreSubtitle),
                action = { callback?.onLearnMoreClicked() }
        )

        // More
        buildSection(stringProvider.getString(R.string.room_profile_section_more))
        buildAction(
                id = "settings",
                title = stringProvider.getString(R.string.room_profile_section_more_settings),
                icon = R.drawable.ic_room_profile_settings,
                action = { callback?.onSettingsClicked() }
        )
        buildAction(
                id = "notifications",
                title = stringProvider.getString(R.string.room_profile_section_more_notifications),
                icon = R.drawable.ic_room_profile_notification,
                action = { callback?.onNotificationsClicked() }
        )
        val numberOfMembers = roomSummary?.joinedMembersCount?.toString() ?: "-"
        buildAction(
                id = "member_list",
                title = stringProvider.getString(R.string.room_profile_section_more_member_list, numberOfMembers),
                icon = R.drawable.ic_room_profile_member_list,
                action = { callback?.onMemberListClicked() }
        )
        buildAction(
                id = "uploads",
                title = stringProvider.getString(R.string.room_profile_section_more_uploads),
                icon = R.drawable.ic_room_profile_uploads,
                action = { callback?.onUploadsClicked() }
        )
        buildAction(
                id = "leave",
                title = stringProvider.getString(R.string.room_profile_section_more_leave),
                divider = false,
                destructive = true,
                action = { callback?.onLeaveRoomClicked() }
        )
    }

    private fun buildSection(title: String) {
        profileItemSection {
            id("section_$title")
            title(title)
        }
    }

    private fun buildAction(
            id: String,
            title: String,
            subtitle: String? = null,
            @DrawableRes icon: Int = 0,
            destructive: Boolean = false,
            divider: Boolean = true,
            action: () -> Unit
    ) {

        profileItemAction {
            iconRes(icon)
            id("action_$id")
            subtitle(subtitle)
            destructive(destructive)
            title(title)
            listener { _ ->
                action()
            }
        }

        DividerItem_()
                .id("divider_$title")
                .addIf(divider, this)
    }


}