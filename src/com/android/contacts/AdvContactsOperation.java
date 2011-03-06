package com.android.contacts;


import java.util.Vector;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;

import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;


import android.provider.Contacts.Phones;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import android.widget.TextView;
import android.util.Log;



public class AdvContactsOperation extends ListActivity {
    /** Called when the activity is first created. */
	private static final String TAG = "ListContacts";
	
	ContactsListAdapter ContactsAdp;
	
    public static final class ContactsListItemViews {
    	long ID = 1;
        TextView nameView;        
        TextView labelView;
        TextView numberView;
        CheckBox selectView;
        String  lookupKey;
    }
	
    static final String[] PHONES_PROJECTION = new String[] {
    	 Phone.RAW_CONTACT_ID,
    	 Contacts.LOOKUP_KEY,
    	 Contacts.DISPLAY_NAME,
         Phone.LABEL, 
         Phone.NUMBER, 
         Phone.TYPE, 
         Contacts._ID,
    }; 
    
    static final int PEOPLE_ID_IDX = 0;
    static final int PEOPLE_LOOKUP_KEY = 1;
    static final int PHONE_NAME_IDX = 2; 
    static final int PHONE_LABEL_IDX = 3; 
    static final int PHONE_NUMBER_IDX = 4; 
    static final int PHONE_TYPE_IDX = 5; 
    
    private static final class PeopleInfo{
    	long ID;
    	String lookupKey;
    	String name;
    	String number;
    }

    
    public static final String CONTACTS_LIST_DATA_TAG = "CONTACTS_LIST_DATA";
    
    private Vector<PeopleInfo> PeopleInfoList;
    
    
    private boolean isSelectAll = false;
    
    ContactsListItemViews selectAllView;
    
    private MenuItem mDeleteMenuItem;

