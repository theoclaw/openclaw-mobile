package com.termux.app.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.termux.shared.theme.NightMode;

public final class AppAppearancePreferences {

    public static final String PREFERENCE_KEY = "clawphones_appearance_mode";

    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";

    private AppAppearancePreferences() {}

    @NonNull
    public static String getSavedMode(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = sharedPreferences.getString(PREFERENCE_KEY, MODE_SYSTEM);
        return normalizeMode(mode);
    }

    public static void applySavedMode(@NonNull Context context) {
        applyMode(getSavedMode(context));
    }

    public static void applyMode(@Nullable String modeValue) {
        NightMode nightMode = mapToNightMode(normalizeMode(modeValue));
        NightMode.setAppNightMode(nightMode.getName());
        AppCompatDelegate.setDefaultNightMode(nightMode.getMode());
    }

    @NonNull
    private static String normalizeMode(@Nullable String modeValue) {
        if (MODE_LIGHT.equals(modeValue) || MODE_DARK.equals(modeValue) || MODE_SYSTEM.equals(modeValue)) {
            return modeValue;
        }
        return MODE_SYSTEM;
    }

    @NonNull
    private static NightMode mapToNightMode(@NonNull String modeValue) {
        switch (modeValue) {
            case MODE_LIGHT:
                return NightMode.FALSE;
            case MODE_DARK:
                return NightMode.TRUE;
            case MODE_SYSTEM:
            default:
                return NightMode.SYSTEM;
        }
    }
}
