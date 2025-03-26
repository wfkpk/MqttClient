package com.picay.mqttclient

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttClientHelper(private val context: Context,
                       private val serverUri: String,
                       private val clientId: String
) {
    private val TAG = "MqttClientHelper"
    private var mqttClient: MqttClient? = null
    var connectionStatus by mutableStateOf(false)
    var message by mutableStateOf("")
    var isSubscribed by mutableStateOf(false)

    init {
        Log.d(TAG, "MqttClientHelper initialized with serverUri: $serverUri, clientId: $clientId")
    }

    fun connect() {
        Log.d(TAG, "Attempting to connect to MQTT broker: $serverUri")
        try {
            val persistence = MemoryPersistence()
            mqttClient = MqttClient(serverUri, clientId, persistence)
            val connOpts = MqttConnectOptions()
            connOpts.isCleanSession = true

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    connectionStatus = false
                    isSubscribed = false
                    Log.d(TAG, "Connection lost: ${cause?.message}")
                    cause?.printStackTrace()
                }

                override fun messageArrived(topic: String?, mqttMessage: MqttMessage?) {
                    val payload = mqttMessage?.payload?.let { String(it) }
                    message = payload ?: ""
                    Log.d(TAG, "Message arrived on topic '$topic': $payload")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Delivery complete for token: ${token?.messageId}")
                }
            })

            mqttClient?.connect(connOpts)
            connectionStatus = true
            Log.d(TAG, "Connected to MQTT broker: $serverUri")
        } catch (e: MqttException) {
            connectionStatus = false
            Log.e(TAG, "Connection failed: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        Log.d(TAG, "Attempting to disconnect from MQTT broker")
        try {
            mqttClient?.disconnect()
            connectionStatus = false
            isSubscribed = false
            Log.d(TAG, "Disconnected from MQTT broker")
        } catch (e: MqttException) {
            Log.e(TAG, "Disconnect failed: ${e.message}")
            e.printStackTrace()
        } finally {
            mqttClient?.close()
            mqttClient = null
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        Log.d(TAG, "Attempting to subscribe to topic: $topic with QoS: $qos")
        try {
            mqttClient?.subscribe(topic, qos)
            isSubscribed = true
            Log.d(TAG, "Subscribed to topic: $topic with QoS: $qos")
        } catch (e: MqttException) {
            isSubscribed = false
            Log.e(TAG, "Subscribe failed for topic: $topic: ${e.message}")
            e.printStackTrace()
        }
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        Log.d(TAG, "Attempting to publish message: '$msg' to topic: $topic with QoS: $qos, retained: $retained")
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient?.publish(topic, message)
            Log.d(TAG, "Published message: '$msg' to topic: $topic with QoS: $qos, retained: $retained")
        } catch (e: MqttException) {
            Log.e(TAG, "Publish failed for topic: $topic: ${e.message}")
            e.printStackTrace()}
    }
}