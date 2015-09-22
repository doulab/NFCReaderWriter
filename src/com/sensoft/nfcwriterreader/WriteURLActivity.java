package com.sensoft.nfcwriterreader;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nfcwriterreader.R;

public class WriteURLActivity extends FragmentActivity {

	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	private String urlAddress = "";

	TextView messageText;
	EditText urlEditText;
	Button writeUrlButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_write_url);
		urlEditText = (EditText) this.findViewById(R.id.urlEditText);
		messageText = (TextView) findViewById(R.id.messageText);
		writeUrlButton = (Button) this.findViewById(R.id.btn_write_url);
		writeUrlButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				urlAddress = urlEditText.getText().toString();

				messageText
						.setText("Positioner le NTAG pour l'Žcriture  http://www."
								+ urlAddress);

			}
		});

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		// ndef.addDataScheme("http");
		mFilters = new IntentFilter[] { ndef, };
		mTechLists = new String[][] { new String[] { Ndef.class.getName() },
				new String[] { NdefFormatable.class.getName() } };

	}

	@Override
	public void onResume() {
		super.onResume();
		if (mNfcAdapter != null)
			mNfcAdapter.enableForegroundDispatch(this, mPendingIntent,
					mFilters, mTechLists);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.i("Envoi de premier plan", "Tag DŽcouvert avec l'intention: " + intent);
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		byte[] uriField = urlAddress.getBytes(Charset.forName("US-ASCII"));
		byte[] payload = new byte[uriField.length + 1]; 
		
		payload[0] = 0x01; // prefixes http://www. to the URI
		System.arraycopy(uriField, 0, payload, 1, uriField.length); // appends
																	// URI to
																	// payload
		NdefRecord URIRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_URI, new byte[0], payload);
		NdefMessage newMessage = new NdefMessage(new NdefRecord[] { URIRecord });
		writeNdefMessageToTag(newMessage, tag);
	}

	@Override
	public void onPause() {
		super.onPause();
		mNfcAdapter.disableForegroundDispatch(this);
	}

	boolean writeNdefMessageToTag(NdefMessage message, Tag detectedTag) {
		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(detectedTag);
			if (ndef != null) {
				ndef.connect();

				if (!ndef.isWritable()) {
					Toast.makeText(this, "Tag est en lecture seule.",
							Toast.LENGTH_SHORT).show();
					return false;
				}
				if (ndef.getMaxSize() < size) {
					Toast.makeText(
							this,
							"Les donnŽes ne peuvent pas Žcrit au tag, la capacitŽ de Tag est "
									+ ndef.getMaxSize()
									+ " bytes, message est " + size + " bytes.",
							Toast.LENGTH_SHORT).show();
					return false;
				}

				ndef.writeNdefMessage(message);
				ndef.close();
				Toast.makeText(this, "Le message est Žcrit tag.",
						Toast.LENGTH_SHORT).show();
				return true;
			} else {
				NdefFormatable ndefFormat = NdefFormatable.get(detectedTag);
				if (ndefFormat != null) {
					try {
						ndefFormat.connect();
						ndefFormat.format(message);
						ndefFormat.close();
						Toast.makeText(this,
								"Les donnŽes sont Žcrites ˆ la balise ",
								Toast.LENGTH_SHORT).show();
						return true;
					} catch (IOException e) {
						Toast.makeText(this, "ƒchec au format tag",
								Toast.LENGTH_SHORT).show();
						return false;
					}
				} else {
					Toast.makeText(this, "NDEF n'est pas pris en charge",
							Toast.LENGTH_SHORT).show();
					return false;
				}
			}
		} catch (Exception e) {
			Toast.makeText(this, "Ecrire opreation a ŽchouŽ",
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}
}
