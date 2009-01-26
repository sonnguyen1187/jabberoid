package uk.ac.napier.android.jabberoid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.napier.android.jabberoid.data.JabberoidDbConnector;
import uk.ac.napier.android.jabberoid.service.JabberoidConnectionService;
import uk.ac.napier.android.jabberoid.service.ConnectionServiceCall;
import uk.ac.napier.android.jabberoid.tools.AddUserDialog;
import uk.ac.napier.android.jabberoid.tools.AimStatusArrayAdapter;
import uk.ac.napier.android.jabberoid.tools.ContactEntry;
import uk.ac.napier.android.jabberoid.tools.ContactListExpandableListAdapter;
import uk.ac.napier.android.jabberoid.tools.OnAbortListener;
import uk.ac.napier.android.jabberoid.tools.OnConfirmListener;
import uk.ac.napier.android.jabberoid.tools.PressedEvent;
import uk.ac.napier.android.jabberoid.tools.SetStatusDialog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class Jabberoid extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener, ExpandableListView.OnChildClickListener {

  /**
   * The request ID, if the Settings Activity is called. If the setting dialog is finished, this ID will be returned. The actual number does not have a meaning.
   */
  private static final int REQUEST_SETTINGS = 2008;
  private static final int RESULT_CHATLIST = 4003;

  private static final String TAG = "Jabberoid.class";

  private SharedPreferences prefs;

  private BroadcastReceiver connectionClosedReceiver;
  private BroadcastReceiver progressReceiver;
  private BroadcastReceiver presenceBcr;
  private IntentFilter f;

  private ProgressDialog pd = null;

  private ExpandableListView contactList;
  private String[] status;

  /**
   * btnSettings invokes a new intent/activity (Settings.class) where all adjustments can be made 
   */
  private  ImageButton btnSettings;
  private  ImageButton btnAddUser;
  private  ImageButton btnEditStatusMsg;
  private  ImageButton btnShowOffliner;

  private Spinner spin;

  private List<List<HashMap<String,ContactEntry>>> groupedContactList;
  private List<String> groups;

  private SetStatusDialog d;

  private final Activity aim = this;

  private Intent aimConServ;
  //private final Intent aimConServ = new Intent(this,service.AimConnectionService);
  private ConnectionServiceCall service;
  private ServiceConnection callConnectService = null;

  private SimpleExpandableListAdapter contactListAdapter;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.main);

    prefs = PreferenceManager.getDefaultSharedPreferences(this);

    contactList = (ExpandableListView)findViewById(R.id.contactList);
    contactListAdapter = null;
    contactList.setOnChildClickListener(this);

    btnSettings = (ImageButton)findViewById(R.id.butSettings);
    btnSettings.setOnClickListener(this);
    btnAddUser = (ImageButton)findViewById(R.id.butAddUser);
    btnAddUser.setOnClickListener(this);
    btnEditStatusMsg = (ImageButton)findViewById(R.id.butEditStatus);
    btnEditStatusMsg.setOnClickListener(this);
    btnShowOffliner = (ImageButton)findViewById(R.id.butShowOffline);
    btnShowOffliner.setOnClickListener(this);
    btnShowOffliner.setVisibility(View.GONE);
    //Debug.startMethodTracing("jabberoid");


    status = getResources().getStringArray(R.array.statusSpinner);
    spin = (Spinner)findViewById(R.id.status);
    spin.setOnItemSelectedListener(this);
    AimStatusArrayAdapter aa = new AimStatusArrayAdapter(this, R.layout.spinner_view_dropdown, status); //(this,R.layout.spinner_view,R.array.statusSpinner);
    spin.setAdapter(aa);
    spin.setPromptId(R.string.setYourStatus);

    registerForContextMenu(contactList);

    /*
     * Start the connection service, which then actually build the connection to the xmpp server and logs the user in.
     */

    aimConServ =  new Intent(this,JabberoidConnectionService.class);
    startService(aimConServ);


    Log.i(getClass().getSimpleName(), "AIM Started");
  }

  @Override
  public void onResume() {
    super.onResume();
    bindToService();
    contactList.setAdapter((ExpandableListAdapter)contactListAdapter);		
    updateList();
  }

  @Override
  public void onPause() {
    super.onPause();
    unbindFromService();
    Log.i(TAG, "AIM Paused");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.i(TAG, "AIM Stopped");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "AIM Destroyed");
    //Debug.stopMethodTracing();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
    ExpandableListContextMenuInfo  elcmi = (ExpandableListContextMenuInfo) info;
    int grpPos = ExpandableListView.getPackedPositionGroup(elcmi.packedPosition);
    int childPos = ExpandableListView.getPackedPositionChild(elcmi.packedPosition);

    String jid = groupedContactList.get(grpPos).get(childPos).get("contactJid").toString();

    menu.setHeaderTitle(jid);
    menu.add(Menu.NONE, 0, Menu.NONE, "Open Chat");
    super.onCreateContextMenu(menu, v, info);
  }

  public boolean onContextItemSelected(MenuItem item) {
    ExpandableListContextMenuInfo  elcmi = (ExpandableListContextMenuInfo) item.getMenuInfo();
    int groupPosition = ExpandableListView.getPackedPositionGroup(elcmi.packedPosition);
    int childPosition = ExpandableListView.getPackedPositionChild(elcmi.packedPosition);

    switch(item.getItemId()) {
    case 0:
      startChat(groupedContactList.get(groupPosition).get(childPosition).get("contactJid").getJid());
    }

    return true;
  }

  public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    startChat(groupedContactList.get(groupPosition).get(childPosition).get("contactJid").getJid());
    return true;
  }


  private boolean bindToService() {
    if(callConnectService == null) {
      callConnectService = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
          service = ConnectionServiceCall.Stub.asInterface(binder);

          try {
            if(service != null && !service.isLoggedIn()) {
              spin.setSelection(Constants.STATUS_OFFLINE);
            } else if(service != null && service.isLoggedIn()) {
              spin.setSelection(prefs.getInt("currentSelection", Constants.STATUS_OFFLINE));
            } else {
              spin.setSelection(Constants.STATUS_OFFLINE);
            }
          } catch (RemoteException e) {
            Log.e(TAG, "Unable to communicate with service");
            e.printStackTrace();
          }

        }

        public void onServiceDisconnected(ComponentName name) {
          service = null;
        }
      };
    }

    boolean bound = bindService(aimConServ,callConnectService,BIND_AUTO_CREATE);

    connectionClosedReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateList();
      }
    };

    registerReceiver(connectionClosedReceiver, new IntentFilter("uk.ac.napier.android.androidim.CONNECTION_CLOSED"));

    progressReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(pd.isShowing()) pd.dismiss();
      }
    };

    IntentFilter progressFilter = new IntentFilter("uk.ac.napier.android.androidim.LOGGED_IN");
    registerReceiver(progressReceiver, progressFilter);

    presenceBcr = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        updateList();
        //abortBroadcast();
      }
    };


    f = new IntentFilter();
    f.addAction("uk.ac.napier.android.androidim.PRESENCE_CHANGED");

    registerReceiver(presenceBcr, f);

    return bound;
  }

  private void unbindFromService() {
    if(callConnectService!=null) {
      unregisterReceiver(presenceBcr);
      unregisterReceiver(progressReceiver);
      unregisterReceiver(connectionClosedReceiver);
      unbindService(callConnectService);
    }
  }

  private void progressD() {
    pd = ProgressDialog.show(this, "Connecting", "Please wait while connecting with account " + prefs.getString("prefJabberIdKey", "NOT SET"));
  }


  public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

    setPreference("currentSelection",position);

    String mode = null;

    switch(position) {
    case Constants.STATUS_ONLINE:
      mode = "available";
      break;
    case Constants.STATUS_AWAY:
      mode = "away";
      break;
    case Constants.STATUS_E_AWAY:
      mode = "xa";
      break;
    case Constants.STATUS_DND:
      mode = "dnd";
      break;
    case Constants.STATUS_FREE:
      mode = "chat";
      break;
    }

    try {
      if(mode != null && !service.isLoggedIn()) {
        progressD();
        service.connect(null, "available", mode);
      } else if(mode != null) {
        service.setStatus(null, "available", mode);
      } else {
        service.disconnect();
      }
    }  catch(DeadObjectException e) {
      callConnectService.onServiceDisconnected(null);
    } catch (RemoteException e) {
      callConnectService.onServiceDisconnected(null);
    }


  }

  public void onNothingSelected(AdapterView<?> parent) {
    //tv.setText("");
  }


  public void onClick(View v) {
    if(v==btnSettings) {
      Intent i = new Intent(this, Settings.class);
      startActivityForResult(i,REQUEST_SETTINGS);
    } else if(v==btnAddUser) {
      AddUserDialog addUserDialog;
      addUserDialog = new AddUserDialog(this,R.layout.add_user_dialog,R.id.addUserDialog_confirm,R.id.addUserDialog_cancel,R.id.addUserDialog_entry,R.id.addUserDialog_groupSwitcher, groups);
      addUserDialog.setCancelable(true);
      addUserDialog.setTitle("Adding a new user");
      addUserDialog.setOnConfirmListener(new OnConfirmListener() {
        public void onConfirm(PressedEvent e) {
          //do nothing for the moment
        }
      });
      addUserDialog.show();

    } else if(v==btnEditStatusMsg) {
      d = new SetStatusDialog(this,R.layout.status_message_dialog,R.id.statusMessageDialog_confirm,R.id.statusMessageDialog_cancel,R.id.statusMessageDialog_msg, R.id.statusMessageDialog_msgSwitcher, getLastStatusMessages());
      d.setCancelable(true);
      d.setTitle("Set your status message");
      d.setOnAbortListener(new OnAbortListener() {
        public void onAbort(PressedEvent e) {
          Toast.makeText(aim, "Status Abort", Toast.LENGTH_SHORT).show();
        }
      });
      d.setOnConfirmListener(new OnConfirmListener() {
        public void onConfirm(PressedEvent e) {
          Toast.makeText(aim, d.getNewUserId(), Toast.LENGTH_SHORT).show();
        }
      });
      d.show();
    } else if (v==btnShowOffliner) {

      //stopService(aimConServ);
      finish();
    }
  }

  private List<HashMap<String,String>> fetchGroups() {
    List<HashMap<String,String>> resultList = new ArrayList<HashMap<String,String>>();

    SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();
    String table = Constants.TABLE_GROUP;
    String[] columns = { Constants.TABLE_GROUP_FIELD_GROUP };
    String selection = null;
    String[] selectionArgs = null;
    String groupBy = Constants.TABLE_GROUP_FIELD_GROUP;
    String having = null;
    String orderBy = null;

    Cursor groupResult = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);

    groups = new ArrayList<String>();
    String groupName;
    if(groupResult.getCount() > 0) {
      groupResult.moveToFirst();
      while(!groupResult.isAfterLast()) {
        HashMap<String,String> hm = new HashMap<String, String>();
        groupName = groupResult.getString(groupResult.getColumnIndex(Constants.TABLE_GROUP_FIELD_GROUP));
        hm.put("groupName", groupName);
        resultList.add(hm);
        groups.add(groupName);
        groupResult.moveToNext();
      }
    }	

    groupResult.deactivate();
    db.close();
    return resultList;
  }

  private List<List<HashMap<String,ContactEntry>>> fetchContacts() {

    List<List<HashMap<String,ContactEntry>>> resultList = new ArrayList<List<HashMap<String,ContactEntry>>>();

    SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();
    String table = Constants.TABLE_GROUP;
    String[] columns = { Constants.TABLE_GROUP_FIELD_GROUP };
    String selection = null;
    String[] selectionArgs = null;
    String groupBy = Constants.TABLE_GROUP_FIELD_GROUP;
    String having = null;
    String orderBy = null;

    Cursor groupResult = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);

    if(groupResult.getCount() > 0) {
      groupResult.moveToFirst();
      while(!groupResult.isAfterLast()) {
        resultList.add(getGroupEntries(groupResult.getString(groupResult.getColumnIndex(Constants.TABLE_GROUP_FIELD_GROUP))));
        groupResult.moveToNext();
      }
    }

    groupedContactList = resultList;
    groupResult.deactivate();
    db.close();
    return resultList;
  }

  public List<HashMap<String, ContactEntry>> getGroupEntries(String group) {
    List<HashMap<String,ContactEntry>> resultList = new ArrayList<HashMap<String,ContactEntry>>();


    SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();

    String sql = "SELECT " + Constants.TABLE_BUDDY + "." + Constants.TABLE_BUDDY_FIELD_JID + " FROM "
    + Constants.TABLE_GROUP + " INNER JOIN " + Constants.TABLE_BUDDY + " ON " 
    + Constants.TABLE_GROUP + "." + Constants.TABLE_GROUP_FIELD_JID + " = "
    + Constants.TABLE_BUDDY + "." + Constants.TABLE_BUDDY_FIELD_JID + " WHERE "
    + Constants.TABLE_GROUP_FIELD_GROUP +" = '" + group + "'"
    + " ORDER BY " + Constants.TABLE_BUDDY_FIELD_PRESENCETYPE + "," 
    + Constants.TABLE_BUDDY_FIELD_PRESENCEMODE + "," + Constants.TABLE_BUDDY + "." 
    + Constants.TABLE_BUDDY_FIELD_JID ;

    Cursor result = db.rawQuery(sql, null);

    if(result.getCount() > 0) {
      result.moveToFirst();
      while(!result.isAfterLast()) {
        HashMap<String,ContactEntry> jids = new HashMap<String,ContactEntry>();
        jids.put("contactJid",new ContactEntry(this, result.getString(result.getColumnIndex(Constants.TABLE_GROUP_FIELD_JID))));
        resultList.add(jids);
        result.moveToNext();
      }
    }

    result.deactivate();
    db.close();
    return resultList;

  }


  private void updateList() {
    contactListAdapter = new ContactListExpandableListAdapter(
        this,
        fetchGroups(),
        R.layout.contactlist_view,
        new String[] {"groupName"},
        new int[] {R.id.list_item},
        fetchContacts(),
        R.layout.contactlist_child,
        new String[] {"contactJid"},
        new int[] {R.id.list_item}
    );
    contactList.setAdapter((ExpandableListAdapter)contactListAdapter);

    if(contactList.getCount()!=0) {
      contactList.expandGroup(0);
    }
    contactList.refreshDrawableState();
  }

  public void setPreference(String name, Object value) {
    SharedPreferences.Editor editor = prefs.edit();

    if(value instanceof String) {
      editor.putString(name, String.valueOf(value));
    } else if(value instanceof Integer) {
      editor.putInt(name, Integer.parseInt(String.valueOf(value)));
    }

    editor.commit();
  }

  private void startChat(String jid) {
    finishActivity(RESULT_CHATLIST);
    Intent i = new Intent(this, ConversationList.class);
    i.putExtra("startChat", true);
    i.putExtra("jid", jid);
    startActivityForResult(i, RESULT_CHATLIST);
  }

  public void setStatusMessage(String message) {
    try {
      service.insertAndUseMessage(message);
    } catch(DeadObjectException e) {
      callConnectService.onServiceDisconnected(null);
    } catch (RemoteException e) {
      callConnectService.onServiceDisconnected(null);
    }
  }

  public List<String> getLastStatusMessages() {
    List<String> list = null;
    try {
      list =  service.getLastStatusMessages();
    } catch(DeadObjectException e) {
      callConnectService.onServiceDisconnected(null);
    } catch (RemoteException e) {
      callConnectService.onServiceDisconnected(null);
    }

    if(list==null) {
      list = new ArrayList<String>();
      list.add("");
    }

    return list;
  }
}