package com.example.nottoday

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    protected var mLastLocation: Location? = null
    private lateinit var address: String
    var button: FloatingActionButton? = null
    var tempNumberHolder: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        button = findViewById(R.id.botao_socorro)

        button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                sendMessage()
                intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                startActivityForResult(intent, 7)
            }
        })
    }

    public override fun onActivityResult(RequestCode: Int, ResultCode: Int, ResultIntent: Intent?) {
        super.onActivityResult(RequestCode, ResultCode, ResultIntent)
        when (RequestCode) {
            7 -> if (ResultCode == RESULT_OK) {
                val uri: Uri?
                val cursor1: Cursor?
                val cursor2: Cursor?
                val TempContactID: String
                var IDresult: String? = ""
                val IDresultHolder: Int
                uri = ResultIntent!!.data
                cursor1 = contentResolver.query(uri!!, null, null, null, null)
                if (cursor1!!.moveToFirst()) {
                    TempContactID = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts._ID))
                    IDresult = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    IDresultHolder = Integer.valueOf(IDresult)
                    if (IDresultHolder == 1) {
                        cursor2 = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + TempContactID, null, null)
                        while (cursor2!!.moveToNext()) {
                            tempNumberHolder = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }
                }
            }
        }
    }

    fun sendMessage() {
        val statusPermissaoSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val statusPermissaoLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val statusPermissaoReadContact = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)


        if (statusPermissaoSMS == PackageManager.PERMISSION_GRANTED &&
                statusPermissaoLocation == PackageManager.PERMISSION_GRANTED &&
                statusPermissaoReadContact == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CONTACTS),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

        mFusedLocationClient!!.lastLocation
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLastLocation = task.result

                        val geocoder: Geocoder
                        val addresses: List<Address>
                        geocoder = Geocoder(this, Locale.getDefault())

                        addresses = geocoder.getFromLocation((mLastLocation)!!.latitude, (mLastLocation)!!.longitude, 1)

                        address = addresses[0].getAddressLine(0)
                        println(address)

                        if (tempNumberHolder == "") {
                            showMessage(getString(R.string.input_phone_empty))
                        } else {
                            if (TextUtils.isDigitsOnly(tempNumberHolder)) {
                                val smsManager: SmsManager = SmsManager.getDefault()
                                val parts: ArrayList<String>
                                //smsManager.sendTextMessage(phoneNumber, null, address, null, null)
                                parts = smsManager.divideMessage((getString(R.string.msg_help))+" "+address)
                                smsManager.sendMultipartTextMessage(tempNumberHolder, null, parts, null, null)
                                showMessage(getString(R.string.send_message))
                            } else {
                                showMessage(getString(R.string.invalid_number))
                            }
                        }

                    } else {
                        Log.w(TAG, "getLastLocation:exception", task.exception)
                        showMessage(getString(R.string.no_location_detected))
                    }
                }
    }

    private fun showMessage(text: String) {
        val container = findViewById<View>(R.id.main_activity_container)
        if (container != null) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSnackbar() {
        Toast.makeText(this@MainActivity, getString(R.string.permission_denied_explanation), Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                Log.i(TAG, getString(R.string.cancel_permission))
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                showSnackbar()
            }
        }
    }

    companion object {

        private val TAG = "LocationProvider"

        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}