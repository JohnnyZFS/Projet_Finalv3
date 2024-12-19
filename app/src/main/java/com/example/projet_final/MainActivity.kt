package com.example.projet_final
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.projet_final.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


private lateinit var binding: ActivityMainBinding

private var Port = ""
private var IP = ""
private const val CHANNEL_ID = "Mise_a_jour_serveur"

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    fun sauvegarderDonneesServeur(ip: String, port: String) {
        if (ip.isEmpty() || port.isEmpty()) {
            throw IllegalArgumentException("IP and Port cannot be empty")
        }

        val sharedPreferences = getSharedPreferences("ServeurPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("IP", ip)
        editor.putString("Port", port)
        editor.apply()
    }

    fun recupererDonneesServeur(context: Context): Pair<String, String> {
        val sharedPreferences = context.getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("IP", "")
        val port = sharedPreferences.getString("Port", "")
        return Pair(ip ?: "", port ?: "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisation des boutons
        binding.buttonNtf.isEnabled = false
        binding.buttonSatut.isEnabled = false
        binding.buttonPlanifier.isEnabled = false
        binding.buttonControler.isEnabled = false

        // Récupérer les données de serveur et vérifier si une connexion est déjà établie
        val (iP2, port2) = recupererDonneesServeur(this)
        Port = port2
        IP = iP2

        // Vérifier et activer/désactiver le bouton "Nouvelle Connexion"
        verifierBoutonNouvelleConnexion()

        // Action pour le bouton "Nouvelle Connexion"
        binding.buttonNvlConnection.setOnClickListener {
            nouvelleConnexion()
        }

        // Initialisation des notifications
        creerCanalNotification()
        demanderPermissionNotifications()

        // Vérifier la présence d'IP et Port
        verifierDonneesServeur()

        // Action pour le bouton "Statut"
        binding.buttonSatut.setOnClickListener {
            val intent = Intent(this, Obtenir_Etat::class.java)
            startActivity(intent)
        }

        // Action pour le bouton "Planifier"
        binding.buttonPlanifier.setOnClickListener {
            val intent = Intent(this, Planifier_ulterieur::class.java)
            startActivity(intent)
        }

        // Action pour le bouton "Contrôler"
        binding.buttonControler.setOnClickListener {
            val intent = Intent(this, Controler_Mtn::class.java)
            startActivity(intent)
        }

        // Action pour le bouton "Notificateur" qui active/désactive les notifications
        binding.buttonNtf.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val isNotificationEnabled = sharedPreferences.getBoolean("isNotificationEnabled", true)

            val editor = sharedPreferences.edit()
            if (isNotificationEnabled) {
                // Désactiver les notifications
                editor.putBoolean("isNotificationEnabled", false)
                Toast.makeText(this, "Notifications désactivées", Toast.LENGTH_SHORT).show()
            } else {
                // Activer les notifications
                editor.putBoolean("isNotificationEnabled", true)
                Toast.makeText(this, "Notifications activées", Toast.LENGTH_SHORT).show()
            }
            editor.apply()
        }

        binding.buttonSauvegarder.setOnClickListener {
            val ipSaisie = binding.editTextIp.text.toString().trim()
            val portSaisi = binding.editTextPort.text.toString().trim()

            if (ipSaisie.isNotEmpty() && portSaisi.isNotEmpty()) {
                IP = ipSaisie
                Port = portSaisi

                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {

                        planifierTachesEnArrierePlan()

                        withContext(Dispatchers.Main) {
                            // Sauvegarde réussie
                            sauvegarderDonneesServeur(
                                IP,
                                Port
                            )  // Sauvegarde dans SharedPreferences
                            verifierDonneesServeur()  // Vérifie l'état des boutons
                            verifierBoutonNouvelleConnexion()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Une erreur est survenue : ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Veuillez entrer l'IP et le port", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun nouvelleConnexion() {
        // Réinitialiser les données du serveur
        val sharedPreferences = getSharedPreferences("ServeurPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()  // Effacer les informations de connexion
        editor.apply()
        // Réinitialiser les données du client
        val sharedPreferences2 = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor2 = sharedPreferences2.edit()
        editor2.clear()  // Effacer les informations de connexion
        editor2.apply()

        // Désactiver les boutons après la nouvelle connexion
        binding.buttonSauvegarder.isEnabled = true  // Réactiver "Sauvegarder"
        binding.buttonSatut.isEnabled = false  // Désactiver "Statut"
        binding.buttonPlanifier.isEnabled = false  // Désactiver "Planifier"
        binding.buttonControler.isEnabled = false  // Désactiver "Contrôler"

        // Réactiver le bouton "Nouvelle Connexion"
        binding.buttonNvlConnection.isEnabled = false  // Le bouton doit être désactivé après utilisation


        // Vérifier l'état des boutons après la réinitialisation des données
        verifierDonneesServeur()
    }



    private fun verifierBoutonNouvelleConnexion() {
        val serverPrefs = getSharedPreferences("ServeurPrefs", MODE_PRIVATE)
        val ip = serverPrefs.getString("IP", null)
        val port = serverPrefs.getString("Port", null)

        if (!ip.isNullOrEmpty() && !port.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Construire l'URL à tester
                    val url = "http://$ip:$port"

                    // Effectuer une requête HTTP GET
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            // Serveur disponible, activer les boutons dans le thread principal
                            withContext(Dispatchers.Main) {
                                binding.buttonNvlConnection.isEnabled = true  // Activer "Nouvelle Connexion"
                                binding.buttonSatut.isEnabled = true
                                binding.buttonPlanifier.isEnabled = true
                                binding.buttonControler.isEnabled = true
                                binding.buttonNtf.isEnabled = true

                                // Affichage de la notification de succès
                                afficherNotification(2, "Succès de la connexion", "Connexion au serveur réussie")
                                Toast.makeText(this@MainActivity, "Connexion au serveur réussie", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Serveur indisponible, désactiver les boutons
                            handleServerUnavailability()
                        }
                    }
                } catch (e: Exception) {
                    // En cas d'échec de la connexion, désactiver les boutons
                    handleServerUnavailability()
                }
            }
        } else {
            // Désactiver les boutons si IP et Port sont manquants
            binding.buttonNvlConnection.isEnabled = false  // Désactiver le bouton "Nouvelle Connexion"
            binding.buttonSatut.isEnabled = false
            binding.buttonPlanifier.isEnabled = false
            binding.buttonControler.isEnabled = false
            Toast.makeText(this, "IP et Port non configurés", Toast.LENGTH_SHORT).show()
        }
    }
    private fun afficherNotification(id: Int, titre: String, texte: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(titre)
            .setContentText(texte)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Vérifier la permission avant d'envoyer la notification
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
        } else {
            Log.e("Erreur", "Permission de notification non accordée")
        }
    }



    // Gérer l'indisponibilité du serveur ou la suppression des données
    suspend fun handleServerUnavailability() {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Serveur non disponible, données supprimées", Toast.LENGTH_LONG).show()

            // Supprimer les données IP et Port des SharedPreferences
            val editor = getSharedPreferences("ServeurPrefs", MODE_PRIVATE).edit()
            editor.remove("IP")
            editor.remove("Port")
            editor.apply()
            // Réinitialiser les données du client
            val sharedPreferences2 = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor2 = sharedPreferences2.edit()
            editor2.clear()  // Effacer les informations de connexion
            editor2.apply()


            // Désactiver les boutons
            binding.buttonNvlConnection.isEnabled = false
            binding.buttonSatut.isEnabled = false
            binding.buttonPlanifier.isEnabled = false
            binding.buttonControler.isEnabled = false
            binding.buttonSauvegarder.isEnabled = true
        }
    }

    fun verifierDonneesServeur() {
        val sharedPreferences = getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
        val ip = sharedPreferences.getString("IP", null)
        val port = sharedPreferences.getString("Port", null)

        Log.d("verifierDonneesServeur", "IP: $ip, Port: $port")  // Ajoutez un log pour vérifier l'état des données

        if (!ip.isNullOrEmpty() && !port.isNullOrEmpty()) {
            binding.buttonSauvegarder.isEnabled = false  // Désactiver le bouton Sauvegarder
            binding.buttonSatut.isEnabled = true  // Activer le bouton Statut
            binding.buttonPlanifier.isEnabled = true  // Activer le bouton Planifier
            binding.buttonControler.isEnabled = true  // Activer le bouton Contrôler
        } else {
            binding.buttonSauvegarder.isEnabled = true  // Activer le bouton Sauvegarder
            binding.buttonSatut.isEnabled = false  // Désactiver le bouton Statut
            binding.buttonPlanifier.isEnabled = false  // Désactiver le bouton Planifier
            binding.buttonControler.isEnabled = false  // Désactiver le bouton Contrôler
            Toast.makeText(this, "Inserez les nouvelles donnes de connection", Toast.LENGTH_LONG).show()
        }
    }


    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mises à jour du serveur"
            val descriptionText = "Notifications pour les changements de données serveur"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun demanderPermissionNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (!result) {
                    Toast.makeText(this, "La permission n'a pas été accordée", Toast.LENGTH_SHORT).show()
                }
            }

            // Vérifie si la permission est déjà accordée
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                // Si non, lance la demande de permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Pas besoin de demander la permission sur les versions antérieures à Android 13
            Log.d("Permission", "Pas de demande nécessaire pour les versions antérieures à Android 13")
        }
    }


    private fun planifierTachesEnArrierePlan() {
        // Créez un "InputData" pour passer l'IP et le Port au Worker
        val inputData = workDataOf("IP" to IP, "Port" to Port)

        // Créez un "PeriodicWorkRequest" pour exécuter la tâche toutes les 15 minutes
        val workRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        // Utilisez WorkManager pour planifier la tâche
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(workRequest)
    }

}


