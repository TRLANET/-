package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.PublicUser
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
  currentUserId: String,
  conversations: List<Conversation>,
  activeConversation: Conversation?,
  messages: List<DirectMessage>,
  discoverableUsers: List<PublicUser>,
  onOpenConversation: (Conversation) -> Unit,
  onCloseConversation: () -> Unit,
  onStartChatWithUser: (PublicUser) -> Unit,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showNewChatSheet by remember { mutableStateOf(false) }

  if (activeConversation != null) {
    BackHandler {
      onCloseConversation()
    }
    ChatThreadView(
      currentUserId = currentUserId,
      conversation = activeConversation,
      messages = messages,
      onBack = onCloseConversation,
      onSendMessage = onSendMessage,
      modifier = modifier,
    )
    return
  }

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
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.messages_title),
          style = MaterialTheme.typography.titleLarge,
        )
        Button(
          onClick = { showNewChatSheet = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("new_chat_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.AddComment,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.start_chat))
        }
      }

      if (conversations.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          EmptyStateCard(
            icon = Icons.Outlined.Forum,
            title = stringResource(R.string.empty_messages_title),
            subtitle = stringResource(R.string.empty_messages_desc),
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("conversations_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(conversations, key = { it.id }) { conv ->
            val otherId = conv.participantIds.firstOrNull { it != currentUserId } ?: currentUserId
            val otherName = conv.participantNames[otherId] ?: "عضو ТЕНЬ"
            val otherHandle = conv.participantHandles[otherId] ?: "ten"
            val otherPhoto = conv.participantPhotos[otherId] ?: ""

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenConversation(conv) }
                .testTag("conversation_card_${conv.id}"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                UserAvatar(
                  displayName = otherName,
                  photoUrl = otherPhoto,
                  size = 46.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = otherName,
                      style = MaterialTheme.typography.titleMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = formatRelativeTime(conv.lastMessageAt),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = conv.lastMessage.ifBlank { "@$otherHandle · ابدأ الحديث" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showNewChatSheet) {
    val others = remember(discoverableUsers, currentUserId) {
      discoverableUsers.filter { it.userId != currentUserId }
    }
    ModalBottomSheet(
      onDismissRequest = { showNewChatSheet = false },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "اختر عضواً للمراسلة",
          style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (others.isEmpty()) {
            item {
              Text(
                text = "لا يوجد أعضاء آخرون متاحون حالياً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
              )
            }
          } else {
            items(others, key = { it.userId }) { user ->
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    showNewChatSheet = false
                    onStartChatWithUser(user)
                  },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  UserAvatar(
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    size = 40.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                      text = "@${user.username}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun ChatThreadView(
  currentUserId: String,
  conversation: Conversation,
  messages: List<DirectMessage>,
  onBack: () -> Unit,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val otherId = conversation.participantIds.firstOrNull { it != currentUserId } ?: currentUserId
  val otherName = conversation.participantNames[otherId] ?: "عضو ТЕНЬ"
  val otherHandle = conversation.participantHandles[otherId] ?: "ten"
  val otherPhoto = conversation.participantPhotos[otherId] ?: ""

  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .imePadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("chat_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
          )
        }
        UserAvatar(
          displayName = otherName,
          photoUrl = otherPhoto,
          size = 38.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = otherName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "@$otherHandle",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("messages_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(messages, key = { it.id }) { msg ->
          val isMine = msg.senderId == currentUserId
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
          ) {
            Column(
              horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
              modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                  RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp,
                  )
                )
                .background(
                  if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
              Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = formatRelativeTime(msg.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = if (isMine) {
                  MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = messageText,
          onValueChange = { if (it.length <= 1000) messageText = it },
          placeholder = { Text(stringResource(R.string.message_placeholder)) },
          modifier = Modifier
            .weight(1f)
            .testTag("message_input"),
          maxLines = 4,
          shape = RoundedCornerShape(18.dp),
        )
        IconButton(
          onClick = {
            if (messageText.isNotBlank()) {
              onSendMessage(messageText)
              messageText = ""
            }
          },
          enabled = messageText.isNotBlank(),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("send_message_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(R.string.send),
            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          )
        }
      }
    }
  }
}
