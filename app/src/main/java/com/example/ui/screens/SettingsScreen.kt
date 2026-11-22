package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LauncherUiState
import com.example.R
import com.example.model.AppThemeMode
import com.example.model.LauncherSurface
import com.example.model.LeftCommandSlot
import com.example.model.LightweightMode
import com.example.model.MetroAccent
import com.example.model.PhotoCropMode
import com.example.model.PhotoDisplayMode
import com.example.model.SettingsSection
import com.example.ui.theme.metroFocusIndicator
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SettingsScreen(
  uiState: LauncherUiState,
  onSelectSection: (SettingsSection) -> Unit,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  onSelectLightweightMode: (LightweightMode) -> Unit,
  onPickProfilePhoto: () -> Unit,
  onSaveProfileName: (String) -> Unit,
  onSaveProfileAge: (String) -> Boolean,
  onClearProfileAge: () -> Unit,
  onRemoveProfilePhoto: () -> Unit,
  onToggleShowProfileOnStart: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onOpenSystemAccessibilitySettings: () -> Unit,
  onOpenSystemCaptionSettings: () -> Unit,
  onOpenRepositoryUrl: (String) -> Unit,
  onRequestHomeRole: () -> Unit,
  onOpenSystemHomeSettings: () -> Unit,
  onSelectAccent: (MetroAccent) -> Unit,
  onApplyCustomAccentHex: (Long) -> Unit,
  onSelectBackgroundHex: (Long) -> Unit,
  onSelectGutterDp: (Int) -> Unit,
  onToggleShowLabels: (Boolean) -> Unit,
  onToggleThemeThirdPartyIcons: (Boolean) -> Unit,
  onReloadIcons: () -> Unit,
  onToggleShowHomePhotos: (Boolean) -> Unit,
  onPickSinglePhoto: () -> Unit,
  onPickMultiplePhotos: () -> Unit,
  onSelectPhotoDisplayMode: (PhotoDisplayMode) -> Unit,
  onSelectPhotoCropMode: (PhotoCropMode) -> Unit,
  onToggleShowPhotoCaptions: (Boolean) -> Unit,
  onForgetSelectedPhotos: () -> Unit,
  onToggleShowPrivatePreviews: (Boolean) -> Unit,
  onRequestCalendarAccess: (Boolean) -> Unit,
  onToggleNotificationsAccess: (Boolean) -> Unit,
  onOpenSystemNotificationAccess: () -> Unit,
  onCreateHubClick: () -> Unit,
  onOpenHub: (String) -> Unit,
  onReorderHub: (String, Int) -> Unit,
  onDeleteHub: (String) -> Unit,
  onToggleLockLayout: (Boolean) -> Unit,
  onToggleReduceMotion: (Boolean) -> Unit,
  onSelectLeftCommandSlot: (LeftCommandSlot) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val bgColor = MaterialTheme.colorScheme.background
  val fgOnBg = MaterialTheme.colorScheme.onBackground
  val mutedOnBg = MaterialTheme.colorScheme.onSurfaceVariant
  val accentColor = uiState.effectiveAccentColor
  val outline = MaterialTheme.colorScheme.outline

  val currentSection = (uiState.surface as? LauncherSurface.Settings)?.section ?: SettingsSection.LANDING
  val detailPages = SettingsSection.detailPages

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(bgColor)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    // Windows Phone / Metro left-aligned header with small category kicker + 34sp Light title
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 720.dp)
          .align(Alignment.CenterHorizontally)
          .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier =
            Modifier.size(48.dp)
              .metroFocusIndicator(accentColor, fgOnBg)
              .testTag("settings_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cancel),
            tint = fgOnBg,
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Image(
          painter = painterResource(id = R.drawable.bloco_logo_mark),
          contentDescription = stringResource(R.string.app_name),
          modifier = Modifier.size(28.dp).testTag("bloco_logo_settings"),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text =
            if (currentSection == SettingsSection.LANDING) {
              stringResource(R.string.app_name).uppercase()
            } else {
              stringResource(R.string.settings).uppercase()
            },
          color = mutedOnBg,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Text(
        text =
          if (currentSection == SettingsSection.LANDING) {
            stringResource(R.string.settings)
          } else {
            currentSection.titlePt
          },
        color = fgOnBg,
        style = MaterialTheme.typography.displayMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 6.dp).testTag("settings_page_title"),
      )
    }

    if (currentSection == SettingsSection.LANDING) {
      SettingsLandingList(
        uiState = uiState,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onSelectSection = onSelectSection,
        modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
      )
    } else {
      val initialPage = detailPages.indexOf(currentSection).coerceAtLeast(0)
      val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { detailPages.size })
      val tabListState = rememberLazyListState()
      val latestSection by rememberUpdatedState(currentSection)

      LaunchedEffect(currentSection) {
        val targetIndex = detailPages.indexOf(currentSection)
        if (targetIndex >= 0) {
          if (pagerState.currentPage != targetIndex) {
            if (uiState.effectiveReducedMotion) {
              pagerState.scrollToPage(targetIndex)
            } else {
              pagerState.animateScrollToPage(targetIndex)
            }
          }
          tabListState.animateScrollToItem(targetIndex)
        }
      }

      LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
          .distinctUntilChanged()
          .collect { settled ->
            val settledSection = detailPages.getOrNull(settled)
            if (settledSection != null && settledSection != latestSection && !pagerState.isScrollInProgress) {
              onSelectSection(settledSection)
            }
          }
      }

      // Visible labeled tap tabs synced with HorizontalPager
      LazyRow(
        state = tabListState,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 720.dp)
            .align(Alignment.CenterHorizontally)
            .testTag("settings_tab_strip"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        itemsIndexed(detailPages, key = { _, item -> item.name }) { idx, section ->
          val active = idx == pagerState.currentPage
          Column(
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, fgOnBg)
                .clickable(role = Role.Tab) { onSelectSection(section) }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("settings_tab_${section.name.lowercase()}"),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = section.titlePt,
              color = if (active) fgOnBg else mutedOnBg,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (active) FontWeight.Medium else FontWeight.Light,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier =
                Modifier.width(36.dp)
                  .height(3.dp)
                  .background(if (active) accentColor else Color.Transparent),
            )
          }
        }
      }

      HorizontalDivider(color = outline)

      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 0,
        modifier =
          Modifier.weight(1f)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .testTag("settings_pager"),
      ) { pageIndex ->
        when (detailPages[pageIndex]) {
          SettingsSection.PROFILE -> {
            SettingsProfilePage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onPickProfilePhoto = onPickProfilePhoto,
              onSaveProfileName = onSaveProfileName,
              onSaveProfileAge = onSaveProfileAge,
              onClearProfileAge = onClearProfileAge,
              onRemoveProfilePhoto = onRemoveProfilePhoto,
              onToggleShowOnStart = onToggleShowProfileOnStart,
            )
          }
          SettingsSection.APPEARANCE -> {
            SettingsAppearancePage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onSelectThemeMode = onSelectThemeMode,
              onSelectAccent = onSelectAccent,
              onApplyCustomAccentHex = onApplyCustomAccentHex,
              onToggleShowLabels = onToggleShowLabels,
            )
          }
          SettingsSection.START_SCREEN -> {
            SettingsStartScreenPage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onToggleShowHomePhotos = onToggleShowHomePhotos,
              onPickSinglePhoto = onPickSinglePhoto,
              onPickMultiplePhotos = onPickMultiplePhotos,
              onSelectPhotoDisplayMode = onSelectPhotoDisplayMode,
              onSelectPhotoCropMode = onSelectPhotoCropMode,
              onToggleShowPhotoCaptions = onToggleShowPhotoCaptions,
              onForgetSelectedPhotos = onForgetSelectedPhotos,
              onToggleShowPrivatePreviews = onToggleShowPrivatePreviews,
              onRequestCalendarAccess = onRequestCalendarAccess,
              onToggleNotificationsAccess = onToggleNotificationsAccess,
              onOpenSystemNotificationAccess = onOpenSystemNotificationAccess,
              onCreateHubClick = onCreateHubClick,
              onOpenHub = onOpenHub,
              onReorderHub = onReorderHub,
              onDeleteHub = onDeleteHub,
              onSelectGutterDp = onSelectGutterDp,
              onSelectLeftCommandSlot = onSelectLeftCommandSlot,
              onToggleLockLayout = onToggleLockLayout,
            )
          }
          SettingsSection.PERFORMANCE -> {
            SettingsPerformancePage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onSelectLightweightMode = onSelectLightweightMode,
              onToggleReduceMotion = onToggleReduceMotion,
            )
          }
          SettingsSection.ACCESSIBILITY -> {
            SettingsAccessibilityPage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onToggleVibration = onToggleVibration,
              onToggleReduceMotion = onToggleReduceMotion,
              onOpenSystemAccessibilitySettings = onOpenSystemAccessibilitySettings,
              onOpenSystemCaptionSettings = onOpenSystemCaptionSettings,
            )
          }
          SettingsSection.ABOUT -> {
            SettingsAboutPage(
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onOpenRepositoryUrl = onOpenRepositoryUrl,
            )
          }
          SettingsSection.ADVANCED -> {
            SettingsAdvancedPage(
              uiState = uiState,
              fgOnBg = fgOnBg,
              mutedOnBg = mutedOnBg,
              accentColor = accentColor,
              onToggleThemeThirdPartyIcons = onToggleThemeThirdPartyIcons,
              onReloadIcons = onReloadIcons,
              onRequestHomeRole = onRequestHomeRole,
              onOpenSystemHomeSettings = onOpenSystemHomeSettings,
              onSelectBackgroundHex = onSelectBackgroundHex,
            )
          }
          SettingsSection.LANDING -> Unit
        }
      }
    }
  }
}

