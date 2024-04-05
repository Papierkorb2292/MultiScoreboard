package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.nbt.*;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SidebarNbtRenderable implements SidebarRenderable {

    private static final Text NO_DATA_TEXT = Text.translatable("multiScoreboard.sidebarNbt.noData").styled(style -> style.withColor(Colors.RED));
    private static final int MAX_ENTRIES = 15;
    private static final int KEY_VALUE_DISTANCE = 10;

    private static final Formatting TAG_FORMATTING = Formatting.AQUA;
    private static final Formatting STRING_FORMATTING = Formatting.GREEN;
    private static final Formatting NUMBER_FORMATTING = Formatting.GOLD;
    private static final Formatting TYPE_SUFFIX_FORMATTING = Formatting.RED;

    private final String nbtSidebarName;
    private final List<NbtElement> nbt;

    public SidebarNbtRenderable(String nbtSidebarName, List<NbtElement> nbt) {
        this.nbtSidebarName = nbtSidebarName;
        this.nbt = nbt;
    }

    @Override
    public String getSortName() {
        return nbtSidebarName;
    }

    @Override
    public void render(DrawContext context) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var title = Text.of(nbtSidebarName);
        var entries = new ArrayList<List<TopLevelNbtVisitor.Entry>>();
        if(!nbt.isEmpty()) {
            var content = new TopLevelNbtVisitor(MAX_ENTRIES / nbt.size(), MAX_ENTRIES % nbt.size(), entries);
            for (NbtElement element : nbt) {
                element.accept(content);
            }
        } else {
            entries.add(List.of(new TopLevelNbtVisitor.Entry(null, NO_DATA_TEXT)));
        }
        int entryCount = getTotalEntriesCount();
        int titleWidth = textRenderer.getWidth(title);
        int entriesHeight = entryCount * textRenderer.fontHeight;
        int maxWidth = titleWidth;
        for(var section : entries) {
            for(var entry : section) {
                int width = textRenderer.getWidth(entry.value);
                if(entry.key != null) {
                    width += textRenderer.getWidth(entry.key) + KEY_VALUE_DISTANCE;
                }
                maxWidth = Math.max(maxWidth, width);
            }
        }
        int lowerY = context.getScaledWindowHeight() / 2 + entriesHeight;
        int border = 3;
        int leftX = context.getScaledWindowWidth() - maxWidth - border;
        int rightX = context.getScaledWindowWidth() - border + 2;
        int entryBackgroundColor = MinecraftClient.getInstance().options.getTextBackgroundColor(0.3f);
        int titleBackgroundColor = MinecraftClient.getInstance().options.getTextBackgroundColor(0.4f);
        int titleLowerY = lowerY - entriesHeight;
        context.fill(leftX - 2, titleLowerY - textRenderer.fontHeight - 1, rightX, titleLowerY - 1, titleBackgroundColor);
        context.fill(leftX - 2, titleLowerY - 1, rightX, lowerY, entryBackgroundColor);
        context.drawText(textRenderer, title, leftX + maxWidth / 2 - titleWidth / 2, titleLowerY - textRenderer.fontHeight, Colors.WHITE, false);
        var i = 0;
        for(var section : entries) {
            for(var entry : section) {
                int entryY = lowerY - (entryCount - i++) * textRenderer.fontHeight;
                var valueWidth = textRenderer.getWidth(entry.value);
                if(entry.key == null) {
                    context.drawText(textRenderer, entry.value, leftX + maxWidth / 2 - valueWidth / 2, entryY, Colors.WHITE, false);
                    continue;
                }
                context.drawText(textRenderer, entry.key, leftX, entryY, Colors.WHITE, false);
                context.drawText(textRenderer, entry.value, leftX + maxWidth - valueWidth, entryY, Colors.WHITE, false);
            }
        }
    }

    private int countElements(NbtElement element) {
        if (element instanceof AbstractNbtList<?> list) {
            return list.size();
        }
        if (element instanceof NbtCompound compound) {
            return compound.getKeys().size();
        }
        return 1;
    }

    private int getTotalEntriesCount() {
        if(nbt.isEmpty()) return 1; // Still displaying an info text that there is no data
        return Math.min(nbt.stream().mapToInt(this::countElements).sum(), MAX_ENTRIES);
    }

    @Override
    public int calculateHeight() {
        return (1 + getTotalEntriesCount()) * MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    private static MutableText getIndexText(int index) {
        return Text.literal(String.valueOf(index)).styled(style -> style.withFormatting(TAG_FORMATTING));
    }

    private static MutableText getTagText(String tag) {
        return Text.literal(tag).styled(style -> style.withFormatting(TAG_FORMATTING));
    }

    private static class NestedNbtVisitor implements NbtElementVisitor {

        private static final int MAX_ENTRIES = 3;

        public MutableText text = Text.literal("");

        @Override
        public void visitString(NbtString element) {
            text.append(Text.literal(element.toString())
                    .styled(style -> style.withFormatting(STRING_FORMATTING))
            );
        }

        @Override
        public void visitByte(NbtByte element) {
            text.append(Text.literal(String.valueOf(element.byteValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
                    .append(Text.literal("b")
                            .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitShort(NbtShort element) {
            text.append(Text.literal(String.valueOf(element.shortValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
                    .append(Text.literal("s")
                            .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitInt(NbtInt element) {
            text.append(Text.literal(String.valueOf(element.intValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
            );
        }

        @Override
        public void visitLong(NbtLong element) {
            text.append(Text.literal(String.valueOf(element.longValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
                    .append(Text.literal("l")
                            .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitFloat(NbtFloat element) {
            text.append(Text.literal(String.format("%.2f", element.floatValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
                    .append(Text.literal("f")
                            .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitDouble(NbtDouble element) {
            text.append(Text.literal(String.format("%.2f", element.doubleValue()))
                    .styled(style -> style.withFormatting(NUMBER_FORMATTING))
                    .append(Text.literal("d")
                            .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                    ));
        }

        @Override
        public void visitByteArray(NbtByteArray element) {
            visitAbstractList(element, "B");
        }

        @Override
        public void visitIntArray(NbtIntArray element) {
            visitAbstractList(element, "I");
        }

        @Override
        public void visitLongArray(NbtLongArray element) {
            visitAbstractList(element, "L");
        }

        @Override
        public void visitList(NbtList element) {
            visitAbstractList(element, null);
        }

        private void visitAbstractList(AbstractNbtList<?> element, @Nullable String typePrefix) {
            text.append(Text.literal("["));
            if(typePrefix != null) {
                text.append(Text.literal(typePrefix)
                        .styled(style -> style.withFormatting(TYPE_SUFFIX_FORMATTING))
                ).append(Text.literal("; "));
            }
            for (int i = 0; i < Math.min(element.size(), MAX_ENTRIES); i++) {
                element.get(i).accept(this);
                if (i < element.size() - 1) {
                    text.append(Text.literal(", "));
                }
            }
            if(element.size() > MAX_ENTRIES) {
                text.append(Text.literal("...]"));
            } else {
                text.append(Text.literal("]"));
            }
        }

        @Override
        public void visitCompound(NbtCompound compound) {
            text.append(Text.literal("{"));
            int i = 0;
            var keys = compound.getKeys().stream().sorted().limit(MAX_ENTRIES).iterator();
            while(keys.hasNext()) {
                var key = keys.next();
                text.append(getTagText(key).append(Text.of(": ")));
                Objects.requireNonNull(compound.get(key)).accept(this);
                if (i++ < compound.getKeys().size() - 1) {
                    text.append(Text.literal(", "));
                }
            }
            if(compound.getKeys().size() > MAX_ENTRIES) {
                text.append(Text.literal("...}"));
            } else {
                text.append(Text.literal("}"));
            }
        }

        @Override
        public void visitEnd(NbtEnd element) { }
    }

    private static class TopLevelNbtVisitor implements NbtElementVisitor {

        public final int entriesPerVisitor;
        public int entriesPerVisitorRest;
        public List<List<Entry>> entries;

        public TopLevelNbtVisitor(int entriesPerVisitor, int entriesPerVisitorRest, List<List<Entry>> entries) {
            this.entriesPerVisitor = entriesPerVisitor;
            this.entriesPerVisitorRest = entriesPerVisitorRest;
            this.entries = entries;
        }

        @Override
        public void visitString(NbtString element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitString(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitByte(NbtByte element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitByte(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitShort(NbtShort element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitShort(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitInt(NbtInt element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitInt(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitLong(NbtLong element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitLong(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitFloat(NbtFloat element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitFloat(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitDouble(NbtDouble element) {
            var nestedVisitor = new NestedNbtVisitor();
            nestedVisitor.visitDouble(element);
            entries.add(List.of(new Entry(null, nestedVisitor.text)));
        }

        @Override
        public void visitByteArray(NbtByteArray element) {
            visitAbstractList(element);
        }

        @Override
        public void visitIntArray(NbtIntArray element) {
            visitAbstractList(element);
        }

        @Override
        public void visitLongArray(NbtLongArray element) {
            visitAbstractList(element);
        }

        @Override
        public void visitList(NbtList element) {
            visitAbstractList(element);
        }

        private void visitAbstractList(AbstractNbtList<?> element) {
            var arrayEntries = new ArrayList<Entry>();
            for(int i = 0; i < Math.min(element.size(), getNextEntryCount()); i++) {
                var nestedVisitor = new NestedNbtVisitor();
                element.get(i).accept(nestedVisitor);
                arrayEntries.add(new Entry(getIndexText(i), nestedVisitor.text));
            }
            entries.add(arrayEntries);
        }

        @Override
        public void visitCompound(NbtCompound compound) {
            var arrayEntries = new ArrayList<Entry>();
            compound.getKeys().stream().sorted().limit(getNextEntryCount()).forEach(key -> {
                var nestedVisitor = new NestedNbtVisitor();
                Objects.requireNonNull(compound.get(key)).accept(nestedVisitor);
                arrayEntries.add(new Entry(getTagText(key), nestedVisitor.text));
            });
            entries.add(arrayEntries);
        }

        @Override
        public void visitEnd(NbtEnd element) { }

        private int getNextEntryCount() {
            if(entriesPerVisitorRest > 0) {
                entriesPerVisitorRest--;
                return entriesPerVisitor + 1;
            }
            return entriesPerVisitor;
        }

        private record Entry(@Nullable Text key, Text value) { }
    }
}
