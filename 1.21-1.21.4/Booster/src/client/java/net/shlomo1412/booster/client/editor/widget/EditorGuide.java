package net.shlomo1412.booster.client.editor.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * First-time user guide for Editor Mode.
 * Shows a nice animated tour introducing all features.
 */
public class EditorGuide implements Drawable, Element {
    
    // Tour steps
    private static final List<GuideStep> STEPS = new ArrayList<>();
    static {
        STEPS.add(new GuideStep(
            "Welcome to Editor Mode!",
            "Customize your Booster buttons exactly how you like them.\n" +
            "This quick tour will show you the basics.",
            GuidePosition.CENTER
        ));
        STEPS.add(new GuideStep(
            "The Sidebar",
            "The sidebar on the left shows all modules and their widgets.\n" +
            "You can collapse it by clicking the ◀ button.\n" +
            "Toggle modules ON/OFF and reset positions here.",
            GuidePosition.LEFT
        ));
        STEPS.add(new GuideStep(
            "Drag to Move",
            "Click and drag any button to move it.\n" +
            "A highlight will show when you hover over a button.",
            GuidePosition.CENTER
        ));
        STEPS.add(new GuideStep(
            "Resize Buttons",
            "Drag the edges or corners of a button to resize it.\n" +
            "The cursor will change when hovering over resize handles.",
            GuidePosition.CENTER
        ));
        STEPS.add(new GuideStep(
            "Save & Exit",
            "Your changes are saved automatically!\n" +
            "Click the ✏ button again to exit Editor Mode.\n\n" +
            "Enjoy customizing your Booster experience!",
            GuidePosition.RIGHT
        ));
    }
    
    // Animation
    private static final float ANIMATION_SPEED = 0.1f;
    private float fadeProgress = 0f;
    private float slideProgress = 0f;
    private boolean isOpening = true;
    private boolean isClosing = false;
    
    // State
    private int currentStep = 0;
    private boolean focused = false;
    private final Runnable onComplete;
    
    // Colors
    private static final int OVERLAY_COLOR = 0xE0000000;
    private static final int CARD_BG = 0xFF1a1a1a;
    private static final int CARD_BORDER = 0xFFFFAA00;
    private static final int TEXT_TITLE = 0xFFFFAA00;
    private static final int TEXT_BODY = 0xFFCCCCCC;
    private static final int BUTTON_BG = 0xFF333333;
    private static final int BUTTON_HOVER = 0xFF444444;
    private static final int BUTTON_TEXT = 0xFFFFFFFF;
    private static final int DOT_ACTIVE = 0xFFFFAA00;
    private static final int DOT_INACTIVE = 0xFF555555;
    
    // Layout
    private static final int CARD_WIDTH = 320;
    private static final int CARD_PADDING = 20;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 80;
    
    private final MinecraftClient client;
    private final TextRenderer textRenderer;
    private int screenWidth, screenHeight;
    
    // Button hover states
    private boolean nextHovered = false;
    private boolean prevHovered = false;
    private boolean skipHovered = false;
    
    public EditorGuide(MinecraftClient client, Runnable onComplete) {
        this.client = client;
        this.textRenderer = client.textRenderer;
        this.onComplete = onComplete;
        
        this.screenWidth = client.getWindow().getScaledWidth();
        this.screenHeight = client.getWindow().getScaledHeight();
    }
    
