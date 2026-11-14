package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class PublicUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val username: String = "",
  val bio: String = "",
  val photoUrl: String = "",
  val isDiscoverable: Boolean = true,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val postsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorHandle: String = "",
  val authorPhotoUrl: String = "",
  val content: String = "",
  val tag: String = "عام",
  val cardStyle: String = "standard",
  val visibility: String = "public",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorHandle: String = "",
  val authorPhotoUrl: String = "",
  val content: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Follow(
  @DocumentId val id: String = "",
  val followerId: String = "",
  val followedId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val participantHandles: Map<String, String> = emptyMap(),
  val participantPhotos: Map<String, String> = emptyMap(),
  val lastMessage: String = "",
  val lastSenderId: String = "",
  @ServerTimestamp val lastMessageAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DirectMessage(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val participantIds: List<String> = emptyList(),
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
