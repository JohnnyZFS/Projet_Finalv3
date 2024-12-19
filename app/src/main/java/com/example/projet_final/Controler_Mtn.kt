package com.example.projet_final

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projet_final.databinding.ActivityControlerMtnBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class Controler_Mtn : AppCompatActivity() {

    private lateinit var binding: ActivityControlerMtnBinding // Le binding pour l'activité
    private lateinit var serverIP: String
    private lateinit var serverPort: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser le view binding
        binding = ActivityControlerMtnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupérer les données depuis SharedPreferences
        val sharedPrefs = getSharedPreferences("ServeurPrefs", MODE_PRIVATE)
        serverIP = sharedPrefs.getString("IP", "") ?: ""
        serverPort = sharedPrefs.getString("Port", "") ?: ""

        // Log ou utiliser ces valeurs pour votre logique
        Log.d("Obtenir_Etat", "IP: $serverIP, Port: $serverPort")

        // Ajouter un listener au bouton pour envoyer la requête
        binding.buttonEnvoyer.setOnClickListener {
            val statut = binding.spinnerstatut.selectedItem.toString() // Récupérer la valeur sélectionnée dans le spinner statut
            val vitesse = binding.spinnerVitesse.selectedItem.toString() // Récupérer la valeur sélectionnée dans le spinner vitesse
            envoyerRequetePost(statut, vitesse)
        }

        // Bouton "Accueil" : Retour au MainActivity
        binding.buttonAcceuill.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonhr.setOnClickListener {
            val intent = Intent(this, Planifier_intervalle::class.java)
            startActivity(intent)
        }
    }

    // Envoyer la requête POST sans chiffrement
    private fun envoyerRequetePost(statut: String, vitesse: String) {
        val url = URL("http://$serverIP:$serverPort")

        // Préparer les données à envoyer
        val dataToSend = JSONObject().apply {
            put("statut", statut)
            put("vitesse", vitesse)
        }

        // Créer la requête HTTP
        Thread {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Écrire les données dans la requête
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(dataToSend.toString().toByteArray())
                outputStream.flush()

                // Lire la réponse du serveur
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    reader.forEachLine {
                        response.append(it)
                    }

                    // Afficher la réponse dans un Toast
                    runOnUiThread {
                        Toast.makeText(this, "Réponse du serveur: $response", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Erreur: Code $responseCode", Toast.LENGTH_LONG).show()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Erreur lors de l'envoi de la requête", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
