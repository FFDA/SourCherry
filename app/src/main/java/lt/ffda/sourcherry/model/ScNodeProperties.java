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

    /**
     * Constructor for SourCherry node object with properties. Changes the previous method of transferring node properties data in String[]{name, prog_lang, nosearch_me, nosearch_ch} that is used to create / update node
     * @param uniqueId unique id of the node
     * @param name node name
     * @param progLang node type. Currently supported: "custom-colors" - rich text, "plain-text' - plain text, "sh" - automatic_syntax_highlighting
     * @param noSearchMe to exclude node from searches, 0 - keep node searches
     * @param noSearchCh to exclude subnodes of the node from searches, 0 - keep subnodes of the node in searches
     */
    public ScNodeProperties(String uniqueId, String name, String progLang, byte noSearchMe, byte noSearchCh) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.progLang = progLang;
        this.noSearchMe = noSearchMe;
        this.noSearchCh = noSearchCh;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getNoSearchCh() {
        return noSearchCh;
    }

    public void setNoSearchCh(byte noSearchCh) {
        this.noSearchCh = noSearchCh;
    }

    public byte getNoSearchMe() {
        return noSearchMe;
    }

    public void setNoSearchMe(byte noSearchMe) {
        this.noSearchMe = noSearchMe;
    }

    public String getProgLang() {
        return progLang;
    }

    public void setProgLang(String progLang) {
        this.progLang = progLang;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
