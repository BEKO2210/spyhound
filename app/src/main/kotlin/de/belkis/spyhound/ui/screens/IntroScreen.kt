package de.belkis.spyhound.ui.screens

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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.TextActionLink
import kotlinx.coroutines.launch

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
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(page.illustration),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(120.dp),
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.indices.forEach { i ->
                val active = pagerState.currentPage == i
                Surface(
                    shape = CircleShape,
                    color = if (active) SpyColors.Amber else SpyColors.Border,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 8.dp),
                ) {}
            }
        }
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
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                accent = true,
                fillWidth = false,
            )
        }
    }
}
