package com.example.projet_final

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.projet_final.databinding.ActivityObtenirEtatBinding
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Obtenir_Etat : AppCompatActivity() {
    private lateinit var binding: ActivityObtenirEtatBinding
    private lateinit var serverIP: String
    private lateinit var serverPort: String
    private var currentWorkerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObtenirEtatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupérer les données depuis SharedPreferences
        val sharedPrefs = getSharedPreferences("ServeurPrefs", MODE_PRIVATE)
        serverIP = sharedPrefs.getString("IP", "") ?: ""
        serverPort = sharedPrefs.getString("Port", "") ?: ""

        // Log des informations récupérées
        Log.d("Obtenir_Etat", "IP: $serverIP, Port: $serverPort")

        binding.buttonctr.setOnClickListener {
            cancelWorker()
            val intent = Intent(this, Controler_Mtn::class.java)
            startActivity(intent)
        }

        // Listener pour le spinner
        binding.spinnerchoixdelais.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View,
                    position: Int,
                    id: Long
                ) {
                    val delayInMinutes = when (position) {
                        0 -> 15L // 15 min
                        1 -> 30L // 30 min
                        2 -> 45L // 45 min
                        3 -> 60L // 1 h
                        else -> 30L
                    }

                    scheduleWorker(delayInMinutes)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        // Bouton "Actualiser" : Envoyer une requête GET
        binding.buttonActualiser.setOnClickListener {
            sendGetRequest { statut, vitesse ->
                binding.textViewONOFF.text = statut
                binding.textVitesse.text = vitesse
            }
        }

        // Enregistrer un observer sur WorkManager pour observer les progrès du travail
        val workRequest = PeriodicWorkRequestBuilder<EtatWorker>(15, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "serverIP" to serverIP,
                    "serverPort" to serverPort
                )
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "EtatWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

        // Observer le progrès du travail
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    val statut = workInfo.progress.getString("statut") ?: "Inconnu"
                    val vitesse = workInfo.progress.getString("frequence") ?: "Inconnue"

                    // Mettre à jour les TextViews avec les valeurs reçues
                    binding.textViewONOFF.text = statut
                    binding.textVitesse.text = vitesse
                }
            }

        // Bouton "Accueil" : Retour au MainActivity
        binding.buttonAcceuil.setOnClickListener {
            cancelWorker()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cancelWorker() // Annuler le worker si l'utilisateur quitte l'activité
    }

    // Fonction pour envoyer une requête GET
    private fun sendGetRequest(callback: (String, String) -> Unit) {
        val url = "http://$serverIP:$serverPort"
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"

                // Lire la réponse brute du serveur
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                val statut = jsonResponse.optString("etat", "Inconnu")
                val vitesse = jsonResponse.optString("frequence", "off")

                runOnUiThread {
                    callback(statut, vitesse)
                }
            } catch (e: Exception) {
                Log.e("Obtenir_Etat", "Erreur GET: ${e.message}")
            }
        }.start()
    }

    private fun scheduleWorker(delayInMinutes: Long) {
        if (serverIP.isEmpty() || serverPort.isEmpty()) {
            Toast.makeText(this, "Paramètres manquants pour planifier la tâche", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Annuler l'ancien worker si existant
        cancelWorker()

        val workRequest = PeriodicWorkRequestBuilder<EtatWorker>(delayInMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf("serverIP" to serverIP, "serverPort" to serverPort)
            )
            .build()

        currentWorkerId = workRequest.id.toString()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EtatWorker", ExistingPeriodicWorkPolicy.REPLACE, workRequest
        )

        Toast.makeText(
            this, "Mise a jour automatique chaque $delayInMinutes minutes", Toast.LENGTH_SHORT
        ).show()

        // Observer le Worker
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    Log.d("Worker", "Worker terminé, aucune interface à mettre à jour.")
                }
            })
    }

    private fun cancelWorker() {
        if (currentWorkerId.isNotEmpty()) {
            WorkManager.getInstance(this).cancelAllWorkByTag("EtatWorker")
            Log.d("Worker", "Worker annulé.")
        }
    }
}

class EtatWorker(appContext: android.content.Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val serverIP = inputData.getString("serverIP") ?: return Result.failure()
        val serverPort = inputData.getString("serverPort") ?: return Result.failure()

        try {
            // Récupérer les données depuis le serveur
            val url = "http://$serverIP:$serverPort"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            // Traiter la réponse JSON
            val jsonResponse = JSONObject(response)
            val statut = jsonResponse.optString("etat", "Inconnu")
            val vitesse = jsonResponse.optString("frequence", "off")

            // Envoyer les progrès avant de terminer le travail
            setProgressAsync(workDataOf("statut" to statut, "vitesse" to vitesse))

            // Indiquer que le travail est terminé avec succès
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            // Retourner un échec si une erreur s'est produite
            return Result.failure()
        }
    }

}