    private ProgressDialog mProgressDiag;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adv_contacts_list);
                
        PeopleInfoList = new Vector<PeopleInfo>();
        
        final ListView list = getListView();
        list.setFastScrollEnabled(false); 
        StringBuilder where = new StringBuilder();
        where.append(Phone.NUMBER + " not NULL or " + Phone.DISPLAY_NAME + " not NULL" ); 
        
        String sort =  Phone.DISPLAY_NAME + "  COLLATE LOCALIZED ASC";
        Cursor c = getContentResolver().query(Phone.CONTENT_URI, PHONES_PROJECTION, where.toString(), null, sort);       
        ContactsAdp = new ContactsListAdapter(this,R.layout.adv_contacts_list_item,c);        
       
        LayoutInflater mInflater;
        
        mInflater = LayoutInflater.from(this);
        View view = mInflater.inflate(R.layout.adv_contacts_list_item, null);

        view.setMinimumHeight(60);
        
        selectAllView = new ContactsListItemViews();
        selectAllView.nameView = (TextView) view.findViewById(R.id.name);	            
        selectAllView.labelView = (TextView) view.findViewById(R.id.label);
        selectAllView.numberView = (TextView) view.findViewById(R.id.number);
        selectAllView.selectView = (CheckBox) view.findViewById(R.id.select);
        selectAllView.selectView.setClickable(false);
        selectAllView.lookupKey = new String();
        selectAllView.ID = -1;
        
        selectAllView.nameView.setText(getString(R.string.selectAll));
        view.setTag(selectAllView);        
        
        list.addHeaderView(view);        
        
        setListAdapter(ContactsAdp);     
    } 
    
    final class ContactsListAdapter extends ResourceCursorAdapter 
    {

		public ContactsListAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
			
		}

       @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);            
            // Get the views to bind to
            ContactsListItemViews views = new ContactsListItemViews();
            views.nameView = (TextView) view.findViewById(R.id.name);	            
            views.labelView = (TextView) view.findViewById(R.id.label);
            views.numberView = (TextView) view.findViewById(R.id.number);
            views.selectView = (CheckBox) view.findViewById(R.id.select); 
            views.lookupKey = new String();
           
            view.setTag(views);
            return view;
        }  

		@Override
		public void bindView(View view, Context context, Cursor c) {
			 final ContactsListItemViews views = (ContactsListItemViews) view.getTag();	
			 String name = c.getString(PHONE_NAME_IDX);
			 String number = c.getString(PHONE_NUMBER_IDX);
			 
			 if(name == null)
				 name = number;
			 
			 views.nameView.setText(name);
			 views.labelView.setText(Phones.getDisplayLabel(context, c.getInt(PHONE_TYPE_IDX), c.getString(PHONE_LABEL_IDX)));
			 views.numberView.setText(number);
			 
			 views.selectView.setClickable(false);

			 views.ID = c.getLong(PEOPLE_ID_IDX);
			 
			 views.lookupKey = c.getString(PEOPLE_LOOKUP_KEY);
			 
			 if(isPeopleSelect(views.ID))
				 views.selectView.setChecked(true);
			 else
				 views.selectView.setChecked(false);
		}		
    }

    protected void onListItemClick(ListView l, View view, int position, long id) {
		final ContactsListItemViews views = (ContactsListItemViews) view.getTag();
		if(views.ID == -1){
			isSelectAll = !views.selectView.isChecked();			
			PeopleInfoList.clear();			  
			if(isSelectAll){			
				Cursor  c = ContactsAdp.getCursor();				
				if(c != null){
					c.moveToFirst();
				    do{
				    	PeopleInfo phoneItem = new PeopleInfo();
				    	phoneItem.ID = c.getLong(PEOPLE_ID_IDX);
				    	phoneItem.lookupKey = c.getString(PEOPLE_LOOKUP_KEY);
				    	phoneItem.name = c.getString(PHONE_NAME_IDX);
				    	phoneItem.number  = c.getString(PHONE_NUMBER_IDX);
				    	PeopleInfoList.add(phoneItem);
				    }while(c.moveToNext());
			    }
			}
			  
			ContactsAdp.notifyDataSetChanged();
			
		}else {
			if(!views.selectView.isChecked()){
				PeopleInfo phoneItem = new PeopleInfo();
				phoneItem.ID = views.ID;
				phoneItem.lookupKey = views.lookupKey;
				phoneItem.name = views.nameView.getText().toString();
				phoneItem.number = views.numberView.getText().toString();			
				PeopleInfoList.add(phoneItem);
				if(isAllPeopleSelect()){
					isSelectAll = true;
					selectAllView.selectView.setChecked(true);
				} 
			}else{
				isSelectAll = false;
				selectAllView.selectView.setChecked(false);
				deletePeopleFromList(views.ID);
			}
		}
		
		views.selectView.setChecked(!views.selectView.isChecked());		
    }
    
    
    private boolean isAllPeopleSelect(){
		Cursor  c = ContactsAdp.getCursor();				
		if(c != null){
			c.moveToFirst();
		    do{
		    	
		    	if(!isPeopleSelect(c.getLong(PEOPLE_ID_IDX)))
		    			return false;
		    }while(c.moveToNext());
	    }
		
		return true;
    }
     
    private boolean isPeopleSelect(long Id){

    	
		for(int i = 0 ; i < PeopleInfoList.size(); i++){
			if(Id  == PeopleInfoList.get(i).ID){
				return true;
			}
		}
		return false;
    }
    
    private void deletePeopleFromList(long id){
   	
		for(int i = 0 ; i < PeopleInfoList.size(); i++){
			if(id == PeopleInfoList.get(i).ID){
				PeopleInfoList.remove(i);
				break;
			}
		}
    } 

    public boolean onCreateOptionsMenu(Menu menu) {
       
    	mDeleteMenuItem = menu.add(0, 0, 0, R.string.deleteConfirmation_title)
            	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
       
        
        return super.onCreateOptionsMenu(menu);
    
    }
    
    
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if(PeopleInfoList.size() > 0)
    		mDeleteMenuItem.setVisible(true);
    	else
    		mDeleteMenuItem.setVisible(false);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) { 
    	new AlertDialog.Builder(this)
        .setTitle(R.string.deleteConfirmation_title)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(R.string.deleteConfirmation)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, new DeleteClickListener())
        .setCancelable(false)
        .show();
    	return true;
    }
    
    private class DeleteClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {

        	
        	mProgressDiag = ProgressDialog.show(AdvContactsOperation.this,
        			getString(R.string.deleteConfirmation_title),getString(R.string.contacts_deleting),
        			true,true);       	
        	
        	DeleteContactsThread deleteTask = new DeleteContactsThread();
        	mProgressDiag.setOnCancelListener(deleteTask);
        	deleteTask.start();
        }
    }
      
    private class DeleteContactsThread extends Thread implements OnCancelListener{
    	boolean mCanceled = false;
        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }
        
    	public void run() {
        	int i ;         	
        	for(i = 0; i < PeopleInfoList.size(); i++){
        		if(mCanceled)
        			break;
        		
        		//Uri mUri = RawContacts.getContactLookupUri(getContentResolver(),
                //        ContentUris.withAppendedId(RawContacts.CONTENT_URI, PeopleInfoList.get(i).ID)); 
                final long contactId = PeopleInfoList.get(i).ID;
                final String lookupKey = PeopleInfoList.get(i).lookupKey;
                Uri mUri = Contacts.getLookupUri(contactId, lookupKey);
                
        			//ContentUris.withAppendedId(Phone.CONTENT_URI,PeopleInfoList.get(i).ID);
        		getContentResolver().delete(mUri, null, null);
        	}
        	
        	mProgressDiag.dismiss();
    	}

    }
    
}
