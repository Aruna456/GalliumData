// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import java.util.Iterator;
import com.galliumdata.server.ServerException;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FilterUtils
{
    public static Set<String> readCommaSeparatedUpperCaseNames(final String typesStr) {
        Set<String> types = null;
        if (typesStr != null && typesStr.trim().length() > 0) {
            final String[] split;
            final String[] nameLines = split = typesStr.split("\\n");
            for (final String nameLine : split) {
                final String[] split2;
                final String[] typesParts = split2 = nameLine.split(",");
                for (final String typePart : split2) {
                    if (typePart.trim().length() != 0) {
                        if (types == null) {
                            types = new HashSet<String>();
                        }
                        types.add(typePart.trim().toUpperCase());
                    }
                }
            }
        }
        return types;
    }
    
    public static Set<String> readCommaSeparatedNames(final String typesStr) {
        Set<String> types = null;
        if (typesStr != null && typesStr.trim().length() > 0) {
            final String[] split;
            final String[] nameLines = split = typesStr.split("\\n");
            for (final String nameLine : split) {
                final String[] split2;
                final String[] typesParts = split2 = nameLine.split(",");
                for (final String typePart : split2) {
                    if (typePart.trim().length() != 0) {
                        if (types == null) {
                            types = new HashSet<String>();
                        }
                        types.add(typePart.trim());
                    }
                }
            }
        }
        return types;
    }
    
    public static Set<Object> readCommaSeparatedNamesOrRegexes(final String s) {
        if (s == null) {
            return null;
        }
        final List<Object> result = readListOfCommaSeparatedNamesOrRegexes(s);
        return new HashSet<Object>(result);
    }
    
    public static List<Object> readListOfCommaSeparatedNamesOrRegexes(final String s) {
        List<Object> result = null;
        if (s != null && s.trim().length() > 0) {
            final String[] split;
            final String[] nameLines = split = s.split("\\n");
            for (String nameLine : split) {
                nameLine = nameLine.trim();
                if (nameLine.length() != 0) {
                    final String[] split2;
                    final String[] nameParts = split2 = nameLine.split("(?<!\\\\),");
                    for (String namePart : split2) {
                        if (result == null) {
                            result = new ArrayList<Object>();
                        }
                        namePart = namePart.replaceAll("\\\\,", ",");
                        namePart = namePart.trim();
                        if (namePart.length() != 0) {
                            if (namePart.startsWith("regex:")) {
                                final String regex = namePart.substring("regex:".length()).trim();
                                try {
                                    final Pattern pattern = Pattern.compile(regex, 106);
                                    result.add(pattern);
                                }
                                catch (final Exception ex) {
                                    throw new ServerException("repo.InvalidRegex", new Object[] { namePart, ex.getMessage() });
                                }
                            }
                            else if (namePart.startsWith("REGEX:")) {
                                final String regex = namePart.substring("REGEX:".length()).trim();
                                try {
                                    final Pattern pattern = Pattern.compile(regex);
                                    result.add(pattern);
                                }
                                catch (final Exception ex) {
                                    throw new ServerException("repo.InvalidRegex", new Object[] { namePart, ex.getMessage() });
                                }
                            }
                            else {
                                result.add(namePart);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public static List<Object> readListOfNamesOrRegexes(final String s) {
        List<Object> result = null;
        if (s != null && !s.trim().isEmpty()) {
            final String[] split;
            final String[] nameLines = split = s.split("\\n");
            for (String nameLine : split) {
                nameLine = nameLine.trim();
                if (nameLine.length() != 0) {
                    if (result == null) {
                        result = new ArrayList<Object>();
                    }
                    if (nameLine.startsWith("regex:")) {
                        final String regex = nameLine.substring("regex:".length()).trim();
                        try {
                            final Pattern pattern = Pattern.compile(regex, 106);
                            result.add(pattern);
                        }
                        catch (final Exception ex) {
                            throw new ServerException("repo.InvalidRegex", new Object[] { nameLine, ex.getMessage() });
                        }
                    }
                    else if (nameLine.startsWith("REGEX:")) {
                        final String regex = nameLine.substring("REGEX:".length()).trim();
                        try {
                            final Pattern pattern = Pattern.compile(regex);
                            result.add(pattern);
                        }
                        catch (final Exception ex) {
                            throw new ServerException("repo.InvalidRegex", new Object[] { nameLine, ex.getMessage() });
                        }
                    }
                    else {
                        result.add(nameLine);
                    }
                }
            }
        }
        return result;
    }
    
    public static Set<Object> readNewlineSeparatedNamesOrRegexes(final String s) {
        if (s == null) {
            return null;
        }
        final List<Object> result = readListOfNewlineSeparatedNamesOrRegexes(s);
        return new HashSet<Object>(result);
    }
    
    public static List<Object> readListOfNewlineSeparatedNamesOrRegexes(final String s) {
        List<Object> result = null;
        if (s != null && s.trim().length() > 0) {
            final String[] split;
            final String[] lines = split = s.split("\\n");
            for (String line : split) {
                line = line.trim();
                if (line.length() != 0) {
                    if (result == null) {
                        result = new ArrayList<Object>();
                    }
                    if (line.startsWith("regex:")) {
                        final String regex = line.substring("regex:".length()).trim();
                        try {
                            final Pattern pattern = Pattern.compile(regex, 106);
                            result.add(pattern);
                        }
                        catch (final Exception ex) {
                            throw new ServerException("repo.InvalidRegex", new Object[] { line, ex.getMessage() });
                        }
                    }
                    else if (line.startsWith("REGEX:")) {
                        final String regex = line.substring("REGEX:".length()).trim();
                        try {
                            final Pattern pattern = Pattern.compile(regex);
                            result.add(pattern);
                        }
                        catch (final Exception ex) {
                            throw new ServerException("repo.InvalidRegex", new Object[] { line, ex.getMessage() });
                        }
                    }
                    else {
                        result.add(line);
                    }
                }
            }
        }
        return result;
    }
    
    public static boolean stringMatchesNamesOrRegexes(String s, final Collection<Object> patterns) {
        if (s == null) {
            return true;
        }
        s = s.trim();
        if (patterns == null || patterns.size() == 0) {
            return true;
        }
        for (final Object name : patterns) {
            if (name instanceof String) {
                if (name.equals(s)) {
                    return true;
                }
                continue;
            }
            else {
                final Pattern pattern = (Pattern)name;
                if (pattern.matcher(s).matches()) {
                    return true;
                }
                continue;
            }
        }
        return false;
    }
}
