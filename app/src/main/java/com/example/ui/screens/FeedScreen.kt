package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard

@Composable
fun FeedScreen(
  postsState: UiState<List<Post>>,
  currentUserId: String,
  selectedFilter: String,
  likedPostIds: Set<String>,
  bookmarkedPostIds: Set<String>,
  followingIds: Set<String>,
  discoverableUsers: List<PublicUser>,
  onSelectFilter: (String) -> Unit,
  onLikeClick: (Post) -> Unit,
  onCommentClick: (Post) -> Unit,
  onBookmarkClick: (Post) -> Unit,
  onDeleteClick: (Post) -> Unit,
  onOpenAuthorProfile: (PublicUser) -> Unit,
  modifier: Modifier = Modifier,
) {
  val filters = listOf("الكل", "المتابَعون", "أفكار", "تقنية", "فن", "موسيقى", "اقتباس", "عام")
  val usersById = remember(discoverableUsers) { discoverableUsers.associateBy { it.userId } }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        filters.forEach { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { onSelectFilter(filter) },
            label = {
              Text(
                text = if (filter in listOf("الكل", "المتابَعون")) filter else "#$filter",
                style = MaterialTheme.typography.labelLarge,
              )
            },
            modifier = Modifier.testTag("feed_filter_$filter"),
          )
        }
      }

      when (postsState) {
        is UiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }

        is UiState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            EmptyStateCard(
              icon = Icons.Outlined.ErrorOutline,
              title = "تعذر تحميل الخط الزمني",
              subtitle = postsState.message,
            )
          }
        }

        is UiState.Success -> {
          val filteredPosts = remember(postsState.data, selectedFilter, followingIds, currentUserId) {
            when (selectedFilter) {
              "الكل" -> postsState.data
              "المتابَعون" -> postsState.data.filter {
                it.authorId == currentUserId || followingIds.contains(it.authorId)
              }
              else -> postsState.data.filter { it.tag == selectedFilter }
            }
          }

          if (filteredPosts.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
              contentAlignment = Alignment.Center,
            ) {
              EmptyStateCard(
                icon = Icons.Outlined.DynamicFeed,
                title = stringResource(R.string.empty_feed_title),
                subtitle = if (selectedFilter == "المتابَعون") {
                  "تابع أعضاء من تبويب استكشاف لتظهر منشوراتهم هنا."
                } else {
                  stringResource(R.string.empty_feed_desc)
                },
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("feed_list"),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filteredPosts, key = { it.id }) { post ->
                PostCard(
                  post = post,
                  isLiked = likedPostIds.contains(post.id),
                  isBookmarked = bookmarkedPostIds.contains(post.id),
                  isOwnPost = post.authorId == currentUserId,
                  onLikeClick = { onLikeClick(post) },
                  onCommentClick = { onCommentClick(post) },
                  onBookmarkClick = { onBookmarkClick(post) },
                  onAuthorClick = {
                    val user = usersById[post.authorId] ?: PublicUser(
                      id = post.authorId,
                      userId = post.authorId,
                      displayName = post.authorName,
                      username = post.authorHandle,
                      photoUrl = post.authorPhotoUrl,
                    )
                    onOpenAuthorProfile(user)
                  },
                  onDeleteClick = { onDeleteClick(post) },
                )
              }
              item { Spacer(modifier = Modifier.height(12.dp)) }
            }
          }
        }
      }
    }
  }
}