@Composable
private fun SettingsLandingList(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  outline: Color,
  onSelectSection: (SettingsSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val profileSummary =
    uiState.profile.name.trim().ifEmpty { stringResource(R.string.profile_add_name) }
  val appearanceSummary =
    "${stringResource(R.string.theme_title)}: ${uiState.themeMode.labelPt}"
  val performanceSummary =
    "${stringResource(R.string.light_mode)}: ${uiState.lightweightMode.labelPt}"

  LazyColumn(
    modifier = modifier.testTag("settings_landing_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "nav_profile") {
      SettingsLandingRow(
        title = SettingsSection.PROFILE.titlePt,
        subtitle = profileSummary,
        icon = Icons.Outlined.Person,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.PROFILE) },
        testTag = "settings_nav_profile",
      )
    }
    item(key = "nav_appearance") {
      SettingsLandingRow(
        title = SettingsSection.APPEARANCE.titlePt,
        subtitle = appearanceSummary,
        icon = Icons.Outlined.Palette,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.APPEARANCE) },
        testTag = "settings_nav_appearance",
      )
    }
    item(key = "nav_start_screen") {
      SettingsLandingRow(
        title = SettingsSection.START_SCREEN.titlePt,
        subtitle = stringResource(R.string.settings_section_start_desc),
        icon = Icons.Outlined.GridView,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.START_SCREEN) },
        testTag = "settings_nav_start_screen",
      )
    }
    item(key = "nav_performance") {
      SettingsLandingRow(
        title = SettingsSection.PERFORMANCE.titlePt,
        subtitle = performanceSummary,
        icon = Icons.Outlined.Speed,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.PERFORMANCE) },
        testTag = "settings_nav_performance",
      )
    }
    item(key = "nav_accessibility") {
      SettingsLandingRow(
        title = SettingsSection.ACCESSIBILITY.titlePt,
        subtitle = stringResource(R.string.settings_section_accessibility_desc),
        icon = Icons.Outlined.AccessibilityNew,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.ACCESSIBILITY) },
        testTag = "settings_nav_accessibility",
      )
    }
    item(key = "nav_about") {
      SettingsLandingRow(
        title = SettingsSection.ABOUT.titlePt,
        subtitle = stringResource(R.string.settings_section_about_desc),
        icon = Icons.Outlined.Code,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.ABOUT) },
        testTag = "settings_nav_about",
      )
    }
    item(key = "nav_divider") {
      HorizontalDivider(color = outline, modifier = Modifier.padding(vertical = 8.dp))
    }
    item(key = "nav_advanced") {
      SettingsLandingRow(
        title = SettingsSection.ADVANCED.titlePt,
        subtitle = stringResource(R.string.settings_section_advanced_desc),
        icon = Icons.Outlined.Tune,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        accentColor = accentColor,
        outline = outline,
        onClick = { onSelectSection(SettingsSection.ADVANCED) },
        testTag = "settings_nav_advanced",
      )
    }
  }
}

@Composable
private fun SettingsLandingRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  outline: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 64.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, outline)
        .metroFocusIndicator(accentColor, fgOnBg)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        color = fgOnBg,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        color = mutedOnBg,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = mutedOnBg,
      modifier = Modifier.size(20.dp),
    )
  }
}
