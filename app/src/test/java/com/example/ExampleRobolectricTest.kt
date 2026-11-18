package com.example

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppLanguage
import com.example.domain.model.Category
import com.example.domain.model.Item
import com.example.ui.add.AddItemScreen
import com.example.ui.archive.ArchiveScreen
import com.example.ui.components.ItemCard
import com.example.ui.privacy.PrivacyScreen
import com.example.ui.theme.MinipiryTheme
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app_name and firestore_database_id resources are properly configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("超简到期", context.getString(R.string.app_name))
    val dbId = context.getString(R.string.firestore_database_id)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `ItemCard renders Paper and Ink Swiss editorial status stamp, title, and giant countdown`() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 17, 15, 0, 0)
    }
    val item = Item(
      id = "milk_1",
      name = "鲜牛奶",
      category = Category.FOOD,
      expiryDate = "2026-11-18",
      createdAt = cal.timeInMillis,
      note = "冷藏保存",
      usedUp = false,
    )

    composeTestRule.setContent {
      MinipiryTheme {
        ItemCard(
          item = item,
          language = AppLanguage.ZH,
          nowMillis = cal.timeInMillis,
          onCardClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("item_card_milk_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("SOON").assertIsDisplayed()
    composeTestRule.onNodeWithText("食品").assertIsDisplayed()
    composeTestRule.onNodeWithText("鲜牛奶").assertIsDisplayed()
    composeTestRule.onNodeWithText("1").assertIsDisplayed()
    composeTestRule.onNodeWithText("天后到期").assertIsDisplayed()
  }

  @Test
  fun `AddItemScreen allows selecting category preset and expiry preset to save item`() {
    var savedTuple: Triple<String, Category, String>? = null

    composeTestRule.setContent {
      MinipiryTheme {
        AddItemScreen(
          language = AppLanguage.ZH,
          isSaving = false,
          onSaveItem = { name, cat, date, _ ->
            savedTuple = Triple(name, cat, date)
          },
          onNavigateBack = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("preset_item_牛奶").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("expiry_preset_days_3").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("add_save_button").performClick()

    composeTestRule.waitForIdle()
    assertNotNull(savedTuple)
    assertEquals("牛奶", savedTuple?.first)
    assertEquals(Category.FOOD, savedTuple?.second)
    assertTrue(savedTuple?.third?.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) == true)
  }

  @Test
  fun `ArchiveScreen renders restore and delete actions for archived items`() {
    var restoredId: String? = null
    val archived = listOf(
      Item(
        id = "arch_1",
        name = "旧酸奶",
        category = Category.FOOD,
        expiryDate = "2026-11-10",
        createdAt = 1000L,
        usedUp = true,
      )
    )

    composeTestRule.setContent {
      MinipiryTheme {
        ArchiveScreen(
          archivedItems = archived,
          language = AppLanguage.ZH,
          nowMillis = 1763337600000L,
          snackbarHostState = SnackbarHostState(),
          onOpenDetail = {},
          onRestoreItem = { restoredId = it.id },
          onDeleteItem = {},
          onNavigateHome = {},
          onNavigateAdd = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("旧酸奶").assertIsDisplayed()
    composeTestRule.onNodeWithTag("archive_restore_arch_1").performClick()
    assertEquals("arch_1", restoredId)
  }

  @Test
  fun `PrivacyScreen renders bilingual title and contact email`() {
    composeTestRule.setContent {
      MinipiryTheme {
        PrivacyScreen(
          language = AppLanguage.ZH,
          onSelectLanguage = {},
          onNavigateBack = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("超简到期 · 隐私政策").assertIsDisplayed()
    composeTestRule.onNodeWithTag("privacy_email_link").performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("hi@viankie.com").assertIsDisplayed()
  }
}
