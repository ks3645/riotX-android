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

@file:Suppress("DEPRECATION")

package im.vector.riotx.features.roomprofile

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.snackbar.Snackbar
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.list.RoomListAction
import im.vector.riotx.features.home.room.list.actions.RoomListActionsArgs
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_profile.*
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class RoomProfileArgs(
        val roomId: String
) : Parcelable

class RoomProfileFragment @Inject constructor(
        private val roomProfileController: RoomProfileController,
        private val avatarRenderer: AvatarRenderer,
        val roomProfileViewModelFactory: RoomProfileViewModel.Factory
) : VectorBaseFragment(), RoomProfileController.Callback {

    private var progress: ProgressDialog? = null
    private val roomProfileArgs: RoomProfileArgs by args()
    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private val roomProfileViewModel: RoomProfileViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_profile

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        setupToolbar(roomProfileToolbar)
        setupRecyclerView()
        roomProfileViewModel.viewEvents
                .observe()
                .subscribe {
                    progress?.dismiss()
                    when (it) {
                        RoomProfileViewEvents.Loading            -> showLoading()
                        RoomProfileViewEvents.OnLeaveRoomSuccess -> onLeaveRoom()
                        is RoomProfileViewEvents.Failure         -> showError(it.throwable)
                    }
                }
                .disposeOnDestroyView()

        sharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
    }

    private fun handleQuickActions(action: RoomListQuickActionsSharedAction) = when (action) {
        is RoomListQuickActionsSharedAction.NotificationsAllNoisy     -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES_NOISY))
        }
        is RoomListQuickActionsSharedAction.NotificationsAll          -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES))
        }
        is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MENTIONS_ONLY))
        }
        is RoomListQuickActionsSharedAction.NotificationsMute         -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MUTE))
        }
        else                                                          -> Timber.v("$action not handled")
    }

    private fun onLeaveRoom() {
        vectorBaseActivity.finish()
    }

    private fun showError(throwable: Throwable) {
        vectorBaseActivity.showSnackbar(errorFormatter.toHumanReadable(throwable))
    }

    private fun showLoading() {
        progress = ProgressDialog(requireContext()).apply {
            setMessage(getString(R.string.room_profile_leaving_room))
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }
    }

    private fun setupRecyclerView() {
        roomProfileController.callback = this
        roomProfileRecyclerView.setHasFixedSize(true)
        roomProfileRecyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        roomProfileRecyclerView.adapter = roomProfileController.adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        roomProfileRecyclerView.adapter = null
    }

    override fun invalidate() = withState(roomProfileViewModel) { state ->
        state.roomSummary()?.let {
            if (it.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                roomProfileNameView.text = it.displayName
                roomProfileNameView2.text = it.displayName
                // Use canonical alias when PR with alias management will be merged
                roomProfileAliasView.text = it.roomId
                roomProfileTopicView.setTextOrHide(it.topic)
                avatarRenderer.render(it.toMatrixItem(), roomProfileAvatarView)
            }
        }
        roomProfileController.setData(state)
    }

    // RoomProfileController.Callback

    override fun onLearnMoreClicked() {
        vectorBaseActivity.notImplemented()
    }

    override fun onMemberListClicked() {
        vectorBaseActivity.notImplemented("See room member list")
    }

    override fun onSettingsClicked() {
        vectorBaseActivity.notImplemented("See Room settings")
    }

    override fun onNotificationsClicked() {
        RoomListQuickActionsBottomSheet
                .newInstance(roomProfileArgs.roomId, RoomListActionsArgs.Mode.NOTIFICATIONS)
                .show(childFragmentManager, "ROOM_PROFILE_NOTIFICATIONS")
    }

    override fun onUploadsClicked() {
        vectorBaseActivity.notImplemented("See uploads")
    }

    override fun onLeaveRoomClicked() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(R.string.room_participants_leave_prompt_msg)
                .setPositiveButton(R.string.leave) { _, _ ->
                    roomProfileViewModel.handle(RoomProfileAction.LeaveRoom)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }
}