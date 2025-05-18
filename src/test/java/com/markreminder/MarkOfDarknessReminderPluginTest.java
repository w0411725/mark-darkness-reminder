package com.markreminder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MarkOfDarknessReminderPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MarkOfDarknessReminderPlugin.class);
		RuneLite.main(args);
	}
}