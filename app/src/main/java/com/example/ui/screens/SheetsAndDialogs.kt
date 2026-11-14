package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicUser
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSheet(
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onPublish: (content: String, tag: String, cardStyle: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var content by remember { mutableStateOf("") }
  var selectedTag by remember { mutableStateOf("أفكار") }
  var selectedStyle by remember { mutableStateOf("standard") }
  val tags = listOf("أفكار", "عام", "تقنية", "فن", "موسيقى", "اقتباس")
  val styles = listOf(
    "standard" to "قياسي",
    "obsidian" to "أوبسيديان",
    "quote" to "اقتباس",
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.new_post),
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "${content.length}/500",
          style = MaterialTheme.typography.labelMedium,
          color = if (content.length > 460) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      OutlinedTextField(
        value = content,
        onValueChange = { if (it.length <= 500) content = it },
        placeholder = { Text(stringResource(R.string.post_placeholder)) },
        minLines = 4,
        maxLines = 8,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("create_post_input"),
      )

      Text(
        text = "التصنيف",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tags.forEach { tag ->
          FilterChip(
            selected = selectedTag == tag,
            onClick = { selectedTag = tag },
            label = { Text("#$tag") },
          )
        }
      }

      Text(
        text = "نمط البطاقة",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        styles.forEach { (styleKey, styleLabel) ->
          FilterChip(
            selected = selectedStyle == styleKey,
            onClick = { selectedStyle = styleKey },
            label = { Text(styleLabel) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
          shape = MaterialTheme.shapes.medium,
        ) {
          Text(stringResource(R.string.cancel))
        }
        Button(
          onClick = { onPublish(content, selectedTag, selectedStyle) },
          enabled = content.isNotBlank() && !isBusy,
          modifier = Modifier
            .weight(1.5f)
            .testTag("publish_post_button"),
          shape = MaterialTheme.shapes.medium,
        ) {
          if (isBusy) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Text(stringResource(R.string.publish_post))
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
  post: Post,
  comments: List<PostComment>,
  onDismiss: () -> Unit,
  onSendComment: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var commentText by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 20.dp),
    ) {
      Text(
        text = stringResource(R.string.comments_title),
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "${post.authorName}: ${post.content}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
      )
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 160.dp, max = 340.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (comments.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Outlined.ChatBubbleOutline,
              title = "لا توجد تعليقات بعد",
              subtitle = "ابدأ النقاش بإضافة أول تعليق.",
              modifier = Modifier.padding(vertical = 16.dp),
            )
          }
        } else {
          items(comments, key = { it.id }) { comment ->
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
              ) {
                UserAvatar(
                  displayName = comment.authorName,
                  photoUrl = comment.authorPhotoUrl,
                  size = 34.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = comment.authorName,
                      style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                      text = "· ${formatRelativeTime(comment.createdAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = commentText,
          onValueChange = { if (it.length <= 300) commentText = it },
          placeholder = { Text(stringResource(R.string.comment_placeholder)) },
          modifier = Modifier
            .weight(1f)
            .testTag("comment_input"),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
        )
        IconButton(
          onClick = {
            if (commentText.isNotBlank()) {
              onSendComment(commentText)
              commentText = ""
            }
          },
          enabled = commentText.isNotBlank(),
          modifier = Modifier.testTag("send_comment_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(R.string.send),
            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSheet(
  user: PublicUser,
  isSelf: Boolean,
  isFollowing: Boolean,
  onDismiss: () -> Unit,
  onToggleFollow: () -> Unit,
  onMessageClick: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      UserAvatar(
        displayName = user.displayName,
        photoUrl = user.photoUrl,
        size = 76.dp,
      )
      Text(
        text = user.displayName,
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = "@${user.username}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (user.bio.isNotBlank()) {
        Text(
          text = user.bio,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        ProfileStatItem(count = user.postsCount, label = stringResource(R.string.posts_label))
        ProfileStatItem(count = user.followersCount, label = stringResource(R.string.followers_label))
        ProfileStatItem(count = user.followingCount, label = stringResource(R.string.following_label))
      }

      if (!isSelf) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = onToggleFollow,
            modifier = Modifier
              .weight(1f)
              .testTag("sheet_follow_button"),
            shape = MaterialTheme.shapes.medium,
          ) {
            Text(
              text = stringResource(if (isFollowing) R.string.following else R.string.follow),
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedButton(
            onClick = onMessageClick,
            modifier = Modifier
              .weight(1f)
              .testTag("sheet_message_button"),
            shape = MaterialTheme.shapes.medium,
          ) {
            Text(stringResource(R.string.message))
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
fun ProfileStatItem(count: Long, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
