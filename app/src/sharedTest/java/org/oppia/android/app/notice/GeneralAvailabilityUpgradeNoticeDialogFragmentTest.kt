package org.oppia.android.app.notice

import android.app.Application
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.BindsInstance
import dagger.Component
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.oppia.android.R
import org.oppia.android.app.activity.ActivityComponent
import org.oppia.android.app.activity.ActivityComponentFactory
import org.oppia.android.app.activity.route.ActivityRouterModule
import org.oppia.android.app.application.ApplicationComponent
import org.oppia.android.app.application.ApplicationInjector
import org.oppia.android.app.application.ApplicationInjectorProvider
import org.oppia.android.app.application.ApplicationModule
import org.oppia.android.app.application.ApplicationStartupListenerModule
import org.oppia.android.app.application.testing.TestingBuildFlavorModule
import org.oppia.android.app.devoptions.DeveloperOptionsModule
import org.oppia.android.app.devoptions.DeveloperOptionsStarterModule
import org.oppia.android.app.notice.testing.GeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity
import org.oppia.android.app.player.state.itemviewmodel.SplitScreenInteractionModule
import org.oppia.android.app.shim.ViewBindingShimModule
import org.oppia.android.app.translation.testing.ActivityRecreatorTestModule
import org.oppia.android.data.backends.gae.NetworkConfigProdModule
import org.oppia.android.data.backends.gae.NetworkModule
import org.oppia.android.domain.classify.InteractionsModule
import org.oppia.android.domain.classify.rules.algebraicexpressioninput.AlgebraicExpressionInputModule
import org.oppia.android.domain.classify.rules.continueinteraction.ContinueModule
import org.oppia.android.domain.classify.rules.dragAndDropSortInput.DragDropSortInputModule
import org.oppia.android.domain.classify.rules.fractioninput.FractionInputModule
import org.oppia.android.domain.classify.rules.imageClickInput.ImageClickInputModule
import org.oppia.android.domain.classify.rules.itemselectioninput.ItemSelectionInputModule
import org.oppia.android.domain.classify.rules.mathequationinput.MathEquationInputModule
import org.oppia.android.domain.classify.rules.multiplechoiceinput.MultipleChoiceInputModule
import org.oppia.android.domain.classify.rules.numberwithunits.NumberWithUnitsRuleModule
import org.oppia.android.domain.classify.rules.numericexpressioninput.NumericExpressionInputModule
import org.oppia.android.domain.classify.rules.numericinput.NumericInputRuleModule
import org.oppia.android.domain.classify.rules.ratioinput.RatioInputModule
import org.oppia.android.domain.classify.rules.textinput.TextInputRuleModule
import org.oppia.android.domain.exploration.ExplorationProgressModule
import org.oppia.android.domain.exploration.ExplorationStorageModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionConfigFastShowTestModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionProdModule
import org.oppia.android.domain.onboarding.ExpirationMetaDataRetrieverModule
import org.oppia.android.domain.oppialogger.LogStorageModule
import org.oppia.android.domain.oppialogger.LoggingIdentifierModule
import org.oppia.android.domain.oppialogger.analytics.ApplicationLifecycleModule
import org.oppia.android.domain.oppialogger.analytics.CpuPerformanceSnapshotterModule
import org.oppia.android.domain.oppialogger.logscheduler.MetricLogSchedulerModule
import org.oppia.android.domain.oppialogger.loguploader.LogReportWorkerModule
import org.oppia.android.domain.platformparameter.PlatformParameterModule
import org.oppia.android.domain.platformparameter.PlatformParameterSingletonModule
import org.oppia.android.domain.question.QuestionModule
import org.oppia.android.domain.workmanager.WorkManagerConfigurationModule
import org.oppia.android.testing.OppiaTestRule
import org.oppia.android.testing.TestImageLoaderModule
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.firebase.TestAuthenticationModule
import org.oppia.android.testing.junit.InitializeDefaultLocaleRule
import org.oppia.android.testing.robolectric.RobolectricModule
import org.oppia.android.testing.threading.TestCoroutineDispatchers
import org.oppia.android.testing.threading.TestDispatcherModule
import org.oppia.android.testing.time.FakeOppiaClockModule
import org.oppia.android.util.accessibility.AccessibilityTestModule
import org.oppia.android.util.caching.AssetModule
import org.oppia.android.util.caching.testing.CachingTestModule
import org.oppia.android.util.gcsresource.GcsResourceModule
import org.oppia.android.util.locale.LocaleProdModule
import org.oppia.android.util.logging.LoggerModule
import org.oppia.android.util.logging.SyncStatusModule
import org.oppia.android.util.logging.firebase.FirebaseLogUploaderModule
import org.oppia.android.util.networking.NetworkConnectionDebugUtilModule
import org.oppia.android.util.networking.NetworkConnectionUtilDebugModule
import org.oppia.android.util.parser.html.HtmlParserEntityTypeModule
import org.oppia.android.util.parser.image.ImageParsingModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import javax.inject.Inject
import javax.inject.Singleton

