package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.PublicUser
import com.example.data.repository.TenRepository
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.TenBrandHeader
import com.example.ui.screens.CommentsSheet
import com.example.ui.screens.CreatePostSheet
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.UserProfileSheet
import com.example.ui.theme.TenTheme
import com.example.ui.viewmodel.TenViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_FEED = "feed"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_PROFILE = "profile"

private data class TenNavItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var isDarkTheme by rememberSaveable { mutableStateOf(true) }
      TenTheme(darkTheme = isDarkTheme) {
        TenAppRoot(
          isDarkTheme = isDarkTheme,
          onToggleTheme = { isDarkTheme = !isDarkTheme },
        )
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun TenAppRoot(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  auth: FirebaseAuth = Firebase.auth,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    SignInScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      TenMainScaffold(
        currentUser = user,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onSignOut = {
          signOutUser(
            credentialManager = credentialManager,
            scope = scope,
          )
        },
      )
    }
  }
}

@Composable
fun TenMainScaffold(
  currentUser: FirebaseUser,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onSignOut: () -> Unit,
) {
  val viewModel: TenViewModel = viewModel(
    key = currentUser.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        TenViewModel(
          repository = TenRepository(db),
          currentUserId = currentUser.uid,
          initialDisplayName = currentUser.displayName,
          initialEmail = currentUser.email,
          initialPhotoUrl = currentUser.photoUrl?.toString(),
        )
      }
    },
  )

  val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
  val postsState by viewModel.postsState.collectAsStateWithLifecycle()
  val discoverableUsers by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
  val bookmarkedPostIds by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()
  val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
  val conversations by viewModel.conversations.collectAsStateWithLifecycle()
  val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val activeCommentsPost by viewModel.activeCommentsPost.collectAsStateWithLifecycle()
  val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
  val selectedFeedFilter by viewModel.selectedFeedFilter.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var showCreatePostSheet by remember { mutableStateOf(false) }
  var inspectedUser by remember { mutableStateOf<PublicUser?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.actionMessage.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_FEED

  val navItems = remember {
    listOf(
      TenNavItem(ROUTE_FEED, R.string.nav_feed, Icons.Filled.Home, Icons.Outlined.Home),
      TenNavItem(ROUTE_EXPLORE, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
      TenNavItem(ROUTE_MESSAGES, R.string.nav_messages, Icons.Filled.Mail, Icons.Outlined.MailOutline),
      TenNavItem(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.PersonOutline),
    )
  }

  fun navigateToTab(route: String) {
    if (route != ROUTE_MESSAGES) {
      viewModel.closeConversation()
    }
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  val screenSubtitle = when (currentRoute) {
    ROUTE_FEED -> stringResource(R.string.nav_feed)
    ROUTE_EXPLORE -> stringResource(R.string.nav_explore)
    ROUTE_MESSAGES -> stringResource(R.string.nav_messages)
    ROUTE_PROFILE -> stringResource(R.string.nav_profile)
    else -> "ТЕНЬ"
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding(),
          containerColor = MaterialTheme.colorScheme.surface,
        ) {
          navItems.forEach { item ->
            val selected = currentRoute == item.route
            val label = stringResource(item.labelRes)
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(item.route) },
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = { Text(label) },
              modifier = Modifier.testTag("nav_item_${item.route}"),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail && !(currentRoute == ROUTE_MESSAGES && activeConversation != null)) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
            ) {
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                val label = stringResource(item.labelRes)
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = label,
                    )
                  },
                  label = { Text(label) },
                  modifier = Modifier.testTag("nav_item_${item.route}"),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (currentRoute == ROUTE_FEED || currentRoute == ROUTE_EXPLORE) {
            FloatingActionButton(
              onClick = { showCreatePostSheet = true },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("create_post_fab"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_create_post),
              )
            }
          }
        },
      ) { innerPadding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (!(currentRoute == ROUTE_MESSAGES && activeConversation != null)) {
            TenBrandHeader(
              title = screenSubtitle,
              isDarkTheme = isDarkTheme,
              onToggleTheme = onToggleTheme,
            )
          }

          NavHost(
            navController = navController,
            startDestination = ROUTE_FEED,
            modifier = Modifier.weight(1f),
          ) {
            composable(ROUTE_FEED) {
              FeedScreen(
                postsState = postsState,
                currentUserId = currentUser.uid,
                selectedFilter = selectedFeedFilter,
                likedPostIds = likedPostIds,
                bookmarkedPostIds = bookmarkedPostIds,
                followingIds = followingIds,
                discoverableUsers = discoverableUsers,
                onSelectFilter = viewModel::setFeedFilter,
                onLikeClick = viewModel::toggleLike,
                onCommentClick = viewModel::openComments,
                onBookmarkClick = viewModel::toggleBookmark,
                onDeleteClick = viewModel::deletePost,
                onOpenAuthorProfile = { user -> inspectedUser = user },
              )
            }

            composable(ROUTE_EXPLORE) {
              ExploreScreen(
                currentUserId = currentUser.uid,
                users = discoverableUsers,
                postsState = postsState,
                followingIds = followingIds,
                likedPostIds = likedPostIds,
                bookmarkedPostIds = bookmarkedPostIds,
                onToggleFollow = viewModel::toggleFollow,
                onMessageUser = { user ->
                  viewModel.openConversationWithUser(user) {
                    navigateToTab(ROUTE_MESSAGES)
                  }
                },
                onOpenUserProfile = { user -> inspectedUser = user },
                onLikePost = viewModel::toggleLike,
                onCommentPost = viewModel::openComments,
                onBookmarkPost = viewModel::toggleBookmark,
                onDeletePost = viewModel::deletePost,
              )
            }

            composable(ROUTE_MESSAGES) {
              MessagesScreen(
                currentUserId = currentUser.uid,
                conversations = conversations,
                activeConversation = activeConversation,
                messages = activeMessages,
                discoverableUsers = discoverableUsers,
                onOpenConversation = viewModel::openConversation,
                onCloseConversation = viewModel::closeConversation,
                onStartChatWithUser = { user ->
                  viewModel.openConversationWithUser(user)
                },
                onSendMessage = { text ->
                  viewModel.sendMessage(text) {}
                },
              )
            }

            composable(ROUTE_PROFILE) {
              ProfileScreen(
                currentUserId = currentUser.uid,
                profile = myProfile,
                postsState = postsState,
                likedPostIds = likedPostIds,
                bookmarkedPostIds = bookmarkedPostIds,
                isBusy = isBusy,
                onUpdateProfile = viewModel::updateProfile,
                onLikePost = viewModel::toggleLike,
                onCommentPost = viewModel::openComments,
                onBookmarkPost = viewModel::toggleBookmark,
                onDeletePost = viewModel::deletePost,
                onSignOut = onSignOut,
              )
            }
          }
        }
      }
    }
  }

  if (showCreatePostSheet) {
    CreatePostSheet(
      isBusy = isBusy,
      onDismiss = { showCreatePostSheet = false },
      onPublish = { content, tag, cardStyle ->
        viewModel.createPost(content, tag, cardStyle) {
          showCreatePostSheet = false
        }
      },
    )
  }

  val commentPost = activeCommentsPost
  if (commentPost != null) {
    CommentsSheet(
      post = commentPost,
      comments = activeComments,
      onDismiss = viewModel::closeComments,
      onSendComment = { text ->
        viewModel.addComment(commentPost.id, text) {}
      },
    )
  }

  val sheetUser = inspectedUser
  if (sheetUser != null) {
    UserProfileSheet(
      user = sheetUser,
      isSelf = sheetUser.userId == currentUser.uid,
      isFollowing = followingIds.contains(sheetUser.userId),
      onDismiss = { inspectedUser = null },
      onToggleFollow = { viewModel.toggleFollow(sheetUser.userId) },
      onMessageClick = {
        inspectedUser = null
        viewModel.openConversationWithUser(sheetUser) {
          navigateToTab(ROUTE_MESSAGES)
        }
      },
    )
  }
}
