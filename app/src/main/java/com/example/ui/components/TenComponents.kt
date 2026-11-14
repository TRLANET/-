package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Post
import com.example.ui.theme.TenCrimson
import com.example.ui.theme.TenObsidian
import com.example.ui.theme.TenSlate
import com.google.firebase.Timestamp

@Composable
fun TenBrandHeader(
  title: String,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.ten_logo),
        contentDescription = stringResource(R.string.cd_brand_logo),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
      )
      Column {
        Text(
          text = "ТЕНЬ",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        if (title != "ТЕНЬ") {
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    IconButton(
      onClick = onToggleTheme,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag("theme_toggle_button"),
    ) {
      Icon(
        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = stringResource(R.string.cd_toggle_theme),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun UserAvatar(
  displayName: String,
  photoUrl: String,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
) {
  val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Т"
  if (photoUrl.isNotBlank()) {
    AsyncImage(
      model = photoUrl,
      contentDescription = displayName,
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
  } else {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.surface,
            )
          )
        )
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
      Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun PostCard(
  post: Post,
  isLiked: Boolean,
  isBookmarked: Boolean,
  isOwnPost: Boolean,
  onLikeClick: () -> Unit,
  onCommentClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onAuthorClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isObsidian = post.cardStyle == "obsidian"
  val isQuote = post.cardStyle == "quote"

  val containerColor = when {
    isObsidian -> TenObsidian
    else -> MaterialTheme.colorScheme.surface
  }
  val contentColor = if (isObsidian) Color(0xFFF8F9FA) else MaterialTheme.colorScheme.onSurface
  val mutedColor = if (isObsidian) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
  val borderColor = if (isObsidian) Color(0xFF343846) else MaterialTheme.colorScheme.outlineVariant

  val likeScale by animateFloatAsState(
    targetValue = if (isLiked) 1.18f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "likeScale",
  )
  val likeTint by animateColorAsState(
    targetValue = if (isLiked) TenCrimson else mutedColor,
    label = "likeTint",
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("post_card_${post.id}"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onAuthorClick)
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          UserAvatar(
            displayName = post.authorName,
            photoUrl = post.authorPhotoUrl,
            size = 42.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = post.authorName,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "· ${formatRelativeTime(post.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                maxLines = 1,
              )
            }
            Text(
              text = "@${post.authorHandle}",
              style = MaterialTheme.typography.labelMedium,
              color = mutedColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = if (isObsidian) TenSlate else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "#${post.tag}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isObsidian) Color(0xFFD6DBE8) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }

        if (isOwnPost) {
          IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("delete_post_${post.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = stringResource(R.string.cd_delete_post),
              tint = mutedColor,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isQuote) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (isObsidian) Color(0xFF14161E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .padding(14.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = post.content,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
          )
        }
      } else {
        Text(
          text = post.content,
          style = MaterialTheme.typography.bodyLarge,
          color = contentColor,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onLikeClick)
              .minimumInteractiveComponentSize()
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("like_button_${post.id}"),
          ) {
            Icon(
              imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = stringResource(R.string.cd_like),
              tint = likeTint,
              modifier = Modifier
                .size(20.dp)
                .scale(likeScale),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = post.likesCount.toString(),
              style = MaterialTheme.typography.labelLarge,
              color = likeTint,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onCommentClick)
              .minimumInteractiveComponentSize()
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("comment_button_${post.id}"),
          ) {
            Icon(
              imageVector = Icons.Outlined.ChatBubbleOutline,
              contentDescription = stringResource(R.string.cd_comment),
              tint = mutedColor,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = post.commentsCount.toString(),
              style = MaterialTheme.typography.labelLarge,
              color = mutedColor,
            )
          }
        }

        IconButton(
          onClick = onBookmarkClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("bookmark_button_${post.id}"),
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = stringResource(R.string.cd_bookmark),
            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else mutedColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(38.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
  if (timestamp == null) return "الآن"
  val diffSeconds = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
  return when {
    diffSeconds < 60L -> "الآن"
    diffSeconds < 3600L -> "منذ ${diffSeconds / 60L} د"
    diffSeconds < 86400L -> "منذ ${diffSeconds / 3600L} س"
    else -> "منذ ${diffSeconds / 86400L} ي"
  }
}
