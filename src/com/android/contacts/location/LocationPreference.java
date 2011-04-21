package com.android.contacts.location;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.phone.location.PhoneLocation;
import android.preference.PreferenceManager;

public class LocationPreference {   
    private final String DAIL_SETTING_KEY="dail_ip_setting";
    private final String DAIL_SETTING_PREFIX = DAIL_SETTING_KEY + IpDailerPreference.IP_PREFIX;
    private final String DAIL_SETTING_LOCAL_NUM = DAIL_SETTING_KEY + IpDailerPreference.LOCAL_CODE;
    
    private final String DAIL_SETTING_LOCAL_CITY = DAIL_SETTING_KEY + IpDailerPreference.LOCAL_CITY;
    private final String DAIL_WITH_IP = "dail_with_ip";
    private final String DISPLAY_CITY = "pref_key_display_city";
    
    public static final int ALL_TYPE_INDEX = 0;
    
    private Context context;    
    
    public LocationPreference(Context context)
    {
    	this.context = context;
    }
    
    public String getMyLocation()
    {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	return  settings.getString(DAIL_SETTING_LOCAL_CITY,"");
    }
    
    public String getMyLocationCode()
    {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	return  settings.getString(DAIL_SETTING_LOCAL_NUM,"");
    }

    public String getIpDailerPrefix()
    {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	return  settings.getString(DAIL_SETTING_PREFIX,"");
    }
    
    public boolean isIpDailerOn(){
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	return  settings.getBoolean(DAIL_WITH_IP,false);
    }
    
    public boolean isCityDisplay(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(DISPLAY_CITY,true);
    }    
}

