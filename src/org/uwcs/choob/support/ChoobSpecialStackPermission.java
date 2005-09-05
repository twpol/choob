package org.uwcs.choob.support;

import java.security.BasicPermission;
import java.util.List;

/**
 * Haxxy hack of a permission to enable us to get a stack trace of plugins.
 * @author bucko
 */

public class ChoobSpecialStackPermission extends BasicPermission
{
	private List haxList;
	public ChoobSpecialStackPermission(List haxList)
	{
		super("HAX");
		this.haxList = haxList;
	}

	public List getHaxList()
	{
		return haxList;
	}
}