package uk.ac.napier.android.jabberoid.tools;

import java.util.EventObject;


public class PressedEvent extends EventObject {

	private static final long serialVersionUID = 999999999999999L;

	public PressedEvent(Object source) {
		super(source);
	}
}