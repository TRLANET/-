package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

@Composable
fun ProfileScreen(
  currentUserId: String,
  profile: PublicUser?,
  postsState: UiState<List<Post>>,
  likedPostIds: Set<String>,
  bookmarkedPostIds: Set<String>,
  isBusy: Boolean,
  onUpdateProfile: (displayName: String, username: String, bio: String, onDone: () -> Unit) -> Unit,
  onLikePost: (Post) -> Unit,
  onCommentPost: (Post) -> Unit,
  onBookmarkPost: (Post) -> Unit,
  onDeletePost: (Post) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }

  val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
  val myPosts = remember(allPosts, currentUserId) {
    allPosts.filter { it.authorId == currentUserId }
  }
  val bookmarkedPosts = remember(allPosts, bookmarkedPostIds) {
    allPosts.filter { bookmarkedPostIds.contains(it.id) }
  }
  val displayedPosts = if (selectedTab == 0) myPosts else bookmarkedPosts

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .testTag("profile_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            UserAvatar(
              displayName = profile?.displayName ?: "ТЕНЬ",
              photoUrl = profile?.photoUrl ?: "",
              size = 78.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = profile?.displayName ?: "عضو ТЕНЬ",
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = "@${profile?.username ?: "ten"}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!profile?.bio.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = profile?.bio ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              ProfileStatItem(
                count = myPosts.size.toLong(),
                label = stringResource(R.string.posts_label),
              )
              ProfileStatItem(
                count = profile?.followersCount ?: 0L,
                label = stringResource(R.string.followers_label),
              )
              ProfileStatItem(
                count = profile?.followingCount ?: 0L,
                label = stringResource(R.string.following_label),
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { showEditDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("edit_profile_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Edit,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.edit_profile),
                  fontWeight = FontWeight.SemiBold,
                )
              }
              OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("sign_out_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Logout,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.sign_out))
              }
            }
          }
        }
      }

      item {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.background,
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("${stringResource(R.string.my_posts)} (${myPosts.size})") },
            modifier = Modifier.testTag("profile_tab_posts"),
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("${stringResource(R.string.bookmarks)} (${bookmarkedPosts.size})") },
            modifier = Modifier.testTag("profile_tab_bookmarks"),
          )
        }
      }

      if (displayedPosts.isEmpty()) {
        item {
          EmptyStateCard(
            icon = if (selectedTab == 0) Icons.Outlined.DynamicFeed else Icons.Outlined.BookmarkBorder,
            title = if (selectedTab == 0) "لا توجد منشورات بعد" else "لا توجد محفوظات بعد",
            subtitle = if (selectedTab == 0) {
              "انشر أول أفكارك من زر الإضافة."
            } else {
              stringResource(R.string.empty_bookmarks_desc)
            },
          )
        }
      } else {
        items(displayedPosts, key = { "profile_${selectedTab}_${it.id}" }) { post ->
          PostCard(
            post = post,
            isLiked = likedPostIds.contains(post.id),
            isBookmarked = bookmarkedPostIds.contains(post.id),
            isOwnPost = post.authorId == currentUserId,
            onLikeClick = { onLikePost(post) },
            onCommentClick = { onCommentPost(post) },
            onBookmarkClick = { onBookmarkPost(post) },
            onAuthorClick = {},
            onDeleteClick = { onDeletePost(post) },
          )
        }
      }
    }
  }

  if (showEditDialog && profile != null) {
    EditProfileDialog(
      profile = profile,
      isBusy = isBusy,
      onDismiss = { showEditDialog = false },
      onSave = { name, username, bio ->
        onUpdateProfile(name, username, bio) {
          showEditDialog = false
        }
      },
    )
  }
}

@Composable
private fun EditProfileDialog(
  profile: PublicUser,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit,
) {
  var displayName by remember { mutableStateOf(profile.displayName) }
  var username by remember { mutableStateOf(profile.username) }
  var bio by remember { mutableStateOf(profile.bio) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.edit_profile)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = displayName,
          onValueChange = { if (it.length <= 50) displayName = it },
          label = { Text(stringResource(R.string.display_name_label)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_display_name_input"),
        )
        OutlinedTextField(
          value = username,
          onValueChange = { if (it.length <= 30) username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
          label = { Text(stringResource(R.string.username_label)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_username_input"),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 200) bio = it },
          label = { Text(stringResource(R.string.bio_label)) },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_bio_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(displayName, username, bio) },
        enabled = displayName.isNotBlank() && username.length >= 3 && !isBusy,
        modifier = Modifier.testTag("save_profile_button"),
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
