/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.spans;

import android.text.style.ClickableSpan;

/**
 * Span used to display links to folders and files.
 * Holds additional values of link type and link itself
 */
public abstract class ClickableSpanLink extends ClickableSpan {
    private String linkType;
    private String base64Link;

    /**
     * Sets link type
     * @return link type. For folder - "fold", files - "file"
     */
    public String getLinkType() {
        return this.linkType;
    }

    /**
     * Sets link type
     * @param linkType link type. For folder - "fold", files - "file"
     */
    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    /**
     * Returns link encrypted in base64
     * @return link url/path in base64
     */
    public String getBase64Link() {
        return this.base64Link;
    }

    /**
     * Set link url/path
     * @param base64Link link url/path in base64
     */
    public void setBase64Link(String base64Link) {
        this.base64Link = base64Link;
    }
}
