/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.model;

public class ScNodeProperties {
    private String uniqueId;
    private String name;
    private String progLang;
    private byte noSearchMe;
    private byte noSearchCh;
    private String sharedNodeGroup;

    /**
     * Constructor for SourCherry node object with properties. Changes the previous method of transferring node properties data in String[]{name, prog_lang, nosearch_me, nosearch_ch} that is used to create / update node
     * @param uniqueId unique id of the node
     * @param name node name
     * @param progLang node type. Currently supported: "custom-colors" - rich text, "plain-text' - plain text, "sh" - automatic_syntax_highlighting
     * @param noSearchMe to exclude node from searches, 0 - keep node searches
     * @param noSearchCh to exclude subnodes of the node from searches, 0 - keep subnodes of the node in searches
     */
    public ScNodeProperties(String uniqueId, String name, String progLang, byte noSearchMe, byte noSearchCh, String sharedNodeGroup) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.progLang = progLang;
        this.noSearchMe = noSearchMe;
        this.noSearchCh = noSearchCh;
        this.sharedNodeGroup = sharedNodeGroup;
    }

    /**
     * Get name property of the node that is displayed in node properties
     * @return node name
     */
    public String getName() {
        return name;
    }

    /**
     * Set node name that will be displayed in node properties
     * @param name node name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get nodes no search children property that is displayed in node properties
     * @return node's no search children property
     */
    public byte getNoSearchCh() {
        return noSearchCh;
    }

    /**
     * Set node's no search children property that will be displayed in node properties
     * @param noSearchCh node's no search children property
     */
    public void setNoSearchCh(byte noSearchCh) {
        this.noSearchCh = noSearchCh;
    }

    /**
     * Get node's no search me property that is displayed in node properties
     * @return node's no search me property
     */
    public byte getNoSearchMe() {
        return noSearchMe;
    }

    /**
     * Set node's no search me property that will be displayd in node properties
     * @param noSearchMe node's no search me property
     */
    public void setNoSearchMe(byte noSearchMe) {
        this.noSearchMe = noSearchMe;
    }

    /**
     * Get node's prog lang property that is displayed in node properties
     * @return node's prog lang property
     */
    public String getProgLang() {
        return progLang;
    }

    /**
     * Set node's prog lang property that will be displayed in node properties
     * @param progLang node's prog lang property
     */
    public void setProgLang(String progLang) {
        this.progLang = progLang;
    }

    /**
     * Get node's shared nodes group that is being displayed in node properties
     * @return shared nodes group
     */
    public String getShareNodeGroup() {
        return sharedNodeGroup;
    }

    /**
     * Set node's shared node group that will be displayed in node properties
     * @param shareNodeGroup shared nodes group
     */
    public void setShareNodeGroup(String shareNodeGroup) {
        this.sharedNodeGroup = shareNodeGroup;
    }

    /**
     * Get unique Id of the node that properties are being displayed for
     * @return node unique ID
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Set unique ID of the node that properties will be dispayyed for
     * @param uniqueId node unique ID
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
