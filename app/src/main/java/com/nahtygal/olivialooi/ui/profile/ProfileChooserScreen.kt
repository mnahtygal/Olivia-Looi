package com.nahtygal.olivialooi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.profile.KidProfile
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
internal fun ProfileChooserScreen(
    activeProfile: KidProfile,
    onProfileSelected: (KidProfile) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.looloo_name),
                color = FrostBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.profile_chooser_title),
                color = SnowWhite,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(30.dp))

            KidProfile.entries.forEach { profile ->
                ProfileChoice(
                    profile = profile,
                    isActive = profile == activeProfile,
                    onClick = { onProfileSelected(profile) },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            WinterNavigationButton(
                labelResource = R.string.back_to_home,
                onClick = onBackToHome,
            )
        }
    }
}

@Composable
private fun ProfileChoice(
    profile: KidProfile,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val description = stringResource(R.string.profile_choose_content_description, profile.displayName)
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .semantics { contentDescription = description }
            .border(
                width = if (isActive) 4.dp else 2.dp,
                color = if (isActive) ReadyMint else FrostBlue,
                shape = shape,
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowWhite.copy(alpha = 0.96f),
            contentColor = DeepIndigo,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = profile.displayName.take(1),
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isActive) ReadyMint else FrostBlue)
                    .padding(top = 10.dp),
                color = DeepIndigo,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Column {
                Text(
                    text = profile.displayName,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (isActive) {
                    Text(
                        text = stringResource(R.string.profile_playing_now),
                        color = Color(0xFF236450),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
