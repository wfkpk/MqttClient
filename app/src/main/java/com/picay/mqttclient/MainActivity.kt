package com.picay.mqttclient

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.picay.mqttclient.ui.theme.MqttClientTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MqttClientTheme {
                MqttApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttApp() {
    val context = LocalContext.current
    var brokerIp by remember { mutableStateOf("192.168.1.38") } // Default IP
    var brokerPort by remember { mutableStateOf("1883") } // Default Port
    var brokerAddress by remember { mutableStateOf("tcp://$brokerIp:$brokerPort") }
    var isConnected by remember { mutableStateOf(false) }
    var showConnectionFailedDialog by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val maxConnectionTime = 10000L // 10 seconds

    val mqttClient = remember(brokerAddress) {
        MqttClientHelper(
            context,
            brokerAddress,
            "your_client_id" // Replace with your client ID
        )
    }

    var messageToSend by remember { mutableStateOf("") }
    var topicToSubscribe by remember { mutableStateOf("test/res") } // Default to test/res

    LaunchedEffect(isConnected) {
        if (isConnected) {
            showLoader = true
            isConnecting = true
            val startTime = System.currentTimeMillis()
            try {
                mqttClient.connect()
                while (!mqttClient.connectionStatus && System.currentTimeMillis() - startTime < maxConnectionTime) {
                    delay(100)
                }
            } catch (e: Exception) {
                // Handle connection exception if needed
            }
            isConnecting = false
            showLoader = false
            if (!mqttClient.connectionStatus) {
                showConnectionFailedDialog = true
                isConnected = false
            }
        } else {
            mqttClient.disconnect()
        }
    }

    LaunchedEffect(mqttClient.connectionStatus) {
        if (mqttClient.connectionStatus) {
            mqttClient.subscribe(topicToSubscribe)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar ={
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Make the column scrollable
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = brokerIp,
                    onValueChange = { brokerIp = it },
                    label = { Text("Broker IP") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = brokerPort,
                    onValueChange = { brokerPort = it },
                    label = { Text("Broker Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        brokerAddress = "tcp://$brokerIp:$brokerPort"
                        isConnected = !isConnected
                    },
                    enabled = !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isConnected) "Disconnect" else "Connect")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Show the loader only when connecting or if the connection is not established
                if (showLoader || !mqttClient.connectionStatus) {
                    if (showLoader) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Connecting...")
                    } else {
                        Text(text = "Connection Status: Disconnected")
                    }
                }

                // Show the rest of the UI only when connected
                if (mqttClient.connectionStatus) {
                    Text(text = "Connection Status: Connected")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Subscription Status: ${if (mqttClient.isSubscribed) "Subscribed to $topicToSubscribe" else "Not Subscribed"}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Last Message Received:")
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = mqttClient.message)
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = messageToSend,
                        onValueChange = { messageToSend = it },
                        label = { Text("Message to Send") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        if (mqttClient.connectionStatus) {
                            mqttClient.publish(topicToSubscribe, messageToSend)
                        } else {
                            coroutineScope.launch {
                                Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Publish to $topicToSubscribe")
                    }
                }
            }
        }
    }
    if (showConnectionFailedDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionFailedDialog = false },
            title = { Text("Connection Failed") },
            text = { Text("Failed to connect to the MQTT broker after 10 seconds.") },
            confirmButton = {
                Button(onClick = { showConnectionFailedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MqttAppPreview() {
    MqttClientTheme {
        MqttApp()
    }
}