/** Tests for [GeneralAvailabilityUpgradeNoticeDialogFragment]. */
// FunctionName: test names are conventionally named with underscores.
@Suppress("FunctionName")
@RunWith(AndroidJUnit4::class)
@Config(
  application = GeneralAvailabilityUpgradeNoticeDialogFragmentTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
@LooperMode(LooperMode.Mode.PAUSED)
class GeneralAvailabilityUpgradeNoticeDialogFragmentTest {
  @get:Rule
  val initializeDefaultLocaleRule = InitializeDefaultLocaleRule()

  @get:Rule
  val oppiaTestRule = OppiaTestRule()

  @field:[Rule JvmField] val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @Mock
  lateinit var mockNoticeClosedListener: GeneralAvailabilityUpgradeNoticeClosedListener

  @Before
  fun setUp() {
    setUpTestApplicationComponent()
  }

  @Test
  fun testFragment_hasExpectedTitle() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      onDialogView(withText(R.string.general_availability_notice_dialog_title))
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testFragment_hasExpectedContentMessageTextUnderTitle() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      onDialogView(withId(R.id.ga_update_notice_dialog_message)).check(matches(isDisplayed()))
      onDialogView(withId(R.id.ga_update_notice_dialog_message))
        .check(matches(withText(R.string.general_availability_notice_dialog_message)))
      onDialogView(withId(R.id.ga_update_notice_dialog_message))
        .check(isCompletelyBelow(withText(R.string.general_availability_notice_dialog_title)))
    }
  }

  @Test
  fun testFragment_hasDoNotShowAgainCheckboxUnderContentMessage() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      onDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox))
        .check(matches(isDisplayed()))
      onDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox)).check(
        matches(withText(R.string.general_availability_notice_dialog_do_not_show_again_text))
      )
      onDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox))
        .check(isCompletelyBelow(withId(R.id.ga_update_notice_dialog_message)))
    }
  }

  @Test
  fun testFragment_hasExpectedAcknowledgementButtonUnderDoNotShowAgainCheckbox() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      onDialogView(withText(R.string.general_availability_notice_dialog_close_button_text))
        .check(matches(isDisplayed()))
      onDialogView(withText(R.string.general_availability_notice_dialog_close_button_text))
        .check(isCompletelyBelow(withId(R.id.ga_update_notice_dialog_preference_checkbox)))
    }
  }

  @Test
  fun testFragment_clickAcknowledgeButton_callsCallbackListenerWithFalse() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      clickOnDialogView(withText(R.string.general_availability_notice_dialog_close_button_text))

      verify(mockNoticeClosedListener).onGaUpgradeNoticeOkayButtonClicked(false)
    }
  }

  @Test
  fun testFragment_clickDoNotShowCheckbox_clickAcknowledgeButton_callsCallbackListenerWithTrue() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      clickOnDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox))

      clickOnDialogView(withText(R.string.general_availability_notice_dialog_close_button_text))

      verify(mockNoticeClosedListener).onGaUpgradeNoticeOkayButtonClicked(true)
    }
  }

  @Test
  fun testFragment_toggleCheckbox_clickAcknowledgeButton_callsCallbackListenerWithFalse() {
    launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity {
      // Select, then deselect, the checkbox before closing the dialog.
      clickOnDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox))
      clickOnDialogView(withId(R.id.ga_update_notice_dialog_preference_checkbox))

      clickOnDialogView(withText(R.string.general_availability_notice_dialog_close_button_text))

      verify(mockNoticeClosedListener).onGaUpgradeNoticeOkayButtonClicked(false)
    }
  }

  private fun launchGeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity(
    testBlock: () -> Unit
  ) {
    // Launch the test activity, but make sure that it's properly set up & time is given for it to
    // initialize.
    ActivityScenario.launch(
      GeneralAvailabilityUpgradeNoticeDialogFragmentTestActivity::class.java
    ).use { scenario ->
      scenario.onActivity { it.mockCallbackListener = mockNoticeClosedListener }
      testCoroutineDispatchers.runCurrent()
      testBlock()
    }
  }

  private fun clickOnDialogView(matcher: Matcher<View>) {
    onDialogView(matcher).perform(click())
    testCoroutineDispatchers.runCurrent()
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  private companion object {
    private fun onDialogView(matcher: Matcher<View>) = onView(matcher).inRoot(isDialog())
  }

  @Singleton
  @Component(
    modules = [
      RobolectricModule::class, PlatformParameterModule::class,
      TestDispatcherModule::class, ApplicationModule::class, LoggerModule::class,
      ContinueModule::class, FractionInputModule::class, ItemSelectionInputModule::class,
      MultipleChoiceInputModule::class, NumberWithUnitsRuleModule::class,
      NumericInputRuleModule::class, TextInputRuleModule::class, DragDropSortInputModule::class,
      ImageClickInputModule::class, InteractionsModule::class, GcsResourceModule::class,
      TestImageLoaderModule::class, ImageParsingModule::class, HtmlParserEntityTypeModule::class,
      QuestionModule::class, TestLogReportingModule::class, AccessibilityTestModule::class,
      LogStorageModule::class,
      ExpirationMetaDataRetrieverModule::class, ViewBindingShimModule::class,
      RatioInputModule::class, ApplicationStartupListenerModule::class,
      HintsAndSolutionConfigFastShowTestModule::class, HintsAndSolutionProdModule::class,
      WorkManagerConfigurationModule::class, LogReportWorkerModule::class,
      FirebaseLogUploaderModule::class, FakeOppiaClockModule::class,
      DeveloperOptionsStarterModule::class, DeveloperOptionsModule::class,
      ExplorationStorageModule::class, NetworkConnectionUtilDebugModule::class,
      NetworkConnectionDebugUtilModule::class, NetworkModule::class, NetworkConfigProdModule::class,
      AssetModule::class, LocaleProdModule::class, ActivityRecreatorTestModule::class,
      PlatformParameterSingletonModule::class, NumericExpressionInputModule::class,
      AlgebraicExpressionInputModule::class, MathEquationInputModule::class,
      SplitScreenInteractionModule::class, LoggingIdentifierModule::class,
      ApplicationLifecycleModule::class, SyncStatusModule::class, TestingBuildFlavorModule::class,
      CachingTestModule::class, MetricLogSchedulerModule::class,
      ActivityRouterModule::class,
      CpuPerformanceSnapshotterModule::class, ExplorationProgressModule::class,
      TestAuthenticationModule::class
    ]
  )
  interface TestApplicationComponent : ApplicationComponent {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder

      fun build(): TestApplicationComponent
    }

    fun inject(test: GeneralAvailabilityUpgradeNoticeDialogFragmentTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerGeneralAvailabilityUpgradeNoticeDialogFragmentTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build()
    }

    fun inject(test: GeneralAvailabilityUpgradeNoticeDialogFragmentTest) = component.inject(test)

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
