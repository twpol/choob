import org.uwcs.choob.*;
import org.uwcs.choob.modules.*;
import org.uwcs.choob.support.*;
import org.uwcs.choob.support.events.*;
import java.util.*;
import java.util.regex.*;

public class FactoidObject 
{
	public FactoidObject()
	{
	}

	public FactoidObject( String subject, String info )
	{
		this.subject = subject;
		this.info = info;
	}

	public int id;
	public String subject;
	public String info;
}

public class Factoids
{
	public String filterFactoidsRegex = "(\\w{4,})\\s+(?:is|was)\\s+(.{4,})";

	public void filterFactoids( Message msg, Modules mods, IRCInterface irc ) throws ChoobException
	{
		Matcher factoidMatcher = (Pattern.compile( filterFactoidsRegex )).matcher( msg.getMessage() );

		factoidMatcher.find();

		FactoidObject fact = new FactoidObject( factoidMatcher.group(1).toLowerCase() , factoidMatcher.group(2) );

		mods.odb.save( fact );
	}

	
	public void commandWhatIs( Message msg, Modules mods, IRCInterface irc ) throws ChoobException
	{
		List<String> params = mods.util.getParams( msg, 2 );

		List facts = mods.odb.retrieve( FactoidObject.class , "SORT RANDOM LIMIT (1) WHERE subject = \"" + params.get(1).toLowerCase() + "\"");

		if( facts.size() > 0 )
		{
			FactoidObject fact = (FactoidObject)facts.get(0);

			irc.sendContextReply( msg, params.get(1) + " is " + fact.info );
		}
		else
		{
			irc.sendContextReply( msg, "I don't know anything about " + params.get(1) + "! Ask your mum.");
		}
	}
}