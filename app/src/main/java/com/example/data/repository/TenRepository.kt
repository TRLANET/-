package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicUser
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TenRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun sanitizeUsername(raw: String?, uid: String): String {
    val cleaned = (raw ?: "")
      .lowercase()
      .replace(Regex("[^a-z0-9_]"), "")
      .take(24)
    return if (cleaned.length >= 3) cleaned else "ten_${uid.take(6).lowercase()}"
  }

  suspend fun ensureUserProfiles(
    displayName: String?,
    email: String?,
    photoUrl: String?,
  ): Result<PublicUser> {
    val uid = requireUserId()
    val publicRef = db.collection("users_public").document(uid)
    val privateRef = db.collection("users_private").document(uid)

    return try {
      val existingPublic = publicRef.get().await()
      val publicUser = if (!existingPublic.exists()) {
        val safeName = (displayName?.trim().takeIf { !it.isNullOrBlank() } ?: "عضو ТЕНЬ").take(50)
        val safeHandle = sanitizeUsername(email?.substringBefore("@") ?: displayName, uid)
        val safePhoto = (photoUrl ?: "").take(500)
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "username" to safeHandle,
          "bio" to "في الظل، حيث تتضح الفكرة.",
          "photoUrl" to safePhoto,
          "isDiscoverable" to true,
          "followersCount" to 0L,
          "followingCount" to 0L,
          "postsCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        publicRef.set(payload).await()
        publicRef.get().await().toObject(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE)
          ?: PublicUser(
            id = uid,
            userId = uid,
            displayName = safeName,
            username = safeHandle,
            bio = "في الظل، حيث تتضح الفكرة.",
            photoUrl = safePhoto,
          )
      } else {
        existingPublic.toObject(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE)
          ?: PublicUser(id = uid, userId = uid)
      }

      val existingPrivate = privateRef.get().await()
      if (!existingPrivate.exists()) {
        val privatePayload = mapOf(
          "userId" to uid,
          "email" to (email ?: "").take(200),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        privateRef.set(privatePayload).await()
      }

      Result.success(publicUser)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, publicRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeMyProfile(uid: String): Flow<PublicUser?> = flow {
    val path = "users_public/$uid"
    emitAll(
      db.collection("users_public").document(uid)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          logFlowError(error, OperationType.GET, path, uid)
          throw error
        }
    )
  }

  fun observeDiscoverableUsers(): Flow<List<PublicUser>> = flow {
    val uid = requireUserId()
    val path = "users_public"
    emitAll(
      db.collection("users_public")
        .whereEqualTo("isDiscoverable", true)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.followersCount }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun updateMyProfile(
    displayName: String,
    username: String,
    bio: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users_public").document(uid)
    val cleanName = displayName.trim().take(50)
    val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").take(30)
    val cleanBio = bio.trim().take(200)

    if (cleanName.isEmpty() || cleanUsername.length < 3) {
      return Result.failure(IllegalArgumentException("الاسم واسم المستخدم (3 أحرف على الأقل) مطلوبان."))
    }

    return try {
      docRef.update(
        mapOf(
          "displayName" to cleanName,
          "username" to cleanUsername,
          "bio" to cleanBio,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observePublicPosts(): Flow<List<Post>> = flow {
    val uid = requireUserId()
    val path = "posts"
    emitAll(
      db.collection("posts")
        .whereEqualTo("visibility", "public")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Post::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMyLikedPostIds(uid: String): Flow<Set<String>> = flow {
    val path = "users_private/$uid/likedPosts"
    emitAll(
      db.collection("users_private").document(uid).collection("likedPosts")
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMyBookmarkedPostIds(uid: String): Flow<Set<String>> = flow {
    val path = "users_private/$uid/bookmarks"
    emitAll(
      db.collection("users_private").document(uid).collection("bookmarks")
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeFollowingIds(uid: String): Flow<Set<String>> = flow {
    val path = "follows"
    emitAll(
      db.collection("follows")
        .whereEqualTo("followerId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { it.getString("followedId") }.toSet()
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun createPost(
    content: String,
    tag: String,
    cardStyle: String,
    author: PublicUser,
  ): Result<String> {
    val uid = requireUserId()
    val trimmedContent = content.trim().take(500)
    if (trimmedContent.isEmpty()) {
      return Result.failure(IllegalArgumentException("محتوى المنشور لا يمكن أن يكون فارغاً."))
    }

    val postRef = db.collection("posts").document()
    val userRef = db.collection("users_public").document(uid)
    val safeStyle = if (cardStyle in setOf("standard", "quote", "obsidian")) cardStyle else "standard"
    val safeTag = tag.trim().ifBlank { "عام" }.take(30)

    val postPayload = mapOf(
      "authorId" to uid,
      "authorName" to author.displayName.ifBlank { "عضو ТЕНЬ" }.take(50),
      "authorHandle" to author.username.ifBlank { "ten_${uid.take(6).lowercase()}" }.take(30),
      "authorPhotoUrl" to author.photoUrl.take(500),
      "content" to trimmedContent,
      "tag" to safeTag,
      "cardStyle" to safeStyle,
      "visibility" to "public",
      "likesCount" to 0L,
      "commentsCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      db.runBatch { batch ->
        batch.set(postRef, postPayload)
        batch.update(
          userRef,
          mapOf(
            "postsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      Result.success(postRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, postRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String, currentPostsCount: Long): Result<Unit> {
    val uid = requireUserId()
    val postRef = db.collection("posts").document(postId)
    val userRef = db.collection("users_public").document(uid)
    return try {
      db.runBatch { batch ->
        batch.delete(postRef)
        if (currentPostsCount > 0L) {
          batch.update(
            userRef,
            mapOf(
              "postsCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, postRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
    val uid = requireUserId()
    val postRef = db.collection("posts").document(postId)
    val likeRef = postRef.collection("likes").document(uid)
    val userLikedRef = db.collection("users_private").document(uid)
      .collection("likedPosts").document(postId)

    return try {
      db.runBatch { batch ->
        if (currentlyLiked) {
          batch.delete(likeRef)
          batch.delete(userLikedRef)
          batch.update(
            postRef,
            mapOf(
              "likesCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        } else {
          val marker = mapOf(
            "userId" to uid,
            "postId" to postId,
            "createdAt" to FieldValue.serverTimestamp(),
          )
          batch.set(likeRef, marker)
          batch.set(userLikedRef, marker)
          batch.update(
            postRef,
            mapOf(
              "likesCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun toggleBookmark(postId: String, currentlyBookmarked: Boolean): Result<Unit> {
    val uid = requireUserId()
    val bookmarkRef = db.collection("users_private").document(uid)
      .collection("bookmarks").document(postId)
    return try {
      if (currentlyBookmarked) {
        bookmarkRef.delete().await()
      } else {
        bookmarkRef.set(
          mapOf(
            "userId" to uid,
            "postId" to postId,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, bookmarkRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeComments(postId: String): Flow<List<PostComment>> = flow {
    val uid = requireUserId()
    val path = "posts/$postId/comments"
    emitAll(
      db.collection("posts").document(postId).collection("comments")
        .whereEqualTo("postId", postId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun addComment(
    postId: String,
    content: String,
    author: PublicUser,
  ): Result<String> {
    val uid = requireUserId()
    val trimmed = content.trim().take(300)
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("التعليق لا يمكن أن يكون فارغاً."))
    }

    val postRef = db.collection("posts").document(postId)
    val commentRef = postRef.collection("comments").document()
    val payload = mapOf(
      "postId" to postId,
      "authorId" to uid,
      "authorName" to author.displayName.ifBlank { "عضو ТЕНЬ" }.take(50),
      "authorHandle" to author.username.ifBlank { "ten_${uid.take(6).lowercase()}" }.take(30),
      "authorPhotoUrl" to author.photoUrl.take(500),
      "content" to trimmed,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      db.runBatch { batch ->
        batch.set(commentRef, payload)
        batch.update(
          postRef,
          mapOf(
            "commentsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      Result.success(commentRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> {
    val uid = requireUserId()
    if (uid == targetUserId) return Result.success(Unit)
    val followId = "${uid}_$targetUserId"
    val followRef = db.collection("follows").document(followId)
    val myRef = db.collection("users_public").document(uid)
    val targetRef = db.collection("users_public").document(targetUserId)

    return try {
      db.runBatch { batch ->
        if (currentlyFollowing) {
          batch.delete(followRef)
          batch.update(
            myRef,
            mapOf(
              "followingCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          batch.update(
            targetRef,
            mapOf(
              "followersCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        } else {
          batch.set(
            followRef,
            mapOf(
              "followerId" to uid,
              "followedId" to targetUserId,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          batch.update(
            myRef,
            mapOf(
              "followingCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          batch.update(
            targetRef,
            mapOf(
              "followersCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, followRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
    val path = "conversations"
    emitAll(
      db.collection("conversations")
        .whereArrayContains("participantIds", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Conversation::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun getOrCreateConversation(
    me: PublicUser,
    other: PublicUser,
  ): Result<Conversation> {
    val uid = requireUserId()
    val participants = listOf(uid, other.userId).sorted()
    val conversationId = participants.joinToString("_")
    val convRef = db.collection("conversations").document(conversationId)

    return try {
      val existing = convRef.get().await()
      if (existing.exists()) {
        val conv = existing.toObject(Conversation::class.java, ServerTimestampBehavior.ESTIMATE)
          ?: Conversation(id = conversationId, participantIds = participants)
        Result.success(conv)
      } else {
        val names = mapOf(
          uid to me.displayName.ifBlank { "عضو ТЕНЬ" }.take(50),
          other.userId to other.displayName.ifBlank { "عضو ТЕНЬ" }.take(50),
        )
        val handles = mapOf(
          uid to me.username.ifBlank { "ten_${uid.take(6).lowercase()}" }.take(30),
          other.userId to other.username.ifBlank { "ten_${other.userId.take(6).lowercase()}" }.take(30),
        )
        val photos = mapOf(
          uid to me.photoUrl.take(500),
          other.userId to other.photoUrl.take(500),
        )
        val payload = mapOf(
          "participantIds" to participants,
          "participantNames" to names,
          "participantHandles" to handles,
          "participantPhotos" to photos,
          "lastMessage" to "",
          "lastSenderId" to uid,
          "lastMessageAt" to FieldValue.serverTimestamp(),
          "createdAt" to FieldValue.serverTimestamp(),
        )
        convRef.set(payload).await()
        val created = convRef.get().await().toObject(Conversation::class.java, ServerTimestampBehavior.ESTIMATE)
          ?: Conversation(
            id = conversationId,
            participantIds = participants,
            participantNames = names,
            participantHandles = handles,
            participantPhotos = photos,
          )
        Result.success(created)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, convRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
    val uid = requireUserId()
    val path = "conversations/$conversationId/messages"
    emitAll(
      db.collection("conversations").document(conversationId)
        .collection("messages")
        .whereArrayContains("participantIds", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(DirectMessage::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun sendMessage(
    conversation: Conversation,
    text: String,
    sender: PublicUser,
  ): Result<String> {
    val uid = requireUserId()
    val trimmed = text.trim().take(1000)
    if (trimmed.isEmpty()) {
      return Result.failure(IllegalArgumentException("الرسالة لا يمكن أن تكون فارغة."))
    }

    val convRef = db.collection("conversations").document(conversation.id)
    val msgRef = convRef.collection("messages").document()
    val msgPayload = mapOf(
      "conversationId" to conversation.id,
      "senderId" to uid,
      "participantIds" to conversation.participantIds,
      "text" to trimmed,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    val updatedNames = conversation.participantNames.toMutableMap().apply {
      put(uid, sender.displayName.ifBlank { "عضو ТЕНЬ" }.take(50))
    }
    val updatedHandles = conversation.participantHandles.toMutableMap().apply {
      put(uid, sender.username.ifBlank { "ten_${uid.take(6).lowercase()}" }.take(30))
    }

    return try {
      db.runBatch { batch ->
        batch.set(msgRef, msgPayload)
        batch.update(
          convRef,
          mapOf(
            "lastMessage" to trimmed.take(500),
            "lastSenderId" to uid,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "participantNames" to updatedNames,
            "participantHandles" to updatedHandles,
          )
        )
      }.await()
      Result.success(msgRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
      Result.failure(e)
    }
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
  }
}
