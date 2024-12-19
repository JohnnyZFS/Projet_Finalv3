package com.example.projet_final

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.projet_final.databinding.ActivityPlanifierIntervalleBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class Planifier_intervalle : AppCompatActivity() {

    private lateinit var binding: ActivityPlanifierIntervalleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanifierIntervalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bouton "Accueil" : Retour au MainActivity
        binding.btnacceill3.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.ulterieur.setOnClickListener {
            val intent = Intent(this, Planifier_ulterieur::class.java)
            startActivity(intent)
        }

        // Bouton pour sauvegarder l'intervalle choisi et planifier la tâche
        binding.buttonsauvegarder3.setOnClickListener {
            val intervalle = binding.spinneintervalle.selectedItem.toString()
            val statut = binding.spinnerstatut3.selectedItem.toString() // Récupérer le statut
            val vitesse = binding.spinnerVitesse3.selectedItem.toString() // Récupérer la vitesse

            // Convertir l'intervalle en millisecondes
            val intervalleMillis = when (intervalle) {
                "1 heure" -> TimeUnit.HOURS.toMillis(1)
                "1 jour" -> TimeUnit.DAYS.toMillis(1)
                "1 semaine" -> TimeUnit.DAYS.toMillis(7)
                else -> TimeUnit.MINUTES.toMillis(30) // Par défaut 30 minutes
            }

            // Planifier la requête avec les données récupérées
            planifierRequete(intervalleMillis, statut, vitesse)

            // Afficher un Toast avec les informations
            Toast.makeText(
                this,
                "Tâche périodique planifiée avec statut: $statut, vitesse: $vitesse, chaque $intervalle.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun planifierRequete(intervalleMillis: Long, statut: String, vitesse: String) {
        // Préparer les données pour le Worker
        val data = Data.Builder()
            .putString("statut", statut)
            .putString("vitesse", vitesse)
            .build()

        // Créer un WorkRequest périodique
        val periodicWorkRequest = PeriodicWorkRequestBuilder<RequeteWorkerinter>(intervalleMillis, TimeUnit.MILLISECONDS)
            .setInputData(data) // Ajouter les données des spinners
            .build()

        // Enregistrer le travail dans le WorkManager
        WorkManager.getInstance(this).enqueue(periodicWorkRequest)
    }
}

class RequeteWorkerinter(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

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

    // Envoyer la requête POST avec les données sans chiffrement
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
