package com.example.educationalapp.features.mainmenu

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.educationalapp.ParallaxMainMenuBackground
import com.example.educationalapp.R
import com.example.educationalapp.Screen
import com.example.educationalapp.SnowfallEffect
import com.example.educationalapp.ui.theme.KidFontFamily

data class MainMenuModule(
    val route: String,
    @DrawableRes val iconRes: Int,
    val title: String,
)

@Composable
fun MainMenuScreen(
    navController: NavController,
    starCount: Int,
) {
    val modules = listOf(
        MainMenuModule("games", R.drawable.main_menu_icon_jocuri, stringResource(id = R.string.main_menu_button_games)),
        MainMenuModule(Screen.InstrumentsMenu.route, R.drawable.main_menu_icon_instrumente, stringResource(id = R.string.main_menu_button_instruments)),
        MainMenuModule(Screen.SongsMenu.route, R.drawable.main_menu_icone_cantece, stringResource(id = R.string.main_menu_button_songs)),
        MainMenuModule(Screen.SoundsMenu.route, R.drawable.main_menu_icon_sunete, stringResource(id = R.string.main_menu_button_sounds)),
        MainMenuModule(Screen.StoriesMenu.route, R.drawable.main_menu_icon_povesti, stringResource(id = R.string.main_menu_button_stories)),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ParallaxMainMenuBackground()

        ConstraintLayout(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val (starsRef, upgradeRef, titleRef, settingsRef, menuRowRef) = createRefs()

            // Top Bar Items
            Row(
                modifier = Modifier.constrainAs(starsRef) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Stele",
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$starCount",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(color = Color.White)
                )
            }

            IconButton(
                onClick = { navController.navigate(Screen.Paywall.route) },
                modifier = Modifier.constrainAs(upgradeRef) {
                    top.linkTo(parent.top, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                }
            ) {
                Image(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Cumpără",
                    modifier = Modifier.size(48.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // Title (RED ZONE)
            val infiniteTransition = rememberInfiniteTransition(label = "titleFloat")
            val titleOffsetY by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "titleFloatY"
            )

            Image(
                painter = painterResource(id = R.drawable.main_menu_title),
                contentDescription = "Titlu",
                modifier = Modifier
                    .constrainAs(titleRef) {
                        top.linkTo(parent.top)
                        bottom.linkTo(menuRowRef.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.5f) // Title is smaller
                    }
                    .aspectRatio(16f / 9f)
                    .graphicsLayer {
                        translationY = titleOffsetY
                    }
            )

            // Menu Buttons (GREEN ZONE)
            Row(
                modifier = Modifier.constrainAs(menuRowRef) {
                    bottom.linkTo(parent.bottom, margin = 24.dp) // Icons are lower
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.95f)
                },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                modules.forEach { module ->
                    ModuleButton(
                        module = module,
                        navController = navController
                    )
                }
            }


            // Settings Button
            IconButton(
                onClick = { navController.navigate(Screen.SettingsScreen.route) },
                modifier = Modifier.constrainAs(settingsRef) {
                    bottom.linkTo(parent.bottom, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                }
            ) {
                Image(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Setări",
                    modifier = Modifier.size(42.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        // Snowfall effect on top of everything
        SnowfallEffect()
    }
}

@Composable
private fun ModuleButton(
    modifier: Modifier = Modifier,
    module: MainMenuModule,
    navController: NavController
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )
    val animShadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 30f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 50 else 300),
        label = "pressGlow"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { navController.navigate(module.route) }
            )
            .graphicsLayer {
                scaleX = animScale
                scaleY = animScale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = module.iconRes),
            contentDescription = module.title,
            modifier = Modifier
                .size(108.dp) // Icons are bigger
                .graphicsLayer {
                    shadowElevation = animShadowElevation
                    if (animShadowElevation > 0) {
                        spotShadowColor = Color(0.9f, 0.9f, 0.5f)
                    }
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = module.title,
            style = TextStyle(
                fontFamily = KidFontFamily,
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                shadow = Shadow(color = Color(0x99000000), offset = Offset(1f, 2f), blurRadius = 4f)
            )
        )
    }
}
