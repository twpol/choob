import org.uwcs.choob.*;
import org.uwcs.choob.modules.*;
import org.uwcs.choob.support.*;
import org.uwcs.choob.support.events.*;
import bsh.*;
import java.util.*;

/**
 * Choob nickserv checker
 * 
 * @author bucko
 * 
 * Anyone who needs further docs for this module has some serious Java issues.
 * :)
 */

public class NickServ
{
	private Map<String,ResultObj> nickChecks;

	public NickServ(Modules modules)
	{
		nickChecks = new HashMap<String,ResultObj>();
	}

	public void destroy(Modules modules)
	{
		synchronized(nickChecks)
		{
			Iterator<String> nicks = nickChecks.keySet().iterator();
			while(nicks.hasNext()) {
				ResultObj result = splatNickCheck(nicks.next());
				synchronized(result)
				{
					result.notifyAll();
				}
			}
		}
	}

	public void commandNickServ( Message con, Modules modules, IRCInterface irc ) throws ChoobException
	{
		String nick = modules.util.getParamString( con );
		int check1 = (Integer)modules.plugin.callAPI("NickServ", "NickServCheck",irc, nick);
		if ( check1 > 0 )
		{
			irc.sendContextReply(con, nick + " is authed (" + check1 + ")!");
		}
		else
		{
			irc.sendContextReply(con, nick + " is not authed!");
		}
	}

	public int apiNickServCheck( IRCInterface irc, String nick )
	{
		ResultObj result = getNewNickCheck(irc, nick);

		synchronized(result)
		{
			try
			{
				result.wait(30000); // Make sure if NickServ breaks we're not screwed
			}
			catch (InterruptedException e)
			{
				// Ooops, timeout
				return -1;
			}
		}
		int status = result.getResult();
		return status;
	}

	private ResultObj getNewNickCheck( IRCInterface irc, String nick )
	{
		ResultObj result;
		synchronized(nickChecks)
		{
			result = nickChecks.get( nick );
			if ( result == null )
			{
				// Not already waiting on this one
				result = new ResultObj();
				irc.sendMessage("NickServ", "STATUS " + nick);
				nickChecks.put( nick, result );
			} 
		}
		return result;
	}

	private ResultObj splatNickCheck( String nick )
	{
		ResultObj result;
		synchronized(nickChecks)
		{
			result = (ResultObj)nickChecks.get( nick );
			if ( result == null )
				// !!! This should never really happen
				return null;

			// Clear status so next time we ask NickServ again
			nickChecks.remove( nick );
		}
		return result;
	}

	public void onPrivateNotice( Message mes, Modules modules, IRCInterface irc )
	{
		if ( ! (mes instanceof PrivateNotice) )
			return; // Only interested in private notices

		if ( ! mes.getNick().equals( "NickServ" ) )
			return; // Not from NickServ --> also don't care

		List params = modules.util.getParams( mes );

		if ( ! ((String)params.get(0)).equals("STATUS") )
			return; // Wrong type of message!

		String nick = (String)params.get(1);
		int status = Integer.valueOf((String)params.get(2));

		ResultObj result = splatNickCheck( nick );
		if ( result == null )
			return; // XXX

		synchronized(result)
		{
			result.setResult( status );

			result.notifyAll();
		}
	}

	// Holds the NickServ result
	private class ResultObj
	{
		int result;
		public ResultObj()
		{
			result = -1;
		}
		public void setResult( int result )
		{
			this.result = result;
		}
		public int getResult()
		{
			return result;
		}
	}
}
