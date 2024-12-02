package com.example.p3_firebase

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.p3_firebase.ui.theme.P3_FirebaseTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class DataActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        database = Firebase.database.reference

        setContent {
            P3_FirebaseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DataScreen(modifier = Modifier.padding(innerPadding), auth = auth, database = database)
                }
            }
        }
    }
}

@Composable
fun DataScreen(modifier: Modifier = Modifier, auth: FirebaseAuth, database: DatabaseReference) {
    var message by remember { mutableStateOf("") }
    var messagesList by remember { mutableStateOf(listOf<Map<String, String>>()) }
    val userId = auth.currentUser?.uid ?: "default"
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Wiadomość") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (message.isNotEmpty()) {
                val timestamp =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val messageData = mapOf(
                    "message" to message,
                    "timestamp" to timestamp
                )
                database.child("users").child(userId).child("messages").push().setValue(messageData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Wiadomość zapisana pomyślnie")
                        message = ""
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firebase", "Błąd przy zapisie wiadomości", exception)
                    }
            }
            else {
                Log.e("Firebase", "Wiadomość jest pusta")
                Toast.makeText(context, "Wiadomość jest pusta", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Zapisz wiadomość do Firebase")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            database.child("users").child(userId).child("messages").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val messages = mutableListOf<Map<String, String>>()

                    val genericTypeIndicator = object : GenericTypeIndicator<Map<String, String>>() {}

                    for (snapshot in dataSnapshot.children) {
                        val messageData = snapshot.getValue(genericTypeIndicator)
                        if (messageData != null) {
                            messages.add(messageData)
                        }
                    }
                    messagesList = messages
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Błąd przy odczycie wiadomości", databaseError.toException())
                }
            })
        }) {
            Text("Pokaż historię wiadomości")
        }

        Spacer(modifier = Modifier.height(16.dp))

        messagesList.forEach { msg ->
            Text(text = "${msg["timestamp"]}: ${msg["message"]}")
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DataScreenPreview() {
    P3_FirebaseTheme {
        DataScreen(auth = FirebaseAuth.getInstance(), database = Firebase.database.reference)
    }
}