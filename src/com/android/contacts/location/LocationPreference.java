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
    private final String PREFS_NAME = "com.android.contacts";
    private final String MY_LOCATION_TAG = "MY_LOCATION";
    private final String MY_LOCATION_CODE_TAG = "MY_LOCATION_CODE";
    
    private final String IP_DAILER_PREFIX_TAG = "IP_DAILER_PREFIX";    
    private final String MY_PHONE_NUM_TAG = "MY_PHONE_NUM";
    private final String IP_DAILER_ENABLE_TAG = "IP_DAILER_ENABLE";   

    private final String DISPLAY_CITY = "pref_key_display_city";
   
    public static final int ALL_TYPE_INDEX = 0;
    
    private Context context;
    
    private final String ShanghaiPhone = "13817190000";
    private String UnknowCity = "Unkonw City";
    private final String defaultPrefix = "17951";
    private final String defaultLocationCode = "021";

    private final static String BLANK_FIELD = " ";


    
    public LocationPreference(Context context)
    {
    	this.context = context;
    	UnknowCity = context.getString(R.string.unknow_city);

    }

    public String getMyPhoneNum()
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String phone = settings.getString(MY_PHONE_NUM_TAG, BLANK_FIELD);
        return phone;
    }
    
    public void setMyLocation(String num)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();        
        String city = BLANK_FIELD; 
        String locationCode = BLANK_FIELD;
        if(num != null && !num.equals("")){
            city = PhoneLocation.getCityFromPhone(num);
            locationCode = PhoneLocation.getCodeFromPhone(num);
            
            if(city == null)
                city = UnknowCity;
        
            if(locationCode == null ){
                locationCode = defaultLocationCode; 
            }
            editor.putString(MY_PHONE_NUM_TAG,num);
        }else{
            editor.putString(MY_PHONE_NUM_TAG,BLANK_FIELD);
        }
        editor.putString(MY_LOCATION_TAG,city);
        editor.putString(MY_LOCATION_CODE_TAG,locationCode);
        editor.commit();
    }
    
    public String getMyLocation()
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String city = settings.getString(MY_LOCATION_TAG, BLANK_FIELD);
        /*
        //如果重来没有保存自己的所在地,那么根据设置的号码重新计算
        if(city.equals(UnknowCity)){ 
            //LocationInfo info = new LocationInfo(context,ShanghaiPhone);
            city = PhoneLocation.getCityFromPhone(ShanghaiPhone);
            
            if(city == null )
            	city = UnknowCity;
            
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(MY_LOCATION_TAG,city);
            editor.commit();
        }*/
        return city;
    }
    
    public String getMyLocationCode()
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String locationCode = settings.getString(MY_LOCATION_CODE_TAG, defaultLocationCode);
        return locationCode;
    }
    
    public void setIpDailerPrefix(String prefix)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(IP_DAILER_PREFIX_TAG,prefix);
        editor.commit();
    }

    public String getIpDailerPrefix()
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String prefix = settings.getString(IP_DAILER_PREFIX_TAG, " ");
        if(prefix.equals(" ")){
            prefix = defaultPrefix;
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(IP_DAILER_PREFIX_TAG,prefix);
            editor.commit();
        }
        return prefix;
    }
    
    public void setIpDailerOnOff(boolean enable)
    {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        int i = enable ? 1 : 0;
        editor.putLong(IP_DAILER_ENABLE_TAG,i);
        editor.commit();
    }
    
    public boolean isIpDailerOn(){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        long enable = settings.getLong(IP_DAILER_ENABLE_TAG, 0);
        if(enable == 1)
        	return true;
        
        return false;	
    }
    

    public boolean isCityDisplay(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        //SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(DISPLAY_CITY,true);
    }

    public void showSettingPrefer(Context ctx)
    {
        LayoutInflater factory = LayoutInflater.from(ctx);
        final View settingView = factory.inflate(R.layout.setting_prefer, null);
        final EditText txtMyPhoneNum = (EditText) settingView.findViewById(R.id.this_phone_num);
        final EditText txtIpDailerPrefix = (EditText)settingView.findViewById(R.id.prefix_num);
        final TextView txtMyLocation = (TextView)settingView.findViewById(R.id.this_mobile_city);

		final CheckBox chkEnableIpDailer = (CheckBox) settingView.findViewById(R.id.enable_ip_dailer);
		
        if(!getMyPhoneNum().equals(BLANK_FIELD))
            txtMyPhoneNum.setText(getMyPhoneNum());
		txtIpDailerPrefix.setText(getIpDailerPrefix());
		txtMyLocation.setText(context.getString((R.string.your_city),getMyLocation()));
		chkEnableIpDailer.setChecked(isIpDailerOn());
        
        new AlertDialog.Builder(ctx)
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setTitle(R.string.title_setting)
            .setView(settingView)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	setIpDailerPrefix(txtIpDailerPrefix.getText().toString());
                	setMyLocation(txtMyPhoneNum.getText().toString());
                	setIpDailerOnOff(chkEnableIpDailer.isChecked());
                	Toast.makeText(context, context.getString((R.string.setting_result),getMyLocation(),getIpDailerPrefix()), Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show();
    }
    
}

