package com.sensoft.nfcwriterreader;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nfcwriterreader.R;

/**
 * Activity pour lecture depuis un NDEF Tag.
 * 
 * @author Abdoulaye DIALLO
 * 
 */
public class ReadActivity extends FragmentActivity {

	public static final String MIME_TEXT_PLAIN = "text/plain";
	public static final String TAG = "NfcDemo";

	private TextView mTextView;
	private NfcAdapter mNfcAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mTextView =(TextView)findViewById(R.id.txt_read_nfc);
		if (mNfcAdapter == null) {
			// Stop ici si le terminal ne comprte pas de lecteur NFC
			Toast.makeText(this, "Cet appareil ne supporte pas la lecture NFC",
					Toast.LENGTH_LONG).show();
			finish();
			return;

		}

		if (!mNfcAdapter.isEnabled()) {
			mTextView.setText("NFC is disabled.");
		} else {
			mTextView.setText(R.string.tap_something);
		}

		handleIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * Il est important que l'activité est au premier plan (reprise).
		 * Autrement une IllegalStateException est levée.
		 */
		setupForegroundDispatch(this, mNfcAdapter);
	}

	@Override
	protected void onPause() {
		/*
		 * Appelez cela avant OnPause, sinon un IllegalArgumentException est
		 * jeté ainsi.
		 */
		stopForegroundDispatch(this, mNfcAdapter);

		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		/*
		 * Cette méthode est appelée, quand un nouveau intention se associé à
		 * l'instance d'activité actuel. Au lieu de créer une nouvelle activité,
		 * onNewIntent sera appelé. Dans notre cas, cette méthode est appelée,
		 * lorsque l'utilisateur attache une étiquette à l'appareil.
		 */
		handleIntent(intent);
	}

	/*
	 * handleIntent(Intent intent) methode permetant la lecture d'un tag
	 */

	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

			String type = intent.getType();
			if (MIME_TEXT_PLAIN.equals(type)) {

				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				new NdefReaderTask().execute(tag);

			} else {
				Log.d(TAG, "type de mime incorrect: " + type);
			}
		} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

			// Dans le cas où nous devrions utiliser Tech Découvert 
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] techList = tag.getTechList();
			String searchedTech = Ndef.class.getName();

			for (String tech : techList) {
				if (searchedTech.equals(tech)) {
					new NdefReaderTask().execute(tag);
					break;
				}
			}
		}
	}

	/**
	 * @param Activity
	 *             correspondante demandant l'envoi au premier plan.
	 * @param NfcAdapter
	 *            utilisé pour l'envoi de premier plan.
	 */
	public static void setupForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(),
				activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(
				activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][] {};

		// Notez que ceci est le même filtre que dans notre manifeste.
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Vérifiez votre type mime.");
		}

		adapter.enableForegroundDispatch(activity, pendingIntent, filters,
				techList);
	}

	/**
	 * @param Activité
	 *            correspondant demandant d'arrêter l'envoi au premier plan.
	 * @param Adaptateur
	 *            NfcAdapter utilisé pour l'envoi au premier plan.
	 */
	public static void stopForegroundDispatch(final Activity activity,
			NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}

	/**
	 * Tâche d'arrière plan pour lire les données. Permet Ne pas bloquer le
	 * thread d'interface utilisateur lors de la lecture.
	 */
	private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

		@Override
		protected String doInBackground(Tag... params) {
			Tag tag = params[0];

			Ndef ndef = Ndef.get(tag);
			if (ndef == null) {
				// NDEF is not supported by this Tag.
				return null;
			}

			NdefMessage ndefMessage = ndef.getCachedNdefMessage();

			NdefRecord[] records = ndefMessage.getRecords();
			for (NdefRecord ndefRecord : records) {
				if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN
						&& Arrays.equals(ndefRecord.getType(),
								NdefRecord.RTD_TEXT)) {
					try {
						return readText(ndefRecord);
					} catch (UnsupportedEncodingException e) {
						Log.e(TAG, "Encodage non pris en charge", e);
					}
				}
			}

			return null;
		}

		@SuppressWarnings("deprecation")
		private String readText(NdefRecord record)
				throws UnsupportedEncodingException {
			/*
			 * See NFC forum specification for "Text Record Type Definition" at
			 * 3.2.1
			 * 
			 * http://www.nfc-forum.org/specs/
			 * 
			 * bit_7 defines encoding bit_6 reserved for future use, must be 0
			 * bit_5..0 length of IANA language code
			 */

			byte[] payload = record.getPayload();

			// obtenir le texte encoder
			String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

			// obtenir le code de langage
			int languageCodeLength = payload[0] & 0063;

			

			// obtenir le texte
			return new String(payload, languageCodeLength + 1, payload.length
					- languageCodeLength - 1, textEncoding);
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				mTextView.setText("TAG enregistrer: " + result);
			}
		}
	}
}
