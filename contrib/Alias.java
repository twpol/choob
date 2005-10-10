import org.uwcs.choob.*;
import org.uwcs.choob.modules.*;
import org.uwcs.choob.support.*;
import org.uwcs.choob.support.events.*;
import java.util.*;
import java.util.regex.*;

public class AliasObject
{
	public AliasObject(String name, String converted) { this.name=name; this.converted=converted; this.id=0; }
	public AliasObject() {}
	public int id;
	public String name;
	public String converted;
}

public class Alias
{
	private final String validator="[^A-Za-z_0-9]+";

	private Modules mods;
	private IRCInterface irc;
	public Alias(Modules mods, IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
	}

	public void commandAdd( Message mes ) throws ChoobException
	{
		String propend=".";
		List<String>s=mods.util.getParams(mes, 2);

		if (s.size() <= 2)
		{
			irc.sendContextReply(mes, "Syntax: add <aliasname> <string>.");
			return;
		}

		String alias=s.get(1).replaceAll(validator,"").toLowerCase();
		String conv=s.get(2);

		List<AliasObject> existing = mods.odb.retrieve( AliasObject.class, "WHERE name='" + alias + "'" );

		if (existing.size() != 0)
		{
			AliasObject aO = existing.get(0);

			propend=", was " + aO.converted + ".";

			aO.converted=conv;

			mods.odb.update(aO);
		}
		else
			mods.odb.save(new AliasObject(alias, conv));

		irc.sendContextReply(mes, "Aliased '" + alias + "' to '" + conv + "'" + propend);
	}

	public void commandList( Message mes ) throws ChoobException
	{
		List<AliasObject> existing = mods.odb.retrieve( AliasObject.class, "WHERE 1" );
		String list="Alias list: ";
		if (existing.size()==0)
			irc.sendContextReply(mes, "No aliases.");
		else
		{
			for (int i=0; i<existing.size(); i++)
				list+=existing.get(i).name + ": " + existing.get(i).converted + ", ";
			irc.sendContextReply(mes, list.substring(0,list.length()-2) + ".");
		}
	}

	public void commandShow( Message mes ) throws ChoobException
	{
		List<String>s=mods.util.getParams(mes, 2);
		String alias = s.get(1).replaceAll(validator,"").toLowerCase();

		List<AliasObject> existing = mods.odb.retrieve( AliasObject.class, "WHERE name='" + alias + "'" );

		if (existing.size()==0)
			irc.sendContextReply(mes, "Alias not found.");
		else
		{
			AliasObject aO=existing.get(0);
			irc.sendContextReply(mes, "'" + aO.name + "' is aliased to '" + aO.converted + "'.");
		}
	}

	public void commandDelete( Message mes ) throws ChoobException
	{
		List<String>s=mods.util.getParams(mes, 2);
		String alias = s.get(1).replaceAll(validator,"").toLowerCase();

		List<AliasObject> existing = mods.odb.retrieve( AliasObject.class, "WHERE name='" + alias + "'" );

		if (existing.size()==0)
			irc.sendContextReply(mes, "Alias not found.");
		else
		{
			AliasObject aO=existing.get(0);
			mods.odb.delete(aO);
			irc.sendContextReply(mes, "Deleted '" + aO.name + "', was aliased to '" + aO.converted + "'.");
		}
	}

	public void onPrivateMessage( Message mes ) throws ChoobException
	{
		onMessage( mes );
	}

	// Muhahahahahahahahaha --bucko
	public void onMessage( Message mes ) throws ChoobException
	{
		String text = mes.getMessage();

		Matcher matcher = Pattern.compile(irc.getTriggerRegex()).matcher(text);
		int offset = 0;

		// Make sure this is actually a command...
		if (matcher.find())
		{
			offset = matcher.end();
		}
		else if (!(mes instanceof PrivateEvent))
		{
			return;
		}

		// Stop recursion
		if (mes.getSynthLevel() > 1) {
			irc.sendContextReply(mes, "Synthetic event recursion detected. Stopping.");
			return;
		}

		// Text is everything up to the next space...
		int cmdEnd = text.indexOf(' ', offset);
		String cmdParams;
		if (cmdEnd == -1)
		{
			cmdEnd = text.length();
			cmdParams = "";
		}
		else
		{
			cmdParams = text.substring(cmdEnd);
		}

		int dotIndex = text.indexOf('.', offset);
		// Real command, not an alias...
		if (dotIndex != -1 && dotIndex < cmdEnd)
			return;

		String command = text.substring(offset, cmdEnd);
		List all = mods.odb.retrieve( AliasObject.class, "WHERE name=\"" + command.replaceAll("\"", "\\\\\"") + "\"" );

		if (all.size() == 0)
		{
			// Consider an error here...
			return;
		}

		AliasObject alias = (AliasObject)all.get(0);

		command = irc.getTrigger() + alias.converted + cmdParams;

		Message newMes = (Message)mes.cloneEvent( command );

		System.out.println("Converted " + text + " -> " + command);

		mods.synthetic.doSyntheticMessage( newMes );
	}
}
