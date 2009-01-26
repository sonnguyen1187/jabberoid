package uk.ac.napier.android.jabberoid;

import java.io.ObjectOutputStream.PutField;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import uk.ac.napier.android.jabberoid.data.JabberoidDbConnector;
import uk.ac.napier.android.jabberoid.service.JabberoidConnectionService;
import uk.ac.napier.android.jabberoid.service.ConnectionServiceCall;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.CursorJoiner.Result;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class Conversations extends Activity implements View.OnClickListener, OnKeyListener {
	
	private final static int CLOSE_CONVERSATION = Menu.FIRST+1;
	private final static int CLOSE_GO_CONTACTLIST = Menu.FIRST+2;
	private final static int RETURN_TO_CONTACTLIST = Menu.FIRST+3;
	
	private final static String TAG = "Conversation.class";
	
	Spinner contactChoser;
	TextView conversationWindow;
	EditText messageInput;
	Button sendButton;
	ScrollView scroller;
	LinearLayout ll1;
	LinearLayout ll2;
	
	SQLiteDatabase db;
	
	private static String jid;
	
	public BroadcastReceiver csr;
	private IntentFilter f;
	
	private Intent aimConServ;
	//private final Intent aimConServ = new Intent(this,service.AimConnectionService);
	private ConnectionServiceCall service;
	private ServiceConnection callConnectService = null;
	
	private Date date;
	private Calendar cal;
	private DateFormat df;
	private Calendar lastStamp;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.conversation);
		
		ll1 = (LinearLayout)findViewById(R.id.conversationLinear1);
		ll1.setVisibility(View.GONE);
		ll2 = (LinearLayout)findViewById(R.id.conversationLinear2);

		scroller = (ScrollView)findViewById(R.id.conversationScroller);
		conversationWindow = (TextView)findViewById(R.id.conversationWindow);
		messageInput = (EditText)findViewById(R.id.messageInput);
		sendButton = (Button)findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);
		conversationWindow.setSingleLine(false);
		
		messageInput.setOnKeyListener(this);
		
		registerForContextMenu(ll2);
		//registerForContextMenu(scroller);
		//registerForContextMenu(conversationWindow);
		
		aimConServ =  new Intent(this,JabberoidConnectionService.class);
		df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());

	}
	
	@Override
	public void onResume() {
		super.onResume();
		db = new JabberoidDbConnector(this).getWritableDatabase();
		jid = getIntent().getStringExtra("jid");
		setTitle(getString(R.string.conversation_name) +": " +jid);
		bindToService();
		
		printMessages(getMessages(false));
		setUnread();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		db.close();
		jid = null;
		unbindFromService();
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.conversation, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		new MenuInflater(getApplication()).inflate(R.menu.conversation, menu);
		menu.setHeaderTitle("Chat with "+jid);
		super.onCreateContextMenu(menu, v, info);

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onContextItemSelected(item);
	}
	
	public void onClick(View v) {
		if(v==sendButton) {
			sendMessage();
		}
	}
	
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if(event.isShiftPressed() && keyCode == KeyEvent.KEYCODE_ENTER) {
			return false;
		} else if(view==messageInput && keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
			sendMessage();
			return true;
		}
		return false;
	}

	
	private boolean doSelectedItem(MenuItem item) {
		
		switch(item.getItemId()) {
			case(R.id.menuConvCloseChat)://CLOSE_CONVERSATION):
				closeConversation();
				return true;
			case(R.id.menuConvReturn)://RETURN_TO_CONTACTLIST):
				finish();
				return true;
		}
		
		return false;
	}
	
	
	private void bindToService() {
    	if(callConnectService == null) {
    		callConnectService = new ServiceConnection() {
    			public void onServiceConnected(ComponentName name, IBinder binder) {
    				service = ConnectionServiceCall.Stub.asInterface(binder);
    				try {
						if(!service.isLoggedIn()) {
							sendButton.setEnabled(false);
							conversationWindow.append("Youre offline and cannot send messages");
							conversationWindow.setEnabled(false);
							
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}

    			public void onServiceDisconnected(ComponentName name) {
    				service = null;
    			}
    		};
    	}
    	
    	csr =  new BroadcastReceiver() {
        	@Override
        	public void onReceive(Context context, Intent intent) {
        		
        		printMessages(getMessages(true));
        		setUnread();
        	}
        };
        f = new IntentFilter();
        f.addAction("uk.ac.nappier.android.androidim.NEW_MESSAGE");
        
        
        bindService(aimConServ,callConnectService,BIND_AUTO_CREATE);
    	registerReceiver(csr, f);
    }
	
    private void unbindFromService() {
    	if(callConnectService!=null) {
    		unregisterReceiver(csr);
    		unbindService(callConnectService);
    	}
    }
	
	private Cursor getMessages(boolean justNewMessages) {
		
		
		final String table = Constants.TABLE_CONVERSATION;
																				//index
		final String[] columns = { Constants.TABLE_CONVERSATION_FIELD_ID,		//0
								   Constants.TABLE_CONVERSATION_FIELD_DATE,		//1
								   Constants.TABLE_CONVERSATION_FIELD_FROM,		//2
								   Constants.TABLE_CONVERSATION_FIELD_TO,		//3
								   Constants.TABLE_CONVERSATION_FIELD_MSG		//4
			};
		
		String selection;
		
		if(justNewMessages) {
			selection = "(" + Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid +"' or "
							+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "') and "
							+ Constants.TABLE_CONVERSATION_FIELD_NEW + " = '1'";
		} else {
			selection = "(" + Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid +"' or "
			 				+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "')";
		}
		
		final String[] selectionArgs = null;
		final String groupBy = null;
		final String having = null;
		final String orderBy = Constants.TABLE_CONVERSATION_FIELD_DATE;
		
		return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
				
	}
	
	private boolean sendMessage() {
		try {
			service.sendMessage(jid, messageInput.getText().toString());
			printMessages(null);
			messageInput.setText("");

		} catch (RemoteException e) {
			return false;
		}
		
		return true;
	}
	
	private void printMessages(final Cursor c) {
		runOnUiThread(new Runnable() {
			public void run() {
				
				
				// TODO Improve the date comparison of the method
				Date currentDate = new Date();
				Calendar today = Calendar.getInstance();
				today.clear();
				today.set(currentDate.getYear(), currentDate.getMonth(), currentDate.getDate());

				
				Calendar compareCal = Calendar.getInstance();
				
				//today.set(currentDate.getYear(), currentDate.getMonth(), currentDate.getDay());
				
				if(c!=null) {
					c.moveToFirst();
					
					String date;
					String from;
					String message;
					
					while(!c.isAfterLast()) {
						
						Date temp = new Date(c.getLong(c.getColumnIndex(Constants.TABLE_CONVERSATION_FIELD_DATE)));
						compareCal.clear();
						compareCal.set(temp.getYear(), temp.getMonth(), temp.getDate());

						from = c.getString(2);
						message = c.getString(4);
						
						
						if(lastStamp == null || today.compareTo(lastStamp)!=0) {
							date = temp.toLocaleString();
						} else {
							date = df.format(temp.getTime());
						}
						
						conversationWindow.append("("+date+ ") " + from + ":\n" + message +"\n");
						
						lastStamp = compareCal;
						
						c.moveToNext();
						
					}

				} else {
					
					String message = messageInput.getText().toString();
					String date;
					
					if(lastStamp == null || today.compareTo(lastStamp)!=0) {
						date = currentDate.toLocaleString();
					} else {
						date = df.format(currentDate);
					}
					
					conversationWindow.append("("+date+") me:\n"+ message +"\n");
					
					lastStamp = today;
					
				}
				//c.close();
				scrollDown();
			}
		});

	}
	
	private void setUnread() {
		ContentValues cv = new ContentValues();
		cv.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 0);
		
		db.update(Constants.TABLE_CONVERSATION,
				  cv,
				  Constants.TABLE_CONVERSATION_FIELD_FROM+"='"+jid+"' or "+Constants.TABLE_CONVERSATION_FIELD_TO+"='"+jid+"'",
				  null);
	}
	
	private void closeConversation() {
		
		db.delete(Constants.TABLE_CONVERSATION,
				Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid + "' or "
				+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "'",
				null);
		
		setResult(RESULT_OK, getIntent().setAction(jid));
		
		finish();
	}
	
	
	private void scrollDown() {
			scroller.smoothScrollTo(0, conversationWindow.getBottom()+2000); // WORKAROUND
	}
	

}
