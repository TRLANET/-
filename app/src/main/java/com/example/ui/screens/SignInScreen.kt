package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.TenIce
import com.example.ui.theme.TenObsidian

@Composable
fun SignInScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TenObsidian)
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(TenIce.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.24f),
            radius = size.maxDimension * 0.48f,
          )
        )
      }
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Image(
        painter = painterResource(id = R.drawable.ten_logo),
        contentDescription = stringResource(R.string.cd_brand_logo),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(136.dp)
          .clip(RoundedCornerShape(32.dp))
          .border(1.5.dp, Color(0xFF2F3342), RoundedCornerShape(32.dp)),
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "ТЕНЬ",
          style = MaterialTheme.typography.displayLarge,
          color = Color(0xFFF8F9FA),
        )
        Text(
          text = stringResource(R.string.brand_tagline),
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFFA0A6B5),
          textAlign = TextAlign.Center,
        )
      }

      Surface(
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF101116),
        border = BorderStroke(1.dp, Color(0xFF232632)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          SignInFeatureRow(
            icon = Icons.Outlined.AutoAwesome,
            title = "منشورات بنمطك",
            subtitle = "أفكار، اقتباسات، وبطاقات أوبسيديان داكنة",
          )
          SignInFeatureRow(
            icon = Icons.Outlined.Groups,
            title = "مجتمع تفاعلي حي",
            subtitle = "متابعة الأعضاء، إعجابات وتعليقات لحظية",
          )
          SignInFeatureRow(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "رسائل خاصة فورية",
            subtitle = "تواصل مباشر وآمن بين أعضاء ТЕНЬ",
          )
        }
      }

      if (errorMessage != null) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFB4BE),
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
              isLoading = false
            },
          )
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFF8F9FA),
          contentColor = Color(0xFF050507),
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color(0xFF050507),
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.signing_in),
            style = MaterialTheme.typography.labelLarge,
          )
        } else {
          Text(
            text = stringResource(R.string.sign_in_with_google),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun SignInFeatureRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF1A1D26)),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TenIce,
        modifier = Modifier.size(20.dp),
      )
    }
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFF8F9FA),
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
      )
    }
  }
}
