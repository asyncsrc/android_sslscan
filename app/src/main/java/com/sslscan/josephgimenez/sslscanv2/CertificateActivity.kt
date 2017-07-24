package com.sslscan.josephgimenez.sslscanv2

import android.os.Bundle
import android.app.Activity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_certificate.*

class CertificateActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate)

        val certDetails = intent.extras.getParcelable<CertDetails>("certificate")

        val certDetailsArray = arrayOf(
                "Issuer DN: ${certDetails.issuerDN}",
                "Subject DN: ${certDetails.subjectDN}",
                "Serial Number: ${certDetails.serialNumber}",
                "Signature Algorithm: ${certDetails.signatureAlgorithm}",
                "Expiration Date: ${certDetails.notAfterDate}")

        val listAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, certDetailsArray)
        this.lstCertDetails.adapter = listAdapter
    }

}
