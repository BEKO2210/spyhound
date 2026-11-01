package de.belkis.spyhound.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.design.SpyColors
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.SpyRadii
import de.belkis.spyhound.ui.design.SpySpacing
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class IntroPage(
    val illustration: Int,
    val title: Int,
    val body: Int,
)

private val pages = listOf(
    IntroPage(R.drawable.illu_clean, R.string.intro_page1_title, R.string.intro_page1_body),
    IntroPage(R.drawable.illu_alert, R.string.intro_page2_title, R.string.intro_page2_body),
    IntroPage(R.drawable.hound_mark, R.string.intro_page3_title, R.string.intro_page3_body),
    IntroPage(R.drawable.illu_shizuku, R.string.intro_page4_title, R.string.intro_page4_body),
)

private val ParallaxDistance = 24.dp
private val TextOffsetDistance = 16.dp
private val IndicatorSlotWidth = 28.dp
private val IndicatorSlotHeight = 16.dp
private val IndicatorActiveWidth = 24.dp
private val IndicatorInactiveWidth = 8.dp
private val IndicatorPillHeight = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(
    onFinished: () -> Unit,
    initialPage: Int = 0,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()
    val motion = LocalMotionEnabled.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            val pageOffset = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val isCurrent = pagerState.currentPage == index
            val textProgress by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0f,
                animationSpec = if (motion) {
                    tween(durationMillis = SpyMotion.Element, easing = SpyMotion.Enter)
                } else {
                    tween(durationMillis = 0)
                },
                label = "intro text enter",
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = SpySpacing.Unit * 2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(page.illustration),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            translationX = if (motion) -pageOffset * ParallaxDistance.toPx() else 0f
                        },
                )
                Spacer(Modifier.height(SpySpacing.Section))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val dragFade = (1f - abs(pageOffset)).coerceIn(0f, 1f)
                            alpha = textProgress * dragFade
                            translationY = if (motion) (1f - textProgress) * TextOffsetDistance.toPx() else 0f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(page.title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = SpyColors.Ivory,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SpySpacing.Unit * 3))
                    Text(
                        stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpyColors.IvoryMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        IntroPageIndicators(
            pagerState = pagerState,
            pageCount = pages.size,
            motion = motion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SpySpacing.Unit * 4),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextActionLink(
                text = stringResource(R.string.intro_skip),
                onClick = onFinished,
            )
            val last = pagerState.currentPage == pages.lastIndex
            SpyButton(
                text = stringResource(if (last) R.string.intro_start else R.string.intro_next),
                onClick = {
                    if (last) {
                        onFinished()
                    } else {
                        scope.launch {
                            val next = pagerState.currentPage + 1
                            if (motion) {
                                pagerState.animateScrollToPage(
                                    page = next,
                                    animationSpec = tween(durationMillis = SpyMotion.Screen, easing = SpyMotion.Enter),
                                )
                            } else {
                                pagerState.scrollToPage(next)
                            }
                        }
                    }
                },
                accent = true,
                fillWidth = false,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntroPageIndicators(
    pagerState: PagerState,
    pageCount: Int,
    motion: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until pageCount) {
            val active = pagerState.currentPage == i
            val animatedFraction by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = if (motion) {
                    tween(durationMillis = SpyMotion.Element, easing = SpyMotion.Enter)
                } else {
                    tween(durationMillis = 0)
                },
                label = "pill indicator",
            )
            // Fixed layout slot prevents any layout jump; active width is drawn on the canvas.
            Canvas(
                modifier = Modifier.size(width = IndicatorSlotWidth, height = IndicatorSlotHeight),
            ) {
                val pillW = IndicatorInactiveWidth.toPx() +
                    (IndicatorActiveWidth.toPx() - IndicatorInactiveWidth.toPx()) * animatedFraction
                val pillH = IndicatorPillHeight.toPx()
                val left = (size.width - pillW) / 2f
                val top = (size.height - pillH) / 2f
                val radius = SpyRadii.Pill.toPx()
                drawRoundRect(
                    color = lerp(SpyColors.Border, SpyColors.Amber, animatedFraction),
                    topLeft = Offset(left, top),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        }
    }
}
