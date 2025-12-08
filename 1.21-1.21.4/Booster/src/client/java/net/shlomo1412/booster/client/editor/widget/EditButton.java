package net.shlomo1412.booster.client.editor.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.shlomo1412.booster.client.editor.EditorModeManager;

/**
 * The Edit button that toggles Booster's editor mode.
 * Appears on screens that have Booster modifications.
 */
public class EditButton extends ButtonWidget {
    private static final int BUTTON_SIZE = 20;
    private static final String EDIT_ICON = "✏";
    private static final String CLOSE_ICON = "✕";

    private boolean editorActive = false;

    public EditButton(int x, int y, Runnable onToggle) {
        super(x, y, BUTTON_SIZE, BUTTON_SIZE, Text.literal(EDIT_ICON), 
                button -> {
                    EditorModeManager.getInstance().toggleEditorMode();
                    ((EditButton) button).updateState();
                    onToggle.run();
                }, 
                DEFAULT_NARRATION_SUPPLIER);
        
        updateTooltip();
    }

    /**
     * Updates the button state based on editor mode.
     */
    public void updateState() {
        this.editorActive = EditorModeManager.getInstance().isEditorModeActive();
        setMessage(Text.literal(editorActive ? CLOSE_ICON : EDIT_ICON));
        updateTooltip();
    }

    private void updateTooltip() {
        if (editorActive) {
            setTooltip(Tooltip.of(
                    Text.literal("Exit Editor").formatted(Formatting.RED)
                            .append(Text.literal("\n"))
                            .append(Text.literal("Click to close editor mode").formatted(Formatting.GRAY))
            ));
        } else {
            setTooltip(Tooltip.of(
                    Text.literal("Booster Editor").formatted(Formatting.GOLD)
                            .append(Text.literal("\n"))
                            .append(Text.literal("Click to edit widget positions").formatted(Formatting.GRAY))
            ));
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Custom rendering with a distinct look
        int bgColor = editorActive ? 0xFFAA3333 : 0xFF333333;
        int borderColor = editorActive ? 0xFFFF5555 : (isHovered() ? 0xFFFFAA00 : 0xFF555555);

        // Background
        context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        // Border
        context.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        context.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        context.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

        // Icon
        int textColor = editorActive ? 0xFFFFFFFF : (isHovered() ? 0xFFFFAA00 : 0xFFCCCCCC);
        context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor
        );
    }

    /**
     * Creates an EditButton builder for easier positioning.
     */
    public static EditButton create(int x, int y, Runnable onToggle) {
        return new EditButton(x, y, onToggle);
    }
}
