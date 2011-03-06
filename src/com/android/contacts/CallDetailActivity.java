/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts;

import com.android.internal.telephony.CallerInfo;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Contacts.Intents.Insert;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


//Geesun 
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.widget.QuickContactBadge;

import com.android.contacts.location.LocationPreference;
import com.android.contacts.location.PhoneNumProcess;

/**
 * Displays the details of a specific call log entry.
 */
//Geesun
public class CallDetailActivity extends ListActivity  {
    private static final String TAG = "CallDetail";

    private TextView mCallType;
    private ImageView mCallTypeIcon;
    private TextView mCallTime;
    private TextView mCallDuration;

    private String mNumber = null;
    
    //Geesun
    private TextView mCity;
    private int mNoPhotoResource;
    Uri personUri = null ;
    private QuickContactBadge mPhotoView;
    static String mIpPrefix = null; 

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;

    static final String[] CALL_LOG_PROJECTION = new String[] {
    	//Geesun
    	CallLog.Calls._ID,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
    };
    
    //Geesun
    static final int LOG_COLUMN_INDEX = 0;
    static final int DATE_COLUMN_INDEX = 1;
    static final int DURATION_COLUMN_INDEX = 2;
    static final int NUMBER_COLUMN_INDEX = 3;
    static final int CALL_TYPE_COLUMN_INDEX = 4;

