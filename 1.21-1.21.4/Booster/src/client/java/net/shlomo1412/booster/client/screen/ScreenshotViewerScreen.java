package net.shlomo1412.booster.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A comprehensive screenshot viewer screen with gallery, preview, zoom, and utilities.
 * Features: Gallery view, full-screen preview, zoom controls, quick edits, folder access.
 */
public class ScreenshotViewerScreen extends Screen {
    
    // Layout constants
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 36;
    private static final int THUMBNAIL_SIZE = 120;
    private static final int THUMBNAIL_PADDING = 8;
    private static final int GALLERY_PADDING = 16;
    
    // Colors
    private static final int BG_COLOR = 0xFF0A0A0A;
    private static final int HEADER_BG = 0xFF1A1A1A;
    private static final int FOOTER_BG = 0xFF1A1A1A;
    private static final int CARD_BG = 0xFF1E1E1E;
    private static final int CARD_HOVER = 0xFF2A2A2A;
    private static final int CARD_SELECTED = 0xFF333333;
    private static final int ACCENT_COLOR = 0xFFFFAA00;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;
    private static final int BUTTON_BG = 0xFF333333;
    private static final int BUTTON_HOVER = 0xFF444444;
    private static final int DANGER_COLOR = 0xFFFF4444;
    
    // View modes
    private enum ViewMode {
        GALLERY,
        PREVIEW
    }
    
    // Sort modes
    private enum SortMode {
        DATE_DESC("Newest First"),
        DATE_ASC("Oldest First"),
        NAME_ASC("Name A-Z"),
        NAME_DESC("Name Z-A"),
        SIZE_DESC("Largest First"),
        SIZE_ASC("Smallest First");
        
        private final String displayName;
        
        SortMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Screenshot entry data
    private static class ScreenshotEntry {
        final File file;
        final String name;
        final long lastModified;
        final long fileSize;
        Identifier textureId;
        boolean textureLoaded = false;
        boolean loadingStarted = false;
        int imageWidth;
        int imageHeight;
        
        ScreenshotEntry(File file) {
            this.file = file;
            this.name = file.getName();
            this.lastModified = file.lastModified();
            this.fileSize = file.length();
        }
        
        String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(lastModified));
        }
        
        String getFormattedSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    // State
    private final Screen parent;
    private ViewMode viewMode = ViewMode.GALLERY;
    private SortMode sortMode = SortMode.DATE_DESC;
    private final List<ScreenshotEntry> screenshots = new ArrayList<>();
    private final List<ScreenshotEntry> filteredScreenshots = new ArrayList<>();
    private ScreenshotEntry selectedScreenshot = null;
    private ScreenshotEntry hoveredScreenshot = null;
    
    // Gallery state
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private int columnsCount = 4;
    
    // Preview state
    private float zoomLevel = 1.0f;
    private float targetZoomLevel = 1.0f;
    private float panX = 0f;
    private float panY = 0f;
    private float targetPanX = 0f;
    private float targetPanY = 0f;
    private boolean isPanning = false;
    private int lastMouseX, lastMouseY;
    
    // Search
    private TextFieldWidget searchField;
    private String searchQuery = "";
    
    // Texture cache
    private final Map<File, Identifier> textureCache = new ConcurrentHashMap<>();
    private final Map<Identifier, NativeImageBackedTexture> loadedTextures = new ConcurrentHashMap<>();
    
    // UI widgets
    private ButtonWidget openFolderButton;
    private ButtonWidget sortButton;
    private ButtonWidget refreshButton;
    private ButtonWidget deleteButton;
    private ButtonWidget copyButton;
    private ButtonWidget renameButton;
    
    // Animation
    private float animationProgress = 0f;
    private static final float ANIMATION_SPEED = 0.15f;
    private static final float SCROLL_SMOOTHING = 0.2f;
    private static final float ZOOM_SMOOTHING = 0.15f;
    
    // Rename dialog
    private boolean showRenameDialog = false;
    private TextFieldWidget renameField;
    
    // Delete confirmation
    private boolean showDeleteConfirm = false;
    private int deleteConfirmTicks = 0;
    
    // Info panel toggle
    private boolean showInfoPanel = true;
    
