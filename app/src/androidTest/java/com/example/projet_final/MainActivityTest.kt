import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.projet_final.MainActivity
import com.example.projet_final.R
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { activity ->
            context = activity.applicationContext
        }
    }
    @Test
    fun testNotificationPermissionRequest() {
        // Utilisation de UiAutomator pour interagir avec la boîte de dialogue système
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Attendre que la boîte de dialogue apparaisse (maximum 5 secondes)
        device.wait(Until.hasObject(By.text("Allow")), 5000)

        // Cliquer sur le bouton "Allow" (Autoriser)
        device.findObject(By.text("Allow")).click()

        // Vérifier si la permission est maintenant accordée
        val permissionStatus = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(PackageManager.PERMISSION_GRANTED, permissionStatus)
    }

    @Test
    fun testNotificationPermissionGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permissionStatus = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(PackageManager.PERMISSION_GRANTED, permissionStatus)
    }





    @Test
    fun testRecupererDonneesServeur_returnsSavedData() {
        activityScenarioRule.scenario.onActivity { mainActivity ->
            // Sauvegarde des données dans SharedPreferences
            mainActivity.sauvegarderDonneesServeur("192.168.1.2", "9090")
        }

        // Récupère les données et vérifie qu'elles sont correctement récupérées
        activityScenarioRule.scenario.onActivity { mainActivity ->
            val (ip, port) = mainActivity.recupererDonneesServeur(context)
            assertEquals("192.168.1.2", ip)
            assertEquals("9090", port)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSauvegarderDonneesServeur_throwsExceptionIfEmpty() {
        activityScenarioRule.scenario.onActivity { mainActivity ->
            mainActivity.sauvegarderDonneesServeur("", "")
        }
    }

    @Test
    fun testSauvegarderDonneesServeur_savesCorrectly() {
        activityScenarioRule.scenario.onActivity { mainActivity ->
            mainActivity.sauvegarderDonneesServeur("192.168.1.1", "8080")
        }

        // Vérification via SharedPreferences
        activityScenarioRule.scenario.onActivity { mainActivity ->
            val sharedPreferences = mainActivity.getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
            val ip = sharedPreferences.getString("IP", null)
            val port = sharedPreferences.getString("Port", null)

            assertEquals("192.168.1.1", ip)
            assertEquals("8080", port)
        }
    }

    @Test
    fun testHandleServerUnavailability_resetsDataAndDisablesButtons() {
        runBlocking {
            activityScenarioRule.scenario.onActivity { mainActivity ->
                // Simuler une inaccessibilité du serveur
                mainActivity.lifecycleScope.launch {
                    mainActivity.handleServerUnavailability()
                }
            }

            // Vérification de l'état des boutons après l'indisponibilité du serveur
            onView(withId(R.id.buttonSatut)).check(matches(not(isEnabled())))
            onView(withId(R.id.buttonPlanifier)).check(matches(not(isEnabled())))
            onView(withId(R.id.buttonControler)).check(matches(not(isEnabled())))
        }
    }



    @Test
    fun testVerifierDonneesServeur_whenDataMissing_buttonsAreDisabled() {
        activityScenarioRule.scenario.onActivity { mainActivity ->
            // Efface les données dans SharedPreferences
            val sharedPreferences = mainActivity.getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
        }

        // Vérifie que les boutons sont désactivés lorsque les données sont manquantes
        onView(withId(R.id.button_Sauvegarder)).check(matches(isEnabled()))
        onView(withId(R.id.buttonSatut)).check(matches(not(isEnabled())))
        onView(withId(R.id.buttonPlanifier)).check(matches(not(isEnabled())))
        onView(withId(R.id.buttonControler)).check(matches(not(isEnabled())))
    }
}
