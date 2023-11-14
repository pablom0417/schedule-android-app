package com.nmd.eventCalendar.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
    private SharedPreferences mPrefs;

    public final String AVATAR_URL_UNDEFINED = "https://www.gstatic.com/webp/gallery3/1.sm.png";
    private static String AVATAR_URL_DEFINED = "avatar_url";

    private static String USERNAME_UNDEFINED = "";
    private static String USERNAME_DEFINED = "username";

    private static String EMAIL_UNDEFINED = "";
    private static String EMAIL_DEFINED = "email";

    private static String PASSWORD_UNDEFINED = "";
    private static String PASSWORD_DEFINED = "password";


    public static AppPrefs create(Context context){
        return new AppPrefs(context);
    }


    public AppPrefs(Context context){
        mPrefs = context.getSharedPreferences(context.getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
    }

    public void putInt(String key, int value){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void putString(String key, String value){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void putBoolean(String key, boolean value){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void removePref(String key){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(key);
        editor.apply();
    }

    public void setAvatarUrl(String serverUrl){
        putString(AVATAR_URL_DEFINED, serverUrl);
    }
    public String getAvatarUrl(){
        return mPrefs.getString(AVATAR_URL_DEFINED, AVATAR_URL_UNDEFINED);
    }

    public void setUsername(String username){
        putString(USERNAME_DEFINED, username);
    }

    public String getUsername(){
        return mPrefs.getString(USERNAME_DEFINED, USERNAME_UNDEFINED);
    }

    public void setEmail(String email){
        putString(EMAIL_DEFINED, email);
    }

    public String getEmail(){
        return mPrefs.getString(EMAIL_DEFINED, EMAIL_UNDEFINED);
    }

    public void setPassword(String password){
        putString(PASSWORD_DEFINED, password);
    }

    public String getPassword(){
        return mPrefs.getString(PASSWORD_DEFINED, PASSWORD_UNDEFINED);
    }
}
