package net.runelite.client.plugins.markreminder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Keybind;

import java.awt.Color;

@ConfigGroup("markreminder")
public interface MarkOfDarknessReminderConfig extends Config
{
	@ConfigItem(
			keyName = "notifyMarkExpired",
			name = "Notify on Expire",
			description = "Show a notification when Mark of Darkness ends or is about to expire"
	)
	default boolean notifyMarkExpired()
	{
		return true;
	}

	@ConfigItem(
			keyName = "flashReminderBox",
			name = "Flash the Reminder Box",
			description = "Enable or disable flashing for the reminder box"
	)
	default boolean flashReminderBox()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			keyName = "flashColor1",
			name = "Flash Color #1",
			description = "First flash color for the reminder box"
	)
	default Color flashColor1()
	{
		return new Color(255, 0, 0, 150); // Red flash color
	}

	@Alpha
	@ConfigItem(
			keyName = "flashColor2",
			name = "Flash Color #2",
			description = "Second flash color for the reminder box"
	)
	default Color flashColor2()
	{
		return new Color(100, 100, 100, 150); // Grey flash color
	}

	@ConfigItem(
			keyName = "onlyArceuus",
			name = "Only on Arceuus Spellbook",
			description = "Only show reminders when using the Arceuus spellbook"
	)
	default boolean onlyOnArceuusSpellbook()
	{
		return false;  // Disabled by default to avoid issues
	}

	@ConfigItem(
			keyName = "hideReminderHotkey",
			name = "Hide Reminder Hotkey",
			description = "Set a hotkey to hide the reminder overlay"
	)
	default Keybind hideReminderHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "reminderStyle",
			name = "Reminder Style",
			description = "Choose between short or long reminder text"
	)
	default ReminderStyle reminderStyle()
	{
		return ReminderStyle.LONG_TEXT;
	}

	@ConfigItem(
			keyName = "customReminderText",
			name = "Reminder Message",
			description = "Custom text to display when the mark expires"
	)
	default String customReminderText()
	{
		return "Mark of Darkness has expired — re-cast it!";
	}

	@ConfigItem(
			keyName = "remindRegex",
			name = "Remind on Regex",
			description = "Display reminder upon chat message matching the regex"
	)
	default String remindRegex()
	{
		return ""; // Default is empty; no regex match
	}

	@ConfigItem(
			keyName = "debugAlwaysShowOverlay",
			name = "Debug: Always Show Overlay",
			description = "Always show the overlay regardless of Mark status (for testing)"
	)
	default boolean debugAlwaysShowOverlay()
	{
		return false;
	}

	enum ReminderStyle
	{
		SHORT_TEXT,
		LONG_TEXT
	}
}