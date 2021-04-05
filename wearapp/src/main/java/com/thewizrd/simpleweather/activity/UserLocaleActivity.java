package com.thewizrd.simpleweather.activity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;

public abstract class UserLocaleActivity extends FragmentActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.attachBaseContext(newBase));
    }

    protected boolean enableLocaleChangeListener() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (enableLocaleChangeListener()) {
            App.getInstance().registerAppSharedPreferenceListener(listener);
        }
    }

    @Override
    protected void onStop() {
        if (enableLocaleChangeListener()) {
            App.getInstance().unregisterAppSharedPreferenceListener(listener);
        }
        super.onStop();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (!StringUtils.isNullOrWhitespace(key)) {
            if (LocaleUtils.KEY_LANGUAGE.equals(key)) {
                ActivityCompat.recreate(UserLocaleActivity.this);
            }
        }
    };
}