class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Récupérer les données passées depuis MainActivity
        val ip = inputData.getString("IP") ?: return Result.failure()
        val port = inputData.getString("Port") ?: return Result.failure()

        // Vérification de la variable isNotificationEnabled dans SharedPreferences
        val sharedPreferences = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isNotificationEnabled = sharedPreferences.getBoolean("isNotificationEnabled", true)

        // Récupérer les données du serveur
        val jsonData = getData("http://$ip:$port")

        if (jsonData != null) {
            try {
                // Tentative de conversion en JSONObject et gestion des erreurs
                val jsonObject = JSONObject(jsonData)  // Tentative de convertir en objet JSON

                // Extraction des données
                val frequence = jsonObject.optString("frequence", "")
                val etat = jsonObject.optString("etat", "")

                // Si l'un des champs est vide, nous considérons cela comme une erreur
                if (frequence.isEmpty() || etat.isEmpty()) {
                    throw JSONException("Données JSON incomplètes: frequence ou etat manquant")
                }

                val serveurPrefs = applicationContext.getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
                val lastFrequence = serveurPrefs.getString("lastFrequence", "")
                val lastEtat = serveurPrefs.getString("lastEtat", "")

                // Comparer les données avec les précédentes
                if (frequence != lastFrequence || etat != lastEtat) {
                    val editor = serveurPrefs.edit()
                    editor.putString("lastFrequence", frequence)
                    editor.putString("lastEtat", etat)
                    editor.apply()

                    // Si les notifications sont activées, afficher une notification
                    if (isNotificationEnabled) {
                        afficherNotification(1, "Nouvelles données", "Les données du serveur ont changé.")
                    }
                }
            } catch (e: JSONException) {
                Log.e("Erreur JSON", "Erreur lors de l'analyse du JSON : ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Erreur", "Erreur lors du traitement des données : ${e.message}")
            }
        }
        return Result.success()
    }

    private fun getData(stUrl: String): String? {
        if (stUrl.isNullOrEmpty() || !stUrl.startsWith("http")) {
            Log.e("Erreur", "URL invalide : $stUrl")
            return null
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)  // Augmenter le délai de connexion
            .readTimeout(20, TimeUnit.SECONDS)     // Augmenter le délai de lecture
            .build()

        val request = Request.Builder().url(stUrl).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e("Erreur", "Erreur de connexion: ${response.code}, URL: $stUrl")
                    null
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e("Erreur", "Temps de connexion dépassé: ${e.message}")
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            Log.e("Erreur", "Exception lors de la récupération des données : ${e.localizedMessage}")
            e.printStackTrace()
            return null
        }
    }

    // Fonction pour afficher la notification
    private fun afficherNotification(id: Int, titre: String, texte: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(titre)
            .setContentText(texte)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
        } else {
            Log.e("Erreur", "Permission de notification non accordée")
        }
    }
}





