package uk.ac.napier.android.jabberoid;

import android.provider.BaseColumns;

public final class Constants implements BaseColumns {
	
	
	
	public static final String DATABASE = "jabber_android.db";
	public static final int DATABASE_VERSION = 1;
	
	public static final String TABLE_CONVERSATION = "openConversations";
	public static final String TABLE_CONVERSATION_FIELD_ID = "_id";
	public static final String TABLE_CONVERSATION_FIELD_DATE = "date";
	public static final String TABLE_CONVERSATION_FIELD_CHAT = "chatWith";
	public static final String TABLE_CONVERSATION_FIELD_FROM = "msgFrom";
	public static final String TABLE_CONVERSATION_FIELD_TO = "msgTo";
	public static final String TABLE_CONVERSATION_FIELD_MSG = "message";
	public static final String TABLE_CONVERSATION_FIELD_NEW = "new";
	
	public static final String TABLE_LOG = "loggedMessages";
	public static final String TABLE_LOG_FIELD_ID = "_id";
	public static final String TABLE_LOG_FIELD_DATE = "date";
	public static final String TABLE_LOG_FIELD_TIME = "time";
	public static final String TABLE_LOG_FIELD_FROM = "msgFrom";
	public static final String TABLE_LOG_FIELD_RESOURCE = "resourceName";
	public static final String TABLE_LOG_FIELD_MSG = "message";
	
	public static final String TABLE_GROUP = "groups";
	public static final String TABLE_GROUP_FIELD_ID = "_id";
	public static final String TABLE_GROUP_FIELD_GROUP = "groupName";
	public static final String TABLE_GROUP_FIELD_JID = "jid";
	
	public static final String TABLE_BUDDY = "buddies";
	public static final String TABLE_BUDDY_FIELD_ID = "_id";
	public static final String TABLE_BUDDY_FIELD_JID = "jid";
	public static final String TABLE_BUDDY_FIELD_NAME = "name";
	public static final String TABLE_BUDDY_FIELD_STATUS = "status";
	public static final String TABLE_BUDDY_FIELD_PRESENCETYPE = "presenceType";
	public static final String TABLE_BUDDY_FIELD_PRESENCEMODE = "presenceMode";
	public static final String TABLE_BUDDY_FIELD_MSG = "message";

	public static final String TABLE_STATUSMSG = "statusMessages";
	public static final String TABLE_STATUSMSG_FIELD_ID = "_id";
	public static final String TABLE_STATUSMSG_FIELD_MSG = "message";
	public static final String TABLE_STATUSMSG_FIELD_ACTIVE = "active";
	public static final String TABLE_STATUSMSG_FIELD_LASTUSED = "lastUsed";
	
	
	public static final int STATUS_ONLINE = 0;
	public static final int STATUS_AWAY = 1;
	public static final int STATUS_E_AWAY = 2;
	public static final int STATUS_DND = 3;
	public static final int STATUS_FREE = 4;
	public static final int STATUS_OFFLINE = 5;
	
	

	public static final int NEW_MESSAGE_NOTIFICATION = 3001;
	
	// Presence Types
	public static final int PRESENCETYPE_AVAILABLE = 0;
	public static final int PRESENCETYPE_UNAVAILABLE = 1;
	public static final int PRESENCETYPE_NULL = 99;
	
	// Presence Modes
	public static final int PRESENCEMODE_NULL = 0;
	public static final int PRESENCEMODE_CHAT = 1;
	public static final int PRESENCEMODE_AWAY = 2;
	public static final int PRESENCEMODE_XA = 3;
	public static final int PRESENCEMODE_DND = 4;

	
}
