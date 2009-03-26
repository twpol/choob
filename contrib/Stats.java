import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.ChoobBadSyntaxError;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.ActionEvent;
import uk.co.uwcs.choob.support.events.ChannelEvent;
import uk.co.uwcs.choob.support.events.Message;

/**
 * Fun (live) stats for all the family.
 *
 * @author Faux
 */

class EntityStat
{
	public int id;
	public String statName;
	public String entityName;
	//String chan; // ???
	public double value; // WMA; over 100 lines for people, 1000 lines for channels
}

class StatSortByValue implements Comparator<EntityStat>
{
	public int compare(final EntityStat o1, final EntityStat o2) {
		if (o1.value > o2.value) {
			return -1;
		}
		if (o1.value < o2.value) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(final Object obj) {
		return false;
	}
}

public class Stats
{
	final static int HISTORY = 1000;
	final static double NICK_LENGTH = 100; // "Significant" lines in WMA calculations.
	final static double CHAN_LENGTH = 1000;
	final static double THRESHOLD = 0.005; // val * THRESHOLD is considered too small to be a part of WMA.
	final static double NICK_ALPHA = Math.exp(Math.log(THRESHOLD) / NICK_LENGTH);
	final static double CHAN_ALPHA = Math.exp(Math.log(THRESHOLD) / CHAN_LENGTH);

	public String[] info()
	{
		return new String[] {
			"Plugin for analysing speech.",
			"The Choob Team",
			"choob@uwcs.co.uk",
			"$Rev$$Date$"
		};
	}

	private final Modules mods;
	private final IRCInterface irc;
	public Stats(final Modules mods, final IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
	}

/*	private String getText(Message mes) throws ChoobException
	{
		List<Message> history = mods.history.getLastMessages( mes, HISTORY );
		final String target = mods.nick.getBestPrimaryNick(mods.util.getParamString(mes));

		StringBuilder wb = new StringBuilder();
		int mlines = 0;

		for (Message m : history)
			if (mods.nick.getBestPrimaryNick(m.getNick()).equalsIgnoreCase(target))
			{
				wb.append(". ").append(m.getMessage()).append(". ");
				mlines++;
			}

		if (mlines < 5)
			throw new ChoobException("Not enough lines for them!");


		return wb.toString().toLowerCase()
								.replaceAll("[a-z-]+[^a-z ]+[a-z-]+", ". ")	// Word with non-word in it (ie. url)
								.replaceAll("[^a-z ]+[a-z-]+", ". ")		// Word with illegal prefix.
								.replaceAll("[a-z]+[^a-z. ]+", "")			// Word with illegal suffix
								.replaceAll("[^a-z .-]", "")				// Remaining chars.
								.replaceAll(" *\\.+ +\\.", "")				// Nusiance 'sentances'.
								.replaceAll(" *\\.", ".")					// Spaces before ends.
								.replaceAll("  +", " ");					// Excess space.
	}

	public void commandFogg( Message mes )
	{

		// http://www.dantaylor.com/pages/fogg.html
		// Approximations ftw.

		final String workingText;
		try
		{
			workingText = getText(mes);
		}
		catch (ChoobException e)
		{
			irc.sendContextReply(mes, e.toString());
			return;
		}

		final int sentences = workingText.replaceAll("[^.]", "").length();
		final int words = countWords(workingText);

		final float A = (float)words / (float)sentences;
		final float B = countWords(workingText.replaceAll("\\b[a-z]{1,6}\\b", "")) / ((float)words/100.0f);

		final float div = 10;
		irc.sendContextReply(mes, "Based on " + mods.nick.getBestPrimaryNick(mods.util.getParamString(mes)) + "'s last few lines in here, their writing age is about (((" + words + "/" + sentences + ") + " + B + ") * 0.4 = " + (Math.round(((A+B) * 0.4) * div) / div) + ".");
	} */

/*
	public void commandSpammers( Message mes )
	{
		List<Message> history = mods.history.getLastMessages( mes, HISTORY );

		Map<String, Integer> scores = new HashMap<String, Integer>();
		for (Message m : history)
		{
			final String nick = mods.nick.getBestPrimaryNick(m.getNick());
			Integer i = scores.get(nick);
			if (i == null)
			{

				// frikkin' immutable integers
		}
	}
*/
	private void update( final String thing, final Message mes, final double thisVal )
	{
		if ( mes instanceof ChannelEvent )
			updateObj( thing, mes.getContext(), thisVal, CHAN_ALPHA );
		updateObj( thing, mods.nick.getBestPrimaryNick( mes.getNick() ), thisVal, NICK_ALPHA );
	}