    public void tick() {
        // Fade animation
        if (isOpening) {
            fadeProgress = MathHelper.lerp(ANIMATION_SPEED, fadeProgress, 1f);
            if (fadeProgress > 0.99f) {
                fadeProgress = 1f;
                isOpening = false;
            }
        } else if (isClosing) {
            fadeProgress = MathHelper.lerp(ANIMATION_SPEED, fadeProgress, 0f);
            if (fadeProgress < 0.01f) {
                fadeProgress = 0f;
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }
        
        // Slide animation
        slideProgress = MathHelper.lerp(ANIMATION_SPEED * 2, slideProgress, 1f);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        tick();
        
        if (fadeProgress <= 0) return;
        
        this.screenWidth = client.getWindow().getScaledWidth();
        this.screenHeight = client.getWindow().getScaledHeight();
        
        // Dark overlay
        int overlayAlpha = (int) (0xE0 * fadeProgress);
        context.fill(0, 0, screenWidth, screenHeight, (overlayAlpha << 24));
        
        GuideStep step = STEPS.get(currentStep);
        
        // Calculate card position
        int cardHeight = calculateCardHeight(step);
        int cardX, cardY;
        
        switch (step.position) {
            case LEFT -> {
                cardX = 220 + (int) ((1 - slideProgress) * -50);
                cardY = (screenHeight - cardHeight) / 2;
            }
            case RIGHT -> {
                cardX = screenWidth - CARD_WIDTH - 20 + (int) ((1 - slideProgress) * 50);
                cardY = (screenHeight - cardHeight) / 2;
            }
            default -> {
                cardX = (screenWidth - CARD_WIDTH) / 2;
                cardY = (screenHeight - cardHeight) / 2 + (int) ((1 - slideProgress) * 30);
            }
        }
        
        // Apply fade to card
        float cardAlpha = fadeProgress;
        
        // Card shadow
        int shadowAlpha = (int) (0x40 * cardAlpha);
        context.fill(cardX + 4, cardY + 4, cardX + CARD_WIDTH + 4, cardY + cardHeight + 4, 
                    (shadowAlpha << 24));
        
        // Card background
        int bgAlpha = (int) (0xFF * cardAlpha);
        context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + cardHeight, 
                    (bgAlpha << 24) | (CARD_BG & 0x00FFFFFF));
        
        // Card border
        int borderAlpha = (int) (0xFF * cardAlpha);
        int borderColor = (borderAlpha << 24) | (CARD_BORDER & 0x00FFFFFF);
        context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + 2, borderColor);
        context.fill(cardX, cardY + cardHeight - 2, cardX + CARD_WIDTH, cardY + cardHeight, borderColor);
        context.fill(cardX, cardY, cardX + 2, cardY + cardHeight, borderColor);
        context.fill(cardX + CARD_WIDTH - 2, cardY, cardX + CARD_WIDTH, cardY + cardHeight, borderColor);
        
        // Step indicator (dots)
        int dotsY = cardY + CARD_PADDING;
        int dotsX = cardX + (CARD_WIDTH - STEPS.size() * 12) / 2;
        for (int i = 0; i < STEPS.size(); i++) {
            int dotColor = i == currentStep ? DOT_ACTIVE : DOT_INACTIVE;
            int dotAlpha = (int) (0xFF * cardAlpha);
            context.fill(dotsX + i * 12, dotsY, dotsX + i * 12 + 8, dotsY + 8, 
                        (dotAlpha << 24) | (dotColor & 0x00FFFFFF));
        }
        
        // Title
        int titleY = dotsY + 20;
        context.drawCenteredTextWithShadow(textRenderer, step.title,
                cardX + CARD_WIDTH / 2, titleY, TEXT_TITLE);
        
        // Body text
        int bodyY = titleY + 20;
        String[] lines = step.description.split("\n");
        for (String line : lines) {
            context.drawCenteredTextWithShadow(textRenderer, line, 
                    cardX + CARD_WIDTH / 2, bodyY, TEXT_BODY);
            bodyY += 12;
        }
        
        // Buttons
        int buttonsY = cardY + cardHeight - CARD_PADDING - BUTTON_HEIGHT;
        
        // Update hover states
        prevHovered = currentStep > 0 && 
                     mouseX >= cardX + CARD_PADDING && mouseX < cardX + CARD_PADDING + BUTTON_WIDTH &&
                     mouseY >= buttonsY && mouseY < buttonsY + BUTTON_HEIGHT;
        
