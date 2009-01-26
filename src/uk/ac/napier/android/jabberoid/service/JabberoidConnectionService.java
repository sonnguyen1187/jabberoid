package uk.ac.napier.android.jabberoid.service;

import java.sql.Time;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;  
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;  
import org.jivesoftware.smack.XMPPException;  
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.packet.DelayInformation;

import uk.ac.napier.android.jabberoid.Constants;
import uk.ac.napier.android.jabberoid.ConversationList;
import uk.ac.napier.android.jabberoid.R;
import uk.ac.napier.android.jabberoid.data.JabberoidDbConnector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class JabberoidConnectionService extends Service {

  private static final String TAG = "AimConnectionService";

  private SharedPreferences prefs;

  private ConnectionConfiguration cc = null;
  private XMPPConnection con = null;
  private Roster roster = null;

  private JabberoidConnectionService acs = this;

  private static final String CERT_DIR = "/system/etc/security/";
  private static final String CERT_FILE = "cacerts.bks";

  private final ConnectionServiceCall.Stub binder = new ConnectionServiceCall.Stub() {

    
    public void connect(String state, String type, String mode) {
      connectToServer(state, type, mode);
    }

    
    public void disconnect() throws RemoteException {
      disconnectFromServer();

    }

    
    public boolean isLoggedIn() throws RemoteException {
      return isUserLoggedIn();
    }

    
    public void logOff() throws RemoteException {
      // not used
    }

    
    public void login() throws RemoteException {
      // not used
    }


    public void setStatus(String state, String type,String mode) throws RemoteException {
      setPresenceState(state,type,mode);
    }


    public void sendMessage(String user, String message) throws RemoteException {
      sendMessagePacket(user, message);
    }


    public List<String> getLastStatusMessages() throws RemoteException {
      return acs.getLastStatusMessages();
    }


    public void insertAndUseMessage(String message) throws RemoteException {
      acs.insertAndUseMessage(message);

    }

  };


  private ConnectionListener connectionListener = new ConnectionListener() {


    public void connectionClosed() {
      setAllContactsOffline();
      Intent intent = new Intent("uk.ac.nappier.android.androidim.CONNECTION_CLOSED");
      sendBroadcast(intent);
    }


    public void connectionClosedOnError(Exception e) {
      setAllContactsOffline();

      Intent intent = new Intent("uk.ac.nappier.android.androidim.CONNECTION_ERROR_CLOSED");
      sendBroadcast(intent);
    }


    public void reconnectingIn(int seconds) {
      // TODO Auto-generated method stub

    }


    public void reconnectionFailed(Exception e) {
      // TODO Auto-generated method stub

    }


    public void reconnectionSuccessful() {
      // TODO Auto-generated method stub

    }

  };


  private PacketListener msgListener = new PacketListener() {
    public void processPacket(Packet packet) {


      Message msg = (Message)packet;
      HashMap<String, String> msgInfo = getMessageInfo(msg); 

      queueIncomingMessage(msgInfo);

      notifyUser(msgInfo.get("username"),"New message from " + msgInfo.get("username"));

      Intent intent = new Intent("uk.ac.nappier.android.androidim.NEW_MESSAGE");
      intent.putExtra("test", msgInfo.get("body"));
      sendBroadcast(intent);
    }
  };

  private void notifyUser(String fromUser, String msg) {
    NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    Notification n = new Notification(R.drawable.newmail_22, msg, System.currentTimeMillis());

    Intent i = new Intent(this,ConversationList.class);
    i.putExtra("newReceived", true);
    PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
    n.setLatestEventInfo(this, "You received a new Message", "From: " + fromUser, pi);

    nm.notify(Constants.NEW_MESSAGE_NOTIFICATION, n);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    Log.i(TAG, "AIM Connection Service created");
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    Log.i(TAG, "AIM Connection Service started");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    disconnectFromServer();
    setAllContactsOffline();
    Log.i(TAG, "AIM Connection Service destroyed");
  }


  public void connectToServer() {
    connectToServer("", "available","available");
  }

  public void connectToServer(final String state, final String type, final String mode) {

    String username = prefs.getString("prefJabberIdKey", null);
    String host = StringUtils.parseServer(username); //.substring(username.indexOf("@")+1);
    final String user = username.substring(0,username.indexOf("@"));
    final String password = prefs.getString("prefPasswordKey", null);
    int port = Integer.parseInt(prefs.getString("prefServerPortKey", "5222"));
    String service = (prefs.getString("prefServerKey", "")).trim();

    if(service=="") {
      cc = new ConnectionConfiguration(host, port);
    } else {
      cc = new ConnectionConfiguration(service, port, host );
    }


    cc.setTruststorePath(CERT_DIR+CERT_FILE);

    if(!prefs.getBoolean("prefEnableTlsKey", true)) {
      cc.setSecurityMode(SecurityMode.disabled);
    }

    if(!prefs.getBoolean("prefEnableSaslKey", true)) {
      cc.setSASLAuthenticationEnabled(false);
    }


    con = new XMPPConnection(cc);

    Thread task= new Thread() {
      public void run() {
        try {
          con.connect();
        } catch (XMPPException xe) {
          Log.e(TAG, "Could not connect to server: " + xe.getLocalizedMessage());
          return;
        }

        con.addPacketListener(msgListener, new PacketTypeFilter(Message.class));
        con.addConnectionListener(connectionListener);

        try {
          con.login(user, password, prefs.getString("prefResourceKey", "Android"),false);
        } catch (XMPPException xe) {
          Log.e(TAG, "Could not login with given username or password: " + xe.getLocalizedMessage());
          return;
        }

        try {
          roster = con.getRoster();

          Thread.sleep(1000); // let the roster do its job
          getAndUpdateRoster();

          roster.addRosterListener(new RosterListener() {


            public void entriesAdded(Collection<String> addresses) {


            }


            public void entriesDeleted(Collection<String> addresses) {
              // TODO Auto-generated method stub

            }


            public void entriesUpdated(Collection<String> addresses) {
              // TODO Auto-generated method stub

            }


            public void presenceChanged(Presence presence) {

              SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();
              String type = presence.getType().name();
              Mode mode = presence.getMode(); //BUG getMode().name() doesnt throw any error if mode returns null
              //String modeName;

              int typeValue = getType(type);
              int modeValue = getMode(mode);

              //if(type==null) { type = "unavailable"; }
              //if(mode==null) { modeName = null; } else { modeName = mode.name(); }

              String table = Constants.TABLE_BUDDY;
              String whereClause = Constants.TABLE_BUDDY_FIELD_JID + " = '" + StringUtils.parseBareAddress(presence.getFrom()) + "'";
              ContentValues values = new ContentValues();
              values.put(Constants.TABLE_BUDDY_FIELD_PRESENCETYPE, typeValue);
              values.put(Constants.TABLE_BUDDY_FIELD_MSG, presence.getStatus());
              values.put(Constants.TABLE_BUDDY_FIELD_PRESENCEMODE, modeValue);
              db.update(table, values, whereClause, null);

              Intent intent = new Intent("uk.ac.napier.android.androidim.PRESENCE_CHANGED");
              intent.putExtra("jid", StringUtils.parseBareAddress(presence.getFrom()));
              sendBroadcast(intent);

              db.close();

            }

          });

          OfflineMessageManager omm = new OfflineMessageManager(con);

          setPresenceState(state, type, mode);

          try {
            omm.deleteMessages();
          } catch (XMPPException e) {
            Log.e(TAG, "Could not delete offline messages.");
          }


        } catch(Exception e) {
          e.printStackTrace();
        }

        Intent intent = new Intent("uk.ac.napier.android.androidim.PRESENCE_CHANGED");
        sendBroadcast(intent);
        Intent loggedIn = new Intent("uk.ac.napier.android.androidim.LOGGED_IN");
        sendBroadcast(loggedIn);
      }
    };
    task.start();
  }

  public void disconnectFromServer() {
    new Thread() {
      public void run() {
        if(con != null) {
          con.disconnect();
        }
      }
    }.start();

  }

  public void updateRoster() {
    new Thread() {
      public void run() {
        if(con != null && con.getUser()!=null) {
          roster = con.getRoster();
        }
      }
    }.start();

  }



  private void getAndUpdateRoster() {
    SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();

    /* Groups */

    Collection<RosterGroup> rGroups  = roster.getGroups();

    db.delete(Constants.TABLE_GROUP, null, null);

    ContentValues val = new ContentValues();
    for(RosterGroup grp : rGroups) {
      for(RosterEntry entry : grp.getEntries()) {
        val.clear();
        val.put(Constants.TABLE_GROUP_FIELD_GROUP, grp.getName());
        val.put(Constants.TABLE_GROUP_FIELD_JID, entry.getUser());
        db.insert(Constants.TABLE_GROUP, null, val);
      }
    }

    /* Entries */
    Collection<RosterEntry> entries = roster.getEntries();

    db.delete(Constants.TABLE_BUDDY, null, null);

    val.clear();

    Presence presence;

    for(RosterEntry entry : entries) {
      presence = roster.getPresence(entry.getUser());

      val.clear();
      val.put(Constants.TABLE_BUDDY_FIELD_JID, entry.getUser());
      val.put(Constants.TABLE_BUDDY_FIELD_NAME, entry.getName());
      val.put(Constants.TABLE_BUDDY_FIELD_STATUS, entry.getStatus()==null? "unknown" : entry.getStatus().toString());
      val.put(Constants.TABLE_BUDDY_FIELD_PRESENCETYPE, Constants.PRESENCETYPE_NULL);
      val.put(Constants.TABLE_BUDDY_FIELD_PRESENCEMODE, Constants.PRESENCEMODE_NULL);
      val.put(Constants.TABLE_BUDDY_FIELD_MSG, presence.getStatus());
      db.insert(Constants.TABLE_BUDDY, null, val);

    }
    db.close();
  }

  public void setAllContactsOffline() {
    SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();

    String table = Constants.TABLE_BUDDY;

    ContentValues values = new ContentValues();
    values.put(Constants.TABLE_BUDDY_FIELD_PRESENCETYPE, Constants.PRESENCETYPE_NULL);
    values.put(Constants.TABLE_BUDDY_FIELD_PRESENCEMODE, Constants.PRESENCEMODE_NULL);

    db.update(table, values, null, null);
  }

  public boolean isUserLoggedIn() {
    if(con!=null && con.getUser()!=null) {
      return true;
    }
    return false;
  }

  protected void sendMessagePacket(String user, String message) {
    final Message msg = new Message(user,Message.Type.chat);
    msg.setBody(message);
    new Thread() {
      @Override
      public void run() {
        if(con != null && con.getUser()!=null) {
          con.sendPacket(msg);
          logOutgoingMessage(msg.getBody(), msg.getTo());
        }
      }
    }.start();
  }

  public HashMap<String, String> getMessageInfo(Message msg) {
    /* Build the user id, username, resource */

    String userWithRes = msg.getFrom(); //plain JabberID (width resource)
    int slash = userWithRes.lastIndexOf("/"); // select index of the separator of Jabber ID and resource
    String resource; // only the resource name
    String user; // only the Jabber id


    // Check if there was a separator (should always be) and assign the proper values
    if(slash != -1) {
      resource = userWithRes.substring(slash+1);
      user = userWithRes.substring(0,slash);
    } else {
      resource = "unknown";
      user = userWithRes;
    }

    String userName = user; // Alias, if there is one

    RosterEntry re = roster.getEntry(user);
    if((re.getName()) != null) {
      userName = re.getName();
    }

    String body = msg.getBody(); // Message

    java.util.Date date = null; //getTimestamp(packet); currently not working, so ignoring it and use the current time.

    if (date == null) {
      date = new java.util.Date();
    } 

    long time = date.getTime();

    HashMap<String,String> list = new HashMap<String,String>();
    list.put("user", user);
    list.put("resource", resource);
    list.put("username", userName);
    list.put("body", body);
    list.put("time", String.valueOf(time));

    return list;
  }

  // NOT WORKING -> seems to be a bug.
  public static java.util.Date getDelayedStamp(final Packet packet) {

    DelayInformation delay = (DelayInformation) packet.getExtension("jabber:x:delay");

    if (delay != null) {
      return delay.getStamp();
    }
    return null;
  }


  private void queueIncomingMessage(final HashMap<String,String> msgInfo) {
    SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();

    long time = Long.parseLong(msgInfo.get("time"));

    ContentValues val = new ContentValues();

    if(prefs.getBoolean("prefLogMessagesKey", true)) {
      val.put(Constants.TABLE_LOG_FIELD_DATE, new Date(time).toString());
      val.put(Constants.TABLE_LOG_FIELD_TIME, new Time(time).toString());
      val.put(Constants.TABLE_LOG_FIELD_FROM, msgInfo.get("user"));
      val.put(Constants.TABLE_LOG_FIELD_RESOURCE, msgInfo.get("resource"));
      val.put(Constants.TABLE_LOG_FIELD_MSG, msgInfo.get("body").trim());
      db.insert(Constants.TABLE_LOG, null , val);
    }

    val.clear();
    val.put(Constants.TABLE_CONVERSATION_FIELD_DATE, time);
    val.put(Constants.TABLE_CONVERSATION_FIELD_CHAT, msgInfo.get("user"));
    val.put(Constants.TABLE_CONVERSATION_FIELD_FROM, msgInfo.get("user"));
    val.put(Constants.TABLE_CONVERSATION_FIELD_TO, "me");
    val.put(Constants.TABLE_CONVERSATION_FIELD_MSG, msgInfo.get("body").trim());
    val.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 1);
    db.insert(Constants.TABLE_CONVERSATION, null , val);

    db.close();
  }

  private void logOutgoingMessage(final String body, final String to) {

    SQLiteDatabase db = new JabberoidDbConnector(acs).getWritableDatabase();

    String user = con.getUser();
    int slash = user.lastIndexOf("/");
    String resource = user.substring(slash+1);
    user = user.substring(0,slash);

    long time = System.currentTimeMillis();

    ContentValues val = new ContentValues();

    if(prefs.getBoolean("prefLogMessagesKey", true)) {
      val.put(Constants.TABLE_LOG_FIELD_DATE, new Date(time).toString());
      val.put(Constants.TABLE_LOG_FIELD_TIME, new Time(time).toString());
      val.put(Constants.TABLE_LOG_FIELD_FROM, user);
      val.put(Constants.TABLE_LOG_FIELD_RESOURCE, resource);
      val.put(Constants.TABLE_LOG_FIELD_MSG, body.trim());
      db.insert(Constants.TABLE_LOG, null , val);
    }

    val.clear();
    val.put(Constants.TABLE_CONVERSATION_FIELD_DATE, time);
    val.put(Constants.TABLE_CONVERSATION_FIELD_CHAT, to);
    val.put(Constants.TABLE_CONVERSATION_FIELD_FROM, "me");
    val.put(Constants.TABLE_CONVERSATION_FIELD_TO, to);
    val.put(Constants.TABLE_CONVERSATION_FIELD_MSG, body.trim());
    val.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 0);
    db.insert(Constants.TABLE_CONVERSATION, null , val);

    db.close();
  }


  public List<String> getLastStatusMessages() {
    SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();

    String table = Constants.TABLE_STATUSMSG;
    String[] columns = { Constants.TABLE_STATUSMSG_FIELD_MSG };
    String orderBy = Constants.TABLE_STATUSMSG_FIELD_LASTUSED + " DESC LIMIT 10";
    //String where = Constants.TABLE_STATUSMSG_FIELD_MSG + " != ''";

    Cursor result = db.query(table, columns, null, null, null, null, orderBy);

    result.moveToFirst();

    List <String> messages = new ArrayList<String>();
    while(!result.isAfterLast()) {
      messages.add(result.getString(result.getColumnIndex(Constants.TABLE_STATUSMSG_FIELD_MSG)));
      result.moveToNext();
    }

    result.close();
    db.close();

    return messages;
  }

  public void setPresenceState(final String state, final String type, final String mode) {
    new Thread() {
      public void run() {
        if(con.getUser()!=null) {
          Presence presence = new Presence(Presence.Type.valueOf(type));
          if(state != null) presence.setStatus(state);
          presence.setMode(Presence.Mode.valueOf(mode));
          con.sendPacket(presence);
        }
      }
    }.start();
  }


  public void insertAndUseMessage(String message) {
    SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();

    String table = Constants.TABLE_STATUSMSG;
    String[] columns = { Constants.TABLE_STATUSMSG_FIELD_MSG };
    String where = Constants.TABLE_STATUSMSG_FIELD_MSG + " = '" + message + "'";

    Cursor result = db.query(table, columns, where, null, null, null, null);
    ContentValues cv = new ContentValues();

    if(result.getCount() < 1) {
      cv.put(Constants.TABLE_STATUSMSG_FIELD_ACTIVE, false);
      db.update(table, cv, null, null);

      cv.clear();
      cv.put(Constants.TABLE_STATUSMSG_FIELD_MSG, message);
      cv.put(Constants.TABLE_STATUSMSG_FIELD_ACTIVE, true);
      cv.put(Constants.TABLE_STATUSMSG_FIELD_LASTUSED, new java.util.Date().getTime());
      db.insert(table, null, cv);
    } else if(result.getCount() == 1) {
      cv.put(Constants.TABLE_STATUSMSG_FIELD_LASTUSED, new java.util.Date().getTime());
      db.update(table, cv, where, null);
    }

    result.close();
    db.close();

    String mode = getCurrentMode();
    if(mode != null) {	
      setPresenceState(message, "available", mode);
    }
  }

  public String getCurrentMode() {
    String mode = null;

    switch(prefs.getInt("currentSelection", Constants.STATUS_OFFLINE)) {
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
    return mode;
  }

  public int getType(String type) {
    if(type == "available") {
      return Constants.PRESENCETYPE_AVAILABLE;
    } else if ( type == "unavailable") {
      return Constants.PRESENCETYPE_UNAVAILABLE;
    }
    return Constants.PRESENCETYPE_NULL;
  }

  public int getMode(Mode mode) {
    if (mode == null) {
      return Constants.PRESENCEMODE_NULL;
    } else if(mode.name() == "away") {
      return Constants.PRESENCEMODE_AWAY;
    } else if (mode.name() == "xa") {
      return Constants.PRESENCEMODE_XA;
    } else if (mode.name() == "chat") {
      return Constants.PRESENCEMODE_CHAT;
    } else if (mode.name() == "dnd") {
      return Constants.PRESENCEMODE_DND;
    }
    return Constants.PRESENCEMODE_NULL;
  }
}
