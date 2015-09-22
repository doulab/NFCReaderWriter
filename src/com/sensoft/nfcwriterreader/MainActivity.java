package com.sensoft.nfcwriterreader;

import com.example.nfcwriterreader.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	Button btn_write,btn_read;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	}
   
	public void readNFC(View view) 
	{
	    Intent intent = new Intent(MainActivity.this, ReadActivity.class);
	    startActivity(intent);
	}
	
	
	public void writeNFC(View view) 
	{
	    Intent intent = new Intent(MainActivity.this, WriteActivity.class);
	    startActivity(intent);
	}
	public void writeURL(View view) 
	{
	    Intent intent = new Intent(MainActivity.this, WriteURLActivity.class);
	    startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
