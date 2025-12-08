package net.shlomo1412.booster.client.editor;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides information about a screen for the editor's dev details panel.
 */
public class ScreenInfo {
    private final String screenClassName;
    private final String screenTitle;
    private final String handlerClassName;
    private final int containerX;
    private final int containerY;
    private final int containerWidth;
    private final int containerHeight;
    private final int screenWidth;
    private final int screenHeight;
    private final int slotCount;

    public ScreenInfo(Screen screen, int containerX, int containerY, int containerWidth, int containerHeight) {
        this.screenClassName = screen.getClass().getSimpleName();
        this.screenTitle = screen.getTitle().getString();
        this.containerX = containerX;
        this.containerY = containerY;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.screenWidth = screen.width;
        this.screenHeight = screen.height;

        if (screen instanceof HandledScreen<?> handledScreen) {
            ScreenHandler handler = handledScreen.getScreenHandler();
            this.handlerClassName = handler.getClass().getSimpleName();
            this.slotCount = handler.slots.size();
        } else {
            this.handlerClassName = "N/A";
            this.slotCount = 0;
        }
    }

    public String getScreenClassName() {
        return screenClassName;
    }

    public String getScreenTitle() {
        return screenTitle;
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public int getContainerX() {
        return containerX;
    }

    public int getContainerY() {
        return containerY;
    }

    public int getContainerWidth() {
        return containerWidth;
    }

    public int getContainerHeight() {
        return containerHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getSlotCount() {
        return slotCount;
    }

    /**
     * Gets all dev details as a list of key-value pairs.
     */
    public List<String[]> getDevDetails() {
        List<String[]> details = new ArrayList<>();
        details.add(new String[]{"Screen Class", screenClassName});
        details.add(new String[]{"Handler Class", handlerClassName});
        details.add(new String[]{"Screen Size", screenWidth + " x " + screenHeight});
        details.add(new String[]{"Container Position", "(" + containerX + ", " + containerY + ")"});
        details.add(new String[]{"Container Size", containerWidth + " x " + containerHeight});
        details.add(new String[]{"Slot Count", String.valueOf(slotCount)});
        return details;
    }
}
