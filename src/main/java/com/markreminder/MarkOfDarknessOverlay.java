package net.runelite.client.plugins.markreminder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MarkOfDarknessOverlay extends Overlay
{
    private static final int ARCEUUS_SPELLBOOK_ID = 3;  // Updated to correct value
    private static final int SPELLBOOK_VARBIT = 4070;

    private final Client client;
    private final MarkOfDarknessReminderPlugin plugin;
    private final MarkOfDarknessReminderConfig config;

    private boolean flashState = false;
    private int flashCount = 0;

    @Inject
    private MarkOfDarknessOverlay(Client client, MarkOfDarknessReminderPlugin plugin, MarkOfDarknessReminderConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setMovable(true);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // If in debug mode, always show regardless of other conditions
        if (config.debugAlwaysShowOverlay())
        {
            // Skip all checks and render
        }
        // If Mark is active, never show (unless in debug mode)
        else if (plugin.getMarkCastTime() != null && plugin.getMarkDuration() > 0)
        {
            return null;
        }

        // If hidden by hotkey, don't show
        if (plugin.isOverlayHidden())
        {
            return null;
        }

        // DISABLED: Spellbook check was causing issues
        // We'll ignore the onlyOnArceuusSpellbook setting for now

        // Handle flashing if enabled
        Color backgroundColor = config.flashColor1(); // Use flash color #1 as the default background

        if (config.flashReminderBox())
        {
            // Update flash state at most twice per second (every 30 game ticks)
            if (flashCount++ % 30 == 0)
            {
                flashState = !flashState;
            }

            if (flashState)
            {
                backgroundColor = config.flashColor1();
            }
            else
            {
                backgroundColor = config.flashColor2();
            }
        }

        // Choose text based on style setting
        String reminderText = config.reminderStyle() == MarkOfDarknessReminderConfig.ReminderStyle.SHORT_TEXT
                ? "Cast Mark of Darkness"
                : config.customReminderText();

        // Measure text for proper sizing
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(reminderText);
        int textHeight = fontMetrics.getHeight();

        // Set dimensions
        int width = textWidth + 10;
        int height = textHeight + 8;

        // Draw background
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, width, height);

        // Draw text
        graphics.setColor(Color.WHITE);
        int textX = 5;
        int textY = height - (height - textHeight) / 2 - fontMetrics.getDescent();
        graphics.drawString(reminderText, textX, textY);

        return new Dimension(width, height);
    }
}