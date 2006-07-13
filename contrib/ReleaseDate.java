/*
 * ReleaseDate.java
 *
 * Created on 12 July 2006, 16:18
 */

import uk.co.uwcs.choob.*;
import uk.co.uwcs.choob.modules.*;
import uk.co.uwcs.choob.support.*;
import uk.co.uwcs.choob.support.events.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

/**
 * Plugin to retrieve the release date of a particular game from Gameplay.co.uk
 * @author Chris Hawley (Blood_God)
 */
public class ReleaseDate {
	
	Modules mods;
	private IRCInterface irc;

	//ReleaseDate help
	public String[] helpCommandReleaseDate = {
		"Attempts to look up a game's release date on Gameplay.co.uk. Results include date, title, platform and Gameplay price.",
		"<TITLE>",
		"<TITLE> the title of a game that you wish to get information for."
	};	
	
	/** 
	 * Creates a new instance of ReleaseDate 
	 * @param mods The modules available.
	 * @param irc The IRCInterface.
	 */
	public ReleaseDate(Modules mods, IRCInterface irc) {
		this.mods = mods;
		this.irc = irc;
	}
	
	/**
	 * Get the information for the plugin.
	 * @return The information strings.
	 */
	public String[] info() {
		return new String[] {
			"Plugin to retrieve the release date of a particular game from Gameplay.co.uk",
			"The Choob Team",
			"choob@uwcs.co.uk",
			"$Rev$$Date$"
		};
	}
	
	//TODO: Return multiple results, and filter out "guide" results - as per the JB version this is re-implementing
	//FIXME: Occaisionally gameplay add 'style="color: #aabbcc"' to some of the tags currently used for parsing.
	//       These need taking into consideration as they are currently ignored by the regular expressions.
	/**
	 * The command provided by this plugin.
	 * @param mes The command input from which parameters will be extracted.
	 */
	public void commandReleaseDate(Message mes) {
		String param = mods.util.getParamString(mes);
		try {
			//Check for only sensible input with at least one alpha-numeric character.
			Pattern dodgyCharPattern = Pattern.compile("^[\\s\\w\\-\\:\\;\\.\\,]*[a-zA-Z0-9]+[\\s\\w\\-\\:\\;\\.\\,]*$");
			Matcher dodgyCharMatcher = dodgyCharPattern.matcher(param);
			if (dodgyCharMatcher.matches()) {
				URL url = generateURL("http://shop.gameplay.co.uk/webstore/advanced_search.asp?keyword=", param);
				//TODO: Restructure code to be less "wow.. this was coded at 2am" (admittedly it was.. but it still sucks)
				//Matcher for detecting when gameplay says there are no results
				Matcher noResultMatcher = getMatcher(url, "(?s)" + "Sorry, your search for" + "(.*?)" + "returned no results.");
				if (noResultMatcher.find()) {
					irc.sendContextReply(mes, "Sorry, no information was found for \"" + param + "\".");
				} else {
					//Matcher for detecting a search page full of results
					Matcher gotResultMatcher = getMatcher(url, "(?s)" + "<h2 class=\"vlgWhite\">" + "(.*?)" + "<a href=\"productpage.asp?" + "(.*?)" +  "class=\"vlgWhite\">" + "(.*?)" + "</a></td></h2>" + "(.*?)" + "<div class=\"vsmorange10\">" + "(.*?)" + "</div>" + "(.*?)" + "<td valign=\"bottom\">" + "(.*?)" + "RRP");
					if (gotResultMatcher.find()) {
						irc.sendContextReply(mes, mods.scrape.readyForIrc(prettyReply(gotResultMatcher.group(3) + " (" + gotResultMatcher.group(5) + ")" + gotResultMatcher.group(7), url.toString(), 1)));
					} else {
						//Matcher for detecting content when we've been redirected to a product page
						Matcher singlePageResultMatcher = getMatcher(url, "(?s)" + "<h1 class=\"bbHeader\">" + "(.*?)" + "</h1>" + "(.*?)" + "<td style=\"padding: 6px;\">" + "(.*?)" + "<img src=\"http://shop" + "(.*?)" + "<td valign=\"bottom\" colspan=\"2\">" + "(.*?)" + "<b>Category:</b><a href=");
						if (singlePageResultMatcher.find()) {
							irc.sendContextReply(mes, mods.scrape.readyForIrc(prettyReply(singlePageResultMatcher.group(1) + singlePageResultMatcher.group(5) + singlePageResultMatcher.group(3), url.toString(), 1)));
						} else {
							//It's clearly some unknown page type that isn't yet handled... Let the user deal with it!
							irc.sendContextReply(mes, "There was an error parsing the results. See " + url.toString());
						}
					}
				}
			} else {
				irc.sendContextReply(mes, "Sorry, I'm limited to A-Z0-9,._;: hyphen and space characters. At least one alpha-numeric character must be provided.");
			}

		} catch (LookupException e) {
			irc.sendContextReply(mes, "Error looking up results.");
		} catch (NullPointerException e) {
			irc.sendContextReply(mes, "Error looking up results.");
		} catch (IllegalStateException e) {
			irc.sendContextReply(mes, "Error looking up results.");
		}
	}
	
	/* -- Start of code stolen from Dict.java -- */
		private String prettyReply(String text, String url, int lines) {
		int maxlen=((irc.MAX_MESSAGE_LENGTH-15)*lines) - url.length();
		text=text.replaceAll("\\s+"," ");
		if (text.length()>maxlen) {
			return text.substring(0, maxlen) + "..., see " + url;
		} else {
			return text + " See " + url + ".";
		}
	}

	private URL generateURL(String base, String url) throws LookupException {
		try	{
			return new URL(base + URLEncoder.encode(url, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new LookupException("Unexpected exception generating url.", e);
		} catch (MalformedURLException e) {
			throw new LookupException("Error, malformed url generated.", e);
		}
	}

	private Matcher getMatcher(URL url, String pat) throws LookupException {
		try	{
			return mods.scrape.getMatcher(url, pat);
		} catch (FileNotFoundException e) {
			throw new LookupException("No article found (404).", e);
		} catch (IOException e)	{
			throw new LookupException("Error reading from site. " + e, e);
		}
	}
	/* -- End of stolen code -- */
}

public class LookupException extends ChoobException {
	
	public LookupException(String text)	{
		super(text);
	}
	
	public LookupException(String text, Throwable e) {
		super(text, e);
	}
	
	public String toString() {
		return getMessage();
	}

}