package uk.ac.napier.android.jabberoid.tools;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ConnectionServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, intent.getStringExtra("test"), Toast.LENGTH_SHORT).show();
		
	}

}
