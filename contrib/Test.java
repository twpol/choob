import org.uwcs.choob.*;
import org.uwcs.choob.modules.*;
import org.uwcs.choob.support.*;
import org.uwcs.choob.support.events.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;


public class Test
{
	public void commandSecurity( Message con, Modules mods, IRCInterface irc )
	{
		String priv = mods.util.getParamString( con );

		if (priv.trim().equals(""))
		{
			irc.sendContextReply(con, "You have to specify an argument.");
			return;
		}

		if ( mods.security.hasNickPerm( new ChoobPermission(priv), con.getNick() ) )
			irc.sendContextReply(con, "You do indeed have " + priv + "!" );
		else
			irc.sendContextReply(con, "You don't have " + priv + "!" );
	}

	public void commandJoin( Message con, Modules mods, IRCInterface irc ) throws ChoobException
	{
		irc.join(mods.util.getParamString(con));
		irc.sendContextReply(con, "Okay!");
	}

	public void commandPart( Message con, Modules mods, IRCInterface irc ) throws ChoobException
	{
		irc.part(mods.util.getParamString(con));
		irc.sendContextReply(con, "Okay!");
	}


	public void commandPirate( Message con, Modules mods, IRCInterface irc )
	{
		irc.sendContextReply(con, "Yarr!");
	}

	public void commandPiratey( Message con, Modules mods, IRCInterface irc )
	{
		irc.sendContextReply(con, "(:;test.piratey:)");
	}

	public void commandInMy( Message con, Modules mods, IRCInterface irc )
	{
		irc.sendContextMessage(con, "..Pants!");
	}

	public void commandExit( Message con, Modules mods, IRCInterface irc )
	{
		List<String> params = mods.util.getParams( con );
		if (params.size() > 1) {
			System.exit(new Integer(params.get(1)));
		} else {
			System.exit(1);
		}
	}

	public void commandRestart( Message con, Modules mods, IRCInterface irc )
	{
		System.exit(2);
	}

	// Define the regex for the KarmaPlus filter.
	public String filterFauxRegex = "Faux sucks";

	public void filterFaux( Message con, Modules modules, IRCInterface irc )
	{
		irc.sendContextMessage( con, "No, I disagree, " + con.getNick() + " is the one that is the suck.");
	}

	public String filterBouncyRegex = "^bouncy bouncy";

	public void filterBouncy( Message con, Modules modules, IRCInterface irc )
	{
		irc.sendContextReply( con, "Ooh, yes please.");
	}

	public void onJoin( ChannelJoin ev, Modules mods, IRCInterface irc )
	{
		if (ev.getLogin().equals("Choob"))
			return;

		String s=null;
		try
		{
			s=(String)mods.plugin.callAPI( "quote", "singlelinequote", ev.getNick(), ev.getContext());
		}
		catch (ChoobException e)
		{}

		if (s==null)
			irc.sendContextMessage( ev, "Hello, " + ev.getNick() + "!");
		else
			irc.sendContextMessage( ev, "Hello, " + ev.getNick() + ": \"" + s + "\"!");
	}

	public void onPart( ChannelPart ev, Modules mod, IRCInterface irc )
	{
		irc.sendContextMessage( ev, "Bye, " + ev.getNick() + "!");
	}

	public void commandAPI ( Message mes, Modules mods, IRCInterface irc ) throws ChoobException
	{
		List<String> params = mods.util.getParams( mes );
		irc.sendContextReply(mes, mods.plugin.callAPI( params.get(1), params.get(2), params.get(3) ).toString());
	}

	public void commandGeneric ( Message mes, Modules mods, IRCInterface irc ) throws ChoobException
	{
		List<String> params = mods.util.getParams( mes );
		if (params.size() == 5)
			irc.sendContextReply(mes, mods.plugin.callGeneric( params.get(1), params.get(2), params.get(3), params.get(4) ).toString());
		else
			irc.sendContextReply(mes, mods.plugin.callGeneric( params.get(1), params.get(2), params.get(3) ).toString());
	}

	public void commandWait (Message mes, Modules mods, IRCInterface irc)
	{
		Object test = new Object();
		synchronized(test)
		{
			try
			{
				test.wait(3000);
			}
			catch (InterruptedException e)
			{
				irc.sendContextReply(mes, "Interrupted!");
			}
			irc.sendContextReply(mes, "OK, waited.");
		}
	}

/*	public void onPluginLoaded (PluginLoaded mes, Modules mods, IRCInterface irc)
	{
		irc.sendMessage("#bots", "Yay! Plugin loaded! Name is: " + mes.getPluginName());
	}

	public void onPluginUnLoaded (PluginUnLoaded mes, Modules mods, IRCInterface irc)
	{
		irc.sendMessage("#bots", "Boo! Plugin unloaded! Name is: " + mes.getPluginName());
	}

	public void onPluginReLoaded (PluginReLoaded mes, Modules mods, IRCInterface irc)
	{
		irc.sendMessage("#bots", "Boo! Plugin reloaded! Name is: " + mes.getPluginName());
	}*/

	public void commandWhoreApi ( Message mes, Modules mods, IRCInterface irc ) throws ChoobException
	{
		for(int i=0; i < 100000; i++)
			try { mods.plugin.callAPI("Test", "Whore", "Iteration" + i, mes); } catch ( ChoobNoSuchCallException e ) { }
		irc.sendContextReply(mes, "OK, API whored!");
	}

	public void apiWhore(String text, Message mes)
	{
		//System.out.println("Whorage called by " + mes.getNick() + ": " + text);
	}
}
