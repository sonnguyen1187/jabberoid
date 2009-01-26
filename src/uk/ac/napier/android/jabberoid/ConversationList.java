package uk.ac.napier.android.jabberoid;


import java.util.ArrayList;

import uk.ac.napier.android.jabberoid.data.JabberoidDbConnector;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ConversationList extends ListActivity {
	
	private static final String TAG = "ConversationList.class";
	private static final int RESULT_CHAT = 4004;
	private static final int RESULT_DIRECTCHAT = 4005;
	
	
	private ArrayList<String> chatList = new ArrayList<String>();
	
	private TextView tv;
	
	private String lastJid;
	BroadcastReceiver csr;
	IntentFilter f;
	
	boolean show = true;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.conversationlist);
		
		tv = (TextView)findViewById(R.id.contactList_NoConversation);
		
		csr =  new BroadcastReceiver() {
        	@Override
        	public void onReceive(Context context, Intent intent) {
        		getChats();
        		updateList(false);
        	}
        };
        f = new IntentFilter();
        f.addAction("uk.ac.nappier.android.androidim.NEW_MESSAGE");
        
        registerForContextMenu(getListView());

	}
	
	@Override
	public void onStart() {
		super.onStart();
		getChats();
		updateList(show);
		show = true;
		if(getIntent().getBooleanExtra("startChat", false)) {
			startChat(getIntent().getStringExtra("jid"));
			show = false;
		}

	}
	
	@Override
	public void onResume() {
		super.onResume();
		cancelNotification();

		registerReceiver(csr, f);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(csr);
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
	}
	
	@Override
	protected void onListItemClick(ListView lv, View v, int pos, long id) {
		super.onListItemClick(lv, v, pos, id);
		startChat(pos);		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		new MenuInflater(getApplication()).inflate(R.menu.conversationlist_context, menu);
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		menu.setHeaderTitle("Chat with " + getJid(acmi.position));
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.menuEnter:
			startChat(acmi.position);
			return true;
		case R.id.menuCloseChat:
			closeChat(acmi.position);
			return true;
		}
		
		
		return super.onContextItemSelected(item);
	}
	
	private int getChats() {
		final String[] returnColumns = {
				Constants.TABLE_CONVERSATION_FIELD_ID,
				Constants.TABLE_CONVERSATION_FIELD_CHAT,
			    "MIN("+(Constants.TABLE_CONVERSATION_FIELD_DATE)+") as 'firstReceived'",
			   };
		
		SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();

		Cursor result = db.query(Constants.TABLE_CONVERSATION, 
				returnColumns, 
				null, //Constants.TABLE_CONVERSATION_FIELD_FROM + "!= 'me'", 
				null, 
				Constants.TABLE_CONVERSATION_FIELD_CHAT, 
				null, 
				"firstReceived ASC" );
		
		int count = result.getCount();
		
		chatList.clear();
		result.moveToFirst();
		while(!result.isAfterLast()) {
			chatList.add(result.getString(result.getColumnIndex(Constants.TABLE_CONVERSATION_FIELD_CHAT)));
			result.moveToNext();
		}
		
		result.close();
		result = null;
		db.close();
		
		return count;
	}
		
	
		
		private void updateList(boolean startChat) {
		if(chatList.size() > 0) {
			
			if(chatList.size()==1 && startChat) {
				startChat(0);
			}
			
			tv.setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
			
			ListAdapter adapter = new ArrayAdapter<String>(
					this,
					R.layout.conversation_item,
					chatList
				);
			getListView().setAdapter(adapter);
		
		} else {
			getListView().setVisibility(View.GONE);
			tv.setVisibility(View.VISIBLE);
			tv.setText(R.string.conversationList_noOpenConversation);
		}
		
		// close the database after usage
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode==RESULT_CHAT) {
			
			int chats = getChats();
			
			if(chats==0) {
				finish();
			} else if(chats==1) {
				String jid;

				jid = getJid(0);
				
				if(lastJid.equals(jid)) {
					finish();		
				} else {
					show = false;
				}
			} else {
				show = false;
			}
			
		} else if (requestCode==RESULT_DIRECTCHAT) {
			finish();
		}
	}
	
	private void cancelNotification() {
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(Constants.NEW_MESSAGE_NOTIFICATION);
	}
	
	private void startChat(int pos) {
		String jid = getJid(pos);
		lastJid = jid;
		finishActivity(RESULT_CHAT);
		Intent i = new Intent(this, Conversations.class);
		i.putExtra("jid", jid);
		startActivityForResult(i, RESULT_CHAT);
	}
	
	private void startChat(String jid) {
		finishActivity(RESULT_DIRECTCHAT);
		Intent i = new Intent(this, Conversations.class);
		i.putExtra("jid", jid);
		startActivityForResult(i, RESULT_DIRECTCHAT);
	}
	
	private void closeChat(int pos) {
		SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();
		db.delete(Constants.TABLE_CONVERSATION,
				Constants.TABLE_CONVERSATION_FIELD_CHAT + " = '" + getJid(pos) + "'",
				null);
		
		getChats();
		updateList(false);
		db.close();
	}
	
	private String getJid(int pos) {
		return chatList.get(pos);
	}
	
	private void handleUpdateList() {
		
	}

}
