/*
 * Copyright (C) 2007 The Android Open Source Project
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
import com.android.internal.telephony.ITelephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

//Geesun 
import com.android.contacts.location.PhoneNumProcess;
import com.android.contacts.location.PhoneNumUtils;
import com.android.contacts.location.LocationPreference;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
/**
 * Displays a list of call log entries.
 */
public class RecentCallsListActivity extends ListActivity
        implements View.OnCreateContextMenuListener {
    private static final String TAG = "RecentCallsList";

    /** The projection to use when querying the call log table */
    static final String[] CALL_LOG_PROJECTION = new String[] {
            Calls._ID,
            Calls.NUMBER,
            Calls.DATE,
            Calls.DURATION,
            Calls.TYPE,
            Calls.CACHED_NAME,
            Calls.CACHED_NUMBER_TYPE,
            Calls.CACHED_NUMBER_LABEL
    };

    static final int ID_COLUMN_INDEX = 0;
    static final int NUMBER_COLUMN_INDEX = 1;
    static final int DATE_COLUMN_INDEX = 2;
    static final int DURATION_COLUMN_INDEX = 3;
    static final int CALL_TYPE_COLUMN_INDEX = 4;
    static final int CALLER_NAME_COLUMN_INDEX = 5;
    static final int CALLER_NUMBERTYPE_COLUMN_INDEX = 6;
    static final int CALLER_NUMBERLABEL_COLUMN_INDEX = 7;

    /** The projection to use when querying the phones table */
    static final String[] PHONES_PROJECTION = new String[] {
            PhoneLookup._ID,
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.TYPE,
            PhoneLookup.LABEL,
            PhoneLookup.NUMBER
    };

    static final int PERSON_ID_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;
    static final int PHONE_TYPE_COLUMN_INDEX = 2;
    static final int LABEL_COLUMN_INDEX = 3;
    static final int MATCHED_NUMBER_COLUMN_INDEX = 4;

    private static final int MENU_ITEM_DELETE_ALL = 1;
    private static final int CONTEXT_MENU_ITEM_DELETE = 1;
    private static final int CONTEXT_MENU_CALL_CONTACT = 2;

    private static final int QUERY_TOKEN = 53;
    private static final int UPDATE_TOKEN = 54;

    private static final int DIALOG_CONFIRM_DELETE_ALL = 1;

    RecentCallsAdapter mAdapter;
    private QueryHandler mQueryHandler;
    String mVoiceMailNumber;

    private boolean mScrollToTop;

    //Geesun 
    final class RecentCallsInfo{
    	public String number;
    	public int type;
    	public String name;
    	public int number_type;
    	public String number_label;
    	public long date;
    	public int duration;
    	public int count ;
    	RecentCallsInfo(){
    		count = 1;
    	}
    }
    //Geesun 
    ArrayList<RecentCallsInfo> mListCallLogs = null;    
    static String mIpPrefix = null; 

    static final class ContactInfo {
        public long personId;
        public String name;
        public int type;
        public String label;
        public String number;
        public String formattedNumber;

        public static ContactInfo EMPTY = new ContactInfo();
    }

    public static final class RecentCallsListItemViews {
        TextView line1View;
	     //Geesun
        TextView cityView;

        TextView labelView;
        TextView numberView;
        TextView dateView;
        ImageView iconView;
        View callView;
        ImageView groupIndicator;
        TextView groupSize;
    }

    static final class CallerInfoQuery {
        String number;
        int position;
        String name;
        int numberType;
        String numberLabel;
    }

    /**
     * Shared builder used by {@link #formatPhoneNumber(String)} to minimize
     * allocations when formatting phone numbers.
     */
    private static final SpannableStringBuilder sEditable = new SpannableStringBuilder();

    /**
     * Invalid formatting type constant for {@link #sFormattingType}.
     */
    private static final int FORMATTING_TYPE_INVALID = -1;

    /**
     * Cached formatting type for current {@link Locale}, as provided by
     * {@link PhoneNumberUtils#getFormatTypeForLocale(Locale)}.
     */
    private static int sFormattingType = FORMATTING_TYPE_INVALID;

    /** Adapter class to fill in data for the Call Log */
    //Geesun
    final class RecentCallsAdapter extends /*ResourceCursorAdapter*/ ArrayAdapter<RecentCallsInfo>
            implements Runnable, ViewTreeObserver.OnPreDrawListener, View.OnClickListener {
        HashMap<String,ContactInfo> mContactInfo;
        private final LinkedList<CallerInfoQuery> mRequests;
        private volatile boolean mDone;
        private boolean mLoading = true;
        ViewTreeObserver.OnPreDrawListener mPreDrawListener;
        private static final int REDRAW = 1;
        private static final int START_THREAD = 2;
        private boolean mFirst;
        private Thread mCallerIdThread;

        private CharSequence[] mLabelArray;

        private Drawable mDrawableIncoming;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableMissed;

        //Geesun        
        private int mLayout;
        private LayoutInflater mInflater;
        private Context mContext;
        List<RecentCallsInfo> mCallLogs;

        /**
         * Reusable char array buffers.
         */
        private CharArrayBuffer mBuffer1 = new CharArrayBuffer(128);
        private CharArrayBuffer mBuffer2 = new CharArrayBuffer(128);

        public void onClick(View view) {
            String number = (String) view.getTag();
            if (!TextUtils.isEmpty(number)) {
                //Geesun
                PhoneNumProcess  process = new PhoneNumProcess(mContext,number);
                process.displayPhoneLocation();
                String phoneNum =process.getPhoneWithIp();
                Uri telUri = Uri.fromParts("tel", phoneNum, null);
                StickyTabs.saveTab(RecentCallsListActivity.this, getIntent());
                startActivity(new Intent(Intent.ACTION_CALL_PRIVILEGED, telUri));
            }
        }

        public boolean onPreDraw() {
            if (mFirst) {
                mHandler.sendEmptyMessageDelayed(START_THREAD, 1000);
                mFirst = false;
            }
            return true;
        }

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REDRAW:
                        notifyDataSetChanged();
                        break;
                    case START_THREAD:
                        startRequestProcessing();
                        break;
                }
            }
        };

        //Geesun
        public RecentCallsAdapter(Context context, int textViewResourceId,
				List<RecentCallsInfo> objects) {
            //Geesun
        	super(context, R.layout.recent_calls_list_item, objects);
        	//super(RecentCallsListActivity.this, R.layout.recent_calls_list_item, null);
			mContext = context;
			mCallLogs = objects;
			mLayout = R.layout.recent_calls_list_item;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mContactInfo = new HashMap<String,ContactInfo>();
            mRequests = new LinkedList<CallerInfoQuery>();
            mPreDrawListener = null;

            mDrawableIncoming = getResources().getDrawable(
                    R.drawable.ic_call_log_list_incoming_call);
            mDrawableOutgoing = getResources().getDrawable(
                    R.drawable.ic_call_log_list_outgoing_call);
            mDrawableMissed = getResources().getDrawable(
                    R.drawable.ic_call_log_list_missed_call);
            mLabelArray = getResources().getTextArray(com.android.internal.R.array.phoneTypes);
        }

        //Geesun Add 
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	
	    	//Toast.makeText(RecentCallsListActivity.this, "Get View", Toast.LENGTH_LONG).show();
	        View v;
	        if (convertView == null) {
	            v = newView(mContext, position, parent);
	        } else {
	            v = convertView;
	        }
	        bindView(v, mContext, position);
	        return v;
	    }
	    
        /**
         * Requery on background thread when {@link Cursor} changes.
         */
        protected void onContentChanged() {
            // Start async requery
            startQuery();
        }

        void setLoading(boolean loading) {
            mLoading = loading;
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }

        public ContactInfo getContactInfo(String number) {
            return mContactInfo.get(number);
        }

        public void startRequestProcessing() {
            mDone = false;
            mCallerIdThread = new Thread(this);
            mCallerIdThread.setPriority(Thread.MIN_PRIORITY);
            mCallerIdThread.start();
        }

        public void stopRequestProcessing() {
            mDone = true;
            if (mCallerIdThread != null) mCallerIdThread.interrupt();
        }

        public void clearCache() {
            synchronized (mContactInfo) {
                mContactInfo.clear();
            }
        }

        private void updateCallLog(CallerInfoQuery ciq, ContactInfo ci) {
            // Check if they are different. If not, don't update.
            if (TextUtils.equals(ciq.name, ci.name)
                    && TextUtils.equals(ciq.numberLabel, ci.label)
                    && ciq.numberType == ci.type) {
                return;
            }
            ContentValues values = new ContentValues(3);
            values.put(Calls.CACHED_NAME, ci.name);
            values.put(Calls.CACHED_NUMBER_TYPE, ci.type);
            values.put(Calls.CACHED_NUMBER_LABEL, ci.label);

            try {
                RecentCallsListActivity.this.getContentResolver().update(Calls.CONTENT_URI, values,
                        Calls.NUMBER + "='" + ciq.number + "'", null);
            } catch (SQLiteDiskIOException e) {
                Log.w(TAG, "Exception while updating call info", e);
            } catch (SQLiteFullException e) {
                Log.w(TAG, "Exception while updating call info", e);
            } catch (SQLiteDatabaseCorruptException e) {
                Log.w(TAG, "Exception while updating call info", e);
            }
        }

        private void enqueueRequest(String number, int position,
                String name, int numberType, String numberLabel) {
            CallerInfoQuery ciq = new CallerInfoQuery();
            ciq.number = number;
            ciq.position = position;
            ciq.name = name;
            ciq.numberType = numberType;
            ciq.numberLabel = numberLabel;
            synchronized (mRequests) {
                mRequests.add(ciq);
                mRequests.notifyAll();
            }
        }

        private boolean queryContactInfo(CallerInfoQuery ciq) {
            // First check if there was a prior request for the same number
            // that was already satisfied
            ContactInfo info = mContactInfo.get(ciq.number);
            boolean needNotify = false;
            if (info != null && info != ContactInfo.EMPTY) {
                return true;
            } else {
                // Ok, do a fresh Contacts lookup for ciq.number.
                boolean infoUpdated = false;

                if (PhoneNumberUtils.isUriNumber(ciq.number)) {
                    // This "number" is really a SIP address.

                    // TODO: This code is duplicated from the
                    // CallerInfoAsyncQuery class.  To avoid that, could the
                    // code here just use CallerInfoAsyncQuery, rather than
                    // manually running ContentResolver.query() itself?

                    // We look up SIP addresses directly in the Data table:
                    Uri contactRef = Data.CONTENT_URI;

                    // Note Data.DATA1 and SipAddress.SIP_ADDRESS are equivalent.
                    //
                    // Also note we use "upper(data1)" in the WHERE clause, and
                    // uppercase the incoming SIP address, in order to do a
                    // case-insensitive match.
                    //
                    // TODO: May also need to normalize by adding "sip:" as a
                    // prefix, if we start storing SIP addresses that way in the
                    // database.
                    String selection = "upper(" + Data.DATA1 + ")=?"
                            + " AND "
                            + Data.MIMETYPE + "='" + SipAddress.CONTENT_ITEM_TYPE + "'";
                    String[] selectionArgs = new String[] { ciq.number.toUpperCase() };

                    Cursor dataTableCursor =
                            RecentCallsListActivity.this.getContentResolver().query(
                                    contactRef,
                                    null,  // projection
                                    selection,  // selection
                                    selectionArgs,  // selectionArgs
                                    null);  // sortOrder

                    if (dataTableCursor != null) {
                        if (dataTableCursor.moveToFirst()) {
                            info = new ContactInfo();

                            // TODO: we could slightly speed this up using an
                            // explicit projection (and thus not have to do
                            // those getColumnIndex() calls) but the benefit is
                            // very minimal.

                            // Note the Data.CONTACT_ID column here is
                            // equivalent to the PERSON_ID_COLUMN_INDEX column
                            // we use with "phonesCursor" below.
                            info.personId = dataTableCursor.getLong(
                                    dataTableCursor.getColumnIndex(Data.CONTACT_ID));
                            info.name = dataTableCursor.getString(
                                    dataTableCursor.getColumnIndex(Data.DISPLAY_NAME));
                            // "type" and "label" are currently unused for SIP addresses
                            info.type = SipAddress.TYPE_OTHER;
                            info.label = null;

                            // And "number" is the SIP address.
                            // Note Data.DATA1 and SipAddress.SIP_ADDRESS are equivalent.
                            info.number = dataTableCursor.getString(
                                    dataTableCursor.getColumnIndex(Data.DATA1));

                            infoUpdated = true;
                        }
                        dataTableCursor.close();
                    }
                } else {
                    // "number" is a regular phone number, so use the
                    // PhoneLookup table:
                    Cursor phonesCursor =
                            RecentCallsListActivity.this.getContentResolver().query(
                                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                                     Uri.encode(ciq.number)),
                                PHONES_PROJECTION, null, null, null);
                    if (phonesCursor != null) {
                        if (phonesCursor.moveToFirst()) {
                            info = new ContactInfo();
                            info.personId = phonesCursor.getLong(PERSON_ID_COLUMN_INDEX);
                            info.name = phonesCursor.getString(NAME_COLUMN_INDEX);
                            info.type = phonesCursor.getInt(PHONE_TYPE_COLUMN_INDEX);
                            info.label = phonesCursor.getString(LABEL_COLUMN_INDEX);
                            info.number = phonesCursor.getString(MATCHED_NUMBER_COLUMN_INDEX);

                            infoUpdated = true;
                        }
                        phonesCursor.close();
                    }
                }

                if (infoUpdated) {
                    // New incoming phone number invalidates our formatted
                    // cache. Any cache fills happen only on the GUI thread.
                    info.formattedNumber = null;

                    mContactInfo.put(ciq.number, info);

                    // Inform list to update this item, if in view
                    needNotify = true;
                }
            }
            if (info != null) {
                updateCallLog(ciq, info);
            }
            return needNotify;
        }

        /*
         * Handles requests for contact name and number type
         * @see java.lang.Runnable#run()
         */
        public void run() {
            boolean needNotify = false;
            while (!mDone) {
                CallerInfoQuery ciq = null;
                synchronized (mRequests) {
                    if (!mRequests.isEmpty()) {
                        ciq = mRequests.removeFirst();
                    } else {
                        if (needNotify) {
                            needNotify = false;
                            mHandler.sendEmptyMessage(REDRAW);
                        }
                        try {
                            mRequests.wait(1000);
                        } catch (InterruptedException ie) {
                            // Ignore and continue processing requests
                        }
                    }
                }
                if (ciq != null && queryContactInfo(ciq)) {
                    needNotify = true;
                }
            }
        }


        //Geesun
        public View newView(Context context, int position, ViewGroup parent) {
            //Geesun 
        	View view = mInflater.inflate(mLayout, parent, false);  
        	//View view = super.newView(context, cursor, parent);

            // Get the views to bind to
            RecentCallsListItemViews views = new RecentCallsListItemViews();
            views.line1View = (TextView) view.findViewById(R.id.line1);
            views.cityView = (TextView) view.findViewById(R.id.city);
            views.labelView = (TextView) view.findViewById(R.id.label);
            views.numberView = (TextView) view.findViewById(R.id.number);
            views.dateView = (TextView) view.findViewById(R.id.date);
            views.iconView = (ImageView) view.findViewById(R.id.call_type_icon);
            views.callView = view.findViewById(R.id.call_icon);
            views.callView.setOnClickListener(this);

            view.setTag(views);

            return view;
        }



        //Geesun
        public void bindView(View view, Context context, int c) {
        	//Geesun
            //final RecentCallsListItemViews views = (RecentCallsListItemViews) view.getTag();
        	final RecentCallsListItemViews views = (RecentCallsListItemViews) view.getTag();
            
            RecentCallsInfo  callsinfo = getItem(c);

            String number = callsinfo.number;//c.getString(NUMBER_COLUMN_INDEX);
            String formattedNumber = null;
            String callerName = callsinfo.name;//c.getString(CALLER_NAME_COLUMN_INDEX);
            int callerNumberType = callsinfo.number_type;//c.getInt(CALLER_NUMBERTYPE_COLUMN_INDEX);
            String callerNumberLabel = callsinfo.number_label;//c.getString(CALLER_NUMBERLABEL_COLUMN_INDEX);
            int count = callsinfo.count;

            LocationPreference  locPref = new LocationPreference(context);
            String city = null;
            if(locPref.isCityDisplay()){
                PhoneNumProcess  process = new PhoneNumProcess(context,number);
                city = process.getPhoneLocation();
            }
            
            // Store away the number so we can call it directly if you click on the call icon
            views.callView.setTag(number);

            // Lookup contacts with this number
            ContactInfo info = mContactInfo.get(number);
            if (info == null) {
                // Mark it as empty and queue up a request to find the name
                // The db request should happen on a non-UI thread
                info = ContactInfo.EMPTY;
                mContactInfo.put(number, info);
                //Geesun
                enqueueRequest(number, c,
                        callerName, callerNumberType, callerNumberLabel);
            } else if (info != ContactInfo.EMPTY) { // Has been queried
                // Check if any data is different from the data cached in the
                // calls db. If so, queue the request so that we can update
                // the calls db.
                if (!TextUtils.equals(info.name, callerName)
                        || info.type != callerNumberType
                        || !TextUtils.equals(info.label, callerNumberLabel)) {
                    // Something is amiss, so sync up.
                	//Geesun
                    enqueueRequest(number, c,
                            callerName, callerNumberType, callerNumberLabel);
                }

                // Format and cache phone number for found contact
                if (info.formattedNumber == null) {
                    info.formattedNumber = formatPhoneNumber(info.number);
                }
                formattedNumber = info.formattedNumber;
            }

            String name = info.name;
            int ntype = info.type;
            String label = info.label;
            // If there's no name cached in our hashmap, but there's one in the
            // calls db, use the one in the calls db. Otherwise the name in our
            // hashmap is more recent, so it has precedence.
            if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(callerName)) {
                name = callerName;
                ntype = callerNumberType;
                label = callerNumberLabel;

                // Format the cached call_log phone number
                formattedNumber = formatPhoneNumber(number);
            }
            // Set the text lines and call icon.
            // Assumes the call back feature is on most of the
            // time. For private and unknown numbers: hide it.
            views.callView.setVisibility(View.VISIBLE);           

            if (!TextUtils.isEmpty(name)) {
            	//Geesun
            	if(count != 1)
            		views.line1View.setText(name + " ("+ count +")");
            	else
            		views.line1View.setText(name );
            	
                views.labelView.setVisibility(View.VISIBLE);
                CharSequence numberLabel = Phone.getDisplayLabel(context, ntype, label,
                        mLabelArray);
                views.numberView.setVisibility(View.VISIBLE);
                views.numberView.setText(formattedNumber);
                if (!TextUtils.isEmpty(numberLabel)) {
                    views.labelView.setText(numberLabel);
                    views.labelView.setVisibility(View.VISIBLE);
                } else {
                    views.labelView.setVisibility(View.GONE);
                }
            } else {
                if (number.equals(CallerInfo.UNKNOWN_NUMBER)) {
                    number = getString(R.string.unknown);
                    views.callView.setVisibility(View.INVISIBLE);
                } else if (number.equals(CallerInfo.PRIVATE_NUMBER)) {
                    number = getString(R.string.private_num);
                    views.callView.setVisibility(View.INVISIBLE);
                } else if (number.equals(CallerInfo.PAYPHONE_NUMBER)) {
                    number = getString(R.string.payphone);
                } else if (number.equals(mVoiceMailNumber)) {
                    number = getString(R.string.voicemail);
                } else {
                    // Just a raw number, and no cache, so format it nicely
                    number = formatPhoneNumber(number);
                }

                //Geesun
                if(count != 1)
                	views.line1View.setText(number+ " ("+ count +")");
                else
                	views.line1View.setText(number);
                views.numberView.setVisibility(View.GONE);
                views.labelView.setVisibility(View.GONE);
            }
            
            views.cityView.setText(city);
            //Geesun
            int type = callsinfo.type;//c.getInt(CALL_TYPE_COLUMN_INDEX);
            long date = callsinfo.date;//c.getLong(DATE_COLUMN_INDEX);

            // Set the date/time field by mixing relative and absolute times.
            int flags = DateUtils.FORMAT_ABBREV_RELATIVE;

            views.dateView.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags));

            // Set the icon
            switch (type) {
                case Calls.INCOMING_TYPE:
                    views.iconView.setImageDrawable(mDrawableIncoming);
                    break;

                case Calls.OUTGOING_TYPE:
                    views.iconView.setImageDrawable(mDrawableOutgoing);
                    break;

                case Calls.MISSED_TYPE:
                    views.iconView.setImageDrawable(mDrawableMissed);
                    break;
            }

            // Listen for the first draw
            if (mPreDrawListener == null) {
                mFirst = true;
                mPreDrawListener = this;
                view.getViewTreeObserver().addOnPreDrawListener(this);
            }
        }
    }

    private static final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<RecentCallsListActivity> mActivity;

        /**
         * Simple handler that wraps background calls to catch
         * {@link SQLiteException}, such as when the disk is full.
         */
        protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
            public CatchingWorkerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    // Perform same query while catching any exceptions
                    super.handleMessage(msg);
                } catch (SQLiteDiskIOException e) {
                    Log.w(TAG, "Exception on background worker thread", e);
                } catch (SQLiteFullException e) {
                    Log.w(TAG, "Exception on background worker thread", e);
                } catch (SQLiteDatabaseCorruptException e) {
                    Log.w(TAG, "Exception on background worker thread", e);
                }
            }
        }

        @Override
        protected Handler createHandler(Looper looper) {
            // Provide our special handler that catches exceptions
            return new CatchingWorkerHandler(looper);
        }

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<RecentCallsListActivity>(
                    (RecentCallsListActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final RecentCallsListActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                final RecentCallsListActivity.RecentCallsAdapter callsAdapter = activity.mAdapter;
                callsAdapter.setLoading(false);
                //Geesun
                activity.getUpdateCallLogsItem(cursor);
                //callsAdapter.changeCursor(cursor);
            } else {
                cursor.close();
            }
        }
    }

    //Geesun add 
    public void addItemIntoList(RecentCallsInfo item){
    	RecentCallsInfo obj;
    	
		if(item.number.startsWith(mIpPrefix))
			item.number = item.number.substring(mIpPrefix.length());
		
		String chinaNum = "+86";		
		if(item.number.startsWith(chinaNum))
			item.number = item.number.substring(chinaNum.length());
		
    	for(int i = 0; i < mListCallLogs.size(); i++){
    		
    		if(mListCallLogs.get(i).number.equals(item.number)){
    			mListCallLogs.get(i).count ++;
    			return;
    		}
    	}
    	mListCallLogs.add(item);
    }
    
    public void getUpdateCallLogsItem(Cursor cursor)
    {
    	mListCallLogs.clear();
    	if(cursor == null){
    		return;
    	}
    	
    	if(cursor.getCount() != 0){
	       	cursor.moveToFirst();
			do { 
				RecentCallsInfo item  = new RecentCallsInfo();;
				item.number = cursor.getString(RecentCallsListActivity.NUMBER_COLUMN_INDEX);
				item.type = cursor.getInt(RecentCallsListActivity.CALL_TYPE_COLUMN_INDEX);
				item.name = cursor.getString(RecentCallsListActivity.CALLER_NAME_COLUMN_INDEX);
				item.number_label = cursor.getString(RecentCallsListActivity.CALLER_NUMBERLABEL_COLUMN_INDEX);
				item.number_type = cursor.getInt(RecentCallsListActivity.CALLER_NUMBERTYPE_COLUMN_INDEX);
				//Toast.makeText(RecentCallsListActivity.this, item.name + item.number_label, Toast.LENGTH_LONG).show();
				item.date = cursor.getLong(RecentCallsListActivity.DATE_COLUMN_INDEX);
				item.duration = cursor.getInt(RecentCallsListActivity.DURATION_COLUMN_INDEX);
				addItemIntoList(item);
			}while (cursor.moveToNext());		
			
			cursor.close();
    	}
    	
    	mAdapter.notifyDataSetChanged(); 
		
    }
    
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.recent_calls);

        // Typing here goes to the dialer
        setDefaultKeyMode(DEFAULT_KEYS_DIALER);
        //Geesun
        //mAdapter = new RecentCallsAdapter();
        mListCallLogs = new ArrayList<RecentCallsInfo> ();
        mAdapter = new RecentCallsAdapter(this,R.layout.recent_calls_list_item,mListCallLogs);
        
        getListView().setOnCreateContextMenuListener(this);
        setListAdapter(mAdapter);

        mVoiceMailNumber = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                .getVoiceMailNumber();
        mQueryHandler = new QueryHandler(this);

        // Reset locale-based formatting cache
        sFormattingType = FORMATTING_TYPE_INVALID;
        
        //Geesun 
        LocationPreference  locPref = new LocationPreference(this);        
        mIpPrefix = locPref.getIpDailerPrefix();
    }

    @Override
    protected void onResume() {
        // The adapter caches looked up numbers, clear it so they will get
        // looked up again.
        if (mAdapter != null) {
            mAdapter.clearCache();
        }

        startQuery();
        resetNewCallsFlag();

        super.onResume();

        mAdapter.mPreDrawListener = null; // Let it restart the thread after next draw
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Kill the requests thread
        mAdapter.stopRequestProcessing();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.stopRequestProcessing();
        //Geesun
        /*Cursor cursor = mAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Clear notifications only when window gains focus.  This activity won't
        // immediately receive focus if the keyguard screen is above it.
        if (hasFocus) {
            try {
                ITelephony iTelephony =
                        ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                if (iTelephony != null) {
                    iTelephony.cancelMissedCallsNotification();
                } else {
                    Log.w(TAG, "Telephony service is null, can't call " +
                            "cancelMissedCallsNotification");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to clear missed calls notification due to remote exception");
            }
        }
    }

    /**
     * Format the given phone number using
     * {@link PhoneNumberUtils#formatNumber(android.text.Editable, int)}. This
     * helper method uses {@link #sEditable} and {@link #sFormattingType} to
     * prevent allocations between multiple calls.
     * <p>
     * Because of the shared {@link #sEditable} builder, <b>this method is not
     * thread safe</b>, and should only be called from the GUI thread.
     * <p>
     * If the given String object is null or empty, return an empty String.
     */
    private String formatPhoneNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }

        // If "number" is really a SIP address, don't try to do any formatting at all.
        if (PhoneNumberUtils.isUriNumber(number)) {
            return number;
        }

        // Cache formatting type if not already present
        if (sFormattingType == FORMATTING_TYPE_INVALID) {
            sFormattingType = PhoneNumberUtils.getFormatTypeForLocale(Locale.getDefault());
        }

        sEditable.clear();
        sEditable.append(number);

        PhoneNumberUtils.formatNumber(sEditable, sFormattingType);
        return sEditable.toString();
    }

    private void resetNewCallsFlag() {
        // Mark all "new" missed calls as not new anymore
        StringBuilder where = new StringBuilder("type=");
        where.append(Calls.MISSED_TYPE);
        where.append(" AND new=1");

        ContentValues values = new ContentValues(1);
        values.put(Calls.NEW, "0");
        mQueryHandler.startUpdate(UPDATE_TOKEN, null, Calls.CONTENT_URI,
                values, where.toString(), null);
    }

    private void startQuery() {
        mAdapter.setLoading(true);

        // Cancel any pending queries
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        mQueryHandler.startQuery(QUERY_TOKEN, null, Calls.CONTENT_URI,
                CALL_LOG_PROJECTION, null, null, Calls.DEFAULT_SORT_ORDER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.recentCalls_deleteAll)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
             menuInfo = (AdapterView.AdapterContextMenuInfo) menuInfoIn;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfoIn", e);
            return;
        }

        //Geesun
        RecentCallsInfo item =  mAdapter.getItem(menuInfo.position);
        String number = item.number;        
        //Cursor cursor = (Cursor) mAdapter.getItem(menuInfo.position);
        //String number = cursor.getString(NUMBER_COLUMN_INDEX);
        
        Uri numberUri = null;
        boolean isVoicemail = false;
        boolean isSipNumber = false;
        if (number.equals(CallerInfo.UNKNOWN_NUMBER)) {
            number = getString(R.string.unknown);
        } else if (number.equals(CallerInfo.PRIVATE_NUMBER)) {
            number = getString(R.string.private_num);
        } else if (number.equals(CallerInfo.PAYPHONE_NUMBER)) {
            number = getString(R.string.payphone);
        } else if (number.equals(mVoiceMailNumber)) {
            number = getString(R.string.voicemail);
            numberUri = Uri.parse("voicemail:x");
            isVoicemail = true;
        } else if (PhoneNumberUtils.isUriNumber(number)) {
            numberUri = Uri.fromParts("sip", number, null);
            isSipNumber = true;
        } else {
            //Geesun            
            PhoneNumProcess  process = new PhoneNumProcess(this,number);
            process.displayPhoneLocation();
            String phoneNum =process.getPhoneWithIp();
            numberUri = Uri.fromParts("tel", phoneNum, null);
        }

        ContactInfo info = mAdapter.getContactInfo(number);
        boolean contactInfoPresent = (info != null && info != ContactInfo.EMPTY);
        if (contactInfoPresent) {
            menu.setHeaderTitle(info.name);
        } else {
            menu.setHeaderTitle(number);
        }

        if (numberUri != null) {
        	//Geesun
        	Intent directIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,Uri.fromParts("tel",
        			number, null));
        	menu.add(0,0,0,R.string.call_direct).setIntent(directIntent);
        	
            Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberUri);
            menu.add(0, CONTEXT_MENU_CALL_CONTACT, 0,
                    getResources().getString(R.string.recentCalls_callNumber, number))
                    .setIntent(intent);
        }

        if (contactInfoPresent) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, info.personId));
            StickyTabs.setTab(intent, getIntent());
            menu.add(0, 0, 0, R.string.menu_viewContact).setIntent(intent);
        }

        if (numberUri != null && !isVoicemail && !isSipNumber) {
            menu.add(0, 0, 0, R.string.recentCalls_editNumberBeforeCall)
                    .setIntent(new Intent(Intent.ACTION_DIAL, numberUri));
            menu.add(0, 0, 0, R.string.menu_sendTextMessage)
                    .setIntent(new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts("sms", number, null)));
        }

        // "Add to contacts" item, if this entry isn't already associated with a contact
        if (!contactInfoPresent && numberUri != null && !isVoicemail && !isSipNumber) {
            // TODO: This item is currently disabled for SIP addresses, because
            // the Insert.PHONE extra only works correctly for PSTN numbers.
            //
            // To fix this for SIP addresses, we need to:
            // - define ContactsContract.Intents.Insert.SIP_ADDRESS, and use it here if
            //   the current number is a SIP address
            // - update the contacts UI code to handle Insert.SIP_ADDRESS by
            //   updating the SipAddress field
            // and then we can remove the "!isSipNumber" check above.

            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            intent.putExtra(Insert.PHONE, number);
            menu.add(0, 0, 0, R.string.recentCalls_addToContact)
                    .setIntent(intent);
        }
        menu.add(0, CONTEXT_MENU_ITEM_DELETE, 0, R.string.recentCalls_removeFromRecentList);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_CONFIRM_DELETE_ALL:
                return new AlertDialog.Builder(this)
                    .setTitle(R.string.clearCallLogConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.clearCallLogConfirmation)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getContentResolver().delete(Calls.CONTENT_URI, null, null);
                            // TODO The change notification should do this automatically, but it
                            // isn't working right now. Remove this when the change notification
                            // is working properly.
                            startQuery();
                        }
                    })
                    .setCancelable(false)
                    .create();
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE_ALL: {
                showDialog(DIALOG_CONFIRM_DELETE_ALL);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //Geesun Add 
    private class DeleteClickListener implements DialogInterface.OnClickListener {
        private String  mWhere;
        
        public DeleteClickListener(String where) {
        	mWhere = where;
        }

        public void onClick(DialogInterface dialog, int which) {
            getContentResolver().delete(Calls.CONTENT_URI, mWhere, null);
            startQuery();
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
       //Geesun Add
        // Convert the menu info to the proper type
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
             menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfoIn", e);
            return false;
        }

        switch (item.getItemId()) {
            case CONTEXT_MENU_ITEM_DELETE: {
            	//Geesun
            	/*
                Cursor cursor = mAdapter.getCursor();
                if (cursor != null) {
                    cursor.moveToPosition(menuInfo.position);
                    cursor.deleteRow();
                }*/
            	RecentCallsInfo item2 =  mAdapter.getItem(menuInfo.position);
            	if(item2 != null){
	                StringBuilder where = new StringBuilder();
	                where.append(Calls.NUMBER);
	                where.append(" like  '%" + item2.number + "'");
	                
	                new AlertDialog.Builder(this)
                    .setTitle(R.string.deleteConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.deleteCallLogConfirmation)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DeleteClickListener(where.toString()))
                    .show();             
	                
            	}
                return true;
            }
            case CONTEXT_MENU_CALL_CONTACT: {
                StickyTabs.saveTab(this, getIntent());
                startActivity(item.getIntent());
                return true;
            }
            default: {
                return super.onContextItemSelected(item);
            }
        }
        
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                long callPressDiff = SystemClock.uptimeMillis() - event.getDownTime();
                if (callPressDiff >= ViewConfiguration.getLongPressTimeout()) {
                    // Launch voice dialer
                    Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                    }
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                try {
                    ITelephony phone = ITelephony.Stub.asInterface(
                            ServiceManager.checkService("phone"));
                    if (phone != null && !phone.isIdle()) {
                        // Let the super class handle it
                        break;
                    }
                } catch (RemoteException re) {
                    // Fall through and try to call the contact
                }

                callEntry(getListView().getSelectedItemPosition());
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /*
     * Get the number from the Contacts, if available, since sometimes
     * the number provided by caller id may not be formatted properly
     * depending on the carrier (roaming) in use at the time of the
     * incoming call.
     * Logic : If the caller-id number starts with a "+", use it
     *         Else if the number in the contacts starts with a "+", use that one
     *         Else if the number in the contacts is longer, use that one
     */
    private String getBetterNumberFromContacts(String number) {
        String matchingNumber = null;
        // Look in the cache first. If it's not found then query the Phones db
        ContactInfo ci = mAdapter.mContactInfo.get(number);
        if (ci != null && ci != ContactInfo.EMPTY) {
            matchingNumber = ci.number;
        } else {
            try {
                Cursor phonesCursor =
                    RecentCallsListActivity.this.getContentResolver().query(
                            Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                    number),
                    PHONES_PROJECTION, null, null, null);
                if (phonesCursor != null) {
                    if (phonesCursor.moveToFirst()) {
                        matchingNumber = phonesCursor.getString(MATCHED_NUMBER_COLUMN_INDEX);
                    }
                    phonesCursor.close();
                }
            } catch (Exception e) {
                // Use the number from the call log
            }
        }
        if (!TextUtils.isEmpty(matchingNumber) &&
                (matchingNumber.startsWith("+")
                        || matchingNumber.length() > number.length())) {
            number = matchingNumber;
        }
        return number;
    }

    private void callEntry(int position) {
        if(mAdapter.getCount() == 0){
            return;
        }

        if (position < 0) {
            // In touch mode you may often not have something selected, so
            // just call the first entry to make sure that [send] [send] calls the
            // most recent entry.
            position = 0;
        }
        //Geesun 
        RecentCallsInfo item =  mAdapter.getItem(position);
        //final Cursor cursor = mAdapter.getCursor();
        //if (cursor != null && cursor.moveToPosition(position)) {
        if(item !=null){
        	//Geesun
            String number = item.number;//cursor.getString(NUMBER_COLUMN_INDEX);
            if (TextUtils.isEmpty(number)
                    || number.equals(CallerInfo.UNKNOWN_NUMBER)
                    || number.equals(CallerInfo.PRIVATE_NUMBER)
                    || number.equals(CallerInfo.PAYPHONE_NUMBER)) {
                // This number can't be called, do nothing
                return;
            }
            //Geesun
            int callType = item.type;//cursor.getInt(CALL_TYPE_COLUMN_INDEX);
            if (!number.startsWith("+") &&
                    (callType == Calls.INCOMING_TYPE
                            || callType == Calls.MISSED_TYPE)) {
                // If the caller-id matches a contact with a better qualified number, use it
                number = getBetterNumberFromContacts(number);
            }
            
            //Geesun            
            PhoneNumProcess  process = new PhoneNumProcess(this,number);
            process.displayPhoneLocation();
            String phoneNum =process.getPhoneWithIp();
            
            Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                    Uri.fromParts("tel", phoneNum, null));
            StickyTabs.saveTab(this, getIntent());
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, CallDetailActivity.class);
        //Geesun
        //intent.setData(ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id));
        intent.putExtra("NUMBER", mListCallLogs.get(position).number);
		  StickyTabs.setTab(intent, getIntent());        
		  startActivity(intent);

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
