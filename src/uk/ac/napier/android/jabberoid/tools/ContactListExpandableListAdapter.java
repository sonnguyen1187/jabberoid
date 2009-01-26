package uk.ac.napier.android.jabberoid.tools;

import java.util.List;
import java.util.Map;

import uk.ac.napier.android.jabberoid.Constants;
import uk.ac.napier.android.jabberoid.R;
import uk.ac.napier.android.jabberoid.tools.ContactEntry;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class ContactListExpandableListAdapter extends SimpleExpandableListAdapter {
	
	private Activity context;
	List<? extends List<? extends Map<String, ?>>> hm;
	private String[] childFrom;
	
	public ContactListExpandableListAdapter(Context context,
			List<? extends Map<String, ?>> groupData,
			int expandedGroupLayout,
			int collapsedGroupLayout, 
			String[] groupFrom, 
			int[] groupTo,
			List<? extends List<? extends Map<String, ?>>> childData,
			int childLayout, 
			int lastChildLayout, 
			String[] childFrom,
			int[] childTo) {
		
		super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom,
				groupTo, childData, childLayout, lastChildLayout, childFrom, childTo);
		this.context = (Activity) context;
		this.hm = childData;
		this.childFrom = childFrom;
	}
	
	public ContactListExpandableListAdapter(Context context,
			List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
			int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
			List<? extends List<? extends Map<String, ?>>> childData,
			int childLayout, String[] childFrom, int[] childTo) {
		super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom,
				groupTo, childData, childLayout, childFrom, childTo);
		this.context = (Activity) context;
		this.hm = childData;
		this.childFrom = childFrom;
	}

	public ContactListExpandableListAdapter(Context context,
			List<? extends Map<String, ?>> groupData, int groupLayout,
			String[] groupFrom, int[] groupTo,
			List<? extends List<? extends Map<String, ?>>> childData,
			int childLayout, String[] childFrom, int[] childTo) {
		super(context, groupData, groupLayout, groupFrom, groupTo, childData,
				childLayout, childFrom, childTo);
		this.context = (Activity) context;
		this.hm = childData;
		this.childFrom = childFrom;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View row = convertView;
		
		if(row==null) {		
			LayoutInflater inflater = context.getLayoutInflater();
			row = inflater.inflate(R.layout.contactlist_entry, null, false);
		}
		
		ContactEntry ce = (ContactEntry)hm.get(groupPosition).get(childPosition).get("contactJid");
		ce.updateFields();
		TextView label = (TextView)row.findViewById(R.id.contactlist_item);
		TextView status = (TextView)row.findViewById(R.id.contactlist_status);
		
		label.setText(ce.toString());
		String message = ce.getPresenceMessage();
		if(message == null) {
			status.setVisibility(View.GONE);
		} else {
			status.setText(message);
		}
		
		ImageView icon = (ImageView)row.findViewById(R.id.contactlist_pic);
		
		int presenceMode = ce.getPresenceMode();
		int presenceType = ce.getPresenceType();
		
		
		
		
		if(presenceType == Constants.PRESENCETYPE_AVAILABLE) {
			if(presenceMode == Constants.PRESENCEMODE_AWAY) {
				icon.setImageResource(R.drawable.presence_away);
			} else if (presenceMode == Constants.PRESENCEMODE_XA) {
				icon.setImageResource(R.drawable.kopete_all_away);
			} else if (presenceMode == Constants.PRESENCEMODE_DND) {
				icon.setImageResource(R.drawable.presence_unknown);
			} else if (presenceMode == Constants.PRESENCEMODE_CHAT) {
				icon.setImageResource(R.drawable.metacontact_online);
			} else {
				icon.setImageResource(R.drawable.presence_online);
			}
		} else {
			icon.setImageResource(R.drawable.kopete_offline);
		}
		

		return row;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		return super.getGroupView(groupPosition, isExpanded, convertView, parent);
	}
}
