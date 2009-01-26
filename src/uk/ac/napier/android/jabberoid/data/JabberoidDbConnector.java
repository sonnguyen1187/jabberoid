package uk.ac.napier.android.jabberoid.data;

import uk.ac.napier.android.jabberoid.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class JabberoidDbConnector extends SQLiteOpenHelper {

  private static final String TAG = "AimDbConnector";

  public JabberoidDbConnector(Context context) {
    super(context, Constants.DATABASE, null, Constants.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + Constants.TABLE_CONVERSATION + " ("
        + Constants.TABLE_CONVERSATION_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + Constants.TABLE_CONVERSATION_FIELD_DATE + " INT,"
        + Constants.TABLE_CONVERSATION_FIELD_CHAT + " TEXT,"
        + Constants.TABLE_CONVERSATION_FIELD_FROM + " TEXT,"
        + Constants.TABLE_CONVERSATION_FIELD_TO + " TEXT,"
        + Constants.TABLE_CONVERSATION_FIELD_MSG + " TEXT,"
        + Constants.TABLE_CONVERSATION_FIELD_NEW + " INT);"
    );
    db.execSQL("CREATE TABLE " + Constants.TABLE_LOG + " ("
        + Constants.TABLE_LOG_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + Constants.TABLE_LOG_FIELD_DATE + " INT,"
        + Constants.TABLE_LOG_FIELD_TIME + " TIME,"
        + Constants.TABLE_LOG_FIELD_FROM + " TEXT,"
        + Constants.TABLE_LOG_FIELD_RESOURCE + " TEXT,"
        + Constants.TABLE_LOG_FIELD_MSG + " TEXT);"
    );
    db.execSQL("CREATE TABLE " + Constants.TABLE_BUDDY + " ("
        + Constants.TABLE_BUDDY_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + Constants.TABLE_BUDDY_FIELD_JID + " TEXT,"
        + Constants.TABLE_BUDDY_FIELD_NAME + " TEXT,"
        + Constants.TABLE_BUDDY_FIELD_STATUS + " TEXT,"
        + Constants.TABLE_BUDDY_FIELD_PRESENCETYPE + " INTEGER,"
        + Constants.TABLE_BUDDY_FIELD_PRESENCEMODE + " INTEGERT,"
        + Constants.TABLE_BUDDY_FIELD_MSG + " TEXT);"
    );
    db.execSQL("CREATE TABLE " + Constants.TABLE_GROUP + " ("
        + Constants.TABLE_GROUP_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + Constants.TABLE_GROUP_FIELD_GROUP + " TEXT,"
        + Constants.TABLE_GROUP_FIELD_JID + " TEXT);"
    );

    db.execSQL("CREATE TABLE " + Constants.TABLE_STATUSMSG + " ("
        + Constants.TABLE_STATUSMSG_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + Constants.TABLE_STATUSMSG_FIELD_MSG + " TEXT,"
        + Constants.TABLE_STATUSMSG_FIELD_ACTIVE + " TEXT,"
        + Constants.TABLE_STATUSMSG_FIELD_LASTUSED + " INT);"
    );
    ContentValues cv = new ContentValues();
    cv.put(Constants.TABLE_STATUSMSG_FIELD_MSG, "");
    cv.put(Constants.TABLE_STATUSMSG_FIELD_ACTIVE, true);
    db.insert(Constants.TABLE_STATUSMSG, null, cv);

    Log.i(TAG, "Tables created");

  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // do nothing
    Log.w(TAG, "onUpgrade called, but nothing done.");
  }
}
