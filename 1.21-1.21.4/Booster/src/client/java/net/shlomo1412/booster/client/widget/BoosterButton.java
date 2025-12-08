package net.shlomo1412.booster.client.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Base button widget for Booster with enhanced tooltip support.
 * Shows action description normally, full description when CTRL is held.
 */
public class BoosterButton extends ButtonWidget {
    private final String actionDescription;
    private final String fullDescription;
    private final Tooltip normalTooltip;
    private final Tooltip extendedTooltip;

    /**
     * Creates a new Booster button.
     *
     * @param x                 X position
     * @param y                 Y position
     * @param width             Button width
     * @param height            Button height
     * @param icon              The icon/text to display on the button (emoji/unicode)
     * @param actionDescription Short description of what the button does
     * @param fullDescription   Full description shown when CTRL is held
     * @param onPress           Action to perform when pressed
     */
    public BoosterButton(int x, int y, int width, int height, String icon,
                         String actionDescription, String fullDescription,
                         PressAction onPress) {
        super(x, y, width, height, Text.literal(icon), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.actionDescription = actionDescription;
        this.fullDescription = fullDescription;
        this.normalTooltip = Tooltip.of(Text.literal(actionDescription));
        this.extendedTooltip = Tooltip.of(createExtendedTooltip());
        setTooltip(normalTooltip);
    }

    /**
     * Creates the extended tooltip with fancy formatting.
     */
    private Text createExtendedTooltip() {
        MutableText text = Text.empty();
        
        // Action title in gold
        text.append(Text.literal(actionDescription).formatted(Formatting.GOLD, Formatting.BOLD));
        text.append(Text.literal("\n\n"));
        
        // Full description in gray/italic
        text.append(Text.literal(fullDescription).formatted(Formatting.GRAY, Formatting.ITALIC));
        
        return text;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update tooltip based on CTRL key state
        if (Screen.hasControlDown()) {
            setTooltip(extendedTooltip);
        } else {
            setTooltip(normalTooltip);
        }

        super.renderWidget(context, mouseX, mouseY, delta);
    }

    /**
     * Builder class for creating BoosterButtons with a fluent API.
     */
    public static class Builder {
        private int x, y;
        private int width = 20;
        private int height = 20;
        private String icon = "?";
        private String actionDescription = "";
        private String fullDescription = "";
        private PressAction onPress = button -> {};

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder actionDescription(String description) {
            this.actionDescription = description;
            return this;
        }

        public Builder fullDescription(String description) {
            this.fullDescription = description;
            return this;
        }

        public Builder onPress(PressAction action) {
            this.onPress = action;
            return this;
        }

        public BoosterButton build() {
            return new BoosterButton(x, y, width, height, icon, 
                    actionDescription, fullDescription, onPress);
        }
    }

    /**
     * @return A new builder for creating a BoosterButton
     */
    public static Builder builder() {
        return new Builder();
    }
}
