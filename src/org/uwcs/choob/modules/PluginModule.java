/*
 * PluginModule.java
 *
 * Created on June 16, 2005, 2:36 PM
 */

package org.uwcs.choob.modules;

import org.uwcs.choob.plugins.*;
import org.uwcs.choob.support.*;
import org.uwcs.choob.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.sql.*;
import java.security.AccessController;
import java.security.AccessControlException;

/**
 * Module that performs functions relating to the plugin architecture of the bot.
 * @author sadiq
 */
public class PluginModule {
	Map pluginMap;
	DbConnectionBroker broker;
	Modules mods;
	ChoobPluginManager plugMan;
	ChoobPluginManager dPlugMan;
	ChoobPluginManager jsPlugMan;
	Choob bot;

	/**
	 * Creates a new instance of the PluginModule.
	 * @param pluginMap Map containing currently loaded plugins.
	 */
	public PluginModule(Map pluginMap, DbConnectionBroker broker, Modules mods, IRCInterface irc) throws ChoobException {
		this.pluginMap = pluginMap;
		this.broker = broker;
		this.mods = mods;
		this.plugMan = new HaxSunPluginManager(mods, irc);
		this.dPlugMan = new ChoobDistributingPluginManager();
		this.jsPlugMan = new JavaScriptPluginManager(mods, irc);
	}

	public ChoobPluginManager getPlugMan()
	{
		// XXX Need better permission name.
		AccessController.checkPermission(new ChoobPermission("getPluginManager"));

		return dPlugMan;
	}

	/**
	 * Adds a plugin to the loaded plugin map but first unloads any plugin already there.
	 *
	 * This method also calls the create() method on any new plugin.
	 * @param URL URL to the source of the plugin.
	 * @param pluginName Name for the class of the new plugin.
	 * @throws Exception Thrown if there's a syntactical error in the plugin's source.
	 */
	public void addPlugin(String pluginName, String URL) throws ChoobException {
		URL srcURL;
		try
		{
			srcURL = new URL(URL);
		}
		catch (MalformedURLException e)
		{
			throw new ChoobException("URL " + URL + " is malformed: " + e);
		}

		if (srcURL.getFile().endsWith(".js"))
			jsPlugMan.loadPlugin(pluginName, srcURL);
		else
			plugMan.loadPlugin(pluginName, srcURL);

		addPluginToDb(pluginName);
	}

	/**
	 * Call the API subroutine of name name on plugin pluginName and return the result.
	 * @param pluginName The name of the plugin to call.
	 * @param APIString The name of the routine to call.
	 * @param params Parameters to pass to the routine.
	 */
	public Object callAPI(String pluginName, String APIString, Object... params) throws ChoobException
	{
		return dPlugMan.doAPI(pluginName, APIString, params);
	}

	/**
	 * Call the generic subroutine of type type and name name on plugin pluginName and return the result.
	 * @param pluginName The name of the plugin to call.
	 * @param type The type of the routine to call.
	 * @param name The name of the routine to call.
	 * @param params Parameters to pass to the routine.
	 */
	public Object callGeneric(String pluginName, String type, String name, Object... params) throws ChoobException
	{
		AccessController.checkPermission(new ChoobPermission("generic." + type));
		return dPlugMan.doGeneric(pluginName, type, name, params);
	}

	public String exceptionReply(Throwable e)
	{
		if (e instanceof ChoobAuthException)
			return e.getMessage();
		else if (e instanceof ChoobNoSuchPluginException)
			return "You need to load the plugin " + ((ChoobNoSuchPluginException)e).getPlugin() + "!";
		else if (e instanceof AccessControlException)
			return "D'oh! The plugin needs permission " + ChoobAuthException.getPermissionText(((AccessControlException)e).getPermission()) + "!";
		else
			return "The plugin author was too lazy to trap the exception: " + e;
	}

	public ChoobTask doInterval(String plugin, Object param)
	{
		AccessController.checkPermission(new ChoobPermission("interval"));
		return dPlugMan.intervalTask(plugin, param);
	}

	public String[] plugins()
	{
		return plugMan.plugins();
	}

	public String[] commands(String pluginName)
	{
		return plugMan.commands(pluginName);
	}

	public void loadDbPlugins( Modules modules ) throws Exception
	{
		AccessController.checkPermission(new ChoobPermission("canLoadSavedPlugins"));

		Connection dbCon = broker.getConnection();

		PreparedStatement getSavedPlugins = dbCon.prepareStatement("SELECT * FROM LoadedPlugins");

		ResultSet savedPlugins = getSavedPlugins.executeQuery();

		savedPlugins.first();

		do
		{
			addPlugin( savedPlugins.getString("PluginName"), null );
		}
		while( savedPlugins.next() );
	}

	private void addPluginToDb(String pluginName) throws ChoobException
	{
		Connection dbCon = broker.getConnection();
		try {
			PreparedStatement pluginReplace = dbCon.prepareStatement("REPLACE INTO LoadedPlugins VALUES(?,?)");

			pluginReplace.setString(1,pluginName);
			pluginReplace.setString(2,"SunHaxPluginManager");

			pluginReplace.executeUpdate();
		}
		catch (SQLException e)
		{
			System.err.println("SQL Exception: " + e);
			throw new ChoobException("SQL Exception while adding the plugin to the database...");
		}
		finally
		{
			broker.freeConnection(dbCon);
		}
	}
}

