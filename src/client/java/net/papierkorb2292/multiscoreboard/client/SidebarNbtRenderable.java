package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.*;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagVisitor;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class SidebarNbtRenderable implements SidebarRenderable {
    private static final int MAX_ENTRIES = 15;
    private static final int KEY_VALUE_DISTANCE = 10;

    private static final ChatFormatting TAG_FORMATTING = ChatFormatting.AQUA;
    private static final ChatFormatting STRING_FORMATTING = ChatFormatting.GREEN;
    private static final ChatFormatting NUMBER_FORMATTING = ChatFormatting.GOLD;
    private static final ChatFormatting TYPE_SUFFIX_FORMATTING = ChatFormatting.RED;

    private final String nbtSidebarName;
    private final List<Tag> nbt;

    public SidebarNbtRenderable(String nbtSidebarName, List<Tag> nbt) {
        this.nbtSidebarName = nbtSidebarName;
        this.nbt = nbt;
    }

    @Override
    public String getSortName() {
        return nbtSidebarName;
    }

    @Override
    public void render(GuiGraphics context, Gui inGameHud) {
        var textRenderer = Minecraft.getInstance().font;
        var title = Component.nullToEmpty(nbtSidebarName);
        var entries = new ArrayList<List<TopLevelNbtVisitor.Entry>>();
        if(!nbt.isEmpty()) {
            var content = new TopLevelNbtVisitor(buildEntryDistribution(), entries);
            for (Tag element : nbt) {
                element.accept(content);
            }
        } else {
            entries.add(List.of(new TopLevelNbtVisitor.Entry(null, MultiScoreboardClient.NO_DATA_TEXT)));
        }
        int entryCount = getTotalEntriesCount();
        int titleWidth = textRenderer.width(title);
        int entriesHeight = entryCount * textRenderer.lineHeight;
        int maxWidth = titleWidth;
        for(var section : entries) {
            for(var entry : section) {
                int width = textRenderer.width(entry.value);
                if(entry.key != null) {
                    width += textRenderer.width(entry.key) + KEY_VALUE_DISTANCE;
                }
                maxWidth = Math.max(maxWidth, width);
            }
        }
        int lowerY = context.guiHeight() / 2 + entriesHeight;
        int border = 3;
        int leftX = context.guiWidth() - maxWidth - border;
        int rightX = context.guiWidth() - border + 2;
        int entryBackgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.3f);
        int titleBackgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.4f);
        int titleLowerY = lowerY - entriesHeight;
        context.fill(leftX - 2, titleLowerY - textRenderer.lineHeight - 1, rightX, titleLowerY - 1, titleBackgroundColor);
        context.fill(leftX - 2, titleLowerY - 1, rightX, lowerY, entryBackgroundColor);
        context.drawString(textRenderer, title, leftX + maxWidth / 2 - titleWidth / 2, titleLowerY - textRenderer.lineHeight, CommonColors.WHITE, false);
        var i = 0;
        for(var section : entries) {
            for(var entry : section) {
                int entryY = lowerY - (entryCount - i++) * textRenderer.lineHeight;
                var valueWidth = textRenderer.width(entry.value);
                if(entry.key == null) {
                    context.drawString(textRenderer, entry.value, leftX + maxWidth / 2 - valueWidth / 2, entryY, CommonColors.WHITE, false);
                    continue;
                }
                context.drawString(textRenderer, entry.key, leftX, entryY, CommonColors.WHITE, false);
                context.drawString(textRenderer, entry.value, leftX + maxWidth - valueWidth, entryY, CommonColors.WHITE, false);
            }
            if(i != entryCount) {
                int separationY = lowerY - (entryCount - i) * textRenderer.lineHeight;
                context.fill(leftX - 2, separationY - 1, rightX, separationY, 0xAA666666);
            }

        }
    }

    private int countElements(Tag element) {
        if (element instanceof CollectionTag list) {
            return list.size();
        }
        if (element instanceof CompoundTag compound) {
            return compound.keySet().size();
        }
        return 1;
    }

    private int getTotalEntriesCount() {
        if(nbt.isEmpty()) return 1; // Still displaying an info text that there is no data
        return Math.min(nbt.stream().mapToInt(this::countElements).sum(), MAX_ENTRIES);
    }

    private int[] buildEntryDistribution() {
        var result = new int[nbt.size()];
        var remainingEntries = getTotalEntriesCount();
        while(remainingEntries > 0) {
            for(int i = 0; i < result.length; i++) {
                if(result[i] >= countElements(nbt.get(i)))
                    continue;
                result[i]++;
                if(--remainingEntries < 1)
                    break;
            }
        }
        return result;
    }

    @Override
    public int calculateHeight() {
        return (1 + getTotalEntriesCount()) * Minecraft.getInstance().font.lineHeight;
    }

    private static MutableComponent getIndexText(int index) {
        return Component.literal(String.valueOf(index)).withStyle(style -> style.applyFormat(TAG_FORMATTING));
    }

    private static Pattern TAG_QUOTATION_UNNECESSARY_PATTERN = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");

    private static MutableComponent getTagText(String tag) {
        if(!TAG_QUOTATION_UNNECESSARY_PATTERN.matcher(tag).matches())
            tag = StringTag.quoteAndEscape(tag);
        return Component.literal(tag).withStyle(style -> style.applyFormat(TAG_FORMATTING));
    }

    private static class NestedNbtVisitor implements TagVisitor {

        private int entriesLeft = 3;

        public MutableComponent text = Component.literal("");

        @Override
        public void visitString(StringTag element) {
            text.append(Component.literal(element.toString())
                    .withStyle(style -> style.applyFormat(STRING_FORMATTING))
            );
        }

        @Override
        public void visitByte(ByteTag element) {
            text.append(Component.literal(String.valueOf(element.byteValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
                    .append(Component.literal("b")
                            .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitShort(ShortTag element) {
            text.append(Component.literal(String.valueOf(element.shortValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
                    .append(Component.literal("s")
                            .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitInt(IntTag element) {
            text.append(Component.literal(String.valueOf(element.intValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
            );
        }

        @Override
        public void visitLong(LongTag element) {
            text.append(Component.literal(String.valueOf(element.longValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
                    .append(Component.literal("l")
                            .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitFloat(FloatTag element) {
            text.append(Component.literal(String.format("%.2f", element.floatValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
                    .append(Component.literal("f")
                            .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitDouble(DoubleTag element) {
            text.append(Component.literal(String.format("%.2f", element.doubleValue()))
                    .withStyle(style -> style.applyFormat(NUMBER_FORMATTING))
                    .append(Component.literal("d")
                            .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitByteArray(ByteArrayTag element) {
            visitAbstractList(element, "B");
        }

        @Override
        public void visitIntArray(IntArrayTag element) {
            visitAbstractList(element, "I");
        }

        @Override
        public void visitLongArray(LongArrayTag element) {
            visitAbstractList(element, "L");
        }

        @Override
        public void visitList(ListTag element) {
            visitAbstractList(element, null);
        }

        private void visitAbstractList(CollectionTag element, @Nullable String typePrefix) {
            text.append(Component.literal("["));
            if(typePrefix != null) {
                text.append(Component.literal(typePrefix)
                        .withStyle(style -> style.applyFormat(TYPE_SUFFIX_FORMATTING))
                ).append(Component.literal("; "));
            }
            for(int i = 0; i < element.size(); i++) {
                if(entriesLeft <= 0) {
                    text.append(Component.literal("...]"));
                    return;
                }
                element.get(i).accept(this);
                entriesLeft--;
                if (i < element.size() - 1) {
                    text.append(Component.literal(", "));
                }
            }
            text.append(Component.literal("]"));
        }

        @Override
        public void visitCompound(CompoundTag compound) {
            text.append(Component.literal("{"));
            int i = 0;
            var keys = compound.keySet().stream().sorted().iterator();
            while(keys.hasNext()) {
                var key = keys.next();
                if(entriesLeft <= 0) {
                    text.append(Component.literal("...}"));
                    return;
                }
                text.append(getTagText(key).append(Component.nullToEmpty(": ")));
                Objects.requireNonNull(compound.get(key)).accept(this);
                entriesLeft--;
                if (i++ < compound.keySet().size() - 1) {
                    text.append(Component.literal(", "));
                }
            }
            text.append(Component.literal("}"));
        }

        @Override
        public void visitEnd(EndTag element) { }
    }

    private static class TopLevelNbtVisitor implements TagVisitor {

        public final int[] visitorEntryCount;
        public List<List<Entry>> entries;
        public int currentVisitorIndex = 0;

        public TopLevelNbtVisitor(int[] visitorEntryCount, List<List<Entry>> entries) {
            this.visitorEntryCount = visitorEntryCount;
            this.entries = entries;
        }

        @Override
        public void visitString(StringTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitString(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitByte(ByteTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitByte(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitShort(ShortTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitShort(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitInt(IntTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitInt(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitLong(LongTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitLong(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitFloat(FloatTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitFloat(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitDouble(DoubleTag element) {
            var allowedEntries = getNextEntryCount();
            if(allowedEntries == 0) return;
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitDouble(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitByteArray(ByteArrayTag element) {
            visitAbstractList(element);
        }

        @Override
        public void visitIntArray(IntArrayTag element) {
            visitAbstractList(element);
        }

        @Override
        public void visitLongArray(LongArrayTag element) {
            visitAbstractList(element);
        }

        @Override
        public void visitList(ListTag element) {
            visitAbstractList(element);
        }

        private void visitAbstractList(CollectionTag element) {
            var arrayEntries = new ArrayList<Entry>();
            var endIndex = Math.min(element.size(), getNextEntryCount());
            for(int i = 0; i < endIndex; i++) {
                var nestedVisitor = new NestedNbtVisitor();
                element.get(i).accept(nestedVisitor);
                arrayEntries.add(new Entry(getIndexText(i), nestedVisitor.text));
            }
            entries.add(arrayEntries);
        }

        @Override
        public void visitCompound(CompoundTag compound) {
            var arrayEntries = new ArrayList<Entry>();
            compound.keySet().stream().sorted().limit(getNextEntryCount()).forEach(key -> {
                var nestedVisitor = new NestedNbtVisitor();
                Objects.requireNonNull(compound.get(key)).accept(nestedVisitor);
                arrayEntries.add(new Entry(getTagText(key), nestedVisitor.text));
            });
            entries.add(arrayEntries);
        }

        @Override
        public void visitEnd(EndTag element) { }

        private int getNextEntryCount() {
            return visitorEntryCount[currentVisitorIndex++];
        }

        private record Entry(@Nullable Component key, Component value) { }
    }
}