    public ScreenshotViewerScreen(Screen parent) {
        super(Text.literal("Screenshot Viewer"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Initialize search field in header
        int searchWidth = Math.min(200, width / 4);
        searchField = new TextFieldWidget(
            textRenderer,
            width / 2 - searchWidth / 2,
            10,
            searchWidth,
            20,
            Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("Search screenshots..."));
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);
        
        // Open folder button (left side of header)
        openFolderButton = ButtonWidget.builder(Text.literal("📁 Open Folder"), button -> openScreenshotsFolder())
            .dimensions(10, 8, 100, 20)
            .build();
        addDrawableChild(openFolderButton);
        
        // Refresh button
        refreshButton = ButtonWidget.builder(Text.literal("🔄"), button -> refreshScreenshots())
            .dimensions(115, 8, 24, 20)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Refresh")))
            .build();
        addDrawableChild(refreshButton);
        
        // Sort button (right side of header)
        sortButton = ButtonWidget.builder(Text.literal("⇅ " + sortMode.getDisplayName()), button -> cycleSortMode())
            .dimensions(width - 160, 8, 150, 20)
            .build();
        addDrawableChild(sortButton);
        
        // Footer buttons (only visible when a screenshot is selected)
        int footerButtonY = height - FOOTER_HEIGHT + 8;
        int buttonWidth = 80;
        int buttonSpacing = 8;
        int totalButtonsWidth = buttonWidth * 4 + buttonSpacing * 3;
        int startX = width / 2 - totalButtonsWidth / 2;
        
        copyButton = ButtonWidget.builder(Text.literal("📋 Copy"), button -> copySelectedScreenshot())
            .dimensions(startX, footerButtonY, buttonWidth, 20)
            .build();
        addDrawableChild(copyButton);
        
        renameButton = ButtonWidget.builder(Text.literal("✏️ Rename"), button -> showRenameDialog())
            .dimensions(startX + buttonWidth + buttonSpacing, footerButtonY, buttonWidth, 20)
            .build();
        addDrawableChild(renameButton);
        
        deleteButton = ButtonWidget.builder(Text.literal("🗑️ Delete"), button -> showDeleteConfirmation())
            .dimensions(startX + (buttonWidth + buttonSpacing) * 2, footerButtonY, buttonWidth, 20)
            .build();
        addDrawableChild(deleteButton);
        
        ButtonWidget closeButton = ButtonWidget.builder(Text.literal("✕ Close"), button -> close())
            .dimensions(startX + (buttonWidth + buttonSpacing) * 3, footerButtonY, buttonWidth, 20)
            .build();
        addDrawableChild(closeButton);
        
        // Initialize rename field (hidden by default)
        renameField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2, 200, 20, Text.literal("Rename"));
        
        // Load screenshots
        loadScreenshots();
        updateFooterButtons();
    }
    
    private void loadScreenshots() {
        screenshots.clear();
        File screenshotsDir = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
        
        if (screenshotsDir.exists() && screenshotsDir.isDirectory()) {
            File[] files = screenshotsDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png") || 
                name.toLowerCase().endsWith(".jpg") || 
                name.toLowerCase().endsWith(".jpeg")
            );
            
            if (files != null) {
                for (File file : files) {
                    screenshots.add(new ScreenshotEntry(file));
                }
            }
        }
        
