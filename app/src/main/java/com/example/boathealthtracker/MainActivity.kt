//package com.example.boathealthtracker
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.bluetooth.*
//import android.bluetooth.le.BluetoothLeScanner
//import android.bluetooth.le.ScanCallback
//import android.bluetooth.le.ScanResult
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.*
//import android.util.Log
//import android.widget.*
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var connectButton: Button
//    private lateinit var statusText: TextView
//    private lateinit var heartRateText: TextView
//    private lateinit var stepCountText: TextView
//    private lateinit var caloriesText: TextView
//
//    private var gatt: BluetoothGatt? = null
//    private val devicesList = mutableListOf<BluetoothDevice>()
//    private lateinit var deviceAdapter: ArrayAdapter<String>
//    private val handler = Handler(Looper.getMainLooper())
//
//    private val serviceUUID = UUID.fromString("00002760-0000-1000-8000-00805F9B34FB")
//    private val heartRateUUID = UUID.fromString("00002762-0000-1000-8000-00805F9B34FB")
//    private val stepCountUUID = UUID.fromString("00002763-0000-1000-8000-00805F9B34FB")
//    private val caloriesUUID = UUID.fromString("00002764-0000-1000-8000-00805F9B34FB")
//
//    private val REQUEST_ENABLE_BT = 1
//    private var bluetoothLeScanner: BluetoothLeScanner? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothAdapter = bluetoothManager.adapter
//        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//
//        connectButton = findViewById(R.id.connectButton)
//        statusText = findViewById(R.id.statusText)
//        heartRateText = findViewById(R.id.heartRateText)
//        stepCountText = findViewById(R.id.stepCountText)
//        caloriesText = findViewById(R.id.caloriesText)
//
//        checkAndRequestPermissions()
//
//        connectButton.setOnClickListener {
//            ensureBluetoothEnabled()
//        }
//    }
//
//    /** ✅ Ensure Bluetooth is Enabled Before Scanning **/
//    @SuppressLint("MissingPermission")
//    private fun ensureBluetoothEnabled() {
//        if (!bluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        } else {
//            scanForDevices()
//        }
//    }
//
//    /** ✅ Check & Request Permissions at Runtime **/
//    private fun checkAndRequestPermissions() {
//        val permissions = mutableListOf<String>()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//        } else {
//            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
//                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//        }
//
//        if (permissions.isNotEmpty()) {
//            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
//        }
//    }
//
//    /** ✅ Scan for Bluetooth LE Devices and Show Popup **/
//    @SuppressLint("MissingPermission")
//    private fun scanForDevices() {
//        if (!hasBluetoothPermissions()) {
//            statusText.text = "Permission Denied!"
//            return
//        }
//
//        devicesList.clear()
//        val deviceNames = mutableListOf<String>()
//        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
//
//        statusText.text = "🔍 Scanning for Smartwatch..."
//        bluetoothLeScanner?.startScan(scanCallback)
//
//        handler.postDelayed({
//            bluetoothLeScanner?.stopScan(scanCallback)
//            showDeviceSelectionPopup()
//        }, 7000) // Increased scan duration
//    }
//
//    /** ✅ Correct ScanCallback Implementation **/
//    private val scanCallback = object : ScanCallback() {
//        @SuppressLint("MissingPermission")
//        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            super.onScanResult(callbackType, result)
//            val device = result?.device
//            if (device != null && !devicesList.contains(device)) {
//                devicesList.add(device)
//                deviceAdapter.add("${device.name ?: "Unknown"} - ${device.address}")
//                deviceAdapter.notifyDataSetChanged()
//            }
//        }
//    }
//
//    /** ✅ Show Popup to Select a Device **/
//    @SuppressLint("MissingPermission")
//    private fun showDeviceSelectionPopup() {
//        if (devicesList.isEmpty()) {
//            Toast.makeText(this, "No devices found. Try again!", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Select a Device to Connect")
//        builder.setAdapter(deviceAdapter) { _, which ->
//            val selectedDevice = devicesList[which]
//            connectToDevice(selectedDevice)
//        }
//        builder.setNegativeButton("Cancel", null)
//        builder.show()
//    }
//
//    /** ✅ Connect to the Selected Smartwatch via Bluetooth GATT **/
//    @SuppressLint("MissingPermission")
//    private fun connectToDevice(device: BluetoothDevice) {
//        statusText.text = "Connecting to ${device.name}..."
//        disconnectGatt()
//
//        if (device.bondState == BluetoothDevice.BOND_NONE) {
//            device.createBond()
//            handler.postDelayed({ attemptGattConnection(device) }, 5000)
//        } else {
//            attemptGattConnection(device)
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun attemptGattConnection(device: BluetoothDevice) {
//        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
//            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//                runOnUiThread {
//                    if (newState == BluetoothProfile.STATE_CONNECTED) {
//                        statusText.text = "Connected!"
//                        handler.postDelayed({ gatt.discoverServices() }, 1000)
//                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                        statusText.text = "Disconnected!"
//                        if (status == 133) {
//                            handler.postDelayed({ connectToDevice(device) }, 5000)
//                        }
//                    }
//                }
//            }
//        })
//    }
//
//    /** ✅ Disconnect & Close GATT Cleanly **/
//    @SuppressLint("MissingPermission")
//    /** ✅ Disconnect & Close GATT Cleanly **/
//
//    private fun disconnectGatt() {
//        if (!hasBluetoothPermissions()) {
//            Log.e("Bluetooth", "❌ Missing Bluetooth permissions. Cannot disconnect.")
//            return
//        }
//
//        if (gatt != null) {
//            Log.d("Bluetooth", "🔌 Disconnecting GATT...")
//
//            // Disconnect the current GATT connection
//            gatt?.disconnect()
//
//            // Add a short delay before closing to ensure clean disconnection
//            handler.postDelayed({
//                Log.d("Bluetooth", "🚫 Closing GATT connection...")
//                gatt?.close()
//                gatt = null
//                Log.d("Bluetooth", "✅ GATT fully disconnected and closed.")
//            }, 1500) // 1.5 seconds delay to ensure a clean disconnect
//        } else {
//            Log.d("Bluetooth", "⚠ No active GATT connection to disconnect.")
//        }
//    }
//
//
//    /** ✅ Check for Bluetooth Permissions **/
//    private fun hasBluetoothPermissions(): Boolean {
//        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun hasPermission(permission: String): Boolean {
//        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
//    }
//}





//package com.example.boathealthtracker
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.bluetooth.*
//import android.bluetooth.le.BluetoothLeScanner
//import android.bluetooth.le.ScanCallback
//import android.bluetooth.le.ScanResult
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.*
//import android.util.Log
//import android.widget.*
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var connectButton: Button
//    private lateinit var statusText: TextView
//    private lateinit var heartRateText: TextView
//    private lateinit var stepCountText: TextView
//    private lateinit var caloriesText: TextView
//
//    private var gatt: BluetoothGatt? = null
//    private val devicesList = mutableListOf<BluetoothDevice>()
//    private lateinit var deviceAdapter: ArrayAdapter<String>
//    private val handler = Handler(Looper.getMainLooper())
//
//    private var connectionRetries = 0
//    private var isConnecting = false
//
//    private val REQUEST_ENABLE_BT = 1
//    private var bluetoothLeScanner: BluetoothLeScanner? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothAdapter = bluetoothManager.adapter
//        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//
//        connectButton = findViewById(R.id.connectButton)
//        statusText = findViewById(R.id.statusText)
//        heartRateText = findViewById(R.id.heartRateText)
//        stepCountText = findViewById(R.id.stepCountText)
//        caloriesText = findViewById(R.id.caloriesText)
//
//        checkAndRequestPermissions()
//
//        connectButton.setOnClickListener {
//            ensureBluetoothEnabled()
//        }
//    }
//
//    /** ✅ Ensure Bluetooth is Enabled Before Scanning **/
//    @SuppressLint("MissingPermission")
//    private fun ensureBluetoothEnabled() {
//        if (!bluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        } else {
//            scanForDevices()
//        }
//    }
//
//    /** ✅ Check & Request Permissions at Runtime **/
//    private fun checkAndRequestPermissions() {
//        val permissions = mutableListOf<String>()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//        } else {
//            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
//                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//        }
//
//        if (permissions.isNotEmpty()) {
//            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
//        }
//    }
//
//    /** ✅ Scan for Bluetooth LE Devices and Show Popup **/
//    @SuppressLint("MissingPermission")
//    private fun scanForDevices() {
//        if (!hasBluetoothPermissions()) {
//            statusText.text = "Permission Denied!"
//            return
//        }
//
//        devicesList.clear()
//        val deviceNames = mutableListOf<String>()
//        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
//
//        statusText.text = "🔍 Scanning for Smartwatch..."
//        bluetoothLeScanner?.startScan(scanCallback)
//
//        handler.postDelayed({
//            bluetoothLeScanner?.stopScan(scanCallback)
//            showDeviceSelectionPopup()
//        }, 8000) // Increased scan duration to 8 seconds
//    }
//
//    /** ✅ Scan Callback Implementation **/
//    private val scanCallback = object : ScanCallback() {
//        @SuppressLint("MissingPermission")
//        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            super.onScanResult(callbackType, result)
//            val device = result?.device
//            if (device != null && !devicesList.contains(device)) {
//                devicesList.add(device)
//                deviceAdapter.add("${device.name ?: "Unknown"} - ${device.address}")
//                deviceAdapter.notifyDataSetChanged()
//            }
//        }
//    }
//
//    /** ✅ Show Popup to Select a Device **/
//    @SuppressLint("MissingPermission")
//    private fun showDeviceSelectionPopup() {
//        if (devicesList.isEmpty()) {
//            Toast.makeText(this, "No devices found. Try again!", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Select a Device to Connect")
//        builder.setAdapter(deviceAdapter) { _, which ->
//            val selectedDevice = devicesList[which]
//            connectToDevice(selectedDevice)
//        }
//        builder.setNegativeButton("Cancel", null)
//        builder.show()
//    }
//
//    /** ✅ Connect to the Selected Smartwatch via Bluetooth GATT **/
//    @SuppressLint("MissingPermission")
//    private fun connectToDevice(device: BluetoothDevice) {
//        if (isConnecting) {
//            return
//        }
//        isConnecting = true
//        statusText.text = "Connecting to ${device.name}..."
//        disconnectGatt()
//
//        if (device.bondState == BluetoothDevice.BOND_NONE) {
//            device.createBond()
//            handler.postDelayed({ attemptGattConnection(device) }, 5000)
//        } else {
//            attemptGattConnection(device)
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun attemptGattConnection(device: BluetoothDevice) {
//        gatt = device.connectGatt(this, true, object : BluetoothGattCallback() {
//            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//                runOnUiThread {
//                    isConnecting = false
//                    if (newState == BluetoothProfile.STATE_CONNECTED) {
//                        Log.d("Bluetooth", "✅ Connected to ${device.name}")
//                        statusText.text = "Connected!"
//                        connectionRetries = 0
//                        handler.postDelayed({ gatt.discoverServices() }, 1500)
//                    } else {
//                        Log.e("Bluetooth", "❌ Connection failed")
//                        statusText.text = "Connection Failed. Retrying..."
//                        if (connectionRetries < 3) {
//                            connectionRetries++
//                            handler.postDelayed({ connectToDevice(device) }, 5000)
//                        } else {
//                            statusText.text = "Failed to connect after multiple attempts."
//                        }
//                    }
//                }
//            }
//        })
//    }
//
//    /** ✅ Disconnect & Close GATT Cleanly **/
//    @SuppressLint("MissingPermission")
//    private fun disconnectGatt() {
//        gatt?.disconnect()
//        handler.postDelayed({
//            gatt?.close()
//            gatt = null
//        }, 2000)
//    }
//
//    /** ✅ Check for Bluetooth Permissions **/
//    private fun hasBluetoothPermissions(): Boolean {
//        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun hasPermission(permission: String): Boolean {
//        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
//    }
//}



































































/*package com.example.boathealthtracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView

    private var gatt: BluetoothGatt? = null
    private val devicesList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private val handler = Handler(Looper.getMainLooper())

    private var isConnecting = false
    private var connectionRetries = 0
    private val REQUEST_ENABLE_BT = 1
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        connectButton = findViewById(R.id.connectButton)
        statusText = findViewById(R.id.statusText)

        checkAndRequestPermissions()

        connectButton.setOnClickListener {
            ensureBluetoothEnabled()
        }
    }

    *//** ✅ Ensure Bluetooth is Enabled Before Scanning **//*
    @SuppressLint("MissingPermission")
    private fun ensureBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            scanForDevices()
        }
    }

    *//** ✅ Check & Request Permissions at Runtime **//*
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        }
    }

    *//** ✅ Scan for Bluetooth LE Devices and Auto-Select Smartwatch **//*
    @SuppressLint("MissingPermission")
    private fun scanForDevices() {
        if (!hasBluetoothPermissions()) {
            statusText.text = "Permission Denied!"
            return
        }

        devicesList.clear()
        val deviceNames = mutableListOf<String>()
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)

        statusText.text = "🔍 Scanning for Smartwatch..."
        bluetoothLeScanner?.startScan(scanCallback)

        handler.postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            autoSelectSmartwatch()
        }, 8000) // Scan for 8 seconds
    }

    *//** ✅ Scan Callback **//*
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            if (device != null && !devicesList.contains(device)) {
                devicesList.add(device)
                deviceAdapter.add("${device.name ?: "Unknown"} - ${device.address}")
                deviceAdapter.notifyDataSetChanged()
            }
        }
    }

    *//** ✅ Auto-Select Smartwatch Based on Name **//*
    @SuppressLint("MissingPermission")
    private fun autoSelectSmartwatch() {
        val targetDevice = devicesList.find {
            it.name?.contains("MAGMABGT_DCE7", ignoreCase = true) == true ||
                    it.name?.contains("MAGMA_DCE7", ignoreCase = true) == true
        }

        if (targetDevice != null) {
            bondAndConnect(targetDevice)
        } else {
            Toast.makeText(this, "No MAGMA smartwatch found. Try again!", Toast.LENGTH_SHORT).show()
        }
    }

    *//** ✅ Bond with Device Before Connecting **//*
    @SuppressLint("MissingPermission")
    private fun bondAndConnect(device: BluetoothDevice) {
        if (isConnecting) return
        isConnecting = true
        statusText.text = "🔗 Pairing with ${device.name}..."

        clearBluetoothCache(device)

        if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond()
            handler.postDelayed({ attemptGattConnection(device) }, 6000) // Wait 6 sec for bonding
        } else {
            attemptGattConnection(device)
        }
    }

    *//** ✅ Attempt GATT Connection with Retry Logic **//*
    @SuppressLint("MissingPermission")
    private fun attemptGattConnection(device: BluetoothDevice) {
        disconnectGatt()

        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                runOnUiThread {
                    isConnecting = false
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("Bluetooth", "✅ Connected to ${device.name}")
                        statusText.text = "Connected!"
                        connectionRetries = 0
                        handler.postDelayed({ gatt.discoverServices() }, 1500)
                    } else {
                        Log.e("Bluetooth", "❌ Connection failed (Error Code: $status)")
                        statusText.text = "Connection Failed. Retrying..."

                        if (status == 133 || status == 135) {
                            clearBluetoothCache(device)
                        }

                        if (connectionRetries < 3) {
                            connectionRetries++
                            handler.postDelayed({ attemptGattConnection(device) }, 5000)
                        } else {
                            statusText.text = "Failed to connect after multiple attempts."
                        }
                    }
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    *//** ✅ Clear Bluetooth Cache Before Connection **//*
    @SuppressLint("MissingPermission")
    private fun clearBluetoothCache(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device)
            Log.d("Bluetooth", "✅ Cleared Bluetooth cache for ${device.address}")
        } catch (e: Exception) {
            Log.e("Bluetooth", "❌ Failed to clear Bluetooth cache: ${e.message}")
        }
    }

    *//** ✅ Disconnect & Close GATT Cleanly **//*
    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        gatt?.disconnect()
        handler.postDelayed({
            gatt?.close()
            gatt = null
        }, 2000)
    }

    *//** ✅ Check for Bluetooth Permissions **//*
    private fun hasBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}*/
/*package com.example.boathealthtracker

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var stepCountText: TextView
    private lateinit var caloriesText: TextView

    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isReconnecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        connectButton = findViewById(R.id.connectButton)
        statusText = findViewById(R.id.statusText)
        heartRateText = findViewById(R.id.heartRateText)
        stepCountText = findViewById(R.id.stepCountText)
        caloriesText = findViewById(R.id.caloriesText)

        checkAndRequestPermissions()

        connectButton.setOnClickListener {
            ensureBluetoothEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    private fun ensureBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        } else {
            scanForDevices()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanForDevices() {
        statusText.text = "🔍 Scanning for Smartwatch..."
        bluetoothLeScanner?.startScan(scanCallback)
        handler.postDelayed({ bluetoothLeScanner?.stopScan(scanCallback) }, 8000)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            if (device != null && device.name?.contains("MAGMA", ignoreCase = true) == true) {
                bluetoothLeScanner?.stopScan(this)
                attemptGattConnection(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptGattConnection(device: BluetoothDevice) {
        gatt?.close()
        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                runOnUiThread {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        statusText.text = "✅ Connected to ${device.name}!"
                        handler.postDelayed({ gatt.discoverServices() }, 1500)
                    } else {
                        statusText.text = "❌ Connection Failed (Error: $status)"
                        if (status == 133 && !isReconnecting) {
                            isReconnecting = true
                            handler.postDelayed({ attemptGattConnection(device) }, 3000)
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (service in gatt.services) {
                        for (characteristic in service.characteristics) {
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                                enableNotification(gatt, characteristic)
                            }
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                                gatt.readCharacteristic(characteristic)
                                Log.d("Bluetooth", "📖 Requesting manual read from ${characteristic.uuid}")
                            }
                        }
                    }
                    sendStartCommand(gatt)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                val dataString = data?.joinToString { String.format("%02X", it) }

                Log.d("Bluetooth", "🔵 Data received from ${characteristic.uuid}: $dataString")

                runOnUiThread {
                    when (characteristic.uuid.toString().uppercase()) {
                        "000027F7-0000-1000-8000-00805F9B34FB" -> heartRateText.text = "❤️ Heart Rate: ${convertHexToDecimal(dataString)} bpm"
                        "000027F2-0000-1000-8000-00805F9B34FB" -> stepCountText.text = "👣 Steps: ${convertHexToDecimal(dataString)}"
                        "00002762-0000-1000-8000-00805F9B34FB" -> caloriesText.text = "🔥 Calories: ${convertHexToDecimal(dataString)} kcal"
                    }
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.d("Bluetooth", "✅ Notification enabled for ${characteristic.uuid}")
        } else {
            Log.e("Bluetooth", "❌ Descriptor 0x2902 not found for ${characteristic.uuid}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendStartCommand(gatt: BluetoothGatt) {
        val startCharacteristic = gatt.getService(UUID.fromString("00002760-0000-1000-8000-00805F9B34FB"))
            ?.getCharacteristic(UUID.fromString("00002763-0000-1000-8000-00805F9B34FB"))

        if (startCharacteristic != null) {
            startCharacteristic.value = byteArrayOf(0x01)  // Example command, change if needed
            gatt.writeCharacteristic(startCharacteristic)
            Log.d("Bluetooth", "✉️ Sent Start Command to ${startCharacteristic.uuid}")
        } else {
            Log.e("Bluetooth", "❌ Start command characteristic not found!")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun convertHexToDecimal(hexString: String?): Int {
        return try {
            hexString?.replace(" ", "")?.toInt(16) ?: 0
        } catch (e: Exception) {
            Log.e("Bluetooth", "❌ Error converting hex to decimal: ${e.message}")
            0
        }
    }
}*/




package com.example.boathealthtracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectButton: Button
    private lateinit var checkButton: Button
    private lateinit var statusText: TextView
    private lateinit var heartRateInput: EditText
    private lateinit var spo2Input: EditText

    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isReconnecting = false
    private var retryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        connectButton = findViewById(R.id.connectButton)
        checkButton = findViewById(R.id.checkButton)
        statusText = findViewById(R.id.statusText)
        heartRateInput = findViewById(R.id.heartRateInput)
        spo2Input = findViewById(R.id.spo2Input)

        checkAndRequestPermissions()

        connectButton.setOnClickListener {
            ensureBluetoothEnabled()
        }

        checkButton.setOnClickListener {
            checkForAnomaly()
        }
    }

    @SuppressLint("MissingPermission")
    private fun ensureBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        } else {
            scanForDevices()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (!hasPermission(Manifest.permission.BLUETOOTH) ||
            !hasPermission(Manifest.permission.BLUETOOTH_ADMIN) ||
            !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun scanForDevices() {
        statusText.text = "🔍 Scanning for Smartwatch..."
        bluetoothLeScanner?.startScan(scanCallback)
        handler.postDelayed({ bluetoothLeScanner?.stopScan(scanCallback) }, 8000)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            if (device != null && device.name?.contains("MAGMA", ignoreCase = true) == true) {
                bluetoothLeScanner?.stopScan(this)
                attemptGattConnection(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptGattConnection(device: BluetoothDevice) {
        retryCount = 0
        closeGattConnection()

        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                runOnUiThread {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        statusText.text = "✅ Connected to ${device.name}!"
                        handler.postDelayed({ gatt.discoverServices() }, 1500)
                    } else {
                        statusText.text = "❌ Connection Failed (Error: $status)"
                        if (status == 133 && retryCount < 3) {
                            retryCount++
                            closeGattConnection()
                            handler.postDelayed({ attemptGattConnection(device) }, 3000)
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (service in gatt.services) {
                        for (characteristic in service.characteristics) {
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                                enableNotification(gatt, characteristic)
                            }
                        }
                    }
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun closeGattConnection() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    private fun checkForAnomaly() {
        val heartRate = heartRateInput.text.toString().toIntOrNull() ?: 0
        val spo2 = spo2Input.text.toString().toIntOrNull() ?: 0

        if (heartRate > 100) {
            sendNotification("⚠️ High Heart Rate!", "Heart rate detected above 100 BPM: $heartRate BPM.")
        }

        if (spo2 < 90) {
            sendNotification("⚠️ Low SpO2 Level!", "SpO2 level detected below 90%: $spo2%.")
        }
    }

    @SuppressLint("NotificationPermission")
    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "HEALTH_ALERTS"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Health Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifies about health anomalies"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(500, 1000, 500, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(1, notification)
    }
}
