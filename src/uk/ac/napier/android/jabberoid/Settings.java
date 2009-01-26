package uk.ac.napier.android.jabberoid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
		@Override
		public void onCreate(Bundle icicle) {
			super.onCreate(icicle);
			addPreferencesFromResource(R.xml.preferences);
		}
}
