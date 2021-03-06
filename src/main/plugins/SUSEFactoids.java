import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.ChoobException;
import uk.co.uwcs.choob.support.ChoobNoSuchCallException;
import uk.co.uwcs.choob.support.ChoobPermission;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.Message;
import uk.co.uwcs.choob.support.events.PrivateEvent;


class ChannelOptions
{
	public int id;
	public String channelName;
	public String lang;

	public ChannelOptions()
	{
		//This space intentionally left blank
	}

	public ChannelOptions(final String channelName, final String lang)
	{
		this.channelName = channelName;
		this.lang = lang;
	}

	public String getChannelName()
	{
		return channelName;
	}

	public void setChannelName(final String channelName)
	{
		this.channelName = channelName;
	}

	public String getLang()
	{
		return lang;
	}

	public void setLang(final String lang)
	{
		this.lang = lang;
	}

}

class Fact
{
	public int id;
	public String subject;

	public String confirmedFact;
	public String author;
	public String created;
	public String lang;
	public String linkedFactoid;
	public String lastRequestedBy;
	public String lastRequestedAt;
	public int numberOfRequests;

	public Fact()
	{
	}

	public Fact(final String subject)
	{
		this.subject = subject;
		lastRequestedBy = "";
		numberOfRequests = 0;
		lang = "";
	}

	public void addConfirmedFact(final String info, final String author)
	{
		confirmedFact = info;

		this.author = author;
		this.lastRequestedBy = author;

		final Date now = new Date();
		final java.text.SimpleDateFormat niceDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		created = niceDate.format(now);
		lastRequestedAt = created;

	}

	public String whatis(final String requester)
	{
		numberOfRequests++;

		lastRequestedBy = requester;

		final Date now = new Date();
		final java.text.SimpleDateFormat niceDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		lastRequestedAt = niceDate.format(now);


		return confirmedFact;
	}

	public String whatis()
	{
		return confirmedFact;
	}

	public void forget()
	{
		confirmedFact = null;
	}
}

public class SUSEFactoids
{

	private final Modules mods;
	private final IRCInterface irc;

	private final Set<String> locks = new HashSet<String>();

	public SUSEFactoids(final Modules mods, final IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
		triggerPattern = Pattern.compile(irc.getTriggerRegex(), Pattern.CASE_INSENSITIVE);
	}

	private void setChannelLanguage(final String channelName, final String language)
	{
		final List<ChannelOptions> opts = mods.odb.retrieve(ChannelOptions.class, "WHERE channelName = \"" + channelName + "\"");
		if (opts.size() == 0)
		{
			try
			{
				final ChannelOptions option = new ChannelOptions(channelName,language);
				mods.odb.save(option);
			} catch (final IllegalStateException e)
			{
				//This space intentionally left blank.
			}
		} else
		{
			try
			{
				final ChannelOptions option = opts.get(0);
				option.setChannelName(channelName);
				option.setLang(language);
				mods.odb.update(option);
			} catch (final IllegalStateException e)
			{
				//This space intentionally left blank.
			}
		}
	}
	private static final String DEFAULT_LANG = "en_GB";
	private String getChannelLanguage(final String channelName)
	{
		final List<ChannelOptions> opts = mods.odb.retrieve(ChannelOptions.class, "WHERE channelName = \"" + channelName + "\"");

		if (opts.size() != 0)
		{
			try
			{
				final ChannelOptions option = opts.get(0);
				return option.getLang();
			} catch (final IllegalStateException e)
			{
				//This space intentionally left blank.
			}
		}
		return DEFAULT_LANG;
	}

	public String[] helpCommandSetChannelLanguage =
	{
		"Set the preferred factoid language for a channel",
		"<ChannelName> is The name of the channel",
		"<Language> is the language"
	};
	public void commandSetChannelLanguage(final Message mes) throws ChoobException
	{
		mods.security.checkNickPerm(new ChoobPermission("plugins.factoids.setchannellanguage"), mes);
		final List<String> params = mods.util.getParams(mes, 3);
		if (params.size() != 3)
		{
			irc.sendContextMessage(mes,"Usage: <ChannelName> <Language>");
			return;
		}

		setChannelLanguage(params.get(1), params.get(2));
		irc.sendContextMessage(mes, "Ok, set " + params.get(1) + " language to " + params.get(2));
	}

