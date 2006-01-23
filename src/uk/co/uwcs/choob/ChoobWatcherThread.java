/*
 * ChoobWatcherThread.java
 *
 * Created on August 5, 2005, 10:34 PM
 */

package uk.co.uwcs.choob;

import uk.co.uwcs.choob.support.*;
import java.util.*;
import uk.co.uwcs.choob.modules.*;

/**
 * Continually monitors the list of active callbacks, executing them as required.
 */
public final class ChoobWatcherThread extends Thread
{
	private List<Interval> intervalList;
	private IRCInterface irc;
	private boolean running;
	private Map pluginMap;
	private Modules mods;

	/** Creates a new instance of ChoobWatcherThread */
	ChoobWatcherThread( List<Interval> intervalList, IRCInterface irc, Map pluginMap, Modules mods )
	{
		this.intervalList = intervalList;
		this.irc = irc;
		this.pluginMap = pluginMap;
		this.mods = mods;
	}

	public void run()
	{
		running = true;

		do
		{
			long timeNow = (new Date()).getTime();
			long next = timeNow + 1000;
			synchronized( intervalList )
			{
				Iterator<Interval> tempIt = intervalList.iterator();

				while( tempIt.hasNext() )
				{
					Interval tempInterval = tempIt.next();

					if( tempInterval.getTrigger() <= timeNow )
					{
						tempIt.remove();
						ChoobTask t = mods.plugin.doInterval(tempInterval.getPlugin(), tempInterval.getParameter());
						if (t != null)
						{
							try
							{
								ChoobThreadManager.queueTask(t);
							}
							catch (Exception e)
							{
								System.err.println("Plugin " + tempInterval.getPlugin() + " got exception queuing task.");
								System.err.println(e);
								e.printStackTrace();
							}
						}
						else
						{
							System.err.println("Plugin manager for plugin " + tempInterval.getPlugin() + " returned a null doInterval ChoobTask.");
						}
					}
					else if (next > tempInterval.getTrigger())
						next = tempInterval.getTrigger();
				}
			}

			long delay = next - timeNow;

			synchronized( this )
			{
				try
				{
					wait(delay);
				}
				catch( InterruptedException e )
				{
					// Well shucks Batman, I guess that _was_ a gay bar.
				}
			}
		}
		while( running );
	}
}