	private void updateObj ( final String thing, final String name, final double thisVal, final double alpha )
	{
		// I assume thing is safe. ^.^
		final List<EntityStat> ret = mods.odb.retrieve( EntityStat.class, "WHERE entityName = \"" + mods.odb.escapeString(name) + "\" && statName = \"" + thing + "\"");
		EntityStat obj;
		if (ret.size() == 0) {
			obj = new EntityStat();
			obj.statName = thing;
			obj.entityName = name;
			obj.value = thisVal;
			mods.odb.save(obj);
		} else {
			obj = ret.get(0);
			obj.value = alpha * obj.value + (1 - alpha) * thisVal;
			mods.odb.update(obj);
		}
	}

	public void onMessage( final Message mes )
	{
		if (Pattern.compile(irc.getTriggerRegex()).matcher(mes.getMessage()).find()) {
			// Ignore commands.
			return;
		}

		String content = mes.getMessage().replaceAll("^[a-zA-Z0-9`_|]+:\\s+", "");
		final boolean referred = !content.equals(mes.getMessage());

		if (mes instanceof ActionEvent) {
			content = "*" + mes.getNick() + " " + content; // bizarrely, this is proper captuation grammar.
		}

		if (Pattern.compile("^<\\S+>").matcher(content).find()) {
			// Ignore quotes, too.
			return;
		}

		try {
			update( "captuation", mes, apiCaptuation( content ) );
			final int wc = apiWordCount( content );
			update( "wordcount", mes, wc );
			update( "characters", mes, apiLength( content ) );
			if (wc > 0)
				update( "wordlength", mes, apiWordLength( content ) );
			update( "referred", mes, referred ? 1.0 : 0.0 );
		} catch (final Exception e) {
			// FAIL!
		}
	}

	public String[] helpCommandGet = {
		"Get stat(s) about some person or channel.",
		"<Entity> [ <Stat> ]",
		"<Entity> is the name of the channel or person to get stats for",
		"<Stat> is the optional name of a specific statistic to get (omit it to get all of them)"
	};
	public void commandGet( final Message mes )
	{
		final String[] params = mods.util.getParamArray(mes);
		if (params.length == 3) {
			final String nick = mods.nick.getBestPrimaryNick( params[1] );
			final String thing = params[2].toLowerCase();
			final List<EntityStat> ret = mods.odb.retrieve( EntityStat.class, "WHERE entityName = \"" + mods.odb.escapeString(nick) + "\" && statName = \"" + mods.odb.escapeString(thing) + "\"");
			EntityStat obj;
			if (ret.size() == 0) {
				irc.sendContextReply( mes, "Sorry, cannae find datta one." );
			} else {
				obj = ret.get(0);
				irc.sendContextReply( mes, "They be 'avin a score of " + Math.round(obj.value * 100) / 100.0 );
			}
		} else if (params.length == 2) {
			final String nick = mods.nick.getBestPrimaryNick( params[1] );
			final List<EntityStat> ret = mods.odb.retrieve( EntityStat.class, "WHERE entityName = \"" + mods.odb.escapeString(nick) + "\"");
			if (ret.size() == 0) {
				irc.sendContextReply( mes, "Sorry, cannae find datta one." );
			} else {
				final StringBuilder results = new StringBuilder( "Stats:" );
				for (final EntityStat obj: ret) {
					results.append( " " + obj.statName + " = " + Math.round(obj.value * 100) / 100.0 + ";" );
				}
				irc.sendContextReply( mes, results.toString() );
			}
		} else {
			throw new ChoobBadSyntaxError();
		}
	}

