// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MatcherTest
{
    public static void main(final String[] args) throws Exception {
        final Pattern pattern = Pattern.compile("(?:http://)([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})");
        final String body = "This is a test http://12.345.6.78 and so on and [9.87.65.43] yadda but http://1.2.3.4 or http://9.8.7.6";
        final Matcher matcher = pattern.matcher(body);
        final List<MatchEntry> matches = new ArrayList<MatchEntry>();
        while (matcher.find()) {
            matches.add(new MatchEntry(matcher));
        }
        for (MatchEntry entry : matches) {
            System.out.println(entry.getValue() + " starts at " + entry.getStart() + " and ends at " + entry.getEnd());
        }
        final String template = "<a href=\"${4}:${3}:${2}:${1}\">${0}</a>";
        final StringBuilder sb = new StringBuilder();
        int sidx = 0;
        for (MatchEntry entry2 : matches) {
            String expandedTemplate = template;
            expandedTemplate = expandedTemplate.replaceAll("\\$\\{0}", entry2.value);
            for (int j = 0; j < entry2.getParts().size(); ++j) {
                expandedTemplate = expandedTemplate.replaceAll("\\$\\{" + (j + 1) + "\\}", entry2.getParts().get(j).value);
            }
            System.out.println("Expanded: " + expandedTemplate);
            sb.append(body, sidx, entry2.getStart());
            sb.append(expandedTemplate);
            sidx = entry2.getEnd();
        }
        sb.append(body, sidx, body.length());
        System.out.println("Done: " + String.valueOf(sb));
    }
    
    private static class MatchEntry
    {
        private final int start;
        private final int end;
        private final String value;
        private final List<MatchEntry> parts;
        
        private MatchEntry(final Matcher matcher) {
            this.parts = new ArrayList<MatchEntry>();
            this.value = matcher.group();
            this.start = matcher.start();
            this.end = matcher.end();
            if (matcher.groupCount() > 1) {
                for (int i = 1; i <= matcher.groupCount(); ++i) {
                    this.parts.add(new MatchEntry(matcher.group(i), matcher.start(i), matcher.end(i)));
                }
            }
        }
        
        private MatchEntry(final String value, final int start, final int end) {
            this.parts = new ArrayList<MatchEntry>();
            this.value = value;
            this.start = start;
            this.end = end;
        }
        
        public int getStart() {
            return this.start;
        }
        
        public int getEnd() {
            return this.end;
        }
        
        public String getValue() {
            return this.value;
        }
        
        public List<MatchEntry> getParts() {
            return this.parts;
        }
        
        @Override
        public String toString() {
            String subs = "";
            for (MatchEntry part : this.parts) {
                subs = subs + ", part " + part.toString();
            }
            return this.value + "[" + this.start + "-" + this.end + "]" + subs;
        }
    }
}