	public String[] helpCommandGetChannelLanguage =
	{
		"Set the preferred factoid language for a channel",
		"<ChannelName> is The name of the channel",
		"<Language> is the language"
	};
	public void commandGetChannelLanguage(final Message mes) throws ChoobException
	{

		final List<String> params = mods.util.getParams(mes, 2);
		if (params.size() != 2)
		{
			irc.sendContextMessage(mes,"Usage: <ChannelName>");
			return;
		}
		irc.sendContextMessage(mes, "Language for " + params.get(1) + " is " + getChannelLanguage(params.get(1)));
	}

	public String[] helpCommandRemember =
	{
		"Make the bot remember a specified factoid",
		"<Factoid> is <FactoidValue>",
		"<Factoid> is the name of the factoid to add, it may be a multiple word phrase",
		"<FactoidValue> is the content of this factoid"
	};
	final Pattern rememberPattern = Pattern.compile("(.{2,}?)\\s+(?:is|was)\\s+(.{3,})");
	public void commandRemember(final Message mes) throws ChoobException
	{
		mods.security.checkNickPerm(new ChoobPermission("plugins.factoids.remember"), mes);

		final List<String> params = mods.util.getParams(mes, 1);
		final Matcher factoidMatcher = rememberPattern.matcher(params.get(1));

		if (factoidMatcher.find())
		{

			final String factName = factoidMatcher.group(1).toLowerCase();

			final String factValue = factoidMatcher.group(2);

			irc.sendContextReply(mes, "Remembering " + factName + "..");

			rememberFact(factName, factValue, mes.getNick(),mes.getContext());
		} else
		{
			irc.sendContextReply(mes, "I don't understand!");
		}
	}

	public String[] helpCommandLink =
	{
		"Make the bot link a specified factoid to another",
		"<Factoid1> <Factoid2>",
		"<Factoid1> is the name of the factoid to link",
		"<Factoid2> is the factoid to link to"
	};
	final Pattern linkPattern = Pattern.compile("(.{2,}?)\\s+(?:to)\\s+(.{3,})");
	public void commandLink(final Message mes) throws ChoobException
	{
		mods.security.checkNickPerm(new ChoobPermission("plugins.factoids.remember"), mes);

		final List<String> params = mods.util.getParams(mes, 1);
		final Matcher linkMatcher = linkPattern.matcher(params.get(1));
		if (linkMatcher.find())
		{
			final String fact1 = linkMatcher.group(1).toLowerCase();
			final String fact2 = linkMatcher.group(2).toLowerCase();
			try
			{
				linkFact(fact1, fact2, mes.getContext());
				irc.sendContextReply(mes, "Linked " + fact1 + " to " + fact2);
			} catch (final FactNotFoundException e)
			{
				irc.sendContextReply(mes, "Could not find the target fact, ensure it exists.");
				return;
			}
		} else
		{
			irc.sendContextReply(mes, "You must specify source and target factoid names");
			return;
		}
	}


	public String[] helpCommandUnLink =
	{
		"Make the bot unlike a specified factoid from another",
		"<Factoid1>",
		"<Factoid1> is the name of the factoid to unlink"
	};
	public void commandUnLink(final Message mes) throws ChoobException
	{
		mods.security.checkNickPerm(new ChoobPermission("plugins.factoids.remember"), mes);


		final List<String> params = mods.util.getParams(mes,1);
		if (params.size() != 2)
		{
			irc.sendContextReply(mes, "You must specify factoid name");
			return;
		}
		try
		{
			unlinkFact(params.get(1), mes.getContext());
			irc.sendContextReply(mes, "UnLinked " + params.get(1));
		} catch (final FactNotFoundException e)
		{
			irc.sendContextReply(mes, "Could not find the fact to unlink, ensure it exists.");
			return;
		}
	}

	private void rememberFact(final String subject, final String value, final String nick, final String context)
	{
		Fact fact;
		try
		{
			fact = getFact(subject,context);
			//remove the <reply> and <action> tags as we ignore those now.

			//Only update this fact if it is for our context, prevent people accidentally clobbering other channels' factoids.
			if (fact.lang.equals(getChannelLanguage(context)))
			{
				fact.addConfirmedFact(value.replaceAll("<.*?>",""), nick);
				mods.odb.update(fact);
			} else throw new FactNotFoundException();
		} catch (final FactNotFoundException e)
		{
			fact = new Fact(subject);
			fact.addConfirmedFact(value.replaceAll("<.*?>",""), nick);
			fact.lang = getChannelLanguage(context);
			mods.odb.save(fact);
		}
	}