	public String[] helpCommandList = {
		"Gets statistics about an entire channel.",
		"<Channel> <Stat>",
		"<Channel> is the name of the channel get statistics for",
		"<Stat> is the optional name of a specific statistic to get (omit it to get all of them)"
	};
	public void commandList(final Message mes)
	{
		final String[] params = mods.util.getParamArray(mes);
		if (params.length < 3 || params.length > 3) {
			throw new ChoobBadSyntaxError();
		}

		final String channel = params[1];
		final String stat = params[2].toLowerCase();

		final List<String> channelMembers = irc.getUsersList(channel);
		final List<EntityStat> stats = new ArrayList<EntityStat>();
		for (int i = 0; i < channelMembers.size(); i++) {
			final List<EntityStat> datas = mods.odb.retrieve(EntityStat.class, "WHERE entityName = \"" + mods.odb.escapeString(channelMembers.get(i)) + "\" && statName = \"" + mods.odb.escapeString(stat) + "\"");
			if (datas.size() == 0) continue;
			stats.add(datas.get(0));
		}

		if (stats.size() == 0) {
			irc.sendContextReply(mes, "No data found for \"" + stat + "\" in \"" + channel + "\".");
			return;
		}

		Collections.sort(stats, new StatSortByValue());
		final int space = 350;
		final StringBuffer text1 = new StringBuffer();
		final StringBuffer text2 = new StringBuffer();
		boolean addToStart = true;

		text1.append("\"");
		text1.append(stat);
		text1.append("\" in \"");
		text1.append(channel);
		text1.append("\": ");
		while (stats.size() > 0) {
			final EntityStat data = addToStart ? stats.get(0) : stats.get(stats.size() - 1);
			stats.remove(data);
			final StringBuffer text = new StringBuffer();
			text.append(data.entityName);
			text.append(" (");
			text.append(Math.round(data.value * 100) / 100.0);
			text.append(")");
			if (text1.length() + text2.length() + text.length() > space) {
				text1.append("...");
				break;
			}
			if (addToStart) {
				text1.append(text);
				if (stats.size() > 0) text1.append(", ");
			} else {
				text2.insert(0, text);
				if (stats.size() > 0) text2.insert(0, ", ");
			}
			addToStart = !addToStart;
		}
		text1.append(text2);
		text1.append(".");
		irc.sendContextReply(mes, text1.toString());
	}

	public int apiCaptuation( String str )
	{
		int score = 0;

		// remove smilies and trailing whitespace.
		str = str.replaceAll("(?:^|\\s+)[:pP)/;\\\\o()^><.� _-]{2,4}(\\s+|$)", "$1");

		// remove URLs
		str = str.replaceAll("[a-z0-9]+:/\\S+", "");

		// Nothing left?
		if (str.length() == 0)
			return 0;

		// No thingie on the end? PENALTY.
		// Must end with ., !, ? with or without optional terminating ) or ".
		if (!Pattern.compile("[\\.\\?\\!][\\)\"]?$").matcher(str).find())
			score += 1;

		// Now remove quoted stuff; it'll only give extra points where not needed.
		str = str.replaceAll("\".*?\"", "");

		// Small letter at start of new sentance/line? PENALTY.
		Matcher ma = Pattern.compile("(?:^|(?<!\\.)\\.\\s+)\\p{Ll}").matcher(str);
		while (ma.find())
			score += 1;

		// Penalty non-words.
		// Exceptions: id, Id, ill, cant, wont, hand,
		// Punish:
		//  Lowercase "I" in special cases.
		//  Missing apostrophes in special cases.
		//  Certain American spellings.
		//  Internetisms, like "zomg."
		//  Certain known abbreviations that aren't capitalised.
		//  Mixed case, like "tHis". "CoW" is fine, however.
		//  Leetspeak. Unfortunately, C0FF33 is still valid, as it's also hex.
		//   Also, words with trailing numbers are fine, since some nicknames
		//   etc. are like this.
		ma = Pattern.compile("\\b(?:im|Im|i'm|i'd|i|i'll|b|u2?|(?i:s?hes|they(?:ve|re|ll)|there(?:s|re|ll)|(?:has|was|sha|have)nt|(?:could|would)(?:ve|nt)|k?thz|pl[xz]|zomg|\\w+xor|sidewalk|color)|(?=[A-Z]*[a-z][A-Za-z]*\\b)(?i:bbq|lol|rofl|i?irc|afaik|hth|imh?o|fy|https?|ft[lw])|[a-z]+[A-Z][a-zA-Z]*|(?!(?:[il]1[08]n))(?:\\w*[g-zG-Z]\\w*[0-9]\\w*[a-zA-Z]|\\w*[0-9]\\w*[g-zG-Z]\\w*[a-zA-Z])|american|british|english|european)\\b").matcher(str);
		while (ma.find())
			score += 1;

		return score;
	}

	// http://schmidt.devlib.org/java/word-count.html#source
	public int apiWordCount(final String str)
	{
		int numWords = 0;
		int index = 0;
		boolean prevWhitespace = true;
		while (index < str.length())
		{
			final char c = str.charAt(index++);
			final boolean currWhitespace = Character.isWhitespace(c);
			if (prevWhitespace && !currWhitespace)
				numWords++;
			prevWhitespace = currWhitespace;
		}
		return numWords;
	}

	public int apiLength(final String str)
	{
		return str.replaceAll("\\s+", "").length();
	}

	public double apiWordLength(final String str)
	{
		return (double)apiLength(str) / (double)apiWordCount(str);
	}


/*
	public void commandText( Message mes ) throws ChoobException
	{
		final String workingText = getText(mes);
		irc.sendContextReply(mes, (String)mods.plugin.callAPI("Http", "StoreString", workingText));
	}
*/

}
