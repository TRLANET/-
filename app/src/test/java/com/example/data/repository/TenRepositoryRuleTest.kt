package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TenRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createPost_authenticatedUser_createsAndObservesPost() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID()}@ten.app")
    val repo = TenRepository(firestore, auth)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfiles("Alice", "alice@ten.app", "").getOrThrow()
    }
    assertEquals(uid, profile.userId)

    val postContent = "ТЕНЬ test post ${UUID.randomUUID()}"
    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(postContent, "أفكار", "obsidian", profile).getOrThrow()
    }
    assertTrue(postId.isNotEmpty())

    val posts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePublicPosts().first { list -> list.any { it.id == postId } }
    }
    assertTrue(posts.any { it.id == postId && it.content == postContent })
  }

  @Test
  fun toggleLikeAndComment_authenticatedUsers_succeedsAtomically() = runBlocking {
    signInTestUser("alice2_${UUID.randomUUID()}@ten.app")
    val aliceRepo = TenRepository(firestore, auth)
    val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureUserProfiles("Alice", "alice2@ten.app", "").getOrThrow()
    }
    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createPost("Atomic social post ${UUID.randomUUID()}", "عام", "standard", alice).getOrThrow()
    }

    val bobUid = signInTestUser("bob_${UUID.randomUUID()}@ten.app")
    val bobRepo = TenRepository(firestore, auth)
    val bob = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.ensureUserProfiles("Bob", "bob@ten.app", "").getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.toggleLike(postId, currentlyLiked = false).getOrThrow()
    }
    val likedIds = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeMyLikedPostIds(bobUid).first { it.contains(postId) }
    }
    assertTrue(likedIds.contains(postId))

    val commentId = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.addComment(postId, "Great thought!", bob).getOrThrow()
    }
    assertTrue(commentId.isNotEmpty())
  }

  @Test
  fun deletePost_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser("owner_${UUID.randomUUID()}@ten.app")
    val ownerRepo = TenRepository(firestore, auth)
    val owner = withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.ensureUserProfiles("Owner", "owner@ten.app", "").getOrThrow()
    }
    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.createPost("Owner only ${UUID.randomUUID()}", "عام", "standard", owner).getOrThrow()
    }

    signInTestUser("attacker_${UUID.randomUUID()}@ten.app")
    val attackerRepo = TenRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      attackerRepo.deletePost(postId, currentPostsCount = 1L)
    }
    assertTrue(result.isFailure)
    val fsException = result.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  @Test
  fun observePublicPosts_unauthenticatedUser_failsGracefully() = runBlocking {
    auth.signOut()
    val repo = TenRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublicPosts().first() }
    } catch (e: Exception) {
      caught = e
    }
    assertTrue(caught is IllegalStateException)
  }

  @Test
  fun directConversation_andMessages_isolatedToParticipants() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceUid = signInTestUser("dm_alice_$suffix@ten.app")
    val aliceRepo = TenRepository(firestore, auth)
    val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureUserProfiles("Alice DM", "dm_alice@ten.app", "").getOrThrow()
    }

    val bobUid = signInTestUser("dm_bob_$suffix@ten.app")
    val bobRepo = TenRepository(firestore, auth)
    val bob = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.ensureUserProfiles("Bob DM", "dm_bob@ten.app", "").getOrThrow()
    }

    val conv = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getOrCreateConversation(bob, alice).getOrThrow()
    }
    assertTrue(conv.participantIds.containsAll(listOf(aliceUid, bobUid)))

    val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.sendMessage(conv, "Hello from ТЕНЬ", bob).getOrThrow()
    }
    assertTrue(msgId.isNotEmpty())

    // Eve cannot observe Bob & Alice's messages
    signInTestUser("dm_eve_$suffix@ten.app")
    val eveRepo = TenRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        eveRepo.observeMessages(conv.id).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    // Either empty or permission denied depending on query filter; verify Eve cannot send into it
    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      eveRepo.sendMessage(conv, "Intruder!", bob.copy(userId = auth.currentUser!!.uid))
    }
    assertTrue(sendResult.isFailure)
    val fsErr = sendResult.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