    static final String[] PHONES_PROJECTION = new String[] {
        PhoneLookup._ID,
        PhoneLookup.DISPLAY_NAME,
        PhoneLookup.TYPE,
        PhoneLookup.LABEL,
        PhoneLookup.NUMBER,
        PhoneLookup.LOOKUP_KEY,
        PhoneLookup.PHOTO_ID,
    };
    static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_NAME = 1;
    static final int COLUMN_INDEX_TYPE = 2;
    static final int COLUMN_INDEX_LABEL = 3;
    static final int COLUMN_INDEX_NUMBER = 4;
    static final int COLUMN_INDEX_LOOKUP_KEY = 5;
    static final int COLUMN_INDEX_PHOTO_ID = 6;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.call_detail);

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();
        
        //Geesun
        mCity = (TextView) findViewById(R.id.city);
        mPhotoView = (QuickContactBadge) findViewById(R.id.photo);
          
        // Set the photo with a random "no contact" image
        long now = SystemClock.elapsedRealtime();
        int num = (int) now & 0xf;
        if (num < 9) {
            // Leaning in from right, common
            mNoPhotoResource = R.drawable.ic_contact_picture;
        } else if (num < 14) {
            // Leaning in from left uncommon
            mNoPhotoResource = R.drawable.ic_contact_picture_2;
        } else {
            // Coming in from the top, rare
            mNoPhotoResource = R.drawable.ic_contact_picture_3;
        }
        
        LocationPreference  locPref = new LocationPreference(this);
        
        mIpPrefix = locPref.getIpDailerPrefix();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Geesun
        updateData(/*getIntent().getData()*/);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                // Make sure phone isn't already busy before starting direct call
                TelephonyManager tm = (TelephonyManager)
                        getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                    //Geesun
                    String phoneNum = mNumber;
                    PhoneNumProcess process = new PhoneNumProcess(/*getApplicationContext()*/this,phoneNum);
                    process.displayPhoneLocation();
                    phoneNum =process.getPhoneWithIp();
                    
                    Intent callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts("tel", phoneNum, null));
                    startActivity(callIntent);
                    StickyTabs.saveTab(this, getIntent());
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    //Geesun Add 
    class viewEntryData{
    	public long id;
    	public String number;
    	public long date;
    	public long duration;
    	public int callType;    	
    }
    
    /**
     * Update user interface with details of given call.
     *
     * @param callUri Uri into {@link CallLog.Calls}
     */
    //Geesun
    private void updateData() {
    	Bundle bundle = getIntent().getExtras();
    	String number = bundle.getString("NUMBER");
    	//Toast.makeText(this, number, Toast.LENGTH_LONG).show();
    	
        StringBuilder where = new StringBuilder();
        where.append(Calls.NUMBER);
        where.append(" = '" + number + "'");
        
        where.append(" or " + Calls.NUMBER);
        where.append(" = '" + mIpPrefix + number + "'");
        
        where.append(" or " + Calls.NUMBER);
        where.append(" = '" + "+86" + number + "'");
        
        Cursor callCursor = getContentResolver().query(Calls.CONTENT_URI, CALL_LOG_PROJECTION,where.toString(), null,
        		Calls.DEFAULT_SORT_ORDER);
        
        mNumber = number;
        
    	ContentResolver resolver = getContentResolver();
      
    	TextView tvName = (TextView) findViewById(R.id.name);
    	TextView tvNumber = (TextView) findViewById(R.id.number);
    	
    	if (mNumber.equals(CallerInfo.UNKNOWN_NUMBER) ||
                mNumber.equals(CallerInfo.PRIVATE_NUMBER)) {
            // List is empty, let the empty view show instead.
            TextView emptyText = (TextView) findViewById(R.id.emptyText);
            if (emptyText != null) {
                emptyText.setText(mNumber.equals(CallerInfo.PRIVATE_NUMBER) 
                        ? R.string.private_num : R.string.unknown);
            }
        } else {
            // Perform a reverse-phonebook lookup to find the PERSON_ID
            String callLabel = null;
            
            Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mNumber));
            Cursor phonesCursor = resolver.query(phoneUri, PHONES_PROJECTION, null, null, null);
            try {
                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    long personId = phonesCursor.getLong(COLUMN_INDEX_ID);  
                    final String lookupKey = phonesCursor.getString(COLUMN_INDEX_LOOKUP_KEY);
                    long photoId = phonesCursor.getLong(COLUMN_INDEX_PHOTO_ID);
                    
                    Log.d(TAG,"Id:" + personId + " Key:" + lookupKey);
                    
                    mPhotoView.assignContactUri(Contacts.getLookupUri(personId, lookupKey));
                    mPhotoView.setVisibility(View.VISIBLE);
                    if (photoId == 0) {
                    	mPhotoView.setImageResource(mNoPhotoResource);
                    } else {
                        Bitmap photo = null;
                        try {
                            photo = ContactsUtils.loadContactPhoto(this, photoId, null);
                        } catch (OutOfMemoryError e) {
                            // Not enough memory for the photo, do nothing.
                        }
                        
                        if(photo !=null){
                        	mPhotoView.setImageBitmap(photo);
                        }else
                        	mPhotoView.setImageResource(mNoPhotoResource);
                    }
                    
                    personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, personId);
                    
                    
                    
                    tvName.setText(phonesCursor.getString(COLUMN_INDEX_NAME));
                    tvNumber.setText(mNumber);
                } else {
                	tvName.setText("("+ getString(R.string.unknown) + ")");
                	tvNumber.setText(mNumber);// = PhoneNumberUtils.formatNumber(mNumber);
                	//tvNumber.setVisibility(View.GONE);
                	mPhotoView.setImageResource(mNoPhotoResource);
                }
            } finally {
                if (phonesCursor != null) phonesCursor.close();
            }
        }
        
        LocationPreference  locPref = new LocationPreference(this);
        if(locPref.isCityDisplay()){
            PhoneNumProcess  process = new PhoneNumProcess(this,mNumber);
            mCity.setText(process.getPhoneLocation());
        }
        
    	
    	List<viewEntryData> logs = new ArrayList<viewEntryData>();
    	viewEntryData firstPlaceHolder = new viewEntryData();
    	firstPlaceHolder.number = mNumber;
    	logs.add(firstPlaceHolder);
        try {
            if (callCursor != null && callCursor.moveToFirst()) {
            	
            	do {
            		viewEntryData data = new viewEntryData();
	                // Read call log specifics
            		data.id = callCursor.getLong(LOG_COLUMN_INDEX);
            		data.date = callCursor.getLong(DATE_COLUMN_INDEX);
            		data.duration = callCursor.getLong(DURATION_COLUMN_INDEX);
            		data.callType = callCursor.getInt(CALL_TYPE_COLUMN_INDEX);
	                
            		logs.add(data);	              
            	}while(callCursor.moveToNext());
            } else {
                // Something went wrong reading in our primary data, so we're going to
                // bail out and show error to users.
                Toast.makeText(this, R.string.toast_call_detail_error,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
        
        ViewAdapter adapter = new ViewAdapter(this, logs);
        setListAdapter(adapter); 
    }
    
    /*
    private String formatDuration(long elapsedSeconds) {
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;

        return getString(R.string.callDetailsDurationFormat, minutes, seconds);
    }*/

    static final class ViewEntry {
        public int icon = -1;
        public String text = null;
        public Intent intent = null;
        public String label = null;
        public String number = null;

        public ViewEntry(int icon, String text, Intent intent) {
            this.icon = icon;
            this.text = text;
            this.intent = intent;
        }
    }
    
    //Geesun
    static final class ViewAdapter extends BaseAdapter implements View.OnClickListener,
	View.OnLongClickListener {
    	//Geesun
        //private final List<ViewEntry> mActions;
    	private final List<viewEntryData> mLogs;
    	private Context mContext;

        private final LayoutInflater mInflater;
        
        //Geesun
        public ViewAdapter(Context context, List<viewEntryData> logs) {
        	mLogs = logs;
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        
        //Geesun Add 
        private String formatDuration(long elapsedSeconds) {
            long minutes = 0;
            long seconds = 0;

            if (elapsedSeconds >= 60) {
                minutes = elapsedSeconds / 60;
                elapsedSeconds -= minutes * 60;
            }
            seconds = elapsedSeconds;

            return mContext.getString(R.string.callDetailsDurationFormat, minutes, seconds);
        }
        
        public int getCount() {
            return mLogs.size();
        }

        public Object getItem(int position) {
            return mLogs.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Make sure we have a valid convertView to start with
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.call_detail_list_item, parent, false);
            }

            // Fill action with icon and text.
            viewEntryData entry = mLogs.get(position);
       	 long date = entry.date;
       	 long duration = entry.duration;
       	 int callType = entry.callType; 

           
       	RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.line_action);
           LinearLayout layout_logs = (LinearLayout) convertView.findViewById(R.id.line_log);
           
           if(position == 0){
           	layout_logs.setVisibility(View.GONE);
           	layout.setVisibility(View.VISIBLE);
               ImageView call_icon = (ImageView) convertView.findViewById(R.id.call_icon);
               ImageView sms_icon = (ImageView) convertView.findViewById(R.id.sms_icon);
              
               //TextView tvSms = (TextView) convertView.findViewById(R.id.send_sms);
               TextView tvCall = (TextView) convertView.findViewById(R.id.call);
              
               //Geesun
               PhoneNumProcess  process = new PhoneNumProcess(mContext,entry.number);
               process.displayPhoneLocation();
               String phoneNum =process.getPhoneWithIp();
               
               Intent callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                       Uri.fromParts("tel", phoneNum, null));
               
               Intent smsIntent = new Intent(Intent.ACTION_SENDTO,
                       Uri.fromParts("sms", entry.number, null));
               
               sms_icon.setImageResource(R.drawable.sym_action_sms);
               call_icon.setImageResource(android.R.drawable.sym_action_call);
              
               viewEntryData firstData = mLogs.get(1);
          	 
	           	switch(firstData.callType){
		           	case Calls.INCOMING_TYPE:
		           		tvCall.setText(mContext.getString(R.string.returnCall));
		           		break;
		           	case Calls.OUTGOING_TYPE:
		           		tvCall.setText(mContext.getString(R.string.callAgain));
		           		break;
		           	case Calls.MISSED_TYPE:
		           		tvCall.setText(mContext.getString(R.string.callBack));
		           		break;
	           	 }
               
               
               call_icon.setTag(callIntent);
               tvCall.setTag(callIntent);
               sms_icon.setTag(smsIntent);
               //tvSms.setTag(smsIntent);                
               call_icon.setOnClickListener(this);                
               sms_icon.setOnClickListener(this);
               //tvSms.setOnClickListener(this);
               tvCall.setOnClickListener(this); 
               
               convertView.setTag(null);
               
           }else{                
           	layout.setVisibility(View.GONE);
           	layout_logs.setVisibility(View.VISIBLE);
               ImageView mCallTypeIcon = (ImageView) convertView.findViewById(R.id.icon);
               TextView tvTime = (TextView) convertView.findViewById(R.id.time);
               TextView tvDuration = (TextView) convertView.findViewById(R.id.duration);
               TextView tvType = (TextView) convertView.findViewById(R.id.type);
               // Pull out string in format [relative], [date]
               CharSequence dateClause = DateUtils.formatDateRange(mContext, date, date,
                       DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                       DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
               tvTime.setText(dateClause);
               
               // Set the duration
               if (callType == Calls.MISSED_TYPE) {
               	tvDuration.setVisibility(View.GONE);
               } else {
               	tvDuration.setVisibility(View.VISIBLE);
               	tvDuration.setText(formatDuration(duration));
               }
   
               // Set the call type icon and caption                
               switch (callType) {
                   case Calls.INCOMING_TYPE:
                       mCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_incoming_call);
                       tvType.setText(R.string.type_incoming);
                      
                       break;
   
                   case Calls.OUTGOING_TYPE:
                       mCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_outgoing_call);
                       tvType.setText(R.string.type_outgoing);
                       
                       break;
   
                   case Calls.MISSED_TYPE:
                       mCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_missed_call);
                       tvType.setText(R.string.type_missed);
                       
                       break;
               }                
               convertView.setTag(entry);
               convertView.setLongClickable(true);
               convertView.setOnLongClickListener(this);
           }

            return convertView;
        }
        
        //Geesun Add 
		public void onClick(View v) {			
            Intent intent = (Intent) v.getTag();

           if(intent != null)
            mContext.startActivity(intent);
		}

	    private class DeleteClickListener implements DialogInterface.OnClickListener {
	        private viewEntryData  mData;
	        
	        public DeleteClickListener(viewEntryData data) {
	        	mData = data;
	        }

	        public void onClick(DialogInterface dialog, int which) {
                StringBuilder where = new StringBuilder();
                where.append(Calls._ID);
                where.append(" =  " + mData.id);
                mContext.getContentResolver().delete(Calls.CONTENT_URI, where.toString(), null);
                mLogs.remove(mData);
                if(mLogs.size() != 1)
                	notifyDataSetChanged();
                else{
                	
                	((Activity) mContext).finish();
                }
	        }
	    }
	    
		public boolean onLongClick(View v) {
			v.setBackgroundResource(android.R.drawable.list_selector_background);
			
			viewEntryData entryData  =  (viewEntryData) v.getTag();
			if(entryData != null){
				
				
            	Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
				vibrator.vibrate(20);

                Uri uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI,
                		entryData.id);
                //TODO make this dialog persist across screen rotations
                new AlertDialog.Builder(mContext)
                    .setTitle(R.string.deleteConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.deleteCallLogConfirmation)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DeleteClickListener(entryData))
                    .show();
			}
			// TODO Auto-generated method stub
			return false;
		}
        
    }
    /*
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        // Handle passing action off to correct handler.
        if (view.getTag() instanceof ViewEntry) {
            ViewEntry entry = (ViewEntry) view.getTag();
            if (entry.intent != null) {
                if (Intent.ACTION_CALL_PRIVILEGED.equals(entry.intent.getAction())) {
                    StickyTabs.saveTab(this, getIntent());
                }
                startActivity(entry.intent);
            }
        }
    }*/
    
    public boolean onCreateOptionsMenu(Menu menu) {
        if (personUri != null) {
            menu.add(0, 0, 0, R.string.menu_viewContact)
            	.setIcon(R.drawable.sym_action_view_contact); 
        } else {
            menu.add(0, 0, 0, R.string.recentCalls_addToContact)
            .setIcon(R.drawable.sym_action_add);
        }

        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (personUri != null) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, personUri);
            startActivity(viewIntent);
        } else {
            Intent createIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            createIntent.setType(Contacts.CONTENT_ITEM_TYPE);
            createIntent.putExtra(Insert.PHONE, mNumber);
            startActivity(createIntent);
        }
        
                return true;

    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData,
            boolean globalSearch) {
        if (globalSearch) {
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        } else {
            ContactsSearchManager.startSearch(this, initialQuery);
        }
    }
}
