/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.utils;

import java.util.regex.Pattern;

/**
 * Holds all regex patterns used in the app for efficiency
 */
public class RegexPatterns {
    private RegexPatterns() {}

    /**
     * Should match all list starts available in CherryTree with any amount of spaces in front of
     * list symbol and any space after it, but not new line.
     */
    public static final Pattern allListStarts = Pattern.compile(
            "^([ \\t\\f\\r]*)(([\\u2610\\u2611\\u2612\\u2022\\u25C7\\u25AA\\u002D\\u2192\\u21D2])|(\\d+[\\.\\)\\->]))[ \\t\\f\\r]*"
    );

    /**
     * Matches all checkboxes
     */
    public static final Pattern allCheckbox = Pattern.compile("\\s*[\\u2610\\u2611\\u2612]");

    /**
     * Matches checked and crossed checkboxes
     */
    public static final Pattern checkedCheckbox = Pattern.compile("\\s*[\\u2611\\u2612]");

    /**
     * Matches new line char
     */
    public static final Pattern lastNewline = Pattern.compile("\\n", Pattern.DOTALL);

    /**
     * Matches oldered list and it's digit as a group(1) at the start of the string
     */
    public static final Pattern orderdList = Pattern.compile("^(\\s*)(\\d+)[\\.\\)\\->]");

    /**
     * Matches oldered list and it's digit as a group(1) at any place of the string
     */
    public static final Pattern orderdListItem = Pattern.compile("\\n([ \\t\\f\\r]*)(\\d+)[\\.\\)\\->].*");

    /**
     * Matches unordered list items
     */
    public static final Pattern unorderedList = Pattern.compile("^\\s*([\\u2022\\u25C7\\u25AA\\u002D\\u2192\\u21D2])");

    /**
     * Matches ordered list items
     */
    public static final Pattern orderedList = Pattern.compile("^\\s*(\\d+[\\.\\)\\->])");
}
