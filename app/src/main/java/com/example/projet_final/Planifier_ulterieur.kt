package com.example.projet_final

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.projet_final.databinding.ActivityPlanifierUlterieurBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class Planifier_ulterieur : AppCompatActivity() {

    private lateinit var binding: ActivityPlanifierUlterieurBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanifierUlterieurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bouton "Accueil" : Retour au MainActivity
        binding.btnacceill4.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonhorairement2.setOnClickListener {
            val intent = Intent(this, Planifier_intervalle::class.java)
            startActivity(intent)
        }

        // Planifier l'activité ultérieure
        binding.buttonsauvegarder4.setOnClickListener {
            val intervalle = binding.spinneulterieur.selectedItem.toString()
            val statut = binding.spinnerstatut4.selectedItem.toString() // Récupération du statut
            val vitesse =
                binding.spinnerVitesse4.selectedItem.toString() // Récupération de la vitesse

            // Convertir l'intervalle sélectionné en millisecondes
            val intervalleMillis = when (intervalle) {
                "15 min" -> TimeUnit.MINUTES.toMillis(15)
                "30 min" -> TimeUnit.MINUTES.toMillis(30)
                "45 min" -> TimeUnit.MINUTES.toMillis(45)
                "1 h" -> TimeUnit.HOURS.toMillis(1)
                else -> { TimeUnit.MINUTES.toMillis(30) // Par défaut 30 minutes
                }
            }

            // Planifier la tâche
            planifierRequete(intervalleMillis, statut, vitesse)

            // Afficher un message de confirmation
            Toast.makeText(
                this,
                "Tâche simple planifiée avec statut: $statut, vitesse: $vitesse, dans $intervalle.",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun planifierRequete(intervalleMillis: Long, statut: String, vitesse: String) {
        // Préparer les données pour le Worker
        val data = workDataOf(
            "statut" to statut,
            "vitesse" to vitesse
        )

        // Créer une requête unique pour exécuter le Worker après un délai
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<RequeteWorkerult>()
            .setInitialDelay(intervalleMillis, TimeUnit.MILLISECONDS) // Appliquer le délai
            .setInputData(data) // Ajouter les données
            .build()

        // Planifier le Worker avec WorkManager
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
    }
}

class RequeteWorkerult(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var serverIP: String
    private lateinit var serverPort: String

    override fun doWork(): Result {
        // Récupérer les données nécessaires depuis SharedPreferences
        val sharedPrefs = applicationContext.getSharedPreferences("ServeurPrefs", Context.MODE_PRIVATE)
        serverIP = sharedPrefs.getString("IP", "") ?: ""
        serverPort = sharedPrefs.getString("Port", "") ?: ""

        // Récupérer les données passées au Worker
        val statut = inputData.getString("statut") ?: "inconnu"
        val vitesse = inputData.getString("vitesse") ?: "inconnu"

        // Envoyer les données via une requête POST
        envoyerRequetePost(statut, vitesse)

        return Result.success() // Signaler la réussite de la tâche
    }

    // Envoyer la requête POST avec les données en clair (pas de chiffrement)
    private fun envoyerRequetePost(statut: String, vitesse: String) {
        val url = URL("http://$serverIP:$serverPort")

        val dataToSend = JSONObject().apply {
            put("statut", statut)
            put("vitesse", vitesse)
        }

        // Convertir les données en chaîne JSON
        val dataToSendString = dataToSend.toString()

        Thread {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val outputStream: OutputStream = connection.outputStream
                outputStream.write(dataToSendString.toByteArray())
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    reader.forEachLine {
                        response.append(it)
                    }
                } else {
                    println("Erreur: Code $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
