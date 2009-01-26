package uk.ac.napier.android.jabberoid.tools;

import uk.ac.napier.android.jabberoid.Constants;
import uk.ac.napier.android.jabberoid.data.JabberoidDbConnector;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ContactEntry {
	private Context context;
	private String jid;
	private String name;
	private String status;
	private int presenceType;
	private int presenceMode;
	private String presenceMsg;
	
	public ContactEntry(Context context, String jid) {
		this(context, jid, null, null, Constants.PRESENCETYPE_NULL, Constants.PRESENCEMODE_NULL, null);
	}
	
	public ContactEntry(Context context, String jid, String name, String status, int presenceType, int presenceMode, String presenceMsg) {
		this.context = context;
		this.jid = jid;
		this.name = name;
		this.status = status;
		this.presenceType = presenceType;
		this.presenceMode = presenceMode;
		this.presenceMsg = presenceMsg;
		updateFields();
	}
	
	public void updateFields() {
		SQLiteDatabase db = new JabberoidDbConnector(context).getReadableDatabase();
		
		String table = Constants.TABLE_BUDDY;
		String[] columns = { Constants.TABLE_BUDDY_FIELD_NAME, 
							 Constants.TABLE_BUDDY_FIELD_STATUS,
							 Constants.TABLE_BUDDY_FIELD_PRESENCETYPE,
							 Constants.TABLE_BUDDY_FIELD_PRESENCEMODE,
							 Constants.TABLE_BUDDY_FIELD_MSG};
		String selection = Constants.TABLE_BUDDY_FIELD_JID + " = '" + jid + "'";
		String[] selectionArgs = null;
		String groupBy = null;
		String having = null;
		String orderBy = null;
		
		Cursor result = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		result.moveToFirst();
		name = result.getString(result.getColumnIndex(Constants.TABLE_BUDDY_FIELD_NAME));
		status = result.getString(result.getColumnIndex(Constants.TABLE_BUDDY_FIELD_STATUS));
		presenceType = result.getInt(result.getColumnIndex(Constants.TABLE_BUDDY_FIELD_PRESENCETYPE));
		presenceMode = result.getInt(result.getColumnIndex(Constants.TABLE_BUDDY_FIELD_PRESENCEMODE));
		presenceMsg = result.getString(result.getColumnIndex(Constants.TABLE_BUDDY_FIELD_MSG));
		
		result.close();
		db.close();
	}
	
	
	@Override
	public String toString() {
		if(name == null) {
			return jid;
		}
			return name;
	}
	
	public String getName() {
		return toString();
	}
	
	public String getJid() {
		return jid;
	}
	
	public int getPresenceMode() {
		return this.presenceMode;
	}
	
	public String getPresenceMessage() {
		return this.presenceMsg;
	}
	
	public int getPresenceType() {
		return this.presenceType;
	}
	
	public String getStatus() {
		return this.status;
	}

}