        sortScreenshots();
        updateFilteredScreenshots();
    }
    
    private void refreshScreenshots() {
        // Clear texture cache
        for (Map.Entry<Identifier, NativeImageBackedTexture> entry : loadedTextures.entrySet()) {
            entry.getValue().close();
        }
        loadedTextures.clear();
        textureCache.clear();
        
        // Reload
        loadScreenshots();
    }
    
    private void sortScreenshots() {
        Comparator<ScreenshotEntry> comparator = switch (sortMode) {
            case DATE_DESC -> Comparator.comparingLong((ScreenshotEntry e) -> e.lastModified).reversed();
            case DATE_ASC -> Comparator.comparingLong(e -> e.lastModified);
            case NAME_ASC -> Comparator.comparing(e -> e.name.toLowerCase());
            case NAME_DESC -> Comparator.comparing((ScreenshotEntry e) -> e.name.toLowerCase()).reversed();
            case SIZE_DESC -> Comparator.comparingLong((ScreenshotEntry e) -> e.fileSize).reversed();
            case SIZE_ASC -> Comparator.comparingLong(e -> e.fileSize);
        };
        
        screenshots.sort(comparator);
        updateFilteredScreenshots();
    }
    
    private void cycleSortMode() {
        SortMode[] modes = SortMode.values();
        int currentIndex = sortMode.ordinal();
        sortMode = modes[(currentIndex + 1) % modes.length];
        sortButton.setMessage(Text.literal("⇅ " + sortMode.getDisplayName()));
        sortScreenshots();
    }
    
    private void onSearchChanged(String query) {
        this.searchQuery = query.toLowerCase().trim();
        updateFilteredScreenshots();
    }
    
    private void updateFilteredScreenshots() {
        filteredScreenshots.clear();
        
        if (searchQuery.isEmpty()) {
            filteredScreenshots.addAll(screenshots);
        } else {
            for (ScreenshotEntry entry : screenshots) {
                if (entry.name.toLowerCase().contains(searchQuery)) {
                    filteredScreenshots.add(entry);
                }
            }
        }
        
        // Update scroll bounds
        calculateMaxScroll();
        
        // Keep selection if still visible
        if (selectedScreenshot != null && !filteredScreenshots.contains(selectedScreenshot)) {
            selectedScreenshot = null;
            viewMode = ViewMode.GALLERY;
        }
    }
    
    private void calculateMaxScroll() {
        int galleryWidth = width - GALLERY_PADDING * 2;
        columnsCount = Math.max(1, galleryWidth / (THUMBNAIL_SIZE + THUMBNAIL_PADDING));
        
        int rows = (int) Math.ceil((double) filteredScreenshots.size() / columnsCount);
        int totalHeight = rows * (THUMBNAIL_SIZE + THUMBNAIL_PADDING) + GALLERY_PADDING;
        int availableHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        
        maxScrollOffset = Math.max(0, totalHeight - availableHeight);
    }
    
    private void openScreenshotsFolder() {
        File screenshotsDir = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }
        Util.getOperatingSystem().open(screenshotsDir);
    }
    
    private void copySelectedScreenshot() {
        if (selectedScreenshot == null) return;
        
        // Copy file path to clipboard using Minecraft's clipboard (GLFW-based)
        // Note: GLFW clipboard only supports text, not images
        String path = selectedScreenshot.file.getAbsolutePath();
        MinecraftClient.getInstance().keyboard.setClipboard(path);
    }
    
    private void showRenameDialog() {
        if (selectedScreenshot == null) return;
        
        showRenameDialog = true;
        String currentName = selectedScreenshot.name;
        // Remove extension for editing
        int dotIndex = currentName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? currentName.substring(0, dotIndex) : currentName;
        
        renameField.setText(nameWithoutExt);
        renameField.setFocused(true);
        renameField.setSelectionStart(0);
        renameField.setSelectionEnd(nameWithoutExt.length());
        setFocused(renameField);
    }
    
    private void performRename() {
        if (selectedScreenshot == null || renameField.getText().trim().isEmpty()) {
            showRenameDialog = false;
            return;
        }
        
        String newName = renameField.getText().trim();
        String extension = "";
        int dotIndex = selectedScreenshot.name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = selectedScreenshot.name.substring(dotIndex);
        }
        
        File newFile = new File(selectedScreenshot.file.getParent(), newName + extension);
        if (!newFile.exists()) {
            if (selectedScreenshot.file.renameTo(newFile)) {
                refreshScreenshots();
            }
        }
        
        showRenameDialog = false;
    }
    
    private void showDeleteConfirmation() {
        if (selectedScreenshot == null) return;
        showDeleteConfirm = true;
        deleteConfirmTicks = 0;
    }
    
    private void performDelete() {
        if (selectedScreenshot == null) return;
        
        // Close texture if loaded
        Identifier textureId = textureCache.get(selectedScreenshot.file);
        if (textureId != null) {
            NativeImageBackedTexture texture = loadedTextures.remove(textureId);
            if (texture != null) {
                texture.close();
            }
            textureCache.remove(selectedScreenshot.file);
        }
        
        // Delete file
        if (selectedScreenshot.file.delete()) {
            screenshots.remove(selectedScreenshot);
            updateFilteredScreenshots();
            
            // Select next screenshot if available
            if (!filteredScreenshots.isEmpty()) {
                int index = filteredScreenshots.indexOf(selectedScreenshot);
                if (index >= 0 && index < filteredScreenshots.size()) {
                    selectedScreenshot = filteredScreenshots.get(Math.min(index, filteredScreenshots.size() - 1));
                } else {
                    selectedScreenshot = filteredScreenshots.get(0);
                }
            } else {
                selectedScreenshot = null;
                viewMode = ViewMode.GALLERY;
            }
        }
        
        showDeleteConfirm = false;
    }
    
    private void updateFooterButtons() {
        boolean hasSelection = selectedScreenshot != null;
        copyButton.active = hasSelection;
        renameButton.active = hasSelection;
        deleteButton.active = hasSelection;
    }
    
    private void loadThumbnail(ScreenshotEntry entry) {
        if (entry.loadingStarted) return;
        entry.loadingStarted = true;
        
        CompletableFuture.runAsync(() -> {
            try {
                // Check cache first
                if (textureCache.containsKey(entry.file)) {
                    entry.textureId = textureCache.get(entry.file);
                    entry.textureLoaded = true;
                    return;
                }
                
                // Load image
                try (InputStream is = new FileInputStream(entry.file)) {
                    NativeImage nativeImage = NativeImage.read(is);
                    entry.imageWidth = nativeImage.getWidth();
                    entry.imageHeight = nativeImage.getHeight();
                    
                    // Create texture on main thread
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                            Identifier id = Identifier.of("booster", "screenshot_" + entry.file.getName().hashCode() + "_" + System.nanoTime());
                            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                            
                            entry.textureId = id;
                            entry.textureLoaded = true;
                            textureCache.put(entry.file, id);
                            loadedTextures.put(id, texture);
                        } catch (Exception e) {
                            entry.textureLoaded = false;
                        }
                    });
                }
            } catch (Exception e) {
                entry.textureLoaded = false;
            }
        });
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Smooth animations
        animationProgress = Math.min(1f, animationProgress + delta * ANIMATION_SPEED);
        scrollOffset += (targetScrollOffset - scrollOffset) * SCROLL_SMOOTHING;
        zoomLevel += (targetZoomLevel - zoomLevel) * ZOOM_SMOOTHING;
        panX += (targetPanX - panX) * ZOOM_SMOOTHING;
        panY += (targetPanY - panY) * ZOOM_SMOOTHING;
        
        // Background
        context.fill(0, 0, width, height, BG_COLOR);
        
        if (viewMode == ViewMode.GALLERY) {
            renderGalleryView(context, mouseX, mouseY, delta);
        } else {
            renderPreviewView(context, mouseX, mouseY, delta);
        }
        
        // Header
        renderHeader(context, mouseX, mouseY);
        
        // Footer
        renderFooter(context, mouseX, mouseY);
        
        // Render widgets (buttons, text fields) BEFORE dialogs
        super.render(context, mouseX, mouseY, delta);
        
        // Dialogs rendered on top of everything at elevated Z-level
        if (showRenameDialog || showDeleteConfirm) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 500);
            
            if (showRenameDialog) {
                renderRenameDialog(context, mouseX, mouseY);
            }
            
            if (showDeleteConfirm) {
                renderDeleteConfirmation(context, mouseX, mouseY);
                deleteConfirmTicks++;
            }
            
            context.getMatrices().pop();
        }
    }
    
    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        // Header background
        context.fill(0, 0, width, HEADER_HEIGHT, HEADER_BG);
        context.fill(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, 0xFF333333);
        
        // Screenshot count
        String countText = filteredScreenshots.size() + " screenshot" + (filteredScreenshots.size() != 1 ? "s" : "");
        if (!searchQuery.isEmpty()) {
            countText += " (filtered)";
        }
        context.drawTextWithShadow(textRenderer, countText, 145, 14, TEXT_SECONDARY);
    }
    
    private void renderFooter(DrawContext context, int mouseX, int mouseY) {
        // Footer background
        context.fill(0, height - FOOTER_HEIGHT, width, height, FOOTER_BG);
        context.fill(0, height - FOOTER_HEIGHT, width, height - FOOTER_HEIGHT + 1, 0xFF333333);
        
        // Keyboard shortcuts hint (left side)
        String shortcuts = viewMode == ViewMode.PREVIEW 
            ? "ESC: Back | ←→: Navigate | +/-: Zoom | R: Reset | I: Info"
            : "Enter: Preview | Del: Delete | F5: Refresh";
        context.drawTextWithShadow(textRenderer, shortcuts, 10, height - FOOTER_HEIGHT + 13, TEXT_DIM);
    }
    
    private void renderGalleryView(DrawContext context, int mouseX, int mouseY, float delta) {
        int galleryTop = HEADER_HEIGHT;
        int galleryBottom = height - FOOTER_HEIGHT;
        int galleryHeight = galleryBottom - galleryTop;
        
        // Enable scissor for gallery area
        context.enableScissor(0, galleryTop, width, galleryBottom);
        
        hoveredScreenshot = null;
        
        int startX = GALLERY_PADDING;
        int startY = galleryTop + GALLERY_PADDING - (int) scrollOffset;
        
        for (int i = 0; i < filteredScreenshots.size(); i++) {
            ScreenshotEntry entry = filteredScreenshots.get(i);
            
            int col = i % columnsCount;
            int row = i / columnsCount;
            
            int x = startX + col * (THUMBNAIL_SIZE + THUMBNAIL_PADDING);
            int y = startY + row * (THUMBNAIL_SIZE + THUMBNAIL_PADDING);
            
            // Skip if not visible
            if (y + THUMBNAIL_SIZE < galleryTop || y > galleryBottom) continue;
            
            // Load texture if needed
            if (!entry.textureLoaded && !entry.loadingStarted) {
                loadThumbnail(entry);
            }
            
            // Check hover
            boolean isHovered = mouseX >= x && mouseX < x + THUMBNAIL_SIZE && 
                               mouseY >= y && mouseY < y + THUMBNAIL_SIZE &&
                               mouseY >= galleryTop && mouseY < galleryBottom;
            boolean isSelected = entry == selectedScreenshot;
            
            if (isHovered) {
                hoveredScreenshot = entry;
            }
            
            // Card background
            int cardColor = isSelected ? CARD_SELECTED : (isHovered ? CARD_HOVER : CARD_BG);
            context.fill(x - 2, y - 2, x + THUMBNAIL_SIZE + 2, y + THUMBNAIL_SIZE + 2, cardColor);
            
            // Selection border
            if (isSelected) {
                context.fill(x - 3, y - 3, x + THUMBNAIL_SIZE + 3, y - 2, ACCENT_COLOR);
                context.fill(x - 3, y + THUMBNAIL_SIZE + 2, x + THUMBNAIL_SIZE + 3, y + THUMBNAIL_SIZE + 3, ACCENT_COLOR);
                context.fill(x - 3, y - 2, x - 2, y + THUMBNAIL_SIZE + 2, ACCENT_COLOR);
                context.fill(x + THUMBNAIL_SIZE + 2, y - 2, x + THUMBNAIL_SIZE + 3, y + THUMBNAIL_SIZE + 2, ACCENT_COLOR);
            }
            
            // Thumbnail
            if (entry.textureLoaded && entry.textureId != null) {
                // Use actual image dimensions for texture size, render to THUMBNAIL_SIZE
                context.drawTexture(RenderLayer::getGuiTextured, entry.textureId, x, y, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, entry.imageWidth, entry.imageHeight);
            } else {
                // Loading placeholder
                context.fill(x, y, x + THUMBNAIL_SIZE, y + THUMBNAIL_SIZE, 0xFF2A2A2A);
                String loadingText = entry.loadingStarted ? "Loading..." : "...";
                int textWidth = textRenderer.getWidth(loadingText);
                context.drawTextWithShadow(textRenderer, loadingText, 
                    x + THUMBNAIL_SIZE / 2 - textWidth / 2, y + THUMBNAIL_SIZE / 2 - 4, TEXT_DIM);
            }
            
            // File name label (below thumbnail)
            String displayName = entry.name;
            if (textRenderer.getWidth(displayName) > THUMBNAIL_SIZE) {
                while (textRenderer.getWidth(displayName + "...") > THUMBNAIL_SIZE && displayName.length() > 3) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "...";
            }
            // Draw name in the card area below image
            context.drawTextWithShadow(textRenderer, displayName, x, y + THUMBNAIL_SIZE + 4, TEXT_SECONDARY);
        }
        
        context.disableScissor();
        
        // Scrollbar
        if (maxScrollOffset > 0) {
            int scrollbarX = width - 6;
            int scrollbarHeight = galleryHeight;
            float scrollRatio = scrollOffset / maxScrollOffset;
            float thumbHeight = Math.max(20, galleryHeight * (galleryHeight / (float)(galleryHeight + maxScrollOffset)));
            int thumbY = galleryTop + (int)((scrollbarHeight - thumbHeight) * scrollRatio);
            
            context.fill(scrollbarX, galleryTop, scrollbarX + 4, galleryBottom, 0x40FFFFFF);
            context.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + (int)thumbHeight, 0x80FFFFFF);
        }
        
        // Empty state
        if (filteredScreenshots.isEmpty()) {
            String message = screenshots.isEmpty() ? "No screenshots found" : "No matching screenshots";
            int textWidth = textRenderer.getWidth(message);
            context.drawCenteredTextWithShadow(textRenderer, message, width / 2, height / 2, TEXT_SECONDARY);
            
            if (screenshots.isEmpty()) {
                String hint = "Press F2 in-game to take a screenshot";
                context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, height / 2 + 16, TEXT_DIM);
            }
        }
    }
    
    private void renderPreviewView(DrawContext context, int mouseX, int mouseY, float delta) {
        if (selectedScreenshot == null || !selectedScreenshot.textureLoaded) {
            viewMode = ViewMode.GALLERY;
            return;
        }
        
        int previewTop = HEADER_HEIGHT;
        int previewBottom = height - FOOTER_HEIGHT;
        int previewWidth = showInfoPanel ? width - 200 : width;
        int previewHeight = previewBottom - previewTop;
        
        // Enable scissor for preview area
        context.enableScissor(0, previewTop, previewWidth, previewBottom);
        
        // Calculate image dimensions with zoom
        float imgWidth = selectedScreenshot.imageWidth * zoomLevel;
        float imgHeight = selectedScreenshot.imageHeight * zoomLevel;
        
        // Center image with pan offset
        float imgX = (previewWidth - imgWidth) / 2 + panX;
        float imgY = previewTop + (previewHeight - imgHeight) / 2 + panY;
        
        // Draw checkerboard background for transparency
        int checkSize = 8;
        for (int cy = previewTop; cy < previewBottom; cy += checkSize) {
            for (int cx = 0; cx < previewWidth; cx += checkSize) {
                int color = ((cx / checkSize + cy / checkSize) % 2 == 0) ? 0xFF1A1A1A : 0xFF222222;
                context.fill(cx, cy, Math.min(cx + checkSize, previewWidth), Math.min(cy + checkSize, previewBottom), color);
            }
        }
        
        // Draw image
        if (selectedScreenshot.textureId != null) {
            // Use actual image dimensions for texture size, render to scaled size
            context.drawTexture(RenderLayer::getGuiTextured, selectedScreenshot.textureId, 
                (int)imgX, (int)imgY, 0, 0, 
                (int)imgWidth, (int)imgHeight, 
                selectedScreenshot.imageWidth, selectedScreenshot.imageHeight);
        }
        
        context.disableScissor();
        
        // Zoom indicator
        String zoomText = String.format("%.0f%%", zoomLevel * 100);
        context.fill(10, previewTop + 10, 10 + textRenderer.getWidth(zoomText) + 8, previewTop + 26, 0xCC000000);
        context.drawTextWithShadow(textRenderer, zoomText, 14, previewTop + 14, TEXT_PRIMARY);
        
        // Navigation hint
        int navIndex = filteredScreenshots.indexOf(selectedScreenshot);
        if (navIndex >= 0) {
            String navText = (navIndex + 1) + " / " + filteredScreenshots.size();
            int navWidth = textRenderer.getWidth(navText);
            context.fill(previewWidth - navWidth - 18, previewTop + 10, previewWidth - 10, previewTop + 26, 0xCC000000);
            context.drawTextWithShadow(textRenderer, navText, previewWidth - navWidth - 14, previewTop + 14, TEXT_PRIMARY);
        }
        
        // Info panel (right side)
        if (showInfoPanel) {
            renderInfoPanel(context, mouseX, mouseY);
        }
    }
    
    private void renderInfoPanel(DrawContext context, int mouseX, int mouseY) {
        if (selectedScreenshot == null) return;
        
        int panelX = width - 200;
        int panelTop = HEADER_HEIGHT;
        int panelBottom = height - FOOTER_HEIGHT;
        
        // Panel background
        context.fill(panelX, panelTop, width, panelBottom, 0xFF1A1A1A);
        context.fill(panelX, panelTop, panelX + 1, panelBottom, 0xFF333333);
        
        int y = panelTop + 16;
        int labelX = panelX + 12;
        int valueX = panelX + 12;
        
        // Title
        context.drawTextWithShadow(textRenderer, "§l§6Screenshot Info", labelX, y, ACCENT_COLOR);
        y += 24;
        
        // File name
        context.drawTextWithShadow(textRenderer, "§7Name:", labelX, y, TEXT_DIM);
        y += 12;
        String fileName = selectedScreenshot.name;
        List<String> nameLines = wrapText(fileName, 176);
        for (String line : nameLines) {
            context.drawTextWithShadow(textRenderer, line, valueX, y, TEXT_PRIMARY);
            y += 10;
        }
        y += 8;
        
        // Date
        context.drawTextWithShadow(textRenderer, "§7Date:", labelX, y, TEXT_DIM);
        y += 12;
        context.drawTextWithShadow(textRenderer, selectedScreenshot.getFormattedDate(), valueX, y, TEXT_PRIMARY);
        y += 20;
        
        // File size
        context.drawTextWithShadow(textRenderer, "§7Size:", labelX, y, TEXT_DIM);
        y += 12;
        context.drawTextWithShadow(textRenderer, selectedScreenshot.getFormattedSize(), valueX, y, TEXT_PRIMARY);
        y += 20;
        
        // Dimensions
        if (selectedScreenshot.imageWidth > 0) {
            context.drawTextWithShadow(textRenderer, "§7Dimensions:", labelX, y, TEXT_DIM);
            y += 12;
            context.drawTextWithShadow(textRenderer, selectedScreenshot.imageWidth + " x " + selectedScreenshot.imageHeight, valueX, y, TEXT_PRIMARY);
            y += 20;
        }
        
        // Path
        context.drawTextWithShadow(textRenderer, "§7Location:", labelX, y, TEXT_DIM);
        y += 12;
        String path = selectedScreenshot.file.getParent();
        List<String> pathLines = wrapText(path, 176);
        for (String line : pathLines) {
            context.drawTextWithShadow(textRenderer, "§8" + line, valueX, y, TEXT_DIM);
            y += 10;
        }
        
        // Toggle hint at bottom
        context.drawTextWithShadow(textRenderer, "§8Press I to hide", panelX + 12, panelBottom - 20, TEXT_DIM);
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            currentLine.append(c);
            if (textRenderer.getWidth(currentLine.toString()) > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.isEmpty() ? List.of(text) : lines;
    }
    
    private void renderRenameDialog(DrawContext context, int mouseX, int mouseY) {
        // Dim background
        context.fill(0, 0, width, height, 0xA0000000);
        
        // Dialog box
        int dialogWidth = 300;
        int dialogHeight = 100;
        int dialogX = width / 2 - dialogWidth / 2;
        int dialogY = height / 2 - dialogHeight / 2;
        
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF1A1A1A);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 2, ACCENT_COLOR);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, "Rename Screenshot", width / 2, dialogY + 12, TEXT_PRIMARY);
        
        // Text field
        renameField.setX(dialogX + 20);
        renameField.setY(dialogY + 35);
        renameField.setWidth(dialogWidth - 40);
        renameField.render(context, mouseX, mouseY, 0);
        
        // Buttons
        int buttonY = dialogY + dialogHeight - 30;
        context.drawCenteredTextWithShadow(textRenderer, "§a[Enter] Confirm    §c[Esc] Cancel", width / 2, buttonY, TEXT_SECONDARY);
    }
    
    private void renderDeleteConfirmation(DrawContext context, int mouseX, int mouseY) {
        // Dim background
        context.fill(0, 0, width, height, 0xA0000000);
        
        // Dialog box
        int dialogWidth = 320;
        int dialogHeight = 110;
        int dialogX = width / 2 - dialogWidth / 2;
        int dialogY = height / 2 - dialogHeight / 2;
        
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF1A1A1A);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 2, DANGER_COLOR);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, "§c⚠ Delete Screenshot?", width / 2, dialogY + 12, DANGER_COLOR);
        
        // Message
        String fileName = selectedScreenshot != null ? selectedScreenshot.name : "";
        context.drawCenteredTextWithShadow(textRenderer, "\"" + fileName + "\"", width / 2, dialogY + 35, TEXT_PRIMARY);
        context.drawCenteredTextWithShadow(textRenderer, "§7This action cannot be undone!", width / 2, dialogY + 55, TEXT_SECONDARY);
        
        // Buttons
        int buttonY = dialogY + dialogHeight - 28;
        context.drawCenteredTextWithShadow(textRenderer, "§c[Enter] Delete    §a[Esc] Cancel", width / 2, buttonY, TEXT_SECONDARY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showRenameDialog) {
            if (renameField.isMouseOver(mouseX, mouseY)) {
                return renameField.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        
        if (showDeleteConfirm) {
            return true;
        }
        
        // Handle gallery clicks
        if (viewMode == ViewMode.GALLERY && button == 0) {
            if (hoveredScreenshot != null) {
                if (hoveredScreenshot == selectedScreenshot) {
                    // Double click to preview
                    viewMode = ViewMode.PREVIEW;
                    resetZoom();
                } else {
                    selectedScreenshot = hoveredScreenshot;
                }
                updateFooterButtons();
                return true;
            }
        }
        
        // Handle preview panning
        if (viewMode == ViewMode.PREVIEW && button == 0) {
            int previewWidth = showInfoPanel ? width - 200 : width;
            if (mouseX < previewWidth && mouseY > HEADER_HEIGHT && mouseY < height - FOOTER_HEIGHT) {
                isPanning = true;
                lastMouseX = (int) mouseX;
                lastMouseY = (int) mouseY;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isPanning = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isPanning && viewMode == ViewMode.PREVIEW) {
            targetPanX += mouseX - lastMouseX;
            targetPanY += mouseY - lastMouseY;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showRenameDialog || showDeleteConfirm) {
            return true;
        }
        
        if (viewMode == ViewMode.GALLERY) {
            targetScrollOffset = Math.max(0, Math.min(maxScrollOffset, targetScrollOffset - (float)(verticalAmount * 40)));
            return true;
        } else {
            // Zoom in preview mode
            float zoomDelta = (float) verticalAmount * 0.1f;
            targetZoomLevel = Math.max(0.1f, Math.min(10f, targetZoomLevel + zoomDelta * targetZoomLevel));
            return true;
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle rename dialog
        if (showRenameDialog) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                performRename();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showRenameDialog = false;
                return true;
            }
            return renameField.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Handle delete confirmation
        if (showDeleteConfirm) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                performDelete();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showDeleteConfirm = false;
                return true;
            }
            return true;
        }
        
        // Escape handling
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (viewMode == ViewMode.PREVIEW) {
                viewMode = ViewMode.GALLERY;
                return true;
            }
            close();
            return true;
        }
        
        // Search field focus
        if (searchField.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Gallery shortcuts
        if (viewMode == ViewMode.GALLERY) {
            // Enter to preview
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && selectedScreenshot != null) {
                viewMode = ViewMode.PREVIEW;
                resetZoom();
                return true;
            }
            
            // Delete key
            if (keyCode == GLFW.GLFW_KEY_DELETE && selectedScreenshot != null) {
                showDeleteConfirmation();
                return true;
            }
            
            // F5 refresh
            if (keyCode == GLFW.GLFW_KEY_F5) {
                refreshScreenshots();
                return true;
            }
            
            // Arrow key navigation in gallery
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT ||
                keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                navigateGallery(keyCode);
                return true;
            }
        }
        
        // Preview shortcuts
        if (viewMode == ViewMode.PREVIEW) {
            // Left/Right navigation
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                navigateScreenshot(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                navigateScreenshot(1);
                return true;
            }
            
            // Zoom controls
            if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
                targetZoomLevel = Math.min(10f, targetZoomLevel * 1.25f);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                targetZoomLevel = Math.max(0.1f, targetZoomLevel / 1.25f);
                return true;
            }
            
            // R to reset zoom
            if (keyCode == GLFW.GLFW_KEY_R) {
                resetZoom();
                return true;
            }
            
            // I to toggle info panel
            if (keyCode == GLFW.GLFW_KEY_I) {
                showInfoPanel = !showInfoPanel;
                return true;
            }
            
            // F to fit to screen
            if (keyCode == GLFW.GLFW_KEY_F) {
                fitToScreen();
                return true;
            }
            
            // 1 to actual size
            if (keyCode == GLFW.GLFW_KEY_1) {
                targetZoomLevel = 1.0f;
                targetPanX = 0;
                targetPanY = 0;
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void navigateGallery(int keyCode) {
        if (filteredScreenshots.isEmpty()) return;
        
        int currentIndex = selectedScreenshot != null ? filteredScreenshots.indexOf(selectedScreenshot) : -1;
        int newIndex = currentIndex;
        
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> newIndex = Math.max(0, currentIndex - 1);
            case GLFW.GLFW_KEY_RIGHT -> newIndex = Math.min(filteredScreenshots.size() - 1, currentIndex + 1);
            case GLFW.GLFW_KEY_UP -> newIndex = Math.max(0, currentIndex - columnsCount);
            case GLFW.GLFW_KEY_DOWN -> newIndex = Math.min(filteredScreenshots.size() - 1, currentIndex + columnsCount);
        }
        
        if (newIndex >= 0 && newIndex < filteredScreenshots.size()) {
            selectedScreenshot = filteredScreenshots.get(newIndex);
            updateFooterButtons();
            ensureSelectedVisible();
        }
    }
    
    private void ensureSelectedVisible() {
        if (selectedScreenshot == null) return;
        
        int index = filteredScreenshots.indexOf(selectedScreenshot);
        int row = index / columnsCount;
        int itemY = GALLERY_PADDING + row * (THUMBNAIL_SIZE + THUMBNAIL_PADDING);
        
        int viewTop = (int) scrollOffset;
        int viewBottom = viewTop + (height - HEADER_HEIGHT - FOOTER_HEIGHT);
        
        if (itemY < viewTop) {
            targetScrollOffset = Math.max(0, itemY - GALLERY_PADDING);
        } else if (itemY + THUMBNAIL_SIZE > viewBottom) {
            targetScrollOffset = Math.min(maxScrollOffset, itemY + THUMBNAIL_SIZE - (height - HEADER_HEIGHT - FOOTER_HEIGHT) + GALLERY_PADDING);
        }
    }
    
    private void navigateScreenshot(int direction) {
        if (filteredScreenshots.isEmpty() || selectedScreenshot == null) return;
        
        int currentIndex = filteredScreenshots.indexOf(selectedScreenshot);
        int newIndex = currentIndex + direction;
        
        if (newIndex >= 0 && newIndex < filteredScreenshots.size()) {
            selectedScreenshot = filteredScreenshots.get(newIndex);
            resetZoom();
            
            // Ensure new screenshot texture is loaded
            if (!selectedScreenshot.textureLoaded && !selectedScreenshot.loadingStarted) {
                loadThumbnail(selectedScreenshot);
            }
        }
    }
    
    private void resetZoom() {
        targetZoomLevel = 1.0f;
        targetPanX = 0;
        targetPanY = 0;
        zoomLevel = 1.0f;
        panX = 0;
        panY = 0;
    }
    
    private void fitToScreen() {
        if (selectedScreenshot == null || selectedScreenshot.imageWidth <= 0) return;
        
        int previewWidth = showInfoPanel ? width - 200 : width;
        int previewHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        
        float scaleX = (float) previewWidth / selectedScreenshot.imageWidth;
        float scaleY = (float) previewHeight / selectedScreenshot.imageHeight;
        
        targetZoomLevel = Math.min(scaleX, scaleY) * 0.95f; // 95% to add some padding
        targetPanX = 0;
        targetPanY = 0;
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showRenameDialog) {
            return renameField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public void close() {
        // Clean up textures
        for (Map.Entry<Identifier, NativeImageBackedTexture> entry : loadedTextures.entrySet()) {
            entry.getValue().close();
        }
        loadedTextures.clear();
        textureCache.clear();
        
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