        int nextX = cardX + CARD_WIDTH - CARD_PADDING - BUTTON_WIDTH;
        nextHovered = mouseX >= nextX && mouseX < nextX + BUTTON_WIDTH &&
                     mouseY >= buttonsY && mouseY < buttonsY + BUTTON_HEIGHT;
        
        int skipX = cardX + (CARD_WIDTH - 40) / 2;
        skipHovered = mouseX >= skipX && mouseX < skipX + 40 &&
                     mouseY >= buttonsY && mouseY < buttonsY + BUTTON_HEIGHT;
        
        // Previous button (if not first step)
        if (currentStep > 0) {
            int prevBg = prevHovered ? BUTTON_HOVER : BUTTON_BG;
            context.fill(cardX + CARD_PADDING, buttonsY, 
                        cardX + CARD_PADDING + BUTTON_WIDTH, buttonsY + BUTTON_HEIGHT, prevBg);
            context.drawCenteredTextWithShadow(textRenderer, "← Previous",
                    cardX + CARD_PADDING + BUTTON_WIDTH / 2, buttonsY + 8, BUTTON_TEXT);
        }
        
        // Skip button
        int skipBg = skipHovered ? BUTTON_HOVER : BUTTON_BG;
        context.fill(skipX, buttonsY, skipX + 40, buttonsY + BUTTON_HEIGHT, skipBg);
        context.drawCenteredTextWithShadow(textRenderer, "Skip", skipX + 20, buttonsY + 8, 0xFF888888);
        
        // Next/Finish button
        String nextText = currentStep == STEPS.size() - 1 ? "Got it!" : "Next →";
        int nextBg = nextHovered ? BUTTON_HOVER : CARD_BORDER;
        context.fill(nextX, buttonsY, nextX + BUTTON_WIDTH, buttonsY + BUTTON_HEIGHT, nextBg);
        context.drawCenteredTextWithShadow(textRenderer, nextText,
                nextX + BUTTON_WIDTH / 2, buttonsY + 8, BUTTON_TEXT);
    }
    
    private int calculateCardHeight(GuideStep step) {
        int lines = step.description.split("\n").length;
        return CARD_PADDING * 2 + // Top/bottom padding
               8 + 20 +           // Dots + spacing
               12 + 20 +          // Title + spacing  
               lines * 12 + 20 +  // Body text + spacing
               BUTTON_HEIGHT;     // Buttons
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isClosing) return false;
        
        GuideStep step = STEPS.get(currentStep);
        int cardHeight = calculateCardHeight(step);
        int cardX = (screenWidth - CARD_WIDTH) / 2;
        int cardY = (screenHeight - cardHeight) / 2;
        
        if (step.position == GuidePosition.LEFT) {
            cardX = 220;
        } else if (step.position == GuidePosition.RIGHT) {
            cardX = screenWidth - CARD_WIDTH - 20;
        }
        
        int buttonsY = cardY + cardHeight - CARD_PADDING - BUTTON_HEIGHT;
        
        // Check Previous button
        if (prevHovered && currentStep > 0) {
            currentStep--;
            slideProgress = 0f;
            return true;
        }
        
        // Check Skip button
        if (skipHovered) {
            close();
            return true;
        }
        
        // Check Next/Finish button
        if (nextHovered) {
            if (currentStep == STEPS.size() - 1) {
                close();
            } else {
                currentStep++;
                slideProgress = 0f;
            }
            return true;
        }
        
        return true; // Consume all clicks while guide is open
    }
    
    public void close() {
        isClosing = true;
    }
    
    public boolean isClosed() {
        return isClosing && fadeProgress <= 0.01f;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    // Guide step data
    private static class GuideStep {
        final String title;
        final String description;
        final GuidePosition position;
        
        GuideStep(String title, String description, GuidePosition position) {
            this.title = title;
            this.description = description;
            this.position = position;
        }
    }
    
    private enum GuidePosition {
        LEFT, CENTER, RIGHT
    }
}
