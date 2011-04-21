package com.android.contacts.location;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.phone.location.PhoneLocation;

import com.android.contacts.R;

public class IpDailerPreference extends DialogPreference  {
	public static final String LOCAL_NUM = "_local_num"; 
	public static final String IP_PREFIX = "_ip_prefix"; 
	public static final String LOCAL_CITY = "_city"; 
	public static final String LOCAL_CODE = "_local_code"; 
	 
	Context mContext;
	EditText thisPhoneNum ;//= (EditText) view.findViewById(R.id.this_phone_num);
    EditText ipPrefix ;//= (EditText) view.findViewById(R.id.prefix_num);
    TextView cityView ;//= (TextView) view.findViewById(R.id.this_mobile_city);

	public IpDailerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.ipdailer_settings);
		mContext = context;
	}
	protected void onBindDialogView(View view) {

	    thisPhoneNum = (EditText) view.findViewById(R.id.this_phone_num);
	    ipPrefix = (EditText) view.findViewById(R.id.prefix_num);
	    cityView = (TextView) view.findViewById(R.id.this_mobile_city);
	    
	    SharedPreferences pref = getSharedPreferences();
	    thisPhoneNum.setText(pref.getString(getKey() + LOCAL_NUM,""));
	    ipPrefix.setText(pref.getString(getKey() + IP_PREFIX,""));
	    cityView.setText(pref.getString(getKey() + LOCAL_CITY,mContext.getString(R.string.unknow_city)));

	    super.onBindDialogView(view);
	}
	
	protected void onDialogClosed(boolean positiveResult) {

	    if(!positiveResult)
	        return;
	    
	    SharedPreferences.Editor editor = getEditor();
	    String num = thisPhoneNum.getText().toString();
	    editor.putString(getKey() + LOCAL_NUM, thisPhoneNum.getText().toString());
	    editor.putString(getKey() + IP_PREFIX, ipPrefix.getText().toString());
	    
	    if(num != null && !num.equals("")){
	    	 String city = PhoneLocation.getCityFromPhone(num);
	         String locationCode = PhoneLocation.getCodeFromPhone(num);
	         editor.putString(getKey() + LOCAL_CITY,city);
	 	     editor.putString(getKey() + LOCAL_CODE,locationCode);
	 	     if(city == null || 
	 	    		 city.equals("")){
	 	    	Toast.makeText(mContext, mContext.getString(R.string.no_city_and_enable_all),Toast.LENGTH_LONG).show();
	 	     }else{
	 	    	 Toast.makeText(mContext, 
	 	    			 mContext.getString((R.string.setting_result),
	 	    				city,ipPrefix.getText().toString()), Toast.LENGTH_LONG).show();
	 	     }
	    }else{	    
	    	Toast.makeText(mContext, mContext.getString(R.string.no_city_and_enable_all),Toast.LENGTH_LONG).show();
	    }
	    editor.commit();
	    
	}

}
