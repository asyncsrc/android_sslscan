package com.sslscan.josephgimenez.sslscanv2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import java.math.BigInteger
import java.net.URL
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection

// TODO: Need to support CA Cert Serial Numbers as well
fun formatSerialNumber(serialNumber: BigInteger) : String {
    val serial = StringBuilder(serialNumber.toString(16))

    for (num in 2..serial.length+6 step 3) {
        serial.insert(num, "-")
    }

    return serial.toString().toUpperCase()
}

data class CertDetails(val description: String,
                       val basicConstraints: Int,
                       val serialNumber: String?,
                       val subjectDN: String?,
                       val issuerDN: String?,
                       val signatureAlgorithm: String,
                       val notAfterDate: Date) : Parcelable {
    val isCA: Boolean
    get() = this.basicConstraints != -1

    override fun toString(): String {
        val description = if (isCA) {
            "Certificate Issuer:\n$description\n\nCA Cert: $isCA"
        } else {
            "Certificate Subject Name:\n$description\n\nCA Cert: $isCA"
        }
        return description
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<CertDetails> = object : Parcelable.Creator<CertDetails> {
            override fun createFromParcel(source: Parcel): CertDetails = CertDetails(source)
            override fun newArray(size: Int): Array<CertDetails?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
    source.readString(),
    source.readInt(),
    source.readString(),
    source.readString(),
    source.readString(),
    source.readString(),
    source.readSerializable() as Date
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(description)
        dest.writeInt(basicConstraints)
        dest.writeString(serialNumber)
        dest.writeString(subjectDN)
        dest.writeString(issuerDN)
        dest.writeString(signatureAlgorithm)
        dest.writeSerializable(notAfterDate)
    }
}

class MainActivity : Activity(), AnkoLogger {
    private lateinit var certificates: List<CertDetails>

    internal var itemClickListener: AdapterView.OnItemClickListener =
            AdapterView.OnItemClickListener { listView, itemView, position, id ->
        val intent = Intent(this@MainActivity, CertificateActivity::class.java)
        intent.putExtra("certificate", certificates[position])
        startActivity(intent)
    }

    fun showCertDetails(certs: Array<Certificate>) {
        this.progressBar.visibility = View.INVISIBLE

        val certsRetrieved = certs
                .asSequence()
                .filterIsInstance(X509Certificate::class.java)
                .map {
                    val description : String = (it.subjectDN.name ?: it.issuerDN.name).substringBefore(",")
                    CertDetails(
                            description,
                            it.basicConstraints,
                            if (it.basicConstraints == -1) { formatSerialNumber(it.serialNumber) } else { null },
                            it.subjectDN?.name,
                            it.issuerDN?.name,
                            it.sigAlgName,
                            it.notAfter)
                }
                .toList()

        certificates = certsRetrieved

        val listAdapter = ArrayAdapter<CertDetails>(this, android.R.layout.simple_list_item_1, certsRetrieved)

        runOnUiThread {
            this.lstCertificates.adapter = listAdapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.lstCertificates.onItemClickListener = itemClickListener

        RxView.clicks(this.btnLookupSSL)
            .observeOn(Schedulers.io())
            .map {
                URL("https://${this.txtURL.text}")
                        .openConnection() as HttpsURLConnection
            }
            .doOnNext { it.connect() }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe {
                this.progressBar.visibility = View.INVISIBLE
                showCertDetails(it.serverCertificates)
            }
    }
}
