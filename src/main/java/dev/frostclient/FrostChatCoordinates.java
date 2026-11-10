package dev.frostclient;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates local-only clickable chat spans. Nothing is sent to the server until clicked. */
public final class FrostChatCoordinates {
    private static final Pattern XYZ=Pattern.compile("(?<![0-9])(-?[0-9]{1,7})[,\\s]+(-?[0-9]{1,7})[,\\s]+(-?[0-9]{1,7})(?![0-9])");
    private FrostChatCoordinates() {}

    public static Component linkify(Component original) {
        if(original == null || !FrostClient.CONFIG.enabled("Chat Coordinate Detection") ||
           !XYZ.matcher(original.getString()).find()) return original;
        MutableComponent result=Component.empty();
        final boolean[] changed={false};
        // Retain each styled text span (prefixes, names, etc.) while styling only XYZ.
        original.visit((style,text)->{
            if(style.getClickEvent() != null) {
                result.append(Component.literal(text).setStyle(style));
                return java.util.Optional.empty();
            }
            Matcher m=XYZ.matcher(text);
            int start=0;
            while(m.find()) {
                if(m.start()>start) result.append(Component.literal(text.substring(start,m.start())).setStyle(style));
                String args=m.group(1)+" "+m.group(2)+" "+m.group(3);
                Style link=style.withColor(0x69DDF5).withUnderlined(true)
                    .withClickEvent(new ClickEvent.RunCommand("/frostwaypoint "+args));
                result.append(Component.literal(m.group()).setStyle(link));
                changed[0]=true;
                start=m.end();
            }
            if(start<text.length())result.append(Component.literal(text.substring(start)).setStyle(style));
            return java.util.Optional.empty();
        },Style.EMPTY);
        return changed[0] ? result : original;
    }
}
