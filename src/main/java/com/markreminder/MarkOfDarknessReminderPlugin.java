package net.runelite.client.plugins.markreminder;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.time.Instant;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
		name = "Mark of Darkness Reminder",
		description = "Notifies you when to re-cast Mark of Darkness based on duration",
		tags = {"mark", "darkness", "reminder", "arceuus"}
)
public class MarkOfDarknessReminderPlugin extends Plugin
{
	private static final int PURGING_STAFF_ID = 27645;
	private static final int SPELLBOOK_VARBIT = 4070;
	private static final int ARCEUUS_SPELLBOOK_ID = 3;
	private static final Logger logger = Logger.getLogger(MarkOfDarknessReminderPlugin.class.getName());

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private MarkOfDarknessReminderConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MarkOfDarknessOverlay overlay;

	@Inject
	private KeyManager keyManager;

	// State variables
	private Instant markCastTime;
	private int markDuration = 0;
	private Pattern remindPattern;
	private boolean overlayHidden = false;

	// Getters for overlay to access
	public Instant getMarkCastTime() {
		return markCastTime;
	}

	public int getMarkDuration() {
		return markDuration;
	}

	public boolean isOverlayHidden() {
		return overlayHidden;
	}

	public boolean isPurgingStaffEquipped() {
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
			return false;

		Item[] items = equipment.getItems();
		if (items.length <= EquipmentInventorySlot.WEAPON.getSlotIdx())
			return false;

		Item weapon = items[EquipmentInventorySlot.WEAPON.getSlotIdx()];
		return weapon != null && weapon.getId() == PURGING_STAFF_ID;
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hideReminderHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			overlayHidden = !overlayHidden;
			logger.info("Hotkey pressed, overlay hidden: " + overlayHidden);
		}
	};

	@Override
	protected void startUp()
	{
		logger.info("Mark of Darkness Reminder started.");

		// Start with mark not active (null time and 0 duration)
		markCastTime = null;
		markDuration = 0;

		// Make sure overlay is not hidden
		overlayHidden = false;

		// Add overlay to manager - it will be visible by default
		overlayManager.add(overlay);
		logger.info("Overlay added to manager, should be visible by default");

		keyManager.registerKeyListener(hotkeyListener);

		// Compile regex pattern from config (only if it's not empty)
		updateRemindPattern();
	}

	@Override
	protected void shutDown()
	{
		markCastTime = null;
		markDuration = 0;
		remindPattern = null;
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(hotkeyListener);
		logger.info("Plugin shutdown, overlay removed");
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Only handle spellbook changes if the option is enabled
		if (event.getVarbitId() == SPELLBOOK_VARBIT && config.onlyOnArceuusSpellbook())
		{
			// This will be handled by the overlay directly
			int spellbook = client.getVarbitValue(SPELLBOOK_VARBIT);
			logger.info("Spellbook changed to: " + spellbook + " (Arceuus is " + ARCEUUS_SPELLBOOK_ID + ")");
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE)
			return;

		String message = event.getMessage();

		// Detecting Mark of Darkness Cast
		if (message.contains("You have placed a Mark of Darkness upon yourself."))
		{
			// When Mark is cast, store its time and calculate duration based on magic level
			int magicLevel = client.getRealSkillLevel(Skill.MAGIC);
			double durationSeconds = 0.6 * magicLevel;

			boolean usingPurgingStaff = isPurgingStaffEquipped();
			if (usingPurgingStaff)
				durationSeconds *= 5;

			markDuration = (int)Math.round(durationSeconds);
			markCastTime = Instant.now();

			logger.info("Mark cast detected. Duration = " + markDuration + "s (Magic: " + magicLevel + ", Staff: " + usingPurgingStaff + ")");
		}
		// When the reminder is about to expire
		else if (message.contains("Your Mark of Darkness is about to run out."))
		{
			if (config.notifyMarkExpired())
			{
				// Show notification using RuneLite's built-in notifier
				notifier.notify("Mark of Darkness is about to expire!");
			}
			logger.info("Mark of Darkness about to expire notification");
		}
		// When the Mark of Darkness expires
		else if (message.contains("Your Mark of Darkness has faded away."))
		{
			if (config.notifyMarkExpired())
			{
				// Show reminder using RuneLite's built-in notifier
				notifier.notify(config.customReminderText());
			}

			// Reset mark status so overlay will show
			markCastTime = null;
			markDuration = 0;

			logger.info("Mark of Darkness expired, overlay should now be visible");
		}

		// Regex Match for custom reminder
		if (remindPattern != null)
		{
			Matcher matcher = remindPattern.matcher(message);
			if (matcher.find())
			{
				if (config.notifyMarkExpired())
				{
					// Trigger reminder if regex matches
					notifier.notify("Mark of Darkness triggered reminder based on regex match!");
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (markCastTime == null || markDuration <= 0)
			return;

		long elapsed = Instant.now().getEpochSecond() - markCastTime.getEpochSecond();

		// Check if mark has expired based on calculated duration
		if (elapsed >= markDuration)
		{
			// The mark has expired
			if (config.notifyMarkExpired())
			{
				// Show notification using RuneLite's built-in notifier
				notifier.notify(config.customReminderText());
			}

			// Reset mark status so overlay will show
			markCastTime = null;
			markDuration = 0;

			logger.info("Mark expired (timed), overlay should now be visible");
		}
	}

	// For testing: activates a mark timer
	public void testActivateMark() {
		// Simulate a Mark of Darkness being cast
		int magicLevel = client.getRealSkillLevel(Skill.MAGIC);
		double durationSeconds = 0.6 * magicLevel;

		boolean usingPurgingStaff = isPurgingStaffEquipped();
		if (usingPurgingStaff)
			durationSeconds *= 5;

		markDuration = (int)Math.round(durationSeconds);
		markCastTime = Instant.now();

		logger.info("Test mark activation triggered. markDuration: " + markDuration);
	}

	// For testing: expires a mark timer
	public void simulateMarkExpired() {
		// Reset the mark state
		markCastTime = null;
		markDuration = 0;
		logger.info("Simulated mark expired - overlay should be visible");
	}

	private void updateRemindPattern()
	{
		if (!config.remindRegex().isEmpty())
		{
			try
			{
				remindPattern = Pattern.compile(config.remindRegex());
			}
			catch (Exception e)
			{
				logger.warning("Invalid regex pattern: " + config.remindRegex());
				remindPattern = null;
			}
		}
		else
		{
			remindPattern = null;
		}
	}

	@Provides
	MarkOfDarknessReminderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MarkOfDarknessReminderConfig.class);
	}
}