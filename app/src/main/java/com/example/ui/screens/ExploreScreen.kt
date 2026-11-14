package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar

@Composable
fun ExploreScreen(
  currentUserId: String,
  users: List<PublicUser>,
  postsState: UiState<List<Post>>,
  followingIds: Set<String>,
  likedPostIds: Set<String>,
  bookmarkedPostIds: Set<String>,
  onToggleFollow: (String) -> Unit,
  onMessageUser: (PublicUser) -> Unit,
  onOpenUserProfile: (PublicUser) -> Unit,
  onLikePost: (Post) -> Unit,
  onCommentPost: (Post) -> Unit,
  onBookmarkPost: (Post) -> Unit,
  onDeletePost: (Post) -> Unit,
  modifier: Modifier = Modifier,
) {
  var query by remember { mutableStateOf("") }
  val trimmedQuery = query.trim().lowercase()

  val filteredUsers = remember(users, trimmedQuery, currentUserId) {
    users.filter { user ->
      user.userId != currentUserId && (
        trimmedQuery.isEmpty() ||
          user.displayName.lowercase().contains(trimmedQuery) ||
          user.username.lowercase().contains(trimmedQuery) ||
          user.bio.lowercase().contains(trimmedQuery)
        )
    }
  }

  val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
  val filteredPosts = remember(allPosts, trimmedQuery) {
    if (trimmedQuery.isEmpty()) {
      emptyList()
    } else {
      allPosts.filter { post ->
        post.content.lowercase().contains(trimmedQuery) ||
          post.tag.lowercase().contains(trimmedQuery) ||
          post.authorName.lowercase().contains(trimmedQuery) ||
          post.authorHandle.lowercase().contains(trimmedQuery)
      }
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .testTag("explore_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text(stringResource(R.string.search_hint)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = null,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("explore_search_input"),
        )
      }

      item {
        Text(
          text = stringResource(R.string.suggested_members),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 6.dp),
        )
      }

      if (filteredUsers.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Outlined.PersonSearch,
            title = "لا يوجد أعضاء مطابقون",
            subtitle = "سيظهر أعضاء ТЕНЬ الجدد هنا فور انضمامهم.",
          )
        }
      } else {
        items(filteredUsers, key = { it.userId }) { user ->
          val isFollowing = followingIds.contains(user.userId)
          MemberRowCard(
            user = user,
            isFollowing = isFollowing,
            onCardClick = { onOpenUserProfile(user) },
            onFollowClick = { onToggleFollow(user.userId) },
            onMessageClick = { onMessageUser(user) },
          )
        }
      }

      if (trimmedQuery.isNotEmpty()) {
        item {
          Text(
            text = "المنشورات المطابقة (${filteredPosts.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
        items(filteredPosts, key = { "search_${it.id}" }) { post ->
          PostCard(
            post = post,
            isLiked = likedPostIds.contains(post.id),
            isBookmarked = bookmarkedPostIds.contains(post.id),
            isOwnPost = post.authorId == currentUserId,
            onLikeClick = { onLikePost(post) },
            onCommentClick = { onCommentPost(post) },
            onBookmarkClick = { onBookmarkPost(post) },
            onAuthorClick = {
              val author = users.find { it.userId == post.authorId } ?: PublicUser(
                id = post.authorId,
                userId = post.authorId,
                displayName = post.authorName,
                username = post.authorHandle,
                photoUrl = post.authorPhotoUrl,
              )
              onOpenUserProfile(author)
            },
            onDeleteClick = { onDeletePost(post) },
          )
        }
      }
    }
  }
}

@Composable
private fun MemberRowCard(
  user: PublicUser,
  isFollowing: Boolean,
  onCardClick: () -> Unit,
  onFollowClick: () -> Unit,
  onMessageClick: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClick)
      .testTag("member_card_${user.userId}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      UserAvatar(
        displayName = user.displayName,
        photoUrl = user.photoUrl,
        size = 48.dp,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = user.displayName,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "@${user.username} · ${user.followersCount} متابع",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        if (user.bio.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = user.bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      IconButton(
        onClick = onMessageClick,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("message_user_${user.userId}"),
      ) {
        Icon(
          imageVector = Icons.Outlined.MailOutline,
          contentDescription = stringResource(R.string.message),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      if (isFollowing) {
        OutlinedButton(
          onClick = onFollowClick,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("follow_button_${user.userId}"),
        ) {
          Text(
            text = stringResource(R.string.following),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      } else {
        Button(
          onClick = onFollowClick,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("follow_button_${user.userId}"),
        ) {
          Text(
            text = stringResource(R.string.follow),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
