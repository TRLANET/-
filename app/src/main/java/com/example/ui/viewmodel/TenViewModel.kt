package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.data.repository.TenRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TenViewModel(
  private val repository: TenRepository,
  val currentUserId: String,
  private val initialDisplayName: String?,
  private val initialEmail: String?,
  private val initialPhotoUrl: String?,
) : ViewModel() {

  private val _actionMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val actionMessage: SharedFlow<String> = _actionMessage.asSharedFlow()

  private val _selectedFeedFilter = MutableStateFlow("الكل")
  val selectedFeedFilter: StateFlow<String> = _selectedFeedFilter.asStateFlow()

  private val _activeCommentsPost = MutableStateFlow<Post?>(null)
  val activeCommentsPost: StateFlow<Post?> = _activeCommentsPost.asStateFlow()

  private val _activeConversation = MutableStateFlow<Conversation?>(null)
  val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureUserProfiles(
        displayName = initialDisplayName,
        email = initialEmail,
        photoUrl = initialPhotoUrl,
      ).onFailure { error ->
        Log.w(TAG, "Failed to ensure initial profile", error)
      }
    }
  }

  val myProfile: StateFlow<PublicUser?> = repository.observeMyProfile(currentUserId)
    .catch { error ->
      logStreamError("profile", error)
      emit(null)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val postsState: StateFlow<UiState<List<Post>>> = repository.observePublicPosts()
    .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
    .catch { error ->
      val rootError = unwrapFirestoreOrThrowCancellation(error)
      Log.w(TAG, "Error observing posts", rootError)
      emit(UiState.Error(rootError.message ?: "تعذر تحميل المنشورات"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val discoverableUsers: StateFlow<List<PublicUser>> = repository.observeDiscoverableUsers()
    .catch { error ->
      logStreamError("discoverableUsers", error)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val likedPostIds: StateFlow<Set<String>> = repository.observeMyLikedPostIds(currentUserId)
    .catch { error ->
      logStreamError("likedPostIds", error)
      emit(emptySet())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptySet(),
    )

  val bookmarkedPostIds: StateFlow<Set<String>> = repository.observeMyBookmarkedPostIds(currentUserId)
    .catch { error ->
      logStreamError("bookmarkedPostIds", error)
      emit(emptySet())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptySet(),
    )

  val followingIds: StateFlow<Set<String>> = repository.observeFollowingIds(currentUserId)
    .catch { error ->
      logStreamError("followingIds", error)
      emit(emptySet())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptySet(),
    )

  val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
    .catch { error ->
      logStreamError("conversations", error)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val activeComments: StateFlow<List<PostComment>> = _activeCommentsPost
    .flatMapLatest { post ->
      if (post == null) {
        flowOf(emptyList())
      } else {
        repository.observeComments(post.id).catch { error ->
          logStreamError("comments", error)
          emit(emptyList())
        }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val activeMessages: StateFlow<List<DirectMessage>> = _activeConversation
    .flatMapLatest { conversation ->
      if (conversation == null) {
        flowOf(emptyList())
      } else {
        repository.observeMessages(conversation.id).catch { error ->
          logStreamError("messages", error)
          emit(emptyList())
        }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  fun setFeedFilter(filter: String) {
    _selectedFeedFilter.value = filter
  }

  fun createPost(content: String, tag: String, cardStyle: String, onSuccess: () -> Unit) {
    val author = myProfile.value ?: PublicUser(
      id = currentUserId,
      userId = currentUserId,
      displayName = initialDisplayName ?: "عضو ТЕНЬ",
      username = "ten_${currentUserId.take(6).lowercase()}",
      photoUrl = initialPhotoUrl ?: "",
    )
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.createPost(content, tag, cardStyle, author)
      _isBusy.value = false
      result.onSuccess {
        _actionMessage.tryEmit("تم نشر منشورك في ТЕНЬ")
        onSuccess()
      }.onFailure { error ->
        _actionMessage.tryEmit(error.localizedMessage ?: "تعذر نشر المنشور")
      }
    }
  }

  fun deletePost(post: Post) {
    if (post.authorId != currentUserId) return
    val currentCount = myProfile.value?.postsCount ?: 0L
    viewModelScope.launch {
      repository.deletePost(post.id, currentCount)
        .onSuccess { _actionMessage.tryEmit("تم حذف المنشور") }
        .onFailure { _actionMessage.tryEmit("تعذر حذف المنشور") }
    }
  }

  fun toggleLike(post: Post) {
    val currentlyLiked = likedPostIds.value.contains(post.id)
    viewModelScope.launch {
      repository.toggleLike(post.id, currentlyLiked)
        .onFailure { _actionMessage.tryEmit("تعذر تحديث الإعجاب") }
    }
  }

  fun toggleBookmark(post: Post) {
    val currentlyBookmarked = bookmarkedPostIds.value.contains(post.id)
    viewModelScope.launch {
      repository.toggleBookmark(post.id, currentlyBookmarked)
        .onSuccess {
          _actionMessage.tryEmit(if (currentlyBookmarked) "أزيل من المحفوظات" else "تم حفظ المنشور")
        }
        .onFailure { _actionMessage.tryEmit("تعذر حفظ المنشور") }
    }
  }

  fun openComments(post: Post) {
    _activeCommentsPost.value = post
  }

  fun closeComments() {
    _activeCommentsPost.value = null
  }

  fun addComment(postId: String, content: String, onSuccess: () -> Unit) {
    val author = myProfile.value ?: PublicUser(
      id = currentUserId,
      userId = currentUserId,
      displayName = initialDisplayName ?: "عضو ТЕНЬ",
      username = "ten_${currentUserId.take(6).lowercase()}",
      photoUrl = initialPhotoUrl ?: "",
    )
    viewModelScope.launch {
      repository.addComment(postId, content, author)
        .onSuccess { onSuccess() }
        .onFailure { _actionMessage.tryEmit(it.localizedMessage ?: "تعذر إرسال التعليق") }
    }
  }

  fun toggleFollow(targetUserId: String) {
    if (targetUserId == currentUserId) return
    val currentlyFollowing = followingIds.value.contains(targetUserId)
    viewModelScope.launch {
      repository.toggleFollow(targetUserId, currentlyFollowing)
        .onFailure { _actionMessage.tryEmit("تعذر تحديث المتابعة") }
    }
  }

  fun updateProfile(displayName: String, username: String, bio: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.updateMyProfile(displayName, username, bio)
      _isBusy.value = false
      result.onSuccess {
        _actionMessage.tryEmit("تم تحديث الملف الشخصي")
        onSuccess()
      }.onFailure {
        _actionMessage.tryEmit(it.localizedMessage ?: "تعذر تحديث الملف الشخصي")
      }
    }
  }

  fun openConversationWithUser(otherUser: PublicUser, onOpened: () -> Unit = {}) {
    if (otherUser.userId == currentUserId) return
    val me = myProfile.value ?: PublicUser(
      id = currentUserId,
      userId = currentUserId,
      displayName = initialDisplayName ?: "عضو ТЕНЬ",
      username = "ten_${currentUserId.take(6).lowercase()}",
      photoUrl = initialPhotoUrl ?: "",
    )
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.getOrCreateConversation(me, otherUser)
      _isBusy.value = false
      result.onSuccess { conv ->
        _activeConversation.value = conv
        onOpened()
      }.onFailure {
        _actionMessage.tryEmit("تعذر فتح المحادثة")
      }
    }
  }

  fun openConversation(conversation: Conversation) {
    _activeConversation.value = conversation
  }

  fun closeConversation() {
    _activeConversation.value = null
  }

  fun sendMessage(text: String, onSent: () -> Unit) {
    val conv = _activeConversation.value ?: return
    val sender = myProfile.value ?: PublicUser(
      id = currentUserId,
      userId = currentUserId,
      displayName = initialDisplayName ?: "عضو ТЕНЬ",
      username = "ten_${currentUserId.take(6).lowercase()}",
      photoUrl = initialPhotoUrl ?: "",
    )
    viewModelScope.launch {
      repository.sendMessage(conv, text, sender)
        .onSuccess { onSent() }
        .onFailure { _actionMessage.tryEmit("تعذر إرسال الرسالة") }
    }
  }

  private fun unwrapFirestoreOrThrowCancellation(error: Throwable): Throwable {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
  }

  private fun logStreamError(streamName: String, error: Throwable) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    Log.w(TAG, "Error observing $streamName", firestoreError ?: error)
  }

  private companion object {
    const val TAG = "TenViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
