/**
 * Exception for Choob plugin not found errors.
 * @author bucko
 */

package uk.co.uwcs.choob.support;
import java.security.Permission;

public final class ChoobPluginAuthError extends ChoobAuthError
{
	private static final long serialVersionUID = -4702733953713229195L;
	private final Permission permission;
	private String plugin;

	public ChoobPluginAuthError(final String plugin, final Permission permission)
	{
		super("The plugin " + plugin + " needs this permission: " + getPermissionText(permission));
		this.permission = permission;
	}
	public Permission getPermission()
	{
		return permission;
	}
	public String getPlugin()
	{
		return plugin;
	}
}
