package jwtc.android.chess.ai;

import android.os.Bundle;

import jwtc.android.chess.R;
import jwtc.android.chess.activities.BasePreferenceActivity;

/**
 * Settings for the AI explanation feature. The API key is stored only on this device
 * in the app's default SharedPreferences and is never sent anywhere except OpenAI.
 */
public class AiPrefs extends BasePreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aiprefs);
    }
}
