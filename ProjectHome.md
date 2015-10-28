1/16/2010 - New code coming soon!

The aim of this project is to develop an XMPP client for Android based on Smack. It was originally meant to be a project for a final paper. After finishing the dissertation project, I released the source code as open source. It is far from being complete, it is just a prototype yet.

26/01/2009: Code (with some Eclipse project files) is uploaded.

At present, there is no active development process. If you are interested in participating (development, documentation or organisation) feel free to contact one of the project owners or ask in the Google Group

> http://groups.google.com/group/jabberoid-discuss

for a membership.

What is currently working?

  * Preferences: Account, Priority, Resource name, TLS, Server location override, other points not implemented yet
  * Start Screen: Change status (Online, Away ...), change status message, List contacts ordered by status and group, start (or reopen) a chat by selecting the contact
  * Chat: Getting notification on incoming messages, if there are only messages from one contact, open directly the chat windows. Otherwise get a list of all open chats. Messages cannot be sent if account is offline. Dates are shown in format h:mm or, if the last message received at least a day in the past MMM DD, YYYY h:mm:ss.
  * Service: Handles the Jabber connection; allows to close the screen of Jabberoid without losing the connection.

Known Issues:
  * Application crashes if the user tries to go online without setting up an account before
  * certainly more