	private void linkFact(final String subject, final String targetSubject, final String context) throws FactNotFoundException
	{
		getFact(targetSubject, context);
		Fact fact;
		try
		{
			fact = getExactFact(subject, context);
			fact.linkedFactoid = targetSubject;
			mods.odb.update(fact);
		} catch (final FactNotFoundException e)
		{
			fact = new Fact(subject);
			fact.addConfirmedFact("This factoid used to be linked to " + targetSubject, "unknown");
			fact.lang = getChannelLanguage(context);
			fact.linkedFactoid = targetSubject;
			mods.odb.save(fact);
		}
	}

	private void unlinkFact(final String subject,  final String context) throws FactNotFoundException
	{
		final Fact fact = getExactFact(subject, context);
		fact.linkedFactoid = "";
		mods.odb.update(fact);
	}


	public void interval(final Object param)
	{
		synchronized(locks)
		{
			locks.remove(param);
		}
	}

	class FactNotFoundException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = 2747650133533345970L;
	};

	private static final int MAX_DEPTH = 5;
	/**
	 * Obtains fact with specified subject, for specified channel context.
	 * Will follow linked factoids up to a maximum depth of 5 to avoid loops.
	 *
	 * @param subject	The factoid key.
	 * @param context	The channel, to allow localised factoids
	 * @return	The factoid
	 * @throws SUSEFactoids.FactNotFoundException
	 */
	private Fact getFact(final String subject,final String context) throws FactNotFoundException
	{
		return getFact(subject,context,0);
	}

	private Fact getExactFact(final String subject,final String context) throws FactNotFoundException
	{
		List<Fact> facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\" AND lang = \"" + getChannelLanguage(context) + "\"");
		//If none for our language, find for default language
		if (facts.size() == 0 || facts.get(0).whatis() == null && facts.get(0).linkedFactoid == null)
			facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\" AND lang = \"" + DEFAULT_LANG + "\"");
		//Fall back to any language, if there are no other alternatives.
		if (facts.size() == 0 || facts.get(0).whatis() == null && facts.get(0).linkedFactoid == null)
			facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\"");
		if (facts.size() > 0)
			return facts.get(0);
		else
			throw new FactNotFoundException();
	}

	private Fact getFact(final String subject,final String context, final int i) throws FactNotFoundException
	{
		//Get facts for our preferred language.
		List<Fact> facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\" AND lang = \"" + getChannelLanguage(context) + "\"");
		//If none for our language, find for default language
		if (facts.size() == 0 || facts.get(0).whatis() == null && facts.get(0).linkedFactoid == null)
			facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\" AND lang = \"" + DEFAULT_LANG + "\"");
		//Fall back to any language, if there are no other alternatives.
		if (facts.size() == 0 || facts.get(0).whatis() == null && facts.get(0).linkedFactoid == null)
			facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\"");

		//Follow links as appropriate and return the found fact.
		if (facts.size() > 0 && i < MAX_DEPTH)
		{
			final Fact fact = facts.get(0);
			if (fact.linkedFactoid != null && fact.linkedFactoid.length() > 0)
				return getFact(fact.linkedFactoid,context, i + 1);
			else
				return fact;
		}
		else
			throw new FactNotFoundException();
	}

	/**
	 * Deletes all facts with specified subject, for specified channel context only
	 *
	 * @param subject	The factoid key.
	 * @param context	The channel, to allow localised factoids
	 * @return	The factoid
	 * @throws SUSEFactoids.FactNotFoundException
	 */
	private void deleteFacts(final String subject, final String context) throws FactNotFoundException
	{
		deleteFacts(subject, context,0);
	}

	private void deleteFacts(final String subject, final String context,int i) throws FactNotFoundException
	{
		final List<Fact> facts = mods.odb.retrieve(Fact.class, "WHERE subject = \"" + subject.toLowerCase() + "\" AND lang = \"" + getChannelLanguage(context) + "\"");

		//Follow links as appropriate and delete the found fact and all parents.
		if (facts.size() > 0 && i < MAX_DEPTH)
		{
			i++;
			final Fact fact = facts.get(0);
			try //to delete linked facts
			{
				if (fact.linkedFactoid != null && fact.linkedFactoid.length() > 0)
					deleteFacts(fact.linkedFactoid,context);
			} catch (final FactNotFoundException e)
			{
				//Ignore
			}
			//Delete this fact.
			mods.odb.delete(fact);
		}
		else
			throw new FactNotFoundException();
	}


	enum ReplyMode
	{
		NORMAL,
		REPLY,
		PRIVATE
	}

	private ReplyMode getReplyMode(final String request)
	{
		if (request.matches(".*>.*"))
			return ReplyMode.PRIVATE;
		else if (request.matches(".*@.*"))
			return ReplyMode.REPLY;
		else
			return ReplyMode.NORMAL;
	}

	private String getTarget(final String request)
	{
		return request.replaceAll(".*?(>|@)\\s*", "").replaceAll("\\s.*", "");
	}

	private String getSubject(final String request)
	{
		return request.replaceAll("(@|#|>).*", "");
	}

	private void privateMessage(final Message mes, final String target, final String message)
	{
		if (Arrays.asList(irc.getUsers(mes.getContext())).contains(target))
		{
			irc.sendMessage(target, message);
		}
		else
		{
			irc.sendContextMessage(mes, "User " + target + " does not appear to be in the channel.");
			return;
		}
	}

	private void sendToIRC(final Message mes, final ReplyMode mode, final String target, final Fact fact)
	{
		sendToIRC(mes, mode, target, fact.whatis(mes.getNick()));
	}

	private void sendToIRC(final Message mes, final ReplyMode mode, final String target, final String message)
	{
		if (mode == ReplyMode.PRIVATE)
			privateMessage(mes, target, message);
		else if (mode == ReplyMode.REPLY)
			irc.sendContextMessage(mes,target + ": " + message);
		else if (mode == ReplyMode.NORMAL)
			irc.sendContextMessage(mes, message);
	}

	public String[] helpCommandWhatis =
	{
		"Returns either a Factoid the bot has been taught, or, if none exists a rumour prefixed with \"Rumour has it:\".",
		"<Factoid>",
		"<Factoid> is the name of the Factoid to lookup."
	};

	private void whatIs(final Message mes, final boolean quiet)
	{
		final List<String> params = mods.util.getParams(mes, 1);

		if (params.size() < 2)
		{
			irc.sendContextReply(mes, "Whatis what?");
			return;
		}
		whatIs(mes,quiet,params.get(1));
	}

	private void whatIs(final Message mes, final boolean quiet, final String request)
	{
		//Get the components we're interested in
		final String subject = getSubject(request);
		final String target = getTarget(request);
		final ReplyMode replyMode = getReplyMode(request);
		//Avoid multiple people requesting same factoid at the same time.
		//Often happens in a busy channel.
		synchronized(locks)
		{
			if (locks.contains(subject))
				return;
			locks.add(subject);
		}
		try
		{
			sendToIRC(mes, replyMode, target, getFact(subject,mes.getContext()));
		} catch (final FactNotFoundException e)
		{
			if (!quiet)
				sendToIRC(mes, replyMode, target, "I'm afraid I don't know anything about " + subject + ".");
			return;
		} finally
		{
			//Remove the subject lock in 5s.
			mods.interval.callBack(subject, 5000, 1);
		}
	}

	public void commandWhatis(final Message mes)
	{
		whatIs(mes,false);
	}

	public String[] helpCommandSearch =
	{
		"Searches for matching factoids",
		"<String>",
		"<String> is the string to search for matching factoids to."
	};

	public void commandSearch(final Message mes)
	{

		final List<String> params = mods.util.getParams(mes, 1);

		if (params.size() < 2)
		{
			irc.sendContextReply(mes, "Search for what?");
			return;
		}

		final String item = params.get(1).replaceAll("\\?", "");

		final List<Fact> facts = mods.odb.retrieve(Fact.class, "WHERE subject REGEXP'.*" + item.toLowerCase() + ".*' AND (lang = \"" + getChannelLanguage(mes.getContext()) + "\" OR lang = \"en_GB\")");

		if (facts.size() == 0)
		{
			irc.sendContextReply(mes, "Couldn't find any factoids matching " + item + ", try asking google!");
		} else
		{
			boolean found = false;
			final StringBuilder resultString = new StringBuilder(" ");
			byte maxNo = 100;
			for (int i = 0; i < facts.size(); i++)
			{
				if (facts.get(i).confirmedFact != null)
				{
					maxNo--;
					found = true;
					resultString.append(facts.get(i).subject).append("; ");
				}
				if (maxNo < 1)
				{
					resultString.append(", and more...; specify a search string to narrow down results.");
					break;
				}
			}
			if (found == false)
			{
				return;
			}
			irc.sendContextMessage(mes, "Matching factoids: " + resultString.toString());
		}
	}

	public String[] helpCommandFactInfo =
	{
		"Returns meta-data about the specified Factoid",
		"<Factoid>",
		"<Factoid> is the factoid to return meta-data about."
	};
	public void commandFactInfo(final Message mes)
	{
		final List<String> params = mods.util.getParams(mes, 1);

		if (params.size() < 2)
		{
			irc.sendContextReply(mes, "Factoid Information for what?");
			return;
		}

		final String item = params.get(1).replaceAll("\\?", "");

		try
		{
			final Fact thisFact = getFact(item, mes.getContext());
			irc.sendContextMessage(mes,
				"Factoid for \"" +
				thisFact.subject +
				"\", was created by " +
				thisFact.author +
				" at " +
				thisFact.created +
				". Last requested by " +
				thisFact.lastRequestedBy +
				" at " +
				thisFact.lastRequestedAt +
				", requested " +
				thisFact.numberOfRequests +
				" times."
			);
		} catch (final FactNotFoundException e)
		{
			irc.sendContextReply(mes, "Couldn't find any factoids matching " + item);
		}
	}

	public String[] helpCommandForget =
	{
		"Deletes the specified factoid",
		"<Factoid>",
		"<Factoid> is the factoid to delete."
	};

	public void commandForget(final Message mes)
	{
		commandDelete(mes);
	}


	public String[] helpCommandDelete =
	{
		"Deletes the specified factoid",
		"<Factoid>",
		"<Factoid> is the factoid to delete."
	};
	public void commandDelete(final Message mes)
	{
		mods.security.checkNickPerm(new ChoobPermission("plugins.factoids.delete"), mes);

		final List<String> params = mods.util.getParams(mes, 1);

		if (params.size() < 2)
		{
			irc.sendContextReply(mes, "Delete what?");
			return;
		}

		final String item = params.get(1);

		try
		{
			deleteFacts(item, mes.getContext());
			irc.sendContextReply(mes, "Ok. Deleted " + item);
		} catch (final FactNotFoundException e)
		{
			irc.sendContextReply(mes, "I don't know anything about " + item + " in this context's language, so can't delete it.");
		}
	}


	private boolean shouldIgnore(final Message mes)
	{
		final String text = mes.getMessage();

		final Matcher matcher = triggerPattern.matcher(text);
		int offset = 0;

		// Make sure this is actually a command...
		if (matcher.find())
		{
			offset = matcher.end();
		} else if (!(mes instanceof PrivateEvent))
		{
			return true;
		}

		final int dotIndex = text.indexOf('.', offset);

		int cmdEnd = text.indexOf(' ', offset) + 1;
		if (cmdEnd == 0)
		{
			cmdEnd = text.length();

			// Real command, not an factoid...
			// Drop out.
		}
		if (dotIndex != -1 && dotIndex < cmdEnd)
		{
			return true;
		}
		final List<String> params = mods.util.getParams(mes, 0);
		final String lookup = params.get(0).replaceAll("@.*","");
		final String isAlias = lookup.replaceAll("\\s.*","");
		try
		{
			if (mods.plugin.callAPI("Alias", "Get",isAlias) != null)
			{
				return true;
			}
		} catch (final ChoobNoSuchCallException e)
		{
			return true;
		}

		return false;
	}


	public String filterTriggerRegex = "";
	private final Pattern triggerPattern;
	private final Pattern factoidPattern = Pattern.compile("(.{2,}?)\\s+(?:is)\\s+(.{3,})");
	public void filterTrigger(final Message mes)
	{
		try
		{
			if (shouldIgnore(mes))
				return;

			final List<String> params = mods.util.getParams(mes, 0);
			final String lookup = params.get(0);
			if (lookup.matches(".*\\sis\\s.*"))
			{
				final Matcher factoidMatcher = factoidPattern.matcher(lookup);
				if (factoidMatcher.find())
				{
					final String factName = factoidMatcher.group(1).toLowerCase();
					final String factValue = factoidMatcher.group(2);
					irc.sendContextReply(mes, "Remembering " + factName + ".");
					rememberFact(factName, factValue, mes.getNick(),mes.getContext());
				} else
				{
					irc.sendContextReply(mes, "I don't understand!");
				}
			} else
			{
				whatIs(mes, true,lookup);
			}

		} catch (final Exception e)
		{
			//Don't want error spamming channel from filters
			return;
		}
	}
}
